package de.egi.geofence.geozone.bt.beacon;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import de.egi.geofence.geozone.bt.EgiGeoZoneApplication;
import de.egi.geofence.geozone.bt.R;
import de.egi.geofence.geozone.bt.db.DbGlobalsHelper;
import de.egi.geofence.geozone.bt.utils.Constants;
import de.egi.geofence.geozone.bt.utils.Utils;

public class BeaconScanPeriods extends AppCompatActivity implements TextWatcher {
    private EditText defaultForegroundScanPeriod = null;
    private EditText defaultForegroundBetweenScanPeriod = null;
    private EditText defaultBackgroundScanPeriod = null;
    private EditText defaultBackgroundBetweenScanPeriod = null;
    private DbGlobalsHelper dbGlobalsHelper;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.onActivityCreateSetTheme(this);
        setContentView(R.layout.beacon_scan_periods);
        dbGlobalsHelper = new DbGlobalsHelper(this);

        defaultForegroundScanPeriod = ((EditText) this.findViewById(R.id.value_foreground_scan_period));
        String fsp = dbGlobalsHelper.getCursorGlobalsByKey(Constants.DB_KEY_DEFAULT_FOREGROUND_SCAN_PERIOD);
        defaultForegroundScanPeriod.setText(fsp == null ? EgiGeoZoneApplication.DEFAULT_FOREGROUND_SCAN_PERIOD : fsp);
        defaultForegroundScanPeriod.setSelection(defaultForegroundScanPeriod.getText().length());
        defaultForegroundScanPeriod.addTextChangedListener(this);

        defaultForegroundBetweenScanPeriod = ((EditText) this.findViewById(R.id.value_foreground_between_scan_period));
        String fbsp = dbGlobalsHelper.getCursorGlobalsByKey(Constants.DB_KEY_DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD);
        defaultForegroundBetweenScanPeriod.setText(fbsp == null ? EgiGeoZoneApplication.DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD : fbsp);
        defaultForegroundBetweenScanPeriod.addTextChangedListener(this);

        defaultBackgroundScanPeriod = ((EditText) this.findViewById(R.id.value_background_scan_period));
        String bsp = dbGlobalsHelper.getCursorGlobalsByKey(Constants.DB_KEY_DEFAULT_BACKGROUND_SCAN_PERIOD);
        defaultBackgroundScanPeriod.setText(bsp == null ? EgiGeoZoneApplication.DEFAULT_BACKGROUND_SCAN_PERIOD : bsp);
        defaultBackgroundScanPeriod.addTextChangedListener(this);

        defaultBackgroundBetweenScanPeriod = ((EditText) this.findViewById(R.id.value_background_between_scan_period));
        String bbsp = dbGlobalsHelper.getCursorGlobalsByKey(Constants.DB_KEY_DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD);
        defaultBackgroundBetweenScanPeriod.setText(bbsp == null ? EgiGeoZoneApplication.DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD : bbsp);
        defaultBackgroundBetweenScanPeriod.addTextChangedListener(this);

    }

    @Override
    public void afterTextChanged(Editable s) {
        if (defaultForegroundScanPeriod.getText().hashCode() == s.hashCode()) {
            dbGlobalsHelper.storeGlobals(Constants.DB_KEY_DEFAULT_FOREGROUND_SCAN_PERIOD,
                    (defaultForegroundScanPeriod.getText().toString().equalsIgnoreCase("") ? EgiGeoZoneApplication.DEFAULT_FOREGROUND_SCAN_PERIOD : defaultForegroundScanPeriod.getText().toString()));
        } else if (defaultForegroundBetweenScanPeriod.getText().hashCode() == s.hashCode()) {
            dbGlobalsHelper.storeGlobals(Constants.DB_KEY_DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD,
                    (defaultForegroundBetweenScanPeriod.getText().toString().equalsIgnoreCase("") ? EgiGeoZoneApplication.DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD : defaultForegroundBetweenScanPeriod.getText().toString()));
        }else if (defaultBackgroundScanPeriod.getText().hashCode() == s.hashCode()) {
            dbGlobalsHelper.storeGlobals(Constants.DB_KEY_DEFAULT_BACKGROUND_SCAN_PERIOD,
                    (defaultBackgroundScanPeriod.getText().toString().equalsIgnoreCase("") ? EgiGeoZoneApplication.DEFAULT_BACKGROUND_SCAN_PERIOD : defaultBackgroundScanPeriod.getText().toString()));
        } else if (defaultBackgroundBetweenScanPeriod.getText().hashCode() == s.hashCode()) {
            dbGlobalsHelper.storeGlobals(Constants.DB_KEY_DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD,
                    (defaultBackgroundBetweenScanPeriod.getText().toString().equalsIgnoreCase("") ? EgiGeoZoneApplication.DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD : defaultBackgroundBetweenScanPeriod.getText().toString()));
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

}