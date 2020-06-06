package com.github.mikelambert.killswitch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class KillswitchRebootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(action != null) {
            if (action.equals(Intent.ACTION_BOOT_COMPLETED) ) {
                KillswitchApplication.getInstance(context).getKillswitch().onStarted();
                // TODO: recover state
            }
        }
    }
}
