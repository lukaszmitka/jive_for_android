package pl.edu.uj.synchrotron.jive;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

/**
 * A class for creating device panel activity screen.
 */
public class DevicePanelActivity extends FragmentActivity implements ActionBar.TabListener {
/**
 * Name of preferences file to store application settings.
 */
public static final String PREFS_NAME = "SolarisDeviceListPrefsFile";
/**
 * Layout manager to flip left and right pages.
 */
private ViewPager viewPager;
/**
 * Adapter for controlling tabs.
 */
private DevicePanelTabsPagerAdapter mAdapter;
/**
 * Action bar of the activity
 */
private ActionBar actionBar;
/**
 * Name of device, which attributes should be listed.
 */
private String deviceName;
/**
 * Default host address, used if user didn't specify other.
 */
private String databaseHost = new String("192.168.100.120");
/**
 * Default port, used if user didn't specify other.
 */
private String databasePort = new String("10000");
/**
 * Database host address.
 */
private String dbHost;
/**
 * Database port.
 */
private String dbPort;

@Override
protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_device_panel);
	// this allows to connect with server in main thread
	StrictMode.ThreadPolicy old = StrictMode.getThreadPolicy();
	StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder(old).permitNetwork().build());

	Intent i = getIntent();
	if (i.hasExtra("devName")) {
		deviceName = i.getStringExtra("devName");
	} else {
		deviceName = new String("sys/tg_test/1");
	}

	// Tab titles
	String[] tabs = {this.getString(R.string.device_panel_commands),
			this.getString(R.string.device_panel_attributes), this.getString(R.string.device_panel_admin)};

	// Initilization
	viewPager = (ViewPager) findViewById(R.id.device_panel_pager);
	actionBar = getActionBar();
	mAdapter = new DevicePanelTabsPagerAdapter(getSupportFragmentManager());

	viewPager.setAdapter(mAdapter);
	actionBar.setHomeButtonEnabled(false);
	actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

	// Adding Tabs
	for (String tab_name : tabs) {
		actionBar.addTab(actionBar.newTab().setText(tab_name).setTabListener(this));
		/**
		 * on swiping the viewpager make respective tab selected
		 * */
		viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

			@Override
			public void onPageSelected(int position) {
				// on changing the page
				// make respected tab selected
				actionBar.setSelectedNavigationItem(position);
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
			}
		});
	}
	SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
	dbHost = settings.getString("dbHost", "");
	dbPort = settings.getString("dbPort", "");
	if (dbHost.equals("")) {
		dbHost = databaseHost;
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("dbHost", databaseHost);
		editor.commit();
	}
	if (dbPort.equals("")) {
		dbPort = databasePort;
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("dbPort", databasePort);
		editor.commit();
	}
}

/**
 * Get name of already processed device.
 *
 * @return Name of the device.
 */
public String getDeviceName() {
	return deviceName;
}

/**
 * Get address of already connected database.
 *
 * @return Database host.
 */
public String getHost() {
	return dbHost;
}

/**
 * Get port of already connected database.
 *
 * @return Database port.
 */
public String getPort() {
	return dbPort;
}

@Override
public boolean onCreateOptionsMenu(Menu menu) {
	// Inflate the menu; this adds items to the action bar if it is present.
	getMenuInflater().inflate(R.menu.menu_device_panel, menu);
	return true;
}

@Override
public boolean onOptionsItemSelected(MenuItem item) {
	// Handle action bar item clicks here. The action bar will
	// automatically handle clicks on the Home/Up button, so long
	// as you specify a parent activity in AndroidManifest.xml.
	int id = item.getItemId();
	return super.onOptionsItemSelected(item);
}

@Override
public void onTabReselected(Tab tab, FragmentTransaction ft) {
}

@Override
public void onTabSelected(Tab tab, FragmentTransaction ft) {
	viewPager.setCurrentItem(tab.getPosition());
}

@Override
public void onTabUnselected(Tab tab, FragmentTransaction ft) {

}
}
