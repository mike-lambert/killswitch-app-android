package com.github.mikelambert.killswitch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class KillswitchRebootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(action != null) {
            if (action.equals(Intent.ACTION_BOOT_COMPLETED) ) {
                Log.v(this.getClass().getSimpleName(), "Reviving Killswitch ...");
                KillswitchApplication.getInstance(context).getKillswitch().onSettingsUpdated();
            }
        }
    }
}
