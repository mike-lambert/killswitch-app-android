package com.github.mikelambert.killswitch.io.usb;

import android.content.Context;

import com.github.mikelambert.killswitch.common.CircuitState;
import com.github.mikelambert.killswitch.common.HardwareCircuit;

public class KillswitchUsbCircuit implements HardwareCircuit {
    public KillswitchUsbCircuit(Context context, String descriptor) {
    }

    @Override
    public boolean isTarget() {
        return false;
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public void connect() {

    }

    @Override
    public void lockOn(boolean fireOnDisconnect) {

    }

    @Override
    public void unlock() {

    }

    @Override
    public boolean ping() {
        return false;
    }

    @Override
    public CircuitState state() {
        return null;
    }

    @Override
    public void disconnect() {

    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getDescriptor() {
        return null;
    }
}
