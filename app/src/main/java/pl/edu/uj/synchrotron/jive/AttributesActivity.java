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

import fr.esrf.Tango.AttrDataFormat;
import fr.esrf.Tango.AttrWriteType;
import fr.esrf.Tango.DevEncoded;
import fr.esrf.Tango.DevFailed;
import fr.esrf.Tango.DevState;
import fr.esrf.TangoApi.AttributeInfo;
import fr.esrf.TangoApi.DeviceAttribute;
import fr.esrf.TangoApi.DeviceProxy;
import fr.esrf.TangoDs.TangoConst;

/**
 * A class for creating attributes activity screen.
 */
public class AttributesActivity extends Activity implements TangoConst {
    /**
     * The intent that activity was called with.
     */
    Intent intent;
    /**
     * Name of device, which attributes should be listed.
     */
    String deviceName;
    /**
     * Database host address.
     */
    String dbHost;
    /**
     * Database port.
     */
    String dbPort;
    DeviceProxy dp;

    /**
     * Check whether attribute could be read, written or both.
     *
     * @param ai Attribute to be checked.
     * @return String defining write permission.
     */
    static String getWriteString(AttributeInfo ai) {
        switch (ai.writable.value()) {
            case AttrWriteType._READ:
                return "READ";
            case AttrWriteType._READ_WITH_WRITE:
                return "READ_WITH_WRITE";
            case AttrWriteType._READ_WRITE:
                return "READ_WRITE";
            case AttrWriteType._WRITE:
                return "WRITE";
        }
        return "Unknown";
    }

    /**
     * Check whether attribute could be presented as scalar, spectrum or image.
     *
     * @param ai Attribute to be checked.
     * @return String defining presentation format.
     */
    static String getFormatString(AttributeInfo ai) {
        switch (ai.data_format.value()) {
            case AttrDataFormat._SCALAR:
                return "Scalar";
            case AttrDataFormat._SPECTRUM:
                return "Spectrum";
            case AttrDataFormat._IMAGE:
                return "Image";
        }
        return "Unknown";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attributes);
        intent = getIntent();
        deviceName = intent.getStringExtra("deviceName");
        dbHost = intent.getStringExtra("dbHost");
        dbPort = intent.getStringExtra("dbPort");

        refreshAttributesList(deviceName, dbHost, dbPort);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_attributes, menu);
        return true;
    }

    /**
     * Refresh already shown list of attributes.
     *
     * @param deviceName Name of device, which attributes should be listed.
     * @param dbHost     Database host address.
     * @param dbPort     Database port.
     */
    private void refreshAttributesList(String deviceName, String dbHost, String dbPort) {
        try {
            System.out.println("AttributesActivity output:");
            System.out.println("Device name: " + deviceName);
            System.out.println("Device host: " + dbHost);
            dp = new DeviceProxy(deviceName, dbHost, dbPort);
            LinearLayout layout = (LinearLayout) findViewById(R.id.attributesActivityLinearLayout);
            layout.removeAllViews();
            String[] attr_list = dp.get_attribute_list();
            final LayoutInflater inflater = LayoutInflater.from(this);
            for (int i = 0; i < attr_list.length; i++) {
                View view = inflater.inflate(R.layout.editable_list_element, null);
                EditText et = (EditText) view.findViewById(R.id.editableListEditText);
                TextView tv = (TextView) view.findViewById(R.id.editableListTextView);
                tv.setText(attr_list[i]);
                DeviceAttribute da = dp.read_attribute(attr_list[i]);
                String s = extractData(da, dp.get_attribute_info(attr_list[i]));
                et.setText(s);
                et.setTag(attr_list[i]);
                if (isWritable(dp.get_attribute_info(attr_list[i]))) {
                    et.setEnabled(true);
                    et.setFocusable(true);
                } else {
                    et.setFocusable(false);
                    et.setEnabled(false);
                }
                ;
                layout.addView(view);
            }
        } catch (DevFailed e) {
            e.printStackTrace();
        }
    }

    /**
     * Listener for the button click, refresh list of activities.
     *
     * @param view Reference to the widget that was clicked.
     */
    public void attributesActivityListRefreshButton(View view) {
        refreshAttributesList(deviceName, dbHost, dbPort);
    }

    /**
     * Listener for the button click, close the activity.
     *
     * @param view Reference to the widget that was clicked.
     */
    public void attributesActivityCancelButton(View view) {
        finish();
    }

    /**
     * Listener for the button click, update the selected attribute.
     *
     * @param view Reference to the widget that was clicked.
     */
    public void attributesActivityUpdateButton(View view) {
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.attributesActivityLinearLayout);
        int childCount = linearLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {

            View linearLayoutView = linearLayout.getChildAt(i);
            EditText et = (EditText) linearLayoutView.findViewById(R.id.editableListEditText);
            if (et.isEnabled()) {
                String value = et.getText().toString();
                String tag = (String) et.getTag();
                try {
                    DeviceProxy dp = new DeviceProxy(deviceName, dbHost, dbPort);
                    DeviceAttribute da = dp.read_attribute(tag);
                    insertData(value, da, dp.get_attribute_info(tag));
                    dp.write_attribute(da);
                } catch (DevFailed e) {
                    e.printStackTrace();
                }
            }

        }
    }

    // -----------------------------------------------------
    // Private stuff
    // -----------------------------------------------------

    /**
     * Check if attribute could be plotted.
     *
     * @param ai Attribute to be checked.
     * @return True when attribute can be plotted.
     */
    private boolean isPlotable(AttributeInfo ai) {
        if ((ai.data_type == Tango_DEV_STRING) || (ai.data_type == Tango_DEV_STATE) || (ai.data_type == Tango_DEV_BOOLEAN))
            return false;

        return (ai.data_format.value() == AttrDataFormat._SPECTRUM) || (ai.data_format.value() == AttrDataFormat._IMAGE);
    }

    /**
     * Check if attribute can be written with new value.
     *
     * @param ai Attribute to be checked.
     * @return True when attribute can be written.
     */
    private boolean isWritable(AttributeInfo ai) {

        return (ai.writable.value() == AttrWriteType._READ_WITH_WRITE) || (ai.writable.value() == AttrWriteType._READ_WRITE)
                || (ai.writable.value() == AttrWriteType._WRITE);

    }

    /**
     * Get alphabetically sorted list of attributes.
     *
     * @return Array of attributes.
     * @throws DevFailed When device is uninitialized or there was problem with
     *                   connection.
     */
    private AttributeInfo[] getAttributeList() throws DevFailed {
        int i, j;
        boolean end;
        AttributeInfo tmp;
        AttributeInfo[] lst = dp.get_attribute_info();
        // Sort the list
        end = false;
        j = lst.length - 1;
        while (!end) {
            end = true;
            for (i = 0; i < j; i++) {
                if (lst[i].name.compareToIgnoreCase(lst[i + 1].name) > 0) {
                    end = false;
                    tmp = lst[i];
                    lst[i] = lst[i + 1];
                    lst[i + 1] = tmp;
                }
            }
            j--;
        }
        return lst;
    }

    /**
     * Extract data read from device and convert to String.
     *
     * @param data Data read from device
     * @param ai   Parameter of read data.
     * @return String with data.
     */
    private String extractData(DeviceAttribute data, AttributeInfo ai) {

        StringBuffer ret_string = new StringBuffer();

        try {

            // Add the date of the measure in two formats
            //TimeVal t = data.getTimeVal();
            //java.util.Date date = new java.util.Date((long) (t.tv_sec * 1000.0 + t.tv_usec / 1000.0));
            // SimpleDateFormat dateformat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            //dateformat.setTimeZone(TimeZone.getDefault());
            //ret_string.append("measure date: " + dateformat.format(date) + " + " + (t.tv_usec / 1000) + "ms\n");

            // Add the quality information
            // AttrQuality q = data.getQuality();

            //ret_string.append("quality: ");
            /*switch (q.value()) {
                case AttrQuality._ATTR_VALID:
                    ret_string.append("VALID\n");
                    break;
                case AttrQuality._ATTR_INVALID:
                    ret_string.append("INVALID\n");
                    return ret_string.toString();
                case AttrQuality._ATTR_ALARM:
                    ret_string.append("ALARM\n");
                    break;
                case AttrQuality._ATTR_CHANGING:
                    ret_string.append("CHANGING\n");
                    break;
                case AttrQuality._ATTR_WARNING:
                    ret_string.append("WARNING\n");
                    break;
                default:
                    ret_string.append("UNKNOWN\n");
                    break;
            }*/

            // Add dimension of the attribute but only if having a meaning
            boolean printIndex = true;
            boolean checkLimit = true;
            switch (ai.data_format.value()) {
                case AttrDataFormat._SCALAR:
                    printIndex = false;
                    checkLimit = false;
                    break;
                case AttrDataFormat._SPECTRUM:
                    //ret_string.append("dim x: " + data.getDimX() + "\n");
                    break;
                case AttrDataFormat._IMAGE:
                    //ret_string.append("dim x: " + data.getDimX() + "\n");
                    //ret_string.append("dim y: " + data.getDimY() + "\n");
                    break;
                default:
                    break;
            }

            // Add values
            switch (ai.data_type) {

                case Tango_DEV_STATE: {
                    DevState[] dummy = data.extractDevStateArray();
                    int nbRead = data.getNbRead();
                    int nbWritten = dummy.length - nbRead;
                    int start = getLimitMin(checkLimit, ret_string, nbRead);
                    int end = getLimitMax(checkLimit, ret_string, nbRead, false);
                    for (int i = start; i < end; i++)
                        printArrayItem(ret_string, i, printIndex, Tango_DevStateName[dummy[i].value()], false);
                    if (isWritable(ai)) {
                        start = getLimitMin(checkLimit, ret_string, nbWritten);
                        end = getLimitMax(checkLimit, ret_string, nbWritten, true);
                        //for (int i = start; i < end; i++)
                        // printArrayItem(ret_string, i, printIndex, Tango_DevStateName[dummy[i + nbRead].value()], true);
                    }
                }
                break;

                case Tango_DEV_UCHAR: {
                    short[] dummy = data.extractUCharArray();
                    int nbRead = data.getNbRead();
                    int nbWritten = dummy.length - nbRead;
                    int start = getLimitMin(checkLimit, ret_string, nbRead);
                    int end = getLimitMax(checkLimit, ret_string, nbRead, false);
                    for (int i = start; i < end; i++)
                        printArrayItem(ret_string, i, printIndex, Short.toString(dummy[i]), false);
                    if (isWritable(ai)) {
                        start = getLimitMin(checkLimit, ret_string, nbWritten);
                        end = getLimitMax(checkLimit, ret_string, nbWritten, true);
                        // for (int i = start; i < end; i++)
                        // printArrayItem(ret_string, i, printIndex, Short.toString(dummy[i + nbRead]), true);
                    }
                }
                break;

                case Tango_DEV_SHORT: {
                    short[] dummy = data.extractShortArray();
                    int nbRead = data.getNbRead();
                    int nbWritten = dummy.length - nbRead;
                    int start = getLimitMin(checkLimit, ret_string, nbRead);
                    int end = getLimitMax(checkLimit, ret_string, nbRead, false);
                    for (int i = start; i < end; i++)
                        printArrayItem(ret_string, i, printIndex, Short.toString(dummy[i]), false);
                    if (isWritable(ai)) {
                        start = getLimitMin(checkLimit, ret_string, nbWritten);
                        end = getLimitMax(checkLimit, ret_string, nbWritten, true);
                        //for (int i = start; i < end; i++)
                        //  printArrayItem(ret_string, i, printIndex, Short.toString(dummy[i + nbRead]), true);
                    }
                }
                break;

                case Tango_DEV_BOOLEAN: {
                    boolean[] dummy = data.extractBooleanArray();
                    int nbRead = data.getNbRead();
                    int nbWritten = dummy.length - nbRead;
                    int start = getLimitMin(checkLimit, ret_string, nbRead);
                    int end = getLimitMax(checkLimit, ret_string, nbRead, false);
                    for (int i = start; i < end; i++)
                        printArrayItem(ret_string, i, printIndex, Boolean.toString(dummy[i]), false);
                    if (isWritable(ai)) {
                        start = getLimitMin(checkLimit, ret_string, nbWritten);
                        end = getLimitMax(checkLimit, ret_string, nbWritten, true);
                        // for (int i = start; i < end; i++)
                        //  printArrayItem(ret_string, i, printIndex, Boolean.toString(dummy[i + nbRead]), true);
                    }
                }
                break;

                case Tango_DEV_USHORT: {
                    int[] dummy = data.extractUShortArray();
                    int nbRead = data.getNbRead();
                    int nbWritten = dummy.length - nbRead;
                    int start = getLimitMin(checkLimit, ret_string, nbRead);
                    int end = getLimitMax(checkLimit, ret_string, nbRead, false);
                    for (int i = start; i < end; i++)
                        printArrayItem(ret_string, i, printIndex, Integer.toString(dummy[i]), false);
                    if (isWritable(ai)) {
                        start = getLimitMin(checkLimit, ret_string, nbWritten);
                        end = getLimitMax(checkLimit, ret_string, nbWritten, true);
                        // for (int i = start; i < end; i++)
                        // printArrayItem(ret_string, i, printIndex, Integer.toString(dummy[i + nbRead]), true);
                    }
                }
                break;

                case Tango_DEV_LONG: {
                    int[] dummy = data.extractLongArray();
                    int nbRead = data.getNbRead();
                    int nbWritten = dummy.length - nbRead;
                    int start = getLimitMin(checkLimit, ret_string, nbRead);
                    int end = getLimitMax(checkLimit, ret_string, nbRead, false);
                    for (int i = start; i < end; i++)
                        printArrayItem(ret_string, i, printIndex, Integer.toString(dummy[i]), false);
                    if (isWritable(ai)) {
                        start = getLimitMin(checkLimit, ret_string, nbWritten);
                        end = getLimitMax(checkLimit, ret_string, nbWritten, true);
                        // for (int i = start; i < end; i++)
                        //  printArrayItem(ret_string, i, printIndex, Integer.toString(dummy[i + nbRead]), true);
                    }
                }
                break;

                case Tango_DEV_ULONG: {
                    long[] dummy = data.extractULongArray();
                    int nbRead = data.getNbRead();
                    int nbWritten = dummy.length - nbRead;
                    int start = getLimitMin(checkLimit, ret_string, nbRead);
                    int end = getLimitMax(checkLimit, ret_string, nbRead, false);
                    for (int i = start; i < end; i++)
                        printArrayItem(ret_string, i, printIndex, Long.toString(dummy[i]), false);
                    if (isWritable(ai)) {
                        start = getLimitMin(checkLimit, ret_string, nbWritten);
                        end = getLimitMax(checkLimit, ret_string, nbWritten, true);
                        // for (int i = start; i < end; i++)
                        // printArrayItem(ret_string, i, printIndex, Long.toString(dummy[i + nbRead]), true);
                    }
                }
                break;

                case Tango_DEV_LONG64: {
                    long[] dummy = data.extractLong64Array();
                    int nbRead = data.getNbRead();
                    int nbWritten = dummy.length - nbRead;
                    int start = getLimitMin(checkLimit, ret_string, nbRead);
                    int end = getLimitMax(checkLimit, ret_string, nbRead, false);
                    for (int i = start; i < end; i++)
                        printArrayItem(ret_string, i, printIndex, Long.toString(dummy[i]), false);
                    if (isWritable(ai)) {
                        start = getLimitMin(checkLimit, ret_string, nbWritten);
                        end = getLimitMax(checkLimit, ret_string, nbWritten, true);
                        // for (int i = start; i < end; i++)
                        // printArrayItem(ret_string, i, printIndex, Long.toString(dummy[i + nbRead]), true);
                    }
                }
                break;

                case Tango_DEV_ULONG64: {
                    long[] dummy = data.extractULong64Array();
                    int nbRead = data.getNbRead();
                    int nbWritten = dummy.length - nbRead;
                    int start = getLimitMin(checkLimit, ret_string, nbRead);
                    int end = getLimitMax(checkLimit, ret_string, nbRead, false);
                    for (int i = start; i < end; i++)
                        printArrayItem(ret_string, i, printIndex, Long.toString(dummy[i]), false);
                    if (isWritable(ai)) {
                        start = getLimitMin(checkLimit, ret_string, nbWritten);
                        end = getLimitMax(checkLimit, ret_string, nbWritten, true);
                        // for (int i = start; i < end; i++)
                        // printArrayItem(ret_string, i, printIndex, Long.toString(dummy[i + nbRead]), true);
                    }
                }
                break;

                case Tango_DEV_DOUBLE: {
                    double[] dummy = data.extractDoubleArray();
                    int nbRead = data.getNbRead();
                    int nbWritten = dummy.length - nbRead;
                    int start = getLimitMin(checkLimit, ret_string, nbRead);
                    int end = getLimitMax(checkLimit, ret_string, nbRead, false);
                    for (int i = start; i < end; i++)
                        printArrayItem(ret_string, i, printIndex, Double.toString(dummy[i]), false);
                    if (isWritable(ai)) {
                        start = getLimitMin(checkLimit, ret_string, nbWritten);
                        end = getLimitMax(checkLimit, ret_string, nbWritten, true);
                        // for (int i = start; i < end; i++)
                        // printArrayItem(ret_string, i, printIndex, Double.toString(dummy[i + nbRead]), true);
                    }
                }
                break;

                case Tango_DEV_FLOAT: {
                    float[] dummy = data.extractFloatArray();
                    int nbRead = data.getNbRead();
                    int nbWritten = dummy.length - nbRead;
                    int start = getLimitMin(checkLimit, ret_string, nbRead);
                    int end = getLimitMax(checkLimit, ret_string, nbRead, false);
                    for (int i = start; i < end; i++)
                        printArrayItem(ret_string, i, printIndex, Float.toString(dummy[i]), false);
                    if (isWritable(ai)) {
                        start = getLimitMin(checkLimit, ret_string, nbWritten);
                        end = getLimitMax(checkLimit, ret_string, nbWritten, true);
                        //for (int i = start; i < end; i++)
                        // printArrayItem(ret_string, i, printIndex, Float.toString(dummy[i + nbRead]), true);
                    }
                }
                break;

                case Tango_DEV_STRING: {
                    String[] dummy = data.extractStringArray();
                    int nbRead = data.getNbRead();
                    int nbWritten = dummy.length - nbRead;
                    int start = getLimitMin(checkLimit, ret_string, nbRead);
                    int end = getLimitMax(checkLimit, ret_string, nbRead, false);
                    for (int i = start; i < end; i++)
                        printArrayItem(ret_string, i, printIndex, dummy[i], false);
                    if (isWritable(ai)) {
                        start = getLimitMin(checkLimit, ret_string, nbWritten);
                        end = getLimitMax(checkLimit, ret_string, nbWritten, true);
                        // for (int i = start; i < end; i++)
                        //printArrayItem(ret_string, i, printIndex, dummy[i + nbRead], true);
                    }
                }
                break;

                case Tango_DEV_ENCODED: {
                    printIndex = true;
                    DevEncoded e = data.extractDevEncoded();
                    //ret_string.append("Format: " + e.encoded_format + "\n");
                    int nbRead = e.encoded_data.length;
                    int start = getLimitMin(checkLimit, ret_string, nbRead);
                    int end = getLimitMax(checkLimit, ret_string, nbRead, false);
                    for (int i = start; i < end; i++) {
                        short vs = (short) e.encoded_data[i];
                        vs = (short) (vs & 0xFF);
                        printArrayItem(ret_string, i, printIndex, Short.toString(vs), false);
                    }
                }
                break;

                default:
                    ret_string.append("Unsupported attribute type code=" + ai.data_type + "\n");
                    break;
            }

        } catch (DevFailed e) {

            // ErrorPane.showErrorMessage(this,device.name() + "/" + ai.name,e);

        }

        return ret_string.toString();

    }

    /**
     * Parses string to be printed
     *
     * @param str       String to be printed.
     * @param idx       Number of value in array.
     * @param printIdx  Defines if array has more than one value.
     * @param value     Value that was read/written.
     * @param writeable Defines if value is writable.
     */
    private void printArrayItem(StringBuffer str, int idx, boolean printIdx, String value, boolean writeable) {
        if (!writeable) {
            if (printIdx)
                str.append(value + ", ");
            else
                str.append(value + ", ");
        } else {
            if (printIdx)
                str.append("Set [" + idx + "]" + value + "");
            else
                str.append("Set:\t" + value + "");
        }
    }

    /**
     * Check maximum length of response.
     *
     * @param checkLimit
     * @param retStr     Response string.
     * @param length     Length of current response.
     * @param writable   Defines if value is writable.
     * @return Maximum response length.
     */
    private int getLimitMax(boolean checkLimit, StringBuffer retStr, int length, boolean writable) {
        if (length < 100) {
            return length;

        }
        return 100;
    }

    /**
     * Check minimum length of response.
     *
     * @param checkLimit
     * @param retStr     Response string.
     * @param length     Length of current response.
     * @return Minimum response length.
     */
    private int getLimitMin(boolean checkLimit, StringBuffer retStr, int length) {

        return 0;

    }

    /**
     * Adds value to DeviceAttribute.
     *
     * @param argin Value to be added.
     * @param send  Value will be added to this DeviceAttribute.
     * @param ai    Define data format.
     * @return DeviceAttribute with new value.
     * @throws NumberFormatException
     */
    private DeviceAttribute insertData(String argin, DeviceAttribute send, AttributeInfo ai) throws NumberFormatException {

        ArgParser arg = new ArgParser(argin);

        switch (ai.data_type) {

            case Tango_DEV_UCHAR:
                switch (ai.data_format.value()) {
                    case AttrDataFormat._SCALAR:
                        send.insert_uc(arg.parse_uchar());
                        break;
                    case AttrDataFormat._SPECTRUM:
                        send.insert_uc(arg.parse_uchar_array());
                        break;
                    case AttrDataFormat._IMAGE:
                        send.insert_uc(arg.parse_uchar_image(), arg.get_image_width(), arg.get_image_height());
                        break;
                }
                break;

            case Tango_DEV_BOOLEAN:
                switch (ai.data_format.value()) {
                    case AttrDataFormat._SCALAR:
                        send.insert(arg.parse_boolean());
                        break;
                    case AttrDataFormat._SPECTRUM:
                        send.insert(arg.parse_boolean_array());
                        break;
                    case AttrDataFormat._IMAGE:
                        send.insert(arg.parse_boolean_image(), arg.get_image_width(), arg.get_image_height());
                        break;
                }
                break;

            case Tango_DEV_SHORT:
                switch (ai.data_format.value()) {
                    case AttrDataFormat._SCALAR:
                        send.insert(arg.parse_short());
                        break;
                    case AttrDataFormat._SPECTRUM:
                        send.insert(arg.parse_short_array());
                        break;
                    case AttrDataFormat._IMAGE:
                        send.insert(arg.parse_short_image(), arg.get_image_width(), arg.get_image_height());
                        break;
                }
                break;

            case Tango_DEV_USHORT:
                switch (ai.data_format.value()) {
                    case AttrDataFormat._SCALAR:
                        send.insert_us(arg.parse_ushort());
                        break;
                    case AttrDataFormat._SPECTRUM:
                        send.insert_us(arg.parse_ushort_array());
                        break;
                    case AttrDataFormat._IMAGE:
                        send.insert_us(arg.parse_ushort_image(), arg.get_image_width(), arg.get_image_height());
                        break;
                }
                break;

            case Tango_DEV_LONG:
                switch (ai.data_format.value()) {
                    case AttrDataFormat._SCALAR:
                        send.insert(arg.parse_long());
                        break;
                    case AttrDataFormat._SPECTRUM:
                        send.insert(arg.parse_long_array());
                        break;
                    case AttrDataFormat._IMAGE:
                        send.insert(arg.parse_long_image(), arg.get_image_width(), arg.get_image_height());
                        break;
                }
                break;

            case Tango_DEV_ULONG:
                switch (ai.data_format.value()) {
                    case AttrDataFormat._SCALAR:
                        send.insert_ul(arg.parse_ulong());
                        break;
                    case AttrDataFormat._SPECTRUM:
                        send.insert_ul(arg.parse_ulong_array());
                        break;
                    case AttrDataFormat._IMAGE:
                        send.insert_ul(arg.parse_ulong_image(), arg.get_image_width(), arg.get_image_height());
                        break;
                }
                break;

            case Tango_DEV_LONG64:
                switch (ai.data_format.value()) {
                    case AttrDataFormat._SCALAR:
                        send.insert(arg.parse_long64());
                        break;
                    case AttrDataFormat._SPECTRUM:
                        send.insert(arg.parse_long64_array());
                        break;
                    case AttrDataFormat._IMAGE:
                        send.insert(arg.parse_long64_image(), arg.get_image_width(), arg.get_image_height());
                        break;
                }
                break;

            case Tango_DEV_ULONG64:
                switch (ai.data_format.value()) {
                    case AttrDataFormat._SCALAR:
                        send.insert_u64(arg.parse_long64());
                        break;
                    case AttrDataFormat._SPECTRUM:
                        send.insert_u64(arg.parse_long64_array());
                        break;
                    case AttrDataFormat._IMAGE:
                        send.insert_u64(arg.parse_long64_image(), arg.get_image_width(), arg.get_image_height());
                        break;
                }
                break;

            case Tango_DEV_FLOAT:
                switch (ai.data_format.value()) {
                    case AttrDataFormat._SCALAR:
                        send.insert(arg.parse_float());
                        break;
                    case AttrDataFormat._SPECTRUM:
                        send.insert(arg.parse_float_array());
                        break;
                    case AttrDataFormat._IMAGE:
                        send.insert(arg.parse_float_image(), arg.get_image_width(), arg.get_image_height());
                        break;
                }
                break;

            case Tango_DEV_DOUBLE:
                switch (ai.data_format.value()) {
                    case AttrDataFormat._SCALAR:
                        send.insert(arg.parse_double());
                        break;
                    case AttrDataFormat._SPECTRUM:
                        send.insert(arg.parse_double_array());
                        break;
                    case AttrDataFormat._IMAGE:
                        send.insert(arg.parse_double_image(), arg.get_image_width(), arg.get_image_height());
                        break;
                }
                break;

            case Tango_DEV_STRING:
                switch (ai.data_format.value()) {
                    case AttrDataFormat._SCALAR:
                        send.insert(arg.parse_string());
                        break;
                    case AttrDataFormat._SPECTRUM:
                        send.insert(arg.parse_string_array());
                        break;
                    case AttrDataFormat._IMAGE:
                        send.insert(arg.parse_string_image(), arg.get_image_width(), arg.get_image_height());
                        break;
                }
                break;

            default:
                throw new NumberFormatException("Attribute type not supported code=" + ai.data_type);

        }
        return send;

    }

    /**
     * Extract data from DeviceAttribute to one dimensional array.
     *
     * @param data DeviceAttribute to extract data from.
     * @param ai   Define data format.
     * @return Array of data that can be plotted.
     */
    private double[] extractSpectrumPlotData(DeviceAttribute data, AttributeInfo ai) {

        double[] ret = new double[0];
        int i;

        try {

            int start = getLimitMinForPlot(data.getNbRead());
            int end = getLimitMaxForPlot(data.getNbRead());

            switch (ai.data_type) {

                case Tango_DEV_UCHAR: {
                    short[] dummy = data.extractUCharArray();
                    ret = new double[end - start];
                    for (i = start; i < end; i++)
                        ret[i - start] = (double) dummy[i];
                }
                break;

                case Tango_DEV_SHORT: {
                    short[] dummy = data.extractShortArray();
                    ret = new double[end - start];
                    for (i = start; i < end; i++)
                        ret[i - start] = (double) dummy[i];
                }
                break;

                case Tango_DEV_USHORT: {
                    int[] dummy = data.extractUShortArray();
                    ret = new double[end - start];
                    for (i = start; i < end; i++)
                        ret[i - start] = (double) dummy[i];
                }
                break;

                case Tango_DEV_LONG: {
                    int[] dummy = data.extractLongArray();
                    ret = new double[end - start];
                    for (i = start; i < end; i++)
                        ret[i - start] = (double) dummy[i];
                }
                break;

                case Tango_DEV_DOUBLE: {
                    double[] dummy = data.extractDoubleArray();
                    ret = new double[end - start];
                    for (i = start; i < end; i++)
                        ret[i - start] = dummy[i];
                }
                break;

                case Tango_DEV_FLOAT: {
                    float[] dummy = data.extractFloatArray();
                    ret = new double[end - start];
                    for (i = start; i < end; i++)
                        ret[i - start] = (double) dummy[i];
                }
                break;

            }

        } catch (DevFailed e) {

            // ErrorPane.showErrorMessage(this, device.name() + "/" + ai.name,
            // e);

        }

        return ret;

    }

    /**
     * Extract data from DeviceAttribute to two dimensional array.
     *
     * @param data DeviceAttribute to extract data from.
     * @param ai   Define data format.
     * @return Array of data that can be plotted.
     */
    private double[][] extractImagePlotData(DeviceAttribute data, AttributeInfo ai) {

        double[][] ret = new double[0][0];
        int i, j, k, dimx, dimy;

        try {

            dimx = data.getDimX();
            dimy = data.getDimY();

            switch (ai.data_type) {

                case Tango_DEV_UCHAR: {
                    short[] dummy = data.extractUCharArray();
                    ret = new double[dimy][dimx];
                    for (j = 0, k = 0; j < dimy; j++)
                        for (i = 0; i < dimx; i++)
                            ret[j][i] = (double) dummy[k++];
                }
                break;
                case Tango_DEV_SHORT: {
                    short[] dummy = data.extractShortArray();
                    ret = new double[dimy][dimx];
                    for (j = 0, k = 0; j < dimy; j++)
                        for (i = 0; i < dimx; i++)
                            ret[j][i] = (double) dummy[k++];
                }
                break;
                case Tango_DEV_USHORT: {
                    int[] dummy = data.extractUShortArray();
                    ret = new double[dimy][dimx];
                    for (j = 0, k = 0; j < dimy; j++)
                        for (i = 0; i < dimx; i++)
                            ret[j][i] = (double) dummy[k++];
                }
                break;
                case Tango_DEV_LONG: {
                    int[] dummy = data.extractLongArray();
                    ret = new double[dimy][dimx];
                    for (j = 0, k = 0; j < dimy; j++)
                        for (i = 0; i < dimx; i++)
                            ret[j][i] = (double) dummy[k++];
                }
                break;
                case Tango_DEV_DOUBLE: {
                    double[] dummy = data.extractDoubleArray();
                    ret = new double[dimy][dimx];
                    for (j = 0, k = 0; j < dimy; j++)
                        for (i = 0; i < dimx; i++)
                            ret[j][i] = dummy[k++];
                }
                break;
                case Tango_DEV_FLOAT: {
                    float[] dummy = data.extractFloatArray();
                    ret = new double[dimy][dimx];
                    for (j = 0, k = 0; j < dimy; j++)
                        for (i = 0; i < dimx; i++)
                            ret[j][i] = (double) dummy[k++];
                }
                break;
            }
        } catch (DevFailed e) {
            // ErrorPane.showErrorMessage(this, device.name() + "/" + ai.name,
            // e);
        }
        return ret;
    }

    /**
     * Check maximum length of data.
     *
     * @param length Length of current data.
     * @return Maximum length.
     */
    private int getLimitMaxForPlot(int length) {
        if (length < 100) {
            return length;

        }
        return 100;
    }

    /**
     * Check minimum length of data.
     *
     * @param length Length of current data.
     * @return Minimum length.
     */
    private int getLimitMinForPlot(int length) {
        return 0;
    }
}
