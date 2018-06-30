package de.egi.geofence.geozone.bt.beacon;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import de.egi.geofence.geozone.bt.R;

/**
 * Created by RitterE on 08.08.2017.
 */

public class BeaconListAdapter extends BaseAdapter {

    private final Context context;
    private final ArrayList<BeaconItem> beaconItems;

    public BeaconListAdapter(Context context, ArrayList<BeaconItem> beaconItems){
        this.context = context;
        this.beaconItems = beaconItems;
    }

    @Override
    public int getCount() {
        return beaconItems.size();
    }

    @Override
    public Object getItem(int position) {
        return beaconItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = mInflater.inflate(R.layout.beacon_scan_item, null);
        }

        TextView uuid = (TextView) convertView.findViewById(R.id.drawer_item_uuid);
        TextView major = (TextView) convertView.findViewById(R.id.drawer_item_major);
        TextView minor = (TextView) convertView.findViewById(R.id.drawer_item_minor);
        TextView distance = (TextView) convertView.findViewById(R.id.drawer_item_distance);
        TextView rssi = (TextView) convertView.findViewById(R.id.drawer_item_rssi);
        TextView macAddress = (TextView) convertView.findViewById(R.id.drawer_item_mac);

        if (position == 0 && beaconItems.size() == 0) {
            uuid.setText("BL not found!");
        }else {
            if (beaconItems.get(position).getBeacon().getIdentifiers().size() == 3) {
                uuid.setText(beaconItems.get(position).getBeacon().getIdentifier(0) + "");
                major.setText(beaconItems.get(position).getBeacon().getIdentifier(1) + "");
                minor.setText(beaconItems.get(position).getBeacon().getIdentifier(2) + "");
                macAddress.setText(beaconItems.get(position).getBeacon().getBluetoothAddress());
                distance.setText(String.format("(%.2f m)", beaconItems.get(position).getBeacon().getDistance()));
                rssi.setText(beaconItems.get(position).getBeacon().getRssi() + "");
            }
        }

        notifyDataSetChanged();

        return convertView;
    }
}