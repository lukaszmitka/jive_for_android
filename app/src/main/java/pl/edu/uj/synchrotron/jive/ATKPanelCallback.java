package pl.edu.uj.synchrotron.jive;

import java.util.HashMap;

import fr.esrf.Tango.DevFailed;

/**
 * Created by lukasz on 23.04.15.
 * This file is element of RESTful Jive application project.
 * You are free to use, copy and edit whole application or any of its components.
 * Application comes with no warranty. Altough author is trying to make it best, it may work or it may not work.
 */
public interface ATKPanelCallback {

boolean areThreadsInterrupted();

void updatePlotView(double[][] plotValues);

void updatePlotView(Number[] series, String label);

void updateScalarListView(HashMap<String, String> response);

void populateAttributeSpinner(HashMap<String, String> response);

void populateStatus(String status);

void displayErrorMessage(final String message, DevFailed e);

void displayErrorMessage(final String message);

void populateCommandSpinner(String[] commandNames, int[] commandInType, int[] commandOutType, int commandCount);

void commandOutput(String message);

void toastMessage(String message);
}
