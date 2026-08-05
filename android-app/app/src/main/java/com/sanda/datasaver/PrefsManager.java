package com.sanda.datasaver;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * PrefsManager — Handles all saved settings.
 * Single source of truth for app state.
 * No Gson — uses simple comma separated
 * strings for ProGuard compatibility.
 */
public class PrefsManager {

    private final SharedPreferences prefs;

    // ── Default blocked apps ──────────────────
    private static final List<String>
    DEFAULT_BLOCKED = Arrays.asList(
        "com.google.android.youtube",
        "com.instagram.android",
        "com.twitter.android",
        "com.snapchat.android",
        "com.spotify.music",
        "com.netflix.mediaclient",
        "com.google.android.gms",
        "com.android.vending",
        "com.google.android.googlequicksearchbox",
        "com.microsoft.teams",
        "com.discord",
        "com.tiktok.musically"
    );

    // ── Separator for storing lists ───────────
    // Using ||| so it never clashes with
    // package names which use dots and slashes
    private static final String SEP = "|||";

    // ─────────────────────────────────────────
   public PrefsManager(Context context) {
    prefs = context.getSharedPreferences(
        Constants.PREFS_NAME,
        Context.MODE_PRIVATE);

    // First run — save defaults
    if (!prefs.contains(
            Constants.KEY_BLOCKED_APPS)) {
        saveBlockedApps(
            new ArrayList<>(DEFAULT_BLOCKED));
    }

    // Version 1.0.1 fix:
    // Remove WhatsApp and Facebook
    // from blocked list if they were
    // in the old defaults
    if (!prefs.getBoolean(
            "v101_cleaned", false)) {
        List<String> current =
            getBlockedApps();
        current.remove("com.whatsapp");
        current.remove(
            "com.facebook.katana");
        saveBlockedApps(current);
        prefs.edit()
            .putBoolean("v101_cleaned", true)
            .apply();
    }
}
	
	// ── Get raw SharedPreferences ─────────
public android.content.SharedPreferences
        getPrefs() {
    return prefs;
}

    // ── Data Saver State ─────────────────────
    public boolean isDataSaverOn() {
        return prefs.getBoolean(
                Constants.KEY_DATA_SAVER_ON, false);
    }

    public void setDataSaverOn(boolean on) {
        prefs.edit()
                .putBoolean(
                        Constants.KEY_DATA_SAVER_ON, on)
                .apply();
    }

    // ── Blocked Apps ─────────────────────────
    // Stored as pipe separated string
    // e.g. "com.whatsapp|||com.instagram"
    // No Gson needed — ProGuard safe!
    public List<String> getBlockedApps() {
        String saved = prefs.getString(
                Constants.KEY_BLOCKED_APPS, null);

        // No saved list yet
        if (saved == null
                || saved.isEmpty()) {
            return new ArrayList<>(
                    DEFAULT_BLOCKED);
        }

        // Split by separator
        String[] parts = saved.split(
                "\\|\\|\\|");

        List<String> list = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }

        // If list is empty return defaults
        if (list.isEmpty()) {
            return new ArrayList<>(
                    DEFAULT_BLOCKED);
        }

        return list;
    }

    public void saveBlockedApps(
            List<String> apps) {
        if (apps == null || apps.isEmpty()) {
            prefs.edit()
                    .putString(
                            Constants.KEY_BLOCKED_APPS,
                            "")
                    .apply();
            return;
        }

        // Join with separator
        StringBuilder sb =
                new StringBuilder();
        for (int i = 0; i < apps.size(); i++) {
            sb.append(apps.get(i));
            if (i < apps.size() - 1) {
                sb.append(SEP);
            }
        }

        prefs.edit()
                .putString(
                        Constants.KEY_BLOCKED_APPS,
                        sb.toString())
                .apply();
    }

    // ── Notifications ─────────────────────────
    public boolean isNotificationsEnabled() {
        return prefs.getBoolean(
                Constants.KEY_NOTIFICATIONS, true);
    }

    public void setNotificationsEnabled(
            boolean enabled) {
        prefs.edit()
                .putBoolean(
                        Constants.KEY_NOTIFICATIONS,
                        enabled)
                .apply();
    }

    // ── Auto Start ────────────────────────────
    public boolean isAutoStartEnabled() {
        return prefs.getBoolean(
                Constants.KEY_AUTO_START, true);
    }

    public void setAutoStartEnabled(
            boolean enabled) {
        prefs.edit()
                .putBoolean(
                        Constants.KEY_AUTO_START,
                        enabled)
                .apply();
    }

    // ── Daily Data Limit ──────────────────────
    public int getDailyLimitMb() {
        return prefs.getInt(
                Constants.KEY_DAILY_LIMIT_MB,
                Constants.DEFAULT_DAILY_LIMIT_MB);
    }

    public void setDailyLimitMb(int mb) {
        prefs.edit()
                .putInt(
                        Constants.KEY_DAILY_LIMIT_MB,
                        mb)
                .apply();
    }

    // ── Theme Mode ────────────────────────────
    public int getThemeMode() {
        // -1 means follow system default
        return prefs.getInt(
                "theme_mode", -1);
    }

    public void setThemeMode(int mode) {
        prefs.edit()
                .putInt("theme_mode", mode)
                .apply();
    }

    // ── Data Warning ──────────────────────────
    public boolean isDataWarningEnabled() {
        return prefs.getBoolean(
                "data_warning_enabled", false);
    }

    public void setDataWarningEnabled(
            boolean enabled) {
        prefs.edit()
                .putBoolean(
                        "data_warning_enabled",
                        enabled)
                .apply();
    }
}