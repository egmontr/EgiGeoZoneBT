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

public class EgiGeoZoneBeacon {
    private final String beaconId;

	public EgiGeoZoneBeacon(String beaconId, String radius, String beacon) {
		this.beaconId = beaconId;
	}

	/**
	 * @return the beaconId
	 */
	public String getBeaconId() {
		return beaconId;
	}

// --Commented out by Inspection START (17.08.2016 08:23):
//	/**
//	 * @return the radius
//	 */
//	public String getRadius() {
//		return radius;
//	}
// --Commented out by Inspection STOP (17.08.2016 08:23)

// --Commented out by Inspection START (17.08.2016 08:23):
//	/**
//	 * @return the beacon
//	 */
//	public String getBeacon() {
//		return beacon;
//	}
// --Commented out by Inspection STOP (17.08.2016 08:23)

}
