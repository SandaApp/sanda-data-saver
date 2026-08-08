package com.sanda.datasaver;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * HealthReminderManager — Schedules periodic health reminders to distract from excessive
 * gaming/social media and focus on scripture, prayer, constructive activities.
 * Uses AlarmManager for exact timing.
 */
public class HealthReminderManager {

    public static final String CHANNEL_ID_HEALTH = "sanda_health_channel";
    public static final int NOTIF_ID_HEALTH = 2001;
    public static final String ACTION_HEALTH_REMINDER = "com.sanda.datasaver.HEALTH_REMINDER";
    public static final String PREF_HEALTH_ENABLED = "health_enabled";
    public static final String PREF_HEALTH_INTERVAL_MIN = "health_interval_min"; // minutes
    public static final String PREF_HEALTH_LAST_TIP_INDEX = "health_last_tip_index";

    private static final int DEFAULT_INTERVAL_MIN = 60; // 60 minutes default

    private Context context;
    private PrefsManager prefs;

    public HealthReminderManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = new PrefsManager(context);
        createChannel();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                NotificationChannel existing = manager.getNotificationChannel(CHANNEL_ID_HEALTH);
                if (existing == null) {
                    NotificationChannel channel = new NotificationChannel(
                            CHANNEL_ID_HEALTH,
                            "Health Reminders - Sanda",
                            NotificationManager.IMPORTANCE_DEFAULT
                    );
                    channel.setDescription("Gentle reminders to reduce screen time, pray, read scripture, do constructive activities");
                    channel.enableVibration(true);
                    channel.setShowBadge(false);
                    manager.createNotificationChannel(channel);
                }
            }
        }
    }

    public boolean isEnabled() {
        return prefs.getPrefs().getBoolean(PREF_HEALTH_ENABLED, true); // enabled by default
    }

    public void setEnabled(boolean enabled) {
        prefs.getPrefs().edit().putBoolean(PREF_HEALTH_ENABLED, enabled).apply();
        if (enabled) {
            scheduleNext();
        } else {
            cancel();
        }
    }

    public int getIntervalMin() {
        return prefs.getPrefs().getInt(PREF_HEALTH_INTERVAL_MIN, DEFAULT_INTERVAL_MIN);
    }

    public void setIntervalMin(int minutes) {
        if (minutes < 15) minutes = 15;
        if (minutes > 240) minutes = 240;
        prefs.getPrefs().edit().putInt(PREF_HEALTH_INTERVAL_MIN, minutes).apply();
        if (isEnabled()) {
            scheduleNext();
        }
    }

    public void scheduleNext() {
        if (!isEnabled()) return;

        int intervalMin = getIntervalMin();
        long intervalMillis = intervalMin * 60L * 1000L;
        long triggerAt = System.currentTimeMillis() + intervalMillis;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, SandaAlertReceiver.class);
        intent.setAction(ACTION_HEALTH_REMINDER);

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                3001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            // Cancel existing first
            alarmManager.cancel(pi);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        }
    }

    public void cancel() {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, SandaAlertReceiver.class);
        intent.setAction(ACTION_HEALTH_REMINDER);
        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                3001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        if (alarmManager != null) {
            alarmManager.cancel(pi);
        }
    }

    public void showHealthNotification() {
        if (!isEnabled()) return;

        // Get next tip in rotation
        int lastIndex = prefs.getPrefs().getInt(PREF_HEALTH_LAST_TIP_INDEX, -1);
        int nextIndex = (lastIndex + 1) % HealthTipsProvider.getCount();
        HealthTip tip = HealthTipsProvider.getTip(nextIndex);

        prefs.getPrefs().edit().putInt(PREF_HEALTH_LAST_TIP_INDEX, nextIndex).apply();

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_HEALTH)
                .setSmallIcon(R.drawable.sanda_logo)
                .setContentTitle(tip.emoji + " " + tip.title)
                .setContentText(tip.action)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(tip.getFullMessage()))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER);

        // Intent to open HealthActivity when tapped
        Intent openIntent = new Intent(context, HealthActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent openPI = PendingIntent.getActivity(
                context,
                3002,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        builder.setContentIntent(openPI);

        // Action button: Mark as done and schedule next
        Intent doneIntent = new Intent(context, SandaAlertReceiver.class);
        doneIntent.setAction(ACTION_HEALTH_REMINDER + "_DONE");
        PendingIntent donePI = PendingIntent.getBroadcast(
                context,
                3003,
                doneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        builder.addAction(new NotificationCompat.Action.Builder(
                R.drawable.ic_shield_on,
                "✅ Done - Prayed",
                donePI
        ).build());

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        try {
            nm.notify(NOTIF_ID_HEALTH + nextIndex, builder.build());
        } catch (SecurityException e) {
            // Notification permission not granted
        }

        // Schedule next
        scheduleNext();
    }

    public HealthTip getCurrentTip() {
        int lastIndex = prefs.getPrefs().getInt(PREF_HEALTH_LAST_TIP_INDEX, 0);
        return HealthTipsProvider.getTip(lastIndex);
    }

    public HealthTip getNextTip() {
        int lastIndex = prefs.getPrefs().getInt(PREF_HEALTH_LAST_TIP_INDEX, -1);
        int nextIndex = (lastIndex + 1) % HealthTipsProvider.getCount();
        return HealthTipsProvider.getTip(nextIndex);
    }
}
