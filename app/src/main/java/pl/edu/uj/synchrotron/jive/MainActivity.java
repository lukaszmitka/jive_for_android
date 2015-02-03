package pl.edu.uj.synchrotron.jive;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.ApiUtil;
import fr.esrf.TangoApi.Database;

public class MainActivity extends Activity {
    public static final String PREFS_NAME = "SolarisDeviceListPrefsFile";
    private String devices[];
    // default host and port, used if user didn't specify other
    private String databaseHost = new String("192.168.101.129");
    private String databasePort = new String("10000");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String dbHost = settings.getString("dbHost", "");
        String dbPort = settings.getString("dbPort", "");
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
        // this allows to connect with server in main thread
        StrictMode.ThreadPolicy old = StrictMode.getThreadPolicy();
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder(old).permitNetwork().build());

        refreshDeviceList(dbHost, dbPort);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_set_host:
                setHost();
                return true;
            case R.id.action_sorted_list:
                showSortedList();
                return true;
            case R.id.action_server_list:
                showServerList();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Listener for "Refresh" button, refreshes list of devices.
     *
     * @param view Reference to the widget that was clicked.
     */
    public void button1_OnClick(View view) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String dbHost = settings.getString("dbHost", null);
        String dbPort = settings.getString("dbPort", null);
        refreshDeviceList(dbHost, dbPort);
    }

    /**
     * Start new activity for getting from user database host address and port.
     */
    private void setHost() {
        Intent i = new Intent(this, SetHostActivity.class);
        startActivityForResult(i, 1);
    }

    /**
     * Start new activity with sorted list of devices.
     */
    private void showSortedList() {
        Intent i = new Intent(this, SortedList.class);
        i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(i);
    }

    /**
     * Start new activity with device server list.
     */
    private void showServerList() {
        Intent i = new Intent(this, ServerList.class);
        i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(i);
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
            }
            if (resultCode == RESULT_CANCELED) {
                System.out.println("Host not changed");
            }
        }
    }

    /**
     * Refresh currently shown list of devices.
     *
     * @param host Host address of database.
     * @param port Database port.
     */
    private void refreshDeviceList(String host, String port) {
        try {
            System.out.println("Lacze z baza danych");
            Database db = ApiUtil.get_db_obj(host, port);
            System.out.println("Pobieram dane z bazy");
            devices = db.get_device_list("*");
            ListView deviceList = (ListView) findViewById(R.id.listView1);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.list_item, R.id.firstLine, devices);
            deviceList.setAdapter(adapter);
            deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                    TextView selectedDeviceName = (TextView) findViewById(R.id.deviceName);
                    final String item = (String) parent.getItemAtPosition(position);
                    selectedDeviceName.setText(item);
                }
            });
        } catch (DevFailed e) {
            e.printStackTrace();
        }
    }
}