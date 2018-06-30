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
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.egi.geofence.geozone.bt.db.DbGlobalsHelper;
import de.egi.geofence.geozone.bt.geofence.GeofenceRequester;
import de.egi.geofence.geozone.bt.geofence.SimpleGeofence;
import de.egi.geofence.geozone.bt.geofence.SimpleGeofenceStore;
import de.egi.geofence.geozone.bt.utils.Constants;
import de.egi.geofence.geozone.bt.utils.Utils;
import de.mindpipe.android.logging.log4j.LogConfigurator;

public class MyBootReceiver extends BroadcastReceiver {

    private final static LogConfigurator logConfigurator = new LogConfigurator();
	private final Logger log = Logger.getLogger(MainEgiGeoZone.class);

    @Override
    public void onReceive(Context context, Intent intent) {

    	logConfigurator.setFileName(Environment.getExternalStorageDirectory() + File.separator + "egigeozone" + File.separator + "egigeozone.log");
        logConfigurator.setUseFileAppender(true);
        logConfigurator.setRootLevel(Level.ERROR);
        // Set log level of a specific logger
        logConfigurator.setLevel("de.egi.geofence.geozone.bt", Level.ERROR);
        try {
            logConfigurator.configure();
            log.error("Logger set!");
            Log.i("", "Logger set!");
		} catch (Exception ignored) {
		}

        log.error("EgiGeoZone gestartet");
        DbGlobalsHelper dbGlobalsHelper = new DbGlobalsHelper(context);
        dbGlobalsHelper.storeGlobals(Constants.DB_KEY_REBOOT, "true");
        // Nur bei Google Geofence registrierenm da PathSense selbst registriert
        if (!Utils.isBoolean(dbGlobalsHelper.getCursorGlobalsByKey(Constants.DB_KEY_NEW_API))) {
        registerGeofencesAfterReboot(context);
    }
    }

    private void registerGeofencesAfterReboot(Context context){
    	
        log.info("in registerGeofencesAfterReboot" );
        // Instantiate a new geofence storage area
        SimpleGeofenceStore geofenceStore = new SimpleGeofenceStore(context);
        // Instantiate the current List of geofences
        List<Geofence> mCurrentGeofences = new ArrayList<>();
        // Instantiate a Geofence requester
        GeofenceRequester mGeofenceRequester = new GeofenceRequester(context);
        log.info("registerGeofencesAfterReboot - created mGeofenceRequester" );
    	
        List<SimpleGeofence> geofences = geofenceStore.getGeofences();
        for (SimpleGeofence simpleGeofence : geofences) {
            mCurrentGeofences.add(simpleGeofence.toGeofence());
            log.info("registerGeofencesAfterReboot - added geofence " + simpleGeofence.getId());
        }
        // Register all again onRestart/ReBoot
        try {
            // Try to add geofences
        	if (mCurrentGeofences.size() > 0){
        		mGeofenceRequester.addGeofences(mCurrentGeofences);
        		log.info("Geofences registered after reboot");
        	}
        } catch (UnsupportedOperationException e) {
            // Notify user that previous request hasn't finished.
            Toast.makeText(context, R.string.add_geofences_already_requested_error, Toast.LENGTH_LONG).show();
            log.error("Error registering Geofence", e);
//            showError("Error registering Geofence", e.toString());
        }
    }


}