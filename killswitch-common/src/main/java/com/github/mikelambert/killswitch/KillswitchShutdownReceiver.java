package com.github.mikelambert.killswitch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.github.mikelambert.killswitch.common.HardwareCircuit;

public class KillswitchShutdownReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            HardwareCircuit circuit = KillswitchApplication.getInstance(context).getKillswitch().getBoundCircuit();
            if (circuit != null && circuit.getDescriptor() != null){
                Log.v(this.getClass().getSimpleName(), action + ": Gracefully disconnecting circuit " + circuit.getDescriptor());
                circuit.disconnect();
            }
        }
    }
}
