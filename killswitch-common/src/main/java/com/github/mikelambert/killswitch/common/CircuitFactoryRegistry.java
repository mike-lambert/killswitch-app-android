package com.github.mikelambert.killswitch.common;

import android.util.Log;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CircuitFactoryRegistry {
    private static final List<CircuitFactory> factories = new CopyOnWriteArrayList<>();

    public static void registerFactoryInstance(CircuitFactory factory) {
        if (factory == null){ return; }
        synchronized (factories){
            for(CircuitFactory f : factories){
                if (f.getClass().equals(factory.getClass())) {
                    return;
                }
            }
            Log.v("CircuitFactory", "Registered factory " + factory.getClass().getName());
            factories.add(factory);
        }
    }

    public static CircuitFactory getByDescriptor(String descriptor){
        for(CircuitFactory f : factories){
            if (f.isDescriptorSupported(descriptor)){
                Log.v("CircuitFactory", f.getClass().getName() + " supports descriptor " + descriptor);
                return f;
            }
        }
        throw new IllegalArgumentException("Descriptor " + descriptor + " not supported by any known factory");
    }
}
