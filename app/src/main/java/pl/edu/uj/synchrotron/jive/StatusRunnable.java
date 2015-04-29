package pl.edu.uj.synchrotron.jive;

import android.util.Log;

import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.CommandInfo;
import fr.esrf.TangoApi.DeviceData;
import fr.esrf.TangoApi.DeviceProxy;
import fr.esrf.TangoDs.TangoConst;

/**
 * Created by lukasz on 23.04.15.
 * This file is element of RESTful Jive application project.
 * You are free to use, copy and edit whole application or any of its components.
 * Application comes with no warranty. Altough author is trying to make it best, it may work or it may not work.
 */

// define thread for refreshing attribute values
public class StatusRunnable implements Runnable, TangoConst {
private String tangoHost;
private String tangoPort;
private String deviceName;
private ATKPanelCallback activityCallback;
private int refreshing;

public StatusRunnable(ATKPanelCallback callback, String devName, String host, String port, int refreshingPeriod) {
	tangoHost = host;
	tangoPort = port;
	deviceName = devName;
	activityCallback = callback;
	refreshing = refreshingPeriod;
	Log.d("StatusRunnable", "Created instance, arguments: devName: " + devName + " TANGO_HOST: " + host + ":" + port);
}

@Override
public void run() {
	while (!activityCallback.areThreadsInterrupted()) {
		Log.d("StatusRunnable.run()", "No interrupt");
		try {
			DeviceProxy dev = new DeviceProxy(deviceName, tangoHost, tangoPort);
			Log.d("StatusRunnable.run()", "Connected with device");
			CommandInfo commInfo = dev.command_query("Status");
			DeviceData argin = new DeviceData();
			String cmd = commInfo.cmd_name;
			DeviceData argout = dev.command_inout(cmd, argin);
			System.out.print("Output argument(s) :\n");
			String commandOut;
			commandOut = argout.extractString();
			System.out.print(commandOut);
			activityCallback.populateStatus(commandOut);
		} catch (DevFailed devFailed) {
			devFailed.printStackTrace();
		}
		try {
			Thread.sleep(refreshing);
		} catch (InterruptedException e) {
			Log.d("StatusRunnable.run()", "Thread interrupted while sleeping");
			Thread.currentThread().interrupt();
			return;
		}
	}
	Log.d("StatusRunnable.run()", "Interrupt occurred");
}
}
