package pl.edu.uj.synchrotron.jive;

import android.util.Log;

import fr.esrf.Tango.AttrDataFormat;
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
public class PlotRunnable implements Runnable {

private String tangoHost;
private String tangoPort;
private String deviceName;
private ATKPanelCallback activityCallback;
private String attributeName;
private int refreshing;

public PlotRunnable(ATKPanelCallback callback, String devName, String host, String port, String attName,
                    int refreshingPeriod) {
	tangoHost = host;
	tangoPort = port;
	deviceName = devName;
	activityCallback = callback;
	attributeName = attName;
	refreshing = refreshingPeriod;
	Log.d("PlotRunnable", "Created instance, arguments: devName: " + devName + " TANGO_HOST: " + host + ":" + port);
	Log.d("PlotRunnable", "Received attribute name: " + attributeName);
}

@Override
public void run() {
	while (!(activityCallback.areThreadsInterrupted() || Thread.currentThread().isInterrupted())) {
		Log.d("PlotRunnable.run()", "Thread started");
		try {
			System.out.println("Connecting to device");
			DeviceProxy dp = new DeviceProxy(deviceName, tangoHost, tangoPort);
			DeviceAttribute da = dp.read_attribute(attributeName);
			AttributeInfo ai = dp.get_attribute_info(attributeName);
			String plotLabel = ai.label + "[" + ai.unit + "]";
			switch (ai.data_format.value()) {
				case AttrDataFormat._SPECTRUM:
					double[] dataToPlot = TangoDataProcessing.extractSpectrumPlotData(da, ai);
					Double[] doubleToPlot = new Double[dataToPlot.length];
					for (int i = 0; i < dataToPlot.length; i++) {
						doubleToPlot[i] = new Double(dataToPlot[i]);
					}
					Log.d("PlotRunnable.run()", "Calling activityCallback.updatePlotView(doubleToPlot, plotLabel)");
					activityCallback.updatePlotView(doubleToPlot, plotLabel);
					break;
				case AttrDataFormat._IMAGE:
					double[][] doubleArray = TangoDataProcessing.extractImagePlotData(da, ai);
					Log.d("PlotRunnable.run()", "Calling activityCallback.updatePlotView(doubleArray)");
					activityCallback.updatePlotView(doubleArray);
					break;
				default:
					activityCallback.displayErrorMessage("Attribute can not be plotted");
					break;
			}
		} catch (DevFailed e) {
			activityCallback.displayErrorMessage("Device failed", e);
		}
		try {
			Thread.sleep(refreshing);
		} catch (InterruptedException e) {
			Log.d("PlotRunnable.run()", "Thread interrupted while sleeping");
			Thread.currentThread().interrupt();
			return;
		}
	}
}
}
