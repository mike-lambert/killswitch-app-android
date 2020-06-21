package com.github.mikelambert.killswitch;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.github.mikelambert.killswitch.common.CircuitFactoryRegistry;
import com.github.mikelambert.killswitch.common.HardwareCircuit;
import com.github.mikelambert.killswitch.common.KillswitchDeviceAdministrator;
import com.github.mikelambert.killswitch.common.service.CircuitMonitorService;
import com.github.mikelambert.killswitch.event.KillswitchAdminStatus;
import com.github.mikelambert.killswitch.event.KillswitchArmedStatus;
import com.github.mikelambert.killswitch.event.KillswitchBluetoothGracefulDisconnect;
import com.github.mikelambert.killswitch.persistence.PersistentState;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import static android.app.admin.DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY;
import static android.app.admin.DevicePolicyManager.WIPE_EXTERNAL_STORAGE;
import static com.github.mikelambert.killswitch.common.Intents.TRIGGER_ACTION_WIPE;
import static com.github.mikelambert.killswitch.common.service.CircuitMonitorService.EXTRA_ACTIVITY_CLASS;

public class KillswitchDeviceAdministratorImpl implements KillswitchDeviceAdministrator {
    public static final String KILLSWITCH_PREFERENCE_ID = "KILLSWITCH_PREFERENCES";
    public static final String STATE_FIELD_ARMED = "isArmed";
    public static final String STATE_FIELD_WIPE_SD = "isWipeSdCard";
    public static final String STATE_FIELD_TRIGGER_MULTICLICK = "triggerUseMulticlick";
    public static final String STATE_FIELD_MULTICLICK_COUNT = "multiclickCounter";
    public static final String STATE_FIELD_DEVICE = "device";

    private final Context context;
    private final DevicePolicyManager devicePolicyManager;
    private final ComponentName adminComponentName;
    private String action;
    private EventBus eventBus;
    private PersistentState state;
    private SharedPreferences preferences;
    private HardwareCircuit circuit;

    public KillswitchDeviceAdministratorImpl(Context context) {
        this.context = context;
        devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponentName = new ComponentName(context, KillswitchAdminReceiver.class);
        action = TRIGGER_ACTION_WIPE;
        eventBus = KillswitchApplication.getEventBus(context);
        preferences = this.context.getSharedPreferences(KILLSWITCH_PREFERENCE_ID, Context.MODE_PRIVATE);
        KillswitchApplication.getEventBus(context).register(this);
        refreshState();
        onStarted();
    }

    @Override
    public void onTrigger(int flags) {
        if (isArmed() && TRIGGER_ACTION_WIPE.equals(action)) {
            Log.v(this.getClass().getSimpleName(), "WIPING DEVICE");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                devicePolicyManager.wipeData((state.isWipeSdCard() ? WIPE_EXTERNAL_STORAGE : 0), "Killswitch engaged");
            } else {
                devicePolicyManager.wipeData((state.isWipeSdCard() ? WIPE_EXTERNAL_STORAGE : 0));
            }
        }
    }

    @Override
    public void onArmed() {
        if (isAdminActive()) {
            state.setArmed(true);
            saveState();
            Log.v(this.getClass().getSimpleName(), "ARMED: securing keyguard");
            Log.v(this.getClass().getSimpleName(), "ARMED: locking screen");
            if (circuit != null) {
                if (!circuit.isConnected()) {
                    Log.v(this.getClass().getSimpleName(), "Connecting circuit");
                    circuit.connect();
                }
                Log.v(this.getClass().getSimpleName(), "Locking on circuit");
                circuit.lockOn(true); // TODO: settings
            }
            eventBus.post(new KillswitchArmedStatus(true));
            devicePolicyManager.lockNow();
        }
    }

    @Override
    public void onDisarmed() {
        if (isArmed()) {
            state.setArmed(false);
            saveState();
            if (circuit != null) {
                Log.v(this.getClass().getSimpleName(), "Unlocking circuit");
                circuit.unlock();
            }
            Log.v(this.getClass().getSimpleName(), "DISARMED: relaxing keyguard");
            eventBus.post(new KillswitchArmedStatus(false));
        }
    }

    @Override
    public void onSettingsUpdated(PersistentState state) {
        if (state != null) {
            this.state = state;
            saveState();
        }
        Log.v(this.getClass().getSimpleName(), "SETTINGS UPDATED");
        refreshState();
    }

    @Override
    public void onEnabled() {
        if (isAdminActive()) {
            Log.v(this.getClass().getSimpleName(), "ENABLED: ensuring device encryption");
            requireStorageEncryption();
            eventBus.post(new KillswitchAdminStatus(true));
        }
    }

    @Override
    public void onDisabled() {
        if (!isAdminActive()) {
            Log.v(this.getClass().getSimpleName(), "DISABLED");
        }
        eventBus.post(new KillswitchAdminStatus(false));
    }

    @Override
    public void onStarted() {
        Log.v(this.getClass().getSimpleName(), "STARTUP");
        processAdminState();
        rebindDevice();
        processArmedState();
    }

    private void rebindDevice() {
        if (!state.getBoundedDevice().trim().isEmpty()) {
            Log.v(this.getClass().getSimpleName(), "Re-acquiring device " + state.getBoundedDevice());
            if (this.circuit == null) {
                this.circuit = CircuitFactoryRegistry.getByDescriptor(state.getBoundedDevice()).get(context, state.getBoundedDevice());
                Log.v(this.getClass().getSimpleName(), "Circuit: " + circuit);
                saveState();
                if (isArmed()) {
                    Log.v(this.getClass().getSimpleName(), "Locking on circuit");
                    circuit.lockOn(true);
                }
                if (!circuit.isConnected()) {
                    Log.v(this.getClass().getSimpleName(), "Connecting circuit");
                    circuit.connect();
                }
                Log.v(this.getClass().getSimpleName(), "Starting monitor");
                // start monitor
                startMonitor();
            }
        }
    }

    private void startMonitor() {
        Intent serviceIntent = new Intent(context, CircuitMonitorService.class);
        serviceIntent.putExtra(EXTRA_ACTIVITY_CLASS, "com.github.mikelambert.killswitch.MainActivity"); // dirty hack
        ContextCompat.startForegroundService(context, serviceIntent);
    }

    private void processArmedState() {
        if (state.isArmed()) {
            onArmed();
        } else {
            onDisarmed();
        }
    }

    private void processAdminState() {
        if (isAdminActive()) {
            onEnabled();
        } else {
            onDisabled();
        }
    }

    @Override
    public boolean isArmed() {
        return state.isArmed() && isAdminActive();
    }

    @Override
    public boolean isEnabled() {
        return isAdminActive();
    }

    @Override
    public void disable() {
        if (!isArmed()) {
            Log.v(this.getClass().getSimpleName(), "DISABLING");
            devicePolicyManager.removeActiveAdmin(adminComponentName);
            onDisabled();
        }
    }

    @Override
    public ComponentName getAdminComponentName() {
        return adminComponentName;
    }

    @Override
    public PersistentState currentState() {
        return PersistentState.cloneState(state);
    }

    @Override
    public HardwareCircuit getBoundCircuit() {
        return circuit;
    }

    @Override
    public void bindCircuit(HardwareCircuit circuit, Activity initiator) {
        if (this.circuit == null) {
            Log.v(this.getClass().getSimpleName(), "Binding circuit " + circuit.getDescriptor());
            this.circuit = circuit;
        }
        if (!this.circuit.isConnected()) {
            Log.v(this.getClass().getSimpleName(), "Connecting circuit");
            this.circuit.connect();
        }
        if (isArmed()) {
            Log.v(this.getClass().getSimpleName(), "Locking on circuit");
            this.circuit.lockOn(true);
        }
        Log.v(this.getClass().getSimpleName(), "Starting monitor");
        CircuitMonitorService.startService(initiator);

        saveState();
    }

    @Override
    public void unbindCircuit(Activity initiator) {
        if (!isArmed() && circuit != null) {
            Log.v(this.getClass().getSimpleName(), "Stopping monitor");
            CircuitMonitorService.stopService(initiator);
            Log.v(this.getClass().getSimpleName(), "Unlocking circuit");
            circuit.unlock();
            circuit = null;
        }
        saveState();
    }

    @Subscribe
    public void onGracefulDisconnect(KillswitchBluetoothGracefulDisconnect event) {
        Log.v(this.getClass().getSimpleName(), "Graceful disconnect");
        if (!isArmed() && circuit != null) {
            circuit.unlock();
            circuit = null;
        }
        saveState();
    }

    private boolean isAdminActive() {
        return devicePolicyManager.isAdminActive(adminComponentName);
    }

    private void requireStorageEncryption() {
        if (isAdminActive()) {
            int ses = devicePolicyManager.getStorageEncryptionStatus();
            Log.v(this.getClass().getSimpleName(), "Encryption status: " + ses);
            switch (ses) {
                case ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY:
                    Log.v(this.getClass().getSimpleName(), "Forcing storage encryption");
                    devicePolicyManager.setStorageEncryption(adminComponentName, true);
                    break;
                default:
                    return;
            }
        }
    }

    private synchronized void refreshState() {
        boolean armed = preferences.getBoolean(STATE_FIELD_ARMED, false);
        boolean wipeSd = preferences.getBoolean(STATE_FIELD_WIPE_SD, false);
        boolean multiclick = preferences.getBoolean(STATE_FIELD_TRIGGER_MULTICLICK, true);
        int counter = preferences.getInt(STATE_FIELD_MULTICLICK_COUNT, 5);
        String descriptor = preferences.getString(STATE_FIELD_DEVICE, "");

        state = new PersistentState();
        state.setArmed(armed);
        state.setWipeSdCard(wipeSd);
        state.setActivateByMulticlick(multiclick);
        state.setClicksCount(counter);
        state.setBoundedDevice(descriptor);
    }

    private synchronized void saveState() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(STATE_FIELD_ARMED, state.isArmed());
        editor.putBoolean(STATE_FIELD_WIPE_SD, state.isWipeSdCard());
        editor.putBoolean(STATE_FIELD_TRIGGER_MULTICLICK, state.isActivateByMulticlick());
        editor.putInt(STATE_FIELD_MULTICLICK_COUNT, state.getClicksCount());
        if (circuit != null && circuit.getDescriptor() != null) {
            editor.putString(STATE_FIELD_DEVICE, circuit.getDescriptor());
        } else {
            editor.putString(STATE_FIELD_DEVICE, "");
        }
        editor.apply();
    }
}
