package de.egi.geofence.geozone.bt.fences;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.egi.geofence.geozone.bt.GlobalSingleton;
import de.egi.geofence.geozone.bt.Help;
import de.egi.geofence.geozone.bt.Karte;
import de.egi.geofence.geozone.bt.R;
import de.egi.geofence.geozone.bt.db.DbMailHelper;
import de.egi.geofence.geozone.bt.db.DbMoreHelper;
import de.egi.geofence.geozone.bt.db.DbRequirementsHelper;
import de.egi.geofence.geozone.bt.db.DbServerHelper;
import de.egi.geofence.geozone.bt.db.DbZoneHelper;
import de.egi.geofence.geozone.bt.db.MoreEntity;
import de.egi.geofence.geozone.bt.db.RequirementsEntity;
import de.egi.geofence.geozone.bt.db.ServerEntity;
import de.egi.geofence.geozone.bt.db.ZoneEntity;
import de.egi.geofence.geozone.bt.geofence.SimpleGeofence;
import de.egi.geofence.geozone.bt.geofence.SimpleGeofenceStore;
import de.egi.geofence.geozone.bt.profile.MailProfile;
import de.egi.geofence.geozone.bt.profile.MoreProfile;
import de.egi.geofence.geozone.bt.profile.RequirementsProfile;
import de.egi.geofence.geozone.bt.profile.ServerProfile;
import de.egi.geofence.geozone.bt.tracker.TrackingLocalSettings;
import de.egi.geofence.geozone.bt.utils.Constants;
import de.egi.geofence.geozone.bt.utils.Utils;
import it.sephiroth.android.library.tooltip.Tooltip;

/**
 * Created by egmontr on 28.07.2016.
 */
public class GeoFence extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener, AdapterView.OnItemSelectedListener {
    private DbZoneHelper datasource;
    private String action;
    private String zone;
    private boolean _new = true;
    private ZoneEntity ze;
    // Stores the current instantiation of the location client in this object
    private GoogleApiClient mLocationClient;
    private final Logger log = Logger.getLogger(GeoFence.class);
    private View viewMerk;

    List<String> listNone;

    Spinner spinner_server;
    Spinner spinner_mail;
    Spinner spinner_more;
    Spinner spinner_requ;

    List<String> listSrvAll;
    List<String> listMailAll;
    List<String> listMoreAll;
    List<String> listRequAll;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.onActivityCreateSetTheme(this);

        setContentView(R.layout.geofence);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Utils.changeBackGroundToolbar(this, toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
        datasource = new DbZoneHelper(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_geo);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                // Speichern
                viewMerk = view;
                saveZone();
            }
        });

        Button trackingButton = (Button) findViewById(R.id.tracking);
        trackingButton.setOnClickListener(this);

        // load profiles
        listNone = new ArrayList<>();
        listNone.add("none");

        // Set servers
        fillSpinnerServer();
        spinner_server.setOnItemSelectedListener(this);

        // Set mails
        filleSpinnerMail();

        // Set More
        fillSpinnerMore();

        // Set Requs
        fillSpinnerRequ();

        // Set Beacons
        Cursor cursorBeacons = datasource.getCursorAllZone(Constants.BEACON);
        Spinner spinner_beacons = (Spinner) findViewById(R.id.spinner_beacons);
        List<String> listBeacons = new ArrayList<>();
        while (cursorBeacons.moveToNext()) {
            listBeacons.add(cursorBeacons.getString(1));
        }
        Collections.sort(listBeacons, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return s1.compareToIgnoreCase(s2);
            }
        });
        cursorBeacons.close();

        List<String>listBeaconsAll = new ArrayList<>();
        listBeaconsAll.addAll(listNone);
        listBeaconsAll.addAll(listBeacons);

        ArrayAdapter<String> adapterBeacons = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, listBeaconsAll);
        adapterBeacons.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_beacons.setAdapter(adapterBeacons);

        ze = new ZoneEntity();

        Bundle b = getIntent().getExtras();
        if (null != b) {
            action = b.getString("action");
            zone = b.getString("zone");
        }

        if (action.equalsIgnoreCase("new")) {
            // Felder leer lassen
            _new = true;
        } else if (action.equalsIgnoreCase("update")) {
            _new = false;
        }

        // No input allowed
        ((EditText) findViewById(R.id.value_wifiInfo)).setKeyListener(null);

        if (_new) {
            // Neue Zone anlegen
            ((EditText) findViewById(R.id.value_geofence)).setText("");
            ((EditText) findViewById(R.id.value_alias)).setText("");
            ((EditText) findViewById(R.id.value_latitude)).setText("");
            ((EditText) findViewById(R.id.value_longitude)).setText("");
            ((EditText) findViewById(R.id.value_radius)).setText("");
            ((EditText) findViewById(R.id.value_wifiInfo)).setText("");
            ze.setId(0);
            // Für Tasker-Einstellungen setzen
            GlobalSingleton.getInstance().setZoneEntity(ze);
        } else {
            // Zone anzeigen --> Felder füllen
            // Zone aus der DB lesen
            ze = datasource.getCursorZoneByName(zone);

            // Instantiate a new geofence storage area
            SimpleGeofenceStore geofenceStore = new SimpleGeofenceStore(this);
            SimpleGeofence geofence = geofenceStore.getGeofence(ze.getName());

            ((EditText) findViewById(R.id.value_geofence)).setText(geofence.getId());
            ((EditText) findViewById(R.id.value_alias)).setText(geofence.getmAlias());
            ((EditText) findViewById(R.id.value_latitude)).setText(geofence.getLatitude());
            ((EditText) findViewById(R.id.value_longitude)).setText(geofence.getLongitude());
            ((EditText) findViewById(R.id.value_radius)).setText(geofence.getRadius());
            ((EditText) findViewById(R.id.value_wifiInfo)).setText(geofence.getmWifiInfo());

            // Mit den Ids die Namen der Profile lesen
            if (ze.getId_email() != null) {
                int ind_me = listMailAll.indexOf(ze.getId_email()) < 0 ? 0 : listMailAll.indexOf(ze.getId_email());
                spinner_mail.setSelection(ind_me, true);
            }
            if (ze.getId_more_actions() != null) {
                int ind_mo = listMoreAll.indexOf(ze.getId_more_actions()) < 0 ? 0 : listMoreAll.indexOf(ze.getId_more_actions());
                spinner_more.setSelection(ind_mo, true);
            }
            if (ze.getId_server() != null) {
                int ind_se = listSrvAll.indexOf(ze.getId_server()) < 0 ? 0 : listSrvAll.indexOf(ze.getId_server());
                spinner_server.setSelection(ind_se, true);
            }
            if (ze.getId_requirements() != null) {
                int ind_re = listRequAll.indexOf(ze.getId_requirements()) < 0 ? 0 : listRequAll.indexOf(ze.getId_requirements());
                spinner_requ.setSelection(ind_re, true);
            }
            if (ze.getId_beacon() != null) {
                int ind_be = listBeaconsAll.indexOf(ze.getId_beacon()) < 0 ? 0 : listBeaconsAll.indexOf(ze.getId_beacon());
                spinner_beacons.setSelection(ind_be, true);
            }
            // Für Tasker-Einstellungen setzen
            GlobalSingleton.getInstance().setZoneEntity(ze);
        }
    }

    private void fillSpinnerRequ() {
        DbRequirementsHelper datasourceRequ = new DbRequirementsHelper(this);

        Cursor cursorRequ = datasourceRequ.getCursorAllRequirements();
        spinner_requ = (Spinner) findViewById(R.id.spinner_requirements_profile);
        List<String> listRequ = new ArrayList<>();
        while (cursorRequ.moveToNext()) {
            listRequ.add(cursorRequ.getString(1));
        }
        Collections.sort(listRequ, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return s1.compareToIgnoreCase(s2);
            }
        });
        cursorRequ.close();

        listRequAll = new ArrayList<>();
        listRequAll.addAll(listNone);
        listRequAll.addAll(listRequ);

        ArrayAdapter<String> adapterRequ = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, listRequAll);
        adapterRequ.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_requ.setAdapter(adapterRequ);
    }

    private void filleSpinnerMail() {
        DbMailHelper datasourceMail = new DbMailHelper(this);

        Cursor cursorMail = datasourceMail.getCursorAllMail();
        spinner_mail = (Spinner) findViewById(R.id.spinner_mail_profile);
        List<String> listMail = new ArrayList<>();
        while (cursorMail.moveToNext()) {
            listMail.add(cursorMail.getString(1));
        }
        Collections.sort(listMail, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return s1.compareToIgnoreCase(s2);
            }
        });
        cursorMail.close();

        listMailAll = new ArrayList<>();
        listMailAll.addAll(listNone);
        listMailAll.addAll(listMail);

        ArrayAdapter<String> adapterMail = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, listMailAll);
        adapterMail.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_mail.setAdapter(adapterMail);
    }


    private void fillSpinnerServer() {
        DbServerHelper datasourceServer = new DbServerHelper(this);

        Cursor cursorSrv = datasourceServer.getCursorAllServer();
        List<String> listSrv = new ArrayList<>();
        while (cursorSrv.moveToNext()) {
            listSrv.add(cursorSrv.getString(1));
        }
        Collections.sort(listSrv, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return s1.compareToIgnoreCase(s2);
            }
        });
        cursorSrv.close();

        listSrvAll = new ArrayList<>();
        listSrvAll.addAll(listNone);
        listSrvAll.addAll(listSrv);

        ArrayAdapter<String> adapterServer = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, listSrvAll);
        spinner_server = (Spinner) findViewById(R.id.spinner_server_profile);

        adapterServer.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_server.setAdapter(adapterServer);
    }

    private void fillSpinnerMore() {
        DbMoreHelper datasourceMore = new DbMoreHelper(this);

        Cursor cursorMore = datasourceMore.getCursorAllMore();
        spinner_more = (Spinner) findViewById(R.id.spinner_more_profile);
        List<String> listMore = new ArrayList<>();
        while (cursorMore.moveToNext()) {
            listMore.add(cursorMore.getString(1));
        }
        Collections.sort(listMore, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return s1.compareToIgnoreCase(s2);
            }
        });
        cursorMore.close();

        listMoreAll = new ArrayList<>();
        listMoreAll.addAll(listNone);
        listMoreAll.addAll(listMore);

        ArrayAdapter<String> adapterMore = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, listMoreAll);
        adapterMore.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_more.setAdapter(adapterMore);
    }

    private void saveZone() {
        if (checkInputFields()) {
            return;
        }

        EditText mZone = (EditText) findViewById(R.id.value_geofence);
        EditText mLatitude = (EditText) findViewById(R.id.value_latitude);
        EditText mLongitude = (EditText) findViewById(R.id.value_longitude);
        EditText mRadius = (EditText) findViewById(R.id.value_radius);
        EditText mAlias = (EditText) findViewById(R.id.value_alias);
        EditText mWifiInfo = (EditText) findViewById(R.id.value_wifiInfo);

        Spinner mSpinner_server = (Spinner) findViewById(R.id.spinner_server_profile);
        Spinner mSpinner_mail = (Spinner) findViewById(R.id.spinner_mail_profile);
        Spinner mSpinner_more = (Spinner) findViewById(R.id.spinner_more_profile);
        Spinner mSpinner_requ = (Spinner) findViewById(R.id.spinner_requirements_profile);
        Spinner mSpinner_beacon = (Spinner) findViewById(R.id.spinner_beacons);

        String zone = mZone.getText().toString();

        ze.setLatitude(mLatitude.getText().toString());
        ze.setLongitude(mLongitude.getText().toString());
        ze.setName(zone);
        ze.setRadius(Integer.valueOf(mRadius.getText().toString()));
        ze.setType(Constants.GEOZONE);
        ze.setAccuracy(GlobalSingleton.getInstance().getZoneEntity().getAccuracy());
        ze.setAlias(mAlias.getText().toString());
        ze.setWifi_info(mWifiInfo.getText().toString());

        String mailProfile = (String) mSpinner_mail.getSelectedItem();
        String moreProfile = (String) mSpinner_more.getSelectedItem();
        String requProfile = (String) mSpinner_requ.getSelectedItem();
        String serverProfile = (String) mSpinner_server.getSelectedItem();
        String beaconZone = (String) mSpinner_beacon.getSelectedItem();

        ze.setId_email(mailProfile.equals("none") ? null : mailProfile);
        ze.setId_more_actions(moreProfile.equals("none") ? null : moreProfile);
        ze.setId_requirements(requProfile.equals("none") ? null : requProfile);
        ze.setId_server(serverProfile.equals("none") ? null : serverProfile);
        ze.setId_beacon(beaconZone.equals("none") ? null : beaconZone);

        // Set tracker settings
        ze.setTrack_id_email(GlobalSingleton.getInstance().getZoneEntity().getTrack_id_email());
        ze.setTrack_to_file(GlobalSingleton.getInstance().getZoneEntity().isTrack_to_file());
        ze.setTrack_url(GlobalSingleton.getInstance().getZoneEntity().getTrack_url());
        ze.setEnter_tracker(GlobalSingleton.getInstance().getZoneEntity().isEnter_tracker());
        ze.setExit_tracker(GlobalSingleton.getInstance().getZoneEntity().isExit_tracker());
        ze.setLocal_tracking_interval(GlobalSingleton.getInstance().getZoneEntity().getLocal_tracking_interval());

        datasource.storeZone(ze);

        Intent data = new Intent();
        data.putExtra("action","add");
        data.putExtra("zoneToAdd", zone);
        setResult(RESULT_OK, data);
        finish();
    }

    /**
     * Check all the input values and flag those that are incorrect
     *
     * @return true if all the widget values are correct; otherwise false
     */
    private boolean checkInputFields() {
        // Start with the input validity flag set to true
        if (TextUtils.isEmpty(((EditText) findViewById(R.id.value_geofence)).getText())) {
            findViewById(R.id.value_geofence).setBackgroundColor(Color.RED);
            findViewById(R.id.value_geofence).requestFocus();
            Snackbar.make(viewMerk, R.string.geofence_input_error_missing, Snackbar.LENGTH_LONG).show();
            // Set the validity to "invalid" (false)
            return true;
        } else if (((EditText) findViewById(R.id.value_geofence)).getText().toString().contains(",")){
            findViewById(R.id.value_geofence).setBackgroundColor(Color.RED);
            findViewById(R.id.value_geofence).requestFocus();
            Snackbar.make(viewMerk, R.string.geofence_input_error_comma, Snackbar.LENGTH_LONG).show();
            // Set the validity to "invalid" (false)
            return true;
        } else if (((EditText) findViewById(R.id.value_geofence)).getText().toString().contains("'")){
            findViewById(R.id.value_geofence).setBackgroundColor(Color.RED);
            findViewById(R.id.value_geofence).requestFocus();
            Snackbar.make(viewMerk, R.string.geofence_input_error_comma, Snackbar.LENGTH_LONG).show();
            // Set the validity to "invalid" (false)
            return true;
        }

        if (((EditText) findViewById(R.id.value_alias)).getText().toString().contains(",")){
            findViewById(R.id.value_alias).setBackgroundColor(Color.RED);
            findViewById(R.id.value_alias).requestFocus();
            Snackbar.make(viewMerk, R.string.geofence_input_error_comma, Snackbar.LENGTH_LONG).show();
            // Set the validity to "invalid" (false)
            return true;
        } else if (((EditText) findViewById(R.id.value_alias)).getText().toString().contains("'")){
            findViewById(R.id.value_alias).setBackgroundColor(Color.RED);
            findViewById(R.id.value_alias).requestFocus();
            Snackbar.make(viewMerk, R.string.geofence_input_error_comma, Snackbar.LENGTH_LONG).show();
            // Set the validity to "invalid" (false)
            return true;
        }

        /*
         * Latitude, longitude, and radius values can't be empty. If they are, highlight the input
         * field in red and put a Toast message in the UI. Otherwise set the input field highlight
         * to black, ensuring that a field that was formerly wrong is reset.
         */
        if (TextUtils.isEmpty(((EditText) findViewById(R.id.value_latitude)).getText())) {
            findViewById(R.id.value_latitude).setBackgroundColor(Color.RED);
            findViewById(R.id.value_latitude).requestFocus();
            Snackbar.make(viewMerk, R.string.geofence_input_error_missing, Snackbar.LENGTH_LONG).show();
            // Set the validity to "invalid" (false)
            return true;
        }

        if (TextUtils.isEmpty(((EditText) findViewById(R.id.value_longitude)).getText())) {
            findViewById(R.id.value_longitude).setBackgroundColor(Color.RED);
            findViewById(R.id.value_longitude).requestFocus();
            Snackbar.make(viewMerk, R.string.geofence_input_error_missing, Snackbar.LENGTH_LONG).show();
            // Set the validity to "invalid" (false)
            return true;
        }
        if (TextUtils.isEmpty(((EditText) findViewById(R.id.value_radius)).getText())) {
            findViewById(R.id.value_radius).setBackgroundColor(Color.RED);
            findViewById(R.id.value_radius).requestFocus();
            Snackbar.make(viewMerk, R.string.geofence_input_error_missing, Snackbar.LENGTH_LONG).show();
            // Set the validity to "invalid" (false)
            return true;
        }
        try {
        if (Integer.valueOf(((EditText) findViewById(R.id.value_radius)).getText().toString()) < 50) {
            findViewById(R.id.value_radius).setBackgroundColor(Color.RED);
            findViewById(R.id.value_radius).requestFocus();
            Snackbar.make(viewMerk, R.string.geofence_input_error_radius_invalid, Snackbar.LENGTH_LONG).show();
            return true;
        }
        }catch (NumberFormatException ne){
            findViewById(R.id.value_radius).setBackgroundColor(Color.RED);
            findViewById(R.id.value_radius).requestFocus();
            Snackbar.make(viewMerk, R.string.geofence_input_error_radius_invalid, Snackbar.LENGTH_LONG).show();
            return true;
        }
        /*
         * If all the input fields have been entered, test to ensure that their values are within
         * the acceptable range. The tests can't be performed until it's confirmed that there are
         * actual values in the fields.
         */

            /*
             * Get values from the latitude, longitude, and radius fields.
             */
        double lat1 = Double.valueOf(((EditText) findViewById(R.id.value_latitude)).getText().toString());
        double lng1 = Double.valueOf(((EditText) findViewById(R.id.value_longitude)).getText().toString());
//        float rd1 = Float.valueOf(((EditText) findViewById(R.id.value_radius)).getText().toString());

            /*
             * Test latitude and longitude for minimum and maximum values. Highlight incorrect
             * values and set a Toast in the UI.
             */

        if (lat1 > Constants.MAX_LATITUDE || lat1 < Constants.MIN_LATITUDE) {
            getSupportActionBar().setTitle(R.string.geofence_input_error_latitude_invalid);
            findViewById(R.id.value_latitude).setBackgroundColor(Color.RED);
            findViewById(R.id.value_latitude).requestFocus();
            Snackbar.make(viewMerk, R.string.geofence_input_error_latitude_invalid, Snackbar.LENGTH_LONG).show();
            // Set the validity to "invalid" (false)
            return true;
        }

        if ((lng1 > Constants.MAX_LONGITUDE) || (lng1 < Constants.MIN_LONGITUDE)) {
            getSupportActionBar().setTitle(R.string.geofence_input_error_longitude_invalid);
            findViewById(R.id.value_longitude).setBackgroundColor(Color.RED);
            findViewById(R.id.value_longitude).requestFocus();
            Snackbar.make(viewMerk, R.string.geofence_input_error_longitude_invalid, Snackbar.LENGTH_LONG).show();
            // Set the validity to "invalid" (false)
            return true;
        }

        // If everything passes, the validity flag will still be true, otherwise it will be false.
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_geofence, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action buttons
        switch (item.getItemId()) {
            case R.id.menu_delete_geofence:

                android.support.v7.app.AlertDialog.Builder ab = Utils.onAlertDialogCreateSetTheme(this);
                ab.setMessage(R.string.action_delete).setPositiveButton(R.string.action_yes, dialogClickListener).setNegativeButton(R.string.action_no, dialogClickListener).show();
                return true;
            case R.id.menu_item_clear_geofence:
//                log.debug("onOptionsItemSelected: menu_item_clear_geofence");
                ((EditText) findViewById(R.id.value_geofence)).setText(Constants.EMPTY_STRING);
                ((EditText) findViewById(R.id.value_latitude)).setText(Constants.EMPTY_STRING);
                ((EditText) findViewById(R.id.value_longitude)).setText(Constants.EMPTY_STRING);
                ((EditText) findViewById(R.id.value_radius)).setText(Constants.EMPTY_STRING);
                ((EditText) findViewById(R.id.value_alias)).setText(Constants.EMPTY_STRING);
                ((EditText) findViewById(R.id.value_wifiInfo)).setText(Constants.EMPTY_STRING);
                ((Spinner) findViewById(R.id.spinner_server_profile)).setSelection(0, true);
                ((Spinner) findViewById(R.id.spinner_mail_profile)).setSelection(0, true);
                ((Spinner) findViewById(R.id.spinner_more_profile)).setSelection(0, true);
                ((Spinner) findViewById(R.id.spinner_requirements_profile)).setSelection(0, true);
                ((Spinner) findViewById(R.id.spinner_beacons)).setSelection(0, true);
                return true;
            case R.id.menu_get_location:
                log.debug("onOptionsItemSelected: menu_get_location");
                mLocationClient = new GoogleApiClient.Builder(this, this, this).addApi(LocationServices.API).build();
                mLocationClient.connect();
                return true;
            case R.id.menu_accuracy:
                Intent iAccuracy = new Intent(this, Accuracy.class);
                startActivityForResult(iAccuracy, 4713);
                return true;
            case R.id.menu_item_help:
//                log.debug("onOptionsItemSelected: menu_help");
                Intent i2 = new Intent(this, Help.class);
                startActivity(i2);
                return true;
            // Pass through any other request
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private final DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    //Do your Yes progress
                    Intent data = new Intent();
                    data.putExtra("action","delete");
                    String zoneToDelete = ((EditText) findViewById(R.id.value_geofence)).getText().toString();
                    data.putExtra("zoneToDelete", zoneToDelete);
                    setResult(RESULT_OK, data);
                    finish();
                case DialogInterface.BUTTON_NEGATIVE:
                    //Do your No progress
                    break;
            }
        }
    };


    /**
     * Map anzeigen
     */
    @SuppressWarnings("UnusedParameters")
    public void onKarteClicked(View view) {
        log.debug("onKarteClicked");
        Intent iKarte = new Intent(this, Karte.class);
        Bundle b = new Bundle();

        b.putString("de.egi.geofence.geozone.bt.lat", ((EditText) findViewById(R.id.value_latitude)).getText().toString());
        b.putString("de.egi.geofence.geozone.bt.lng", ((EditText) findViewById(R.id.value_longitude)).getText().toString());
        b.putString("de.egi.geofence.geozone.bt.rad", ((EditText) findViewById(R.id.value_radius)).getText().toString());
        if (findViewById(R.id.value_geofence) != null) {
            b.putString("de.egi.geofence.geozone.bt.zone", ((EditText) findViewById(R.id.value_geofence)).getText().toString());
        } else if (findViewById(R.id.value_beacon) != null) {
            b.putString("de.egi.geofence.geozone.bt.zone", ((EditText) findViewById(R.id.value_beacon)).getText().toString());
        }

        iKarte.putExtras(b);
        startActivityForResult(iKarte, 4711);
    }

    /**
     * Add Server
     */
    @SuppressWarnings("UnusedParameters")
    public void onAddServerClicked(View view) {
        log.debug("onAddServerClicked");
        Intent i = new Intent(this, ServerProfile.class);
        i.putExtra("action", "new");
        startActivityForResult(i, 4811);
    }

    /**
     * Add Mail
     */
    @SuppressWarnings("UnusedParameters")
    public void onAddMailClicked(View view) {
        log.debug("onAddMailClicked");
        Intent i = new Intent(this, MailProfile.class);
        i.putExtra("action", "new");
        startActivityForResult(i, 4813);
    }

    /**
     * Add More
     */
    @SuppressWarnings("UnusedParameters")
    public void onAddMoreClicked(View view) {
        log.debug("onAddMoreClicked");
        Intent i = new Intent(this, MoreProfile.class);
        i.putExtra("action", "new");
        startActivityForResult(i, 4814);
    }

    /**
     * Add Requs
     */
    @SuppressWarnings("UnusedParameters")
    public void onAddRequClicked(View view) {
        log.debug("onAddRequClicked");
        Intent i = new Intent(this, RequirementsProfile.class);
        i.putExtra("action", "new");
        startActivityForResult(i, 4815);
    }

    /**
     * Wifi access points anzeigen anzeigen
     */
    @SuppressWarnings("UnusedParameters")
    public void onWifiScanClicked(View view) {
        log.debug("onWifiScanClicked");
        Intent intent = new Intent(this, WifiScanAll.class);
        startActivityForResult(intent, 4715);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            // Wifi
            case 4715:
                if (resultCode == RESULT_OK) {
                    String wifi = intent.getStringExtra("wifi_info");
                    // Display the Wifi/MAC
                    ((EditText) findViewById(R.id.value_wifiInfo)).setText(wifi);
                }
                break;
            // Karte
            case 4711:
                if (resultCode == RESULT_OK) {
                    Double lat = intent.getDoubleExtra("lat", 0);
                    Double lng = intent.getDoubleExtra("lng", 0);
                    @SuppressWarnings("unused")
                    Double radius = intent.getDoubleExtra("radius", 500);
                    Long radi = radius.longValue();
                    // Display the current location in the UI
                    ((EditText) findViewById(R.id.value_latitude)).setText(lat.toString());
                    ((EditText) findViewById(R.id.value_longitude)).setText(lng.toString());
                    ((EditText) findViewById(R.id.value_radius)).setText(radi.toString());
//                    myCloseDrawer = true;
                }
                break;
            // tracking settings
            case 4712 :
                View view;
                String toolTip;
                if (GlobalSingleton.getInstance().getZoneEntity().getId() == 0){
                    // Register
                    view  = findViewById(R.id.fab_geo);
                    toolTip = this.getString(R.string.toolTipRegisterZone);
                }else{
                    // Alter
                    view  = findViewById(R.id.fab_geo);
                    toolTip = this.getString(R.string.toolTipAlterZone);
                }
                Tooltip.make(this,
                        new Tooltip.Builder(101)
                                .anchor(view, Tooltip.Gravity.BOTTOM)
                                .closePolicy(new Tooltip.ClosePolicy().insidePolicy(true, false).outsidePolicy(true, false), 5000)
                                .activateDelay(800)
                                .showDelay(300)
                                .text(toolTip)
                                .maxWidth(500)
                                .withArrow(true)
                                .withOverlay(false)
//                                .typeface(mYourCustomFont)
                                .floatingAnimation(Tooltip.AnimationBuilder.SLOW)
                                .fitToScreen(true)
                                .build()
                ).show();
                break;
            // Accuracy
            case 4713 :
                View view_acc;
                String toolTip_acc;
                if (GlobalSingleton.getInstance().getZoneEntity().getId() == 0){
                    // Register
                    view_acc  = findViewById(R.id.fab_geo);
                    toolTip_acc = this.getString(R.string.toolTipRegisterZone);
                }else{
                    // Alter
                    view_acc  = findViewById(R.id.fab_geo);
                    toolTip_acc = this.getString(R.string.toolTipAlterZone);
                }
                Tooltip.make(this,
                        new Tooltip.Builder(101)
                                .anchor(view_acc, Tooltip.Gravity.BOTTOM)
                                .closePolicy(new Tooltip.ClosePolicy().insidePolicy(true, false).outsidePolicy(true, false), 5000)
                                .activateDelay(800)
                                .showDelay(300)
                                .text(toolTip_acc)
                                .maxWidth(500)
                                .withArrow(true)
                                .withOverlay(false)
//                                .typeface(mYourCustomFont)
                                .floatingAnimation(Tooltip.AnimationBuilder.SLOW)
                                .fitToScreen(true)
                                .build()
                ).show();
                break;
            // Add Server
            case 4811:
                fillSpinnerServer();

                break;
            case 4812:
                break;
            // Add Mail
            case 4813:
                filleSpinnerMail();
                break;
            // Add More
            case 4814:
                MoreEntity me = GlobalSingleton.getInstance().getMoreEntity();
                DbMoreHelper dbMoreHelper = new DbMoreHelper(this);
                if (me.getName() != null && !me.getName().equalsIgnoreCase("")){
                    dbMoreHelper.storeMore(me);
                }

                fillSpinnerMore();
                break;
            // Add requs
            case 4815:
                RequirementsEntity rq = GlobalSingleton.getInstance().getRequirementsEntity();
                DbRequirementsHelper dbRequHelper = new DbRequirementsHelper(this);
                if (rq.getName() != null && !rq.getName().equalsIgnoreCase("")){
                    dbRequHelper.storeRequirements(rq);
                }

                fillSpinnerRequ();
                break;
            // If any other request code was received
            default:
                // Report that this Activity received an unknown requestCode
//                Log.d(Constants.APPTAG, getString(R.string.unknown_activity_request_code, requestCode));
                break;
        }
    }

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
                AlertDialog.Builder alertDialogBuilder = Utils.onAlertDialogCreateSetTheme(this);
                alertDialogBuilder.setMessage(getString(R.string.alertPermissions));
                alertDialogBuilder.setTitle(getString(R.string.titleAlertPermissions));

                alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                    }
                });
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }

            if (currentLocation != null){
                // Display the current location in the UI
                ((EditText) findViewById(R.id.value_latitude)).setText(Double.valueOf(currentLocation.getLatitude()).toString());
                ((EditText) findViewById(R.id.value_longitude)).setText(Double.valueOf(currentLocation.getLongitude()).toString());
//                    log.debug("onConnected - location: " + (Double.valueOf(currentLocation.getLatitude()).toString()) + "##" + (Double.valueOf(currentLocation.getLongitude()).toString()));
            }else{
                Toast.makeText(this, "Could not determine location. ", Toast.LENGTH_LONG).show();
//                    log.error("Could not determine location.");
            }
        }
        mLocationClient.disconnect();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

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

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tracking:
//                log.debug("onOptionsItemSelected: button_tracking");
                onLocationTrackerLokalSettingsClicked();
                break;
            default:
                break;
        }
    }

    /**
     * Weiter lokale Tracking Einstellungen
     */
    private void onLocationTrackerLokalSettingsClicked() {
        Intent iLocationTracker = new Intent(this, TrackingLocalSettings.class);
        Bundle b = new Bundle();
        if (findViewById(R.id.value_geofence) != null) {
            b.putString("de.egi.geofence.geozone.bt.tracking.zone", ((EditText) findViewById(R.id.value_geofence)).getText().toString());
        }else if (findViewById(R.id.value_beacon) != null) {
            b.putString("de.egi.geofence.geozone.bt.tracking.zone", ((EditText) findViewById(R.id.value_beacon)).getText().toString());
        }
        iLocationTracker.putExtras(b);
        startActivityForResult(iLocationTracker, 4712);
    }


    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        String serverProfile = (String)spinner_server.getSelectedItem();
        // Show Tooltip, if track url is set @@
        View view_track;
        String toolTip_track;
        DbServerHelper dbServerHelper = new DbServerHelper(this);
        ServerEntity serverEntity = dbServerHelper.getCursorServerByName(serverProfile);

        if (serverEntity != null && serverEntity.getUrl_tracking() != null && !serverEntity.getUrl_tracking().equals("")){
            view_track  = findViewById(R.id.tracking);
            toolTip_track = this.getString(R.string.toolTipTrackingSettings);
            Tooltip.make(this,
                    new Tooltip.Builder(101)
                            .anchor(view_track, Tooltip.Gravity.BOTTOM)
                            .closePolicy(new Tooltip.ClosePolicy().insidePolicy(true, false).outsidePolicy(true, false), 10000)
                            .activateDelay(800)
                            .showDelay(300)
                            .text(toolTip_track)
                            .maxWidth(500)
                            .withArrow(true)
                            .withOverlay(false)
                            //                      .typeface(mYourCustomFont)
                            .floatingAnimation(Tooltip.AnimationBuilder.SLOW)
                            .fitToScreen(true)
                            .build()
            ).show();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}