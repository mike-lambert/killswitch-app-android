package com.github.mikelambert.killswitch.common;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import static android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_BIOMETRICS;
import static android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_FEATURES_NONE;
import static android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS;
import static android.app.admin.DevicePolicyManager.WIPE_SILENTLY;
import static com.github.mikelambert.killswitch.common.Intents.TRIGGER_ACTION_REBOOT;
import static com.github.mikelambert.killswitch.common.Intents.TRIGGER_ACTION_WIPE;

public class KillswitchDeviceAdministratorImpl implements KillswitchDeviceAdministrator {
    private final Context context;
    private final DevicePolicyManager devicePolicyManager;
    private final ComponentName adminComponentName;
    private String action;
    private boolean armed;

    public KillswitchDeviceAdministratorImpl(Context context) {
        this.context = context;
        // Prepare to work with the DPM
        devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponentName = new ComponentName(context, KillswitchAdminReceiver.class);
    }

    @Override
    public void onTrigger(int flags) {
        if (isArmed() && isAdminActive() && TRIGGER_ACTION_WIPE.equals(action)) {
            devicePolicyManager.wipeData(WIPE_SILENTLY);
        } else if (isArmed() && isAdminActive() && TRIGGER_ACTION_REBOOT.equals(action)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                devicePolicyManager.reboot(adminComponentName);
            } else {
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
            disableKeyGuardFeatures();
            devicePolicyManager.lockNow();
        }
    }

    @Override
    public void onDisarmed() {
        if (isAdminActive()) {
            armed = false;
            enableKeyguardFeatures();
        }
    }

    @Override
    public void onSettingsUpdated() {

    }

    @Override
    public void onEnabled() {
        disableKeyGuardFeatures();
    }

    @Override
    public void onDisabled() {

    }

    @Override
    public void onStarted() {
        onArmed();
    }

    @Override
    public boolean isArmed() {
        return armed;
    }

    @Override
    public boolean isEnabled() {
        return isAdminActive();
    }

    @Override
    public void disable() {
        if (!armed && isAdminActive()){
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

}
