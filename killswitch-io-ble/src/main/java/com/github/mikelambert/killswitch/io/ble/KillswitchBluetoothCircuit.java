package com.github.mikelambert.killswitch.io.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.github.mikelambert.killswitch.common.CircuitState;
import com.github.mikelambert.killswitch.common.HardwareCircuit;

import java.math.BigInteger;
import java.util.UUID;

import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;

public class KillswitchBluetoothCircuit implements HardwareCircuit {
    public static final UUID UUID_KILLSWITCH_BLE_SERVICE = UUID.fromString("0000F001-0000-1000-8000-00805F9B34FB");
    public static final UUID UUID_KILLSWITCH_BLE_CHARACTERISTIC = UUID.fromString("0000F002-0000-1000-8000-00805F9B34FB");

    private final ScanResult descriptor;
    private final Context context;
    private BluetoothGatt gatt;
    private BluetoothGattService service;
    private BluetoothGattCharacteristic source;
    private boolean locked;
    private boolean fireOnDisconnect;
    private CircuitState state;
    private BigInteger lastId;

    public KillswitchBluetoothCircuit(Context context, ScanResult descriptor) {
        this.context = context;
        this.descriptor = descriptor;
        setupConnection();
    }

    public void setupConnection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            gatt = descriptor.getDevice().connectGatt(context, false,
                    new BluetoothGattCallback() {
                        @Override
                        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                            switch (newState){
                                case STATE_DISCONNECTED:
                                    Log.v("BLE", "DISCONNECT");
                                    KillswitchBluetoothCircuit.this.gatt = null;
                                    service = null;
                                    source = null;
                                    onDisconnect();
                                    break;
                                case STATE_CONNECTED:
                                    KillswitchBluetoothCircuit.this.gatt = gatt;
                                    Log.v("BLE", "CONNECT; GATT: " + KillswitchBluetoothCircuit.this.gatt);
                                    onConnect();
                                    break;
                            }
                        }

                        @Override
                        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                            service = gatt.getService(UUID_KILLSWITCH_BLE_SERVICE);
                            Log.v("BLE", "SERVICES DISCOVERED; target: " + service);
                        }

                        @Override
                        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                            Log.v("BLE", "NOTIFIED; source : " + characteristic.getUuid() + "; value: " + hex(characteristic.getValue()));
                            if (UUID_KILLSWITCH_BLE_CHARACTERISTIC.equals(characteristic.getUuid())){
                                Log.v("BLE", "KILLSWITCH SOURCE acquired");
                                byte[] payload = characteristic.getValue();
                                onPayload(payload);
                            }
                        }
                    }
            );
        }
    }

    private String hex(byte[] value) {
        StringBuilder sb = new StringBuilder(value.length * 2);
        for(byte b: value) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void onPayload(byte[] payload) {
        if(payload == null){
            return;
        }
        if (payload.length < 2){
            return;
        }
        byte appLayer = payload[0];
        byte dataLen = payload[1];
        if (appLayer != 0x00){
            Log.v("BLE", "Unknown trigger protocol: " + appLayer);
            return;
        }
        if (dataLen < 8){
            Log.v("BLE", "Unknown payload size: " + dataLen);
            return;
        }
        if (payload.length < dataLen + 2){
            Log.v("BLE", "Protocol mismatch. Declared packet size " + dataLen + ", overall buffer " + payload.length);
            return;
        }
        byte[] packet = new byte[dataLen];
        System.arraycopy(payload, 2, packet, 0, dataLen);
        BigInteger value = new BigInteger(1, packet);
        lastId = value;
        Log.v("BLE", " <- " + value.toString());
    }

    private void onConnect() {
        Log.v("BLE", "");
    }

    private void onDisconnect() {
        Log.v("BLE", "");
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
        return state;
    }
}
