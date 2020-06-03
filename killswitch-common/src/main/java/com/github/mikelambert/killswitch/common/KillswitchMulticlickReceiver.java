package com.github.mikelambert.killswitch.common;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Objects;

import static com.github.mikelambert.killswitch.common.Intents.FLAG_KILLSWITCH_TRIGGER_RED_BUTTON;

public class KillswitchMulticlickReceiver extends BroadcastReceiver {
    private int count;
    private long last;
    private static long WINDOW = 2000;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(this.getClass().getSimpleName(), Objects.requireNonNull(intent.getAction()));
        long now = System.currentTimeMillis();
        if (last <= 0 || now - last >= WINDOW){
            count = 1;
            last = now;
        } else if (last > 0 && now - last < WINDOW){
            count++;
            last = now;
        }
        Log.v(this.getClass().getSimpleName(), intent.getAction() + ": " + count + "; last: " + (now - last));
        if (count >= 5){
            count = 0;
            last = 0;
            Log.v(this.getClass().getSimpleName(), "Sending trigger broadcast");
            context.sendBroadcast(Intents.createKillswitchTriggerIntent(FLAG_KILLSWITCH_TRIGGER_RED_BUTTON));
        }
    }
}
