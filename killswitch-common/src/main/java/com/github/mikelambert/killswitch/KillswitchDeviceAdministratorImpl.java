package com.github.mikelambert.killswitch;

import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.util.Log;

import static android.app.admin.DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY;
import static android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_BIOMETRICS;
import static android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_FEATURES_NONE;
import static android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS;
import static android.app.admin.DevicePolicyManager.LEAVE_ALL_SYSTEM_APPS_ENABLED;
import static android.app.admin.DevicePolicyManager.SKIP_SETUP_WIZARD;
import static android.app.admin.DevicePolicyManager.WIPE_EXTERNAL_STORAGE;
import static android.app.admin.DevicePolicyManager.WIPE_SILENTLY;
import static com.github.mikelambert.killswitch.Intents.TRIGGER_ACTION_REBOOT;
import static com.github.mikelambert.killswitch.Intents.TRIGGER_ACTION_WIPE;

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
        action = TRIGGER_ACTION_WIPE;
    }

    @Override
    public void onTrigger(int flags) {
        if (isArmed() && TRIGGER_ACTION_WIPE.equals(action)) {
            Log.v(this.getClass().getSimpleName(), "WIPING DEVICE");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                devicePolicyManager.wipeData((wipeSd ? WIPE_EXTERNAL_STORAGE : 0), "Killswitch engaged"/* | WIPE_SILENTLY*/);
            } else {
                devicePolicyManager.wipeData(0);
            }
        }
    }

    @Override
    public void onArmed() {
        if (isAdminActive()) {
            armed = true;
            Log.v(this.getClass().getSimpleName(), "ARMED: securing keyguard");
            //disableKeyGuardFeatures();
            forceKeyguard();
            Log.v(this.getClass().getSimpleName(), "ARMED: locking screen");
            devicePolicyManager.lockNow();
        }
    }

    @Override
    public void onDisarmed() {
        if (isArmed()) {
            armed = false;
            Log.v(this.getClass().getSimpleName(), "DISARMED: relaxing keyguard");
            //enableKeyguardFeatures();
        }
    }

    @Override
    public void onSettingsUpdated() {
        Log.v(this.getClass().getSimpleName(), "SETTINGS UPDATED");
    }

    @Override
    public void onEnabled() {
        if (isAdminActive()){
            Log.v(this.getClass().getSimpleName(), "ENABLED: ensuring device encryption");
            requireStorageEncryption();
            forceKeyguard();
        }
    }

    @Override
    public void onDisabled() {
        Log.v(this.getClass().getSimpleName(), "DISABLED");
    }

    @Override
    public void onStarted() {
        Log.v(this.getClass().getSimpleName(), "STARTUP");
        onEnabled();
        // TODO: check state
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

    private void forceKeyguard() {
        if (isAdminActive()) {
            if (!getKeyguardManager().isDeviceSecure()) {
                Intent k = getKeyguardManager().createConfirmDeviceCredentialIntent("Killswitch", "Securing device disarming");
                if (k == null){
                    // TODO: toast
                }
            }
        }
    }

    private KeyguardManager getKeyguardManager() {
        return (KeyguardManager)context.getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
    }
}
