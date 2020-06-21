package com.github.mikelambert.killswitch.io.usb;

import android.content.Context;
import android.util.Log;

import com.github.mikelambert.killswitch.common.CircuitFactory;
import com.github.mikelambert.killswitch.common.CircuitFactoryRegistry;
import com.github.mikelambert.killswitch.common.HardwareCircuit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KillswitchUsbCircuitFactory implements CircuitFactory {
    private static KillswitchUsbCircuitFactory INSTANCE;

    private final Map<String, HardwareCircuit> circuits;
    private KillswitchUsbCircuitFactory(){
        circuits = new ConcurrentHashMap<>();
    }

    @Override
    public boolean isDescriptorSupported(String descriptor) {
        return descriptor.startsWith("usb://");
    }

    @Override
    public HardwareCircuit get(Context context, String descriptor) {
        synchronized (circuits){
            HardwareCircuit circuit = circuits.get(descriptor);
            if (circuit == null){
                Log.v("USBCF", "Instantiating circuit for " + descriptor);
                circuit = new KillswitchUsbCircuit(context, descriptor);
                circuits.put(descriptor, circuit);
            }
            return circuit;
        }
    }

    static {
        Log.v("USBCF", "Register USB circuit factory");
        INSTANCE = new KillswitchUsbCircuitFactory();
        CircuitFactoryRegistry.registerFactoryInstance(INSTANCE);
    }
}
