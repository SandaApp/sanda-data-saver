package com.sanda.datasaver;

/**
 * Constants — All shared constants in one place.
 * Change branding details here.
 */
public class Constants {

    // ── Branding ─────────────────────────────────
    public static final String APP_NAME      = "Sanda Data Saver";
    public static final String APP_VERSION   = "1.0.16";
    public static final String APP_AUTHOR    = "Bishop Dr. David Sanda";
    public static final String APP_TAGLINE   = "Smart Data. Your Control.";
    public static final String APP_COPYRIGHT = "© 2026 Bishop Dr. David Sanda - Free for Jesus";

    // ── Shared Preferences Keys ───────────────────
    public static final String PREFS_NAME          = "SandaDataSaverPrefs";
    public static final String KEY_DATA_SAVER_ON   = "data_saver_on";
    public static final String KEY_BLOCKED_APPS    = "blocked_apps";
    public static final String KEY_NOTIFICATIONS   = "notifications_enabled";
    public static final String KEY_AUTO_START      = "auto_start_enabled";
    public static final String KEY_DAILY_LIMIT_MB  = "daily_limit_mb";

    // ── Intent Actions ────────────────────────────
    public static final String ACTION_TOGGLE =
        "com.sanda.datasaver.ACTION_TOGGLE";
    public static final String ACTION_STATUS_UPDATE =
        "com.sanda.datasaver.ACTION_STATUS_UPDATE";

    // ── Notification IDs ──────────────────────────
    public static final int NOTIF_ID_SERVICE = 1001;
    public static final int NOTIF_ID_ALERT   = 1002;

    // ── Default Daily Data Limit ──────────────────
    public static final int DEFAULT_DAILY_LIMIT_MB = 500;

    // ── Colors (as hex strings for reference) ─────
    public static final String COLOR_PRIMARY = "#00C9FF";
    public static final String COLOR_ACCENT  = "#FF6B6B";
    public static final String COLOR_SUCCESS = "#00FF88";
    public static final String COLOR_BG      = "#0D1117";
}