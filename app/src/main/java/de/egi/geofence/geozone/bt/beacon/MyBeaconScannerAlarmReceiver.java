package de.egi.geofence.geozone.bt.beacon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import de.egi.geofence.geozone.bt.EgiGeoZoneApplication;
import de.egi.geofence.geozone.bt.db.DbGlobalsHelper;
import de.egi.geofence.geozone.bt.utils.Constants;

/**
 * Created by egmont on 10.08.2017.
 */

public class MyBeaconScannerAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String beaconScannerType = intent.getStringExtra("beaconScannerType");
        DbGlobalsHelper dbGlobalsHelper = new DbGlobalsHelper(context);

        if (beaconScannerType.equals("start")) {
            dbGlobalsHelper.storeGlobals(Constants.DB_KEY_BEACON_SCAN, "true");
            ((EgiGeoZoneApplication) context.getApplicationContext()).bind();
        }else{
            dbGlobalsHelper.storeGlobals(Constants.DB_KEY_BEACON_SCAN, "false");
            ((EgiGeoZoneApplication) context.getApplicationContext()).unbind();
        }
    }
}
