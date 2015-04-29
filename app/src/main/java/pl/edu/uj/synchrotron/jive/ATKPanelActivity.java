package pl.edu.uj.synchrotron.jive;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.ui.AnchorPosition;
import com.androidplot.ui.DynamicTableModel;
import com.androidplot.ui.SizeLayoutType;
import com.androidplot.ui.SizeMetrics;
import com.androidplot.ui.XLayoutStyle;
import com.androidplot.ui.YLayoutStyle;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoDs.TangoConst;

public class ATKPanelActivity extends WifiMonitorActivity implements TangoConst, ATKPanelCallback {
private static final int DEFAULT_REFRESHING_PERIOD = 1000;
private int refreshingPeriod = DEFAULT_REFRESHING_PERIOD;
private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

private final Context context = this;
private boolean firstSelection, threadsInterrupted = false;
private boolean[] attributeWritableArray;

private String attributeName, plotAttributeName, deviceName, tangoHost, tangoPort, commandOut, plotLabel, deviceStatus,
		message;
private String[] commandNamesArray;
private List<String> commandArray, commandInTypeArray, commandOutTypeArray, scalarAttrbuteArray, nonScalarAttributeArray;
private HashMap<String, String> hmResponse, request;

private int plotHeight, plotWidth, maxId, minId, plotId, scaleId, numberOfScalars, numberOfCommands;
private int[] commandInTypesArray, commandOutTypesArray;
private int[][] ids;
private Number[] plotSeries;
private double[][] doublePlotSeries;

private XYPlot plot;
private ScrollView scrollView;
private RelativeLayout relativeLayout;
private StatusRunnable statusRunnable;
private AttributeListRunnable attributeListRunnable;
private AttributesRunnable attributesRunnable;
private PlotRunnable plotRunnable;
private CommandListRunnable commandListRunnable;
private CommandExecuteRunnable commandExecuteRunnable;
private Thread statusThread, attributeListThread, attributesThread, plotThread, commandListThread, commandExecuteThread,
		attributeUpdateThread;

/**
 * Generate a value suitable for use in setId
 * This value will not collide with ID values generated at build time by aapt for R.id.
 *
 * @return a generated ID value
 */
public static int generateViewId() {
	for (; ; ) {
		final int result = sNextGeneratedId.get();
		// aapt-generated IDs have the high byte nonzero; clamp to the range under that.
		int newValue = result + 1;
		if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
		if (sNextGeneratedId.compareAndSet(result, newValue)) {
			return result;
		}
	}
}

@Override
protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);

	firstSelection = true;
	setContentView(R.layout.activity_atkpanel);
	//StrictMode.ThreadPolicy old = StrictMode.getThreadPolicy();
	//StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder(old).permitNetwork().build());

	scrollView = (ScrollView) findViewById(R.id.scrollView);
	relativeLayout = (RelativeLayout) findViewById(R.id.atkPanel_innerLayout);
	plotId = generateViewId();
	scaleId = generateViewId();
	maxId = generateViewId();
	minId = generateViewId();

	//restartQueue();

	// gett device name from intent if was set
	Intent i = getIntent();
	Log.v("onCreate()", "Got intent");
	if (i.hasExtra("DEVICE_NAME")) {
		Log.d("onCreate()", "Got device name from intent");
		deviceName = i.getStringExtra("DEVICE_NAME");
		setTitle(getString(R.string.title_activity_atkpanel) + " : " + deviceName);

	} else { // prompt user for device name
		Log.d("onCreate()", "Requesting device name from user");
		setDeviceName();
	}

	// check if tango host is saved in config, else prompt user for it
	if (i.hasExtra("tangoHost") && i.hasExtra("tangoPort")) {
		Log.d("onCreate()", "Got host from intent");
		tangoHost = i.getStringExtra("tangoHost");
		tangoPort = i.getStringExtra("tangoPort");
		//populatePanel();
	} else {
		Log.d("onCreate()", "Request host from user");
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
		String settingsTangoHost = settings.getString("TangoHost", "");
		String settingsTangoPort = settings.getString("TangoPort", "");
		Log.d("onCreate()", "Found Tango host: " + settingsTangoHost);
		Log.d("onCreate()", "Found Tango port: " + settingsTangoPort);
		if (settingsTangoHost.equals("") || settingsTangoPort.equals("")) {
			Log.d("ATK Panel onCreate", "Requesting new tango host,port and RESTful host");
			setHost();
		} else {
			tangoHost = settingsTangoHost;
			tangoPort = settingsTangoPort;
			Log.d("onCreate()", "Populating panel from Tango Host: " +
					settingsTangoHost + ":" + settingsTangoPort);
			//populatePanel();
		}
	}

	statusRunnable = new StatusRunnable(this, deviceName, tangoHost, tangoPort, refreshingPeriod);
	if (statusThread != null) {
		statusThread.interrupt();
	}
	statusThread = new Thread(statusRunnable);
	statusThread.start();

	attributeListRunnable = new AttributeListRunnable(this, deviceName, tangoHost, tangoPort);
	if (attributeListThread != null) {
		attributeListThread.interrupt();
	}
	attributeListThread = new Thread(attributeListRunnable);
	attributeListThread.start();

	commandListRunnable = new CommandListRunnable(this, deviceName, tangoHost, tangoPort);
	if (commandListThread != null) {
		commandListThread.interrupt();
	}
	commandListThread = new Thread(commandListRunnable);
	commandListThread.start();
}

@Override
public boolean onCreateOptionsMenu(Menu menu) {
	// Inflate the menu; this adds items to the action bar if it is present.
	getMenuInflater().inflate(R.menu.menu_atkpanel, menu);
	return true;
}

private ImageView preparePlotImageView(int scaleID, int plotId) {
	ImageView plotImageView = new ImageView(context);
	plotImageView.setId(plotId);
	// create layout
	RelativeLayout.LayoutParams plotImageViewLayParam = new RelativeLayout.LayoutParams
			(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
	// add layout params
	plotImageViewLayParam.addRule(RelativeLayout.ALIGN_PARENT_TOP);
	plotImageViewLayParam.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
	plotImageViewLayParam.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
	//plotImageViewLayParam.addRule(RelativeLayout.CENTER_HORIZONTAL);
	plotImageViewLayParam.addRule(RelativeLayout.LEFT_OF, scaleID);
	// apply layout to view
	plotImageView.setLayoutParams(plotImageViewLayParam);
	plotImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
	return plotImageView;
}

private ImageView prepareScaleImageView(int maxId, int scaleId) {
	ImageView scaleImageView = new ImageView(context);
	scaleImageView.setId(scaleId);
	// create layout
	RelativeLayout.LayoutParams scaleImageViewLayParam = new RelativeLayout.LayoutParams
			(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
	// add layout params
	scaleImageViewLayParam.addRule(RelativeLayout.ALIGN_PARENT_TOP);
	scaleImageViewLayParam.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
	scaleImageViewLayParam.addRule(RelativeLayout.LEFT_OF, maxId);
	// apply layout to view
	scaleImageView.setLayoutParams(scaleImageViewLayParam);
	scaleImageView.setScaleType(ImageView.ScaleType.FIT_XY);
	return scaleImageView;
}

private TextView prepareTextViewMaxValue(int maxId) {
	TextView textViewMaxValue = new TextView(context);
	textViewMaxValue.setId(maxId);
	// create layout
	RelativeLayout.LayoutParams textViewMaxValueLayParam = new RelativeLayout.LayoutParams
			(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
	// add layout params
	textViewMaxValueLayParam.addRule(RelativeLayout.ALIGN_PARENT_TOP);
	textViewMaxValueLayParam.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
	// apply layout to view
	textViewMaxValue.setLayoutParams(textViewMaxValueLayParam);
	return textViewMaxValue;
}

private TextView prepareTextViewMinValue(int minId) {
	TextView textViewMinValue = new TextView(context);
	textViewMinValue.setId(minId);
	// create layout
	RelativeLayout.LayoutParams textViewMinValueLayParam = new RelativeLayout.LayoutParams
			(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
	// add layout params
	textViewMinValueLayParam.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
	textViewMinValueLayParam.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
	// apply layout to view
	textViewMinValue.setLayoutParams(textViewMinValueLayParam);
	return textViewMinValue;
}

@Override
public boolean onOptionsItemSelected(MenuItem item) {
	int id = item.getItemId();
	if (id == R.id.action_set_refreshing_period) {
		setRefreshingPeriod();
		return true;
	}
	return super.onOptionsItemSelected(item);
}

/**
 * Start new activity for getting from user database host address and port.
 */
private void setDeviceName() {
	Intent i = new Intent(this, SetDeviceActivity.class);
	startActivityForResult(i, 2);
}

/**
 * Prompt user to set new refreshing period
 */
private void setRefreshingPeriod() {
	AlertDialog.Builder alert = new AlertDialog.Builder(this);

	alert.setTitle(getString(R.string.set_new_refreshing_period));

	// Set an EditText view to get user input
	final EditText input = new EditText(this);
	input.setHint(R.string.hint_period_in_ms);
	input.setInputType(InputType.TYPE_CLASS_NUMBER);
	input.setText("" + refreshingPeriod);
	alert.setView(input);

	alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
			String value = input.getText().toString();
			int period = Integer.parseInt(value);
			if (period > 0) {
				refreshingPeriod = period;
				if (statusThread != null) {
					if (statusThread.isAlive()) {
						statusThread.interrupt();
						statusRunnable = new StatusRunnable((ATKPanelCallback) context, deviceName, tangoHost, tangoPort,
								refreshingPeriod);
						statusThread = new Thread(statusRunnable);
						statusThread.start();
					}
				}
				if (attributesThread != null) {
					if (attributesThread.isAlive()) {
						attributesThread.interrupt();
						attributesRunnable = new AttributesRunnable((ATKPanelCallback) context, deviceName, tangoHost, tangoPort,
								request, refreshingPeriod);
						attributesThread = new Thread(attributesRunnable);
						attributesThread.start();
					}
				}
				if (plotThread != null) {
					if (plotThread.isAlive()) {
						plotThread.interrupt();
						plotRunnable = new PlotRunnable((ATKPanelCallback) context, deviceName, tangoHost, tangoPort,
								plotAttributeName, refreshingPeriod);
						plotThread = new Thread(plotRunnable);
						plotThread.start();
					}
				}
			}
		}
	});

	alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) { }
	});

	alert.show();
}

@Override
protected void onDestroy() {
	Log.d("onDestroy()", "Stopping application");
	threadsInterrupted = true;
	if (attributeListThread != null) {
		Log.d("onDestroy()", "attributeListThread is not null");
		Log.d("onDestroy()", "Stopping attributeListThread");
		attributeListThread.interrupt();
	}
	if (attributesThread != null) {
		Log.d("onDestroy()", "attributesThread is not null");
		Log.d("onDestroy()", "Stopping attributesThread");
		attributesThread.interrupt();
	}
	if (plotThread != null) {
		Log.d("onDestroy()", "plotThread is not null");
		Log.d("onDestroy()", "Stopping plotThread");
		plotThread.interrupt();
	}
	if (statusThread != null) {
		Log.d("onDestroy()", "statusThread is not null");
		Log.d("onDestroy()", "Stopping statusThread");
		statusThread.interrupt();
	}
	if (commandListThread != null) {
		Log.d("onDestroy()", "commandListThread is not null");
		Log.d("onDestroy()", "Stopping commandListThread");
		commandListThread.interrupt();
	}
	// this line should be at the end of the method
	super.onDestroy();
}

public boolean areThreadsInterrupted() {
	return threadsInterrupted;
}

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	super.onActivityResult(requestCode, resultCode, data);
	if (requestCode == 1) {
		if (resultCode == RESULT_OK) {
			tangoHost = data.getStringExtra("TangoHost");
			tangoPort = data.getStringExtra("TangoPort");
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString("TangoHost", tangoHost);
			editor.putString("TangoPort", tangoPort);
			editor.commit();
			Log.d("ATK onActivityResult", "Result: " + tangoHost + ":" + tangoPort);
			if (deviceName != null) {
				//populatePanel();
			}
		}
		if (resultCode == RESULT_CANCELED) {
			Log.d("ATK onActivityResult", "Host not changed");
		}
	}
	if (requestCode == 2) {
		if (resultCode == RESULT_OK) {
			deviceName = data.getStringExtra("DEVICE_NAME");
			Log.d("ATK onActivityResult", "Result: " + deviceName);
			setTitle(getString(R.string.title_activity_atkpanel) + " : " + deviceName);
			if (tangoHost != null && tangoPort != null) {
				//populatePanel();
			}
		}
		if (resultCode == RESULT_CANCELED) {
			Log.d("ATK onActivityResult", getString(R.string.atk_panel_dev_not_set));
			setTitle(getString(R.string.title_activity_atkpanel) + " : " + getString(R.string.atk_panel_dev_not_set));
		}
	}
}

/**
 * Start new activity for getting from user database host address and port.
 */
private void setHost() {
	Intent i = new Intent(this, SetHostActivity.class);
	if (tangoHost != null) {
		if (!tangoHost.equals("")) {
			i.putExtra("tangoHost", tangoHost);
		}
	}
	if (tangoPort != null) {
		if (!tangoPort.equals("")) {
			i.putExtra("tangoPort", tangoPort);
		}
	}
	startActivityForResult(i, 1);
}

private void startAttributeRefreshing() {
	// define thread for refreshing attribute values
	request = new HashMap<>();
	numberOfScalars = scalarAttrbuteArray.size();
	request.put("attCount", "" + numberOfScalars);
	Log.d("startAttRef", "Att count: " + numberOfScalars);
	for (int i = 0; i < numberOfScalars; i++) {
		Log.d("startAttRef", "Attribute[" + i + "] name: " + scalarAttrbuteArray.get(i));
		Log.d("startAttRef", "Result: " + deviceName);
		request.put("att" + i, scalarAttrbuteArray.get(i));
		request.put("attID" + i, "" + ids[i][2]);
	}
	// set new thread with Attributes runnable
	attributesRunnable = new AttributesRunnable(this, deviceName, tangoHost, tangoPort, request, refreshingPeriod);
	if (attributesThread != null) {
		attributesThread.interrupt();
	}
	if (plotThread != null) {
		plotThread.interrupt();
	}
	attributesThread = new Thread(attributesRunnable);
	attributesThread.start();
}

public void populateCommandSpinner(String[] commandNames, int[] commandInType, int[] commandOutType, int commandCount) {
	numberOfCommands = commandCount;
	commandNamesArray = commandNames;
	commandInTypesArray = commandInType;
	commandOutTypesArray = commandOutType;
	runOnUiThread(new Runnable() {
		@Override
		public void run() {
			Log.d("populateCommandSpinner", "Populating command spinner");
			commandArray = new ArrayList<>();
			commandInTypeArray = new ArrayList<>();
			commandOutTypeArray = new ArrayList<>();
			for (int i = 0; i < numberOfCommands; i++) {
				commandArray.add(i, commandNamesArray[i]);
				commandInTypeArray.add(i, Tango_CmdArgTypeName[commandInTypesArray[i]]);
				commandOutTypeArray.add(i, Tango_CmdArgTypeName[commandOutTypesArray[i]]);
				Log.v("populateCommandSpinner",
						"Command " + commandArray.get(i) + ", inType: " + commandInTypeArray.get(i) + ", " +
								"outType: " + commandOutTypeArray.get(i));
			}
			ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, commandArray);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			NoSelectionSpinner sItems = (NoSelectionSpinner) findViewById(R.id.atk_panel_command_spinner);
			sItems.setAdapter(adapter);
			sItems.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
					if (firstSelection) {
						Log.d("populateCommandSpinner", "Command spinner first selection, omitting action");
						firstSelection = false;
					} else {
						Log.d("populateCommandSpinner", "Position: " + position);
						final String commandName = commandArray.get(position);
						String commandInType = commandInTypeArray.get(position);
						Log.d("populateCommandSpinner", "Executing " + commandName + ", inType: " + commandInType);
						if (commandInType.equals("DevVoid")) {
							Log.d("populateCommandSpinner", "Command run wth void argument");
							executeCommand(commandName, "DevVoidArgument");
						} else {
							Log.d("populateCommandSpinner", "Prompt for command argument");
							AlertDialog.Builder alert = new AlertDialog.Builder(context);
							alert.setTitle(getString(R.string.command_input));
							// Set an EditText view to get user input
							final EditText input = new EditText(context);
							alert.setMessage(getString(R.string.command_input_type) + commandInType);
							alert.setView(input);
							alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									executeCommand(commandName, input.getText().toString());
								}
							});
							alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) { }
							});
							alert.show();
						}
					}
				}

				@Override
				public void onNothingSelected(AdapterView<?> parentView) {
				}
			});
		}
	});
}

private void executeCommand(String commandName, String arginStr) {
	Log.d("executeCommand()", "Executing command: " + commandName + " with argument: " + arginStr);
	commandExecuteRunnable = new CommandExecuteRunnable(this, deviceName, tangoHost, tangoPort, commandName, arginStr);
	if (commandExecuteThread != null) {
		while (commandExecuteThread.isAlive()) { }
	}
	commandExecuteThread = new Thread(commandExecuteRunnable);
	commandExecuteThread.start();
}

public void commandOutput(String message) {
	commandOut = message;
	runOnUiThread(new Runnable() {
		@Override
		public void run() {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setMessage(commandOut).setTitle("Command output");
			builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
				}
			});
			AlertDialog dialog = builder.create();
			dialog.show();
		}
	});
}

public void toastMessage(String messageToDisplay) {
	message = messageToDisplay;
	runOnUiThread(new Runnable() {
		@Override
		public void run() {
			Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
		}
	});
}

private void populateAttributePlot(String attributeName) {
	Log.d("populateAttributePlot()", "Polulating attribute plot");

	if (attributeListThread != null) {
		Log.d("onDestroy()", "attributeListThread is not null");
		Log.d("onDestroy()", "Stopping attributeListThread");
		attributeListThread.interrupt();
	}
	if (attributesThread != null) {
		Log.d("onDestroy()", "attributesThread is not null");
		Log.d("onDestroy()", "Stopping attributesThread");
		attributesThread.interrupt();
	}
	if (plotThread != null) {
		Log.d("onDestroy()", "plotThread is not null");
		Log.d("onDestroy()", "Stopping plotThread");
		plotThread.interrupt();
	}
	plotAttributeName = attributeName;
	plotRunnable = new PlotRunnable(this, deviceName, tangoHost, tangoPort, plotAttributeName, refreshingPeriod);
	plotThread = new Thread(plotRunnable);
	plotThread.start();
}

private void populateScalarListView() {
	if (!scalarAttrbuteArray.isEmpty()) {

		runOnUiThread(new Runnable() {
			@Override
			public void run() {

				Log.d("populateScalarListView", "Getting RelativeLayout");
				RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.atkPanel_innerLayout);

				relativeLayout.removeAllViews();
				numberOfScalars = scalarAttrbuteArray.size();
				ids = new int[numberOfScalars][5];

				for (int i = 0; i < numberOfScalars; i++) {
					RelativeLayout.LayoutParams textViewLayParam = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams
							.WRAP_CONTENT,
							RelativeLayout.LayoutParams.WRAP_CONTENT);
					RelativeLayout.LayoutParams editTextLayParam = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams
							.WRAP_CONTENT,
							RelativeLayout.LayoutParams.WRAP_CONTENT);
					RelativeLayout.LayoutParams buttonLayParam = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams
							.WRAP_CONTENT,
							RelativeLayout.LayoutParams.WRAP_CONTENT);

					TextView tv = new TextView(context);
					ids[i][1] = generateViewId();
					tv.setId(ids[i][1]);

					EditText et = new EditText(context);
					ids[i][2] = generateViewId();
					et.setId(ids[i][2]);

					Button b = new Button(context);
					ids[i][3] = generateViewId();
					b.setId(ids[i][3]);

					if (i == 0) {
						editTextLayParam.addRule(RelativeLayout.ALIGN_PARENT_TOP);

					} else {
						editTextLayParam.addRule(RelativeLayout.BELOW, ids[i - 1][2]);
					}

					editTextLayParam.addRule(RelativeLayout.CENTER_HORIZONTAL);

					buttonLayParam.addRule(RelativeLayout.ALIGN_BASELINE, ids[i][1]);
					buttonLayParam.addRule(RelativeLayout.RIGHT_OF, ids[i][2]);

					textViewLayParam.addRule(RelativeLayout.ALIGN_BASELINE, ids[i][2]);
					textViewLayParam.addRule(RelativeLayout.LEFT_OF, ids[i][2]);

					tv.setText(scalarAttrbuteArray.get(i));
					tv.setLayoutParams(textViewLayParam);

					et.setLayoutParams(editTextLayParam);
					et.setFocusable(false);
					et.setWidth(300);

					b.setLayoutParams(buttonLayParam);
					b.setText("Edit");
					if (attributeWritableArray[i]) {
						b.setVisibility(View.VISIBLE);
						b.setTag(scalarAttrbuteArray.get(i));
						b.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								//int rowId = (int) v.getTag(1);
								final String attName = (String) v.getTag();
								Log.d("populateCommandSpinner", "Prompt for attribute new value");
								AlertDialog.Builder alert = new AlertDialog.Builder(context);
								alert.setTitle(getString(R.string.attribute_new_value));
								alert.setMessage(getString(R.string.new_value_for_att) + attName);

								// Set an EditText view to get user input
								final EditText input = new EditText(context);

								alert.setView(input);
								alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
										updateAttribute(attName, input.getText().toString());
									}
								});

								alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) { }
								});

								alert.show();

							}
						});
					} else {
						b.setVisibility(View.INVISIBLE);
					}
					relativeLayout.addView(tv);
					relativeLayout.addView(et);
					relativeLayout.addView(b);
				}
				startAttributeRefreshing();
			}
		});
	}
}

private void updateAttribute(String attName, String argin) {
	AttributeUpdateRunnable attributeUpdateRunnable = new AttributeUpdateRunnable(this, deviceName, tangoHost,
			tangoPort, attName, argin);
	if (attributeUpdateThread != null) {
		if (attributeUpdateThread.isAlive()) {
			while (attributeUpdateThread.isAlive()) {}
		}
	}
	attributeUpdateThread = new Thread(attributeUpdateRunnable);
	attributeUpdateThread.start();
}

/**
 * Method updating values of scalar attributes.
 *
 * @param response HashMap containing attribute IDs and values
 */
public void updateScalarListView(HashMap<String, String> response) {
	hmResponse = response;
	//Log.d("updateScalarListView()", "Updating scalar list view");
	//Log.d("updateScalarListView()", "Received response: " + response.toString());
	runOnUiThread(new Runnable() {
		@Override
		public void run() {
			EditText editText;
			int attCount = Integer.parseInt(hmResponse.get("attCount"));
			//Log.d("updateScalarListView()", "Received " + attCount + " attributes");
			for (int i = 0; i < attCount; i++) {
				//Log.d("updateScalarListView()",
				//		"Updating attribute[ " + i + "]" + hmResponse.get("attID" + i) + "with value: " + hmResponse.get
				//				("attValue" + i));
				editText = (EditText) findViewById(Integer.parseInt(hmResponse.get("attID" + i)));
				//Log.d("updateScalarListView()", "EditText ID: " + editText.getId());
				String attValue = hmResponse.get("attValue" + i);
				//Log.d("updateScalarListView()", "attValue: " + attValue);
				editText.setText(attValue);
				//Log.d("updateScalarListView()", "Updated editText[" + editText.getId() + "] with value: " + attValue);
			}
		}
	});
}

public void displayErrorMessage(final String message, DevFailed e) {
	// print error to LogCat
	Log.d("displayErrorMessage()", "Error occured!");
	e.printStackTrace();

	// show dialog box with error message
	AlertDialog.Builder builder = new AlertDialog.Builder(context);
	builder.setMessage(e.toString()).setTitle("Error!").setPositiveButton(getString(R.string.ok_button),
			null);
	AlertDialog dialog = builder.create();
	dialog.show();
}

public void displayErrorMessage(final String message) {
	// print error to LogCat
	Log.d("displayErrorMessage()", "Error occurred!");
	Log.d("displayErrorMessage()", message);

	// show dialog box with error message
	AlertDialog.Builder builder = new AlertDialog.Builder(context);
	builder.setMessage(message).setTitle("Error!").setPositiveButton(getString(R.string.ok_button),
			null);
	AlertDialog dialog = builder.create();
	dialog.show();
}

public void populateStatus(String status) {
	deviceStatus = status;
	runOnUiThread(new Runnable() {
		@Override
		public void run() {
			TextView statusTextView = (TextView) findViewById(R.id.atk_panel_status_text_view);
			statusTextView.setText(deviceStatus);
		}
	});

}

public void populateAttributeSpinner(HashMap<String, String> response) {
	Log.d("popAttributeSpinner", "Populating attribute spinner");
	scalarAttrbuteArray = new ArrayList<>();
	nonScalarAttributeArray = new ArrayList<>();

	int attributeCount = Integer.parseInt(response.get("attCount"));
	attributeWritableArray = new boolean[attributeCount];
	for (int i = 0; i < attributeCount; i++) {
		attributeName = response.get("attribute" + i);
		if (Boolean.parseBoolean(response.get("attScalar" + i))) {
			if (!attributeName.equals("State") && !attributeName.equals("Status")) {
				scalarAttrbuteArray.add(attributeName);
				attributeWritableArray[i] = Boolean.parseBoolean(response.get("attWritable" + i));
			}
		} else {
			nonScalarAttributeArray.add(attributeName);
		}
	}
	if (!nonScalarAttributeArray.isEmpty()) {
		nonScalarAttributeArray.add(0, "Scalar");

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				ArrayAdapter<String> adapter = new ArrayAdapter<>(
						context, android.R.layout.simple_spinner_item, nonScalarAttributeArray);
				adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				Spinner attributeSpinner = (Spinner) findViewById(R.id.atk_panel_attribute_spinner);
				attributeSpinner.setAdapter(adapter);
				attributeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
						String attName = nonScalarAttributeArray.get(position);
						if (attName.equals("Scalar")) {
							populateScalarListView();
						} else {
							populateAttributePlot(attName);
						}
					}

					@Override
					public void onNothingSelected(AdapterView<?> parentView) {
					}
				});
			}
		});

	} else {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Spinner attributeSpinner = (Spinner) findViewById(R.id.atk_panel_attribute_spinner);
				attributeSpinner.setVisibility(View.INVISIBLE);

			}
		});
		populateScalarListView();
	}

}

public void updatePlotView(Number[] series, String label) {
	Log.d("rAttributePlot.run()", "Device connection OK");
	plotSeries = series;
	plotLabel = label;

	runOnUiThread(new Runnable() {
		@Override
		public void run() {
			plot = new XYPlot(context, "");
			plotHeight = scrollView.getHeight();
			plot.setMinimumHeight(plotHeight);
			Log.d("populateAttributePlot()", "Plot height set to: " + plotHeight);
			relativeLayout.removeAllViews();
			relativeLayout.addView(plot);
			Log.v("rAttributePlot.run()", "Have " + plotSeries.length + " elements");
			XYSeries series1;
			series1 = new SimpleXYSeries(Arrays.asList(plotSeries), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, plotLabel);
			LineAndPointFormatter series1Format = new LineAndPointFormatter();
			series1Format.setPointLabelFormatter(new PointLabelFormatter());
			series1Format.configure(getApplicationContext(), R.xml.line_point_formatter_with_plf1);
			// add a new series' to the xyplot:
			plot.addSeries(series1, series1Format);
			plot.setTicksPerRangeLabel(3);
			plot.getGraphWidget().setDomainLabelOrientation(-45);
			plot.getLegendWidget().setTableModel(new DynamicTableModel(1, 1));
			plot.getLegendWidget().position(10, XLayoutStyle.ABSOLUTE_FROM_LEFT, 10, YLayoutStyle.ABSOLUTE_FROM_BOTTOM,
					AnchorPosition.LEFT_BOTTOM);
			plot.getLegendWidget().setSize(new SizeMetrics(55, SizeLayoutType.ABSOLUTE, 100, SizeLayoutType.FILL));
		}
	});
}

public void updatePlotView(double[][] plotValues) {
	doublePlotSeries = plotValues;

	runOnUiThread(new Runnable() {
		@Override
		public void run() {
			TextView textViewMaxValue = prepareTextViewMaxValue(maxId);
			TextView textViewMinValue = prepareTextViewMinValue(minId);
			ImageView imageViewScale = prepareScaleImageView(maxId, scaleId);
			ImageView imageViewPlot = preparePlotImageView(scaleId, plotId);
			plotHeight = scrollView.getHeight();
			plotWidth = scrollView.getWidth() - 50;
			if (plotHeight < plotWidth) {
				imageViewPlot.setMinimumHeight(plotHeight);
				imageViewPlot.setMinimumWidth(plotHeight);
			} else {
				imageViewPlot.setMinimumHeight(plotWidth);
				imageViewPlot.setMinimumWidth(plotWidth);
			}

			imageViewScale.setMinimumWidth(20);
			imageViewScale.setMinimumHeight(plotHeight);
			Log.d("rAttributePlot.run() IM", "Plot height: " + plotHeight);
			imageViewScale.setScaleType(ImageView.ScaleType.FIT_XY);

			relativeLayout.removeAllViews();
			relativeLayout.addView(textViewMaxValue);
			relativeLayout.addView(textViewMinValue);
			relativeLayout.addView(imageViewPlot);
			relativeLayout.addView(imageViewScale);

			Bitmap b = Bitmap.createBitmap(doublePlotSeries.length, doublePlotSeries[0].length,
					Bitmap.Config.RGB_565);
			double minValue = doublePlotSeries[0][0];
			double maxValue = doublePlotSeries[0][0];
			for (int i = 0; i < doublePlotSeries.length; i++) {
				for (int j = 0; j < doublePlotSeries[0].length; j++) {
					if (minValue > doublePlotSeries[i][j]) {
						minValue = doublePlotSeries[i][j];
					}
					if (maxValue < doublePlotSeries[i][j]) {
						maxValue = doublePlotSeries[i][j];
					}
				}
			}
			System.out.println("Min: " + minValue + "Max: " + maxValue);
			double range = maxValue - minValue;
			float step = (float) (330 / range);

			int color;
			float hsv[] = {0, 1, 1};

			for (int i = 1; i < doublePlotSeries.length; i++) {
				for (int j = 1; j < doublePlotSeries[0].length; j++) {
					hsv[0] = 330 - (float) (doublePlotSeries[i][j] * step);
					color = Color.HSVToColor(hsv);
					b.setPixel(doublePlotSeries.length - i, doublePlotSeries[0].length - j, color);
				}
			}
			imageViewPlot.setImageBitmap(b);
			textViewMaxValue.setText("" + maxValue);
			textViewMinValue.setText("" + minValue);
			Bitmap scaleBitmap = Bitmap.createBitmap(20, 330, Bitmap.Config.RGB_565);
			for (int j = 0; j < 330; j++) {
				hsv[0] = j;
				color = Color.HSVToColor(hsv);
				for (int k = 0; k < 20; k++) {
					scaleBitmap.setPixel(k, j, color);
				}
			}
			imageViewScale.setImageBitmap(scaleBitmap);
			imageViewScale.setScaleType(ImageView.ScaleType.FIT_XY);
		}
	});
}
}