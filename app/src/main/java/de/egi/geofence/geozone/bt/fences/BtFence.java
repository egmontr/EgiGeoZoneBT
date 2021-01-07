package de.egi.geofence.geozone.bt.fences;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.Identifier;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.egi.geofence.geozone.bt.GlobalSingleton;
import de.egi.geofence.geozone.bt.Help;
import de.egi.geofence.geozone.bt.R;
import de.egi.geofence.geozone.bt.beacon.BeaconScanAll;
import de.egi.geofence.geozone.bt.beacon.SimpleBeacon;
import de.egi.geofence.geozone.bt.beacon.SimpleBeaconStore;
import de.egi.geofence.geozone.bt.db.DbMailHelper;
import de.egi.geofence.geozone.bt.db.DbMoreHelper;
import de.egi.geofence.geozone.bt.db.DbRequirementsHelper;
import de.egi.geofence.geozone.bt.db.DbServerHelper;
import de.egi.geofence.geozone.bt.db.DbZoneHelper;
import de.egi.geofence.geozone.bt.db.MoreEntity;
import de.egi.geofence.geozone.bt.db.RequirementsEntity;
import de.egi.geofence.geozone.bt.db.ZoneEntity;
import de.egi.geofence.geozone.bt.profile.MailProfile;
import de.egi.geofence.geozone.bt.profile.MoreProfile;
import de.egi.geofence.geozone.bt.profile.RequirementsProfile;
import de.egi.geofence.geozone.bt.profile.ServerProfile;
import de.egi.geofence.geozone.bt.profile.SmsProfile;
import de.egi.geofence.geozone.bt.utils.Constants;
import de.egi.geofence.geozone.bt.utils.Utils;

/**
 * Created by egmontr on 28.07.2016.
 */
public class BtFence  extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {
    private DbZoneHelper datasource;
    private String action;
    private String beaconName;
    private boolean _new = true;
    private ZoneEntity ze;
    private final Logger log = Logger.getLogger(BtFence.class);
    private View viewMerk;
    private ToggleButton toggleMode;

    // Stores the current instantiation of the location client in this object
    private GoogleApiClient mLocationClient;

    List<String> listNone;

    Spinner spinner_server;
    Spinner spinner_sms;
    Spinner spinner_mail;
    Spinner spinner_more;
    Spinner spinner_requ;

    List<String> listSrvAll;
    List<String> listSmsAll;
    List<String> listMailAll;
    List<String> listMoreAll;
    List<String> listRequAll;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.onActivityCreateSetTheme(this);
        setContentView(de.egi.geofence.geozone.bt.R.layout.btfence);

        Toolbar toolbar = (Toolbar) findViewById(de.egi.geofence.geozone.bt.R.id.toolbar);
        setSupportActionBar(toolbar);
        Utils.changeBackGroundToolbar(this, toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
        datasource = new DbZoneHelper(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(de.egi.geofence.geozone.bt.R.id.fab_bt);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                // Speichern
                viewMerk = view;
                saveZone();
            }
        });

        toggleMode = (ToggleButton) this.findViewById(R.id.toggleMode);
        Button scanButton = (Button) findViewById(de.egi.geofence.geozone.bt.R.id.scan);
        Button locationButton = (Button) findViewById(de.egi.geofence.geozone.bt.R.id.getLocation);
        scanButton.setOnClickListener(this);
        locationButton.setOnClickListener(this);

        // Prüfen, ob BLE verfügbar
        if(Build.VERSION.SDK_INT < Constants.BEACON_MIN_BUILD) {
            AlertDialog alertDialog = Utils.onAlertDialogCreateSetTheme(this).create();
            alertDialog.setTitle("BLE Alert");
            alertDialog.setMessage("Bluetooth Low Energy not supported prior to Android 4.3 (API Level " + Constants.BEACON_MIN_BUILD + ")");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        }

        // Ab hier Sachen für BeaconZone erledigen
        // load profiles
        listNone = new ArrayList<>();
        listNone.add("none");

        // Set servers
        fillSpinnerServer();

        // Set SMSs
//        fillSpinnerSMS();

        // Set mails
        filleSpinnerMail();

        // Set More
        fillSpinnerMore();

        // Set Requs
        fillSpinnerRequ();

        ze = new ZoneEntity();

        Bundle b = getIntent().getExtras();
        if (null != b) {
            action = b.getString("action");
            beaconName = b.getString("beaconName");
        }

        if (action.equalsIgnoreCase("new")) {
            // Felder leer lassen
            _new = true;
        } else if (action.equalsIgnoreCase("update")) {
            _new = false;
        }

        if (_new) {
            // Neuen Beacon anlegen
            ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon)).setText("");
            ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon_ident1)).setText("");
            ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon_ident2)).setText("");
            ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon_ident3)).setText("");
            ((EditText) findViewById(R.id.value_mac)).setText("");
            ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_radius)).setText("1");
            ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_latitude)).setText("");
            ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_longitude)).setText("");
            ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_alias)).setText("");
            ((ToggleButton) findViewById(R.id.toggleMode)).setChecked(true);

            ze.setId(0);

            // Für Tasker-Einstellungen setzen
            GlobalSingleton.getInstance().setZoneEntity(ze);

        } else {
            // Beacon anzeigen --> Felder füllen
            // Zone aus der DB lesen
            ze = datasource.getCursorZoneByName(beaconName);

            // Instantiate a new geofence storage area
            SimpleBeaconStore beaconStore = new SimpleBeaconStore(this);
            SimpleBeacon beacon = beaconStore.getBeacon(beaconName);

            ((ToggleButton) findViewById(R.id.toggleMode)).setChecked(beacon.isAutomatic());

            ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon)).setText(beacon.getId());
            List<Identifier> listB = Utils.getStringIdentifiers(beacon.getBeaconUuid());
            switch (listB.size()) {
                case 1:
                    ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon_ident1)).setText(listB.get(0).toString());
                    ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon_ident2)).setText("");
                    ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon_ident3)).setText("");
                    break;
                case 2:
                    ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon_ident1)).setText(listB.get(0).toString());
                    ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon_ident2)).setText(listB.get(1).toString());
                    ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon_ident3)).setText("");
                    break;
                case 3:
                    ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon_ident1)).setText(listB.get(0).toString());
                    ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon_ident2)).setText(listB.get(1).toString());
                    ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon_ident3)).setText(listB.get(2).toString());
                    break;
            }
            ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_mac)).setText(Utils.getStringMacAddress(beacon.getBeaconUuid()));

            ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_radius)).setText(beacon.getRadius());
            ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_latitude)).setText(beacon.getLatitude());
            ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_longitude)).setText(beacon.getLongitude());
            ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_alias)).setText(beacon.getAlias());

            // Beacon aus der DB lesen
            datasource = new DbZoneHelper(this);
            ze = datasource.getCursorZoneByName(beacon.getId());

            // Mit den Ids die Namen der Profile lesen
            if (ze.getId_email() != null) {
                int ind_me = listMailAll.indexOf(ze.getId_email()) < 0 ? 0 : listMailAll.indexOf(ze.getId_email());
                spinner_mail.setSelection(ind_me, true);
            }
            if (ze.getId_more_actions() != null) {
                int ind_mo = listMoreAll.indexOf(ze.getId_more_actions()) < 0 ? 0 : listMoreAll.indexOf(ze.getId_more_actions());
                spinner_more.setSelection(ind_mo, true);
            }
            if (ze.getId_sms() != null) {
                int ind_sm = listSmsAll.indexOf(ze.getId_sms()) < 0 ? 0 : listSmsAll.indexOf(ze.getId_sms());
                spinner_sms.setSelection(ind_sm, true);
            }
            if (ze.getId_server() != null) {
                int ind_se = listSrvAll.indexOf(ze.getId_server()) < 0 ? 0 : listSrvAll.indexOf(ze.getId_server());
                spinner_server.setSelection(ind_se, true);
            }
            if (ze.getId_requirements() != null) {
                int ind_re = listRequAll.indexOf(ze.getId_requirements()) < 0 ? 0 : listRequAll.indexOf(ze.getId_requirements());
                spinner_requ.setSelection(ind_re, true);
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

//    private void fillSpinnerSMS() {
//        DbSmsHelper datasourceSms = new DbSmsHelper(this);
//
//        Cursor cursorSms = datasourceSms.getCursorAllSms();
//        spinner_sms = (Spinner) findViewById(R.id.spinner_sms_profile);
//        List<String> listSms = new ArrayList<>();
//        while (cursorSms.moveToNext()) {
//            listSms.add(cursorSms.getString(1));
//        }
//        Collections.sort(listSms, new Comparator<String>() {
//            @Override
//            public int compare(String s1, String s2) {
//                return s1.compareToIgnoreCase(s2);
//            }
//        });
//        cursorSms.close();
//
//        listSmsAll = new ArrayList<>();
//        listSmsAll.addAll(listNone);
//        listSmsAll.addAll(listSms);
//
//        ArrayAdapter<String> adapterSms = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, listSmsAll);
//        adapterSms.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spinner_sms.setAdapter(adapterSms);
//    }

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
//        if (checkInputFields()) {
//            return;
//        }

        final boolean[] isBuildVersion18 = {true};

        // Prüfen, ob BLE verfügbar
        if(Build.VERSION.SDK_INT < Constants.BEACON_MIN_BUILD) {
            isBuildVersion18[0] = false;
            AlertDialog alertDialog = Utils.onAlertDialogCreateSetTheme(this).create();
            alertDialog.setTitle("BLE Alert");
            alertDialog.setMessage("Bluetooth Low Energy not supported prior to Android 4.3 (API Level " + Constants.BEACON_MIN_BUILD + ")");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        }

        if(!isBuildVersion18[0]){
            return;
        }

        EditText mRegion = (EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon);
        ToggleButton mMode = (ToggleButton) findViewById(R.id.toggleMode);

        String mBeaconId = ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon_ident1)).getText().toString() + "#" +
                ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon_ident2)).getText().toString() + "#" +
                ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon_ident3)).getText().toString() + "#" +
                ((EditText) findViewById(R.id.value_mac)).getText().toString();
        EditText mRadius = (EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_radius);
        EditText mLatitude = (EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_latitude);
        EditText mLongitude = (EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_longitude);
        EditText mAlias = (EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_alias);

        Spinner mSpinner_server = (Spinner)findViewById(de.egi.geofence.geozone.bt.R.id.spinner_server_profile);
//        Spinner mSpinner_sms = (Spinner)findViewById(de.egi.geofence.geozone.bt.R.id.spinner_sms_profile);
        Spinner mSpinner_mail = (Spinner)findViewById(de.egi.geofence.geozone.bt.R.id.spinner_mail_profile);
        Spinner mSpinner_more = (Spinner)findViewById(de.egi.geofence.geozone.bt.R.id.spinner_more_profile);
        Spinner mSpinner_requ = (Spinner)findViewById(de.egi.geofence.geozone.bt.R.id.spinner_requirements_profile);

        String region = mRegion.getText().toString();
//        log.info("onNewBeaconClicked: Monitor beacon " + region);

        /*
         * Check that the input fields have values and that the values are with the
         * permitted range
         */
        if (checkInputFieldsBeacon()) {
            return;
        }

        /*
         * Create a version of beacon that is "flattened" into individual fields. This
         * allows it to be stored in database.
         */
//        SimpleBeacon mUIBeacon = new SimpleBeacon(
//                region,
//                mRadius.getText().toString(),
//                mBeaconId, mLatitude.getText().toString(), mLongitude.getText().toString(), mAlias.getText().toString());

        ze.setAutomatic(mMode.isChecked());
        ze.setName(region);
        ze.setRadius(Integer.valueOf(mRadius.getText().toString()));
        ze.setType(Constants.BEACON);
        ze.setBeacon(mBeaconId);
        ze.setLatitude(mLatitude.getText().toString());
        ze.setLongitude(mLongitude.getText().toString());
        ze.setAlias(mAlias.getText().toString());

        String mailProfile = (String)mSpinner_mail.getSelectedItem();
        String moreProfile = (String)mSpinner_more.getSelectedItem();
        String requProfile = (String)mSpinner_requ.getSelectedItem();
//        String smsProfile = (String)mSpinner_sms.getSelectedItem();
        String serverProfile = (String)mSpinner_server.getSelectedItem();

        ze.setId_email(mailProfile.equals("none") ? null : mailProfile);
        ze.setId_more_actions(moreProfile.equals("none") ? null : moreProfile);
        ze.setId_requirements(requProfile.equals("none") ? null : requProfile);
//        ze.setId_sms(smsProfile.equals("none") ? null : smsProfile);
        ze.setId_server(serverProfile.equals("none") ? null : serverProfile);

        // Set tasker settings
        ze.setTrack_id_email(GlobalSingleton.getInstance().getZoneEntity().getTrack_id_email());
        ze.setTrack_to_file(GlobalSingleton.getInstance().getZoneEntity().isTrack_to_file());
        ze.setTrack_url(GlobalSingleton.getInstance().getZoneEntity().getTrack_url());
        ze.setEnter_tracker(GlobalSingleton.getInstance().getZoneEntity().isEnter_tracker());
        ze.setExit_tracker(GlobalSingleton.getInstance().getZoneEntity().isExit_tracker());
        ze.setLocal_tracking_interval(GlobalSingleton.getInstance().getZoneEntity().getLocal_tracking_interval());

        datasource.storeZone(ze);

        Intent data = new Intent();
        data.putExtra("action","add");
        data.putExtra("beaconRegion", region);
        data.putExtra("beaconBeaconId", mBeaconId);
        data.putExtra("beaconMode", mMode.isChecked());
        setResult(RESULT_OK, data);
        finish();
    }

    /**
     * Check all the input values and flag those that are incorrect
     *
     * @return true if all the widget values are correct; otherwise false
     */
    private boolean checkInputFieldsBeacon() {
        // Start with the input validity flag set to true
        if (TextUtils.isEmpty(((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon)).getText())) {
            findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon).setBackgroundColor(Color.RED);
            Snackbar.make(viewMerk, de.egi.geofence.geozone.bt.R.string.geofence_input_error_missing, Snackbar.LENGTH_LONG).show();
            // Set the validity to "invalid" (false)
            return true;
        } else if (((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon)).getText().toString().contains(",")){
            findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon).setBackgroundColor(Color.RED);
            Snackbar.make(viewMerk, de.egi.geofence.geozone.bt.R.string.geofence_input_error_comma, Snackbar.LENGTH_LONG).show();
            // Set the validity to "invalid" (false)
            return true;
        } else if (((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon)).getText().toString().contains("'")){
            findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon).setBackgroundColor(Color.RED);
            Snackbar.make(viewMerk, de.egi.geofence.geozone.bt.R.string.geofence_input_error_comma, Snackbar.LENGTH_LONG).show();
            // Set the validity to "invalid" (false)
            return true;
        }

        if (((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_alias)).getText().toString().contains(",")){
            findViewById(de.egi.geofence.geozone.bt.R.id.value_alias).setBackgroundColor(Color.RED);
            Snackbar.make(viewMerk, de.egi.geofence.geozone.bt.R.string.geofence_input_error_comma, Snackbar.LENGTH_LONG).show();
            // Set the validity to "invalid" (false)
            return true;
        } else if (((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_alias)).getText().toString().contains("'")){
            findViewById(de.egi.geofence.geozone.bt.R.id.value_alias).setBackgroundColor(Color.RED);
            Snackbar.make(viewMerk, de.egi.geofence.geozone.bt.R.string.geofence_input_error_comma, Snackbar.LENGTH_LONG).show();
            // Set the validity to "invalid" (false)
            return true;
        }

        /*
         * Latitude, longitude, and radius values can't be empty. If they are, highlight the input
         * field in red and put a Toast message in the UI. Otherwise set the input field highlight
         * to black, ensuring that a field that was formerly wrong is reset.
         */
        String mBeaconId = ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon_ident1)).getText().toString() + "#" + ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon_ident2)).getText().toString() + "#" + ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon_ident3)).getText().toString();
        if (TextUtils.isEmpty(mBeaconId) || mBeaconId.equals("##")) {
            findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon_ident1).setBackgroundColor(Color.RED);
            findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon_ident2).setBackgroundColor(Color.RED);
            findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon_ident3).setBackgroundColor(Color.RED);
            Snackbar.make(viewMerk, de.egi.geofence.geozone.bt.R.string.geofence_input_error_missing, Snackbar.LENGTH_LONG).show();

            // Set the validity to "invalid" (false)
            return true;
        }
        if (TextUtils.isEmpty(((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_radius)).getText())) {
            findViewById(de.egi.geofence.geozone.bt.R.id.value_radius).setBackgroundColor(Color.RED);
            Snackbar.make(viewMerk, de.egi.geofence.geozone.bt.R.string.geofence_input_error_missing, Snackbar.LENGTH_LONG).show();

            // Set the validity to "invalid" (false)
            return true;
        }

        /*
         * If all the input fields have been entered, test to ensure that their values are within
         * the acceptable range. The tests can't be performed until it's confirmed that there are
         * actual values in the fields.
         */

            /*
             * Get values from radius.
             */
        float rd1 = Float.valueOf(((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_radius)).getText().toString());

        if (rd1 < Constants.MIN_RADIUS) {
            findViewById(de.egi.geofence.geozone.bt.R.id.value_radius).setBackgroundColor(Color.RED);
            Snackbar.make(viewMerk, de.egi.geofence.geozone.bt.R.string.bt_input_error_radius_invalid, Snackbar.LENGTH_LONG).show();

            // Set the validity to "invalid" (false)
            return true;
        }

        // If everything passes, the validity flag will still be true, otherwise it will be false.
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(de.egi.geofence.geozone.bt.R.menu.menu_btfence, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action buttons
        switch (item.getItemId()) {
            case de.egi.geofence.geozone.bt.R.id.menu_delete_geofence:
                android.support.v7.app.AlertDialog.Builder ab = Utils.onAlertDialogCreateSetTheme(this);
                ab.setMessage(de.egi.geofence.geozone.bt.R.string.action_delete).setPositiveButton(de.egi.geofence.geozone.bt.R.string.action_yes, dialogClickListener).setNegativeButton(de.egi.geofence.geozone.bt.R.string.action_no, dialogClickListener).show();
                return true;
            case de.egi.geofence.geozone.bt.R.id.menu_item_clear_geofence:
//                log.debug("onOptionsItemSelected: menu_item_clear_geofence");
                ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon)).setText(Constants.EMPTY_STRING);
                ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon_ident1)).setText(Constants.EMPTY_STRING);
                ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon_ident2)).setText(Constants.EMPTY_STRING);
                ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon_ident3)).setText(Constants.EMPTY_STRING);
                ((EditText) findViewById(R.id.value_mac)).setText(Constants.EMPTY_STRING);
                ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_radius)).setText("1");
                ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_latitude)).setText(Constants.EMPTY_STRING);
                ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_longitude)).setText(Constants.EMPTY_STRING);
                ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_alias)).setText(Constants.EMPTY_STRING);

                ((Spinner) findViewById(de.egi.geofence.geozone.bt.R.id.spinner_server_profile)).setSelection(0, true);
//                ((Spinner) findViewById(de.egi.geofence.geozone.bt.R.id.spinner_sms_profile)).setSelection(0, true);
                ((Spinner) findViewById(de.egi.geofence.geozone.bt.R.id.spinner_mail_profile)).setSelection(0, true);
                ((Spinner) findViewById(de.egi.geofence.geozone.bt.R.id.spinner_more_profile)).setSelection(0, true);
                ((Spinner) findViewById(de.egi.geofence.geozone.bt.R.id.spinner_requirements_profile)).setSelection(0, true);
                return true;
            case de.egi.geofence.geozone.bt.R.id.menu_get_location:
                log.debug("onOptionsItemSelected: menu_get_location");
                mLocationClient = new GoogleApiClient.Builder(this, this, this).addApi(LocationServices.API).build();
                mLocationClient.connect();

                return true;
            case de.egi.geofence.geozone.bt.R.id.menu_scan:
                Intent iBeacons = new Intent(this, BeaconScanAll.class);
                startActivityForResult(iBeacons, 4721);
                return true;
            case de.egi.geofence.geozone.bt.R.id.menu_item_help:
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
                    String zoneToDelete = ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon)).getText().toString();
                    String mBeaconId = ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon_ident1)).getText().toString() + "#" +
                            ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon_ident2)).getText().toString() + "#" +
                            ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon_ident3)).getText().toString() + "#" +
                            ((EditText) findViewById(R.id.value_mac)).getText().toString();
                    data.putExtra("zoneToDelete", zoneToDelete);
                    data.putExtra("beaconBeaconId", mBeaconId);
                    setResult(RESULT_OK, data);
                    finish();
                case DialogInterface.BUTTON_NEGATIVE:
                    //Do your No progress
                    break;
            }
        }
    };
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
     * Add SMS
     */
    @SuppressWarnings("UnusedParameters")
    public void onAddSmsClicked(View view) {
        log.debug("onAddSmsClicked");
        Intent i = new Intent(this, SmsProfile.class);
        i.putExtra("action", "new");
        startActivityForResult(i, 4812);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // Choose what to do based on the request code
        switch (requestCode) {
            // Beacon
            case 4721 :
                if (resultCode == RESULT_OK) {
                    try {
                    String beacon = intent.getStringExtra("beacon");
                    List<Identifier> listB = Utils.getStringIdentifiers(beacon);
                    ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon_ident1)).setText(listB.get(0).toString());
                    ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon_ident2)).setText(listB.get(1).toString());
                    ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_beacon_ident3)).setText(listB.get(2).toString());

                        String mac = Utils.getStringMacAddress(beacon);
                        ((EditText) findViewById(R.id.value_mac)).setText(mac);

                    } catch (Throwable throwable) {
                        log.error(throwable.getMessage());
                    }

                }

                BeaconManager mBeaconManager = BeaconManager.getInstanceForApplication(this);
                // Scan lasts for SCAN_PERIOD time
                mBeaconManager.setForegroundScanPeriod(Utils.getDefaultForegroundScanPeriod(this));
                // Wait every SCAN_PERIOD_INBETWEEN time
                mBeaconManager.setForegroundBetweenScanPeriod(Utils.getDefaultForegroundBetweenScanPeriod(this));

                // Update default time with the new one
                try {
                    mBeaconManager.updateScanPeriods();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                break;
            // Add Server
            case 4811:
                fillSpinnerServer();
                break;
            // Add SMS
            case 4812:
//                fillSpinnerSMS();
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
                Log.d(Constants.APPTAG, getString(de.egi.geofence.geozone.bt.R.string.unknown_activity_request_code, requestCode));
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
                alertDialogBuilder.setMessage(getString(de.egi.geofence.geozone.bt.R.string.alertPermissions));
                alertDialogBuilder.setTitle(getString(de.egi.geofence.geozone.bt.R.string.titleAlertPermissions));

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
                ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_latitude)).setText(Double.valueOf(currentLocation.getLatitude()).toString());
                ((EditText) findViewById(de.egi.geofence.geozone.bt.R.id.value_longitude)).setText(Double.valueOf(currentLocation.getLongitude()).toString());
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
            Log.d(Constants.APPTAG, getString(de.egi.geofence.geozone.bt.R.string.play_services_available));
            log.info("servicesConnected result from Google Play Services: " + getString(de.egi.geofence.geozone.bt.R.string.play_services_available));
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
            case de.egi.geofence.geozone.bt.R.id.scan:
                log.debug("onBeaconScanClicked");
                Intent iBeacons = new Intent(this, BeaconScanAll.class);
                startActivityForResult(iBeacons, 4721);
                break;
            case de.egi.geofence.geozone.bt.R.id.getLocation:
                log.debug("getCurrentLocation");
                mLocationClient = new GoogleApiClient.Builder(this, this, this).addApi(LocationServices.API).build();
                mLocationClient.connect();
                break;
            default:
                break;
        }
    }
}
