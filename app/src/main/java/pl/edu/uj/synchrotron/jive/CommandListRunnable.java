package pl.edu.uj.synchrotron.jive;

import android.util.Log;

import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.CommandInfo;
import fr.esrf.TangoApi.DeviceProxy;

/**
 * Created by lukasz on 29.04.15.
 * This file is element of RESTful Jive application project.
 * You are free to use, copy and edit whole application or any of its components.
 * Application comes with no warranty. Altough author is trying to make it best, it may work or it may not work.
 */
public class CommandListRunnable implements Runnable {
private String tangoHost;
private String tangoPort;
private String deviceName;
private ATKPanelCallback activityCallback;

public CommandListRunnable(ATKPanelCallback callback, String devName, String host, String port) {
	tangoHost = host;
	tangoPort = port;
	deviceName = devName;
	activityCallback = callback;
	Log.d("AttributeListRunnable", "Created instance, arguments: devName: " + devName + " TANGO_HOST: " + host + ":" + port);
}

@Override
public void run() {
	String[] commandNames;
	int[] commandInTypes;
	int[] commandOutTypes;
	int commandCount;
	try {
		System.out.println("Connecting to device: " + deviceName);
		DeviceProxy dp = new DeviceProxy(deviceName, tangoHost, tangoPort);
		CommandInfo comm_info[] = dp.command_list_query();
		commandCount = comm_info.length;
		commandNames = new String[commandCount];
		commandInTypes = new int[commandCount];
		commandOutTypes = new int[commandCount];
		for (int i = 0; i < comm_info.length; i++) {
			commandNames[i] = comm_info[i].cmd_name;
			commandInTypes[i] = comm_info[i].in_type;
			commandOutTypes[i] = comm_info[i].out_type;
		}
		if (!(Thread.currentThread().isInterrupted() || activityCallback.areThreadsInterrupted())) {
			activityCallback.populateCommandSpinner(commandNames, commandInTypes, commandOutTypes, commandCount);
		}
	} catch (DevFailed e) {
		System.out.println("Problem occured while connecting with device: " + deviceName);
		e.printStackTrace();
		activityCallback.displayErrorMessage("Problem occured while connecting with device: " + deviceName, e);
	}
}
}
