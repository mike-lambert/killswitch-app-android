package com.github.mikelambert.killswitch.io.ble;

import android.content.Context;
import android.util.Log;

import com.github.mikelambert.killswitch.common.CircuitFactory;
import com.github.mikelambert.killswitch.common.CircuitFactoryRegistry;
import com.github.mikelambert.killswitch.common.HardwareCircuit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KillswitchBluetoothCircuitFactory implements CircuitFactory {
    private static KillswitchBluetoothCircuitFactory INSTANCE;

    private final Map<String, HardwareCircuit> circuits;

    private KillswitchBluetoothCircuitFactory() {
        circuits = new ConcurrentHashMap<>();
    }

    @Override
    public boolean isDescriptorSupported(String descriptor) {
        return true; // TODO: scheme
    }

    @Override
    public HardwareCircuit get(Context context, String descriptor) {
        synchronized (circuits) {
            HardwareCircuit circuit = circuits.get(descriptor);
            if (circuit == null) {
                Log.v("BLECF", "Instantianting circuit for " + descriptor);
                circuit = new KillswitchBluetoothCircuit(context, descriptor);
                circuits.put(descriptor, circuit);
            }
            return circuit;
        }
    }

    static {
        Log.v(KillswitchBluetoothCircuitFactory.class.getSimpleName(), "Register BLE circuit factory");
        INSTANCE = new KillswitchBluetoothCircuitFactory();
        CircuitFactoryRegistry.registerFactoryInstance(INSTANCE);
    }
}
