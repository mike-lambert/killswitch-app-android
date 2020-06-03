package com.github.mikelambert.killswitch.common;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import static android.app.admin.DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY;
import static android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_BIOMETRICS;
import static android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_FEATURES_NONE;
import static android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS;
import static android.app.admin.DevicePolicyManager.WIPE_EXTERNAL_STORAGE;
import static android.app.admin.DevicePolicyManager.WIPE_SILENTLY;
import static com.github.mikelambert.killswitch.common.Intents.TRIGGER_ACTION_REBOOT;
import static com.github.mikelambert.killswitch.common.Intents.TRIGGER_ACTION_WIPE;

public class KillswitchDeviceAdministratorImpl implements KillswitchDeviceAdministrator {
    private final Context context;
    private final DevicePolicyManager devicePolicyManager;
    private final ComponentName adminComponentName;
    private String action;
    private boolean armed;
    private boolean wipeSd;

    public KillswitchDeviceAdministratorImpl(Context context) {
        this.context = context;
        devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponentName = new ComponentName(context, KillswitchAdminReceiver.class);
    }

    @Override
    public void onTrigger(int flags) {
        if (isArmed() && TRIGGER_ACTION_WIPE.equals(action)) {
            Log.v(this.getClass().getSimpleName(), "WIPING DEVICE");
            devicePolicyManager.wipeData((wipeSd ? WIPE_EXTERNAL_STORAGE : 0) | WIPE_SILENTLY);
        } else if (isArmed() && TRIGGER_ACTION_REBOOT.equals(action)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Log.v(this.getClass().getSimpleName(), "REBOOTING: DPM");
                devicePolicyManager.reboot(adminComponentName);
            } else {
                Log.v(this.getClass().getSimpleName(), "REBOOTING: PS");
                Intents.reboot(context, false);
            }
        } else {
            // not matched
        }
    }

    @Override
    public void onArmed() {
        if (isAdminActive()) {
            armed = true;
            Log.v(this.getClass().getSimpleName(), "ARMED: securing keyguard");
            disableKeyGuardFeatures();
            Log.v(this.getClass().getSimpleName(), "ARMED: locking screen");
            devicePolicyManager.lockNow();
        }
    }

    @Override
    public void onDisarmed() {
        if (isArmed()) {
            armed = false;
            Log.v(this.getClass().getSimpleName(), "DISARMED: relaxing keyguard");
            enableKeyguardFeatures();
        }
    }

    @Override
    public void onSettingsUpdated() {
        Log.v(this.getClass().getSimpleName(), "SETTINGS UPDATED");
    }

    @Override
    public void onEnabled() {
        Log.v(this.getClass().getSimpleName(), "ENABLED: ensuring device encryption");
        requireStorageEncryption();
    }

    @Override
    public void onDisabled() {
        Log.v(this.getClass().getSimpleName(), "DISABLED");
    }

    @Override
    public void onStarted() {
        Log.v(this.getClass().getSimpleName(), "STARTUP");
        onEnabled();
        onArmed();
    }

    @Override
    public boolean isArmed() {
        return armed && isAdminActive();
    }

    @Override
    public boolean isEnabled() {
        return isAdminActive();
    }

    @Override
    public void disable() {
        if (!isArmed()){
            Log.v(this.getClass().getSimpleName(), "DISABLING");
            devicePolicyManager.removeActiveAdmin(adminComponentName);
        }
    }

    @Override
    public Intent createDeviceAdminRequest(String explanation) {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponentName);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, explanation);
        return intent;
    }

    private void enableKeyguardFeatures() {
        devicePolicyManager.setKeyguardDisabledFeatures(adminComponentName, KEYGUARD_DISABLE_FEATURES_NONE);
    }

    private void disableKeyGuardFeatures() {
        devicePolicyManager.setKeyguardDisabledFeatures(adminComponentName, KEYGUARD_DISABLE_BIOMETRICS | KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS);
    }

    private boolean isAdminActive() {
        return devicePolicyManager.isAdminActive(adminComponentName);
    }

    private void requireStorageEncryption() {
        if (isAdminActive()){
            int ses = devicePolicyManager.getStorageEncryptionStatus();
            Log.v(this.getClass().getSimpleName(), "Encryption status: " + ses);
            switch(ses){
                case ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY:
                    Log.v(this.getClass().getSimpleName(), "Forcing storage encryption");
                    devicePolicyManager.setStorageEncryption(adminComponentName, true);
                    break;
                default:
                    return;
            }
        }
    }
}
