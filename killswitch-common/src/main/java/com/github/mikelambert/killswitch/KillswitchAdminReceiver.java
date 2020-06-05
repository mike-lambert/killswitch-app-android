package com.github.mikelambert.killswitch;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

public class KillswitchAdminReceiver extends DeviceAdminReceiver {
    @Override
    public void onEnabled(@NonNull Context context, @NonNull Intent intent) {
        super.onEnabled(context, intent);
        KillswitchApplication.getInstance(context).getKillswitch().onEnabled();
    }

    @Override
    public void onDisabled(@NonNull Context context, @NonNull Intent intent) {
        super.onDisabled(context, intent);
        KillswitchApplication.getInstance(context).getKillswitch().onDisabled();
    }
}
