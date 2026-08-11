package com.sanda.datasaver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.NotificationCompat;

/**
 * DataSaverService — Persistent background
 * service. Shows the persistent notification
 * with quick toggle. Keeps the app alive in
 * the background. Also checks data usage
 * alerts periodically.
 */
public class DataSaverService extends Service {

    private PrefsManager          prefs;
    private DataSaverManager      manager;
    private DataUsageAlertManager alertManager;
    private Handler               handler;
    private Runnable              usageChecker;

    // Check usage every 15 minutes
    private static final long CHECK_INTERVAL
            = 15 * 60 * 1000L;

    // ─────────────────────────────────────
    @Override
    public void onCreate() {
        super.onCreate();
        prefs        =
                new PrefsManager(this);
        manager      =
                new DataSaverManager(this);
        alertManager =
                new DataUsageAlertManager(this);
        handler      =
                new Handler(
                        Looper.getMainLooper());

        // Set up periodic usage checker
        usageChecker = new Runnable() {
            @Override
            public void run() {
                checkDataUsage();
                handler.postDelayed(
                        this, CHECK_INTERVAL);
            }
        };
    }

    // ─────────────────────────────────────
    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId) {

        // Handle toggle from notification
        if (intent != null
                && Constants.ACTION_TOGGLE
                .equals(
                        intent.getAction())) {
            if (manager.isDataSaverOn()) {
                manager.deactivate();
            } else {
                manager.activate();
            }
            updateNotification();
        }

        // Start as foreground service
        startForeground(
                Constants.NOTIF_ID_SERVICE,
                buildNotification());

        // Start usage checking
        handler.removeCallbacks(
                usageChecker);
        handler.postDelayed(
                usageChecker, CHECK_INTERVAL);

        return START_STICKY;
    }

    // ─────────────────────────────────────
    // CHECK DATA USAGE ALERTS
    // ─────────────────────────────────────
    private void checkDataUsage() {
        new Thread(() -> {
            try {
                alertManager.checkAndAlert();
            } catch (Exception e) {
                // Silent fail
            }
        }).start();
    }

    // ─────────────────────────────────────
    @Override
    public void onDestroy() {
        // Stop usage checker when
        // service is destroyed
        if (handler != null
                && usageChecker != null) {
            handler.removeCallbacks(
                    usageChecker);
        }
        super.onDestroy();
    }

    // ─────────────────────────────────────
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // ─────────────────────────────────────
    // BUILD PERSISTENT NOTIFICATION
    // ─────────────────────────────────────
    public Notification buildNotification() {
        boolean isOn =
                manager.isDataSaverOn();

        // Open main activity on tap
        Intent openIntent = new Intent(
                this, MainActivity.class);
        openIntent.setFlags(
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPending =
                PendingIntent.getActivity(
                        this, 0, openIntent,
                        PendingIntent
                                .FLAG_UPDATE_CURRENT
                                | PendingIntent
                                .FLAG_IMMUTABLE);

        // Toggle button in notification
        Intent toggleIntent = new Intent(
                this, DataSaverService.class);
        toggleIntent.setAction(
                Constants.ACTION_TOGGLE);
        PendingIntent togglePending =
                PendingIntent.getService(
                        this, 1, toggleIntent,
                        PendingIntent
                                .FLAG_UPDATE_CURRENT
                                | PendingIntent
                                .FLAG_IMMUTABLE);

        String statusText = isOn
                ? "🛡️ Protecting your hotspot data"
                : "Tap to activate data saving";
        String toggleText = isOn
                ? "Turn OFF" : "Turn ON";
        int iconRes = isOn
                ? R.drawable.ic_shield_on
                : R.drawable.ic_shield_off;

        // Add usage info to notification
        // if alerts are enabled
        String subText = "";
        if (alertManager.isEnabled()) {
            subText =
                    alertManager.getUsageSummary();
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        this,
                        SandaApp.CHANNEL_ID_SERVICE)
                        .setContentTitle(
                                Constants.APP_NAME)
                        .setContentText(statusText)
                        .setSmallIcon(iconRes)
                        .setContentIntent(openPending)
                        .setOngoing(true)
                        .setPriority(
                                NotificationCompat
                                        .PRIORITY_LOW)
                        .addAction(
                                R.drawable.ic_toggle,
                                toggleText,
                                togglePending);

        // Add usage subtext if available
        if (!subText.isEmpty()) {
            builder.setSubText(subText);
        }

        return builder.build();
    }

    // ─────────────────────────────────────
    // UPDATE NOTIFICATION
    // Call this after toggle to refresh
    // ─────────────────────────────────────
    public void updateNotification() {
        NotificationManager nm =
                (NotificationManager)
                        getSystemService(
                                NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(
                    Constants.NOTIF_ID_SERVICE,
                    buildNotification());
        }
    }
}