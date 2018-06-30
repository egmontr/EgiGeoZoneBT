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

package de.egi.geofence.geozone.bt.beacon;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.egi.geofence.geozone.bt.EgiGeoZoneApplication;
import de.egi.geofence.geozone.bt.R;
import de.egi.geofence.geozone.bt.db.DbGlobalsHelper;
import de.egi.geofence.geozone.bt.utils.Constants;
import de.egi.geofence.geozone.bt.utils.Utils;

public class BeaconScanAll extends AppCompatActivity {
	private ListView listViewBeacons = null;
	private TextView wait = null;
	// Identifier.parse("Here is my UUID"), null, null);
	private static final Region mRegion = new Region(EgiGeoZoneApplication.EGIGEOZONE, null, null, null);
	private BeaconManager mBeaconManager;
	private final List<String> beaconsListe = new ArrayList<>();
	private ArrayList<BeaconItem> beaconItems;
	private BeaconListAdapter adapter;
	private boolean btScanStarted = false;
	private DbGlobalsHelper dbGlobalsHelper;

	public void onCreate(Bundle savedInstanceState) {

		// https://stackoverflow.com/questions/27964761/all-beacons-are-not-shown-in-android-using-altbeacon-library

		// http://stackoverflow.com/questions/27054324/altbeacon-scan-multiple-uuid-in-an-array
		// http://stackoverflow.com/questions/26993654/altbeacon-scanning-ibeacon-on-android
		// http://docwiki.embarcadero.com/RADStudio/XE8/de/Verwenden_von_Beacons

		super.onCreate(savedInstanceState);
		Utils.onActivityCreateSetTheme(this);
		setContentView(R.layout.beacons_scan);

		dbGlobalsHelper = new DbGlobalsHelper(this);

		verifyBluetooth();

		mBeaconManager = BeaconManager.getInstanceForApplication(this);

		beaconItems = new ArrayList<>();

		listViewBeacons = (ListView) findViewById(R.id.listView_beacons);
        adapter = new BeaconListAdapter(getApplicationContext(), beaconItems);
		wait = (TextView) findViewById(R.id.listText_wait);
		listViewBeacons.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				BeaconItem beaconItem = (BeaconItem) listViewBeacons.getItemAtPosition(position);
				int size = beaconItem.getBeacon().getIdentifiers().size();
				String itemValue = null;
				if (size == 1){
					itemValue = beaconItem.getBeacon().getIdentifier(0) + "#" + "" + "#" + "";
					Log.i("BeaconScanAll", beaconItem.getBeacon().getIdentifier(0) + "#" + "" + "#" + "" + " distance: " + beaconItem.getBeacon().getDistance());
				}else if (size == 2){
					itemValue = beaconItem.getBeacon().getIdentifier(0) + "#" + beaconItem.getBeacon().getIdentifier(1) + "#" + "";
					Log.i("BeaconScanAll", beaconItem.getBeacon().getIdentifier(0) + "#" + beaconItem.getBeacon().getIdentifier(1) + "#" + "" + " distance: " + beaconItem.getBeacon().getDistance());
				}else if (size == 3){
					itemValue = beaconItem.getBeacon().getIdentifier(0) + "#" + beaconItem.getBeacon().getIdentifier(1) + "#" + beaconItem.getBeacon().getIdentifier(2);
					Log.i("BeaconScanAll", beaconItem.getBeacon().getIdentifier(0) + "#" + beaconItem.getBeacon().getIdentifier(1) + "#" + beaconItem.getBeacon().getIdentifier(2) + " distance: " + beaconItem.getBeacon().getDistance());
				}
				if (beaconItem.getBeacon().getBluetoothAddress() != null){
					itemValue = itemValue + "#" + beaconItem.getBeacon().getBluetoothAddress();
				}
				Intent data = new Intent();
				data.putExtra("beacon", itemValue);
				setResult(RESULT_OK, data);
				finish();
			}
		});

		beaconItems.clear();
		onBeaconServiceConnect();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			mBeaconManager.startRangingBeaconsInRegion(mRegion);
			if (btScanStarted){
				dbGlobalsHelper.storeGlobals(Constants.DB_KEY_BEACON_SCAN, "false");
				((EgiGeoZoneApplication) getApplication()).unbind();
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private void onBeaconServiceConnect() {
		try {
			// Scan lasts for SCAN_PERIOD time
			mBeaconManager.setForegroundScanPeriod(5000L);
			// Wait every SCAN_PERIOD_INBETWEEN time
			mBeaconManager.setForegroundBetweenScanPeriod(0L);
			// Update default time with the new one
			mBeaconManager.updateScanPeriods();

			mBeaconManager.startRangingBeaconsInRegion(mRegion);
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		// Set Ranging
		mBeaconManager.addRangeNotifier(new RangeNotifier() {
			@Override
			public void didRangeBeaconsInRegion(final Collection<Beacon> beacons, Region region) {

				if (beacons != null && beacons.size() > 0) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (beaconsListe.size() == 0){
								for (Beacon beacon : beacons) {
//									int size = beacon.getIdentifiers().size();
									boolean gef = false;
									for (BeaconItem beaconItem : beaconItems){
										if (beaconItem.getBeacon().getBluetoothAddress().equals(beacon.getBluetoothAddress())){
											gef = true;
											break;
										}
									}
									if (!gef) beaconItems.add(new BeaconItem(beacon));

									listViewBeacons.setAdapter(adapter);
								}
								wait.setText("");
							}
						}
					});
				}
			}
		});
	}


	@SuppressLint("NewApi")
	private void verifyBluetooth() {
		try {
			if (!BeaconManager.getInstanceForApplication(this).checkAvailability()) {
				final AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.bluetoothNotEnabled);
				builder.setMessage(R.string.bluetoothWillBeEnabled);
				builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						((BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter().enable();
                        verifyBluetoothScanOn();
                    }
				});
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        finish();
                    }
				});
				builder.show();
			}else{
                verifyBluetoothScanOn();
            }
		}
		catch (RuntimeException e) {
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.bluetoothLENotAvailable);
			builder.setMessage(R.string.bluetoothLeNotSupported);
			builder.setPositiveButton(android.R.string.ok, null);
			builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					finish();
				}
			});
			builder.show();
		}
	}

	@SuppressLint("NewApi")
	private void verifyBluetoothScanOn() {
		try {
			if (!Utils.isBoolean(dbGlobalsHelper.getCursorGlobalsByKey(Constants.DB_KEY_BEACON_SCAN))) {
				btScanStarted = true;
				dbGlobalsHelper.storeGlobals(Constants.DB_KEY_BEACON_SCAN, "true");
				((EgiGeoZoneApplication) getApplication()).bind();
					}
//			if (!BeaconManager.getInstanceForApplication(this).isAnyConsumerBound()) {
//				final AlertDialog.Builder builder = new AlertDialog.Builder(this);
//				builder.setTitle(R.string.BtScanNotEnabled);
//				builder.setMessage(R.string.BtScanNotEnabledSettings);
//				builder.setPositiveButton(android.R.string.ok, null);
//
//				builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
//					@Override
//					public void onDismiss(DialogInterface dialog) {
//						finish();
//					}
//				});
//				builder.show();
//			}
		}
		catch (RuntimeException e) {
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.bluetoothLENotAvailable);
			builder.setMessage(R.string.bluetoothLeNotSupported);
			builder.setPositiveButton(android.R.string.ok, null);
			builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					finish();
				}
			});
			builder.show();
		}
	}
}