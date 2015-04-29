package pl.edu.uj.synchrotron.jive;

import android.util.Log;

import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.AttributeInfo;
import fr.esrf.TangoApi.DeviceAttribute;
import fr.esrf.TangoApi.DeviceProxy;

/**
 * Created by lukasz on 29.04.15.
 * This file is element of RESTful Jive application project.
 * You are free to use, copy and edit whole application or any of its components.
 * Application comes with no warranty. Altough author is trying to make it best, it may work or it may not work.
 */
public class AttributeUpdateRunnable implements Runnable {
private String tangoHost, tangoPort, deviceName, argument, attribute;
private ATKPanelCallback activityCallback;

public AttributeUpdateRunnable(ATKPanelCallback callback, String devName, String host, String port,
                               String attributeName, String inputArgument) {
	tangoHost = host;
	tangoPort = port;
	deviceName = devName;
	activityCallback = callback;
	argument = inputArgument;
	attribute = attributeName;
	Log.d("AttributeListRunnable", "Created instance, arguments: devName: " + devName + " TANGO_HOST: " + host + ":" + port);
}

@Override
public void run() {
	try {
		System.out.println("Connecting to device");
		DeviceProxy dp = new DeviceProxy(deviceName, tangoHost, tangoPort);
		DeviceAttribute da = dp.read_attribute(attribute);
		AttributeInfo ai = dp.get_attribute_info(attribute);
		da = TangoDataProcessing.insertData(argument, da, ai);
		dp.write_attribute(da);
		activityCallback.toastMessage("Attribute " + attribute + " updated");
	} catch (DevFailed e) {
		Log.d("CommandExecuteRunnable", "Problem occured while connecting with device: " + deviceName);
		activityCallback.displayErrorMessage("Number format exception", e);
		e.printStackTrace();
	}
}
}
