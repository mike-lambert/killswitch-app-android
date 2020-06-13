package com.github.mikelambert.killswitch.model.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.github.mikelambert.killswitch.R;
import com.github.mikelambert.killswitch.io.ble.BluetoothDiscovery;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BluetoothDeviceListViewAdapter extends ArrayAdapter<BluetoothDevice> implements BluetoothDiscovery.DiscoveryEventCallback {
    private BluetoothDevice selected;
    private BluetoothDeviceListViewAdapter(@NonNull Context context, int resource, @NonNull List<BluetoothDevice> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        final BluetoothDevice device = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_ble_device, parent, false);
        }
        // Lookup view for data population
        TextView name = convertView.findViewById(R.id.item_ble_device_name);
        name.setText(device.getName());
        if (device.getName() == null || device.getName().trim().isEmpty()){
            name.setText(device.getAddress());
        }

        name.setOnClickListener(view -> {
            selected = device;
        });
        // Return the completed view to render on screen
        return convertView;
    }

    public BluetoothDevice getSelectedDevice(){
        return selected;
    }

    public static BluetoothDeviceListViewAdapter create(Context context){
        return new BluetoothDeviceListViewAdapter(context, R.layout.item_ble_device, new CopyOnWriteArrayList<>());
    }

    @Override
    public void discoveryBegin() {
        clear();
    }

    @Override
    public void discoveryDone() {

    }

    @Override
    public void deviceDiscovered(BluetoothDevice device) {
        if (contains(device)){
            //Log.v("DiscoveryList", device + " already registered");
            return;
        }
        add(device);
    }

    private boolean contains(BluetoothDevice device){
        for(int i = 0; i < getCount(); i++){
            BluetoothDevice stored = getItem(i);
            if (stored.getAddress().equalsIgnoreCase(device.getAddress())){
                return true;
            }
        }
        return false;
    }
}
