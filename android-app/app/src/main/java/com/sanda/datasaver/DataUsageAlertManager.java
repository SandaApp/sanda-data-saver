package com.sanda.datasaver;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * DataUsageAlertManager
 * Monitors daily data usage and
 * sends alerts at 50%, 80%, 100%
 * of the user's set daily limit.
 */
public class DataUsageAlertManager {

    private static final String PREFS =
            "sanda_usage_alerts";

    private static final String KEY_LIMIT_MB =
            "daily_limit_mb";
    private static final String KEY_ENABLED =
            "alerts_enabled";
    private static final String KEY_ALERT_50 =
            "alert_50_sent";
    private static final String KEY_ALERT_80 =
            "alert_80_sent";
    private static final String KEY_ALERT_100 =
            "alert_100_sent";
    private static final String KEY_LAST_DATE =
            "last_check_date";

    private final Context context;
    private final SharedPreferences prefs;
    private final DataUsageHelper usageHelper;

    // ─────────────────────────────────────
    public DataUsageAlertManager(
            Context context) {
        this.context = context
                .getApplicationContext();
        this.prefs = context
                .getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE);
        this.usageHelper =
                new DataUsageHelper(context);
    }

    // ─────────────────────────────────────
    // SAVE ALERT SETTINGS
    // ─────────────────────────────────────
    public void setEnabled(boolean enabled) {
        prefs.edit()
                .putBoolean(KEY_ENABLED, enabled)
                .apply();
    }

    public boolean isEnabled() {
        return prefs.getBoolean(
                KEY_ENABLED, false);
    }

    public void setDailyLimitMb(int mb) {
        prefs.edit()
                .putInt(KEY_LIMIT_MB, mb)
                .apply();
        // Reset alerts when limit changes
        resetDailyAlerts();
    }

    public int getDailyLimitMb() {
        return prefs.getInt(
                KEY_LIMIT_MB, 1024);
    }

    // ─────────────────────────────────────
    // CHECK USAGE AND SEND ALERTS
    // Called periodically by service
    // ─────────────────────────────────────
    public void checkAndAlert() {
        if (!isEnabled()) return;

        int limitMb = getDailyLimitMb();
        if (limitMb <= 0) return;

        // Reset alerts each new day
        String today = getTodayString();
        String lastDate = prefs.getString(
                KEY_LAST_DATE, "");
        if (!today.equals(lastDate)) {
            resetDailyAlerts();
            prefs.edit()
                    .putString(
                            KEY_LAST_DATE, today)
                    .apply();
        }

        // Get current usage in MB
        long usedBytes =
                usageHelper
                        .getTotalMobileBytesToday();
        long usedMb =
                usedBytes / (1024 * 1024);

        // Calculate percentage
        int percent =
                (int) ((usedMb * 100) / limitMb);

        // Send alerts at thresholds
        if (percent >= 100
                && !prefs.getBoolean(
                KEY_ALERT_100, false)) {
            sendAlert(100);
            prefs.edit()
                    .putBoolean(
                            KEY_ALERT_100, true)
                    .apply();
        } else if (percent >= 80
                && !prefs.getBoolean(
                KEY_ALERT_80, false)) {
            sendAlert(80);
            prefs.edit()
                    .putBoolean(
                            KEY_ALERT_80, true)
                    .apply();
        } else if (percent >= 50
                && !prefs.getBoolean(
                KEY_ALERT_50, false)) {
            sendAlert(50);
            prefs.edit()
                    .putBoolean(
                            KEY_ALERT_50, true)
                    .apply();
        }
    }

    // ─────────────────────────────────────
    // SEND ALERT BROADCAST
    // ─────────────────────────────────────
    private void sendAlert(int percent) {
        Intent intent = new Intent(
                context,
                SandaAlertReceiver.class);
        intent.setAction(
                SandaAlertReceiver
                        .ACTION_USAGE_ALERT);
        intent.putExtra("percent", percent);
        context.sendBroadcast(intent);
    }

    // ─────────────────────────────────────
    // RESET DAILY ALERTS
    // Called at start of each new day
    // ─────────────────────────────────────
    public void resetDailyAlerts() {
        prefs.edit()
                .putBoolean(KEY_ALERT_50, false)
                .putBoolean(KEY_ALERT_80, false)
                .putBoolean(KEY_ALERT_100, false)
                .apply();
    }

    // ─────────────────────────────────────
    // GET TODAY AS STRING
    // Used to detect day change
    // ─────────────────────────────────────
    private String getTodayString() {
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat(
                        "yyyy-MM-dd",
                        java.util.Locale.getDefault());
        return sdf.format(
                new java.util.Date());
    }

    // ─────────────────────────────────────
    // GET USAGE SUMMARY
    // Returns formatted usage string
    // ─────────────────────────────────────
    public String getUsageSummary() {
        int limitMb = getDailyLimitMb();
        long usedBytes =
                usageHelper
                        .getTotalMobileBytesToday();
        long usedMb =
                usedBytes / (1024 * 1024);
        int percent = limitMb > 0
                ? (int) ((usedMb * 100) / limitMb)
                : 0;

        return usedMb + " MB used of "
                + limitMb + " MB limit ("
                + percent + "%)";
    }
}