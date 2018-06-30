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

import java.util.ArrayList;
import java.util.List;

import de.egi.geofence.geozone.bt.db.GlobalsEntity;
import de.egi.geofence.geozone.bt.db.RequirementsEntity;
import de.egi.geofence.geozone.bt.db.MoreEntity;
import de.egi.geofence.geozone.bt.db.ZoneEntity;
import de.egi.geofence.geozone.bt.geofence.GeofenceRemover;

public class GlobalSingleton {
	private static GlobalSingleton _instance;

//	private GeofenceRequester geofenceRequester = null;
	private GeofenceRemover geofenceRemover = null;
	private List<String> btDevicesConnected = new ArrayList<>();
	// Testergebnis falsch Schalter
	private boolean testResultError = false;
	// Update Schalter
	private boolean update = false;

    private ZoneEntity zoneEntity = null;
    private MoreEntity moreEntity = null;
    private RequirementsEntity requirementsEntity = null;
    private GlobalsEntity settingsEntity = null;
	// Settings
	private boolean notification;
	private boolean errorNotification;
	private boolean gcm;
	private boolean gcmLogging;
	private String senderId;
	private String locInterval;
	private String locPriority;
	// Tracking
	private String localTrackingInterval;
	private String trackUrl;
	private boolean trackToFile;
	private boolean trackToMail;
	// Conditions
	private String condBluetoothDeviceB;
	private String condBluetoothDeviceV;
	// Conditions weekdays
	private boolean mo;
	private boolean di;
	private boolean mi;
	private boolean don;
	private boolean fr;
	private boolean sa;
	private boolean so;

	public String getNotificationTitel() {
		return notificationTitel;
	}

	public void setNotificationTitel(String notificationTitel) {
		this.notificationTitel = notificationTitel;
	}

	public String getNotificationText() {
		return notificationText;
	}

	public void setNotificationText(String notificationText) {
		this.notificationText = notificationText;
	}

	private String notificationTitel;
	private String notificationText;

	private GlobalSingleton() {
	}

	public static GlobalSingleton getInstance() {
		if (_instance == null) {
			_instance = new GlobalSingleton();
		}
		return _instance;
	}

	public GeofenceRemover getGeofenceRemover() {
		return geofenceRemover;
	}

	public void setGeofenceRemover(GeofenceRemover geofenceRemover) {
		this.geofenceRemover = geofenceRemover;
	}

	


	/**
	 * Alle Felder löschen
	 */
	public void clearAll() {
		this.localTrackingInterval="";
		this.trackToFile=false;
		this.trackUrl="";
		this.trackToMail=false;
		this.condBluetoothDeviceB="";
		this.condBluetoothDeviceV="";
		this.mo=false;
		this.di=false;
		this.mi=false;
		this.don=false;
		this.fr=false;
		this.sa=false;
		this.so=false;

	}

	
//	public boolean isReboot() {
//		return reboot;
//	}
//
//	public void setReboot(boolean reboot) {
//		this.reboot = reboot;
//	}

	public boolean isNotification() {
		return notification;
	}

	public void setNotification(boolean notification) {
		this.notification = notification;
	}

	public boolean isErrorNotification() {
		return errorNotification;
	}

	public void setErrorNotification(boolean errorNotification) {
		this.errorNotification = errorNotification;
	}

	public boolean isGcm() {
		return gcm;
	}

	public void setGcm(boolean gcm) {
		this.gcm = gcm;
	}


	public boolean isUpdate() {
		return update;
	}

	public void setUpdate(boolean update) {
		this.update = update;
	}

	/**
	 * @return the condBluetoothDeviceB
	 */
	public String getCondBluetoothDeviceB() {
		return condBluetoothDeviceB;
	}

	/**
	 * @param condBluetoothDeviceB the condBluetoothDeviceB to set
	 */
	public void setCondBluetoothDeviceB(String condBluetoothDeviceB) {
		this.condBluetoothDeviceB = condBluetoothDeviceB;
	}

	/**
	 * @return the condBluetoothDeviceV
	 */
	public String getCondBluetoothDeviceV() {
		return condBluetoothDeviceV;
	}

	/**
	 * @param condBluetoothDeviceV the condBluetoothDeviceV to set
	 */
	public void setCondBluetoothDeviceV(String condBluetoothDeviceV) {
		this.condBluetoothDeviceV = condBluetoothDeviceV;
	}

	/**
	 * @return the senderId
	 */
	public String getSenderId() {
		return senderId;
	}

	/**
	 * @param senderId the senderId to set
	 */
	public void setSenderId(String senderId) {
		this.senderId = senderId;
	}

	public List<String> getBtDevicesConnected() {
		return btDevicesConnected;
	}

	public void setBtDevicesConnected(List<String> btDevicesConnected) {
		this.btDevicesConnected = btDevicesConnected;
	}

	public boolean isMo() {
		return mo;
	}

	public void setMo(boolean mo) {
		this.mo = mo;
	}

	public boolean isDi() {
		return di;
	}

	public void setDi(boolean di) {
		this.di = di;
	}

	public boolean isMi() {
		return mi;
	}

	public void setMi(boolean mi) {
		this.mi = mi;
	}

	public boolean isDon() {
		return don;
	}

	public void setDon(boolean don) {
		this.don = don;
	}

	public boolean isFr() {
		return fr;
	}

	public void setFr(boolean fr) {
		this.fr = fr;
	}

	public boolean isSa() {
		return sa;
	}

	public void setSa(boolean sa) {
		this.sa = sa;
	}

	public boolean isSo() {
		return so;
	}

	public void setSo(boolean so) {
		this.so = so;
	}

	public boolean isGcmLogging() {
		return gcmLogging;
	}

	public void setGcmLogging(boolean gcmLogging) {
		this.gcmLogging = gcmLogging;
	}


	/**
	 * @return the localTrackingInterval
	 */
	public String getLocalTrackingInterval() {
		return localTrackingInterval;
	}

	/**
	 * @param localTrackingInterval the localTrackingInterval to set
	 */
	public void setLocalTrackingInterval(String localTrackingInterval) {
		this.localTrackingInterval = localTrackingInterval;
	}

	/**
	 * @return the locInterval
	 */
	public String getLocInterval() {
		return locInterval;
	}

	/**
	 * @param locInterval the locInterval to set
	 */
	public void setLocInterval(String locInterval) {
		this.locInterval = locInterval;
	}

	/**
	 * @return the locPriority
	 */
	public String getLocPriority() {
		return locPriority;
	}

	/**
	 * @param locPriority the locPriority to set
	 */
	public void setLocPriority(String locPriority) {
		this.locPriority = locPriority;
	}

	/**
	 * @return the trackUrl
	 */
	public String getTrackUrl() {
		return trackUrl;
	}

	/**
	 * @param trackUrl the trackUrl to set
	 */
	public void setTrackUrl(String trackUrl) {
		this.trackUrl = trackUrl;
	}

	/**
	 * @return the trackToFile
	 */
	public boolean isTrackToFile() {
		return trackToFile;
	}

	/**
	 * @param trackToFile the trackToFile to set
	 */
	public void setTrackToFile(boolean trackToFile) {
		this.trackToFile = trackToFile;
	}

	public boolean isTrackToMail() {
		return trackToMail;
	}

	public void setTrackToMail(boolean trackToMail) {
		this.trackToMail = trackToMail;
	}

	/**
	 * @return the moreEntity
	 */
	public MoreEntity getMoreEntity() {
		return moreEntity;
	}

	/**
	 * @param moreEntity the moreEntity to set
	 */
	public void setMoreEntity(MoreEntity moreEntity) {
		this.moreEntity = moreEntity;
	}

	/**
	 * @return the requirementsEntity
	 */
	public RequirementsEntity getRequirementsEntity() {
		return requirementsEntity;
	}

	/**
	 * @param requirementsEntity the requirementsEntity to set
	 */
	public void setRequirementsEntity(RequirementsEntity requirementsEntity) {
		this.requirementsEntity = requirementsEntity;
	}

	/**
	 * @return the settingsEntity
	 */
	public GlobalsEntity getSettingsEntity() {
		return settingsEntity;
	}

	/**
	 * @param settingsEntity the settingsEntity to set
	 */
	public void setSettingsEntity(GlobalsEntity settingsEntity) {
		this.settingsEntity = settingsEntity;
	}

	/**
	 * @return the zoneEntity
	 */
	public ZoneEntity getZoneEntity() {
		return zoneEntity;
	}

	/**
	 * @param zoneEntity the zoneEntity to set
	 */
	public void setZoneEntity(ZoneEntity zoneEntity) {
		this.zoneEntity = zoneEntity;
	}

	/**
	 * @return the testResultError
	 */
	public boolean isTestResultError() {
		return testResultError;
	}

	/**
	 * the testResultError to set
	 */
	public void setTestResultError(boolean testResultFalse) {
		this.testResultError = testResultFalse;
	}

}










