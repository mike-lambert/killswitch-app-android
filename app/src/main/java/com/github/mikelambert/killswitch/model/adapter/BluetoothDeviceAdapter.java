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

import java.util.List;

public class BluetoothDeviceAdapter extends ArrayAdapter<BluetoothDevice> {
    private BluetoothDevice selected;
    public BluetoothDeviceAdapter(@NonNull Context context, int resource, @NonNull List<BluetoothDevice> objects) {
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
        TextView name = convertView.findViewById(R.id.item_dle_device_name);
        name.setText(device.getName());
        name.setOnClickListener(view -> {
            selected = device;
        });
        // Return the completed view to render on screen
        return convertView;
    }
}
