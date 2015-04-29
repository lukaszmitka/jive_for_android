package pl.edu.uj.synchrotron.jive;

import android.util.Log;

import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.CommandInfo;
import fr.esrf.TangoApi.DeviceData;
import fr.esrf.TangoApi.DeviceProxy;
import fr.esrf.TangoDs.TangoConst;

/**
 * Created by lukasz on 29.04.15.
 * This file is element of RESTful Jive application project.
 * You are free to use, copy and edit whole application or any of its components.
 * Application comes with no warranty. Altough author is trying to make it best, it may work or it may not work.
 */
public class CommandExecuteRunnable implements Runnable, TangoConst {
private String tangoHost, tangoPort, deviceName, argument, command;
private ATKPanelCallback activityCallback;

public CommandExecuteRunnable(ATKPanelCallback callback, String devName, String host, String port,
                              String commandName, String inputArgument) {
	tangoHost = host;
	tangoPort = port;
	deviceName = devName;
	activityCallback = callback;
	argument = inputArgument;
	command = commandName;
	Log.d("CommandExecuteRunnable", "Created instance, arguments: devName: " + devName + " TANGO_HOST: " + host + ":" +
			port);
}

@Override
public void run() {
	try {
		Log.d("CommandExecuteRunnable", "Connecting to device: " + deviceName);
		DeviceProxy dp = new DeviceProxy(deviceName, tangoHost, tangoPort);
		dp.command_list_query();
		CommandInfo ci = dp.command_query(command);
		DeviceData commandInput = new DeviceData();
		commandInput = TangoDataProcessing.insertData(argument, commandInput, ci.in_type);
		DeviceData commandOutput = dp.command_inout(command, commandInput);

		if (ci.out_type == Tango_DEV_VOID) {
			activityCallback.toastMessage("Command OK");
		} else {
			String commandOut;
			commandOut = TangoDataProcessing.extractData(commandOutput, ci.out_type);
			activityCallback.commandOutput(commandOut);
		}
	} catch (NumberFormatException e) {
		activityCallback.displayErrorMessage("Number format exception");
	} catch (DevFailed e) {
		Log.d("CommandExecuteRunnable", "Problem occured while connecting with device: " + deviceName);
		activityCallback.displayErrorMessage("Number format exception", e);
		e.printStackTrace();
	}
}
}
