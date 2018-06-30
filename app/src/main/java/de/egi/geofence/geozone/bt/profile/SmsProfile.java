/*
* Copyright 2014 - 2015 Egmont R. (egmontr@gmail.com)
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package de.egi.geofence.geozone.bt.profile;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.apache.log4j.Logger;

import de.egi.geofence.geozone.bt.GlobalSingleton;
import de.egi.geofence.geozone.bt.InfoReplace;
import de.egi.geofence.geozone.bt.R;
import de.egi.geofence.geozone.bt.Worker;
import de.egi.geofence.geozone.bt.WorkerMain;
import de.egi.geofence.geozone.bt.db.DbSmsHelper;
import de.egi.geofence.geozone.bt.db.SmsEntity;
import de.egi.geofence.geozone.bt.db.ZoneEntity;
import de.egi.geofence.geozone.bt.utils.Constants;
import de.egi.geofence.geozone.bt.utils.NotificationUtil;
import de.egi.geofence.geozone.bt.utils.Utils;

@SuppressWarnings("deprecation")
public class SmsProfile extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
	private DbSmsHelper datasource;
	private String aktion;
	private String ind;
	private boolean _update = false;
	private CheckBox enter = null;
	private CheckBox exit = null;
	private View viewMerk;
	private GoogleApiClient mLocationClient;
	private final Logger log = Logger.getLogger(ServerProfile.class);

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Utils.onActivityCreateSetTheme(this);
		setContentView(R.layout.sms_profile);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		Utils.changeBackGroundToolbar(this, toolbar);

		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_mail_profile);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// Speichern
				viewMerk = view;
				saveSms();
			}
		});


//		if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
//			// Check Permissions Now
//			if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS)) {
//				// Display UI and wait for user interaction
//				AlertDialog.Builder alertDialogBuilder = Utils.onAlertDialogCreateSetTheme(this);
//				alertDialogBuilder.setMessage(getString(R.string.checkSMS));
//				alertDialogBuilder.setTitle(getString(R.string.titleSMS));
//
//				alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//					@Override
//					public void onClick(DialogInterface arg0, int arg1) {
//						ActivityCompat.requestPermissions(SmsProfile.this, new String[]{Manifest.permission.SEND_SMS}, MainEgiGeoZone.REQUEST_SMS);
//					}
//				});
//				AlertDialog alertDialog = alertDialogBuilder.create();
//				alertDialog.show();
//
//			} else {
//				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, MainEgiGeoZone.REQUEST_SMS);
//			}
//		} else {
//			// permission has been granted, continue as usual
////			Toast.makeText(this,"permission has been granted, continue as usual",Toast.LENGTH_LONG).show();
//		}


		datasource = new DbSmsHelper(this);

		enter = ((CheckBox) this.findViewById(R.id.chk_enter));
		exit = ((CheckBox) this.findViewById(R.id.chk_exit));

		// Receiver registrieren
		IntentFilter testOkStatusFilter = new IntentFilter(Constants.ACTION_TEST_STATUS_OK);
		this.registerReceiver(mReceiver, testOkStatusFilter);
		IntentFilter testNokStatusFilter = new IntentFilter(Constants.ACTION_TEST_STATUS_NOK);
		this.registerReceiver(mReceiver, testNokStatusFilter);

		Bundle b = getIntent().getExtras();
		if (null != b){
			aktion = b.getString("action");
			ind = b.getString("ind");
		}


		if (aktion.equalsIgnoreCase("new")){
			// Felder leer lassen
//			boolean _new = true;
			enter.setChecked(true);
			exit.setChecked(true);
		}else if (aktion.equalsIgnoreCase("update")){
			_update = true;
		}

		if (_update){
			// Satz aus der DB lesen
			SmsEntity se = datasource.getCursorSmsById(Integer.valueOf(ind));
			((TextView) this.findViewById(R.id.value_name)).setText(se.getName());
			((EditText) this.findViewById(R.id.value_sms_to)).setText(se.getNumber());
			((EditText) this.findViewById(R.id.value_sms_text)).setText(se.getText());
			enter.setChecked(se.isEnter());
			exit.setChecked(se.isExit());
		}
	}
	private void saveSms(){
		if (checkInputFields()) {
			return;
		}

		SmsEntity smsEntity = new SmsEntity();
		if (_update){
			smsEntity.setId(Integer.parseInt(ind));
		}
		smsEntity.setName(((EditText) this.findViewById(R.id.value_name)).getText().toString());
		smsEntity.setNumber(((EditText) this.findViewById(R.id.value_sms_to)).getText().toString());
		smsEntity.setText(((EditText) this.findViewById(R.id.value_sms_text)).getText().toString());

		if (enter.isChecked()){
			smsEntity.setEnter(true);
		}else{
			smsEntity.setEnter(false);
		}

		if (exit.isChecked()){
			smsEntity.setExit(true);
		}else{
			smsEntity.setExit(false);
		}

		datasource.storeSms(smsEntity);

		setResult(RESULT_OK);
		finish();
	}

	/**
	 * Check all the input values and flag those that are incorrect
	 * @return true if all the widget values are correct; otherwise false
	 */
	private boolean checkInputFields() {
		// Start with the input validity flag set to true
		if (TextUtils.isEmpty(((EditText) this.findViewById(R.id.value_name)).getText().toString())) {
			this.findViewById(R.id.value_name).setBackgroundColor(Color.RED);
//			Toast.makeText(this, R.string.geofence_input_error_missing, Toast.LENGTH_LONG).show();
			Snackbar.make(viewMerk, R.string.geofence_input_error_missing, Snackbar.LENGTH_LONG).show();
			return true;
		} else if (((EditText) findViewById(R.id.value_name)).getText().toString().contains(",")){
			findViewById(R.id.value_name).setBackgroundColor(Color.RED);
			Snackbar.make(viewMerk, R.string.geofence_input_error_comma, Snackbar.LENGTH_LONG).show();
			return true;
		} else if (((EditText) findViewById(R.id.value_name)).getText().toString().contains("'")){
			findViewById(R.id.value_name).setBackgroundColor(Color.RED);
			Snackbar.make(viewMerk, R.string.geofence_input_error_comma, Snackbar.LENGTH_LONG).show();
			return true;
		}

		if (TextUtils.isEmpty(((EditText) this.findViewById(R.id.value_sms_to)).getText().toString())) {
			this.findViewById(R.id.value_sms_to).setBackgroundColor(Color.RED);
//			Toast.makeText(this, R.string.geofence_input_error_missing, Toast.LENGTH_LONG).show();
			Snackbar.make(viewMerk, R.string.geofence_input_error_missing, Snackbar.LENGTH_LONG).show();
			return true;
		}

		if (TextUtils.isEmpty(((EditText) this.findViewById(R.id.value_sms_text)).getText().toString())) {
			this.findViewById(R.id.value_sms_text).setBackgroundColor(Color.RED);
//			Toast.makeText(this, R.string.geofence_input_error_missing, Toast.LENGTH_LONG).show();
			Snackbar.make(viewMerk, R.string.geofence_input_error_missing, Snackbar.LENGTH_LONG).show();
			return true;
		}
		// If everything passes, the validity flag will still be true, otherwise it will be false.
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_profil_del_help, menu);
		return super.onCreateOptionsMenu(menu);
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action buttons
		switch(item.getItemId()) {
			case R.id.menu_delete_profile_log:
				AlertDialog.Builder ab = Utils.onAlertDialogCreateSetTheme(this);
				ab.setMessage(R.string.action_delete).setPositiveButton(R.string.action_yes, dialogClickListener).setNegativeButton(R.string.action_no, dialogClickListener).show();
				return true;
			case R.id.menu_help:
				Intent i = new Intent(this, InfoReplace.class);
				startActivity(i);
				return true;
			case R.id.menu_test:
				if (checkInputFields()) {
					return true;
				}

//				boolean ret = checkPermissionsLocation();
//				if (ret) return true;

				mLocationClient = new GoogleApiClient.Builder(this, this, this).addApi(LocationServices.API).build();
				mLocationClient.connect();

				return true;
			// Pass through any other request
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private final DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which){
				case DialogInterface.BUTTON_POSITIVE:
					//Do your Yes progress
					try{
						datasource.deleteSms(ind);
					}catch(Exception e){
						Toast.makeText(getApplicationContext(), R.string.profile_in_use, Toast.LENGTH_LONG).show();
					}
					finish();
					break;

				case DialogInterface.BUTTON_NEGATIVE:
					//Do your No progress
					break;
			}
		}
	};

	//The BroadcastReceiver that listens for broadcasts
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			GlobalSingleton.getInstance().setTestResultError(false);
			String result = intent.getStringExtra("TestResult") != null ? intent.getStringExtra("TestResult") : "";
			showAlert(action, result);
		}
	};

	private void showAlert(String action, String result){
		if (Constants.ACTION_TEST_STATUS_OK.equals(action)) {
			// Teststatus anzeigen
			AlertDialog.Builder ab = Utils.onAlertDialogCreateSetTheme(this);
			ab.setMessage(R.string.test_ok_text).setPositiveButton(R.string.action_ok, testDialogClickListener).setTitle(R.string.test_ok_title).setIcon(R.drawable.ic_lens_green_24dp).show();
		}
		if (Constants.ACTION_TEST_STATUS_NOK.equals(action)) {
			// Teststatus anzeigen
			AlertDialog.Builder ab = Utils.onAlertDialogCreateSetTheme(this);
			ab.setMessage(result).setPositiveButton(R.string.action_ok, testDialogClickListener)
					.setTitle(R.string.test_nok_title).setIcon(R.drawable.ic_lens_red_24dp).show();
		}

	}

	// Dialog für TestErgebnis
	private final DialogInterface.OnClickListener testDialogClickListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which){
				case DialogInterface.BUTTON_POSITIVE:
					break;
			}
		}
	};

//	public void onRequestPermissionsResult(final int requestCode, @NonNull String[] permissions, int[] grantResults) {
////        http://stackoverflow.com/questions/30719047/android-m-check-runtime-permission-how-to-determine-if-the-user-checked-nev
//		String perm = "";
//		String title = "";
//		String descNeverAsk = "";
//		String descAsk = "";
//		boolean goToSettings = false;
//
//		if (requestCode == MainEgiGeoZone.REQUEST_SMS) {
////            goToSettings = true;
//			perm = Manifest.permission.SEND_SMS;
//			descAsk = getString(R.string.descAskSMS);
//			descNeverAsk = getString(R.string.descNeverAskSMS);
//			title = getString(R.string.titleSMS);
//		}
//
//		if (requestCode == MainEgiGeoZone.REQUEST_LOCATION) {
//			perm = Manifest.permission.ACCESS_FINE_LOCATION;
//			descAsk = getString(R.string.descAskLocation);
//			descNeverAsk = getString(R.string.descNeverAskLocation);
//			title = getString(R.string.titleLocation);
//		}
//
//		if(grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//			// We can now safely use the API we requested access to
////			Toast.makeText(this,"We can now safely use the API we requested access to",Toast.LENGTH_LONG).show();
//		} else {
//			// Permission was denied or request was cancelled
//			boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0]);
//			if (!showRationale) {
////				Toast.makeText(this, "Permission was denied with flag NEVER ASK AGAIN",Toast.LENGTH_LONG).show();
//				// user denied flagging NEVER ASK AGAIN
//				// you can either enable some fall back,
//				// disable features of your app
//				// or open another dialog explaining
//				// again the permission and directing to
//				// the app setting
//				AlertDialog.Builder alertDialogBuilder = Utils.onAlertDialogCreateSetTheme(this);
//				alertDialogBuilder.setMessage(descNeverAsk);
//				alertDialogBuilder.setTitle(title);
//
//				final boolean finalGoToSettings = goToSettings;
//				alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//					@Override
//					public void onClick(DialogInterface arg0, int arg1) {
////						Toast.makeText(SmsProfile.this,"You clicked yes button",Toast.LENGTH_LONG).show();
//						// Benutzer hat mit nicht mehr fragen geantwortet.
//						// Dialog mit Erklärung und zu den App-Settings leiten
//						if (finalGoToSettings) {
//							Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//							Uri uri = Uri.fromParts("package", getPackageName(), null);
//							intent.setData(uri);
//							startActivityForResult(intent, 3);
//						}
//					}
//				});
//				AlertDialog alertDialog = alertDialogBuilder.create();
//				alertDialog.show();
//
//			} else{
//				// user denied WITHOUT never ask again
//				// this is a good place to explain the user
//				// why you need the permission and ask if he want
//				// to accept it (the rationale)
////				Toast.makeText(this, "Permission was denied or request was cancelled",Toast.LENGTH_LONG).show();
//				// Nochmals Dialog mit Erklärung und dann wieder anfragen
//				AlertDialog.Builder alertDialogBuilder = Utils.onAlertDialogCreateSetTheme(this);
//				alertDialogBuilder.setMessage(descAsk);
//				alertDialogBuilder.setTitle(title);
//
//				final String finalPerm = perm;
//				alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//					@Override
//					public void onClick(DialogInterface arg0, int arg1) {
//						ActivityCompat.requestPermissions(SmsProfile.this, new String[]{finalPerm}, requestCode);
////						Toast.makeText(SmsProfile.this,"You clicked yes button",Toast.LENGTH_LONG).show();
//					}
//				});
//				AlertDialog alertDialog = alertDialogBuilder.create();
//				alertDialog.show();
//			}
//		}
//	}
//


//	private boolean checkPermissionsLocation() {
//		//noinspection StatementWithEmptyBody
//		if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//			// Check Permissions Now
//			if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
//				// Display UI and wait for user interaction
//				android.support.v7.app.AlertDialog.Builder alertDialogBuilder = Utils.onAlertDialogCreateSetTheme(this);
//				alertDialogBuilder.setMessage(getString(R.string.checkLocation));
//				alertDialogBuilder.setTitle(getString(R.string.titleLocation));
//
//				alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//					@Override
//					public void onClick(DialogInterface arg0, int arg1) {
//						ActivityCompat.requestPermissions(SmsProfile.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MainEgiGeoZone.REQUEST_LOCATION);
//					}
//				});
//				android.support.v7.app.AlertDialog alertDialog = alertDialogBuilder.create();
//				alertDialog.show();
//				return true;
//			} else {
//				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MainEgiGeoZone.REQUEST_LOCATION);
//			}
//		} else {
//			// permission has been granted, continue as usual
////            Toast.makeText(this, "permission has been granted, continue as usual", Toast.LENGTH_LONG).show();
//		}
//		return false;
//	}
//


	@SuppressLint("SetTextI18n")
	@Override
	public void onConnected(@Nullable Bundle bundle) {
		// If Google Play Services is available
		if (servicesConnected()) {
			// Get the current location
			Location currentLocation = null;
			try{
				currentLocation = LocationServices.FusedLocationApi.getLastLocation(mLocationClient);
			}catch(SecurityException se){
				// Display UI and wait for user interaction
				android.support.v7.app.AlertDialog.Builder alertDialogBuilder = Utils.onAlertDialogCreateSetTheme(this);
				alertDialogBuilder.setMessage(getString(R.string.alertPermissions));
				alertDialogBuilder.setTitle(getString(R.string.titleAlertPermissions));

				alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
					}
				});
				android.support.v7.app.AlertDialog alertDialog = alertDialogBuilder.create();
				alertDialog.show();
			}

			if (currentLocation != null){
				// Start test
				log.debug("onConnected - location: " + (Double.valueOf(currentLocation.getLatitude()).toString()) + "##" + (Double.valueOf(currentLocation.getLongitude()).toString()));
				doTest(currentLocation);
			}else{
				Toast.makeText(this, "Could not determine location. ", Toast.LENGTH_LONG).show();
				log.error("Could not determine location.");
			}
		}
		mLocationClient.disconnect();
	}

	@Override
	public void onConnectionSuspended(int i) {

	}

	@Override
	public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

	}
	/**
	 * Verify that Google Play services is available before making a request.
	 *
	 * @return true if Google Play services is available, otherwise false
	 */
	private boolean servicesConnected() {
		log.debug("servicesConnected");
		// Check that Google Play services is available
		GoogleApiAvailability api = GoogleApiAvailability.getInstance();
		int code = api.isGooglePlayServicesAvailable(this);
		// If Google Play services is available
		if (ConnectionResult.SUCCESS == code) {
			// In debug mode, log the status
			Log.d(Constants.APPTAG, getString(R.string.play_services_available));
			log.info("servicesConnected result from Google Play Services: " + getString(R.string.play_services_available));
			// Continue
			return true;
			// Google Play services was not available for some reason
		} else if (api.isUserResolvableError(code)){
			log.error("servicesConnected result: could not connect to Google Play services");
			api.showErrorDialogFragment(this, code, Constants.PLAY_SERVICES_RESOLU‌​TION_REQUEST);
		} else {
			log.error("servicesConnected result: could not connect to Google Play services");
			Toast.makeText(this, api.getErrorString(code), Toast.LENGTH_LONG).show();
		}
		return false;
	}

	private void doTest(Location currentLocation){

		String to = ((EditText) this.findViewById(R.id.value_sms_to)).getText().toString();
		String text = ((EditText) this.findViewById(R.id.value_sms_text)).getText().toString();

		ZoneEntity ze = Utils.makeTestZone();

		String textReplace = Utils.replaceAll(this, text, ze.getName(), ze.getAlias(), 1, ze.getRadius(),
				(Double.valueOf(currentLocation.getLatitude()).toString()), (Double.valueOf(currentLocation.getLongitude()).toString()),
				(Double.valueOf(currentLocation.getLatitude()).toString()), (Double.valueOf(currentLocation.getLongitude()).toString()),
				null, null, (Float.valueOf(currentLocation.getAccuracy()).toString()));

		String textNeu = textReplace.length() > 155 ? textReplace.substring(0,155) : textReplace;

		WorkerMain worker = new WorkerMain(this.getApplicationContext());
		try {
			worker.doSendSms(this.getApplicationContext(), Constants.TEST_ZONE, to, textNeu, true);
		} catch (Exception ex) {
			Log.e(Constants.APPTAG, "error sending test sms", ex);
			NotificationUtil.showError(this.getApplicationContext(), "TestSms" + ": Error sending test sms", ex.toString());
		}
	}
	@Override
	protected void onPause() {
		super.onPause();
		try {
		this.unregisterReceiver(mReceiver);
		} catch (Exception e) {
		}
	}
	@Override
	protected void onResume() {
		super.onResume();
		// Receiver registrieren
		IntentFilter testOkStatusFilter = new IntentFilter(Constants.ACTION_TEST_STATUS_OK);
		this.registerReceiver(mReceiver, testOkStatusFilter);
		IntentFilter testNokStatusFilter = new IntentFilter(Constants.ACTION_TEST_STATUS_NOK);
		this.registerReceiver(mReceiver, testNokStatusFilter);
	}
}