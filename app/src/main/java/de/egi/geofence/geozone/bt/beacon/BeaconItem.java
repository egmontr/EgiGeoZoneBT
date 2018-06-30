package de.egi.geofence.geozone.bt.beacon;


import org.altbeacon.beacon.Beacon;

/**
 * Created by RitterE on 08.08.2017.
 */

public class BeaconItem {
    private Beacon beacon;
    //		holder.manufacturerTextView.setText("Manufacturer: " + beacon.getManufacturer());
//		holder.idOneTextView.setText("UUID: " + beacon.getId1());
//		holder.idTwoTextView.setText("Major: " + beacon.getId2());
//		holder.idThreeTextView.setText("Minor: " + beacon.getId3());
//		holder.txPowerTextView.setText("TX-Power: " + beacon.getTxPower());
//		holder.rssiTextView.setText("RSSI: " + beacon.getRssi());
//		holder.distanceTextView.setText(String.format("DISTANCE: (%.2f m)", beacon.getDistance()));
//		holder.nameTextView.setText("Bluetooth Name: " + beacon.getBluetoothName());
//		holder.addressTextView.setText("Bluetooth Adrs: " + beacon.getBluetoothAddress());

    public BeaconItem(){}

    public BeaconItem(Beacon beacon){
        this.beacon= beacon;
    }
    public Beacon getBeacon() {
        return beacon;
    }

    public void setBeacon(Beacon beacon) {
        this.beacon = beacon;
    }
}