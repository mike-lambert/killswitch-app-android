package com.github.mikelambert.killswitch.common.service;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.github.mikelambert.killswitch.common.R;

public class CircuitMonitorService extends Service {
    public static final String CHANNEL_ID = "KillswitchMonitorService";
    public static final String EXTRA_ACTIVITY_CLASS = "X-Activity-Class";
    @Override
    public void onCreate() {
        super.onCreate();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Intent notificationIntent = null;
        try {
            notificationIntent = new Intent(this, Class.forName(intent.getStringExtra(EXTRA_ACTIVITY_CLASS)));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return START_NOT_STICKY;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getResources().getString(R.string.monitor_title))
                .setContentText(getResources().getString(R.string.monitor_text))
                .setSmallIcon(R.drawable.gear)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);
        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Killswitch Monitor Service",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static Intent createServiceStartIntent(Activity target){
        Intent serviceIntent = new Intent(target, CircuitMonitorService.class);
        serviceIntent.putExtra(EXTRA_ACTIVITY_CLASS, target.getClass().getName());
        return serviceIntent;
    }

    public static void startService(Activity target){
        ContextCompat.startForegroundService(target, createServiceStartIntent(target));
    }

    public static void stopService(Activity initiator) {
        initiator.stopService(new Intent(initiator, CircuitMonitorService.class));
    }
}
