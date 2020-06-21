package com.github.mikelambert.killswitch.io.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import com.github.mikelambert.killswitch.KillswitchApplication;
import com.github.mikelambert.killswitch.common.CircuitState;
import com.github.mikelambert.killswitch.common.HardwareCircuit;
import com.github.mikelambert.killswitch.common.Intents;
import com.github.mikelambert.killswitch.event.KillswitchBluetoothGracefulDisconnect;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;

public class KillswitchBluetoothCircuit implements HardwareCircuit {
    public static final UUID UUID_KILLSWITCH_BLE_SERVICE_PING = UUID.fromString("0000F001-0000-1000-8000-00805F9B34FB");
    public static final UUID UUID_KILLSWITCH_BLE_PING = UUID.fromString("0000F002-0000-1000-8000-00805F9B34FB");
    public static final UUID UUID_KILLSWITCH_BLE_SERVICE_LED = UUID.fromString("0000F021-0000-1000-8000-00805F9B34FB");
    public static final UUID UUID_KILLSWITCH_BLE_LED = UUID.fromString("0000F022-0000-1000-8000-00805F9B34FB");
    public static final UUID UUID_KILLSWITCH_BLE_PING_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");

    public static final byte[] KILLSWITCH_LED_ON = new byte[]{0x00, 0x00};
    public static final byte[] KILLSWITCH_LED_OFF = new byte[]{0x00, 0x01};
    public static final byte[] KILLSWITCH_LED_BLINK = new byte[]{0x23, 0x01};

    private final BluetoothDevice device;
    private final Context context;
    private final ExecutorService pool;
    private final Object pingLock;
    private BluetoothGatt gatt;
    private BluetoothGattService servicePing;
    private boolean locked;
    private boolean fireOnDisconnect;
    private CircuitState state;
    private byte[] packet;
    private long disconnectTime;
    private long lastReconnectTime;

    public KillswitchBluetoothCircuit(Context context, BluetoothDevice device) {
        this.pingLock = new Object();
        this.context = context;
        this.device = device;
        this.pool = Executors.newFixedThreadPool(6);
        this.state = CircuitState.OFFLINE;
    }

    public KillswitchBluetoothCircuit(Context context, String descriptor) {
        this.pingLock = new Object();
        this.context = context;
        this.pool = Executors.newFixedThreadPool(6);
        this.state = CircuitState.OFFLINE;
        BluetoothDevice dev = null;
        for(BluetoothDevice d : BluetoothAdapter.getDefaultAdapter().getBondedDevices()) {
            if (descriptor.equals(d.getAddress())) {
                Log.v("BLE", "Found bonded device " + d);
                dev = d;
                break;
            }
        }
        if (dev == null){
            throw new IllegalStateException("Not found in bonded devices: " + descriptor);
        }
        this.device = dev;
    }

    public void connect() {
        gatt = device.connectGatt(context, false,
                new BluetoothGattCallback() {
                    @Override
                    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                        switch (newState) {
                            case STATE_DISCONNECTED:
                                Log.v("BLE", "DISCONNECT");
                                onDisconnect();
                                KillswitchBluetoothCircuit.this.gatt = null;
                                servicePing = null;
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
                        servicePing = gatt.getService(UUID_KILLSWITCH_BLE_SERVICE_PING);
                        for (BluetoothGattService s : gatt.getServices()) {
                            Log.v("BLE", "s: " + s.getUuid().toString());
                            for (BluetoothGattCharacteristic c : s.getCharacteristics()) {
                                Log.v("BLE", "  c: " + c.getUuid().toString());
                                for (BluetoothGattDescriptor d : c.getDescriptors()) {
                                    Log.v("BLE", "    d: " + d.getUuid());
                                }
                            }
                        }
                        Log.v("BLE", "SERVICES DISCOVERED; notification source: " + servicePing.getUuid());
                        pool.submit(() -> subscribeToKillswitchService());
                        pool.submit(() -> ledBlink());
                        state = CircuitState.ENGAGED;
                    }

                    @Override
                    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                        synchronized (gatt) {
                            Log.v("BLE", "NOTIFIED; source : " + characteristic.getUuid() + "; value: " + hex(characteristic.getValue()));
                            if (UUID_KILLSWITCH_BLE_PING.equals(characteristic.getUuid())) {
                                Log.v("BLE", "KILLSWITCH SOURCE acquired");
                                byte[] payload = characteristic.getValue();
                                onPayload(payload);
                            }
                        }
                    }
                }
        );

    }

    @Override
    public boolean isTarget() {
        return locked;
    }

    @Override
    public boolean isConnected() {
        return device != null && gatt != null && servicePing != null;
    }

    @Override
    public void lockOn(boolean fireOnDisconnect) {
        Log.v("BLE", "Locked on to token");
        locked = true;
        this.fireOnDisconnect = fireOnDisconnect;
    }

    @Override
    public void unlock() {
        Log.v("BLE", "Locked off from token");
        locked = false;
    }

    @Override
    public boolean ping() {
        synchronized (pingLock) {
            try {
                long now = System.currentTimeMillis();
                pingLock.wait(5000L);
                long end = System.currentTimeMillis();
                return end - now < 5000;
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
        if (device != null && gatt != null) {
            Log.v("BLE", "DISCONNECTING");
            fireOnDisconnect = false;
            ledOff();
            gatt.disconnect();
        }
    }

    @Override
    public String getName() {
        return device != null ? device.getName() : "";
    }

    @Override
    public String getDescriptor() {
        return device != null ? device.getAddress() : null;
    }

    private void subscribeToKillswitchService() {
        if (gatt == null){
            Log.w("BLE", "connection inactive; exiting");
            return;
        }
        if (servicePing == null){
            Log.w("BLE", "service inactive; exiting");
            return;
        }
        synchronized (gatt) {
            BluetoothGattCharacteristic characteristic = servicePing.getCharacteristic(UUID_KILLSWITCH_BLE_PING);
            if (characteristic == null){
                Log.w("BLE", "Ping characteristic not supported");
                return;
            }
            gatt.setCharacteristicNotification(characteristic, true);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID_KILLSWITCH_BLE_PING_DESCRIPTOR);
            int properties = characteristic.getProperties();
            byte[] value = null;
            if ((properties & PROPERTY_NOTIFY) > 0) {
                value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
            } else if ((properties & PROPERTY_INDICATE) > 0) {
                value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
            } else {
                Log.v("BLE", UUID_KILLSWITCH_BLE_PING + " neither support NOTIFY or INDICATE");
                return;
            }
            descriptor.setValue(value);
            boolean result = false;
            int i = 0;
            do {
                if (i > 10) {
                    Log.w("BLE", "Failed to subscribe notifications");
                    disconnect();
                    break;
                }
                result = gatt.writeDescriptor(descriptor);
                synchronized (servicePing) {
                    try {
                        servicePing.wait(330);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                i++;
            } while (!result);

            Log.v("BLE", "Subscribed to notifications for " + UUID_KILLSWITCH_BLE_PING);
        }
    }

    private void ledBlink() {
        if (gatt != null) {
            BluetoothGattService serviceLed = gatt.getService(UUID_KILLSWITCH_BLE_SERVICE_LED);
            if (serviceLed != null) {
                BluetoothGattCharacteristic cled = serviceLed.getCharacteristic(UUID_KILLSWITCH_BLE_LED);
                if (cled != null) {
                    cled.setValue(KILLSWITCH_LED_BLINK);
                    writeLedState(cled);
                    Log.v("BLE", "LED set blinking");
                }
            }
        }
    }

    private void ledOn() {
        if (gatt != null ) {
            BluetoothGattService serviceLed = gatt.getService(UUID_KILLSWITCH_BLE_SERVICE_LED);
            if (serviceLed != null) {
                BluetoothGattCharacteristic cled = serviceLed.getCharacteristic(UUID_KILLSWITCH_BLE_LED);
                if (cled != null) {
                    Log.v("BLE", "LED ON");
                    cled.setValue(KILLSWITCH_LED_ON);
                    writeLedState(cled);
                }
            }
        }
    }

    private void ledOff() {
        if (gatt != null ) {
            BluetoothGattService serviceLed = gatt.getService(UUID_KILLSWITCH_BLE_SERVICE_LED);
            if (serviceLed != null) {
                BluetoothGattCharacteristic cled = serviceLed.getCharacteristic(UUID_KILLSWITCH_BLE_LED);
                if (cled != null) {
                    Log.v("BLE", "LED OFF");
                    cled.setValue(KILLSWITCH_LED_OFF);
                    writeLedState(cled);
                }
            }
        }
    }

    private String hex(byte[] value) {
        StringBuilder sb = new StringBuilder(value.length * 2);
        for (byte b : value) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void onPayload(byte[] payload) {
        if (payload == null) {
            return;
        }
        if (payload.length < 2) {
            return;
        }
        handleProtocolVer0(payload);
    }

    private boolean handleProtocolVer0(byte[] payload) {
        // protocol v.0
        if (payload.length == 8) {
            packet = payload;
            Log.v("BLE", " <- " + hex(packet));
            dispatchEvent();
            return true;
        }
        Log.v("BLE", "Unknown payload size for v.0: " + payload.length);
        return false;
    }

    private void dispatchEvent() {
        pool.submit(() -> {
            if (packet[0] == 0x00 && packet[1] == 0x00 && packet[2] == 0x00 && packet[3] == 0x00) {
                Log.v("BLE", "Trigger value received");
                onTriggered();
            }
            onHeartbeat();
        });
    }

    private void onTriggered() {
        ledOn();
        if (state != CircuitState.TRIGGERED && locked) {
            Log.v("BLE", "TRIGGERED");
            state = CircuitState.TRIGGERED;
            Log.v("BLE", "Sending trigger intent");
            context.sendBroadcast(Intents.createKillswitchTriggerIntent(Intents.FLAG_KILLSWITCH_TRIGGER_RED_BUTTON));
        }
    }

    private void onHeartbeat() {
        synchronized (pingLock) {
            pingLock.notifyAll();
        }
    }

    private void onConnect() {
        Log.v("BLE", "START DISCOVERING");
        state = CircuitState.CONNECTED;
        gatt.discoverServices();
    }

    private void onDisconnect() {
        Log.v("BLE", "DISCONNECTED");
        handleInfrastructure();
        disconnectTime = System.currentTimeMillis();
        if (fireOnDisconnect && locked) {
            Log.v("BLE", "Blackout. Reconnecting");
            state = CircuitState.COUNTDOWN;
            reconnect();
        } else {
            Log.v("BLE", "Graceful offline");
            state = CircuitState.OFFLINE;
            KillswitchApplication.getEventBus(context).post(new KillswitchBluetoothGracefulDisconnect());
        }
    }

    private void handleInfrastructure() {
        Log.v("BLE", "Gracefully closing notifications");
        if (servicePing != null && gatt != null) {
            BluetoothGattCharacteristic characteristic = servicePing.getCharacteristic(UUID_KILLSWITCH_BLE_PING);
            gatt.setCharacteristicNotification(characteristic, false);
            gatt.close();
        }
    }

    private void reconnect() {
        pool.submit(() -> {
            final Object countdownLock = new Object();
            while (true) {
                lastReconnectTime = System.currentTimeMillis();
                try {
                    connect();
                } catch (Exception e) {
                    Log.w("BLE", "reconnect failed", e);
                } finally {
                    synchronized (countdownLock) {
                        try {
                            countdownLock.wait(1000);
                        } catch (InterruptedException e) {

                        }
                    }
                }
                if (state == CircuitState.ENGAGED) {
                    Log.v("BLE", "connection re-established");
                    return;
                } else if (state == CircuitState.COUNTDOWN) {
                    Log.v("BLE", "Countdown: " + (lastReconnectTime - disconnectTime));
                    if (lastReconnectTime - disconnectTime > 10000) {
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

    private void writeLedState(BluetoothGattCharacteristic cled) {
        if (gatt == null || servicePing == null){
            return;
        }
        synchronized (gatt) {
            boolean result = false;
            int i = 0;
            do {
                if (i > 10) {
                    Log.w("BLE", "Failed to write LED: " + hex(cled.getValue()));
                    break;
                }
                result = gatt.writeCharacteristic(cled);
                synchronized (servicePing) {
                    try {
                        servicePing.wait(330);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
                i++;
            } while (!result);
        }
    }
}
