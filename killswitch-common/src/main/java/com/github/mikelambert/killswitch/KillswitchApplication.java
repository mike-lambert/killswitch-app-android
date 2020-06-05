package com.github.mikelambert.killswitch;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import static com.github.mikelambert.killswitch.Intents.EVENT_KILLSWITCH_ARMED;
import static com.github.mikelambert.killswitch.Intents.EVENT_KILLSWITCH_DISARMED;
import static com.github.mikelambert.killswitch.Intents.EVENT_KILLSWITCH_TRIGGER;

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

    @Override
    public void onCreate() {
        super.onCreate();
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
}