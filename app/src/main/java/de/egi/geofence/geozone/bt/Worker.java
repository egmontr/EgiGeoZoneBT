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

import com.google.android.gms.location.Geofence;

import org.apache.log4j.Logger;

import java.util.StringTokenizer;

import de.egi.geofence.geozone.bt.db.DbGlobalsHelper;
import de.egi.geofence.geozone.bt.db.DbZoneHelper;
import de.egi.geofence.geozone.bt.db.ZoneEntity;
import de.egi.geofence.geozone.bt.utils.Constants;
import de.egi.geofence.geozone.bt.utils.Utils;

public class Worker {
	private final Context context;
	private final Logger log = Logger.getLogger(Worker.class);
    private DbGlobalsHelper dbGlobalsHelper;

	public Worker(Context context){
		this.context = context;
        dbGlobalsHelper = new DbGlobalsHelper(context);
	}

	/**
	 * Report geofence transitions to the UI
	 *
	 * context A Context for this component
	 * intent The Intent containing the transition
	 */
	public void handleTransition(int transition, String ids, String type, float accuracy, Location location, String origin) {
		Log.i(Constants.APPTAG, "in handleTransitionGPS");
		log.info("in handleTransitionGPS");
		log.info("Zones: " +  ids);

		StringTokenizer st = new StringTokenizer(ids, ",");
		while (st.hasMoreTokens()) {
			DbZoneHelper datasource = new DbZoneHelper(context);
			ZoneEntity ze = datasource.getCursorZoneByName(st.nextToken());

			// Bug: Manchmal ist Geofence NULL! Grund nicht bekannt.
			if (ze == null){
				continue;
			}

			if (type.equalsIgnoreCase(Constants.GEOZONE) && Utils.isBoolean(dbGlobalsHelper.getCursorGlobalsByKey(Constants.DB_KEY_FALSE_POSITIVES))) {
				log.debug("0-01 GeofencingFalsePositives handleTransitionWifi");
				if (transition == Geofence.GEOFENCE_TRANSITION_EXIT && ze.getWifi_info() != null && !ze.getWifi_info().equals("")) {
					WorkerWifi workerWifi = new WorkerWifi(context);
					workerWifi.handleTransitionWifi(transition, ze, type, accuracy, location, origin);
				} else {
					WorkerGPS workerGps = new WorkerGPS(context);
					workerGps.handleTransitionGPS(transition, ze, type, accuracy, location, origin);
				}
			} else {
				log.debug("0-02 GeofencingFalsePositives handleTransitionGPS");
				WorkerMain workerMain = new WorkerMain(context);
				workerMain.doMainWork(ze, transition, type, accuracy, location, origin);
			}
		}
	}
}




















