package pl.edu.uj.synchrotron.jive;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.DbDatum;
import fr.esrf.TangoApi.DeviceProxy;

/**
 * A class for creating properties activity screen.
 */
public class PropertiesActivity extends Activity {
Intent intent;
String deviceName;
String dbHost;
String dbPort;

@Override
protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_properties);
	intent = getIntent();
	deviceName = intent.getStringExtra("deviceName");
	dbHost = intent.getStringExtra("dbHost");
	dbPort = intent.getStringExtra("dbPort");

	refreshPropertiesList(deviceName, dbHost, dbPort);
}

@Override
public boolean onCreateOptionsMenu(Menu menu) {
	// Inflate the menu; this adds items to the action bar if it is present.
	getMenuInflater().inflate(R.menu.menu_properties, menu);
	return true;
}

/**
 * Refresh already shown list of properties.
 *
 * @param name Name of device, which properties should be listed.
 * @param host Database host address.
 * @param port Database port.
 */
private void refreshPropertiesList(String name, String host, String port) {
	try {
		System.out.println("PropertiesActivity output:");
		System.out.println("Device name: " + name);
		System.out.println("Database host: " + host);
		System.out.println("Database port: " + host);
		DeviceProxy dp = new DeviceProxy(name, host, port);

		LinearLayout layout = (LinearLayout) findViewById(R.id.properties_activity_linear_layout);
		layout.removeAllViews();

		String[] prop_list = dp.get_property_list("*");
		final LayoutInflater inflater = LayoutInflater.from(this);
		for (int i = 0; i < prop_list.length; i++) {
			View view = inflater.inflate(R.layout.editable_list_element, null);
			EditText et = (EditText) view.findViewById(R.id.editableListEditText);
			TextView tv = (TextView) view.findViewById(R.id.editableListTextView);
			tv.setText(prop_list[i]);
			DbDatum dbd = dp.get_property(prop_list[i]);
			et.setText(dbd.extractString());
			et.setTag(prop_list[i]);
			layout.addView(view);
		}
	} catch (DevFailed e) {
		e.printStackTrace();
	}
}

/**
 * Listener for the button click, refresh list of properties.
 *
 * @param view Reference to the widget that was clicked.
 */
public void propertiesListRefreshButton(View view) {
	refreshPropertiesList(deviceName, dbHost, dbPort);
}

/**
 * Listener for the button click, close the activity.
 *
 * @param view Reference to the widget that was clicked.
 */
public void devicePropertiesCancelButton(View view) {
	finish();
}

/**
 * Listener for the button click, update the selected property.
 *
 * @param view Reference to the widget that was clicked.
 */
public void devicePropertiesUpdateButton(View view) {
	LinearLayout linearLayout = (LinearLayout) findViewById(R.id.properties_activity_linear_layout);
	int childCount = linearLayout.getChildCount();
	for (int i = 0; i < childCount; i++) {
		View linearLayoutView = linearLayout.getChildAt(i);
		EditText et = (EditText) linearLayoutView.findViewById(R.id.editableListEditText);
		String value = et.getText().toString();
		String tag = (String) et.getTag();
		try {
			DeviceProxy dp = new DeviceProxy(deviceName, dbHost, dbPort);
			DbDatum dbd = new DbDatum(tag, value);
			dp.put_property(dbd);
		} catch (DevFailed e) {
			e.printStackTrace();
		}
	}
}
}
