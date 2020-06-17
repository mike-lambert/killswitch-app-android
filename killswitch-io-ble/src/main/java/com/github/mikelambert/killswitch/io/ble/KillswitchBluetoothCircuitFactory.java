package com.github.mikelambert.killswitch.io.ble;

import android.content.Context;
import android.util.Log;

import com.github.mikelambert.killswitch.common.CircuitFactory;
import com.github.mikelambert.killswitch.common.CircuitFactoryRegistry;
import com.github.mikelambert.killswitch.common.HardwareCircuit;

public class KillswitchBluetoothCircuitFactory implements CircuitFactory {
    @Override
    public boolean isDescriptorSupported(String descriptor) {
        return true; // TODO: scheme
    }

    @Override
    public HardwareCircuit get(Context context, String descriptor) {
        return new KillswitchBluetoothCircuit(context, descriptor);
    }

    static {
        Log.v(KillswitchBluetoothCircuitFactory.class.getSimpleName(), "Register BLE circuit factory");
        CircuitFactoryRegistry.registerFactoryInstance(new KillswitchBluetoothCircuitFactory());
    }
}
