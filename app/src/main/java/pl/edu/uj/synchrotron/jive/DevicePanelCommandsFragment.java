package pl.edu.uj.synchrotron.jive;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import fr.esrf.Tango.DevFailed;
import fr.esrf.Tango.DevState;
import fr.esrf.Tango.DevVarDoubleStringArray;
import fr.esrf.Tango.DevVarLongStringArray;
import fr.esrf.TangoApi.CommandInfo;
import fr.esrf.TangoApi.DeviceData;
import fr.esrf.TangoApi.DeviceProxy;
import fr.esrf.TangoDs.TangoConst;

public class DevicePanelCommandsFragment extends Fragment implements TangoConst {
    private int selectedCommandId;
    private CommandInfo[] ci;
    private View rootView;
    private DeviceProxy dp;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_device_panel_commands, container, false);
        String deviceName = ((DevicePanelActivity) getActivity()).getDeviceName();
        TextView tvDeviceName = (TextView) rootView.findViewById(R.id.devicePanel_deviceName);
        tvDeviceName.setText(deviceName);
        System.out.println("Device name: " + deviceName);
        String dbHost = ((DevicePanelActivity) getActivity()).getHost();
        System.out.println("Host: " + dbHost);
        String dbPort = ((DevicePanelActivity) getActivity()).getPort();
        System.out.println("Port: " + dbPort);
        try {
            dp = new DeviceProxy(deviceName, dbHost, dbPort);
            ci = dp.command_list_query();

            String[] commandList = new String[ci.length];
            for (int i = 0; i < ci.length; i++) {
                commandList[i] = ci[i].cmd_name;
            }
            ListView lv = (ListView) rootView.findViewById(R.id.devicePanel_listView);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item, R.id.firstLine,
                    commandList);
            lv.setAdapter(adapter);

            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                    selectedCommandId = position;
                    for (int a = 0; a < parent.getChildCount(); a++) {
                        parent.getChildAt(a).setBackgroundColor(Color.TRANSPARENT);
                    }
                    view.setBackgroundColor(Color.GRAY);
                    // String inType =
                    // CommandInfo.TangoTypesArray[ci[selectedCommandId].in_type].substring(6);
                    String inType = Tango_CmdArgTypeName[ci[selectedCommandId].in_type];
                    // String outType =
                    // CommandInfo.TangoTypesArray[ci[selectedCommandId].out_type].substring(6);
                    String outType = Tango_CmdArgTypeName[ci[selectedCommandId].out_type];
                    boolean clickablePlotButton = isPlotable(ci[selectedCommandId].out_type);
                    Button plotButton = (Button) rootView.findViewById(R.id.devicePanel_plotButton);
                    plotButton.setClickable(clickablePlotButton);
                    plotButton.setEnabled(clickablePlotButton);
                    Button descriptionButton = (Button) rootView.findViewById(R.id.devicePanel_descriptionButton);
                    descriptionButton.setClickable(true);
                    descriptionButton.setEnabled(true);
                    Button executeButton = (Button) rootView.findViewById(R.id.devicePanel_executeButton);
                    executeButton.setClickable(true);
                    executeButton.setEnabled(true);
                    TextView arginType = (TextView) getActivity().findViewById(R.id.devicePanel_arginTypeValue);
                    arginType.setText(inType);
                    TextView argoutType = (TextView) getActivity().findViewById(R.id.devicePanel_argoutTypeValue);
                    argoutType.setText(outType);
                }
            });
        } catch (DevFailed e) {
            e.printStackTrace();
        }

        Button descriptionButton = (Button) rootView.findViewById(R.id.devicePanel_descriptionButton);
        descriptionButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
                builder.setTitle("Command \"" + ci[selectedCommandId].cmd_name + " \"description");
                String message = new String("Argin:\n" + ci[selectedCommandId].in_type_desc + "\nArgout:\n"
                        + ci[selectedCommandId].out_type_desc);
                builder.setMessage(message);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        Button executeButton = (Button) rootView.findViewById(R.id.devicePanel_executeButton);
        executeButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    CommandInfo commInfo = ci[selectedCommandId];
                    EditText arginEditTextValue = (EditText) rootView.findViewById(R.id.devicePanel_arginValueEditText);
                    String arginStr = arginEditTextValue.getText().toString();
                    System.out.println("Have text from input: " + arginStr);
                    DeviceData argin = new DeviceData();
                    argin = insertData(arginStr, argin, commInfo.in_type);
                    String cmd = commInfo.cmd_name;
                    long t0 = System.currentTimeMillis();
                    DeviceData argout = dp.command_inout(cmd, argin);
                    long t1 = System.currentTimeMillis();
                    System.out.print("----------------------------------------------------\n");
                    System.out.print("Command: " + dp.name() + "/" + cmd + "\n");
                    System.out.print("Duration: " + (t1 - t0) + " msec\n");
                    if (commInfo.out_type == Tango_DEV_VOID) {
                        System.out.print("Command OK\n");
                        AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
                        builder.setMessage("OK").setTitle("Command reply");
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    } else {
                        System.out.print("Output argument(s) :\n");
                        String commandOut = new String();
                        commandOut = extractData(argout, commInfo.out_type);
                        System.out.print(commandOut);
                        AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
                        builder.setMessage(commandOut).setTitle("Command output");
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                } catch (NumberFormatException e1) {
                    System.out.println("Invalid argin syntax\n" + e1.getMessage());
                    AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
                    builder.setMessage(e1.getMessage()).setTitle("Invalid argin syntax");
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();

                } catch (DevFailed e2) {
                    System.out.println("Device failed: " + dp.name() + "\n" + e2);
                    AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
                    builder.setMessage(e2.getMessage()).setTitle("Device failed");
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }

            }
        });

        Button plotButton = (Button) rootView.findViewById(R.id.devicePanel_plotButton);
        plotButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    CommandInfo commInfo = ci[selectedCommandId];
                    EditText arginEditTextValue = (EditText) rootView.findViewById(R.id.devicePanel_arginValueEditText);
                    String arginStr = arginEditTextValue.getText().toString();
                    DeviceData argin = new DeviceData();
                    argin = insertData(arginStr, argin, commInfo.in_type);
                    String cmd = commInfo.cmd_name;
                    DeviceData argout = dp.command_inout(cmd, argin);
                    double[] values = extractPlotData(argout, commInfo.out_type);
                    System.out.println("Sending data: " + values.length);
                    System.out.println("Sending data: " + values.toString());
                    DataToPlot dtp = new DataToPlot(values);
                    Intent intent = new Intent(getActivity(), PlotActivity.class);
                    intent.putExtra("data", dtp);
                    intent.putExtra("domainLabel", commInfo.cmd_name);
                    // intent.putExtra("plotTitle", dp.name()+"/"+commInfo.cmd_name);
                    startActivity(intent);
                } catch (NumberFormatException e1) {
                    System.out.println("Invalid argin syntax\n" + e1.getMessage());
                    AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
                    builder.setMessage(e1.getMessage()).setTitle("Invalid argin syntax");
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                } catch (DevFailed e2) {
                    System.out.println("Device failed: " + dp.name() + "\n" + e2);
                    AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
                    builder.setMessage(e2.getMessage()).setTitle("Device failed");
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }

            }
        });
        return rootView;
    }

    /**
     * Extract data read from device and converts to String.
     *
     * @param data    Data read from device
     * @param outType Identifier of data type.
     * @return String with data.
     */
    private String extractData(DeviceData data, int outType) {
        StringBuffer ret_string = new StringBuffer();
        switch (outType) {
            case Tango_DEV_VOID:
                break;
            case Tango_DEV_BOOLEAN:
                ret_string.append(Boolean.toString(data.extractBoolean()));
                ret_string.append("\n");
                break;
            case Tango_DEV_USHORT:
                ret_string.append(Integer.toString(data.extractUShort()));
                ret_string.append("\n");
                break;
            case Tango_DEV_SHORT:
                ret_string.append(Short.toString(data.extractShort()));
                ret_string.append("\n");
                break;
            case Tango_DEV_ULONG:
                ret_string.append(Long.toString(data.extractULong()));
                ret_string.append("\n");
                break;
            case Tango_DEV_LONG:
                ret_string.append(Integer.toString(data.extractLong()));
                ret_string.append("\n");
                break;
            case Tango_DEV_FLOAT:
                ret_string.append(Float.toString(data.extractFloat()));
                ret_string.append("\n");
                break;
            case Tango_DEV_DOUBLE:
                ret_string.append(Double.toString(data.extractDouble()));
                ret_string.append("\n");
                break;
            case Tango_DEV_STRING:
                ret_string.append(data.extractString());
                ret_string.append("\n");
                break;
            case Tango_DEVVAR_CHARARRAY: {
                byte[] dummy = data.extractByteArray();
                int start = getLimitMin(ret_string, dummy.length);
                int end = getLimitMax(ret_string, dummy.length);
                for (int i = start; i < end; i++) {
                    ret_string.append("[" + i + "]\t " + Integer.toString(dummy[i]));
                    if (dummy[i] >= 32)
                        ret_string.append(" '" + (new Character((char) dummy[i]).toString()) + "'");
                    else
                        ret_string.append(" '.'");
                    ret_string.append("\n");
                }
            }
            break;
            case Tango_DEVVAR_USHORTARRAY: {
                int[] dummy = data.extractUShortArray();
                int start = getLimitMin(ret_string, dummy.length);
                int end = getLimitMax(ret_string, dummy.length);
                for (int i = start; i < end; i++)
                    ret_string.append("[" + i + "]\t " + Integer.toString(dummy[i]) + "\n");
            }
            break;
            case Tango_DEVVAR_SHORTARRAY: {
                short[] dummy = data.extractShortArray();
                int start = getLimitMin(ret_string, dummy.length);
                int end = getLimitMax(ret_string, dummy.length);
                for (int i = start; i < end; i++)
                    ret_string.append("[" + i + "]\t " + Short.toString(dummy[i]) + "\n");
            }
            break;
            case Tango_DEVVAR_ULONGARRAY: {
                long[] dummy = data.extractULongArray();
                int start = getLimitMin(ret_string, dummy.length);
                int end = getLimitMax(ret_string, dummy.length);
                for (int i = start; i < end; i++)
                    ret_string.append("[" + i + "]\t " + Long.toString(dummy[i]) + "\n");
            }
            break;
            case Tango_DEVVAR_LONGARRAY: {
                int[] dummy = data.extractLongArray();
                int start = getLimitMin(ret_string, dummy.length);
                int end = getLimitMax(ret_string, dummy.length);
                for (int i = start; i < end; i++)
                    ret_string.append("[" + i + "]\t " + Integer.toString(dummy[i]) + "\n");
            }
            break;
            case Tango_DEVVAR_FLOATARRAY: {
                float[] dummy = data.extractFloatArray();
                int start = getLimitMin(ret_string, dummy.length);
                int end = getLimitMax(ret_string, dummy.length);
                for (int i = start; i < end; i++)
                    ret_string.append("[" + i + "]\t " + Float.toString(dummy[i]) + "\n");
            }
            break;
            case Tango_DEVVAR_DOUBLEARRAY: {
                double[] dummy = data.extractDoubleArray();
                int start = getLimitMin(ret_string, dummy.length);
                int end = getLimitMax(ret_string, dummy.length);
                for (int i = start; i < end; i++)
                    ret_string.append("[" + i + "]\t" + Double.toString(dummy[i]) + "\n");
            }
            break;
            case Tango_DEVVAR_STRINGARRAY: {
                String[] dummy = data.extractStringArray();
                int start = getLimitMin(ret_string, dummy.length);
                int end = getLimitMax(ret_string, dummy.length);
                for (int i = start; i < end; i++)
                    ret_string.append("[" + i + "]\t " + dummy[i] + "\n");
            }
            break;
            case Tango_DEVVAR_LONGSTRINGARRAY: {
                DevVarLongStringArray dummy = data.extractLongStringArray();
                int start = getLimitMin(ret_string, dummy.lvalue.length);
                int end = getLimitMax(ret_string, dummy.lvalue.length);
                ret_string.append("lvalue:\n");
                for (int i = start; i < end; i++)
                    ret_string.append("[" + i + "]\t " + Integer.toString(dummy.lvalue[i]) + "\n");
                start = getLimitMin(ret_string, dummy.svalue.length);
                end = getLimitMax(ret_string, dummy.svalue.length);
                ret_string.append("svalue:\n");
                for (int i = start; i < end; i++)
                    ret_string.append("[" + i + "]\t " + dummy.svalue[i] + "\n");
            }
            break;
            case Tango_DEVVAR_DOUBLESTRINGARRAY: {
                DevVarDoubleStringArray dummy = data.extractDoubleStringArray();
                int start = getLimitMin(ret_string, dummy.dvalue.length);
                int end = getLimitMax(ret_string, dummy.dvalue.length);
                ret_string.append("dvalue:\n");
                for (int i = start; i < end; i++)
                    ret_string.append("[" + i + "]\t " + Double.toString(dummy.dvalue[i]) + "\n");
                start = getLimitMin(ret_string, dummy.svalue.length);
                end = getLimitMax(ret_string, dummy.svalue.length);
                ret_string.append("svalue:\n");
                for (int i = start; i < end; i++)
                    ret_string.append("[" + i + "]\t " + dummy.svalue[i] + "\n");
            }
            break;
            case Tango_DEV_STATE:
                ret_string.append(Tango_DevStateName[data.extractDevState().value()]);
                ret_string.append("\n");
                break;
            default:
                ret_string.append("Unsupported command type code=" + outType);
                ret_string.append("\n");
                break;
        }
        return ret_string.toString();
    }

    /**
     * Check maximum length of response.
     *
     * @param retStr Response string.
     * @param length Length of current response.
     * @return Maximum response length.
     */
    private int getLimitMax(StringBuffer retStr, int length) {
        if (length < 100) {
            return length;

        }
        return 100;
    }

    /**
     * Check minimum length of response.
     *
     * @param retStr Response string.
     * @param length Length of current response.
     */

    private int getLimitMin(StringBuffer retStr, int length) {
        // if(length<=common.getAnswerLimitMin()) {
        // retStr.append("Array cannot be displayed. (You may change the AnswerLimitMin)\n");
        // return length;
        // } else {
        // return common.getAnswerLimitMin();
        return 0;
        // }
    }

    /**
     * Adds value to DeviceAttribute.
     *
     * @param argin   Value to be added.
     * @param send    Value will be added to this DeviceData.
     * @param outType Identifier of data type.
     * @return DeviceData with new value.
     * @throws NumberFormatException
     */
    private DeviceData insertData(String argin, DeviceData send, int outType) throws NumberFormatException {

        if (outType == Tango_DEV_VOID)
            return send;

        ArgParser arg = new ArgParser(argin);

        switch (outType) {
            case Tango_DEV_BOOLEAN:
                send.insert(arg.parse_boolean());
                break;
            case Tango_DEV_USHORT:
                send.insert_us(arg.parse_ushort());
                break;
            case Tango_DEV_SHORT:
                send.insert(arg.parse_short());
                break;
            case Tango_DEV_ULONG:
                send.insert_ul(arg.parse_ulong());
                break;
            case Tango_DEV_LONG:
                send.insert(arg.parse_long());
                break;
            case Tango_DEV_FLOAT:
                send.insert(arg.parse_float());
                break;
            case Tango_DEV_DOUBLE:
                send.insert(arg.parse_double());
                break;
            case Tango_DEV_STRING:
                send.insert(arg.parse_string());
                break;
            case Tango_DEVVAR_CHARARRAY:
                send.insert(arg.parse_char_array());
                break;
            case Tango_DEVVAR_USHORTARRAY:
                send.insert_us(arg.parse_ushort_array());
                break;
            case Tango_DEVVAR_SHORTARRAY:
                send.insert(arg.parse_short_array());
                break;
            case Tango_DEVVAR_ULONGARRAY:
                send.insert_ul(arg.parse_ulong_array());
                break;
            case Tango_DEVVAR_LONGARRAY:
                send.insert(arg.parse_long_array());
                break;
            case Tango_DEVVAR_FLOATARRAY:
                send.insert(arg.parse_float_array());
                break;
            case Tango_DEVVAR_DOUBLEARRAY:
                send.insert(arg.parse_double_array());
                break;
            case Tango_DEVVAR_STRINGARRAY:
                send.insert(arg.parse_string_array());
                break;
            case Tango_DEVVAR_LONGSTRINGARRAY:
                send.insert(new DevVarLongStringArray(arg.parse_long_array(), arg.parse_string_array()));
                break;
            case Tango_DEVVAR_DOUBLESTRINGARRAY:
                send.insert(new DevVarDoubleStringArray(arg.parse_double_array(), arg.parse_string_array()));
                break;
            case Tango_DEV_STATE:
                send.insert(DevState.from_int(arg.parse_ushort()));
                break;

            default:
                throw new NumberFormatException("Command type not supported code=" + outType);

        }
        return send;

    }

    /**
     * Check if data can be plotted.
     *
     * @param outType Identifier of data type.
     * @return True if plotable.
     */
    private boolean isPlotable(int outType) {
        switch (outType) {
            case Tango_DEVVAR_CHARARRAY:
            case Tango_DEVVAR_USHORTARRAY:
            case Tango_DEVVAR_SHORTARRAY:
            case Tango_DEVVAR_ULONGARRAY:
            case Tango_DEVVAR_LONGARRAY:
            case Tango_DEVVAR_FLOATARRAY:
            case Tango_DEVVAR_DOUBLEARRAY:
                return true;
        }
        return false;
    }

    /**
     * Extract data from DeviceData to one dimensional array.
     *
     * @param data    DeviceData to extract data from.
     * @param outType Identifier of data type.
     * @return Array of data that can be plotted.
     */
    private double[] extractPlotData(DeviceData data, int outType) {

        double[] ret = new double[0];
        int i;

        switch (outType) {

            case Tango_DEVVAR_CHARARRAY: {
                byte[] dummy = data.extractByteArray();
                int start = this.getLimitMinForPlot(dummy.length);
                int end = this.getLimitMaxForPlot(dummy.length);
                ret = new double[end - start];
                for (i = start; i < end; i++)
                    ret[i - start] = (double) dummy[i];
            }
            break;
            case Tango_DEVVAR_USHORTARRAY: {
                int[] dummy = data.extractUShortArray();
                int start = this.getLimitMinForPlot(dummy.length);
                int end = this.getLimitMaxForPlot(dummy.length);
                ret = new double[end - start];
                for (i = start; i < end; i++)
                    ret[i - start] = (double) dummy[i];
            }
            break;
            case Tango_DEVVAR_SHORTARRAY: {
                short[] dummy = data.extractShortArray();
                int start = this.getLimitMinForPlot(dummy.length);
                int end = this.getLimitMaxForPlot(dummy.length);
                ret = new double[end - start];
                for (i = start; i < end; i++)
                    ret[i - start] = (double) dummy[i];
            }
            break;
            case Tango_DEVVAR_ULONGARRAY: {
                long[] dummy = data.extractULongArray();
                int start = this.getLimitMinForPlot(dummy.length);
                int end = this.getLimitMaxForPlot(dummy.length);
                ret = new double[end - start];
                for (i = start; i < end; i++)
                    ret[i - start] = (double) dummy[i];
            }
            break;
            case Tango_DEVVAR_LONGARRAY: {
                int[] dummy = data.extractLongArray();
                int start = this.getLimitMinForPlot(dummy.length);
                int end = this.getLimitMaxForPlot(dummy.length);
                ret = new double[end - start];
                for (i = start; i < end; i++)
                    ret[i - start] = (double) dummy[i];
            }
            break;
            case Tango_DEVVAR_FLOATARRAY: {
                float[] dummy = data.extractFloatArray();
                int start = this.getLimitMinForPlot(dummy.length);
                int end = this.getLimitMaxForPlot(dummy.length);
                ret = new double[end - start];
                for (i = start; i < end; i++)
                    ret[i - start] = (double) dummy[i];
            }
            break;
            case Tango_DEVVAR_DOUBLEARRAY: {
                double dummy[] = data.extractDoubleArray();
                int start = this.getLimitMinForPlot(dummy.length);
                int end = this.getLimitMaxForPlot(dummy.length);
                ret = new double[end - start];
                for (i = start; i < end; i++)
                    ret[i - start] = dummy[i];
            }
            break;
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
