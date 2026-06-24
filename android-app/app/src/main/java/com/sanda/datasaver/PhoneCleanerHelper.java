package com.sanda.datasaver;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * PhoneCleanerHelper — Android Phone Cleaner
 * Safely removes junk files and frees space.
 * Translated from Sanda PC Cleaner logic.
 * No root required.
 */
public class PhoneCleanerHelper {

    private static final String TAG =
            "PhoneCleanerHelper";

    private final Context context;

    // ── Callback interface ────────────────
    public interface CleanCallback {
        void onLog(String message);
        void onDone(long totalFreed);
        void onError(String error);
    }

    // ─────────────────────────────────────
    public PhoneCleanerHelper(Context context) {
        this.context = context
                .getApplicationContext();
    }

    // ─────────────────────────────────────
    // FORMAT BYTES TO READABLE STRING
    // Same as PC: bytes_to_readable()
    // ─────────────────────────────────────
    public static String formatBytes(
            long bytes) {
        if (bytes <= 0) return "0 B";
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format(
                    "%.1f KB",
                    bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024)
            return String.format(
                    "%.2f MB",
                    bytes / (1024.0 * 1024));
        return String.format(
                "%.2f GB",
                bytes / (1024.0 * 1024 * 1024));
    }

    // ─────────────────────────────────────
    // GET DIRECTORY SIZE
    // Same as PC: get_size()
    // ─────────────────────────────────────
    public static long getDirSize(File dir) {
        long total = 0;
        if (dir == null
                || !dir.exists()) return 0;
        try {
            File[] files = dir.listFiles();
            if (files == null) return 0;
            for (File file : files) {
                if (file.isFile()) {
                    total += file.length();
                } else if (file.isDirectory()) {
                    total += getDirSize(file);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getDirSize: "
                    + e.getMessage());
        }
        return total;
    }

    // ─────────────────────────────────────
    // SAFE CLEAN FOLDER
    // Same as PC: safe_clean_folder()
    // ─────────────────────────────────────
    public long cleanFolder(
            File folder,
            String description,
            CleanCallback callback) {

        if (folder == null
                || !folder.exists()) {
            log(callback,
                    "  ⏭️ Skipped: "
                            + description
                            + " (not found)");
            return 0;
        }

        long spaceBefore =
                getDirSize(folder);
        int  cleaned     = 0;
        int  errors      = 0;

        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                try {
                    if (deleteRecursive(file)) {
                        cleaned++;
                    } else {
                        errors++;
                    }
                } catch (Exception e) {
                    errors++;
                }
            }
        }

        long spaceAfter  =
                getDirSize(folder);
        long spaceFreed  =
                spaceBefore - spaceAfter;

        log(callback,
                "  ✅ " + description + ": "
                        + cleaned + " items removed ("
                        + formatBytes(spaceFreed)
                        + " freed)"
                        + (errors > 0
                        ? ", " + errors + " skipped"
                        : ""));

        return Math.max(0, spaceFreed);
    }

    // ─────────────────────────────────────
    // DELETE FILE OR FOLDER RECURSIVELY
    // ─────────────────────────────────────
    private boolean deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        return f.delete();
    }

    // ─────────────────────────────────────
    // 1. CLEAN APP CACHE
    // Android equivalent of User Temp
    // Clears our own app cache
    // ─────────────────────────────────────
    public long cleanAppCache(
            CleanCallback callback) {
        log(callback,
                "\n🗑️ Cleaning App Cache...");
        long total = 0;

        // Our own app cache
        File cacheDir =
                context.getCacheDir();
        total += cleanFolder(
                cacheDir,
                "App Cache",
                callback);

        // External cache if available
        File extCache =
                context.getExternalCacheDir();
        if (extCache != null) {
            total += cleanFolder(
                    extCache,
                    "External Cache",
                    callback);
        }

        return total;
    }

    // ─────────────────────────────────────
    // 2. CLEAN THUMBNAIL CACHE
    // Android equivalent of PC thumbnail
    // ─────────────────────────────────────
    public long cleanThumbnailCache(
            CleanCallback callback) {
        log(callback,
                "\n🖼️ Cleaning Thumbnail Cache...");
        long total = 0;

        // Android stores thumbnails here
        File thumbDir = new File(
                Environment
                        .getExternalStorageDirectory(),
                ".thumbnails");

        total += cleanFolder(
                thumbDir,
                "Media Thumbnails",
                callback);

        // DCIM thumbnails
        File dcimThumb = new File(
                Environment
                        .getExternalStorageDirectory(),
                "DCIM/.thumbnails");

        total += cleanFolder(
                dcimThumb,
                "DCIM Thumbnails",
                callback);

        return total;
    }

    // ─────────────────────────────────────
    // 3. CLEAN DOWNLOADED APK FILES
    // Android equivalent of Recent Files
    // Finds leftover APK installers
    // ─────────────────────────────────────
    public long cleanApkFiles(
            CleanCallback callback) {
        log(callback,
                "\n📦 Cleaning APK Files...");
        long total   = 0;
        int  count   = 0;
        int  errors  = 0;

        File downloadDir =
                Environment
                        .getExternalStoragePublicDirectory(
                                Environment
                                        .DIRECTORY_DOWNLOADS);

        if (downloadDir != null
                && downloadDir.exists()) {
            File[] files =
                    downloadDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName()
                            .toLowerCase()
                            .endsWith(".apk")) {
                        long size = f.length();
                        if (f.delete()) {
                            total += size;
                            count++;
                        } else {
                            errors++;
                        }
                    }
                }
            }
        }

        log(callback,
                "  ✅ APK Files: "
                        + count + " removed ("
                        + formatBytes(total) + " freed)"
                        + (errors > 0
                        ? ", " + errors + " skipped"
                        : ""));

        return total;
    }

    // ─────────────────────────────────────
    // 4. CLEAN TEMP AND LOG FILES
    // Android equivalent of Windows Logs
    // ─────────────────────────────────────
    public long cleanTempFiles(
            CleanCallback callback) {
        log(callback,
                "\n📋 Cleaning Temp and Log Files...");
        long total = 0;

        // App files directory temp folder
        File filesDir =
                context.getFilesDir();
        if (filesDir != null) {
            // Look for .log and .tmp files
            File[] files =
                    filesDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    String name =
                            f.getName()
                                    .toLowerCase();
                    if (name.endsWith(".log")
                            || name.endsWith(
                            ".tmp")
                            || name.endsWith(
                            ".temp")) {
                        long size = f.length();
                        if (f.delete()) {
                            total += size;
                        }
                    }
                }
            }
        }

        log(callback,
                "  ✅ Temp and Log Files: "
                        + formatBytes(total) + " freed");

        return total;
    }

    // ─────────────────────────────────────
    // 5. CLEAN EMPTY FOLDERS
    // Finds and removes empty directories
    // ─────────────────────────────────────
    public int cleanEmptyFolders(
            CleanCallback callback) {
        log(callback,
                "\n📁 Cleaning Empty Folders...");
        int count = 0;

        File sdCard =
                Environment
                        .getExternalStorageDirectory();
        if (sdCard != null
                && sdCard.exists()) {
            count += removeEmptyDirs(sdCard);
        }

        log(callback,
                "  ✅ Empty Folders: "
                        + count + " removed");
        return count;
    }

    private int removeEmptyDirs(File dir) {
        int count = 0;
        if (!dir.isDirectory()) return 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;

        for (File f : files) {
            if (f.isDirectory()) {
                count += removeEmptyDirs(f);
                // Delete if now empty
                File[] remaining =
                        f.listFiles();
                if (remaining != null
                        && remaining.length
                        == 0) {
                    if (f.delete()) count++;
                }
            }
        }
        return count;
    }

    // ─────────────────────────────────────
    // 6. ANALYZE STORAGE
    // Shows storage breakdown
    // ─────────────────────────────────────
    public StorageInfo getStorageInfo() {
        StorageInfo info = new StorageInfo();
        try {
            StatFs stat = new StatFs(
                    Environment
                            .getDataDirectory()
                            .getPath());
            long blockSize =
                    stat.getBlockSizeLong();
            info.totalBytes =
                    stat.getBlockCountLong()
                            * blockSize;
            info.freeBytes =
                    stat.getAvailableBlocksLong()
                            * blockSize;
            info.usedBytes =
                    info.totalBytes
                            - info.freeBytes;
        } catch (Exception e) {
            Log.e(TAG, "Storage info: "
                    + e.getMessage());
        }
        return info;
    }

    // ─────────────────────────────────────
    // 7. GET CLEANABLE SIZE ESTIMATE
    // Shows how much can be cleaned before
    // actually cleaning
    // ─────────────────────────────────────
    public long estimateCleanableSize() {
        long total = 0;

        // App cache
        File cacheDir =
                context.getCacheDir();
        total += getDirSize(cacheDir);

        File extCache =
                context.getExternalCacheDir();
        if (extCache != null) {
            total += getDirSize(extCache);
        }

        // Thumbnails
        File thumbDir = new File(
                Environment
                        .getExternalStorageDirectory(),
                ".thumbnails");
        total += getDirSize(thumbDir);

        // APK files
        File downloadDir =
                Environment
                        .getExternalStoragePublicDirectory(
                                Environment
                                        .DIRECTORY_DOWNLOADS);
        if (downloadDir != null
                && downloadDir.exists()) {
            File[] files =
                    downloadDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName()
                            .toLowerCase()
                            .endsWith(".apk")) {
                        total += f.length();
                    }
                }
            }
        }

        return total;
    }

    // ─────────────────────────────────────
    // RUN FULL CLEAN
    // Same as PC: run_full_clean()
    // Runs all cleaning steps in sequence
    // ─────────────────────────────────────
    public void runFullClean(
            CleanCallback callback) {
        new Thread(() -> {
            long total = 0;
            log(callback,
                    "🚀 Starting Full Phone Clean...\n"
                            + "================================");

            try {
                // Step 1: App Cache
                log(callback,
                        "\n[1/5] App Cache...");
                total += cleanAppCache(
                        callback);

                // Step 2: Thumbnails
                log(callback,
                        "\n[2/5] Thumbnails...");
                total += cleanThumbnailCache(
                        callback);

                // Step 3: APK Files
                log(callback,
                        "\n[3/5] APK Files...");
                total += cleanApkFiles(
                        callback);

                // Step 4: Temp Files
                log(callback,
                        "\n[4/5] Temp Files...");
                total += cleanTempFiles(
                        callback);

                // Step 5: Empty Folders
                log(callback,
                        "\n[5/5] Empty Folders...");
                cleanEmptyFolders(callback);

            } catch (Exception e) {
                log(callback,
                        "⚠️ Error: " + e.getMessage());
            }

            final long finalTotal = total;
            log(callback,
                    "\n================================"
                            + "\n✅ CLEAN COMPLETE!"
                            + "\n💾 Total freed: "
                            + formatBytes(finalTotal)
                            + "\n================================");

            if (callback != null) {
                callback.onDone(finalTotal);
            }
        }).start();
    }

    // ─────────────────────────────────────
    // HELPER: Log message
    // ─────────────────────────────────────
    private void log(
            CleanCallback callback,
            String message) {
        Log.d(TAG, message);
        if (callback != null) {
            callback.onLog(message);
        }
    }

    // ─────────────────────────────────────
    // STORAGE INFO MODEL
    // ─────────────────────────────────────
    public static class StorageInfo {
        public long totalBytes = 0;
        public long usedBytes  = 0;
        public long freeBytes  = 0;

        public int getUsedPercent() {
            if (totalBytes == 0) return 0;
            return (int) ((usedBytes * 100)
                    / totalBytes);
        }
    }
}