package com.sanda.datasaver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

/**
 * SandaAlertReceiver
 * Receives scheduled alarms and
 * data usage alert broadcasts.
 * Handles:
 * 1. Schedule ON/OFF timer
 * 2. Data usage limit alerts
 */
public class SandaAlertReceiver
        extends BroadcastReceiver {

    // ── Action Constants ──────────────────
    public static final String ACTION_SCHEDULE_ON =
            "com.sanda.datasaver.SCHEDULE_ON";
    public static final String ACTION_SCHEDULE_OFF =
            "com.sanda.datasaver.SCHEDULE_OFF";
    public static final String ACTION_USAGE_ALERT =
            "com.sanda.datasaver.USAGE_ALERT";

    // ── Notification IDs ──────────────────
    public static final int NOTIF_SCHEDULE = 1001;
    public static final int NOTIF_USAGE    = 1002;

    // ── Channel ID ────────────────────────
    public static final String CHANNEL_ID =
            "sanda_alerts";

    // ─────────────────────────────────────
    @Override
    public void onReceive(
            Context context, Intent intent) {

        if (intent == null
                || intent.getAction() == null)
            return;

        createNotificationChannel(context);

        switch (intent.getAction()) {

            case ACTION_SCHEDULE_ON:
                handleScheduleOn(context);
                break;

            case ACTION_SCHEDULE_OFF:
                handleScheduleOff(context);
                break;

            case ACTION_USAGE_ALERT:
                int percent = intent
                        .getIntExtra(
                                "percent", 80);
                handleUsageAlert(
                        context, percent);
                break;
        }
    }

    // ─────────────────────────────────────
    // HANDLE SCHEDULE ON
    // ─────────────────────────────────────
    private void handleScheduleOn(
            Context context) {
        PrefsManager prefs =
                new PrefsManager(context);

        // Activate data saver
        DataSaverManager manager =
                new DataSaverManager(context);
        manager.activate();

        // Send notification
        sendNotification(
                context,
                NOTIF_SCHEDULE,
                "🛡️ Data Saver Activated",
                "Scheduled Data Saver is now ON. "
                        + "Your data is protected.",
                true
        );
    }

    // ─────────────────────────────────────
    // HANDLE SCHEDULE OFF
    // ─────────────────────────────────────
    private void handleScheduleOff(
            Context context) {
        // Deactivate data saver
        DataSaverManager manager =
                new DataSaverManager(context);
        manager.deactivate();

        // Send notification
        sendNotification(
                context,
                NOTIF_SCHEDULE,
                "✅ Data Saver Deactivated",
                "Scheduled Data Saver is now OFF. "
                        + "Normal mode restored.",
                false
        );
    }

    // ─────────────────────────────────────
    // HANDLE USAGE ALERT
    // ─────────────────────────────────────
    private void handleUsageAlert(
            Context context, int percent) {
        PrefsManager prefs =
                new PrefsManager(context);

        String title;
        String message;
        boolean autoActivate = false;

        if (percent >= 100) {
            title = "🚨 Data Limit Reached!";
            message =
                    "You have used 100% of your "
                            + "daily data limit. "
                            + "Data Saver activated "
                            + "automatically!";
            autoActivate = true;
        } else if (percent >= 80) {
            title = "⚠️ Data Warning: 80%";
            message =
                    "You have used 80% of your "
                            + "daily data limit. "
                            + "Consider turning on "
                            + "Data Saver.";
        } else {
            title = "📊 Data Alert: 50%";
            message =
                    "You have used 50% of your "
                            + "daily data limit. "
                            + "Keep an eye on your usage.";
        }

        // Auto activate at 100%
        if (autoActivate) {
            DataSaverManager manager =
                    new DataSaverManager(context);
            manager.activate();
        }

        sendNotification(
                context,
                NOTIF_USAGE,
                title,
                message,
                autoActivate
        );
    }

    // ─────────────────────────────────────
    // SEND NOTIFICATION
    // ─────────────────────────────────────
    private void sendNotification(
            Context context,
            int notifId,
            String title,
            String message,
            boolean isOn) {

        // Open MainActivity on tap
        Intent openApp = new Intent(
                context, MainActivity.class);
        openApp.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        context, 0, openApp,
                        PendingIntent
                                .FLAG_UPDATE_CURRENT
                                | PendingIntent
                                .FLAG_IMMUTABLE);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        context, CHANNEL_ID)
                        .setSmallIcon(
                                R.drawable.ic_shield_on)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setStyle(
                                new NotificationCompat
                                        .BigTextStyle()
                                        .bigText(message))
                        .setPriority(
                                NotificationCompat
                                        .PRIORITY_HIGH)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);

        NotificationManager nm =
                (NotificationManager) context
                        .getSystemService(
                                Context
                                        .NOTIFICATION_SERVICE);

        if (nm != null) {
            nm.notify(notifId, builder.build());
        }
    }

    // ─────────────────────────────────────
    // CREATE NOTIFICATION CHANNEL
    // ─────────────────────────────────────
    private void createNotificationChannel(
            Context context) {
        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.O) {
            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Sanda Alerts",
                            NotificationManager
                                    .IMPORTANCE_HIGH);
            channel.setDescription(
                    "Data usage and schedule "
                            + "alerts from Sanda "
                            + "Data Saver");

            NotificationManager nm =
                    context.getSystemService(
                            NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(
                        channel);
            }
        }
    }
}