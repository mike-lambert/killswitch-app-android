package com.github.mikelambert.killswitch.common;

public interface HardwareCircuit {
    boolean isTarget();
    boolean isConnected();
    void connect();
    void lockOn(boolean fireOnDisconnect);
    void unlock();
    boolean ping();
    CircuitState state();
    void disconnect();
    String getName();
    String getDescriptor();
}
