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

package de.egi.geofence.geozone.bt.geofence;

import android.content.Context;
import android.database.Cursor;

import com.google.android.gms.location.Geofence;

import java.util.ArrayList;
import java.util.List;

import de.egi.geofence.geozone.bt.db.DbZoneHelper;
import de.egi.geofence.geozone.bt.db.ZoneEntity;
import de.egi.geofence.geozone.bt.utils.Constants;

/**
 * Geofence Helper
 */
public class SimpleGeofenceStore {
    private final Context context;
    private DbZoneHelper dbZoneHelper;

    // Create the SharedPreferences storage with private access only
    public SimpleGeofenceStore(Context context) {
        this.context = context;
    }


    /**
     * Returns all stored geofences
     *
     * {@link SimpleGeofence}
     */
    public List<SimpleGeofence> getGeofences() {
        List<SimpleGeofence> geofences = new ArrayList<>();

        dbZoneHelper = new DbZoneHelper(context);
        Cursor cursor =  dbZoneHelper.getCursorAllZone(Constants.GEOZONE);
        while (cursor.moveToNext()) {
            geofences.add(getGeofence(cursor));
        }
        cursor.close();
        return geofences;
    }

    /**
     * Returns a stored geofence by its id, or returns {@code null}
     * if it's not found.
     *
     * @return A geofence defined by its center and radius. See
     * {@link SimpleGeofence}
     */
    private SimpleGeofence getGeofence(Cursor cursor) {
        String zone = cursor.getString(1);
        /*
         * Get the latitude for the geofence
         */
        String lat = cursor.getString(2);
        /*
         * Get the longitude for the geofence
         */
        String lng = cursor.getString(3);
        /*
         * Get the radius for the geofence
         */
        String radius= cursor.getString(4);
        String wait_time= cursor.getString(17);
        String alias= cursor.getString(20);
        String wifiInfo= cursor.getString(23);
        /*
         * Get the expiration duration for the geofence exist
         */
        long expirationDuration = Geofence.NEVER_EXPIRE;
        /*
         * Get the transition type for the geofence
         */
        int transitionType = Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT;
        boolean status = cursor.getInt(16) == 1;
        // If none of the values is incorrect, return the object
        if (lat != null && lng != null && radius != null) {
            // Return a true Geofence object
            return new SimpleGeofence(zone, lat, lng, radius, wait_time, expirationDuration, transitionType, status, alias, wifiInfo);
            // Otherwise, return null.
        } else {
            return null;
        }
    }
    /**
     * Returns a stored geofence by its id, or returns {@code null}
     * if it's not found.
     *
     * @return A geofence defined by its center and radius. See
     * {@link SimpleGeofence}
     */
    public SimpleGeofence getGeofence(String zone) {
        dbZoneHelper = new DbZoneHelper(context);
        ZoneEntity zoneEntity = dbZoneHelper.getCursorZoneByName(zone);
        /*
         * Get the latitude for the geofence
         */
        String lat = zoneEntity.getLatitude();
        /*
         * Get the longitude for the geofence
         */
        String lng = zoneEntity.getLongitude();
        /*
         * Get the radius for the geofence
        */
        String radius= Integer.toString(zoneEntity.getRadius());
        String wait_time= Integer.toString(zoneEntity.getAccuracy());
        String alias= zoneEntity.getAlias();
        String wifiInfo= zoneEntity.getWifi_info();
        /*
         * Get the expiration duration for the geofence
         */
        long expirationDuration = Geofence.NEVER_EXPIRE;
        /*
         * Get the transition type for the geofence
         */
        int transitionType = Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT;
        boolean status = zoneEntity.isStatus();

        // If none of the values is incorrect, return the object
        if (lat != null && lng != null) {
            // Return a true Geofence object
            return new SimpleGeofence(zone, lat, lng, radius, wait_time, expirationDuration, transitionType, status, alias, wifiInfo);
            // Otherwise, return null.
        } else {
            return null;
        }
    }
}
