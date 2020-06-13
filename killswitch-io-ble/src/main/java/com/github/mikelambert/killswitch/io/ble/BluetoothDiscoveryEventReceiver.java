package com.github.mikelambert.killswitch.io.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.List;

public class BluetoothDiscoveryEventReceiver extends BroadcastReceiver {
    public BluetoothDiscoveryEventReceiver() {
        primary = BluetoothAdapter.getDefaultAdapter();
    }

    public interface DiscoveryEventCallback {
        void discoveryBegin();
        void discoveryDone();
        void deviceDiscovered(BluetoothDevice device);
    }

    private DiscoveryEventCallback callback;
    private final BluetoothAdapter primary;
    private boolean discoveryPending;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
            //discovery starts, we can show progress dialog or perform other tasks
            Log.v("BLE/Discovery", "Discovery started");
            if (callback != null){
                callback.discoveryBegin();
            }
        } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
            //discovery finishes, dismis progress dialog
            Log.v("BLE/Discovery", "Discovery finished");
            if (callback != null){
                callback.discoveryDone();
            }
        } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            Object result = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            Log.v("BLE/Discovery", "Device discovered: " + result);
            BluetoothDevice device = null;
            if (result instanceof BluetoothDevice) {
                device = (BluetoothDevice)result;
            }

            if (result instanceof ScanResult){
                ScanResult scan = (ScanResult)result;
                device = scan.getDevice();
            }
            if (device != null && callback != null){
                callback.deviceDiscovered(device);
            }
        }
    }

    public DiscoveryEventCallback getCallback() {
        return callback;
    }

    public void setCallback(DiscoveryEventCallback callback) {
        this.callback = callback;
    }

    public static BluetoothDiscoveryEventReceiver registerReceiver(Context context) {
        BluetoothDiscoveryEventReceiver result = new BluetoothDiscoveryEventReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(result, filter);
        return result;
    }

    public void startDiscovery(){
        if (primary == null){
            Log.w("BLE/Discovery", "Bluetooth unavailable");
            return;
        }
        synchronized (primary) {
            if (discoveryPending){
                primary.getBluetoothLeScanner().stopScan(new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        super.onScanResult(callbackType, result);
                    }
                });
            }
            discoveryPending = true;
            if (callback != null){
                callback.discoveryBegin();
            }
            Log.v("BLE/Discovery", "Starting discovery");
            primary.getBluetoothLeScanner().startScan(new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    if (callback != null){
                        callback.deviceDiscovered(result.getDevice());
                    }
                }

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    if (callback != null){
                        for(ScanResult result : results) {
                            callback.deviceDiscovered(result.getDevice());
                        }
                    }
                }
            });
        }
    }
}
