package pl.edu.uj.synchrotron.jive;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.Database;
import fr.esrf.TangoApi.DeviceInfo;

/**
 * Activity for listing devices in three level sorted list.
 */
public class SortedList extends Activity {
    public static final String PREFS_NAME = "SolarisDeviceListPrefsFile";
    final Context context = this;
    List<NLevelItem> list;
    ListView listView;
    private String devDomain;
    private String devClass;
    private String devMember;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // default host and port, used if user didn't specify other
        String databaseHost = new String("192.168.101.129");
        String databasePort = new String("10000");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sorted_list);

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
        refreshDeviceList(dbHost, dbPort);
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
                showFullList();
                return true;
            case R.id.action_server_list:
                showServerList();
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

    /**
     * Start new activity with full, unsorted list of devices.
     */
    private void showFullList() {
        Intent i = new Intent(this, MainActivity.class);
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
                String databaseHost = new String();
                String databasePort = new String();
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
     * Get list of devices from databse, and sort them accordingly to their
     * domain and class.
     *
     * @param host Database host address.
     * @param port Database port.
     * @return TreeMap with sorted list.
     */
    private TreeMap<String, DevClassList> getSortedList(String host, String port) {
        try {
            System.out.println("Connecting to db at: " + host + ":" + port);
            Database db = new Database(host, port);
            // System.out.println("Pobieram listê urz¹dzeñ");
            String list[] = db.get_device_list("*");
            // System.out.println(list.toString());

            int i = list.length;
            // System.out.println("Wykryto " + i + " urzadzen");
            String devDomain = new String("");
            String devClass = new String("");

            TreeMap<String, DevClassList> domains = new TreeMap<String, DevClassList>(new AlphabeticComparator());
            int j = 0;
            String[] splitted = new String[3];
            while (j < i) {
                splitted = list[j].split("/");
                devDomain = splitted[0];
                DevClassList dcl = new DevClassList(devDomain);
                System.out.println("Petla 1 :" + devDomain + "  " + splitted[0]);

                while (devDomain.equals(splitted[0]) && (j < i)) {

                    splitted = list[j].split("/");
                    devClass = splitted[1];
                    System.out.println("    Petla 2 :" + devClass + "  " + splitted[1]);
                    ArrayList<String> members = new ArrayList<String>();
                    while (devClass.equals(splitted[1]) && (j < i) && devDomain.equals(splitted[0])) {
                        System.out.println("      Petla 3 :" + splitted[2]);

                        members.add(splitted[2]);
                        j++;
                        if (j < i) {
                            splitted = list[j].split("/");
                        } else {
                            break;
                        }
                    }
                    Collections.sort(members, new AlphabeticComparator());
                    dcl.addToMap(devClass, members);
                }
                domains.put(devDomain, dcl);
            }
            // System.out.println("Zakonczono listowanie urzadzen");

            return domains;
        } catch (DevFailed e) {
            e.printStackTrace();
        }
        return null;
    }

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
    public void sortedListRefresh(View v) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String dbHost = settings.getString("dbHost", "");
        String dbPort = settings.getString("dbPort", "");
        System.out.println("Refreshing list at: " + dbHost + ":" + dbPort);
        refreshDeviceList(dbHost, dbPort);
    }

    /**
     * Refresh currently shown list of devices.
     *
     * @param host Database host address.
     * @param port Database port.
     */
    private void refreshDeviceList(String host, String port) {
        listView = (ListView) findViewById(R.id.listView2);
        list = new ArrayList<NLevelItem>();
        final LayoutInflater inflater = LayoutInflater.from(this);
        TreeMap<String, DevClassList> devCL = new TreeMap<String, DevClassList>();

        System.out.println("Connecting to: " + host + ":" + port);
        devCL = getSortedList(host, port);
        if (devCL != null) {

            String[] s = new String[1];
            String[] domainList = devCL.keySet().toArray(s);

            for (String searchDomain : domainList) {
                devDomain = searchDomain;
                if (devCL.containsKey(searchDomain)) {
                    DevClassList dclRet = devCL.get(searchDomain);
                    String[] classList = dclRet.getClassSet();

                    final NLevelItem grandParent = new NLevelItem(new SomeObject(searchDomain, ""), null, new NLevelView() {

                        public View getView(NLevelItem item) {
                            View view = inflater.inflate(R.layout.n_level_list_item_lev_1, null);
                            TextView tv = (TextView) view.findViewById(R.id.nLevelList_item_L1_textView);
                            String name = (String) ((SomeObject) item.getWrappedObject()).getName();
                            tv.setText(name);
                            return view;
                        }
                    });

                    list.add(grandParent);

                    for (String sClass : classList) {
                        devClass = sClass;
                        ArrayList<String> classMembers = dclRet.getClass(sClass);

                        NLevelItem parent = new NLevelItem(new SomeObject(sClass, ""), grandParent, new NLevelView() {

                            public View getView(NLevelItem item) {
                                View view = inflater.inflate(R.layout.n_level_list_item_lev_2, null);
                                TextView tv = (TextView) view.findViewById(R.id.nLevelList_item_L2_textView);
                                String name = (String) ((SomeObject) item.getWrappedObject()).getName();
                                tv.setText(name);
                                return view;
                            }
                        });

                        list.add(parent);

                        for (String cMember : classMembers) {
                            devMember = cMember;
                            NLevelItem child = new NLevelItem(new SomeObject(cMember, searchDomain + "/" + sClass + "/"
                                    + cMember), parent, new NLevelView() {

                                public View getView(NLevelItem item) {
                                    View view = inflater.inflate(R.layout.n_level_list_member_item, null);
                                    Button b = (Button) view.findViewById(R.id.nLevelList_member_button);
                                    b.setTag((String) ((SomeObject) item.getWrappedObject()).getTag());
                                    TextView tv = (TextView) view.findViewById(R.id.nLevelList_member_textView);
                                    tv.setClickable(true);
                                    String name = (String) ((SomeObject) item.getWrappedObject()).getName();
                                    tv.setText(name);
                                    tv.setTag((String) ((SomeObject) item.getWrappedObject()).getTag());
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
                                                        Toast toast = Toast.makeText(context, "This should run ATKPanel",
                                                                Toast.LENGTH_LONG);
                                                        toast.show();
                                                    }
                                                    if (choice == 1) {
                                                        Intent i = new Intent(context, DevicePanelActivity.class);
                                                        i.putExtra("devName", name);
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
                                    properties.setTag((String) ((SomeObject) item.getWrappedObject()).getTag());
                                    Button attributes = (Button) view.findViewById(R.id.nLevelList_member_attributes);
                                    attributes.setTag((String) ((SomeObject) item.getWrappedObject()).getTag());
                                    return view;
                                }
                            });
                            list.add(child);
                        }
                    }
                }
            }
            NLevelAdapter adapter = new NLevelAdapter(list);
            listView.setAdapter(adapter);
            listView.setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                    ((NLevelAdapter) listView.getAdapter()).toggle(arg2);
                    ((NLevelAdapter) listView.getAdapter()).getFilter().filter();
                }
            });
        }
    }

    /**
     * Class for storing name and tag of list element.
     */
    class SomeObject {
        public String name;
        public String tag;

        /**
         * @param name Name of the object.
         * @param tag  Tag of the object.
         */
        public SomeObject(String name, String tag) {
            this.name = name;
            this.tag = tag;
        }

        /**
         * Get name of the object.
         *
         * @return Name of the object.
         */
        public String getName() {
            return name;
        }

        /**
         * Get tag of the object.
         *
         * @return Tag of the object.
         */
        public String getTag() {
            return tag;
        }
    }
}

/**
 * Class for storing class list.
 */
class DevClassList {
    /**
     * In this TreeMap are stored device classes with devices of their type.
     * Class names are stores as key, and devices are stored as value.
     */
    private TreeMap<String, ArrayList<String>> deviceClassList;

    public DevClassList(String domainName) {
        deviceClassList = new TreeMap<String, ArrayList<String>>(new AlphabeticComparator());
    }

    /**
     * Add class with its devices to map.
     *
     * @param key Name of the class.
     * @param dc  Device list.
     */
    public void addToMap(String key, ArrayList<String> dc) {
        deviceClassList.put(key, dc);
    }

    /**
     * Get list of stored classes.
     *
     * @return String containing names of classes comma separated.
     */
    public String getClassList() {
        return deviceClassList.keySet().toString();
    }

    /**
     * Get list of stored classes.
     *
     * @return Array of strings containing names of classes.
     */
    public String[] getClassSet() {
        String[] s = new String[1];
        return deviceClassList.keySet().toArray(s);
    }

    /**
     * Get list of devices of selected class.
     *
     * @param key Name of selected class.
     * @return List of devices.
     */
    public ArrayList<String> getClass(String key) {
        if (deviceClassList.containsKey(key)) {
            return deviceClassList.get(key);
        }
        return null;
    }
}

/**
 * Class comparing elements, used to sort alphapetically.
 */
class AlphabeticComparator implements Comparator<String> {
    @Override
    public int compare(String e1, String e2) {
        return e1.toLowerCase().compareTo(e2.toLowerCase());
    }
}