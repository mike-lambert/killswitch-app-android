package com.github.mikelambert.killswitch;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.github.mikelambert.killswitch.common.CircuitFactory;
import com.github.mikelambert.killswitch.common.KillswitchDeviceAdministrator;

import org.greenrobot.eventbus.EventBus;

import static com.github.mikelambert.killswitch.common.Intents.EVENT_KILLSWITCH_ARMED;
import static com.github.mikelambert.killswitch.common.Intents.EVENT_KILLSWITCH_DISARMED;
import static com.github.mikelambert.killswitch.common.Intents.EVENT_KILLSWITCH_TRIGGER;

public class KillswitchApplication extends Application {
    private static class KillswitchEventsReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.v(this.getClass().getSimpleName(), "KILLSWITCH EVENT: " + action);
            if (EVENT_KILLSWITCH_ARMED.equals(action)) {
                KillswitchApplication.getInstance(context).getKillswitch().onArmed();
            }
            if (EVENT_KILLSWITCH_DISARMED.equals(action)){
                KillswitchApplication.getInstance(context).getKillswitch().onDisarmed();
            }
            if (EVENT_KILLSWITCH_TRIGGER.equals(action)) {
                KillswitchApplication.getInstance(context).getKillswitch().onTrigger(intent.getFlags());
            }
        }
    }

    private KillswitchDeviceAdministrator killswitchDeviceAdministrator;
    private KillswitchEventsReceiver eventsReceiver;
    private EventBus eventBus;

    @Override
    public void onCreate() {
        super.onCreate();
        eventBus = EventBus.getDefault();
        loadFactories();
        killswitchDeviceAdministrator = new KillswitchDeviceAdministratorImpl(this);
        killswitchDeviceAdministrator.onStarted();
        registerEventsReceiver();
        registerReceiver(new KillswitchMulticlickReceiver(), new IntentFilter(Intent.ACTION_SCREEN_ON));
        registerReceiver(new KillswitchMulticlickReceiver(), new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }

    @Override
    public void onTerminate() {
        unregisterReceiver(eventsReceiver);
        super.onTerminate();
    }

    public KillswitchDeviceAdministrator getKillswitch() {
        return killswitchDeviceAdministrator;
    }

    public static KillswitchApplication getInstance(Context context){
        return (KillswitchApplication)context.getApplicationContext();
    }

    public static EventBus getEventBus(Context context) {
        return getInstance(context).eventBus;
    }

    private void registerEventsReceiver() {
        eventsReceiver = new KillswitchEventsReceiver();
        final IntentFilter filter = new IntentFilter();
        filter.addAction(EVENT_KILLSWITCH_TRIGGER);
        filter.addAction(EVENT_KILLSWITCH_ARMED);
        filter.addAction(EVENT_KILLSWITCH_DISARMED);
        registerReceiver(
            eventsReceiver,
            filter
        );
    }

    private void loadFactories() {
        try {
            Class<CircuitFactory> bfc = (Class<CircuitFactory>) Class.forName("com.github.mikelambert.killswitch.io.ble.KillswitchBluetoothCircuitFactory");
            Log.v("App", "factory loaded");
        } catch (ClassNotFoundException e) {
            Log.w("App", "Factory class not found", e);
        }
    }
}
