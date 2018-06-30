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

package de.egi.geofence.geozone.bt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.google.android.gms.location.Geofence;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import de.egi.geofence.geozone.bt.db.DbGlobalsHelper;
import de.egi.geofence.geozone.bt.db.ZoneEntity;
import de.egi.geofence.geozone.bt.geofence.GeofenceRequester;
import de.egi.geofence.geozone.bt.geofence.PathsenseGeofence;
import de.egi.geofence.geozone.bt.geofence.SimpleGeofence;
import de.egi.geofence.geozone.bt.utils.Constants;
import de.egi.geofence.geozone.bt.utils.Utils;

public class WorkerWifi {
	private final Context context;
	private final Logger log = Logger.getLogger(WorkerWifi.class);
	private GeofenceRequester mGeofenceRequester;
    private PathsenseGeofence mPathsenseGeofence;
    private DbGlobalsHelper dbGlobalsHelper;
    private WifiManager mWifiManager;
	private ZoneEntity ze;
    private int transition;
    private float accuracy;
    private String type;
    private String origin;
    private Location location;
    private String wifiMac = "";
	private boolean foundWifiMac = false;

	public WorkerWifi(Context context){
		this.context = context;
		// Instantiate a Geofence requester
		if (mGeofenceRequester == null){
			mGeofenceRequester = new GeofenceRequester(context);
		}
        if (mPathsenseGeofence == null){
            mPathsenseGeofence = new PathsenseGeofence(context);
        }
        dbGlobalsHelper = new DbGlobalsHelper(context);
	}

	/**
	 * Report geofence transitions to the UI
	 *
	 * context A Context for this component
	 * intent The Intent containing the transition
	 */
	public void handleTransitionWifi(int transition, ZoneEntity ze, String type, float accuracy, Location location, String origin) {
		Log.i(Constants.APPTAG, "in handleTransitionWifi");
		log.debug("in handleTransitionWifi");

        this.transition = transition;
        this.type = type;
        this.accuracy = accuracy;
        this.location = location;
        this.origin = origin;
        this.ze = ze;

		log.debug("W-01 Transition: " + transition);

		if (type.equalsIgnoreCase(Constants.GEOZONE) && Utils.isBoolean(dbGlobalsHelper.getCursorGlobalsByKey(Constants.DB_KEY_FALSE_POSITIVES))) {
            if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                String wifiInfo = ze.getWifi_info();
                if (!wifiInfo.contains("##")) {
                    log.debug("WifiMac not found. Continue without Wifi check!");
                    doWork();
                } else {
                    List<String> wifis = new ArrayList<>();
                    StringTokenizer st = new StringTokenizer(wifiInfo, "##");
                    while (st.hasMoreTokens()) {
                        wifis.add(st.nextToken());
                    }

                    if (wifis.size() == 2) {
                        wifiMac = wifis.get(1);
                    } else {
                        log.debug("W-02 WifiMac not found. Continue without Wifi check!");
                        doWork();
                    }
                }
            }else{
                doWork();
            }
			mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
			context.getApplicationContext().registerReceiver(mWifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
			mWifiManager.startScan();

		}else{
			log.debug("W-03 GeofencingFalsePositives handleTransitionGPS");
			doWork();
		}
	}

	private void doWork() {
		context.getApplicationContext().unregisterReceiver(mWifiScanReceiver);

        List<Geofence> currentGeofence = new ArrayList<>();

        if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            if (foundWifiMac) {
                // NOK
                // Return
                if (!Utils.isBoolean(dbGlobalsHelper.getCursorGlobalsByKey(Constants.DB_KEY_NEW_API))) {
                    currentGeofence.add(getGeofence(ze, Geofence.GEOFENCE_TRANSITION_EXIT));
                    mGeofenceRequester.setInProgressFlag(false);
                    mGeofenceRequester.addGeofences(currentGeofence);
                }else{
                    SimpleGeofence simpleGeofence = new SimpleGeofence(ze.getName(), ze.getLatitude(), ze.getLongitude(),
                            Integer.toString(ze.getRadius()), null, Geofence.NEVER_EXPIRE, Geofence.GEOFENCE_TRANSITION_EXIT, true, null, null);
                    mPathsenseGeofence.addGeofence(simpleGeofence);
                }
                log.debug("W-04 GeofencingFalsePositives Exit " + ze.getName() + " : False positives NOK " + origin + ": Wifi MAC found");
                return;
            } else {
                // OK
                log.debug("W-05 GeofencingFalsePositives DoubleCheck - OK - Enter event " + ze.getName() + " : - set Exit and continue");
                // Set Geofence Exit
                // Continue
                if (!Utils.isBoolean(dbGlobalsHelper.getCursorGlobalsByKey(Constants.DB_KEY_NEW_API))) {
                    currentGeofence.add(getGeofence(ze, Geofence.GEOFENCE_TRANSITION_EXIT));
                    mGeofenceRequester.setInProgressFlag(false);
                    mGeofenceRequester.addGeofences(currentGeofence);
                }else{
                    SimpleGeofence simpleGeofence = new SimpleGeofence(ze.getName(), ze.getLatitude(), ze.getLongitude(),
                            Integer.toString(ze.getRadius()), null, Geofence.NEVER_EXPIRE, Geofence.GEOFENCE_TRANSITION_EXIT, true, null, null);
                    mPathsenseGeofence.addGeofence(simpleGeofence);
                }
            }

        }

        WorkerMain workerMain = new WorkerMain(context);
        workerMain.doMainWork(ze, transition, type, accuracy, location, origin);

	}

    private Geofence getGeofence(ZoneEntity ze, int transition){
        return  new Geofence.Builder().setRequestId(ze.getName())
                .setTransitionTypes(transition)
                .setCircularRegion(Double.valueOf(ze.getLatitude()), Double.valueOf(ze.getLongitude()), ze.getRadius())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build();
    }


    private final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                List<ScanResult> mScanResults = mWifiManager.getScanResults();
                for (ScanResult scanResult : mScanResults){
					if (wifiMac.equals(scanResult.BSSID.toString())){
						foundWifiMac = true;
						break;
					}
				}
				doWork();
            }
        }
    };
}




















