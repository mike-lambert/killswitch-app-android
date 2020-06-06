package com.github.mikelambert.killswitch.io.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Build;

import com.github.mikelambert.killswitch.common.CircuitState;
import com.github.mikelambert.killswitch.common.HardwareCircuit;

import java.util.UUID;

import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;

public class KillswitchBluetoothCircuit implements HardwareCircuit {
    public static final UUID UUID_KILLSWITCH_BLE = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final ScanResult descriptor;
    private final Context context;
    private BluetoothGatt gatt;
    private boolean locked;
    private boolean fireOnDisconnect;

    public KillswitchBluetoothCircuit(Context context, ScanResult descriptor) {
        this.context = context;
        this.descriptor = descriptor;
        setupConnection();
    }

    private void setupConnection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            gatt = descriptor.getDevice().connectGatt(context, false,
                    new BluetoothGattCallback() {
                        @Override
                        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                            switch (newState){
                                case STATE_DISCONNECTED:
                                    break;
                                case STATE_CONNECTED:
                                    break;
                            }
                        }

                        @Override
                        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

                        }

                        @Override
                        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                            if (UUID_KILLSWITCH_BLE.equals(characteristic.getUuid())){
                                byte[] payload = characteristic.getValue();
                            }
                        }
                    }
            );
        }
    }

    @Override
    public boolean isTarget() {
        return locked;
    }

    @Override
    public void lockOn(boolean fireOnDisconnect) {
        locked = true;
        this.fireOnDisconnect = fireOnDisconnect;
    }

    @Override
    public void unlock() {
        locked = false;
    }

    @Override
    public void ping() {

    }

    @Override
    public CircuitState state() {
        return null;
    }
}
