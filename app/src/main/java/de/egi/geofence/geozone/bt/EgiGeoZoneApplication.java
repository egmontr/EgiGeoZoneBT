package de.egi.geofence.geozone.bt;

import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.location.Geofence;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.service.RangedBeacon;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;
import org.apache.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import de.egi.geofence.geozone.bt.beacon.SimpleBeacon;
import de.egi.geofence.geozone.bt.beacon.SimpleBeaconStore;
import de.egi.geofence.geozone.bt.db.DbContract;
import de.egi.geofence.geozone.bt.db.DbGlobalsHelper;
import de.egi.geofence.geozone.bt.db.DbZoneHelper;
import de.egi.geofence.geozone.bt.db.ZoneEntity;
import de.egi.geofence.geozone.bt.utils.Constants;
import de.egi.geofence.geozone.bt.utils.NotificationUtil;
import de.egi.geofence.geozone.bt.utils.Utils;

// http://altbeacon.github.io/android-beacon-library/documentation.html
public class EgiGeoZoneApplication extends Application implements BootstrapNotifier, RangeNotifier, BeaconConsumer {
	private static final String TAG = "EgiGeoZoneApplication";
//	final static LogConfigurator logConfigurator = new LogConfigurator();
	private final Logger log = Logger.getLogger(EgiGeoZoneApplication.class);
	private BeaconManager mBeaconManager;
	private Region mAllBeaconsRegion;
	@SuppressWarnings("unused")
	private RegionBootstrap mRegionBootstrap;
	private Worker worker;
	public final static String EGIGEOZONE = "##EgiGeoZone##";
	private final static long EXITMULTIPLICATOR = 2;
	private DbZoneHelper datasource;
	private DbGlobalsHelper dbGlobalsHelper;

	public static final String DEFAULT_FOREGROUND_SCAN_PERIOD = "1100";
	public static final String DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD = "0";
	public static final String DEFAULT_BACKGROUND_SCAN_PERIOD = "2100";
	public static final String DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD = "60000";

	public static final long DEFAULT_FOREGROUND_SCAN_PERIOD_L = 1100L;
	public static final long DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD_L = 0L;
	public static final long DEFAULT_BACKGROUND_SCAN_PERIOD_L = 2100L;
	public static final long DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD_L = 60000L;

	private String currentApplicationVersion = "UNKNOWN";

	@Override
	public void onCreate() {
		super.onCreate();
		// altBeacon Logging
		LogManager.setVerboseLoggingEnabled(true);

		dbGlobalsHelper = new DbGlobalsHelper(this);

		try {
			PackageInfo pi = getPackageManager().getPackageInfo(this.getPackageName(), PackageManager.GET_CONFIGURATIONS);
			String v = pi.versionName;
			String f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss - ", Locale.getDefault()).format(new Date(pi.firstInstallTime));
			String l = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss - ", Locale.getDefault()).format(new Date(pi.lastUpdateTime));
			Log.i(TAG, "*** Current application version: " + v + " ***");
			Log.i(TAG, "*** First install time: " + f + " ***");
			Log.i(TAG, "*** Last update time: " + l + " ***");
		} catch (PackageManager.NameNotFoundException ignored) {
		}

		// Dann auch Version Exportieren/Importieren
		makeUpdates();

		// Faster Ranging
		RangedBeacon.setSampleExpirationMilliseconds(5000);

		boolean isBeaconScan = Utils.isBoolean(dbGlobalsHelper.getCursorGlobalsByKey(Constants.DB_KEY_BEACON_SCAN));
		if(isBeaconScan){
			bind();
		}
	}

	// Set Regions to monitor at boot/startup
	private void setRegionsAtBoot(){
		Log.d(TAG, "setRegionsAtBoot");
		log.debug("setRegionsAtBoot");
		SimpleBeaconStore beaconStore = new SimpleBeaconStore(this);
		List<SimpleBeacon> beacons = beaconStore.getBeacons();
		for (SimpleBeacon simpleBeacon : beacons) {
			// Register only automatics
			if (!simpleBeacon.isAutomatic()) continue;

			try {
				List<Identifier> listB = Utils.getStringIdentifiers(simpleBeacon.getBeaconUuid());
				Log.d(TAG, "setRegionsAtBoot " + simpleBeacon.getId());
				log.debug("setRegionsAtBoot " + simpleBeacon.getId());
				Region region = new Region(simpleBeacon.getId(), listB);
				mBeaconManager.startRangingBeaconsInRegion(region);
				mBeaconManager.startMonitoringBeaconsInRegion(region);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
		// Aktionen ausführen
		if (region.getUniqueId().equals(EGIGEOZONE)){
			return;
		}
		Log.d(TAG, "didRangeBeaconsInRegion");
		log.debug("didRangeBeaconsInRegion");
		if (beacons.size() > 0) {
			for (Beacon beacon: beacons) {
				Log.d(TAG, "Beacon " + beacon.toString() + " is about " + beacon.getDistance() + " meters away, with Rssi: " + beacon.getRssi());
				log.debug("Beacon " + beacon.toString() + " is about " + beacon.getDistance() + " meters away, with Rssi: " + beacon.getRssi());
				// Damit man den Status lesen kann
				datasource = new DbZoneHelper(getApplicationContext());
				ZoneEntity ze;
				try {
					ze = datasource.getCursorZoneByName(region.getUniqueId());
				}catch(Exception e){
					// Nichts tun, da inzwischen wahrscheinlich gelöscht
					return;
				}

				// Nach MAC-Adresse suchen
				if (!Utils.getStringMacAddress(ze.getBeacon()).equals("") && !Utils.getStringMacAddress(ze.getBeacon()).equals(beacon.getBluetoothAddress())){
					continue;
				}

//				if (beacon.getDistance() < ze.getRadius() && !ze.isStatus()) { // status != Enter
				if (beacon.getDistance() < ze.getRadius() && (!ze.isAutomatic() || !ze.isStatus())) { // not automatically or status != Enter

					Collection<Region> allRanged = mBeaconManager.getRangedRegions();
					boolean foundRangedRegion = false;
					Iterator allRangedIterator = allRanged.iterator();
					while(allRangedIterator.hasNext()) {
						Region rangedRegion = (Region)allRangedIterator.next();
						if(region.getUniqueId().equals(rangedRegion.getUniqueId())) {
							foundRangedRegion = true;
							break;
						}
					}
					if (foundRangedRegion) {
//						Collection<Region> allMonitored = mBeaconManager.getMonitoredRegions();
						// Status wird fuer das naechste Mal und fuer die Ampel in handleTransition auf true/gruen gesetzt
						Log.d(TAG, "Beacon " + beacon.toString() + " Just became less than " + ze.getRadius() + " meters away.");
						log.debug("*** Beacon " + beacon.toString() + " Just became less than " + ze.getRadius() + " meters away. ***");
						String transitionType = getTransitionString(1);
						NotificationUtil.sendNotification(getApplicationContext(), transitionType, region.getUniqueId(), Constants.FROM_BEACON);

						worker = new Worker(getApplicationContext());
						worker.handleTransition(1, region.getUniqueId(), Constants.BEACON, -1, null, Constants.FROM_BEACON);

						// Stop monitoring/ranging
						// Set Scan period back to default
						if (!ze.isAutomatic()) {
                            // Broadcast to the Main, to refresh drawer.
                            datasource.updateZoneField(ze.getName(), DbContract.ZoneEntry.CN_STATUS, false);
                            Intent intent = new Intent();
                            intent.setAction(Constants.ACTION_STATUS_CHANGED);
                            intent.putExtra("state", Constants.BEACON);
                            intent.putExtra("automaticZone", ze.getName());
                            sendBroadcast(intent);

                            mBeaconManager.setBackgroundBetweenScanPeriod(getDefaultBackgroundBetweenScanPeriod());
							mBeaconManager.setBackgroundScanPeriod(getDefaultBackgroundScanPeriod());
							try {
								mBeaconManager.updateScanPeriods();

								mBeaconManager.stopRangingBeaconsInRegion(region);
								mBeaconManager.stopMonitoringBeaconsInRegion(region);
							} catch (RemoteException e) {
							}
						}
					}
				}

				if (beacon.getDistance() > (ze.getRadius() * EXITMULTIPLICATOR) && ze.isAutomatic() && ze.isStatus()) { // Status == Enter
					// Status wird fuer das naechste Mal und fuer die Ampel in handleTransition auf false/rot gesetzt
					Log.d(TAG, "Beacon "+ beacon.toString()+" Just became over " + ze.getRadius() * EXITMULTIPLICATOR + " meters away.");
					log.debug("*** Beacon " + beacon.toString() + " Just became over " + ze.getRadius() * EXITMULTIPLICATOR + " meters away. ***");
					String transitionType = getTransitionString(2);
					NotificationUtil.sendNotification(getApplicationContext(), transitionType, region.getUniqueId(), Constants.FROM_BEACON);

					worker = new Worker(getApplicationContext());
					worker.handleTransition(2, region.getUniqueId(), Constants.BEACON, -1, null, Constants.FROM_BEACON);
				}
			}
		}
	}

	@Override
	public void didDetermineStateForRegion(int arg0, Region region) {
		Log.d(TAG, "didDetermineStateForRegion: " + region.getUniqueId());
		log.debug("didDetermineStateForRegion: " + region.getUniqueId());
	}

	@Override
	public void didEnterRegion(Region region) {
		Log.d(TAG, "entered region: " + region.getUniqueId());
		log.debug("entered region: " + region.getUniqueId());
	}

	@Override
	public void didExitRegion(Region region) {
		Log.d(TAG, "exited region: " + region.getUniqueId());
		log.debug("exited region: " + region.getUniqueId());

		// Überspringen, da nicht in DB
		if (region.getUniqueId().equals(EGIGEOZONE)){
			return;
		}

		log.debug("exited region.  stopping ranging: " + region.getUniqueId());

		// Damit man den Status lesen kann
		datasource = new DbZoneHelper(getApplicationContext());
		ZoneEntity ze = datasource.getCursorZoneByName(region.getUniqueId());

		if (ze.isStatus()) {
			// Status wird fuer das naechste Mal und fuer die Ampel in handleTransition auf false/rot gesetzt
			Log.d(TAG, "Beacon " + region.getUniqueId() + " Just exited region.");
			log.debug("Beacon " + region.getUniqueId() + " Just exited region.");
			String transitionType = getTransitionString(2);
			NotificationUtil.sendNotification(getApplicationContext(), transitionType, region.getUniqueId(), Constants.FROM_BEACON);

			worker = new Worker(getApplicationContext());
			worker.handleTransition(2, region.getUniqueId(), Constants.BEACON, -1, null, Constants.FROM_BEACON);
		}
	}

	/**
	 * Maps geofence transition types to their human-readable equivalents.
	 * @param transitionType A transition type constant defined in Geofence
	 * @return A String indicating the type of transition
	 */
	private String getTransitionString(int transitionType) {
		switch (transitionType) {

			case Geofence.GEOFENCE_TRANSITION_ENTER:
				return getString(R.string.geofence_transition_entered);

			case Geofence.GEOFENCE_TRANSITION_EXIT:
				return getString(R.string.geofence_transition_exited);

			default:
				return getString(R.string.geofence_transition_unknown);
		}
	}

	@Override
	public void onBeaconServiceConnect() {
		Log.d(TAG, "onBeaconServiceConnect");
		log.debug("onBeaconServiceConnect");
		setRegionsAtBoot();
		mBeaconManager.addRangeNotifier(this);
	}

	public void bind(){
		Log.d(TAG, "bind");
		log.debug("bind");
		mAllBeaconsRegion = new Region(EGIGEOZONE, null, null, null);
		mBeaconManager = BeaconManager.getInstanceForApplication(this);

		// Simply constructing this class and holding a reference to it in your custom Application class
		// enables auto battery saving of about 60%
		BackgroundPowerSaver mBackgroundPowerSaver = new BackgroundPowerSaver(this);

		mBeaconManager.setBackgroundBetweenScanPeriod(getDefaultBackgroundBetweenScanPeriod());
		mBeaconManager.setBackgroundScanPeriod(getDefaultBackgroundScanPeriod());

		try {
			mBeaconManager.updateScanPeriods();
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		// iBeacon
		mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
		// EddyStone
		mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19"));
		// wake up the app when a beacon is seen
		mRegionBootstrap = new RegionBootstrap(this, mAllBeaconsRegion);
		mBeaconManager.bind(this);
	}

	public void unbind(){
		Log.d(TAG, "unbind");
		log.debug("unbind");
		if (mBeaconManager == null) return;
		try {
			mBeaconManager.stopRangingBeaconsInRegion(mAllBeaconsRegion);
			mBeaconManager.stopMonitoringBeaconsInRegion(mAllBeaconsRegion);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (mBeaconManager.isAnyConsumerBound()) {
			mBeaconManager.unbind(this);
		}
		mRegionBootstrap.disable();
	}
	/**
	 * Updates durchführen
	 */
	private void makeUpdates() {
		try {
			currentApplicationVersion = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
		} catch (PackageManager.NameNotFoundException e) {
			Log.w(TAG, "Could not lookup current application version", e);
		}

		String lastInstalledApplicationVersion = dbGlobalsHelper.getCursorGlobalsByKey(Constants.DB_KEY_LASTINSTALLEDAPPLICATIONVERSION);
		if (lastInstalledApplicationVersion == null || !lastInstalledApplicationVersion.equals(currentApplicationVersion)) {
			// Etwas tun
			// Wenn Updates nötig

			dbGlobalsHelper.storeGlobals(Constants.DB_KEY_LASTINSTALLEDAPPLICATIONVERSION, currentApplicationVersion);
		}

	}

	public long getDefaultForegroundBetweenScanPeriod(){
		String fbsp = dbGlobalsHelper.getCursorGlobalsByKey(Constants.DB_KEY_DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD);
		try {
			if (fbsp == null) {
				return Long.parseLong(DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD);
			} else {
				return Long.parseLong(fbsp);
			}
		}catch(NumberFormatException nfe){
			return DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD_L;
		}
	}
	public long getDefaultBackgroundScanPeriod(){
		String bsp = dbGlobalsHelper.getCursorGlobalsByKey(Constants.DB_KEY_DEFAULT_BACKGROUND_SCAN_PERIOD);
		try {
			if (bsp == null) {
				return Long.parseLong(DEFAULT_BACKGROUND_SCAN_PERIOD);
			} else {
				return Long.parseLong(bsp);
			}
		}catch(NumberFormatException nfe){
			return DEFAULT_BACKGROUND_SCAN_PERIOD_L;
		}
	}
	public long getDefaultBackgroundBetweenScanPeriod(){
		String bbsp = dbGlobalsHelper.getCursorGlobalsByKey(Constants.DB_KEY_DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD);
		try {
			if (bbsp == null) {
				return Long.parseLong(DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD);
			} else {
				return Long.parseLong(bbsp);
			}
		}catch(NumberFormatException nfe){
			return DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD_L;
		}
	}
}













