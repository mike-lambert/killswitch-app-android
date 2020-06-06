package com.github.mikelambert.killswitch;

import android.content.ComponentName;
import android.content.Intent;

public interface KillswitchDeviceAdministrator {
    void onTrigger(int flags);
    void onArmed();
    void onDisarmed();
    void onSettingsUpdated();
    void onEnabled();
    void onDisabled();
    void onStarted();
    boolean isArmed();
    boolean isEnabled();
    void disable();
    Intent createDeviceAdminRequest(String explanation);
    ComponentName getAdminComponentName();
}
