package com.github.mikelambert.killswitch.common.service;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.github.mikelambert.killswitch.KillswitchApplication;
import com.github.mikelambert.killswitch.common.HardwareCircuit;
import com.github.mikelambert.killswitch.common.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CircuitMonitorService extends Service {
    public static final String CHANNEL_ID = "KillswitchMonitorService";
    public static final String EXTRA_ACTIVITY_CLASS = "X-Activity-Class";
    public static final int DEFAULT_NOTIFICATION_ID = 1;

    private ExecutorService pool;
    private boolean running;
    private Object lock;
    private Class targetClass;

    @Override
    public void onCreate() {
        super.onCreate();
        pool = Executors.newFixedThreadPool(3);
        lock = new Object();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        running = true;
        createNotificationChannel();
        Intent notificationIntent = null;
        try {
            targetClass = Class.forName(intent.getStringExtra(EXTRA_ACTIVITY_CLASS));
            notificationIntent = new Intent(this, targetClass);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return START_STICKY;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getResources().getString(R.string.monitor_title))
                .setContentText(getResources().getString(R.string.monitor_text))
                .setSmallIcon(R.drawable.gear)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setWhen(System.currentTimeMillis())
                .build();
        startForeground(DEFAULT_NOTIFICATION_ID, notification);
        scheduleMonitorThread();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        running = false;
        pool.shutdown();
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.cancel(DEFAULT_NOTIFICATION_ID);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.deleteNotificationChannel(CHANNEL_ID);
        }
        stopSelf();
        super.onDestroy();
    }

    private void scheduleMonitorThread() {
        pool.submit(() -> {
            while (running){
                HardwareCircuit bounded = KillswitchApplication.getInstance(this).getKillswitch().getBoundCircuit();
                if (bounded != null && bounded.isConnected()){
                    if (bounded.isTarget()){
                        Log.v("Monitor", "ping circuit");
                        if (!bounded.ping()) {
                            Log.w("Monitor", "PING FAILED");
                        } else {
                            Log.v("Monitor", "ping ok");
                        }
                    }
                } else {
                    Log.v("Monitor", "device disconnected. Exiting");
                    this.stopService(new Intent(this, CircuitMonitorService.class));
                    break;
                }

                synchronized (lock){
                    try {
                        lock.wait(2000);
                    } catch (InterruptedException e) {

                    }
                }
            }
            Log.v("Monitor", "Shutting down");
        });
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

    public static Intent createServiceStartIntent(Activity target) {
        Intent serviceIntent = new Intent(target, CircuitMonitorService.class);
        serviceIntent.putExtra(EXTRA_ACTIVITY_CLASS, target.getClass().getName());
        return serviceIntent;
    }

    public static void startService(Activity target) {
        ContextCompat.startForegroundService(target, createServiceStartIntent(target));
    }

    public static void stopService(Activity initiator) {
        initiator.stopService(new Intent(initiator, CircuitMonitorService.class));
    }
}
