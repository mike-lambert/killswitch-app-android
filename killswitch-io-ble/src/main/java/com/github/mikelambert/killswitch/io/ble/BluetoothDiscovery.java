package com.github.mikelambert.killswitch.io.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.util.Log;

import java.util.List;

public class BluetoothDiscovery {
    public BluetoothDiscovery() {
        primary = BluetoothAdapter.getDefaultAdapter();
        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                if (callback != null){
                    Log.v("BLE/Discovery", "Device discovered: " + result);
                    synchronized (callback) {
                        callback.deviceDiscovered(result.getDevice());
                    }
                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                if (callback != null){
                    synchronized (callback) {
                        callback.discoveryBegin();
                        for (ScanResult result : results) {
                            Log.v("BLE/Discovery", "Device discovered: " + result);
                            callback.deviceDiscovered(result.getDevice());
                        }
                    }
                }
            }
        };
    }

    public interface DiscoveryEventCallback {
        void discoveryBegin();
        void discoveryDone();
        void deviceDiscovered(BluetoothDevice device);
    }

    private DiscoveryEventCallback callback;
    private final BluetoothAdapter primary;
    private boolean discoveryPending;
    private final ScanCallback scanCallback;

    public DiscoveryEventCallback getCallback() {
        return callback;
    }

    public void setCallback(DiscoveryEventCallback callback) {
        this.callback = callback;
    }

    public static BluetoothDiscovery createScanner(DiscoveryEventCallback callback) {
        BluetoothDiscovery result = new BluetoothDiscovery();
        result.setCallback(callback);
        return result;
    }

    public void stopDiscovery() {
        if (primary == null){
            Log.w("BLE/Discovery", "Bluetooth unavailable");
            return;
        }
        synchronized (primary){
            Log.v("BLE/Discovery", "Discovery cancelled");
            primary.getBluetoothLeScanner().stopScan(scanCallback);
        }
    }

    public void startDiscovery(){
        if (primary == null){
            Log.w("BLE/Discovery", "Bluetooth unavailable");
            return;
        }
        synchronized (primary) {
            if (discoveryPending){
                primary.getBluetoothLeScanner().stopScan(scanCallback);
            }
            discoveryPending = true;
            if (callback != null){
                callback.discoveryBegin();
            }
            Log.v("BLE/Discovery", "Starting discovery");
            primary.getBluetoothLeScanner().startScan(scanCallback);
        }
    }
}
