package de.egi.geofence.geozone.bt;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationServices;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.Region;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import de.egi.geofence.geozone.bt.beacon.EgiGeoZoneBeacon;
import de.egi.geofence.geozone.bt.beacon.SimpleBeacon;
import de.egi.geofence.geozone.bt.beacon.SimpleBeaconStore;
import de.egi.geofence.geozone.bt.db.DbGlobalsHelper;
import de.egi.geofence.geozone.bt.db.DbZoneHelper;
import de.egi.geofence.geozone.bt.db.ZoneEntity;
import de.egi.geofence.geozone.bt.fences.BtFence;
import de.egi.geofence.geozone.bt.fences.GeoFence;
import de.egi.geofence.geozone.bt.gcm.GcmRegistrationIntentService;
import de.egi.geofence.geozone.bt.geofence.GeofenceRemover;
import de.egi.geofence.geozone.bt.geofence.GeofenceRequester;
import de.egi.geofence.geozone.bt.geofence.PathsenseGeofence;
import de.egi.geofence.geozone.bt.geofence.SimpleGeofence;
import de.egi.geofence.geozone.bt.geofence.SimpleGeofenceStore;
import de.egi.geofence.geozone.bt.profile.Profiles;
import de.egi.geofence.geozone.bt.utils.Constants;
import de.egi.geofence.geozone.bt.utils.NavDrawerItem;
import de.egi.geofence.geozone.bt.utils.NavDrawerListAdapter;
import de.egi.geofence.geozone.bt.utils.NotificationUtil;
import de.egi.geofence.geozone.bt.utils.RuntimePermissionsActivity;
import de.egi.geofence.geozone.bt.utils.Utils;
import de.mindpipe.android.logging.log4j.LogConfigurator;


// http://www.myandroidsolutions.com/2016/07/13/android-navigation-view-tabs/
// https://www.materialui.co/icons

// http://www.flaticon.com/search/13?word=map

// <div>Icons made by <a href="http://www.freepik.com" title="Freepik">Freepik</a> from <a href="http://www.flaticon.com" title="Flaticon">www.flaticon.com</a> is licensed by <a href="http://creativecommons.org/licenses/by/3.0/" title="Creative Commons BY 3.0" target="_blank">CC 3.0 BY</a></div>
// <div>Icons made by <a href="http://www.flaticon.com/authors/vignesh-oviyan" title="Vignesh Oviyan">Vignesh Oviyan</a> from <a href="http://www.flaticon.com" title="Flaticon">www.flaticon.com</a> is licensed by <a href="http://creativecommons.org/licenses/by/3.0/" title="Creative Commons BY 3.0" target="_blank">CC 3.0 BY</a></div>
// <div>Icons made by <a href="http://www.flaticon.com/authors/freepik" title="Freepik">Freepik</a> from <a href="http://www.flaticon.com" title="Flaticon">www.flaticon.com</a> is licensed by <a href="http://creativecommons.org/licenses/by/3.0/" title="Creative Commons BY 3.0" target="_blank">CC 3.0 BY</a></div>
// Icon made by Freepik from www.flaticon.com
public class MainEgiGeoZone extends RuntimePermissionsActivity
        implements NavigationView.OnNavigationItemSelectedListener, AdapterView.OnItemClickListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, SwipeRefreshLayout.OnRefreshListener{

    private GoogleApiClient mLocationClient;
    private final String TAG = "MainEgiGeoZone";
    private Location locationMerk = null;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private boolean typeGeoZone = true;
    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    // Dangerous permissions and permission groups.
    // http://developer.android.com/guide/topics/security/permissions.html
    public static final int REQUEST_LOCATION = 1;
    public static final int REQUEST_WRITE_EXTERNAL_STORAGE = 2;
    public static final int REQUEST_PHONE_STATE = 3;
    public static final int REQUEST_BLUETOOTH = 4; // Location
    public static final int REQUEST_SMS = 5;
    public static final int REQUEST_GET_ACCOUNTS = 6;

//    Manifest.permission.WRITE_EXTERNAL_STORAGE,
//    Manifest.permission.READ_PHONE_STATE,
//    Manifest.permission.ACCESS_FINE_LOCATION,
//    Manifest.permission.SEND_SMS,
//    Manifest.permission.WRITE_EXTERNAL_STORAGE,
//    Manifest.permission.GET_ACCOUNTS

    public static final String SEED_MASTER = "Ok.KOmM_V04_60#_HugeNdubEl";

    // Add geofences handler
    private GeofenceRequester mGeofenceRequester;
    private PathsenseGeofence mPathsenseGeofence;
    // Remove geofences handler
    private GeofenceRemover mGeofenceRemover;
    // Store the list of geofences to remove
    private List<String> mGeofenceIdsToRemove;

    // Store the current request
    private static Constants.REQUEST_TYPE mRequestType;
    // Store the current type of removal
    public static Constants.REMOVE_TYPE mRemoveType;

    private ListView list;
    // Persistent storage for geofences
    private SimpleGeofenceStore geofenceStore;
    // Persistent storage for Beacons
    private SimpleBeaconStore beaconStore;
    // Store a list of geofences to add
    private List<Geofence> mCurrentGeofences;
    // Store a list of beacons to add
    private List<EgiGeoZoneBeacon> mCurrentBeacons;
    private ArrayList<NavDrawerItem> navDrawerItems;
    private DbGlobalsHelper dbGlobalsHelper;

    final static LogConfigurator logConfigurator = new LogConfigurator();
    private Logger log;
    private Toolbar toolbar;
    //The BroadcastReceiver that listens for bluetooth broadcasts and status of zones
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                //Device is now connected
                GlobalSingleton.getInstance().getBtDevicesConnected().add(device.getName());
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                //Device has disconnected
                GlobalSingleton.getInstance().getBtDevicesConnected().remove(device.getName());
            }else if (Constants.ACTION_STATUS_CHANGED.equals(action)) {
                // Status einer Zone hat sich geändert
                // Zonen im Drawer neu laden
                String state = intent.getStringExtra("state");
                if (state.equalsIgnoreCase(Constants.GEOZONE)) {
                    fillListGeofences();
                }else {
                    fillListBTfences();
                }
//                list.invalidateViews();
            }
        }
    };

    @SuppressLint("BatteryLife")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.onActivityCreateSetTheme(this);

        setContentView(R.layout.activity_main_nav);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Utils.changeBackGroundToolbar(this, toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        final NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        // BLE-Menu entfernen
        if (Build.VERSION.SDK_INT < Constants.BEACON_MIN_BUILD) {
            navigationView.getMenu().removeItem(R.id.nav_bt);
        }

        if (!checkAllNeededPermissions()){
            requestAppPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE}, R.string.alertPermissions, 2000);
        } else {
            init();
                }
        }

//        if (!checkAllNeededPermissions()) {
//            // Display UI and wait for user interaction
//            android.support.v7.app.AlertDialog.Builder alertDialogBuilder = Utils.onAlertDialogCreateSetTheme(this);
//            alertDialogBuilder.setMessage(getString(R.string.alertPermissions));
//            alertDialogBuilder.setTitle(getString(R.string.titleAlertPermissions));
//
//            alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface arg0, int arg1) {
//                    Intent intent = new Intent();
//                    intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//                    intent.addCategory(Intent.CATEGORY_DEFAULT);
//                    intent.setData(Uri.parse("package:" + getPackageName()));
//                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
//                    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
//                    startActivity(intent);
//                    finish();
//                }
//            });
//            android.support.v7.app.AlertDialog alertDialog = alertDialogBuilder.create();
//            alertDialog.show();
//            return;
//        }
//    }
//        super.requestAppPermissions(new String[]{
//                Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                Manifest.permission.ACCESS_FINE_LOCATION,
//                Manifest.permission.GET_ACCOUNTS,
//                Manifest.permission.READ_PHONE_STATE,
//                Manifest.permission.SEND_SMS},
//                R.string.checkAll, REQUEST_WRITE_EXTERNAL_STORAGE);

    @SuppressLint("BatteryLife")
    protected void init() {
        dbGlobalsHelper = new DbGlobalsHelper(this);
        String level = dbGlobalsHelper.getCursorGlobalsByKey(Constants.DB_KEY_LOG_LEVEL);
        if (level == null || level.equalsIgnoreCase("")){
            level = Level.ERROR.toString();
        }
        log = Logger.getLogger(MainEgiGeoZone.class);
        if (logConfigurator.getFileName().equalsIgnoreCase("android-log4j.log")){
            logConfigurator.setFileName(Environment.getExternalStorageDirectory() + File.separator + "egigeozone" + File.separator + "egigeozone.log");
            logConfigurator.setUseFileAppender(true);
            logConfigurator.setRootLevel(Level.toLevel(level));
            // Set log level of a specific logger
            logConfigurator.setLevel("de.egi.geofence.geozone.bt", Level.toLevel(level));
            try {
                logConfigurator.configure();
                log.info("Logger set!");
                Log.i("", "Logger set!");
            } catch (Exception e) {
                // Nichts tun. Manchmal kann auf den Speicher nicht zugegriffen werden.
            }
        }

        log.debug("onCreate");

        // Akku-Optimierung ausschalten, da sonst kein Netzwerkbetrieb möglich wäre
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            Intent intent = new Intent();
            try {
                //noinspection StatementWithEmptyBody
                if (pm.isIgnoringBatteryOptimizations(packageName)) {
                    // Nichts tun
                } else {
                    intent.setAction(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    startActivity(intent);
                }
            }catch(Exception e){
                // Ignore
            }
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                if (((TextView) findViewById(R.id.fences)).getText().equals(getString(R.string.geoZones))) {
                    // Geofences
                    Intent ig = new Intent(MainEgiGeoZone.this, GeoFence.class);
                    ig.putExtra("action", "new");
                    startActivityForResult(ig, 4730);
                }else{
                    Intent ib = new Intent(MainEgiGeoZone.this, BtFence.class);
                    ib.putExtra("action", "new");
                    startActivityForResult(ib, 4731);
                }

            }
        });

//        checkPermissionsExternalStorage();

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);

        // Permanent notifcation, if requested
        boolean stickyNotification =  Utils.isBoolean(dbGlobalsHelper.getCursorGlobalsByKey(Constants.DB_KEY_STICKY_NOTIFICATION));
        if (stickyNotification) {
            NotificationUtil.sendPermanentNotification(getApplicationContext(), R.drawable.locating_geo, getString(R.string.text_running_notification), 7676);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // Instantiate a new geofence storage area
        geofenceStore = new SimpleGeofenceStore(this);
        // Instantiate a new beacon storage area
        beaconStore = new SimpleBeaconStore(this);
        // Instantiate the current List of geofences
        mCurrentGeofences = new ArrayList<>();
        mCurrentBeacons = new ArrayList<>();
        navDrawerItems = new ArrayList<>();

        dbGlobalsHelper = new DbGlobalsHelper(this);
//        globalProperties = dbGlobalsHelper.getCursorAllGlobals();

//        PathsenseLocationProviderApi mApi = PathsenseLocationProviderApi.getInstance(this);

        // Instantiate a Geofence requester
        if (mGeofenceRequester == null){
            mGeofenceRequester = new GeofenceRequester(this);
        }
        if (mPathsenseGeofence == null){
            mPathsenseGeofence = new PathsenseGeofence(this);
        }
        // Instantiate a Geofence remover
        mGeofenceRemover = new GeofenceRemover(this);

        GlobalSingleton.getInstance().setGeofenceRemover(mGeofenceRemover);

        list = (ListView) findViewById (R.id.list);
        list.setOnItemClickListener(this);

        IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        IntentFilter filter3 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        IntentFilter statusFilter = new IntentFilter(Constants.ACTION_STATUS_CHANGED);

        this.registerReceiver(mReceiver, filter1);
        this.registerReceiver(mReceiver, filter2);
        this.registerReceiver(mReceiver, filter3);
        this.registerReceiver(mReceiver, statusFilter);

        // Check device for Play Services APK. If check succeeds, proceed with GCM registration.
        if (servicesConnected()) {
            if (Utils.isBoolean(dbGlobalsHelper.getCursorGlobalsByKey(Constants.DB_KEY_GCM))){
                String regid = GcmRegistrationIntentService.getRegistrationId(this);
                if (regid.isEmpty()) {
                    // Start IntentService to register this application with GCM.
                    Intent intent = new Intent(this, GcmRegistrationIntentService.class);
                    startService(intent);
                }
            }
        } else {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }

        refreshFences();
        if (!mCurrentGeofences.isEmpty()){
            fillListGeofences();
        }else if (!mCurrentBeacons.isEmpty()){
            fillListBTfences();
        }else{
            drawer.openDrawer(GravityCompat.START);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch(item.getItemId()) {
            case R.id.action_settings:
                if (!checkAllNeededPermissions()){
                    requestAppPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE}, R.string.alertPermissions, 2000);
                    return true;
                }
                log.debug("onOptionsItemSelected: menu_settings");
                Intent i5 = new Intent(this, Settings.class);
                startActivityForResult(i5, 5004);
                return true;
            case R.id.action_help:
                Intent i2 = new Intent(this, Help.class);
                startActivity(i2);
                return true;
            case R.id.action_profiles:
                if (!checkAllNeededPermissions()){
                    requestAppPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE}, R.string.alertPermissions, 2000);
                    return true;
                }
                log.debug("onOptionsItemSelected: menu_profiles");
                Intent i3 = new Intent(this, Profiles.class);
                startActivityForResult(i3, 5005);
                return true;
            case R.id.action_tech_info:
                log.debug("onOptionsItemSelected: menu_item_info");
                Intent i4a = new Intent(this, TechInfo.class);
                startActivity(i4a);
                return true;
            case R.id.action_info:
                Intent i4 = new Intent(this, Info.class);
                startActivityForResult(i4, 6000);
                return true;
            case R.id.action_privacy:
                Intent i6 = new Intent(this, Privacy.class);
                startActivityForResult(i6, 6001);
                return true;
            case R.id.action_map_all:
                if (!checkAllNeededPermissions()){
                    requestAppPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE}, R.string.alertPermissions, 2000);
                    return true;
                }
                log.debug("onOptionsItemSelected: menu_item_map_all");
                Intent i7 = new Intent(this, KarteAll.class);
                startActivityForResult(i7, 6002);
                return true;
            case R.id.action_refresh:
                if (!checkAllNeededPermissions()){
                    requestAppPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE}, R.string.alertPermissions, 2000);
                    return true;
                }
                log.debug("onOptionsItemSelected: menu_item_refresh");
                mSwipeRefreshLayout.setRefreshing(true);
                if (typeGeoZone) {
                    fillListGeofences();
                }else{
                    fillListBTfences();
                }
                mSwipeRefreshLayout.setRefreshing(false);
                return true;
            // Pass through any other request
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        item.setChecked(true);
        if (id == R.id.nav_geofence) {
            ((TextView) findViewById(R.id.fences)).setText(R.string.geoZones);
            fillListGeofences();
        } else if (id == R.id.nav_bt) {
            ((TextView) findViewById(R.id.fences)).setText(R.string.btZones);
            fillListBTfences();
        } else if (id == R.id.nav_profiles) {
            log.debug("onNavigationItemSelected: menu_item_profile");
            Intent i2 = new Intent(this, Profiles.class);
            startActivityForResult(i2, 5005);

        } else if (id == R.id.nav_settings) {
            log.debug("onNavigationItemSelected: menu_item_settings");
            Intent i = new Intent(this, Settings.class);
            startActivityForResult(i, 5004);

        } else if (id == R.id.nav_info) {
            log.debug("onNavigationItemSelected: menu_item_info");
            Intent i3 = new Intent(this, Info.class);
            startActivityForResult(i3, 6000);

        } else if (id == R.id.nav_help) {
                log.debug("onNavigationItemSelected: menu_item_help");
            Intent i3 = new Intent(this, Help.class);
            startActivity(i3);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
            return true;
    }

    private void fillListGeofences(){
        if (!super.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)) return;

        locationMerk = null;
        typeGeoZone = true;
        mLocationClient = new GoogleApiClient.Builder(this, this, this).addApi(LocationServices.API).build();
        mLocationClient.connect();

        ((TextView) findViewById(R.id.fences)).setText(R.string.geoZones);
        refreshFences();
        setGeofenceList2Drawer();
    }

    private void fillListBTfences(){
        if (!super.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)) return;

        locationMerk = null;
        typeGeoZone = false;
        mLocationClient = new GoogleApiClient.Builder(this, this, this).addApi(LocationServices.API).build();
        mLocationClient.connect();

        ((TextView) findViewById(R.id.fences)).setText(R.string.btZones);
        refreshFences();
        setBTfenceList2Drawer();
    }


    private void setGeofenceList2Drawer() {
        navDrawerItems.clear();
        // Gespeicherte GeoZonen auflisten
        for (int i = 0; i < mCurrentGeofences.size(); i++) {
            Geofence geofence = mCurrentGeofences.get(i);
            String dist = "";
            if (locationMerk != null){
                // Calculate distance to fence center
                try {
                    DbZoneHelper dbZoneHelper = new DbZoneHelper(this);
                    ZoneEntity zoneEntity = dbZoneHelper.getCursorZoneByName(geofence.getRequestId());
                    Location locationZone = new Location("locationZone");
                    locationZone.setLatitude(Double.valueOf(zoneEntity.getLatitude()));
                    locationZone.setLongitude(Double.valueOf(zoneEntity.getLongitude()));

                    float distanceMeters = locationMerk.distanceTo(locationZone);
                    if (distanceMeters < 50) {
                        dist = getString(R.string.distanceLess);
                    } else if (distanceMeters < 1000) {
                        NumberFormat formatter = NumberFormat.getInstance(Locale.getDefault());
                        formatter.setMaximumFractionDigits(2);
                        formatter.setMinimumFractionDigits(2);
                        formatter.setRoundingMode(RoundingMode.HALF_UP);
                        String formatedFloat = formatter.format(distanceMeters);
                        dist = getString(R.string.distance) + " " + formatedFloat + "m";
                    } else {
                        NumberFormat formatter = NumberFormat.getInstance(Locale.getDefault());
                        formatter.setMaximumFractionDigits(2);
                        formatter.setMinimumFractionDigits(2);
                        formatter.setRoundingMode(RoundingMode.HALF_UP);
                        String formatedFloat = formatter.format(distanceMeters / 1000);
                        dist = getString(R.string.distance) + " " + formatedFloat + "km";
                    }
                }catch(Exception e){
                    dist = getString(R.string.distanceNa);
                }
            }else{
                dist = getString(R.string.distanceNa);
            }

            if (geofenceStore.getGeofence(geofence.getRequestId()).isStatus()) {
                navDrawerItems.add(new NavDrawerItem(geofence.getRequestId(), R.drawable.ic_green_circle_24dp, dist));
            } else {
                navDrawerItems.add(new NavDrawerItem(geofence.getRequestId(), R.drawable.ic_red_circle_24dp, dist));
            }
        }

        // Sorting
        Collections.sort(navDrawerItems, new Comparator<NavDrawerItem>() {
            public int compare(NavDrawerItem item2, NavDrawerItem item1)
            {
                return  item2.getZone().compareToIgnoreCase(item1.getZone());
            }
        });

        // setting the nav drawer list adapter
        NavDrawerListAdapter adapter = new NavDrawerListAdapter(getApplicationContext(), navDrawerItems);
        list.setAdapter(adapter);
    }

    private void setBTfenceList2Drawer(){
        navDrawerItems.clear();
        int z = 0;
        // Gespeicherte Beacons auflisten
        for (int i = 0; i < mCurrentBeacons.size(); i++) {
            EgiGeoZoneBeacon beacon = mCurrentBeacons.get(i);
            String dist = "";
            if (locationMerk != null) {
                // Calculate distance to fence center
                try {
                    DbZoneHelper dbZoneHelper = new DbZoneHelper(this);
                    ZoneEntity zoneEntity = dbZoneHelper.getCursorZoneByName(beacon.getBeaconId());
                    Location locationZone = new Location("beaconZone");
                        if (zoneEntity.getLatitude() == null || zoneEntity.getLongitude() == null) {
                            dist = getString(R.string.distanceNa);
                        } else {
                            locationZone.setLatitude(Double.valueOf(zoneEntity.getLatitude()));
                            locationZone.setLongitude(Double.valueOf(zoneEntity.getLongitude()));
                            float distanceMeters = locationMerk.distanceTo(locationZone);
                            if (distanceMeters < 50) {
                                dist = getString(R.string.distanceLess);
                            } else if (distanceMeters < 1000) {
                                NumberFormat formatter = NumberFormat.getInstance(Locale.getDefault());
                                formatter.setMaximumFractionDigits(2);
                                formatter.setMinimumFractionDigits(2);
                                formatter.setRoundingMode(RoundingMode.HALF_UP);
                                String formatedFloat = formatter.format(distanceMeters);
                                dist = getString(R.string.distance) + " " + formatedFloat + "m";
                            } else {
                                NumberFormat formatter = NumberFormat.getInstance(Locale.getDefault());
                                formatter.setMaximumFractionDigits(2);
                                formatter.setMinimumFractionDigits(2);
                                formatter.setRoundingMode(RoundingMode.HALF_UP);
                                String formatedFloat = formatter.format(distanceMeters / 1000);
                                dist = getString(R.string.distance) + " " + formatedFloat + "km";
                            }
                        }
                } catch (Exception e) {
                    dist = getString(R.string.distanceNa);
                }
            } else {
                dist = getString(R.string.distanceNa);
            }

            if (beaconStore.getBeacon(beacon.getBeaconId()).isStatus()){
                navDrawerItems.add(new NavDrawerItem(beacon.getBeaconId(), R.drawable.ic_green_circle_24dp, dist));
            }else{
                navDrawerItems.add(new NavDrawerItem(beacon.getBeaconId(), R.drawable.ic_red_circle_24dp, dist));
            }
        }
        // Sorting
        Collections.sort(navDrawerItems, new Comparator<NavDrawerItem>() {
            public int compare(NavDrawerItem item2, NavDrawerItem item1)
            {
                return  item2.getZone().compareToIgnoreCase(item1.getZone());
            }
        });

        // setting the nav drawer list adapter
        NavDrawerListAdapter adapter = new NavDrawerListAdapter(getApplicationContext(), navDrawerItems);
        list.setAdapter(adapter);
    }

    private List<SimpleGeofence> refreshFences() {
        mCurrentBeacons.clear();
        List<SimpleBeacon> beacons = beaconStore.getBeacons();
        for (SimpleBeacon simpleBeacon : beacons)
        {
            mCurrentBeacons.add(simpleBeacon.toBeacon());
        }

        mCurrentGeofences.clear();
        List<SimpleGeofence> geofences = geofenceStore.getGeofences();
        for (SimpleGeofence simpleGeofence : geofences)
        {
            mCurrentGeofences.add(simpleGeofence.toGeofence());
        }
        return geofences;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        NavDrawerListAdapter navDrawerListAdapter = (NavDrawerListAdapter)adapterView.getAdapter();
        NavDrawerItem navDrawerItem = (NavDrawerItem)navDrawerListAdapter.getItem(i);
        String zone = navDrawerItem.getZone();

        if (((TextView) findViewById(R.id.fences)).getText().equals(getString(R.string.geoZones))) {
            // Geofences
            Intent is = new Intent(this, GeoFence.class);
            is.putExtra("action", "update");
            is.putExtra("zone", zone);
            startActivityForResult(is, 4730);
        }else{
            // BTfences
            Intent is = new Intent(this, BtFence.class);
            is.putExtra("action", "update");
            is.putExtra("beaconName", zone);
            startActivityForResult(is, 4731);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            // If the request code matches the code sent in onConnectionFailed
            case Constants.CONNECTION_FAILURE_RESOLUTION_REQUEST :
                switch (resultCode) {
                    // If Google Play services resolved the problem
                    case Activity.RESULT_OK:

                        // If the request was to add geofences
                        if (Constants.REQUEST_TYPE.ADD == mRequestType) {

                            // Restart the process of adding the current geofences
                            if (mCurrentGeofences.size() > 0) {
                                mGeofenceRequester.addGeofences(mCurrentGeofences);
                            }
                            // If the request was to remove geofences
                        } else if (Constants.REQUEST_TYPE.REMOVE == mRequestType ){

                            // Toggle the removal flag and send a new removal request
                            mGeofenceRemover.setInProgressFlag(false);

                            // If the removal was by Intent
                            if (Constants.REMOVE_TYPE.INTENT == mRemoveType) {

                                // Restart the removal of all geofences for the PendingIntent
                                mGeofenceRemover.removeGeofencesByIntent(
                                        mGeofenceRequester.getRequestPendingIntent());

                                // If the removal was by a List of geofence IDs
                            } else {

                                // Restart the removal of the geofence list
                                mGeofenceRemover.removeGeofencesById(mGeofenceIdsToRemove);
                            }
                        }
                        break;

                    // If any other result was returned by Google Play services
                    default:
                        // Report that Google Play services was unable to resolve the problem.
                        Log.d(Constants.APPTAG, getString(R.string.no_resolution));
                        log.info("onActivityResult: " + getString(R.string.no_resolution));
                }
                // Geofence hinzufügen und starte die Überwachung
            case 4730 :
                if (resultCode == RESULT_OK) {
                    String action = null;
                    if (intent != null) {
                        action = intent.getStringExtra("action");
                    }
                    if (action.equalsIgnoreCase("delete")) {
                        String zoneToDelete = intent.getStringExtra("zoneToDelete");
                        if (zoneToDelete == null || zoneToDelete.equalsIgnoreCase("")){
                            return;
                        }
                        deleteNow(zoneToDelete);
                        // Display Liste mit Zonen
                        refreshFences();
                        setGeofenceList2Drawer();
                    } else {
                        String zoneToAdd = intent.getStringExtra("zoneToAdd");
                        if (zoneToAdd == null || zoneToAdd.equalsIgnoreCase("")){
                            return;
                        }

                        // Display Liste mit Zonen
                        List<SimpleGeofence> geofences = refreshFences();
                        setGeofenceList2Drawer();
                        // Start the request. Fail if there's already a request in progress
                        try {
                            // Try to add geofences
                            mRequestType = Constants.REQUEST_TYPE.ADD;

                            if (mCurrentGeofences.size() > 0) {
                                // Old style, without trying to repair
//                                if (!Utils.isBoolean(dbGlobalsHelper.getCursorGlobalsByKey(Constants.DB_KEY_FALSE_POSITIVES))) {
                                    if (!Utils.isBoolean(dbGlobalsHelper.getCursorGlobalsByKey(Constants.DB_KEY_NEW_API))) {
                                        mGeofenceRequester.addGeofences(mCurrentGeofences);
                                    } else {
                                        for (SimpleGeofence simpleGeofence : geofences) {
                                            mPathsenseGeofence.addGeofence(simpleGeofence);
                                        }
                                    }
//                                } else {
//                                    DbZoneHelper dbZoneHelper = new DbZoneHelper(this);
//                                    ZoneEntity ze = dbZoneHelper.getCursorZoneByName(zoneToAdd);
//                                    if (!Utils.isBoolean(dbGlobalsHelper.getCursorGlobalsByKey(Constants.DB_KEY_NEW_API))) {
//                                        Geofence geof = new Geofence.Builder().setRequestId(zoneToAdd)
//                                                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
//                                                .setCircularRegion(Double.valueOf(ze.getLatitude()), Double.valueOf(ze.getLongitude()), ze.getRadius())
//                                                .setExpirationDuration(Geofence.NEVER_EXPIRE)
//                                                .build();
//
//                                        List<Geofence> currentGeofence = new ArrayList<>();
//                                        currentGeofence.add(geof);
//
//                                        mGeofenceRequester.addGeofences(currentGeofence);
//                                    } else {
//                                        SimpleGeofence simpleGeofence = new SimpleGeofence(zoneToAdd, ze.getLatitude(), ze.getLongitude(),
//                                                Integer.toString(ze.getRadius()), null, Geofence.NEVER_EXPIRE, Geofence.GEOFENCE_TRANSITION_ENTER, true, null, null);
//                                        mPathsenseGeofence.addGeofence(simpleGeofence);
//                                    }
//                                }
                            }
                        } catch (UnsupportedOperationException e) {
                            // Notify user that previous request hasn't finished.
                            Toast.makeText(this, R.string.add_geofences_already_requested_error, Toast.LENGTH_LONG).show();
                            log.error("Error registering Geofence", e);
                            showError("Error registering Geofence", e.toString());
                        }
                    }
                }
                break;
            // BT fence
            case 4731 :
                // Display Liste mit Beacons und starte die Überwachung, wenn automatisch
                if (resultCode == RESULT_OK) {
                    String action = null;
                    String mBeaconId = null;
                    if (intent != null) {
                        action = intent.getStringExtra("action");
                        mBeaconId = intent.getStringExtra("beaconBeaconId");
                    }
                    if (action.equalsIgnoreCase("delete")) {
                        String zoneToDelete = intent.getStringExtra("zoneToDelete");
                        if (zoneToDelete == null || zoneToDelete.equalsIgnoreCase("")){
                            return;
                        }
                        onDeleteBeaconNow(zoneToDelete, mBeaconId);
                        // Display Liste mit Zonen
                        refreshFences();
                        setBTfenceList2Drawer();
                    } else {
                        // action = add
                        String region = null;
                        boolean automatic = true;
                        if (intent != null) {
                            region = intent.getStringExtra("beaconRegion");
                            mBeaconId = intent.getStringExtra("beaconBeaconId");
                            automatic = intent.getBooleanExtra("beaconMode", true);
                        }
                        refreshFences();
                        setBTfenceList2Drawer();

                        List<Identifier> listB = Utils.getStringIdentifiers(mBeaconId);
                        BeaconManager mBeaconManager = BeaconManager.getInstanceForApplication(this);
                        Region singleBeaconRegion = new Region(region, listB);
                        try {
                            if (automatic) { // true = automatic
                                mBeaconManager.startRangingBeaconsInRegion(singleBeaconRegion);
                                mBeaconManager.startMonitoringBeaconsInRegion(singleBeaconRegion);
                            }
                        } catch (RemoteException ignored) {
                        }
                    }
                }
                break;
            // Settings
            case 5004 :
                if (resultCode == RESULT_OK) {
                    // Nur wenn Import war, dann durchlaufen
                    boolean imp = intent.getBooleanExtra("import", false);
                    if (imp){
                        // Drawer neu setzen
                        List<SimpleGeofence> geofences = refreshFences();
                        mRequestType = Constants.REQUEST_TYPE.ADD;
                        // Start the request. Fail if there's already a request in progress
                        try {
                            // Try to add geofences
                            if (mCurrentGeofences.size() > 0) {
                                if (!Utils.isBoolean(dbGlobalsHelper.getCursorGlobalsByKey(Constants.DB_KEY_NEW_API))) {
                                    if (!servicesConnected()) {
                                        return;
                                    }
                                    mGeofenceRequester.addGeofences(mCurrentGeofences);
                                }else {
                                    for (SimpleGeofence simpleGeofence : geofences) {
                                       mPathsenseGeofence.addGeofence(simpleGeofence);
                                    }
                                }
                            }
                        } catch (UnsupportedOperationException e) {
                            // Notify user that previous request hasn't finished.
                            Toast.makeText(this, R.string.add_geofences_already_requested_error, Toast.LENGTH_LONG).show();
                            log.error("Import: Error registering Geofence", e);
                            showError("Import: Error registering Geofence", e.toString());
                        }
                        //
                        // Settings wieder aufrufen, da Aktion Import war
                        log.debug("onOptionsItemSelected: menu_settings");
                        Intent i5 = new Intent(this, Settings.class);
                        Bundle b = new Bundle();
                        b.putBoolean("import", true);
                        i5.putExtras(b);
                        startActivityForResult(i5, 5004);
                    }
                }
                refreshFences();
                setGeofenceList2Drawer();
                break;
            // If any other request code was received
            default:
                // Report that this Activity received an unknown requestCode
                Log.d(Constants.APPTAG, getString(R.string.unknown_activity_request_code, requestCode));
                break;
        }
    }
    /**
     * Fehlerdialog anzeigen
     */
    private void showError(String title, String error){
        Log.d(TAG, error);
        NotificationUtil.showError(getApplicationContext(), title, error);
    }

    /**
     * Called when the user clicks the "Remove geofence" button #### Eine Zone löschen ####
     */
    private void deleteNow(String zone) {
        log.info("deleteNow: Remove geofence " + zone);
        // Create a List of 1 Geofence with the ID= name and store it in the global list
        mGeofenceIdsToRemove = Collections.singletonList(zone);
        /*
         * Record the removal as remove by list. If a connection error occurs,
         * the app can automatically restart the removal if Google Play services
         * can fix the error
         */
        mRemoveType = Constants.REMOVE_TYPE.LIST;
        /*
         * Check for Google Play services. Do this after
         * setting the request type. If connecting to Google Play services
         * fails, onActivityResult is eventually called, and it needs to
         * know what type of request was in progress.
         */

        // Try to remove the geofence
        try {
            if (!Utils.isBoolean(dbGlobalsHelper.getCursorGlobalsByKey(Constants.DB_KEY_NEW_API))) {
                if (!servicesConnected()) {
                    return;
                }
                mGeofenceRemover.removeGeofencesById(mGeofenceIdsToRemove);
            }else {
                mPathsenseGeofence.removeGeofence(zone);
            }
            // Catch errors with the provided geofence IDs
        } catch (IllegalArgumentException e) {
            log.error(zone + ": Error removing Geofence", e);
            showError(zone + ": Error removing Geofence", e.toString());
            return;
        } catch (UnsupportedOperationException e) {
            // Notify user that previous request hasn't finished.
            Toast.makeText(this, R.string.remove_geofences_already_requested_error, Toast.LENGTH_LONG).show();
            log.error(zone + ": Error removing Geofence", e);
            showError(zone + ": Error removing Geofence", e.toString());
            return;
        }

        // Remove zone
        DbZoneHelper dbZoneHelper = new DbZoneHelper(this);
        dbZoneHelper.deleteZone(zone);
    }
    /**
     * Called when the user clicks the "Remove beacon" button #### Ein Beacon lÃ¶schen ####
     */
    private void onDeleteBeaconNow(String region, String mBeaconId) {
        /*
         * Remove a beacon
         */
        log.info("onDeleteBeaconNow: Remove beacon " + region);

        /*
         * Beaconliste neu aufbauen
         */
        if(!mBeaconId.equals("##")) {
            List<Identifier> listB = Utils.getStringIdentifiers(mBeaconId);
            BeaconManager mBeaconManager = BeaconManager.getInstanceForApplication(this);
            Region singleBeaconRegion = new Region(region, listB);
            try {
                mBeaconManager.stopRangingBeaconsInRegion(singleBeaconRegion);
                mBeaconManager.stopMonitoringBeaconsInRegion(singleBeaconRegion);
            } catch (RemoteException ignored) {
            }
        }
        // Remove a flattened beacon object from storage
        DbZoneHelper dbZoneHelper = new DbZoneHelper(this);
        dbZoneHelper.deleteZone(region);

    }

    /**
     * Verify that Google Play services is available before making a request.
     *
     * @return true if Google Play services is available, otherwise false
     */
    private boolean servicesConnected() {
        log.debug("servicesConnected");
        // Check that Google Play services is available
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int code = api.isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == code) {
            // In debug mode, log the status
            Log.d(Constants.APPTAG, getString(R.string.play_services_available));
            log.info("servicesConnected result from Google Play Services: " + getString(R.string.play_services_available));
            // Continue
            return true;
            // Google Play services was not available for some reason
        } else if (api.isUserResolvableError(code)){
            log.error("servicesConnected result: could not connect to Google Play services");
            api.showErrorDialogFragment(this, code, Constants.PLAY_SERVICES_RESOLU‌​TION_REQUEST);
        } else {
            log.error("servicesConnected result: could not connect to Google Play services");
            Toast.makeText(this, api.getErrorString(code), Toast.LENGTH_LONG).show();
        }
        return false;
    }

    /**
     * Define a DialogFragment to display the error dialog generated in
     * showErrorDialog.
     */
    public static class ErrorDialogFragment extends DialogFragment {
        // Global field to contain the error dialog
        private Dialog mDialog;

        /**
         * Default constructor. Sets the dialog field to null
         */
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        /**
         * Set the dialog to display
         *
         * @param dialog An error dialog
         */
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        /*
         * This method must return a Dialog to the DialogFragment.
         */
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }
//    // Berechtigungen ExternalStorage
//    private void checkPermissionsExternalStorage(){
//        //noinspection StatementWithEmptyBody
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            // Check Permissions Now
//            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
//                // Display UI and wait for user interaction
//                AlertDialog.Builder alertDialogBuilder = Utils.onAlertDialogCreateSetTheme(this);
//                alertDialogBuilder.setMessage(getString(R.string.checkExtStorage));
//                alertDialogBuilder.setTitle(getString(R.string.titleExtStorage));
//
//                alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface arg0, int arg1) {
//                        ActivityCompat.requestPermissions(MainEgiGeoZone.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE);
//                    }
//                });
//                AlertDialog alertDialog = alertDialogBuilder.create();
//                alertDialog.show();
//
//            } else {
//                ActivityCompat.requestPermissions(MainEgiGeoZone.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE);
//            }
//        } else {
//            // permission has been granted, continue as usual
////            Toast.makeText(this,"permission has been granted, continue as usual",Toast.LENGTH_LONG).show();
//        }
//    }


//    public void onRequestPermissionsResult(final int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
////        http://stackoverflow.com/questions/30719047/android-m-check-runtime-permission-how-to-determine-if-the-user-checked-nev
//        String perm = "";
//        String title = "";
//        String descNeverAsk = "";
//        String descAsk = "";
////        boolean goToSettings = false;
//
//        if (requestCode == REQUEST_LOCATION) {
//            perm = Manifest.permission.ACCESS_FINE_LOCATION;
//            descAsk = getString(R.string.descAskLocation);
//            descNeverAsk = getString(R.string.descNeverAskLocation);
//            title = getString(R.string.titleLocation);
//        }
//
//        if (requestCode == REQUEST_BLUETOOTH) {
//            perm = Manifest.permission.ACCESS_FINE_LOCATION;
//            descAsk = getString(R.string.descAskLocationBT);
//            descNeverAsk = getString(R.string.descNeverAskLocationBT);
//            title = getString(R.string.titleLocation);
//        }
//
//        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE) {
//            perm = Manifest.permission.WRITE_EXTERNAL_STORAGE;
//            descAsk = getString(R.string.descAskExtStorage);
//            descNeverAsk = getString(R.string.descNeverAskExtStorage);
//            title = getString(R.string.titleExtStorage);
//        }
//
//
//        //noinspection StatementWithEmptyBody
//        if(grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//            // We can now safely use the API we requested access to
////            Toast.makeText(this,"We can now safely use the API we requested access to",Toast.LENGTH_LONG).show();
//        } else {
//            // Permission was denied or request was cancelled
//            boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0]);
//            if (!showRationale) {
////                Toast.makeText(this, "Permission was denied with flag NEVER ASK AGAIN",Toast.LENGTH_LONG).show();
//                // user denied flagging NEVER ASK AGAIN
//                // you can either enable some fall back,
//                // disable features of your app
//                // or open another dialog explaining
//                // again the permission and directing to
//                // the app setting
//                AlertDialog.Builder alertDialogBuilder = Utils.onAlertDialogCreateSetTheme(this);
//                alertDialogBuilder.setMessage(descNeverAsk);
//                alertDialogBuilder.setTitle(title);
//
////                final boolean finalGoToSettings = false;
//                alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface arg0, int arg1) {
////                        Toast.makeText(MainActivity.this,"You clicked yes button",Toast.LENGTH_LONG).show();
//                        // Benutzer hat mit nicht mehr fragen geantwortet.
//                        // Dialog mit Erklärung und zu den App-Settings leiten
////                        if (finalGoToSettings) {
////                            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
////                            Uri uri = Uri.fromParts("package", getPackageName(), null);
////                            intent.setData(uri);
////                            startActivityForResult(intent, 3);
////                        }
//                    }
//                });
//                AlertDialog alertDialog = alertDialogBuilder.create();
//                alertDialog.show();
//
//            } else{
//                // user denied WITHOUT never ask again
//                // this is a good place to explain the user
//                // why you need the permission and ask if he want
//                // to accept it (the rationale)
////                Toast.makeText(this, "Permission was denied or request was cancelled",Toast.LENGTH_LONG).show();
//                // Nochmals Dialog mit Erklärung und dann wieder anfragen
//                AlertDialog.Builder alertDialogBuilder = Utils.onAlertDialogCreateSetTheme(this);
//                alertDialogBuilder.setMessage(descAsk);
//                alertDialogBuilder.setTitle(title);
//
//                final String finalPerm = perm;
//                alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface arg0, int arg1) {
//                        ActivityCompat.requestPermissions(MainEgiGeoZone.this, new String[]{finalPerm}, requestCode);
////                        Toast.makeText(MainActivity.this,"You clicked yes button",Toast.LENGTH_LONG).show();
//                    }
//                });
//                AlertDialog alertDialog = alertDialogBuilder.create();
//                alertDialog.show();
//            }
//        }
//    }

    @Override
    public void onPermissionsGranted(int requestCode) {
        init();
    }


//    private boolean checkPermissionsLocation() {
//        //noinspection StatementWithEmptyBody
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // Check Permissions Now
//            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
//                // Display UI and wait for user interaction
//                android.support.v7.app.AlertDialog.Builder alertDialogBuilder = Utils.onAlertDialogCreateSetTheme(this);
//                alertDialogBuilder.setMessage(getString(R.string.checkLocation));
//                alertDialogBuilder.setTitle(getString(R.string.titleLocation));
//
//                alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface arg0, int arg1) {
//                        ActivityCompat.requestPermissions(MainEgiGeoZone.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MainEgiGeoZone.REQUEST_LOCATION);
//                    }
//                });
//                android.support.v7.app.AlertDialog alertDialog = alertDialogBuilder.create();
//                alertDialog.show();
//                return true;
//            } else {
//                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MainEgiGeoZone.REQUEST_LOCATION);
//            }
//        } else {
//            // permission has been granted, continue as usual
////            Toast.makeText(this, "permission has been granted, continue as usual", Toast.LENGTH_LONG).show();
//        }
//        return false;
//    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // If Google Play Services is available
        if (servicesConnected()) {
            // Get the current location
            Location currentLocation = null;
            try{
                currentLocation = LocationServices.FusedLocationApi.getLastLocation(mLocationClient);
            }catch(SecurityException se){
                // Display UI and wait for user interaction
                android.support.v7.app.AlertDialog.Builder alertDialogBuilder = Utils.onAlertDialogCreateSetTheme(this);
                alertDialogBuilder.setMessage(getString(R.string.alertPermissions));
                alertDialogBuilder.setTitle(getString(R.string.titleAlertPermissions));

                alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                    }
                });
                android.support.v7.app.AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }

            if (currentLocation != null){
                // Start test
                log.debug("onConnected - location: " + (Double.valueOf(currentLocation.getLatitude()).toString()) + "##" + (Double.valueOf(currentLocation.getLongitude()).toString()));
                locationMerk = currentLocation;
            }else{
//                Toast.makeText(this, "Could not determine location. ", Toast.LENGTH_LONG).show();
                log.error("Could not determine location.");
            }
        }

        refreshFences();
        if (typeGeoZone){
            setGeofenceList2Drawer();
        }else{
            setBTfenceList2Drawer();
        }

        mLocationClient.disconnect();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onRefresh() {
        log.debug("onRefresh called from SwipeRefreshLayout");
        if (typeGeoZone) {
            fillListGeofences();
        }else{
            fillListBTfences();
        }
        mSwipeRefreshLayout.setRefreshing(false);
    }
}





















