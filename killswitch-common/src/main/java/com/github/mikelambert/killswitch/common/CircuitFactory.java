package com.github.mikelambert.killswitch.common;

import android.content.Context;

public interface CircuitFactory {
    boolean isDescriptorSupported(String descriptor);
    HardwareCircuit get(Context context, String descriptor);
}
