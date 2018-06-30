package de.egi.geofence.geozone.bt;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.location.Location;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.Region;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import de.egi.geofence.geozone.bt.beacon.MyBeaconScannerAlarmReceiver;
import de.egi.geofence.geozone.bt.beacon.StartStopBtScannerJobSchedulerService;
import de.egi.geofence.geozone.bt.db.DbContract;
import de.egi.geofence.geozone.bt.db.DbGlobalsHelper;
import de.egi.geofence.geozone.bt.db.DbServerHelper;
import de.egi.geofence.geozone.bt.db.DbZoneHelper;
import de.egi.geofence.geozone.bt.db.ServerEntity;
import de.egi.geofence.geozone.bt.db.ZoneEntity;
import de.egi.geofence.geozone.bt.geofence.RetryJobSchedulerService;
import de.egi.geofence.geozone.bt.geofence.RetryRequestQueue;
import de.egi.geofence.geozone.bt.tasker.TaskerIntent;
import de.egi.geofence.geozone.bt.tracker.TrackingUtils;
import de.egi.geofence.geozone.bt.utils.Api;
import de.egi.geofence.geozone.bt.utils.AuthenticationParameters;
import de.egi.geofence.geozone.bt.utils.Constants;
import de.egi.geofence.geozone.bt.utils.IOUtil;
import de.egi.geofence.geozone.bt.utils.NotificationUtil;
import de.egi.geofence.geozone.bt.utils.Utils;

/**
 * Created by RitterE on 25.08.2017.
 */

public class WorkerMain {
    private final Context context;
    private Api geoApi;
    private DbServerHelper datasourceServer;
    private String fallback;
    private static int kJobId = 0;
    private static String XHR = "XHR=1";
    private static String FWCSRF = "&fwcsrf=";
    private final Logger log = Logger.getLogger(WorkerMain.class);

    public WorkerMain(Context context) {
        this.context = context;
    }


    public void doMainWork(ZoneEntity ze, int transition, String type, float accuracy, Location location, String origin){
        DbZoneHelper datasourceZone = new DbZoneHelper(context);
        // Gesamtgenauigkeit/Accuracy berücksichtigen
        if (ze.getAccuracy() > 0 && accuracy > -1){
            if (accuracy > ze.getAccuracy()){
                log.debug("Actions will not be performed. Location accuracy bigger then given accuracy: " + accuracy + " > " + ze.getAccuracy());
                return;
            }
        }

        // Post a notification
        NotificationUtil.sendNotification(context, getTransitionString(transition), ze.getName(), origin);

        // Fallback setzen
        if (ze.getServerEntity() != null && ze.getServerEntity().getId_fallback() != null && !ze.getServerEntity().getId_fallback().equals("")){
            fallback = ze.getServerEntity().getId_fallback();
        }else{
            fallback = null;
        }

        // Für Anzeige der Anwesenheit in der App, Status hier merken
        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER){
            ze.setStatus(true);
        }else{
            ze.setStatus(false);
        }
        datasourceZone.updateZoneField(ze.getName(), DbContract.ZoneEntry.CN_STATUS, ze.isStatus());
        // Broadcast to the Main, to refresh drawer.
        Intent intent = new Intent();
        intent.setAction(Constants.ACTION_STATUS_CHANGED);
        if (type.equalsIgnoreCase(Constants.GEOZONE)){
            intent.putExtra("state", Constants.GEOZONE);
        }else{
            intent.putExtra("state", Constants.BEACON);
        }
        context.sendBroadcast(intent);

        if (!checkWeekday(ze)){
            log.debug("Condition day of week for zone " + ze.getName() + " false!");
            return;
        }

        // Kann erst ab Version 4.3 verwendet werden!
        if (checkConditionBluetoothDeviceConnected(context, ze, transition)){
            // Weiter
        }else{
            // Gerät nicht mit Bluetooth verbunden oder condition nicht konfiguriert
            return;
        }

        String realLat = null;
        String realLng = null;
        String location_accuracy = null;
        String locationDate = null;
        String localLocationDate = null;
        if (location != null){
            realLat = Double.toString(location.getLatitude());
            realLng = Double.toString(location.getLongitude());
            location_accuracy = Float.valueOf(location.getAccuracy()).toString();

            TimeZone tz = TimeZone.getTimeZone("UTC");
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            df.setTimeZone(tz);
            locationDate = df.format(new Date(location.getTime()));

            TimeZone tz1 = TimeZone.getDefault();
            DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            df1.setTimeZone(tz1);
            localLocationDate = df1.format(new Date(location.getTime()));
        }

        // Broadcast to the plugins.
        DbGlobalsHelper dbGlobalsHelper = new DbGlobalsHelper(context);
        boolean doBroadcast = Utils.isBoolean(dbGlobalsHelper.getCursorGlobalsByKey(Constants.DB_KEY_BROADCAST));
        if (doBroadcast){
            doBroadcastToPlugins(transition, ze, realLat, realLng, location_accuracy);
        }

        if (ze.getServerEntity() != null){
            log.info("Send server request...");

            String urlEntered = Utils.replaceAll(context, ze.getServerEntity().getUrl_enter(), ze.getName(), ze.getAlias(), transition, ze.getRadius(),
                    ze.getLatitude(), ze.getLongitude(), realLat, realLng, locationDate, localLocationDate, location_accuracy);

            String urlExited = Utils.replaceAll(context, ze.getServerEntity().getUrl_exit(), ze.getName(), ze.getAlias(), transition, ze.getRadius(),
                    ze.getLatitude(), ze.getLongitude(), realLat, realLng, locationDate, localLocationDate, location_accuracy);

            doServerRequest(transition, context, urlEntered, urlExited, ze.getServerEntity().getUrl_fhem(),
                    ze.getName(), ze.getLatitude() == null ? "0" : ze.getLatitude(),
                    ze.getLongitude() == null ? "0" : ze.getLongitude(),
                    ze.getServerEntity().getCert(), ze.getServerEntity().getCert_password(),
                    ze.getServerEntity().getCa_cert(), ze.getServerEntity().getUser(),
                    ze.getServerEntity().getUser_pw(), ze.getServerEntity().getTimeout(), ze.getAlias(), realLat, realLng, false, 0);
        }
        if (ze.getMailEntity() != null){
            log.info("Send email...");
            if ((transition == Geofence.GEOFENCE_TRANSITION_ENTER && ze.getMailEntity().isEnter()) ||
                    (transition == Geofence.GEOFENCE_TRANSITION_EXIT && ze.getMailEntity().isExit())){

                String subjectReplace = Utils.replaceAll(context, ze.getMailEntity().getSubject(), ze.getName(), ze.getAlias(), transition, ze.getRadius(),
                        ze.getLatitude(), ze.getLongitude(), realLat, realLng, locationDate, localLocationDate, location_accuracy);
                String textReplace = Utils.replaceAll(context, ze.getMailEntity().getBody(), ze.getName(), ze.getAlias(), transition, ze.getRadius(),
                        ze.getLatitude(), ze.getLongitude(), realLat, realLng, locationDate, localLocationDate, location_accuracy);

                doSendMail(context, ze.getName(), subjectReplace, textReplace, ze.getMailEntity().getSmtp_user(), ze.getMailEntity().getSmtp_pw(), ze.getMailEntity().getSmtp_server(),
                        ze.getMailEntity().getSmtp_port(), ze.getMailEntity().getFrom(), ze.getMailEntity().getTo(), ze.getMailEntity().isSsl(), ze.getMailEntity().isStarttls(), false);
            }
        }
        if (ze.getSmsEntity() != null){
            log.info("Send sms...");
            if ((transition == Geofence.GEOFENCE_TRANSITION_ENTER && ze.getSmsEntity().isEnter()) ||
                    (transition == Geofence.GEOFENCE_TRANSITION_EXIT && ze.getSmsEntity().isExit())){

                String textReplace = Utils.replaceAll(context, ze.getSmsEntity().getText(), ze.getName(), ze.getAlias(), transition, ze.getRadius(),
                        ze.getLatitude(), ze.getLongitude(), realLat, realLng, locationDate, localLocationDate, location_accuracy);
                String text = textReplace.length() > 155 ? textReplace.substring(0,155) : textReplace;

                doSendSms(context, ze.getName(), ze.getSmsEntity().getNumber(), text, false);
            }
        }

        if (ze.getMoreEntity() != null) {
            doWifi(context, ze, transition);
            doBluetooth(context, ze, transition);
            doSound(context, ze, transition);
            doSoundMM(context, ze, transition);
            // Start beacon scanner before start of beacon zone, if configured
            doStartBeaconScanner(context, ze, transition, dbGlobalsHelper);
            doCallTasker(context, ze, transition);
        }

        // Start beacon zone, if configured
        if (ze.getId_beacon() != null) {
            doStartBeaconZone(context, ze, datasourceZone);
        }

        doTracking(context, ze, transition, dbGlobalsHelper);


    }

    // checkConditionBluetoothDeviceConnected
    private boolean checkConditionBluetoothDeviceConnected(Context context, ZoneEntity zone, int transition){
        log.info("checkConditionBluetoothDeviceConnected ...");

        if (zone.getRequirementsEntity() == null) return true;

        String bt_name_enter = zone.getRequirementsEntity().getEnter_bt();
        String bt_name_exit = zone.getRequirementsEntity().getExit_bt();

        PackageManager packageManager = context.getPackageManager();
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)){
            log.error("No bluetooth on device!");
            Toast.makeText(context, "No bluetooth on device!", Toast.LENGTH_LONG).show();
            return true;
        }

        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            if (bt_name_enter == null || bt_name_enter.equalsIgnoreCase("") || bt_name_enter.equalsIgnoreCase("none")) {
                return true;
            }
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            return bluetoothAdapter.isEnabled() && GlobalSingleton.getInstance().getBtDevicesConnected().contains(bt_name_enter);
        }

        if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            if (bt_name_exit == null || bt_name_exit.equalsIgnoreCase("") || bt_name_exit.equalsIgnoreCase("none")) {
                return true;
            }
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            return bluetoothAdapter.isEnabled() && GlobalSingleton.getInstance().getBtDevicesConnected().contains(bt_name_exit);

        }
        return false;
    }

    // Auf Wochentage prüfen
    private boolean checkWeekday(ZoneEntity zone){
        log.info("checkWeekdays ...");

        if (zone.getRequirementsEntity() == null) return true;

        Calendar c = Calendar.getInstance();
        // Sonntag = 1
        // Montag = 2
        // usw.
        int day_of_week = c.get(Calendar.DAY_OF_WEEK);
        log.debug("Day is " + day_of_week);

        switch (day_of_week) {
            case Calendar.SUNDAY:
                return zone.getRequirementsEntity().isSun();
            case Calendar.MONDAY:
                return zone.getRequirementsEntity().isMon();
            case Calendar.TUESDAY:
                return zone.getRequirementsEntity().isTue();
            case Calendar.WEDNESDAY:
                return zone.getRequirementsEntity().isWed();
            case Calendar.THURSDAY:
                return zone.getRequirementsEntity().isThu();
            case Calendar.FRIDAY:
                return zone.getRequirementsEntity().isFri();
            case Calendar.SATURDAY:
                return zone.getRequirementsEntity().isSat();
            default:
                return true;
        }
    }



    // WLAN/Wifi
    public void doWifi(Context context, ZoneEntity zone, int transition){
        log.info("doWifi ...");
        PackageManager packageManager = context.getPackageManager();
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)){
            log.error("No Wifi on device!");
            Toast.makeText(context, "No Wifi on device!", Toast.LENGTH_LONG).show();
            return;
        }

        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER){
            if (zone.getMoreEntity().getEnter_wifi() != null){
                if (zone.getMoreEntity().getEnter_wifi() == 1){
                    if (!wifiManager.isWifiEnabled())
                        wifiManager.setWifiEnabled(true);
                }
                if (zone.getMoreEntity().getEnter_wifi() == 0){
                    if (wifiManager.isWifiEnabled())
                        wifiManager.setWifiEnabled(false);
                }
            }
        }
        if (transition == Geofence.GEOFENCE_TRANSITION_EXIT){
            if (zone.getMoreEntity().getExit_wifi() != null){
                if (zone.getMoreEntity().getExit_wifi() == 1){
                    if (!wifiManager.isWifiEnabled())
                        wifiManager.setWifiEnabled(true);
                }
                if (zone.getMoreEntity().getExit_wifi() == 0){
                    if (wifiManager.isWifiEnabled())
                        wifiManager.setWifiEnabled(false);
                }
            }
        }
    }
    // Bluetooth
    public void doBluetooth(Context context, ZoneEntity zone, int transition){
        log.info("doBluetooth ...");
        PackageManager packageManager = context.getPackageManager();
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)){
            log.error("No bluetooth on device!");
            Toast.makeText(context, "No bluetooth on device!", Toast.LENGTH_LONG).show();
            return;
        }

        BluetoothAdapter bluetoothAdapter  = BluetoothAdapter.getDefaultAdapter();
        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER){
            if (zone.getMoreEntity().getEnter_bt() != null){
                if (zone.getMoreEntity().getEnter_bt() == 1){
                    if (!bluetoothAdapter.isEnabled())
                        bluetoothAdapter.enable();
                }
                if (zone.getMoreEntity().getEnter_bt() == 0){
                    if (bluetoothAdapter.isEnabled())
                        bluetoothAdapter.disable();
                }
            }
        }
        if (transition == Geofence.GEOFENCE_TRANSITION_EXIT){
            if (zone.getMoreEntity().getExit_bt() != null){
                if (zone.getMoreEntity().getExit_bt() == 1){
                    if (!bluetoothAdapter.isEnabled())
                        bluetoothAdapter.enable();
                }
                if (zone.getMoreEntity().getExit_bt() == 0){
                    if (bluetoothAdapter.isEnabled())
                        bluetoothAdapter.disable();
                }
            }
        }
    }

    // Start BeaconScanner
    private void doStartBeaconScanner(Context context, ZoneEntity zone, int transition, DbGlobalsHelper dbGlobalsHelper){
        log.info("doStartBeaconScanner ...");
        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER){
            if (zone.getMoreEntity().getEnter_btscan() != null){
                if (zone.getMoreEntity().getEnter_btscan() == 1){
                    dbGlobalsHelper.storeGlobals(Constants.DB_KEY_BEACON_SCAN, "true");
                    if (zone.getMoreEntity().getEnter_btscan_on_timeout() == 0) {
                        ((EgiGeoZoneApplication) context.getApplicationContext()).bind();
                    }else{
                        startStopBeaconScanner("start", zone.getMoreEntity().getEnter_btscan_on_timeout());
                    }
                }
                if (zone.getMoreEntity().getEnter_btscan() == 0){
                    dbGlobalsHelper.storeGlobals(Constants.DB_KEY_BEACON_SCAN, "false");
                    if (zone.getMoreEntity().getEnter_btscan_off_timeout() == 0) {
                        ((EgiGeoZoneApplication) context.getApplicationContext()).unbind();
                    }else{
                        startStopBeaconScanner("stop", zone.getMoreEntity().getEnter_btscan_off_timeout());
                    }
                }
            }
        }
        if (transition == Geofence.GEOFENCE_TRANSITION_EXIT){
            if (zone.getMoreEntity().getExit_btscan() != null){
                if (zone.getMoreEntity().getExit_btscan() == 1){
                    dbGlobalsHelper.storeGlobals(Constants.DB_KEY_BEACON_SCAN, "true");
                    if (zone.getMoreEntity().getExit_btscan_on_timeout() == 0) {
                        ((EgiGeoZoneApplication) context.getApplicationContext()).bind();
                    }else{
                        startStopBeaconScanner("start", zone.getMoreEntity().getExit_btscan_on_timeout());
                    }
                }
                if (zone.getMoreEntity().getExit_btscan() == 0){
                    dbGlobalsHelper.storeGlobals(Constants.DB_KEY_BEACON_SCAN, "false");
                    if (zone.getMoreEntity().getExit_btscan_off_timeout() == 0) {
                        ((EgiGeoZoneApplication) context.getApplicationContext()).unbind();
                    }else{
                        startStopBeaconScanner("stop", zone.getMoreEntity().getExit_btscan_off_timeout());
                    }
                }
            }
        }
    }

    private void startStopBeaconScanner(String type, int timeout) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                ComponentName serviceComponent = new ComponentName(context, StartStopBtScannerJobSchedulerService.class);
                JobInfo.Builder builder = new JobInfo.Builder(kJobId++, serviceComponent);
                PersistableBundle extras = new PersistableBundle();
                extras.putString("beaconScannerType", type);

                builder.setMinimumLatency(timeout * 60 * 1000);
                builder.setExtras(extras);
                JobInfo jobInfo = builder.build();
                JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                int result = jobScheduler.schedule(jobInfo);
                log.debug("############ StartStopBtScannerJob: result = " + result);
                if (result == JobScheduler.RESULT_SUCCESS) {
                    log.error("StartStopBtScannerJob scheduled successfully!");
                }
            } catch (Exception e) {
                log.error("Error Starting StartStopBtScannerJob with JobScheduler: " + e);
            }
        }else{
            // Set alarm after 15 minutes to set back scan periods, if something goes wrong
            Intent intentAlarm = new Intent(context, MyBeaconScannerAlarmReceiver.class);
            intentAlarm.putExtra("beaconScannerType", type);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 235325253, intentAlarm, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (timeout * 60 * 1000), pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (timeout * 60 * 1000), pendingIntent);
            }

        }

    }

    // Sound
    public void doSound(Context context, ZoneEntity zone, int transition) {
        log.info("doSound ...");
        try {
            AudioManager aManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                if (zone.getMoreEntity().getEnter_sound() != null) {
                    final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        if (!notificationManager.isNotificationPolicyAccessGranted()) {
                            NotificationUtil.sendErrorNotificationWithButtons(context, context.getString(R.string.doNotDisturbPermissionsTitle), context.getString(R.string.doNotDisturbPermissionsMessage));
                            context.registerReceiver(myReceiver, new IntentFilter(Constants.ACTION_DONOTDISTURB_OK));
                            context.registerReceiver(myReceiver, new IntentFilter(Constants.ACTION_DONOTDISTURB_NOK));
                        }
                    }
                    if (zone.getMoreEntity().getEnter_sound() == 1) {
                        // Ton an
                        aManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                    }
                    if (zone.getMoreEntity().getEnter_sound() == 0) {
                        // Ton aus
                        aManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                    }
                    if (zone.getMoreEntity().getEnter_sound() == 3) {
                        // Vibration an
                        aManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                    }
                }
            }
            if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                if (zone.getMoreEntity().getExit_sound() != null) {
                    final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        if (!notificationManager.isNotificationPolicyAccessGranted()) {
                            NotificationUtil.sendErrorNotificationWithButtons(context, context.getString(R.string.doNotDisturbPermissionsTitle), context.getString(R.string.doNotDisturbPermissionsMessage));
                            context.registerReceiver(myReceiver, new IntentFilter(Constants.ACTION_DONOTDISTURB_OK));
                            context.registerReceiver(myReceiver, new IntentFilter(Constants.ACTION_DONOTDISTURB_NOK));
                        }
                    }
                    if (zone.getMoreEntity().getExit_sound() == 1) {
                        // Ton an
                        aManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                    }
                    if (zone.getMoreEntity().getExit_sound() == 0) {
                        // Ton aus
                        aManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                    }
                    if (zone.getMoreEntity().getExit_sound() == 3) {
                        // Vibration an
                        aManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                    }
                }
            }
        }catch(Exception sec){
            if (sec instanceof SecurityException) {
                log.error("Error: No sound permission accepted - " + sec);
            }else{
                log.error("Error: " + sec);
            }
        }
    }

    // SoundMM
    public void doSoundMM(Context context, ZoneEntity zone, int transition) {
        log.info("doSoundMM ...");

        AudioManager aManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            if (zone.getMoreEntity().getEnter_soundMM() != null) {
                final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    if (!notificationManager.isNotificationPolicyAccessGranted()) {
                        NotificationUtil.sendErrorNotificationWithButtons(context, context.getString(R.string.doNotDisturbPermissionsTitle), context.getString(R.string.doNotDisturbPermissionsMessage));
                        context.registerReceiver(myReceiver, new IntentFilter(Constants.ACTION_DONOTDISTURB_OK));
                        context.registerReceiver(myReceiver, new IntentFilter(Constants.ACTION_DONOTDISTURB_NOK));
                    }
                }
                if (zone.getMoreEntity().getEnter_soundMM() == 1) {
                    // MM Ton an
                    int vol = aManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2;
                    aManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0);
                }
                if (zone.getMoreEntity().getEnter_soundMM() == 0) {
                    // MM Ton aus
                    aManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                }
            }
        }
        if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            if (zone.getMoreEntity().getExit_soundMM() != null) {
                final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    if (!notificationManager.isNotificationPolicyAccessGranted()) {
                        NotificationUtil.sendErrorNotificationWithButtons(context, context.getString(R.string.doNotDisturbPermissionsTitle), context.getString(R.string.doNotDisturbPermissionsMessage));
                        context.registerReceiver(myReceiver, new IntentFilter(Constants.ACTION_DONOTDISTURB_OK));
                        context.registerReceiver(myReceiver, new IntentFilter(Constants.ACTION_DONOTDISTURB_NOK));
                    }
                }
                if (zone.getMoreEntity().getExit_soundMM() == 1) {
                    // MM Ton an
                    int vol = aManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2;
                    aManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0);
                }
                if (zone.getMoreEntity().getExit_soundMM() == 0) {
                    // MM Ton aus
                    aManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                }
            }
        }
    }


    // Mail senden
    public void doSendMail(Context context, String zone, String subject, String text, String mailUser, String mailUserPw, String mailSmtpHost,
                           String mailSmtpPort, String mailSender, String mailEmpf, boolean mailSsl, boolean mailStarttls, boolean test) {
        log.info("doSendMail");
        log.debug("zone: " + zone);
        log.debug("mail to: " + mailEmpf);
        log.debug("mail subject: " + subject);
        log.debug("mail text: " + text);
        log.debug("mail user: " + mailUser);
        log.debug("mail host: " + mailSmtpHost);
        log.debug("mail port: " + mailSmtpPort);
        log.debug("mail sender: " + mailSender);
        log.debug("mail ssl: " + mailSsl);
        log.debug("mail starttls: " + mailStarttls);

        try {
            // Mail senden
            SendMail smail = new SendMail(context, mailUser, mailUserPw, mailSmtpHost, mailSmtpPort, mailSender, mailEmpf, mailSsl, mailStarttls);
            smail.sendMail(subject, text, test);

        } catch (Exception ex) {
            Log.e(Constants.APPTAG, "error sending mail", ex);
            log.error(zone + ": Error sending mail", ex);
            NotificationUtil.showError(context, zone + ": Error sending mail", ex.toString());
            // TestErgebnis
            if (test){
                // Broadcast an die Main, damit der Drawer sich refreshed.
                Intent intent = new Intent();
                intent.setAction(Constants.ACTION_TEST_STATUS_NOK);
                intent.putExtra("TestResult", "Error sending mail: " + ex.toString());
                context.sendBroadcast(intent);
            }
        }
    }

    // Tasker aufrufen
    public void doCallTasker(Context context, ZoneEntity zone, int transition) {
        log.info("doCallTasker");
        log.debug("zone: " + zone.getId());

        try {
            // Task im Tasker aufrufen
            String task = null;
            String taskTransition = "";
            if (transition == Geofence.GEOFENCE_TRANSITION_ENTER){
                task = zone.getMoreEntity().getEnter_task();
                taskTransition = "1"; // wie in fhem
            }
            if (transition == Geofence.GEOFENCE_TRANSITION_EXIT){
                task = zone.getMoreEntity().getExit_task();
                taskTransition = "0"; // wie in fhem
            }

            // Wenn kein Task angegeben, dann nichts tun
            if (task == null || task.equals("")) return;

            // Task wurde angeben
            if (TaskerIntent.testStatus(context).equals( TaskerIntent.Status.OK)){
                TaskerIntent i = new TaskerIntent(task);

                // Parameter und Variablen für Task, die damit was anfangen können.
                i.addParameter(zone.getName()); // Zonenname
                i.addParameter(taskTransition); // Transition: 1 = Enter; 0 = Exit
                i.addParameter(zone.getLatitude()); // Breitengrad
                i.addParameter(zone.getLongitude()); // Längengrad

                i.addLocalVariable("%zone", zone.getName()); // Zonenname
                i.addLocalVariable("%transition", taskTransition); // Transition: 1 = Enter; 0 = Exit
                i.addLocalVariable("%latitude", zone.getLatitude()); // Breitengrad
                i.addLocalVariable("%longitude", zone.getLongitude()); // Längengrad

                context.sendBroadcast(i);

            }else if (TaskerIntent.testStatus(context).equals( TaskerIntent.Status.NoPermission)){
                Log.e(Constants.APPTAG, "NoPermission: calling app does not have the needed Android permission");
                log.error(zone.getId() + ": NoPermission: calling app does not have the needed Android permission");
                NotificationUtil.showError(context, zone.getId() + ": Error calling Tasker", "NoPermission: calling app does not have the needed Android permission");
            }else if (TaskerIntent.testStatus(context).equals( TaskerIntent.Status.NoReceiver)){
                Log.e(Constants.APPTAG, "NoReceiver: nothing is listening for TaskerIntents. Probably a Tasker bug.");
                log.error(zone.getId() + ": NoReceiver: nothing is listening for TaskerIntents. Probably a Tasker bug.");
                NotificationUtil.showError(context, zone.getId() + ": Error calling Tasker", "NoReceiver: nothing is listening for TaskerIntents. Probably a Tasker bug.");
            }else if (TaskerIntent.testStatus(context).equals( TaskerIntent.Status.NotEnabled)){
                Log.e(Constants.APPTAG, "NotEnabled: Tasker is disabled by the user.");
                log.error(zone.getId() + ": NotEnabled: Tasker is disabled by the user.");
                NotificationUtil.showError(context, zone.getId() + ": Error calling Tasker", "NotEnabled: Tasker is disabled by the user.");
            }else if (TaskerIntent.testStatus(context).equals( TaskerIntent.Status.NotInstalled)){
                Log.e(Constants.APPTAG, "NotInstalled: no Tasker App could be found on the device");
                log.error(zone.getId() + ": NotInstalled: no Tasker App could be found on the device");
                NotificationUtil.showError(context, zone.getId() + ": Error calling Tasker", "NotInstalled: no Tasker App could be found on the device");
            }else if (TaskerIntent.testStatus(context).equals( TaskerIntent.Status.AccessBlocked)){
                Log.e(Constants.APPTAG, "AccessBlocked: external access is blocked in the user preferences.");
                log.error(zone.getId() + ": AccessBlocked: external access is blocked in the user preferences.");
                NotificationUtil.showError(context, zone.getId() + ": Error calling Tasker", "AccessBlocked: external access is blocked in the user preferences.");
            }
        } catch (Exception ex) {
            Log.e(Constants.APPTAG, "error calling Tasker", ex);
            log.error(zone.getId() + ": Error calling Tasker", ex);
            NotificationUtil.showError(context, zone.getId() + ": Error calling Tasker", ex.toString());
        }
    }

    // SMS senden
    public void doSendSms(Context context, String zone, String to, String text, boolean test) {
        PackageManager packageManager = context.getPackageManager();
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)){
            log.error("No Telephony on device!");
            Toast.makeText(context, "No Telephony on device!", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            log.info("doSendSms");
            log.debug("zone: " + zone);
            log.debug("sms to: " + to);
            log.debug("sms text: " + text);

            // SMS senden
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(to, null, text, null, null);

            // TestErgebnis
            if (test){
                // Broadcats damit der Test-Dialog angezeigt wird
                Intent intent = new Intent();
                intent.setAction(Constants.ACTION_TEST_STATUS_OK);
                context.sendBroadcast(intent);
            }

        } catch (Exception ex) {
            Log.e(Constants.APPTAG, "error sending sms", ex);
            log.error(zone + ": Error sending sms", ex);
            NotificationUtil.showError(context, zone + ": Error sending sms", ex.toString());
            // TestErgebnis
            if (test){
                // Broadcats damit der Test-Dialog angezeigt wird
                Intent intent = new Intent();
                intent.setAction(Constants.ACTION_TEST_STATUS_NOK);
                context.sendBroadcast(intent);
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void doServerRequest(final int transition, final Context context, final String urlEntered, final String urlExited, final String fhemGeofancyUrl, final String zone,
                                final String latitude, final String longitude, final String cert, final String certPasswd, final String caCert, final String user,
                                final String userPasswd, final String timeout, final String alias, final String realLat, final String realLng,
                                final boolean test, final int retrys) {

        log.info("doServerRequest");
        try {
            final AuthenticationParameters authParams = new AuthenticationParameters();
            String fhemTransition = "";
            log.debug("Transition: " + transition);

            // 1 == Enter
            if (transition == Geofence.GEOFENCE_TRANSITION_ENTER){
                authParams.setUrl(urlEntered);
                fhemTransition = "1";
            }
            // 2 = Exit
            if (transition == Geofence.GEOFENCE_TRANSITION_EXIT){
                authParams.setUrl(urlExited);
                fhemTransition = "0";
            }
            // Wenn keine URL angegeben wurde, dann nichts tun
            if (TextUtils.isEmpty(urlEntered) && TextUtils.isEmpty(urlExited) && TextUtils.isEmpty(fhemGeofancyUrl)){
                log.info("No URL set. Exiting.");
                return;
            }else{
                // Hier URL für Fhem basteln, wenn Fhem-Adresse/Location angegeben wurde.
                if (!TextUtils.isEmpty(fhemGeofancyUrl)){
                    log.info("FhemGeofancyUrl: requested");

                    TimeZone tz = TimeZone.getTimeZone("UTC");
                    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                    df.setTimeZone(tz);
                    String nowAsISO = df.format(new Date());
                    final String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
                    // Use the Android ID unless it's broken, in which case fallback on deviceId,
                    // unless it's not available, then fallback on a random number which we store
                    // to a prefs file
                    UUID uuid = null;
                    try {
                        if (!"9774d56d682e549c".equals(androidId)) {
                            uuid = UUID.nameUUIDFromBytes(androidId.getBytes("utf8"));
                        } else {
                            final String deviceId = ((TelephonyManager) context.getSystemService( Context.TELEPHONY_SERVICE )).getDeviceId();
                            uuid = deviceId!=null ? UUID.nameUUIDFromBytes(deviceId.getBytes("utf8")) : UUID.randomUUID();
                        }
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    } catch (SecurityException e) {
                        // Permission read phone state is missing
                    }

                    // /$infix?id=UUIDloc&name=locName&entry=(1|0)&date=DATE&latitude=xx.x&longitude=xx.x&device=UUIDdev
                    StringBuilder fhemGeofancy = new StringBuilder();
                    fhemGeofancy.append("id=");
                    fhemGeofancy.append(uuid != null ? uuid.toString() : "0");
                    fhemGeofancy.append("&name=");
                    fhemGeofancy.append(TextUtils.isEmpty(alias) ? zone : alias);
                    fhemGeofancy.append("&entry=");
                    fhemGeofancy.append(fhemTransition);
                    fhemGeofancy.append("&date=");
                    fhemGeofancy.append(nowAsISO);
                    fhemGeofancy.append("&latitude=");
                    fhemGeofancy.append(latitude);
                    fhemGeofancy.append("&longitude=");
                    fhemGeofancy.append(longitude);
                    fhemGeofancy.append("&device=");
                    fhemGeofancy.append(uuid.toString());

                    if (fhemGeofancyUrl.endsWith("?")){
                        authParams.setUrl(fhemGeofancyUrl + fhemGeofancy.toString());
                        authParams.setUrlXHR(fhemGeofancyUrl + XHR);
                    }else{
                        authParams.setUrl(fhemGeofancyUrl + "?" + fhemGeofancy.toString());
                        authParams.setUrlXHR(fhemGeofancyUrl + "?" + XHR);
                    }

                    // For csrfToken in Fhem
                    authParams.setGeofancyUrl(true);
                }
            }

            // No Url set --> return
            if (authParams.getUrl().isEmpty()) return;

            authParams.setClientCertificate(TextUtils.isEmpty(cert) ? null : getClientCertFile(cert));
            authParams.setClientCertificatePassword(TextUtils.isEmpty(certPasswd) ? null : certPasswd);
            authParams.setCaCertificate(TextUtils.isEmpty(caCert) ? null : readCaCert(caCert));
            authParams.setUser(TextUtils.isEmpty(user) ? null : user);
            authParams.setUserPasswd(TextUtils.isEmpty(userPasswd) ? null : userPasswd);

            log.debug("server zone: " + zone);
            log.debug("server alias: " + alias);
            log.debug("server urlEntered: " + urlEntered);
            log.debug("server urlExited: " + urlExited);
            log.debug("server urlFhemGeofancy: " + fhemGeofancyUrl);

            log.debug("server url chosen: " + authParams.getUrl());
            log.debug("server  user: " + user);
            log.debug("server client_cert: " + cert);
            log.debug("server ca_cert: " + caCert);

            log.debug("server latitude: " + latitude);
            log.debug("server longitude: " + longitude);
            log.debug("server timeout: " + timeout);


            Log.d(Constants.APPTAG, "SimpleGeofence: " + authParams.getUrl());

            geoApi = new Api(authParams, timeout);

            new AsyncTask() {
                @Override
                protected Object doInBackground(Object... objects) {


                    try {
                        if (authParams.isGeofancyUrl()) {
                            String result = geoApi.doGet(true);
                            if (geoApi.getLastResponseCode() == 200){
                                if (result != null && !result.equals("")) {
                                    authParams.setUrl(authParams.getUrl() + FWCSRF + result);
                                }
                            }
                        }
                    }catch(Exception e){
                        // ??
                    }
                    try {
                        geoApi.doGet(false);

                        int responseCode = geoApi.getLastResponseCode();
                        if (responseCode == 200) {
                            if (test){
                                // Broadcast damit der Test-Dialog angezeigt wird
                                Intent intent = new Intent();
                                intent.setAction(Constants.ACTION_TEST_STATUS_OK);
                                intent.putExtra("TestType", "GeoZone");
                                context.sendBroadcast(intent);
                            }

                            // Löschen des Events, damit der "alte" nicht mehr ausgeführt wird.
                            // Wenn Schalter Retry true
                            RetryRequestQueue.removePref(context, zone);

                            log.info("Response code after get: "  + responseCode);
                        } else {
                            if(fallback != null){
                                datasourceServer = new DbServerHelper(context);
                                ServerEntity se = datasourceServer.getCursorServerByName(fallback);
                                String fallbackServer = fallback;
                                fallback = null;
                                doServerRequest(transition, context, se.getUrl_enter(), se.getUrl_exit(), se.getUrl_fhem(), TextUtils.isEmpty(alias) ? zone : alias, latitude, longitude, se.getCert(),
                                        se.getCert_password(), se.getCa_cert(), se.getUser(), se.getUser_pw(), se.getTimeout(), alias, realLat, realLng, test, 0);
                                // Rest überspringen, da Fallback
                                return null;
                            }
                            log.error("Response code after get: "  + responseCode);
                            NotificationUtil.showError(context, zone + ": Error (GR01) in get of the server response", "Response Code: " + responseCode);
                            if (test){
                                // Broadcats damit der Test-Dialog angezeigt wird
                                Intent intent = new Intent();
                                intent.setAction(Constants.ACTION_TEST_STATUS_NOK);
                                intent.putExtra("TestResult", "Error (GR01) in get of the server response. Response Code: " + responseCode);
                                intent.putExtra("TestType", "GeoZone");
                                context.sendBroadcast(intent);
                            }

                        }
                    } catch (Throwable ex) {
                        if(fallback != null){
                            datasourceServer = new DbServerHelper(context);
                            ServerEntity se = datasourceServer.getCursorServerByName(fallback);
                            String fallbackServer = fallback;
                            fallback = null;
                            doServerRequest(transition, context, se.getUrl_enter(), se.getUrl_exit(), se.getUrl_fhem(), TextUtils.isEmpty(alias) ? zone : alias, latitude, longitude, se.getCert(),
                                    se.getCert_password(), se.getCa_cert(), se.getUser(), se.getUser_pw(), se.getTimeout(), alias, realLat, realLng, test, 0);
                            // Rest überspringen, da Fallback
                            return null;
                        }

                        log.error(zone + ": Error (GR02) in get of the server response", ex);
                        NotificationUtil.showError(context, zone + ": Error (GR02) in get of the server response", ex.toString());

                        // Speichern des Events für einen späteren Request, wenn wieder Internet verfügbar ist.
                        // Wenn Schalter Retry true
                        if (!test && retrys < 6) {
                            log.debug("Store retry number: " + retrys + 1);
                            RetryRequestQueue.setRequest(context, zone, transition, realLat, realLng, retrys + 1);
                            log.debug("############ RetryJob: 1 ");
                            // If Android 7+ retry with JobScheduler
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                log.debug("############ RetryJob: 2 ");
                                try {
//									RetryJobSchedulerService retryJobScheduler;
                                    ComponentName serviceComponent = new ComponentName(context, RetryJobSchedulerService.class);
                                    JobInfo.Builder builder = new JobInfo.Builder(kJobId++, serviceComponent);
                                    builder.setMinimumLatency(5 * 1000);
                                    builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
                                    JobInfo jobInfo = builder.build();
                                    JobScheduler jobScheduler =  (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                                    int result = jobScheduler.schedule(jobInfo);
                                    log.debug("############ RetryJob: result = " + result);
                                    if (result == JobScheduler.RESULT_SUCCESS) {
                                        log.error("RetryJob scheduled successfully!");
                                    }
                                }catch(Exception e){
                                    log.error("Error Starting RetryJob with JobScheduler: " + e);
                                }
                            }
                            log.error("The request for " + zone + " is queued and will be retried, when internet connection is available.");
                        }

                        if (test){
                            // Broadcats damit der Test-Dialog angezeigt wird
                            Intent intent = new Intent();
                            intent.setAction(Constants.ACTION_TEST_STATUS_NOK);
                            intent.putExtra("TestResult", "Error (GR02) in get of the server response: " + ex.toString());
                            intent.putExtra("TestType", "GeoZone");
                            context.sendBroadcast(intent);
                        }
                    }
                    return null;
                }

                @Override
                protected void onProgressUpdate(final Object... values) {
                }

                @Override
                protected void onPostExecute(final Object result) {
                }

            }.execute();

        } catch (Exception ex) {
            Log.e(Constants.APPTAG, "error sending server request", ex);
            log.error(zone + ": Error (GR03) sending server request", ex);
            NotificationUtil.showError(context, zone + ": Error (GR03) sending server request", ex.toString());
        }
    }

    private File getClientCertFile(String clientCertificateName) {
        File externalStorageDir = Environment.getExternalStorageDirectory();
        return new File(externalStorageDir + File.separator + "egigeozone", clientCertificateName);
    }

    private String readCaCert(String caCertificateName) throws Exception {
        File externalStorageDir = Environment.getExternalStorageDirectory();
        File caCert = new File(externalStorageDir + File.separator + "egigeozone", caCertificateName);
        InputStream inputStream = new FileInputStream(caCert);
        return IOUtil.readFully(inputStream);
    }

    // Broadcast only if plugins are installed
    private void doBroadcastToPlugins(int transition, ZoneEntity ze, String realLat, String realLng, String location_accuracy){

        PackageManager manager = context.getPackageManager();
        Intent intent = new Intent();
        intent.setAction(Constants.ACTION_EGIGEOZONE_GETPLUGINS);

        // Query for all activities that match my filter and request that the filter used
        //  to match is returned in the ResolveInfo
        List<ResolveInfo> infos = manager.queryIntentActivities (intent, PackageManager.GET_RESOLVED_FILTER);
        for (ResolveInfo info : infos) {
            ActivityInfo activityInfo = info.activityInfo;
            IntentFilter filter = info.filter;
            if (filter != null && filter.hasAction(Constants.ACTION_EGIGEOZONE_GETPLUGINS)){
                String pckg = activityInfo.packageName;

                if (pckg == null || pckg.equals("")) continue;

                // Broadcast starten
                Intent plugintIntent = new Intent();
                plugintIntent.setAction(Constants.ACTION_EGIGEOZONE_PLUGIN_EVENT);
                plugintIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                plugintIntent.setPackage(pckg);

                String pluginTransition = "";
                if (transition == Geofence.GEOFENCE_TRANSITION_ENTER){
                    pluginTransition = "1"; // wie in fhem
                }
                if (transition == Geofence.GEOFENCE_TRANSITION_EXIT){
                    pluginTransition = "0"; // wie in fhem
                }

                plugintIntent.putExtra("zone_name", TextUtils.isEmpty(ze.getAlias()) ? ze.getName() : ze.getAlias()); // String
                plugintIntent.putExtra("transition", pluginTransition); // int
                plugintIntent.putExtra("latitude", ze.getLatitude()); // String
                plugintIntent.putExtra("longitude", ze.getLongitude()); // String

                plugintIntent.putExtra("realLatitude", realLat);
                plugintIntent.putExtra("realLongitude", realLng);
                plugintIntent.putExtra("location_accuracy", location_accuracy);

                TimeZone tz = TimeZone.getTimeZone("UTC");
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                df.setTimeZone(tz);
                String nowAsISO = df.format(new Date());
                final String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
                // Use the Android ID unless it's broken, in which case fallback on deviceId,
                // unless it's not available, then fallback on a random number which we store
                // to a prefs file
                UUID uuid = null;
                try {
                    if (!"9774d56d682e549c".equals(androidId)) {
                        uuid = UUID.nameUUIDFromBytes(androidId.getBytes("utf8"));
                    } else {
                        final String deviceId = ((TelephonyManager) context.getSystemService( Context.TELEPHONY_SERVICE )).getDeviceId();
                        uuid = deviceId!=null ? UUID.nameUUIDFromBytes(deviceId.getBytes("utf8")) : UUID.randomUUID();
                    }
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                } catch (SecurityException e) {
                    // Permission read phone state is missing
                }

                plugintIntent.putExtra("device_id", uuid != null ? uuid.toString() : "0"); // String
                plugintIntent.putExtra("date_iso", nowAsISO); // String

                TimeZone tz1 = TimeZone.getDefault();
                DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                df.setTimeZone(tz1);
                String nowAsLocal = df1.format(new Date());

                plugintIntent.putExtra("date_device", nowAsLocal); // String

                context.sendBroadcast(plugintIntent);
            }
        }
    }

    // Trackeing starten/stoppen
    private void doTracking(Context context, ZoneEntity ze, int transition, DbGlobalsHelper dbGlobalsHelper) {
        log.info("doTracker");
        log.debug("zone: " + ze.getName());
        try {

            // Zonen Einstellungen
            String zone = ze.getName();
            boolean trackEnter = ze.isEnter_tracker();
            boolean trackExit = ze.isExit_tracker();
            boolean trackToFile = ze.isTrack_to_file();
//			String trackUrl = ze.getTrack_url();
            int trackIntervallZone = ze.getLocal_tracking_interval() == null || ze.getLocal_tracking_interval() == 0 ? 5 : ze.getLocal_tracking_interval();

            if (transition == Geofence.GEOFENCE_TRANSITION_ENTER){
                // Stop all trackings, when entering one off the zones
                if (Utils.isBoolean(dbGlobalsHelper.getCursorGlobalsByKey(Constants.DB_KEY_STOPTRACKINGATENTER))){
                    // Alle Tracks stoppen
                    TrackingUtils.stopAllTrackings(context);
                    log.debug("All trackings stopped at entering of one zone!");
                    return;
                }

                if (trackEnter){
                    TrackingUtils.startTracking(context, zone, trackIntervallZone, trackToFile, (ze.getTrackServerEntity() != null), (ze.getTrackMailEntity() != null));
                }else{
                    if (TrackingUtils.exists(context, zone) > 0) TrackingUtils.stopTracking(context, zone);
                }
            }

            if (transition == Geofence.GEOFENCE_TRANSITION_EXIT){
                // Stop all trackinga, when exiting one off the zones
                if (Utils.isBoolean(dbGlobalsHelper.getCursorGlobalsByKey(Constants.DB_KEY_STOPTRACKINGATEXIT))){
                    // Alle Tracks stoppen
                    TrackingUtils.stopAllTrackings(context);
                    log.debug("All trackings stopped at exiting of one zone!");
                    return;
                }

                if (trackExit){
                    TrackingUtils.startTracking(context, zone, trackIntervallZone, trackToFile, (ze.getTrackServerEntity() != null), (ze.getTrackMailEntity() != null));
                }else{
                    if (TrackingUtils.exists(context, zone) > 0) TrackingUtils.stopTracking(context, zone);
                }
            }

        } catch (Exception ex) {
            Log.e(Constants.APPTAG, "error starting location tracking", ex);
            log.error(ze.getName() + ": Error starting location tracking", ex);
            NotificationUtil.showError(context, ze.getName() + ": Error starting location tracking", ex.toString());
        }
    }

    // Start beacon zone, if configured
    private void doStartBeaconZone(Context context, ZoneEntity ze, DbZoneHelper datasource) {
        log.info("Start monitoring/ranging beacon: " + ze.getId_beacon());
        ZoneEntity beacon = datasource.getCursorZoneByName(ze.getId_beacon());
        // Check if beacon was not deleted and the config was not changed
        if (beacon != null) {
            List<Identifier> listB = Utils.getStringIdentifiers(beacon.getBeacon());
            BeaconManager mBeaconManager = BeaconManager.getInstanceForApplication(context);
            try {
                log.debug("Worker: setRegion  " + beacon.getName());
                Region region = new Region(beacon.getName(), listB);
                // Set background scan to foreground values.
                mBeaconManager.setBackgroundBetweenScanPeriod(Utils.getDefaultForegroundBetweenScanPeriod(context));
                mBeaconManager.setBackgroundScanPeriod(Utils.getDefaultForegroundScanPeriod(context));

                mBeaconManager.updateScanPeriods();

                // Set alarm after 15 minutes to set back scan periods, if something goes wrong
                Intent intentAlarm = new Intent(context, de.egi.geofence.geozone.bt.beacon.MyBeaconAlarmReceiver.class);
                intentAlarm.putExtra("beaconZone", beacon.getName());
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 234324243, intentAlarm, PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (15 * 60 * 1000), pendingIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (15 * 60 * 1000), pendingIntent);
                }

                mBeaconManager.startRangingBeaconsInRegion(region);
                mBeaconManager.startMonitoringBeaconsInRegion(region);

            } catch (RemoteException e) {

                mBeaconManager.setBackgroundBetweenScanPeriod(Utils.getDefaultBackgroundBetweenScanPeriod(context));
                mBeaconManager.setBackgroundScanPeriod(Utils.getDefaultBackgroundScanPeriod(context));

                try {
                    mBeaconManager.updateScanPeriods();
                } catch (RemoteException e1) {
                    e1.printStackTrace();
                }

                log.error("Worker: setRegion failed " + beacon.getName() + " Please start Bluetooth and Beacon Scan");
                e.printStackTrace();
            }
        }
    }

    private Geofence getGeofence(ZoneEntity ze, int transition){
        return  new Geofence.Builder().setRequestId(ze.getName())
                .setTransitionTypes(transition)
                .setCircularRegion(Double.valueOf(ze.getLatitude()), Double.valueOf(ze.getLongitude()), ze.getRadius())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build();
    }

    /**
     * Maps geofence transition types to their human-readable equivalents.
     * @param transitionType A transition type constant defined in Geofence
     * @return A String indicating the type of transition
     */
    private String getTransitionString(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return context.getString(R.string.geofence_transition_entered);

            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return context.getString(R.string.geofence_transition_exited);

            default:
                return context.getString(R.string.geofence_transition_unknown);
        }
    }

    final BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action_name = intent.getAction();
            if (action_name.equals(Constants.ACTION_DONOTDISTURB_OK)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    context.startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
                }

            }
            if (action_name.equals(Constants.ACTION_DONOTDISTURB_NOK)) {
                // Do nothing
            }
            // Get an instance of the Notification manager
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(222);
            context.unregisterReceiver(myReceiver);
        };
    };


}
