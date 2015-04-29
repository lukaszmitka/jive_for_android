package pl.edu.uj.synchrotron.jive;

import android.util.Log;

import java.util.HashMap;

import fr.esrf.Tango.AttrDataFormat;
import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.AttributeInfo;
import fr.esrf.TangoApi.DeviceAttribute;
import fr.esrf.TangoApi.DeviceProxy;
import fr.esrf.TangoDs.TangoConst;

/**
 * Created by lukasz on 27.04.15.
 * This file is element of RESTful Jive application project.
 * You are free to use, copy and edit whole application or any of its components.
 * Application comes with no warranty. Altough author is trying to make it best, it may work or it may not work.
 */
public class AttributeListRunnable implements Runnable, TangoConst {

private String tangoHost;
private String tangoPort;
private String deviceName;
private ATKPanelCallback activityCallback;

public AttributeListRunnable(ATKPanelCallback callback, String devName, String host, String port) {
	tangoHost = host;
	tangoPort = port;
	deviceName = devName;
	activityCallback = callback;
	Log.d("AttributeListRunnable", "Created instance, arguments: devName: " + devName + " TANGO_HOST: " + host + ":" + port);
}

@Override
public void run() {
	HashMap<String, String> response = new HashMap<>();
	try {
		DeviceProxy dp = new DeviceProxy(deviceName, tangoHost, tangoPort);
		Log.d("AttributeListRunnable", "Getting attribute list");
		String att_list[] = dp.get_attribute_list();
		//System.out.println("Add attribute count to reply");
		response.put("attCount", "" + att_list.length);
		for (int i = 0; i < att_list.length; i++) {
			//System.out.println("Add attribute data to reply");
			response.put("attribute" + i, att_list[i]);
			//System.out.println("Getting attribute[" + i + "]: " + att_list[i]);
			try {
				DeviceAttribute da = dp.read_attribute(att_list[i]);
				//System.out.println("Getting attribute[" + i + "] info ");
				AttributeInfo ai = dp.get_attribute_info(att_list[i]);
				//System.out.println("Getting attribute format");
				if (ai.data_format.value() == AttrDataFormat._SCALAR) {
					response.put("attScalar" + i, "" + true);
				} else {
					response.put("attScalar" + i, "" + false);
				}
				//System.out.println("Getting attribute value");
				response.put("attValue" + i, TangoDataProcessing.extractDataValue(da, ai));
				//System.out.println("Getting attribute isWritable");
				response.put("attWritable" + i, "" + TangoDataProcessing.isWritable(ai));
				//System.out.println("Getting attribute isPlottable");
				response.put("attPlotable" + i, "" + TangoDataProcessing.isPlotable(ai));
				//System.out.println("Getting attribute description");
				response.put("attDesc" + i, "Name: " + ai.name + "\n" + "Label: " + ai.label + "\n"
						+ "Writable: " + TangoDataProcessing.getWriteString(ai) + "\n" + "Data format: " + TangoDataProcessing
						.getFormatString(ai) + "\n"
						+ "Data type: " + Tango_CmdArgTypeName[ai.data_type] + "\n" + "Max Dim X: " + ai.max_dim_x
						+ "\n" + "Max Dim Y: " + ai.max_dim_y + "\n" + "Unit: " + ai.unit + "\n" + "Std Unit: "
						+ ai.standard_unit + "\n" + "Disp Unit: " + ai.display_unit + "\n" + "Format: " + ai.format
						+ "\n" + "Min value: " + ai.min_value + "\n" + "Max value: " + ai.max_value + "\n"
						+ "Min alarm: " + ai.min_alarm + "\n" + "Max alarm: " + ai.max_alarm + "\n" + "Description: "
						+ ai.description);
			} catch (DevFailed e) {
				System.out.println("Attribute is unreadable, cause: " + e.getMessage());
				response.put("attScalar" + i, "" + true);
				response.put("attValue" + i, "Unreadable");
				response.put("attWritable" + i, "" + false);
				response.put("attPlotable" + i, "" + false);
				response.put("attDesc" + i, e.getMessage());
			}
			if (Thread.currentThread().isInterrupted()) {
				i = att_list.length;
			}
		}
		if (!(Thread.currentThread().isInterrupted() || activityCallback.areThreadsInterrupted())) {
			activityCallback.populateAttributeSpinner(response);
		}
	} catch (DevFailed e) {
		response.put("connectionStatus", "Unable to connect with device " + deviceName);
		Log.d("AttributeList.run()", "Problem occurred while connecting with device: " + deviceName);
		e.printStackTrace();
		activityCallback.displayErrorMessage("Unable to connect with device " + deviceName, e);
	}
}
}
