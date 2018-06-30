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

import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

import de.egi.geofence.geozone.bt.db.DbZoneHelper;
import de.egi.geofence.geozone.bt.db.ZoneEntity;
import de.egi.geofence.geozone.bt.utils.Constants;

/**
 * beacon Helper
 */
public class SimpleBeaconStore {
    private final Context context;
    private DbZoneHelper dbZoneHelper;

    public SimpleBeaconStore(Context context) {
        this.context = context;
    }


    /**
     * Returns all stored beacons
     *
     * {@link SimpleBeacon}
     */
    public List<SimpleBeacon> getBeacons() {
        List<SimpleBeacon> beacons = new ArrayList<>();

        dbZoneHelper = new DbZoneHelper(context);
        Cursor cursor =  dbZoneHelper.getCursorAllZone(Constants.BEACON);
        while (cursor.moveToNext()) {
            beacons.add(getBeacon(cursor));
        }
        cursor.close();
        return beacons;
    }

// --Commented out by Inspection START (23.12.2015 16:18):
//    /**
//     * Removes all stored beacons @return A beacon defined by its center and radius. See
//     * {@link SimpleBeacon}
//     */
//    public void removeAllBeacons() {
//    	dbZoneHelper = new DbZoneHelper(context);
//    	Cursor cursor =  dbZoneHelper.getCursorAllZone(Constants.BEACON);
//	    while (cursor.moveToNext()) {
//	    	dbZoneHelper.deleteZone(cursor.getString(1));
//        }
//	    cursor.close();
//    }
// --Commented out by Inspection STOP (23.12.2015 16:18)


    /**
     * Returns a stored beacon by its id, or returns {@code null}
     * if it's not found.
     *
     * id The ID of a stored beacon
     * @return A beacon defined by its center and radius. See
     * {@link SimpleBeacon}
     */
    private SimpleBeacon getBeacon(Cursor cursor) {
        String region = cursor.getString(1);
        /*
         * Get the radius for the beacon
         */
        String radius= cursor.getString(4);
        boolean status = cursor.getInt(16) == 1;
        String beacon = cursor.getString(19);
        String lat = cursor.getString(2);
        String lng = cursor.getString(3);
        String alias = cursor.getString(20);
        boolean automatic = cursor.getInt(21) == 1;

        // Return a true beacon object
        return new SimpleBeacon(region, radius, status, beacon, lat, lng, alias, automatic);

    }
    /**
     * Returns a stored beacon by its id, or returns {@code null}
     * if it's not found.
     *
     * id The ID of a stored beacon
     * @return A beacon defined by its center and radius. See
     * {@link SimpleBeacon}
     */
    public SimpleBeacon getBeacon(String region) {

        dbZoneHelper = new DbZoneHelper(context);
        ZoneEntity zoneEntity = dbZoneHelper.getCursorZoneByName(region);

        /*
         * Get the radius for the beacon
        */
        String radius= Integer.toString(zoneEntity.getRadius());
        boolean status = zoneEntity.isStatus();
        String beacon= zoneEntity.getBeacon();
        String lat = zoneEntity.getLatitude();
        String lng = zoneEntity.getLongitude();
        String alias = zoneEntity.getAlias();
        boolean automatic = zoneEntity.isAutomatic();

        // Return a true Beacone object
        return new SimpleBeacon(region, radius, status, beacon, lat, lng, alias, automatic);
    }
}
