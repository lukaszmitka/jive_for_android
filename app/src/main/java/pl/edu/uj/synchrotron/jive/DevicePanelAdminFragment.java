package pl.edu.uj.synchrotron.jive;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import fr.esrf.Tango.DevFailed;
import fr.esrf.Tango.DevInfo;
import fr.esrf.TangoApi.DeviceData;
import fr.esrf.TangoApi.DeviceProxy;

/**
 * A class for creating device panel admin fragment.
 */
public class DevicePanelAdminFragment extends Fragment {

    EditText answerLimitMinEditText;
    EditText answerLimitMaxEditText;
    EditText timeoutEditText;
    EditText blackBoxEditText;
    private DeviceProxy device = null;
    private DeviceProxy deviceAdm = null;
    private int answerLimitMin = 0;
    private int answerLimitMax = 1024;
    private View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_device_panel_admin, container, false);

        String deviceName = ((DevicePanelActivity) getActivity()).getDeviceName();
        System.out.println("Device name: " + deviceName);
        String dbHost = ((DevicePanelActivity) getActivity()).getHost();
        System.out.println("Host: " + dbHost);
        String dbPort = ((DevicePanelActivity) getActivity()).getPort();
        System.out.println("Port: " + dbPort);

        try {
            answerLimitMinEditText = (EditText) rootView.findViewById(R.id.devicePanel_adminFragment_limitMinEditText);
            answerLimitMinEditText.setText(new String("" + answerLimitMin));
            answerLimitMaxEditText = (EditText) rootView.findViewById(R.id.devicePanel_adminFragment_limitMaxEditText);
            answerLimitMaxEditText.setText(new String("" + answerLimitMax));
            timeoutEditText = (EditText) rootView.findViewById(R.id.devicePanel_adminFragment_timeoutEditText);
            device = new DeviceProxy(deviceName, dbHost, dbPort);
            try {
                device.ping();
                deviceAdm = device.get_adm_dev();
            } catch (DevFailed e) {
                AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
                builder.setTitle("Admin error");
                builder.setMessage(e.getMessage());
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                e.printStackTrace();
            }
            timeoutEditText.setText(Integer.toString(device.get_timeout_millis()));
            blackBoxEditText = (EditText) rootView.findViewById(R.id.devicePanel_adminFragment_blackboxEditText);
            blackBoxEditText.setText("10");

            Button blackBoxButton = (Button) rootView.findViewById(R.id.devicePanel_adminFragment_BlackBoxButton);
            blackBoxButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    int nbCmd = Integer.parseInt(blackBoxEditText.getText().toString());

                    try {
                        long t0 = System.currentTimeMillis();
                        String[] out = device.black_box(nbCmd);
                        long t1 = System.currentTimeMillis();

                        AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
                        builder.setTitle("Black Box");

                        String message = new String(
                                "Command: " + device.name() + "/BlackBox\n" + "Duration: " + (t1 - t0) + " msec\n\n");
                        for (int i = 0; i < out.length; i++) {
                            message = message + "[" + i + "]\t " + out[i] + "\n";
                        }
                        builder.setMessage(message);
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    } catch (DevFailed e) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
                        builder.setTitle("Command error");
                        builder.setMessage(e.getMessage());
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                        e.printStackTrace();
                    }
                }
            });

            Button setAnswerLimitMin = (Button) rootView.findViewById(R.id.devicePanel_adminFragment_LimitMinButton);
            setAnswerLimitMin.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    answerLimitMin = Integer.parseInt(answerLimitMinEditText.getText().toString());
                }
            });

            Button setAnswerLimitMax = (Button) rootView.findViewById(R.id.devicePanel_adminFragment_LimitMaxButton);
            setAnswerLimitMax.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    answerLimitMax = Integer.parseInt(answerLimitMaxEditText.getText().toString());
                }
            });

            Button setTimeout = (Button) rootView.findViewById(R.id.devicePanel_adminFragment_timeoutButton);
            setTimeout.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    try {
                        int timeout = Integer.parseInt(timeoutEditText.getText().toString());
                        device.set_timeout_millis(timeout);
                    } catch (DevFailed e) {
                        e.printStackTrace();
                    }
                }
            });

            Button deviceInfo = (Button) rootView.findViewById(R.id.devicePanel_adminFragment_deviceInfoButton);
            deviceInfo.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    try {
                        long t0 = System.currentTimeMillis();
                        DevInfo out = device.info();
                        long t1 = System.currentTimeMillis();
                        AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
                        builder.setTitle("Device Info");
                        String message = new String("Command: " + device.name() + "/Info\n");
                        message = message + "Duration: " + (t1 - t0) + " msec\n\n";
                        message = message + "Server: " + out.server_id + "\n";
                        message = message + "Server host: " + out.server_host + "\n";
                        message = message + "Server version: " + out.server_version + "\n";
                        message = message + "Class: " + out.dev_class + "\n";
                        message = message + out.doc_url + "\n";
                        builder.setMessage(message);
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    } catch (DevFailed e) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
                        builder.setTitle("Command error");
                        builder.setMessage(e.getMessage());
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                        e.printStackTrace();
                    }
                }
            });

            Button pingDevice = (Button) rootView.findViewById(R.id.devicePanel_adminFragment_pingDeviceButton);
            pingDevice.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    try {
                        long t0 = System.currentTimeMillis();
                        device.ping();
                        long t1 = System.currentTimeMillis();
                        AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
                        builder.setTitle("Command: " + device.name() + "/Ping\n");
                        String message = new String("Duration: " + (t1 - t0) + " msec\n\n");
                        message = message + "Device is alive\n";

                        builder.setMessage(message);
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();

                    } catch (DevFailed e) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
                        builder.setTitle("Command error");
                        builder.setMessage(e.getMessage());
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                        e.printStackTrace();
                    }
                }
            });

            Button pollingStatus = (Button) rootView.findViewById(R.id.devicePanel_adminFragment_pollStButton);
            pollingStatus.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    try {
                        DeviceData argin = new DeviceData();
                        argin.insert(device.name());
                        DeviceData argout = deviceAdm.command_inout("DevPollStatus", argin);
                        String[] pollStatus = argout.extractStringArray();
                        String message = new String();
                        for (int i = 0; i < pollStatus.length; i++) {
                            message = message + pollStatus[i] + "\n\n";
                        }
                        AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
                        builder.setTitle("Polling status");
                        builder.setMessage(message);
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    } catch (DevFailed e) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
                        builder.setTitle("Command error");
                        builder.setMessage(e.getMessage());
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                        e.printStackTrace();
                    }
                }
            });

            Button restart = (Button) rootView.findViewById(R.id.devicePanel_adminFragment_restartButton);
            restart.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    DeviceData argin;
                    try {
                        argin = new DeviceData();
                        argin.insert(device.name());
                        deviceAdm.command_inout("DevRestart", argin);
                        AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
                        builder.setTitle("Restart command");
                        builder.setMessage("Restart OK\n\n");
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    } catch (DevFailed e) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
                        builder.setTitle("Command error");
                        builder.setMessage(e.getMessage());
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                        e.printStackTrace();
                    }
                }
            });

        } catch (DevFailed e) {
            e.printStackTrace();
        }
        return rootView;
    }

    public int getAnswerLimitMin() {
        return answerLimitMin;
    }

    public int getAnswerLimitMax() {
        return answerLimitMax;
    }
}
