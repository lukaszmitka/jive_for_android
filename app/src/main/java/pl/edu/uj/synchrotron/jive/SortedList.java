package pl.edu.uj.synchrotron.jive;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.Database;
import fr.esrf.TangoApi.DeviceInfo;

/**
 * Activity for listing devices in multi level sorted list.
 */
public class SortedList extends WifiMonitorActivity implements SortedListCallback {
private final static int SORT_BY_DEVICE = 0;
private final static int DEFAULT_SORT_TYPE = SORT_BY_DEVICE;
private int sortingType = DEFAULT_SORT_TYPE;
private final static int SORT_BY_CLASS = 1;
private final static int SORT_BY_SERVER = 2;
private static final int SORT_FULL_LIST = 3;
final Context context = this;
List<NLevelItem> list;
ListView listView;
SortedListCallback sortedListCallback;
private boolean trackDeviceStatus = false;
private String databaseHost;
private String databasePort;
private ListDevicesRunnable listDevicesRunnable;
private Thread devicesThread;
private HashMap<String, String> lastResponse;
private int lastSortType;

// ****************
// BUTTON LISTENERS
// ****************

/**
 * Listener for button, show device info.
 *
 * @param v Reference to the widget that was clicked.
 */
public void buttonClick(View v) {
	String devName = (String) v.getTag();
	System.out.println("Clicked object: " + devName);
	try {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
		String dbHost = settings.getString("dbHost", "");
		String dbPort = settings.getString("dbPort", "");
		Database db = new Database(dbHost, dbPort);
		DeviceInfo di = db.get_device_info(devName);
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(di.toString()).setTitle("Device info");
		AlertDialog dialog = builder.create();
		dialog.show();

	} catch (DevFailed e) {
		System.out.println("Error while connecting");
		e.printStackTrace();
	}
}

/**
 * Listener for button, start new activity which show properties.
 *
 * @param v Reference to the widget that was clicked.
 */
public void buttonProperties(View v) {
	String devName = (String) v.getTag();
	System.out.println("Clicked object: " + devName);
	SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
	String dbHost = settings.getString("dbHost", "");
	String dbPort = settings.getString("dbPort", "");
	Intent intent = new Intent(this, PropertiesActivity.class);
	intent.putExtra("deviceName", devName);
	intent.putExtra("dbHost", dbHost);
	intent.putExtra("dbPort", dbPort);
	startActivity(intent);
}

/**
 * Listener for button, start new activity which show attributes.
 *
 * @param v Reference to the widget that was clicked.
 */
public void buttonAttributes(View v) {
	String devName = (String) v.getTag();
	System.out.println("Clicked object: " + devName);
	SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
	String dbHost = settings.getString("dbHost", "");
	String dbPort = settings.getString("dbPort", "");
	Intent intent = new Intent(this, AttributesActivity.class);
	intent.putExtra("deviceName", devName);
	intent.putExtra("dbHost", dbHost);
	intent.putExtra("dbPort", dbPort);
	startActivity(intent);
}

/**
 * Listener for button, refresh list of devices.
 *
 * @param v Reference to the widget that was clicked.
 */
public void buttonSortedListRefresh(View v) {
	SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
	String dbHost = settings.getString("dbHost", "");
	String dbPort = settings.getString("dbPort", "");
	System.out.println("Refreshing list at: " + dbHost + ":" + dbPort);
	listDevicesRunnable = new ListDevicesRunnable(this, databaseHost, databasePort, sortingType, trackDeviceStatus);
	if (devicesThread.isAlive()) {
		devicesThread.interrupt();
	}
	devicesThread = new Thread(listDevicesRunnable);
	devicesThread.start();
}

/**
 * Button listener, read pattern from text view and filter device list with it.
 *
 * @param view interface element that called method
 */
public void buttonFilter(View view) {
	EditText filterPattern = (EditText) findViewById(R.id.sortedList_filterPattern);
	//filterDeviceList(lastResponse, lastSortType, filterPattern.getText().toString());
}

// **************************
// ACTIVITY LIFECYCLE METHODS
// **************************

@Override
protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_sorted_list);
	sortedListCallback = this;

	// this allows to connect with server in main thread
	//StrictMode.ThreadPolicy old = StrictMode.getThreadPolicy();
	//StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder(old).permitNetwork().build());

	SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
	String dbHost = settings.getString("dbHost", "");
	String dbPort = settings.getString("dbPort", "");
	System.out.println("Found tango host: " + dbHost + ":" + dbPort);
	if (dbHost.equals("") || dbPort.equals("")) {
		System.out.println("Requesting new tango host");
		setHost();
	} else {
		databaseHost = dbHost;
		databasePort = dbPort;
		System.out.println("Getting device list from server:  " + dbHost + ":" + dbPort);
		listDevicesRunnable = new ListDevicesRunnable(this, databaseHost, databasePort, sortingType, trackDeviceStatus);
		if (devicesThread != null) {
			if (devicesThread.isAlive()) {
				devicesThread.interrupt();
			}
		}
		devicesThread = new Thread(listDevicesRunnable);
		devicesThread.start();
	}
}

@Override
protected void onDestroy() {
	if (devicesThread != null) {
		if (devicesThread.isAlive()) {
			devicesThread.interrupt();
		}
	}
	super.onDestroy();
}

@Override
public boolean onPrepareOptionsMenu(Menu menu) {
	MenuItem menuItemTrackStatus = menu.findItem(R.id.action_track_status);
	MenuItem menuItemUntrackStatus = menu.findItem(R.id.action_untrack_status);
	menuItemTrackStatus.setVisible(!trackDeviceStatus);
	menuItemUntrackStatus.setVisible(trackDeviceStatus);
	return true;
}

@Override
public boolean onCreateOptionsMenu(Menu menu) {
	getMenuInflater().inflate(R.menu.menu_sorted_list, menu);

	return true;
}

@Override
public boolean onOptionsItemSelected(MenuItem item) {
	// Handle item selection
	switch (item.getItemId()) {
		case R.id.action_set_host:
			setHost();
			return true;
		case R.id.action_full_list:
			sortingType = SORT_FULL_LIST;
			listDevicesRunnable = new ListDevicesRunnable(this, databaseHost, databasePort, sortingType, trackDeviceStatus);
			if (devicesThread.isAlive()) {
				devicesThread.interrupt();
			}
			devicesThread = new Thread(listDevicesRunnable);
			devicesThread.start();
			return true;
		case R.id.action_sort_by_devices:
			sortingType = SORT_BY_DEVICE;
			System.out.println("Switch sorting to device");
			listDevicesRunnable = new ListDevicesRunnable(this, databaseHost, databasePort, sortingType, trackDeviceStatus);
			if (devicesThread.isAlive()) {
				devicesThread.interrupt();
			}
			devicesThread = new Thread(listDevicesRunnable);
			devicesThread.start();
			return true;
		case R.id.action_sort_by_classes:
			sortingType = SORT_BY_CLASS;
			System.out.println("Switch sorting to class");
			listDevicesRunnable = new ListDevicesRunnable(this, databaseHost, databasePort, sortingType, trackDeviceStatus);
			if (devicesThread.isAlive()) {
				devicesThread.interrupt();
			}
			devicesThread = new Thread(listDevicesRunnable);
			devicesThread.start();
			return true;
		case R.id.action_sort_by_servers:
			sortingType = SORT_BY_SERVER;
			System.out.println("Switch sorting to server");
			listDevicesRunnable = new ListDevicesRunnable(this, databaseHost, databasePort, sortingType, trackDeviceStatus);
			if (devicesThread.isAlive()) {
				devicesThread.interrupt();
			}
			devicesThread = new Thread(listDevicesRunnable);
			devicesThread.start();
			return true;
		case R.id.action_about_filter:
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setMessage(R.string.sorted_list_filter_description).setTitle(R.string.menu_item_about_filter);
			AlertDialog dialog = builder.create();
			dialog.show();
			((TextView) dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
			return true;
		case R.id.action_track_status:
			AlertDialog.Builder trackStatusBuilder = new AlertDialog.Builder(context);
			trackStatusBuilder.setMessage(R.string.track_status_warning).setTitle(R.string.warning);
			trackStatusBuilder.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					trackDeviceStatus = false;
				}
			});
			trackStatusBuilder.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					trackDeviceStatus = true;
					listDevicesRunnable = new ListDevicesRunnable(sortedListCallback, databaseHost, databasePort, sortingType,
							trackDeviceStatus);
					if (devicesThread.isAlive()) {
						devicesThread.interrupt();
					}
					devicesThread = new Thread(listDevicesRunnable);
					devicesThread.start();

				}
			});
			AlertDialog trackStatusDialog = trackStatusBuilder.create();
			trackStatusDialog.show();
			return true;
		case R.id.action_untrack_status:
			trackDeviceStatus = false;
			listDevicesRunnable = new ListDevicesRunnable(this, databaseHost, databasePort, sortingType, trackDeviceStatus);
			if (devicesThread.isAlive()) {
				devicesThread.interrupt();
			}
			devicesThread = new Thread(listDevicesRunnable);
			devicesThread.start();
			return true;
		default:
			return super.onOptionsItemSelected(item);
	}
}

/**
 * Start new activity for getting from user database host address and port.
 */
private void setHost() {
	Intent i = new Intent(this, SetHostActivity.class);
	startActivityForResult(i, 1);
}

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {

	if (requestCode == 1) {
		if (resultCode == RESULT_OK) {
			databaseHost = data.getStringExtra("host");
			databasePort = data.getStringExtra("port");
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString("dbHost", databaseHost);
			editor.putString("dbPort", databasePort);
			editor.commit();
			System.out.println("Result: " + databaseHost + ":" + databasePort);
			//TODO start thread
			//TODO refreshDeviceList(sortingType);
		}
		if (resultCode == RESULT_CANCELED) {
			System.out.println("Host not changed");
		}
	}
}

/**
 * Update currently shown list of devices with list received from server in JSON format.
 *
 * @param deviceList HashMap containing devices to be shown.
 * @param sortCase   Method of sorting devices.
 */
public void updateDeviceList(HashMap<String, String> deviceList, int sortCase) {
	listView = (ListView) findViewById(R.id.sortedList_listView);
	list = new ArrayList<>();
	boolean isDeviceAlive = false;

	int serverCount;
	String serverName;
	int instancesCount;
	String instanceName;
	int classCount;
	String className;
	int devicesCount;
	String deviceName;
	String domainName;

	switch (sortCase) {
		case SORT_BY_CLASS:
			final LayoutInflater inflater = LayoutInflater.from(this);

			classCount = Integer.parseInt(deviceList.get("numberOfClasses"));   //.getInt("numberOfClasses");

			for (int i = 0; i < classCount; i++) {
				className = deviceList.get("className" + i);

				final NLevelItem grandParent =
						new NLevelItem(new SomeObject(className, "", false), null, new NLevelView() {
							public View getView(NLevelItem item) {
								View view = inflater.inflate(R.layout.n_level_list_item_lev_1, listView, false);
								TextView tv = (TextView) view.findViewById(R.id.nLevelList_item_L1_textView);
								String name = ((SomeObject) item.getWrappedObject()).getName();
								tv.setText(name);
								return view;
							}
						});
				list.add(grandParent);
				devicesCount = Integer.parseInt(deviceList.get(className + "DevCount"));
				for (int j = 0; j < devicesCount; j++) {
					deviceName = deviceList.get(className + "Device" + j);
					if (trackDeviceStatus) {
						isDeviceAlive = Boolean.parseBoolean(deviceList.get(className + "isDeviceAlive" + j));
					}
					NLevelItem child =
							new NLevelItem(new SomeObject(deviceName, deviceName, isDeviceAlive), grandParent,
									new NLevelView() {
										public View getView(NLevelItem item) {
											View view = inflater.inflate(R.layout.n_level_list_member_item, listView, false);
											ImageView imageView = (ImageView) view.findViewById(R.id.nLevelListMemberDiode);
											if (trackDeviceStatus) {
												if (((SomeObject) item.getWrappedObject()).getIsAlive()) {
													imageView.setImageResource(R.drawable.dioda_zielona);
												} else {
													imageView.setImageResource(R.drawable.dioda_czerwona);
												}
											} else {
												imageView.setVisibility(View.INVISIBLE);
											}
											Button b = (Button) view.findViewById(R.id.nLevelList_member_button);
											b.setTag(((SomeObject) item.getWrappedObject()).getTag());
											TextView tv = (TextView) view.findViewById(R.id.nLevelList_member_textView);
											tv.setClickable(true);
											String name = ((SomeObject) item.getWrappedObject()).getName();
											tv.setText(name);
											tv.setTag(((SomeObject) item.getWrappedObject()).getTag());
											tv.setOnLongClickListener(new OnLongClickListener() {
												@Override
												public boolean onLongClick(View v) {
													TextView tv = (TextView) v;
													AlertDialog.Builder builder = new AlertDialog.Builder(tv.getContext());
													builder.setTitle("Choose action");
													builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
														public void onClick(DialogInterface dialog, int id) {
														}
													});
													String[] s = {"Monitor", "Test"};
													final String name = tv.getTag().toString();
													builder.setItems(s, new DialogInterface.OnClickListener() {
														public void onClick(DialogInterface dialog, int choice) {
															if (choice == 0) {
																	/*Intent i = new Intent(context, ATKPanelActivity.class);
																	i.putExtra("DEVICE_NAME", name);
																	i.putExtra("tangoHost", tangoHost);
																	i.putExtra("tangoPort", tangoPort);
																	startActivity(i);*/
																Toast toast = Toast.makeText(context, "This should run ATKPanel",
																		Toast.LENGTH_LONG);
																toast.show();
															}
															if (choice == 1) {
																Intent i = new Intent(context, DevicePanelActivity.class);
																i.putExtra("devName", name);
																i.putExtra("tangoHost", databaseHost);
																i.putExtra("tangoPort", databasePort);
																startActivity(i);
															}
														}
													});
													AlertDialog dialog = builder.create();
													dialog.show();
													return true;
												}
											});
											Button properties = (Button) view.findViewById(R.id.nLevelList_member_properties);
											properties.setTag(((SomeObject) item.getWrappedObject()).getTag());
											Button attributes = (Button) view.findViewById(R.id.nLevelList_member_attributes);
											attributes.setTag(((SomeObject) item.getWrappedObject()).getTag());
											return view;
										}
									});
					list.add(child);
				}
			}

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					NLevelAdapter devSortAdapter = new NLevelAdapter(list);
					listView.setAdapter(devSortAdapter);
					listView.setOnItemClickListener(new OnItemClickListener() {
						public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
							((NLevelAdapter) listView.getAdapter()).toggle(arg2);
							((NLevelAdapter) listView.getAdapter()).getFilter().filter();
						}
					});
				}
			});

			break;
		case SORT_BY_SERVER:
			final LayoutInflater serverSortingInflater = LayoutInflater.from(this);

			// get number of servers
			serverCount = Integer.parseInt(deviceList.get("ServerCount"));
			for (int i = 0; i < serverCount; i++) {
				serverName = deviceList.get("Server" + i);

				// prepare first level list item
				final NLevelItem serverLevel =
						new NLevelItem(new SomeObject(serverName, "", false), null, new NLevelView() {
							public View getView(NLevelItem item) {
								View view = serverSortingInflater.inflate(R.layout.n_level_list_item_lev_1, listView, false);
								TextView tv = (TextView) view.findViewById(R.id.nLevelList_item_L1_textView);
								String name = ((SomeObject) item.getWrappedObject()).getName();
								tv.setText(name);
								return view;
							}
						});
				// add frist level list item
				list.add(serverLevel);

				// get number of instances for the server
				instancesCount = Integer.parseInt(deviceList.get(serverName + "InstCnt"));
				for (int j = 0; j < instancesCount; j++) {
					instanceName = deviceList.get(serverName + "Instance" + j);
					// prepare second level list item
					final NLevelItem instanceLevel = new NLevelItem(new SomeObject(instanceName, "", false), serverLevel,
							new NLevelView() {
								public View getView(NLevelItem item) {
									View view =
											serverSortingInflater.inflate(R.layout.n_level_list_item_lev_2, listView, false);
									TextView tv = (TextView) view.findViewById(R.id.nLevelList_item_L2_textView);
									String name = ((SomeObject) item.getWrappedObject()).getName();
									tv.setText(name);
									return view;
								}
							});
					// add second level list item
					list.add(instanceLevel);

					// get number of classes in the instance
					classCount = Integer.parseInt(deviceList.get("Se" + i + "In" + j + "ClassCnt"));
					for (int k = 0; k < classCount; k++) {
						className = deviceList.get("Se" + i + "In" + j + "Cl" + k);
						// prepare third level list item
						final NLevelItem classLevel =
								new NLevelItem(new SomeObject(className, "", false), instanceLevel, new NLevelView() {
									public View getView(NLevelItem item) {
										View view =
												serverSortingInflater.inflate(R.layout.n_level_list_item_lev_3, listView, false);
										TextView tv = (TextView) view.findViewById(R.id.nLevelList_item_L3_textView);
										String name = ((SomeObject) item.getWrappedObject()).getName();
										tv.setText(name);
										return view;
									}
								});
						// add third level list item
						list.add(classLevel);

						// get count of devices for class
						devicesCount = Integer.parseInt(deviceList.get("Se" + i + "In" + j + "Cl" + k + "DCnt"));
						if (devicesCount > 0) {
							for (int l = 0; l < devicesCount; l++) {
								deviceName = deviceList.get("Se" + i + "In" + j + "Cl" + k + "Dev" + l);
								System.out.println("Add device " + deviceName + " to list");
								// prepare fourth level list item
								if (trackDeviceStatus) {
									isDeviceAlive = Boolean.parseBoolean(deviceList.get(deviceName + "isDeviceAlive" + l));
								}
								NLevelItem deviceLevel =
										new NLevelItem(new SomeObject(deviceName, deviceName, isDeviceAlive), classLevel,
												new NLevelView() {
													public View getView(NLevelItem item) {
														View view =
																serverSortingInflater.inflate(R.layout.n_level_list_member_item,
																		listView, false);
														ImageView imageView =
																(ImageView) view.findViewById(R.id.nLevelListMemberDiode);
														if (trackDeviceStatus) {
															if (((SomeObject) item.getWrappedObject()).getIsAlive()) {
																imageView.setImageResource(R.drawable.dioda_zielona);
															} else {
																imageView.setImageResource(R.drawable.dioda_czerwona);
															}
														} else {
															imageView.setVisibility(View.INVISIBLE);
														}
														Button b = (Button) view.findViewById(R.id.nLevelList_member_button);
														b.setTag(((SomeObject) item.getWrappedObject()).getTag());
														TextView tv = (TextView) view.findViewById(R.id.nLevelList_member_textView);
														tv.setClickable(true);
														String name = ((SomeObject) item.getWrappedObject()).getName();
														tv.setText(name);
														tv.setTag(((SomeObject) item.getWrappedObject()).getTag());
														tv.setOnLongClickListener(new OnLongClickListener() {
															@Override
															public boolean onLongClick(View v) {
																TextView tv = (TextView) v;
																AlertDialog.Builder builder = new AlertDialog.Builder(tv.getContext
																		());
																builder.setTitle("Choose action");
																builder.setNegativeButton("Cancel",
																		new DialogInterface.OnClickListener() {
																			public void onClick(DialogInterface dialog, int id) {
																			}
																		});
																String[] s = {"Monitor", "Test"};
																final String name = tv.getTag().toString();
																builder.setItems(s, new DialogInterface.OnClickListener() {
																	public void onClick(DialogInterface dialog, int choice) {
																		if (choice == 0) {
																				/*Intent i = new Intent(context, ATKPanelActivity.class);
																				i.putExtra("DEVICE_NAME", name);
																				i.putExtra("restHost", RESTfulTangoHost);
																				i.putExtra("tangoHost", tangoHost);
																				i.putExtra("tangoPort", tangoPort);
																				startActivity(i);*/
																			Toast toast = Toast.makeText(context, "This should run ATKPanel",
																					Toast.LENGTH_LONG);
																			toast.show();
																		}
																		if (choice == 1) {
																			Intent i = new Intent(context, DevicePanelActivity.class);
																			i.putExtra("devName", name);
																			i.putExtra("tangoHost", databaseHost);
																			i.putExtra("tangoPort", databasePort);
																			startActivity(i);
																		}
																	}
																});
																AlertDialog dialog = builder.create();
																dialog.show();
																return true;
															}
														});
														Button properties =
																(Button) view.findViewById(R.id.nLevelList_member_properties);
														properties.setTag(((SomeObject) item.getWrappedObject()).getTag());
														Button attributes =
																(Button) view.findViewById(R.id.nLevelList_member_attributes);
														attributes.setTag(((SomeObject) item.getWrappedObject()).getTag());
														return view;
													}
												});
								// add fourth level list item
								list.add(deviceLevel);
							}
						}

					}
				}
			}

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					NLevelAdapter devSortAdapter = new NLevelAdapter(list);
					listView.setAdapter(devSortAdapter);
					listView.setOnItemClickListener(new OnItemClickListener() {
						public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
							((NLevelAdapter) listView.getAdapter()).toggle(arg2);
							((NLevelAdapter) listView.getAdapter()).getFilter().filter();
						}
					});
				}
			});

			break;
		case SORT_BY_DEVICE:
			final LayoutInflater deviceSortingInflater = LayoutInflater.from(this);

			Log.d("updateDeviceList", "Received response: " + deviceList.toString());
			// get number of servers
			int domainCount = Integer.parseInt(deviceList.get("domainCount"));
			for (int i = 0; i < domainCount; i++) {
				domainName = deviceList.get("domain" + i);
				// prepare first level list item
				final NLevelItem serverLevel =
						new NLevelItem(new SomeObject(domainName, "", false), null, new NLevelView() {
							public View getView(NLevelItem item) {
								View view = deviceSortingInflater.inflate(R.layout.n_level_list_item_lev_1, listView, false);
								TextView tv = (TextView) view.findViewById(R.id.nLevelList_item_L1_textView);
								String name = ((SomeObject) item.getWrappedObject()).getName();
								tv.setText(name);
								return view;
							}
						});
				// add frist level list item
				list.add(serverLevel);

				// get number of classes in the instance
				classCount = Integer.parseInt(deviceList.get("domain" + i + "classCount"));
				for (int k = 0; k < classCount; k++) {
					className = deviceList.get("domain" + i + "class" + k);
					// prepare third level list item
					final NLevelItem classLevel =
							new NLevelItem(new SomeObject(className, "", false), serverLevel, new NLevelView() {
								public View getView(NLevelItem item) {
									View view =
											deviceSortingInflater.inflate(R.layout.n_level_list_item_lev_2, listView, false);
									TextView tv = (TextView) view.findViewById(R.id.nLevelList_item_L2_textView);
									String name = ((SomeObject) item.getWrappedObject()).getName();
									tv.setText(name);
									return view;
								}
							});
					// add third level list item
					list.add(classLevel);

					// get count of devices for class
					devicesCount = Integer.parseInt(deviceList.get("domain" + i + "class" + k + "devCount"));
					if (devicesCount > 0) {
						for (int l = 0; l < devicesCount; l++) {
							deviceName = deviceList.get("domain" + i + "class" + k + "device" + l);
							System.out.println("Add device " + deviceName + " to list");
							// prepare fourth level list item
							if (trackDeviceStatus) {
								isDeviceAlive = Boolean.parseBoolean(deviceList.get(deviceName + "isDeviceAlive"));
							}
							NLevelItem deviceLevel =
									new NLevelItem(new SomeObject(deviceName, deviceName, isDeviceAlive), classLevel,
											new NLevelView() {
												public View getView(NLevelItem item) {
													View view =
															deviceSortingInflater.inflate(R.layout.n_level_list_member_item,
																	listView, false);
													ImageView imageView = (ImageView) view.findViewById(R.id
															.nLevelListMemberDiode);
													if (trackDeviceStatus) {
														if (((SomeObject) item.getWrappedObject()).getIsAlive()) {
															imageView.setImageResource(R.drawable.dioda_zielona);
														} else {
															imageView.setImageResource(R.drawable.dioda_czerwona);
														}
													} else {
														imageView.setVisibility(View.INVISIBLE);
													}
													Button b = (Button) view.findViewById(R.id.nLevelList_member_button);
													b.setTag(((SomeObject) item.getWrappedObject()).getTag());
													TextView tv = (TextView) view.findViewById(R.id.nLevelList_member_textView);
													tv.setClickable(true);
													String name = ((SomeObject) item.getWrappedObject()).getName();
													tv.setText(name);
													tv.setTag(((SomeObject) item.getWrappedObject()).getTag());
													tv.setOnLongClickListener(new OnLongClickListener() {
														@Override
														public boolean onLongClick(View v) {
															TextView tv = (TextView) v;
															AlertDialog.Builder builder = new AlertDialog.Builder(tv.getContext
																	());
															builder.setTitle("Choose action");
															builder.setNegativeButton("Cancel",
																	new DialogInterface.OnClickListener() {
																		public void onClick(DialogInterface dialog, int id) {
																		}
																	});
															String[] s = {"Monitor", "Test"};
															final String name = tv.getTag().toString();
															builder.setItems(s, new DialogInterface.OnClickListener() {
																public void onClick(DialogInterface dialog, int choice) {
																	if (choice == 0) {
																			/*Intent i = new Intent(context, ATKPanelActivity.class);
																			i.putExtra("DEVICE_NAME", name);
																			i.putExtra("restHost", RESTfulTangoHost);
																			i.putExtra("tangoHost", tangoHost);
																			i.putExtra("tangoPort", tangoPort);
																			startActivity(i);*/
																		Toast toast = Toast.makeText(context, "This should run ATKPanel",
																				Toast.LENGTH_LONG);
																		toast.show();
																	}
																	if (choice == 1) {
																		Intent i = new Intent(context, DevicePanelActivity.class);
																		i.putExtra("devName", name);
																		i.putExtra("tangoHost", databaseHost);
																		i.putExtra("tangoPort", databasePort);
																		startActivity(i);
																	}
																}
															});
															AlertDialog dialog = builder.create();
															dialog.show();
															return true;
														}
													});
													Button properties =
															(Button) view.findViewById(R.id.nLevelList_member_properties);
													properties.setTag(((SomeObject) item.getWrappedObject()).getTag());
													Button attributes =
															(Button) view.findViewById(R.id.nLevelList_member_attributes);
													attributes.setTag(((SomeObject) item.getWrappedObject()).getTag());
													return view;
												}
											});
							// add fourth level list item
							list.add(deviceLevel);
						}
					}

				}

			}
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					NLevelAdapter devSortAdapter = new NLevelAdapter(list);
					listView.setAdapter(devSortAdapter);
					listView.setOnItemClickListener(new OnItemClickListener() {
						public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
							((NLevelAdapter) listView.getAdapter()).toggle(arg2);
							((NLevelAdapter) listView.getAdapter()).getFilter().filter();
						}
					});
				}
			});

			break;
		case SORT_FULL_LIST:
			final LayoutInflater fullListInflater = LayoutInflater.from(this);

			int deviceCount = Integer.parseInt(deviceList.get("numberOfDevices"));
			for (int j = 0; j < deviceCount; j++) {
				deviceName = deviceList.get("device" + j);
				if (trackDeviceStatus) {
					isDeviceAlive = Boolean.parseBoolean(deviceList.get(deviceName + "isDeviceAlive"));
				}
				NLevelItem child =
						new NLevelItem(new SomeObject(deviceName, deviceName, isDeviceAlive), null,
								new NLevelView() {
									public View getView(NLevelItem item) {
										View view = fullListInflater.inflate(R.layout.n_level_list_member_item, listView,
												false);
										ImageView imageView = (ImageView) view.findViewById(R.id.nLevelListMemberDiode);
										if (trackDeviceStatus) {
											if (((SomeObject) item.getWrappedObject()).getIsAlive()) {
												imageView.setImageResource(R.drawable.dioda_zielona);
											} else {
												imageView.setImageResource(R.drawable.dioda_czerwona);
											}
										} else {
											imageView.setVisibility(View.INVISIBLE);
										}
										Button b = (Button) view.findViewById(R.id.nLevelList_member_button);
										b.setTag(((SomeObject) item.getWrappedObject()).getTag());
										TextView tv = (TextView) view.findViewById(R.id.nLevelList_member_textView);
										tv.setClickable(true);
										String name = ((SomeObject) item.getWrappedObject()).getName();
										tv.setText(name);
										tv.setTag(((SomeObject) item.getWrappedObject()).getTag());
										tv.setOnLongClickListener(new OnLongClickListener() {
											@Override
											public boolean onLongClick(View v) {
												TextView tv = (TextView) v;
												AlertDialog.Builder builder = new AlertDialog.Builder(tv.getContext());
												builder.setTitle("Choose action");
												builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
													public void onClick(DialogInterface dialog, int id) {
													}
												});
												String[] s = {"Monitor", "Test"};
												final String name = tv.getTag().toString();
												builder.setItems(s, new DialogInterface.OnClickListener() {
													public void onClick(DialogInterface dialog, int choice) {
														if (choice == 0) {
																/*Intent i = new Intent(context, ATKPanelActivity.class);
																i.putExtra("DEVICE_NAME", name);
																i.putExtra("restHost", RESTfulTangoHost);
																i.putExtra("tangoHost", tangoHost);
																i.putExtra("tangoPort", tangoPort);
																startActivity(i);*/
															Toast toast = Toast.makeText(context, "This should run ATKPanel",
																	Toast.LENGTH_LONG);
															toast.show();
														}
														if (choice == 1) {
															Intent i = new Intent(context, DevicePanelActivity.class);
															i.putExtra("devName", name);
															i.putExtra("tangoHost", databaseHost);
															i.putExtra("tangoPort", databasePort);
															startActivity(i);
														}
													}
												});
												AlertDialog dialog = builder.create();
												dialog.show();
												return true;
											}
										});
										Button properties = (Button) view.findViewById(R.id.nLevelList_member_properties);
										properties.setTag(((SomeObject) item.getWrappedObject()).getTag());
										Button attributes = (Button) view.findViewById(R.id.nLevelList_member_attributes);
										attributes.setTag(((SomeObject) item.getWrappedObject()).getTag());
										return view;
									}
								});
				list.add(child);
			}

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					NLevelAdapter devSortAdapter = new NLevelAdapter(list);
					listView.setAdapter(devSortAdapter);
					listView.setOnItemClickListener(new OnItemClickListener() {
						public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
							((NLevelAdapter) listView.getAdapter()).toggle(arg2);
							((NLevelAdapter) listView.getAdapter()).getFilter().filter();
						}
					});
				}
			});
			break;
		default:
			break;
	}
	runOnUiThread(new Runnable() {
		@Override
		public void run() {
			enableUserInterface(true);
		}
	});
}

private void enableUserInterface(boolean enabled) {
	if (enabled) {
		ProgressBar progressBar = (ProgressBar) findViewById(R.id.sortedList_progressBar);
		progressBar.setVisibility(View.INVISIBLE);
		ListView listView = (ListView) findViewById(R.id.sortedList_listView);
		listView.setFocusable(true);
		listView.setEnabled(true);
		Button refreshButton = (Button) findViewById(R.id.sortedList_refreshButton);
		refreshButton.setFocusable(true);
		refreshButton.setEnabled(true);
		Button filterButton = (Button) findViewById(R.id.sortedList_filterButton);
		filterButton.setEnabled(true);
		filterButton.setFocusable(true);
		EditText filterTextView = (EditText) findViewById(R.id.sortedList_filterPattern);
		filterTextView.setEnabled(true);
		filterTextView.setFocusable(true);
		filterTextView.setFocusableInTouchMode(true);
	} else {
		ProgressBar progressBar = (ProgressBar) findViewById(R.id.sortedList_progressBar);
		progressBar.setVisibility(View.VISIBLE);
		ListView listView = (ListView) findViewById(R.id.sortedList_listView);
		listView.setFocusable(false);
		listView.setEnabled(false);
		Button refreshButton = (Button) findViewById(R.id.sortedList_refreshButton);
		refreshButton.setFocusable(false);
		refreshButton.setEnabled(false);
		Button filterButton = (Button) findViewById(R.id.sortedList_filterButton);
		filterButton.setEnabled(false);
		filterButton.setFocusable(false);
		EditText filterTextView = (EditText) findViewById(R.id.sortedList_filterPattern);
		filterTextView.setEnabled(false);
		filterTextView.setFocusable(false);
		filterTextView.setFocusableInTouchMode(false);
	}
}

public void displayErrorMessage(final String message, DevFailed e) {
	Log.d("displayErrorMessage()", "Error occurred: " + message);
	e.printStackTrace();
	runOnUiThread(new Runnable() {
		@Override
		public void run() {
			Log.d("displayErrorMessage()", "Error occurred: " + message);
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle("Error !!!");
			builder.setMessage(message);
			AlertDialog dialog = builder.create();
			dialog.show();
		}
	});
}
}
