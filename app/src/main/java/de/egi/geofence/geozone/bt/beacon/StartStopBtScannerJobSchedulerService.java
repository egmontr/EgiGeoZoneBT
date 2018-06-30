package de.egi.geofence.geozone.bt.beacon;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;

import de.egi.geofence.geozone.bt.EgiGeoZoneApplication;
import de.egi.geofence.geozone.bt.db.DbGlobalsHelper;
import de.egi.geofence.geozone.bt.utils.Constants;

/**
 * Created by egmont on 10.08.2017.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class StartStopBtScannerJobSchedulerService extends JobService {
//    private final Logger log = Logger.getLogger(StartStopBtScannerJobSchedulerService.class.getSimpleName());
    private Context context;
    private String beaconScannerType;

    private Handler mJobHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage( Message msg ) {
            DbGlobalsHelper dbGlobalsHelper = new DbGlobalsHelper(context);

            if (beaconScannerType.equals("start")) {
                dbGlobalsHelper.storeGlobals(Constants.DB_KEY_BEACON_SCAN, "true");
                ((EgiGeoZoneApplication) context.getApplicationContext()).bind();
            }else{
                dbGlobalsHelper.storeGlobals(Constants.DB_KEY_BEACON_SCAN, "false");
                ((EgiGeoZoneApplication) context.getApplicationContext()).unbind();
            }

            jobFinished((JobParameters) msg.obj, false);
            return true;
        }
    } );
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        context = this;
        PersistableBundle pb = jobParameters.getExtras();
        beaconScannerType = pb.getString("beaconScannerType");
        // Start action in own thread
        mJobHandler.sendMessage(Message.obtain( mJobHandler, 1, jobParameters ));
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        // Do not restart
        return false;
    }
}