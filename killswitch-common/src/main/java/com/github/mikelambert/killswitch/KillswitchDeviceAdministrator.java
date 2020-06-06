package com.github.mikelambert.killswitch;

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
}
