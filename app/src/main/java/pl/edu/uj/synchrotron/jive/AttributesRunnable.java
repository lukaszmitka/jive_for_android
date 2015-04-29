package pl.edu.uj.synchrotron.jive;

import android.util.Log;

import java.util.HashMap;

import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.AttributeInfo;
import fr.esrf.TangoApi.CommunicationFailed;
import fr.esrf.TangoApi.DeviceAttribute;
import fr.esrf.TangoApi.DeviceProxy;

/**
 * Created by lukasz on 27.04.15.
 * This file is element of RESTful Jive application project.
 * You are free to use, copy and edit whole application or any of its components.
 * Application comes with no warranty. Altough author is trying to make it best, it may work or it may not work.
 */
public class AttributesRunnable implements Runnable {

private String tangoHost;
private String tangoPort;
private String deviceName;
private ATKPanelCallback activityCallback;
private HashMap<String, String> hmRequest;
private int refreshing;

public AttributesRunnable(ATKPanelCallback callback, String devName, String host, String port, HashMap<String,
		String> request, int refreshingPeriod) {
	tangoHost = host;
	tangoPort = port;
	deviceName = devName;
	activityCallback = callback;
	hmRequest = request;
	refreshing = refreshingPeriod;
	Log.d("AttributesRunnable", "Created instance, arguments: devName: " + devName + " TANGO_HOST: " + host + ":" + port);
	Log.d("AttributesRunnable", "Received request: " + hmRequest);
}

@Override
public void run() {
	while (!(activityCallback.areThreadsInterrupted() || Thread.currentThread().isInterrupted())) {
		Log.d("AttributesRunnable.run", "Thread started");
		HashMap<String, String> response = new HashMap<>();
		int attCount = Integer.parseInt(hmRequest.get("attCount"));
		try {
			Log.d("AttributesRunnable.run", "Connecting to device");
			DeviceProxy dp = new DeviceProxy(deviceName, tangoHost, tangoPort);
			String attName;
			for (int i = 0; i < attCount; i++) {
				attName = hmRequest.get("att" + i);
				try {
					DeviceAttribute da = dp.read_attribute(attName);
					AttributeInfo ai = dp.get_attribute_info(attName);
					response.put("attValue" + i, TangoDataProcessing.extractDataValue(da, ai));
					response.put("attID" + i, hmRequest.get("attID" + i));
				} catch (CommunicationFailed e) {
					Log.d("AttributesRunnable.run", "Attribute unreadable, cause: " + e.getMessage());
					response.put("att" + i, "Attribute read error");
				}
			}
			response.put("attCount", "" + attCount);
			response.put("connectionStatus", "OK");
		} catch (DevFailed e) {
			response.put("connectionStatus", "Unable to connect with device " + deviceName);
			Log.d("AttributesRunnable.run", "Problem occured while connecting with device: " + deviceName);
			e.printStackTrace();
		}
		Log.d("AttributesRunnable.run", "Calling activityCallback.updateScalarListView(response)");
		if (!(activityCallback.areThreadsInterrupted() || Thread.currentThread().isInterrupted())) {
			activityCallback.updateScalarListView(response);
		} else {
			return;
		}
		try {
			Thread.sleep(refreshing);
		} catch (InterruptedException e) {
			Log.d("StatusRunnable.run()", "Thread interrupted while sleeping");
			Thread.currentThread().interrupt();
			return;
		}
	}
}
}

