package com.github.mikelambert.killswitch.common;

import android.app.Activity;
import android.content.ComponentName;

import com.github.mikelambert.killswitch.persistence.PersistentState;

public interface KillswitchDeviceAdministrator {
    void onTrigger(int flags);
    void onArmed();
    void onDisarmed();
    void onSettingsUpdated(PersistentState state);
    void onEnabled();
    void onDisabled();
    void onStarted();
    boolean isArmed();
    boolean isEnabled();
    void disable();
    ComponentName getAdminComponentName();
    PersistentState currentState();
    HardwareCircuit getBoundCircuit();
    void bindCircuit(HardwareCircuit circuit, Activity initiator);
    void unbindCircuit(Activity initiator);
}
