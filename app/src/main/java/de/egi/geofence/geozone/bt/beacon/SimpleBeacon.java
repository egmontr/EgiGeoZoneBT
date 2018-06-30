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



/**
 * A single beacon object
 */
public class SimpleBeacon {
    // Instance variables
    private final String mId;
    private final String mRadius;
    private final String mBeacon;
    private boolean status;
    private final String mLatitude;
    private final String mLongitude;
    private final String mAlias;
    private boolean automatic;

    /**
     * @param beaconId The beacon's request ID
     * @param radius Radius of the beacon circle. The value is not checked for validity
     */
    public SimpleBeacon(
            String beaconId,
            String radius,
            boolean status,
            String beacon,
            String latitude,
            String longitude,
            String alias,
            boolean automatic
    ) {
        // Set the instance fields from the constructor
        // An identifier for the beacon
        this.mId = beaconId;

        // Radius of the beacon, in meters
        this.mRadius = radius;
        this.status = status;
        this.mBeacon = beacon;
        this.mLatitude = latitude;
        this.mLongitude = longitude;
        this.mAlias = alias;
        this.automatic = automatic;
    }

    public SimpleBeacon(
            String beaconId,
            String radius,
            String beacon,
            String latitude,
            String longitude,
            String alias
    ) {
        // Set the instance fields from the constructor
        // An identifier for the beacon
        this.mId = beaconId;
        // Radius of the beacon, in meters
        this.mRadius = radius;
        // Transition type
        this.mBeacon = beacon;
        this.mLatitude = latitude;
        this.mLongitude = longitude;
        this.mAlias = alias;
    }

    public boolean isAutomatic() {
        return automatic;
    }

    // Instance field getters

    /**
     * Get the beacon ID
     * @return A Simplebeacon ID
     */
    public String getId() {
        return mId;
    }
    /**
     * Get the beacon radius
     * @return A radius value
     */
    public String getRadius() {
        return mRadius;
    }
    /**
     * Creates a EgiGeoZoneBeacon object from a
     * Simplebeacon.
     *
     * @return A beacon object
     */
    public EgiGeoZoneBeacon toBeacon() {
        // Build a new EgiGeoZoneBeacon object
        return new EgiGeoZoneBeacon(mId, mRadius, mBeacon);
    }

    /**
     * @return the status
     */
    public boolean isStatus() {
        return status;
    }

// --Commented out by Inspection START (23.12.2015 16:18):
//	/**
//	 * @param status the status to set
//	 */
//	public void setStatus(boolean status) {
//		this.status = status;
//	}
// --Commented out by Inspection STOP (23.12.2015 16:18)

    /**
     * @return the mBeacon
     */
    public String getBeaconUuid() {
        return mBeacon;
    }
    /**
     * Get the geofence latitude
     * @return A latitude value
     */
    public String getLatitude() {
        return mLatitude;
    }

    /**
     * Get the geofence longitude
     * @return A longitude value
     */
    public String getLongitude() {
        return mLongitude;
    }

    public String getAlias() {
        return mAlias;
    }

}
