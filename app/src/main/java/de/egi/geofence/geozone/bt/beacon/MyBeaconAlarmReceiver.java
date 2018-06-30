package de.egi.geofence.geozone.bt.beacon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;

import org.altbeacon.beacon.BeaconManager;

import de.egi.geofence.geozone.bt.db.DbContract;
import de.egi.geofence.geozone.bt.db.DbZoneHelper;
import de.egi.geofence.geozone.bt.utils.Constants;
import de.egi.geofence.geozone.bt.utils.Utils;

public class MyBeaconAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String beacon = intent.getStringExtra("beaconZone");
        BeaconManager mBeaconManager = BeaconManager.getInstanceForApplication(context);
        mBeaconManager.setBackgroundBetweenScanPeriod(Utils.getDefaultBackgroundBetweenScanPeriod(context));
        mBeaconManager.setBackgroundScanPeriod(Utils.getDefaultBackgroundScanPeriod(context));

        try {
            mBeaconManager.updateScanPeriods();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        // Broadcast to the Main, to refresh drawer.
        DbZoneHelper datasource = new DbZoneHelper(context);
        datasource.updateZoneField(beacon, DbContract.ZoneEntry.CN_STATUS, false);
        Intent intentBeacon = new Intent();
        intentBeacon.setAction(Constants.ACTION_STATUS_CHANGED);
        intentBeacon.putExtra("state", Constants.BEACON);
        intentBeacon.putExtra("automaticZone", beacon);
        context.sendBroadcast(intentBeacon);
    }
}
