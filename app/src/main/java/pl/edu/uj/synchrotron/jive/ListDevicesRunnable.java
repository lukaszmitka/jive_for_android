package pl.edu.uj.synchrotron.jive;

import android.util.Log;

import java.util.HashMap;

import fr.esrf.Tango.DevFailed;
import fr.esrf.Tango.DevVarLongStringArray;
import fr.esrf.TangoApi.ApiUtil;
import fr.esrf.TangoApi.Database;
import fr.esrf.TangoApi.DeviceData;
import fr.esrf.TangoApi.DeviceProxy;

/**
 * Created by lukasz on 15.04.15.
 * This file is element of RESTful Jive application project.
 * You are free to use, copy and edit whole application or any of its components.
 * Application comes with no warranty. Altough author is trying to make it best, it may work or it may not work.
 */
public class ListDevicesRunnable implements Runnable {
private String tangoHost;
private String tangoPort;
private SortedListCallback activityCallback;
private int sorting;
private boolean interrupted = false;
private boolean trackStatus;
private boolean connectionOK = false;

public ListDevicesRunnable(SortedListCallback callback, String host, String port, int sortCase, boolean trackStatus) {
	tangoHost = host;
	tangoPort = port;
	activityCallback = callback;
	sorting = sortCase;
	this.trackStatus = trackStatus;
}

@Override
public void run() {
	Log.d("ListDevRun: run()", "tangoHost=" + tangoHost);
	Log.d("ListDevRun: run()", "tangoPort=" + tangoPort);
	Log.d("ListDevRun: run()", "activityCallback=" + activityCallback);
	Log.d("ListDevRun: run()", "sorting=" + sorting);
	HashMap<String, String> deviceList = new HashMap<>();

	String devices[];

	try {
		System.out.println("Connecting to database");
		Database db = ApiUtil.get_db_obj(tangoHost, tangoPort);
		Log.d("ListDevRun.run()", "Connected to database at: " + tangoHost + ":" + tangoPort);
		connectionOK = true;
		DeviceProxy dp;
		long t0, t1, time;
		switch (sorting) {
			case 1: // sort by class
				t0 = System.nanoTime();
				String classes[] = db.get_class_list("*");
				deviceList.put("numberOfClasses", String.valueOf(classes.length));
				for (int i = 0; i < classes.length; i++) {
					deviceList.put("className" + i, classes[i]);
					DeviceData argin = new DeviceData();
					String request = "select name from device where class='" + classes[i] + "' order by name";
					argin.insert(request);
					DeviceData argout = db.command_inout("DbMySqlSelect", argin);
					DevVarLongStringArray arg = argout.extractLongStringArray();
					for (int j = 0; j < arg.svalue.length; j++) {
						deviceList.put(classes[i] + "DevCount", String.valueOf(arg.svalue.length));
						deviceList.put(classes[i] + "Device" + j, arg.svalue[j].toUpperCase());
						// checking if device is alive
						if (trackStatus) {
							try {
								dp = new DeviceProxy(arg.svalue[j], tangoHost, tangoPort);
								dp.ping();
								deviceList.put(classes[i] + "isDeviceAlive" + j, String.valueOf(true));
								// System.out.println("Device "+classes[j]+"is alive");
							} catch (DevFailed e) {
								e.printStackTrace();
								deviceList.put(classes[i] + "isDeviceAlive" + j, String.valueOf(false));
							}
						}
						if (Thread.interrupted()) {
							interrupted = true;
							break;
						}
					}
				}
				deviceList.put("connectionStatus", "OK");
				t1 = System.nanoTime();
				time = (t1 - t0) / 1000000;
				deviceList.put("Operation time", String.valueOf(time));
				break;
			case 2: // sort by server
				t0 = System.nanoTime();
				System.out.println("Getting devices sorted by servers");
				String servers[];
				servers = db.get_server_name_list();
				// int device_count = 0;
				deviceList.put("ServerCount", String.valueOf(servers.length));
				String[] instances;
				String[] srvList;
				String admName;
				String admNameSub;
				String classSub;
				String devSub;
				String[] devList;
				String[] dbList;
				for (int i = 0; i < servers.length; i++) {
					// System.out.println("Server name: [" + i + "] " + servers[i]);
					deviceList.put("Server" + i, servers[i]);
					instances = db.get_instance_name_list(servers[i]);
					deviceList.put(servers[i] + "InstCnt", String.valueOf(instances.length));
					admNameSub = "dserver/" + servers[i] + "/";
					for (int j = 0; j < instances.length; j++) {
						classSub = "Se" + i + "In" + j + "Cl";
						// System.out.println("		Instance [" + j + "] name: " + instances[j]);
						deviceList.put(servers[i] + "Instance" + j, instances[j]);
						dbList = db.get_server_class_list(servers[i] + "/" + instances[j]);
						// Get the list from the database
						if (dbList.length > 0) {
							deviceList.put("Se" + i + "In" + j + "ClassCnt", String.valueOf(dbList.length));
							for (int k = 0; k < dbList.length; k++) {
								// System.out.println("				Class name: " + dbList[k]);
								deviceList.put(classSub + k, dbList[k]);
								devList = db.get_device_name(servers[i] + "/" + instances[j], dbList[k]);
								deviceList.put("Se" + i + "In" + j + "Cl" + k + "DCnt", String.valueOf(devList.length));
								// prepare string for naming devices
								devSub = classSub + k + "Dev";
								for (int l = 0; l < devList.length; l++) {
									deviceList.put(devSub + l, devList[l].toUpperCase());
									// System.out.println("					Device[" + device_count + "]: " + devList[l]);
									// device_count++;
									if (trackStatus) {
										try {
											dp = new DeviceProxy(devList[l], tangoHost, tangoPort);
											dp.ping();
											deviceList.put(devList[l].toUpperCase() + "isDeviceAlive" + l, String.valueOf(true));
											// System.out.println("Device "+classes[j]+"is alive");
										} catch (DevFailed e) {
											deviceList.put(devList[l].toUpperCase() + "isDeviceAlive" + l, String.valueOf(false));
										}
									}
									if (Thread.interrupted()) {
										interrupted = true;
										break;
									}
								}
							}
						} else {
							deviceList.put("Se" + i + "In" + j + "ClassCnt", String.valueOf(dbList.length));
							srvList = null;
							try {
								// Try to get class list through the admin device
								admName = admNameSub + instances[j];
								DeviceProxy adm = new DeviceProxy(admName, tangoHost, tangoPort);
								DeviceData datum = adm.command_inout("QueryClass");
								srvList = datum.extractStringArray();
							} catch (DevFailed e) {
								System.out.println("Adm name try error: " + e.getMessage());
							}
							if (srvList != null) {
								deviceList.put("Se" + i + "In" + j + "ClassCnt", String.valueOf(srvList.length));
								for (int k = 0; k < srvList.length; k++) {
									// System.out.println("				Server name: " + srvList[k]);
									deviceList.put(classSub + k, srvList[k]);
									devList = db.get_device_name(servers[i] + "/" + instances[j], srvList[k]);
									deviceList.put(classSub + k + "DCnt", String.valueOf(devList.length));
									devSub = classSub + k + "Dev";
									for (int l = 0; l < devList.length; l++) {
										deviceList.put(devSub + l, devList[l].toUpperCase());
										// System.out.println("					Device[" + device_count + "]: " + devList[l]);
										// device_count++;
										if (trackStatus) {
											try {
												dp = new DeviceProxy(devList[l], tangoHost, tangoPort);
												dp.ping();
												deviceList.put(devList[l].toUpperCase() + "isDeviceAlive" + l,
														String.valueOf(true));
												// System.out.println("Device "+classes[j]+"is alive");
											} catch (DevFailed e) {
												e.printStackTrace();
												deviceList.put(devList[l].toUpperCase() + "isDeviceAlive" + l,
														String.valueOf(false));
											}
										}
										if (Thread.interrupted()) {
											interrupted = true;
											break;
										}
									}
								}
							}
						}
					}
				}
				// System.out.println("Device count: " + device_count);
				deviceList.put("connectionStatus", "OK");
				t1 = System.nanoTime();
				time = (t1 - t0) / 1000000;
				deviceList.put("Operation time", String.valueOf(time));
				break;
			case 0: // sort by devices
				t0 = System.nanoTime();
				System.out.println("Connecting to database");
				// db = ApiUtil.get_db_obj(host, port);
				System.out.println("Getting data from database");
				devices = db.get_device_list("*");
				deviceList.put("connectionStatus", "OK");
				deviceList.put("numberOfDevices", String.valueOf(devices.length));
				String[] splitted;
				System.out.println("Devices: ");
				for (String device : devices) {
					System.out.println(device);
				}
				int i = devices.length;
				int domainCount = 0;
				int classCount;
				int deviceCount;
				String devDomain;
				String devClass;
				int j = 0;
				while (j < i) {
					splitted = devices[j].toUpperCase().split("/");
					devDomain = splitted[0];
					deviceList.put("domain" + domainCount, devDomain);
					classCount = 0;
					// DevClassList dcl = new DevClassList(devDomain);
					System.out.println("Petla 1 :" + devDomain + "  " + splitted[0]);
					while (devDomain.equals(splitted[0]) && (j < i)) {
						splitted = devices[j].toUpperCase().split("/");
						devClass = splitted[1];
						System.out.println("    Petla 2 :" + devClass + "  " + splitted[1]);
						deviceList.put("domain" + domainCount + "class" + classCount, devClass);
						// ArrayList<String> members = new ArrayList<String>();
						deviceCount = 0;
						while (devClass.equals(splitted[1]) && (j < i) && devDomain.equals(splitted[0])) {
							System.out.println("      Petla 3 :" + splitted[2]);
							deviceList.put("domain" + domainCount + "class" + classCount + "device" + deviceCount,
									devices[j].toUpperCase());
							deviceCount++;
							System.out.println("Processing device: " + devDomain + "/" + devClass + "/" + splitted[2]);
							if (trackStatus) {
								try {
									dp = new DeviceProxy(devices[j].toUpperCase(), tangoHost, tangoPort);
									dp.ping();
									deviceList.put(devices[j].toUpperCase() + "isDeviceAlive", String.valueOf(true));
									System.out.println("Device " + devices[j] + "is alive");
								} catch (DevFailed e) {
									e.printStackTrace();
									deviceList.put(devices[j].toUpperCase() + "isDeviceAlive", String.valueOf(false));
								}
							}
							j++;
							if (j < i) {
								splitted = devices[j].toUpperCase().split("/");
							} else {
								break;
							}
							if (Thread.interrupted()) {
								interrupted = true;
								break;
							}
						}
						deviceList.put("domain" + domainCount + "class" + classCount + "devCount",
								String.valueOf(deviceCount));
						classCount++;
					}
					deviceList.put("domain" + domainCount + "classCount", String.valueOf(classCount));
					domainCount++;
				}
				deviceList.put("domainCount", String.valueOf(domainCount));
				deviceList.put("connectionStatus", "OK");
				t1 = System.nanoTime();
				time = (t1 - t0) / 1000000;
				deviceList.put("Operation time", String.valueOf(time));
				break;
			case 3: // full list
				try {
					System.out.println("Connecting to database");
					System.out.println("Getting data from database");
					devices = db.get_device_list("*");
					deviceList.put("numberOfDevices", String.valueOf(devices.length));
					t0 = System.nanoTime();
					for (i = 0; i < devices.length; i++) {
						deviceList.put("device" + i, devices[i].toUpperCase());
						if (trackStatus) {
							try {
								dp = new DeviceProxy(devices[i].toUpperCase(), tangoHost, tangoPort);
								dp.ping();
								deviceList.put(devices[i].toUpperCase() + "isDeviceAlive", String.valueOf(true));
							} catch (DevFailed e) {
								e.printStackTrace();
								deviceList.put(devices[i].toUpperCase() + "isDeviceAlive", String.valueOf(false));
							}
						}
					}
					deviceList.put("connectionStatus", "OK");
					t1 = System.nanoTime();
					time = (t1 - t0) / 1000000;
					deviceList.put("Operation time", String.valueOf(time));
				} catch (DevFailed e) {
					deviceList.put("connectionStatus", "Unable to connect with device");
					e.printStackTrace();
				}
				break;
			default:
				deviceList.put("connectionStatus", "Unknown type of sorting!");
				break;
		}
	} catch (DevFailed e) {
		deviceList.put("connectionStatus", "Unable to connect with device");
		connectionOK = false;
		activityCallback.displayErrorMessage("Unable to connect with device", e);
	}

	if (!interrupted && connectionOK) {
		Log.d("ListDevRun: run()", "Updating device list");
		activityCallback.updateDeviceList(deviceList, sorting);
	}
}
}
