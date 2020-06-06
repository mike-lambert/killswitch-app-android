package com.github.mikelambert.killswitch.common;

public interface HardwareCircuit {
    boolean isTarget();
    void lockOn(boolean fireOnDisconnect);
    void unlock();
    void ping();
    CircuitState state();
}
