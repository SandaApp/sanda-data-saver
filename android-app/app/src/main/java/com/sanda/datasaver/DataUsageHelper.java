package com.sanda.datasaver;

import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * DataUsageHelper — Reads real data usage per app.
 * Requires PACKAGE_USAGE_STATS permission.
 */
public class DataUsageHelper {

    private static final String TAG = "DataUsageHelper";

    private final Context             context;
    private final NetworkStatsManager statsManager;
    private final PackageManager      packageManager;

    public DataUsageHelper(Context context) {
        this.context        = context.getApplicationContext();
        this.packageManager = context.getPackageManager();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.statsManager = (NetworkStatsManager)
                context.getSystemService(Context.NETWORK_STATS_SERVICE);
        } else {
            this.statsManager = null;
        }
    }

    // ─────────────────────────────────────────────
    // APP DATA MODEL
    // ─────────────────────────────────────────────
    public static class AppDataUsage {
        public String   packageName;
        public String   appName;
        public Drawable appIcon;
        public long     bytesUsed;
        public long     mobileBytes;
        public long     wifiBytes;

        public String getReadableSize() {
            return formatBytes(bytesUsed);
        }

        public String getMobileReadableSize() {
            return formatBytes(mobileBytes);
        }

        private static String formatBytes(long bytes) {
            if (bytes <= 0)           return "0 B";
            if (bytes < 1024)         return bytes + " B";
            if (bytes < 1024 * 1024)  return (bytes / 1024) + " KB";
            if (bytes < 1024 * 1024 * 1024)
                return String.format("%.1f MB", bytes / (1024.0 * 1024));
            return String.format("%.2f GB",
                bytes / (1024.0 * 1024 * 1024));
        }
    }

    // ─────────────────────────────────────────────
    // GET USAGE FOR TODAY
    // ─────────────────────────────────────────────
    public List<AppDataUsage> getTodayUsage() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return new ArrayList<>();
        }

        List<AppDataUsage> result = new ArrayList<>();

        // Get start of today
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startTime = calendar.getTimeInMillis();
        long endTime   = System.currentTimeMillis();

        List<ApplicationInfo> apps =
            packageManager.getInstalledApplications(
                PackageManager.GET_META_DATA);

        for (ApplicationInfo app : apps) {
            try {
                long mobile = getMobileUsage(
                    app.uid, startTime, endTime);
                long wifi   = getWifiUsage(
                    app.uid, startTime, endTime);
                long total  = mobile + wifi;

                if (total > 0) {
                    AppDataUsage usage = new AppDataUsage();
                    usage.packageName  = app.packageName;
                    usage.appName      = packageManager
                        .getApplicationLabel(app).toString();
                    usage.appIcon      = packageManager
                        .getApplicationIcon(app.packageName);
                    usage.bytesUsed    = total;
                    usage.mobileBytes  = mobile;
                    usage.wifiBytes    = wifi;
                    result.add(usage);
                }
            } catch (Exception e) {
                // Skip apps we cannot read
            }
        }

        // Sort by highest usage first
        Collections.sort(result,
            (a, b) -> Long.compare(b.bytesUsed, a.bytesUsed));

        return result;
    }

    // ─────────────────────────────────────────────
    // MOBILE DATA USAGE
    // ─────────────────────────────────────────────
    private long getMobileUsage(int uid, long start, long end) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || statsManager == null) return 0;
        try {
            NetworkStats stats = statsManager.queryDetailsForUid(
                ConnectivityManager.TYPE_MOBILE, null, start, end, uid);
            return sumStats(stats);
        } catch (Exception e) {
            return 0;
        }
    }

    // ─────────────────────────────────────────────
    // WIFI DATA USAGE
    // ─────────────────────────────────────────────
    private long getWifiUsage(int uid, long start, long end) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || statsManager == null) return 0;
        try {
            NetworkStats stats = statsManager.queryDetailsForUid(
                ConnectivityManager.TYPE_WIFI, null, start, end, uid);
            return sumStats(stats);
        } catch (Exception e) {
            return 0;
        }
    }

    private long sumStats(NetworkStats stats) {
        long total = 0;
        NetworkStats.Bucket bucket = new NetworkStats.Bucket();
        while (stats.hasNextBucket()) {
            stats.getNextBucket(bucket);
            total += bucket.getRxBytes() + bucket.getTxBytes();
        }
        stats.close();
        return total;
    }

    // ─────────────────────────────────────────────
    // TOTAL DATA TODAY
    // ─────────────────────────────────────────────
    public long getTotalMobileBytesToday() {
        List<AppDataUsage> usage = getTodayUsage();
        long total = 0;
        for (AppDataUsage app : usage) {
            total += app.mobileBytes;
        }
        return total;
    }
}