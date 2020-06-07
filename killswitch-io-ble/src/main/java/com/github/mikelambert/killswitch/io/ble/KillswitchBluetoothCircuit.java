package com.github.mikelambert.killswitch.io.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.github.mikelambert.killswitch.common.CircuitState;
import com.github.mikelambert.killswitch.common.HardwareCircuit;
import com.github.mikelambert.killswitch.common.Intents;

import java.math.BigInteger;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;

public class KillswitchBluetoothCircuit implements HardwareCircuit {
    public static final UUID UUID_KILLSWITCH_BLE_SERVICE = UUID.fromString("0000F001-0000-1000-8000-00805F9B34FB");
    public static final UUID UUID_KILLSWITCH_BLE_CHARACTERISTIC = UUID.fromString("0000F002-0000-1000-8000-00805F9B34FB");
    public static final UUID UUID_KILLSWITCH_BLE_DESC = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");
    public static final BigInteger VALUE_TRIGGERED = new BigInteger("18446744073150521005");

    private final BluetoothDevice device;
    private final Context context;
    private final ExecutorService pool;
    private final Object pingLock;
    private BluetoothGatt gatt;
    private BluetoothGattService service;
    private BluetoothGattCharacteristic source;
    private boolean locked;
    private boolean fireOnDisconnect;
    private CircuitState state;
    private BigInteger lastId;
    private long disconnectTime;
    private long lastReconnectTime;

    public KillswitchBluetoothCircuit(Context context, BluetoothDevice device) {
        this.pingLock = new Object();
        this.context = context;
        this.device = device;
        this.pool = Executors.newFixedThreadPool(6);
        this.state = CircuitState.OFFLINE;
    }

    public void setupConnection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            gatt = device.connectGatt(context, false,
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
                            Log.v("BLE", "SERVICES DISCOVERED; target: " + service.getUuid());
                            subscribeToKillswitchService(gatt);
                            state = CircuitState.ENGAGED;
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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void subscribeToKillswitchService(BluetoothGatt gatt) {
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID_KILLSWITCH_BLE_CHARACTERISTIC);
        gatt.setCharacteristicNotification(characteristic, true);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID_KILLSWITCH_BLE_DESC);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(descriptor);
        Log.v("BLE", "Subscribed to notifications for " + UUID_KILLSWITCH_BLE_CHARACTERISTIC);
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
        handleProtocolVer0(payload);
    }

    private boolean handleProtocolVer0(byte[] payload) {
        // protocol v.0
        if (payload.length == 8){
            BigInteger value = new BigInteger(1, payload);
            lastId = value;
            Log.v("BLE", " <- " + value.toString());
            dispatchEvent();
            return true;
        }
        Log.v("BLE", "Unknown payload size for v.0: " + payload.length);
        return false;
    }

    private void dispatchEvent() {
        if (lastId.equals(VALUE_TRIGGERED)) {
            Log.v("BLE", "Trigger value received");
            onTriggered();
        }
        onHeartbeat();
        sendAck();
    }

    private void sendAck() {
    }

    private void onTriggered() {
        if (state != CircuitState.TRIGGERED) {
            Log.v("BLE", "TRIGGERED");
            state = CircuitState.TRIGGERED;
            Log.v("BLE", "Sending trigger intent");
            context.sendBroadcast(Intents.createKillswitchTriggerIntent(Intents.FLAG_KILLSWITCH_TRIGGER_RED_BUTTON));
        }
    }

    private void onHeartbeat() {
        synchronized (pingLock){
            pingLock.notifyAll();
        }
    }

    private void onConnect() {
        Log.v("BLE", "START DISCOVERING");
        state = CircuitState.CONNECTED;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            gatt.discoverServices();
        }
    }

    private void onDisconnect() {
        Log.v("BLE", "DISCONNECTED");
        disconnectTime = System.currentTimeMillis();
        if (fireOnDisconnect){
            state = CircuitState.OFFLINE;
        } else {
            state = CircuitState.COUNTDOWN;
            reconnect();
        }
    }

    private void reconnect() {
        pool.submit(() -> {
            final Object countdownLock = new Object();
            while (true){
                lastReconnectTime = System.currentTimeMillis();
                try {
                    setupConnection();
                } catch (Exception e){
                    Log.w("BLE", "reconnect failed", e);
                }finally {
                    synchronized (countdownLock){
                        try {
                            countdownLock.wait(1000);
                        } catch (InterruptedException e) {

                        }
                    }
                }
                if (state == CircuitState.ENGAGED){
                    Log.v("BLE", "connection re-established");
                    return;
                } else if(state == CircuitState.COUNTDOWN){
                    Log.v("BLE", "Countdown: " + (lastReconnectTime - disconnectTime));
                    if (lastReconnectTime - disconnectTime > 10000){
                        Log.v("BLE", "Blackout trigger: " + (lastReconnectTime - disconnectTime));
                        onTriggered();
                        return;
                    }
                } else {
                    Log.v("BLE", "irreleveant state: " + state);
                    return;
                }
            }
        });
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
    public boolean ping() {
        synchronized (pingLock){
            try {
                long now = System.currentTimeMillis();
                pingLock.wait(5000L);
                long end = System.currentTimeMillis();
                return end - now <= 5000;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    @Override
    public CircuitState state() {
        return state;
    }

    @Override
    public void disconnect() {
        if (device != null && gatt != null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                Log.v("BLE", "DISCONNECTING");
                fireOnDisconnect = false;
                gatt.disconnect();
            }
        }
    }
}