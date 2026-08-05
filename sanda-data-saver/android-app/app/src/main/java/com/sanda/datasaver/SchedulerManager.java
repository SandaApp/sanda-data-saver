package com.sanda.datasaver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Calendar;

/**
 * SchedulerManager
 * Manages the Data Saver schedule timer.
 * Sets alarms to automatically turn
 * Data Saver ON and OFF at set times.
 */
public class SchedulerManager {

    private static final String PREFS =
            "sanda_schedule";

    // ── Keys ──────────────────────────────
    private static final String KEY_ENABLED =
            "schedule_enabled";
    private static final String KEY_ON_HOUR =
            "on_hour";
    private static final String KEY_ON_MIN =
            "on_minute";
    private static final String KEY_OFF_HOUR =
            "off_hour";
    private static final String KEY_OFF_MIN =
            "off_minute";
    private static final String KEY_DAYS =
            "active_days";

    // ── Alarm Request Codes ───────────────
    private static final int RC_ON  = 3001;
    private static final int RC_OFF = 3002;

    private final Context       context;
    private final AlarmManager  alarmManager;
    private final SharedPreferences prefs;

    // ─────────────────────────────────────
    public SchedulerManager(Context context) {
        this.context = context
                .getApplicationContext();
        this.alarmManager =
                (AlarmManager) context
                        .getSystemService(
                                Context.ALARM_SERVICE);
        this.prefs = context
                .getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE);
    }

    // ─────────────────────────────────────
    // SAVE SCHEDULE SETTINGS
    // ─────────────────────────────────────
    public void saveSchedule(
            boolean enabled,
            int onHour, int onMinute,
            int offHour, int offMinute,
            boolean[] days) {

        // Convert days array to int bitmask
        // e.g. Mon=1, Tue=2, Wed=4 etc
        int daysMask = 0;
        for (int i = 0; i < 7; i++) {
            if (days[i]) {
                daysMask |= (1 << i);
            }
        }

        prefs.edit()
                .putBoolean(KEY_ENABLED, enabled)
                .putInt(KEY_ON_HOUR, onHour)
                .putInt(KEY_ON_MIN, onMinute)
                .putInt(KEY_OFF_HOUR, offHour)
                .putInt(KEY_OFF_MIN, offMinute)
                .putInt(KEY_DAYS, daysMask)
                .apply();

        if (enabled) {
            scheduleAlarms(
                    onHour, onMinute,
                    offHour, offMinute);
        } else {
            cancelAlarms();
        }
    }

    // ─────────────────────────────────────
    // GET SCHEDULE SETTINGS
    // ─────────────────────────────────────
    public boolean isEnabled() {
        return prefs.getBoolean(
                KEY_ENABLED, false);
    }

    public int getOnHour() {
        return prefs.getInt(KEY_ON_HOUR, 8);
    }

    public int getOnMinute() {
        return prefs.getInt(KEY_ON_MIN, 0);
    }

    public int getOffHour() {
        return prefs.getInt(
                KEY_OFF_HOUR, 18);
    }

    public int getOffMinute() {
        return prefs.getInt(KEY_OFF_MIN, 0);
    }

    public boolean[] getActiveDays() {
        int mask = prefs.getInt(
                KEY_DAYS, 0b1111100);
        // Default: Mon-Fri
        boolean[] days = new boolean[7];
        for (int i = 0; i < 7; i++) {
            days[i] = (mask & (1 << i)) != 0;
        }
        return days;
    }

    // ─────────────────────────────────────
    // SCHEDULE ALARMS
    // ─────────────────────────────────────
    private void scheduleAlarms(
            int onHour, int onMinute,
            int offHour, int offMinute) {

        // Cancel existing first
        cancelAlarms();

        // Schedule ON alarm
        setDailyAlarm(
                onHour, onMinute,
                SandaAlertReceiver
                        .ACTION_SCHEDULE_ON,
                RC_ON);

        // Schedule OFF alarm
        setDailyAlarm(
                offHour, offMinute,
                SandaAlertReceiver
                        .ACTION_SCHEDULE_OFF,
                RC_OFF);
    }

    // ─────────────────────────────────────
    // SET DAILY REPEATING ALARM
    // ─────────────────────────────────────
    private void setDailyAlarm(
            int hour, int minute,
            String action,
            int requestCode) {

        Intent intent = new Intent(
                context,
                SandaAlertReceiver.class);
        intent.setAction(action);

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent
                                .FLAG_UPDATE_CURRENT
                                | PendingIntent
                                .FLAG_IMMUTABLE);

        // Calculate next trigger time
        Calendar calendar =
                Calendar.getInstance();
        calendar.set(
                Calendar.HOUR_OF_DAY, hour);
        calendar.set(
                Calendar.MINUTE, minute);
        calendar.set(
                Calendar.SECOND, 0);
        calendar.set(
                Calendar.MILLISECOND, 0);

        // If time already passed today
        // set for tomorrow
        if (calendar.getTimeInMillis()
                <= System.currentTimeMillis()) {
            calendar.add(
                    Calendar.DAY_OF_MONTH, 1);
        }

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT
                    >= Build.VERSION_CODES.M) {
                alarmManager
                        .setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.getTimeInMillis(),
                                pendingIntent);
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent);
            }
        }
    }

    // ─────────────────────────────────────
    // CANCEL ALL ALARMS
    // ─────────────────────────────────────
    public void cancelAlarms() {
        cancelAlarm(
                SandaAlertReceiver
                        .ACTION_SCHEDULE_ON,
                RC_ON);
        cancelAlarm(
                SandaAlertReceiver
                        .ACTION_SCHEDULE_OFF,
                RC_OFF);
    }

    private void cancelAlarm(
            String action,
            int requestCode) {
        Intent intent = new Intent(
                context,
                SandaAlertReceiver.class);
        intent.setAction(action);

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent
                                .FLAG_UPDATE_CURRENT
                                | PendingIntent
                                .FLAG_IMMUTABLE);

        if (alarmManager != null) {
            alarmManager.cancel(
                    pendingIntent);
        }
    }

    // ─────────────────────────────────────
    // GET FORMATTED TIME STRING
    // ─────────────────────────────────────
    public static String formatTime(
            int hour, int minute) {
        String period =
                hour >= 12 ? "PM" : "AM";
        int displayHour = hour % 12;
        if (displayHour == 0)
            displayHour = 12;
        return String.format(
                "%d:%02d %s",
                displayHour, minute, period);
    }

    // ─────────────────────────────────────
    // GET DAY NAMES
    // ─────────────────────────────────────
    public static String[] getDayNames() {
        return new String[]{
                "Mon", "Tue", "Wed",
                "Thu", "Fri", "Sat", "Sun"
        };
    }
}