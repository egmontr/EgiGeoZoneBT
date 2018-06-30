package de.egi.geofence.geozone.bt.fences;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.egi.geofence.geozone.bt.R;
import de.egi.geofence.geozone.bt.utils.Utils;

public class WifiScanAll extends AppCompatActivity {
    private ListView listViewWifi = null;
    private TextView wait = null;
    private WifiManager mWifiManager;
    private ArrayList<HashMap<String, String>> arraylist = new ArrayList<HashMap<String, String>>();
    private HashMap<String, String> arraylistL = new HashMap<String, String>();
    private SimpleAdapter adapter;
    private final String SSID = "ssid";
    private final String BSSID = "bssid";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.onActivityCreateSetTheme(this);
        setContentView(R.layout.wifi_info);
        listViewWifi = (ListView)findViewById(R.id.listView_wifis);
        wait = (TextView) findViewById(R.id.listText_waitWifi);

        listViewWifi.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                arraylistL = (HashMap<String, String>) listViewWifi.getItemAtPosition(position);
                Intent data = new Intent();
                data.putExtra("wifi_info", arraylistL.get(SSID) + "##" + arraylistL.get(BSSID));
                setResult(RESULT_OK, data);
                finish();
            }
        });


        arraylist.clear();

        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        registerReceiver(mWifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mWifiManager.startScan();

        this.adapter = new SimpleAdapter(WifiScanAll.this, arraylist, R.layout.profile_list_item, new String[] { SSID, BSSID }, new int[] { R.id.profName, R.id.profWert });
        listViewWifi.setAdapter(this.adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            this.unregisterReceiver(mWifiScanReceiver);
        } catch (Exception e) {
        }
    }

    private final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                List<ScanResult> mScanResults = mWifiManager.getScanResults();
                Log.d("", "");
                for (ScanResult scanResult : mScanResults){
                    HashMap<String, String> item = new HashMap<>();
                    item.put(SSID, scanResult.SSID.toString());
                    item.put(BSSID, scanResult.BSSID.toString());
                    arraylist.add(item);
                }
            }
            wait.setText("");
            adapter.notifyDataSetChanged();
        }
    };
}
