package pl.edu.uj.synchrotron.jive;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

/**
 * Activity for getting database host and port from user.
 */
public class SetHostActivity extends Activity {

@Override
protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_set_host);
}

@Override
public boolean onCreateOptionsMenu(Menu menu) {
	// Inflate the menu; this adds items to the action bar if it is present.
	getMenuInflater().inflate(R.menu.menu_set_host, menu);
	return true;
}

/**
 * Listener for the button click, get data from user and send them to parent
 * activity.
 *
 * @param view Reference to the widget that was clicked.
 */
public void buttonClickOk(View view) {
	Intent returnIntent = new Intent();
	EditText host = (EditText) findViewById(R.id.editTextTangoHost);
	EditText port = (EditText) findViewById(R.id.editTextTangoPort);
	String sHost = host.getText().toString();
	String sPort = port.getText().toString();
	returnIntent.putExtra("host", sHost);
	returnIntent.putExtra("port", sPort);
	setResult(RESULT_OK, returnIntent);
	finish();
}

/**
 * Listener for the button click, close the activity.
 *
 * @param view Reference to the widget that was clicked.
 */
public void buttonClickCancel(View view) {
	Intent returnIntent = new Intent();
	setResult(RESULT_CANCELED, returnIntent);
	finish();
}
}
