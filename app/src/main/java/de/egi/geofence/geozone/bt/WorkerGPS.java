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

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import de.egi.geofence.geozone.bt.db.DbGlobalsHelper;
import de.egi.geofence.geozone.bt.db.ZoneEntity;
import de.egi.geofence.geozone.bt.geofence.GeofenceRequester;
import de.egi.geofence.geozone.bt.geofence.PathsenseGeofence;
import de.egi.geofence.geozone.bt.geofence.SimpleGeofence;
import de.egi.geofence.geozone.bt.utils.Constants;
import de.egi.geofence.geozone.bt.utils.MyLocation;
import de.egi.geofence.geozone.bt.utils.Utils;

public class WorkerGPS{
	private final Context context;
	private final Logger log = Logger.getLogger(WorkerGPS.class);
	private GoogleApiClient mLocationClient;
	private Location checkLocation;
	private int transition;
    private float accuracy;
    private String type;
    private String origin;
    private Location location;
    private GeofenceRequester mGeofenceRequester;
    private PathsenseGeofence mPathsenseGeofence;
    private DbGlobalsHelper dbGlobalsHelper;
	private ZoneEntity ze;
    private MyLocation myLocation;

	public WorkerGPS(Context context){
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
	public void handleTransitionGPS(int transition, ZoneEntity ze, String type, float accuracy, Location location, String origin) {
		Log.i(Constants.APPTAG, "in handleTransitionGPS");
		log.debug("in handleTransitionGPS");

		this.transition = transition;
        this.type = type;
        this.accuracy = accuracy;
        this.location = location;
        this.origin = origin;
		this.ze = ze;

		log.debug("G-01 Transition: " + transition);

        log.debug("G-02 GeofencingFalsePositives handleTransitionGPS: " + Double.valueOf(location.getLatitude()).toString());
        log.debug("G-03 GeofencingFalsePositives handleTransitionGPS: " + Double.valueOf(location.getLongitude()).toString());
        myLocation = new MyLocation();
        myLocation.getLocation(context, locationResult);
	}

	private void doWork() {
		Location locationZone = new Location("locationZone");
		locationZone.setLatitude(Double.valueOf(ze.getLatitude()));
		locationZone.setLongitude(Double.valueOf(ze.getLongitude()));

		int radius = ze.getRadius();
		float distanceMeters = 0;
		if (checkLocation != null) {
			distanceMeters = checkLocation.distanceTo(locationZone);
		}else{
			log.debug("G-04 GeofencingFalsePositives doWork checkLocation = null");
			if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
				distanceMeters = 0;
			}else{
				distanceMeters = radius + 1;
			}
		}

		List<Geofence> currentGeofence = new ArrayList<>();

		if (distanceMeters > radius) {
			log.debug("G-05 GeofencingFalsePositives DoubleCheck - We are outside of the fence " + ze.getName() + ". DistanceToCenter: " + distanceMeters + " Radius: " + radius);
			if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
				log.debug("G-06 GeofencingFalsePositives DoubleCheck - NOK - Enter event " + ze.getName() + " : - set Enter and return");
				// Set Geofence Enter
				// Return
				if (!Utils.isBoolean(dbGlobalsHelper.getCursorGlobalsByKey(Constants.DB_KEY_NEW_API))) {
					currentGeofence.add(getGeofence(ze, Geofence.GEOFENCE_TRANSITION_ENTER));
					mGeofenceRequester.setInProgressFlag(false);
					mGeofenceRequester.addGeofences(currentGeofence);
				}else{
					SimpleGeofence simpleGeofence = new SimpleGeofence(ze.getName(), ze.getLatitude(), ze.getLongitude(),
							Integer.toString(ze.getRadius()), null, Geofence.NEVER_EXPIRE, Geofence.GEOFENCE_TRANSITION_ENTER, true, null, null);
					mPathsenseGeofence.addGeofence(simpleGeofence);
				}
				// Post a notification
//				NotificationUtil.showError(context, "Enter " + ze.getName() + " : False positives", origin + ": " + (distanceMeters - radius) + " difference");
				log.debug("G-07 GeofencingFalsePositives Enter " + ze.getName() + " : False positives" + origin + ": " + (distanceMeters - radius) + " difference");
				return;
			} else {
				log.debug("G-08 GeofencingFalsePositives DoubleCheck - OK - Exit event " + ze.getName() + " : - set Enter and continue");
				// Set Geofence Enter
				// Continue
				if (!Utils.isBoolean(dbGlobalsHelper.getCursorGlobalsByKey(Constants.DB_KEY_NEW_API))) {
					currentGeofence.add(getGeofence(ze, Geofence.GEOFENCE_TRANSITION_ENTER));
					mGeofenceRequester.setInProgressFlag(false);
					mGeofenceRequester.addGeofences(currentGeofence);
				}else{
					SimpleGeofence simpleGeofence = new SimpleGeofence(ze.getName(), ze.getLatitude(), ze.getLongitude(),
							Integer.toString(ze.getRadius()), null, Geofence.NEVER_EXPIRE, Geofence.GEOFENCE_TRANSITION_ENTER, true, null, null);
					mPathsenseGeofence.addGeofence(simpleGeofence);
				}
			}
		} else {
			log.debug("G-09 GeofencingFalsePositives DoubleCheck - We are inside the fence " + ze.getName() + ". DistanceToCenter: " + distanceMeters + " Radius: " + radius);
			if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
				log.debug("G-10 GeofencingFalsePositives DoubleCheck - NOK - Exit event " + ze.getName() + " : - set Exit and return");
				// Set Geofence Exit
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
				log.debug("G-11 GeofencingFalsePositives Exit " + ze.getName() + " : False positives" + origin + ": " + (radius - distanceMeters) + " difference");
//				NotificationUtil.showError(context, "Exit " + ze.getName() + " : False positives", origin + ": " + (radius - distanceMeters) + " difference");
				return;
			} else {
				log.debug("G-12 GeofencingFalsePositives DoubleCheck - OK - Enter event " + ze.getName() + " : - set Exit and continue");
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
		// Reset
//		checkLocation = null;

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


    private MyLocation.LocationResult locationResult = new MyLocation.LocationResult(){
        @Override
        public void gotLocation(Location location){
           //Got the location!
			checkLocation = location;
			if (location != null) {
				log.debug("G-00 GeofencingFalsePositives locationResult: " + Double.valueOf(location.getLatitude()).toString());
				log.debug("G-00 GeofencingFalsePositives locationResult: " + Double.valueOf(location.getLongitude()).toString());
			}
            doWork();
			// Reset
			checkLocation = null;
        }
    };
}




















