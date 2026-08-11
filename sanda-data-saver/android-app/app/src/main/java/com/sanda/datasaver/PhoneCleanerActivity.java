package com.sanda.datasaver;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

/**
 * PhoneCleanerActivity — Android Phone Cleaner
 * Translates the PC cleaner logic into Android.
 * Safely removes junk files to free space.
 */
public class PhoneCleanerActivity
        extends AppCompatActivity
        implements PhoneCleanerHelper
        .CleanCallback {

    // ── UI ────────────────────────────────
    private TextView       tvLog;
    private TextView       tvCleanStatus;
    private TextView       tvUsedStorage;
    private TextView       tvFreeStorage;
    private TextView       tvTotalStorage;
    private TextView       tvCleanable;
    private TextView       tvResult;
    private ProgressBar    cleanProgress;
    private ProgressBar    storageProgress;
    private MaterialButton btnQuickClean;
    private MaterialButton btnAnalyze;
    private ScrollView     scrollLog;
    private View           resultBanner;

    // ── Core ──────────────────────────────
    private PhoneCleanerHelper cleaner;
    private Handler            handler;
    private boolean            isCleaning = false;

    // ─────────────────────────────────────
    @Override
    protected void onCreate(
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(
                R.layout.activity_phone_cleaner);

        cleaner = new PhoneCleanerHelper(this);
        handler = new Handler(
                Looper.getMainLooper());

        bindViews();
        loadStorageInfo();
        estimateCleanSize();
    }

    // ─────────────────────────────────────
    // BIND VIEWS
    // ─────────────────────────────────────
    private void bindViews() {
        tvLog           =
                findViewById(R.id.tv_log);
        tvCleanStatus   =
                findViewById(R.id.tv_clean_status);
        tvUsedStorage   =
                findViewById(R.id.tv_used_storage);
        tvFreeStorage   =
                findViewById(R.id.tv_free_storage);
        tvTotalStorage  =
                findViewById(R.id.tv_total_storage);
        tvCleanable     =
                findViewById(R.id.tv_cleanable);
        tvResult        =
                findViewById(R.id.tv_result);
        cleanProgress   =
                findViewById(R.id.clean_progress);
        storageProgress =
                findViewById(R.id.storage_progress);
        btnQuickClean   =
                findViewById(R.id.btn_quick_clean);
        btnAnalyze      =
                findViewById(R.id.btn_analyze);
        scrollLog       =
                findViewById(R.id.scroll_log);
        resultBanner    =
                findViewById(R.id.result_banner);

        // Back button
        findViewById(R.id.btn_back)
                .setOnClickListener(v -> finish());

        // Quick Clean button
        btnQuickClean.setOnClickListener(
                v -> startFullClean());

        // Analyze/Scan button
        btnAnalyze.setOnClickListener(
                v -> runAnalysis());
    }

    // ─────────────────────────────────────
    // LOAD STORAGE INFO
    // ─────────────────────────────────────
    private void loadStorageInfo() {
        new Thread(() -> {
            PhoneCleanerHelper.StorageInfo info
                    = cleaner.getStorageInfo();
            handler.post(() -> {
                tvUsedStorage.setText(
                        PhoneCleanerHelper
                                .formatBytes(
                                        info.usedBytes));
                tvFreeStorage.setText(
                        PhoneCleanerHelper
                                .formatBytes(
                                        info.freeBytes));
                tvTotalStorage.setText(
                        PhoneCleanerHelper
                                .formatBytes(
                                        info.totalBytes));
                storageProgress.setProgress(
                        info.getUsedPercent());
            });
        }).start();
    }

    // ─────────────────────────────────────
    // ESTIMATE CLEANABLE SIZE
    // ─────────────────────────────────────
    private void estimateCleanSize() {
        tvCleanable.setText(
                "Calculating cleanable space...");
        new Thread(() -> {
            long size =
                    cleaner.estimateCleanableSize();
            handler.post(() -> {
                if (size > 0) {
                    tvCleanable.setText(
                            "🟢 ~"
                                    + PhoneCleanerHelper
                                    .formatBytes(size)
                                    + " can be freed");
                } else {
                    tvCleanable.setText(
                            "✅ Phone looks clean!");
                }
            });
        }).start();
    }

    // ─────────────────────────────────────
// RUN ANALYSIS ONLY
// Shows what will be cleaned
// without actually cleaning
// ─────────────────────────────────────
private void runAnalysis() {
    if (isCleaning) return;

    tvLog.setText(
        "🔍 Scanning your phone...\n\n");
    tvCleanStatus.setText("Scanning...");
    resultBanner.setVisibility(View.GONE);
    cleanProgress.setVisibility(
        View.VISIBLE);

    new Thread(() -> {
        // Calculate cache size
        long cacheSize =
            PhoneCleanerHelper.getDirSize(
                getCacheDir());

        // Calculate external cache size
        // Must be final for lambda
        long extCache = 0;
        if (getExternalCacheDir() != null) {
            extCache =
                PhoneCleanerHelper
                    .getDirSize(
                        getExternalCacheDir());
        }

        // Make final for use in lambda
        final long finalCacheSize =
            cacheSize + extCache;

        final long totalEstimate =
            cleaner.estimateCleanableSize();

        handler.post(() -> {
            cleanProgress.setVisibility(
                View.GONE);
            tvCleanStatus.setText(
                "Scan complete!");

            String report =
                "📊 SCAN RESULTS\n"
                + "─────────────────\n\n"
                + "App Cache:\n  "
                + PhoneCleanerHelper
                    .formatBytes(
                        finalCacheSize)
                + "\n\n"
                + "APK Files + Other Junk:\n"
                + "  Included in estimate\n\n"
                + "─────────────────\n"
                + "Estimated Total:\n  ~"
                + PhoneCleanerHelper
                    .formatBytes(
                        totalEstimate)
                + "\n\n"
                + "Tap Quick Clean to\n"
                + "remove all junk now!";

            tvLog.setText(report);
            tvCleanable.setText(
                "🟢 ~"
                + PhoneCleanerHelper
                    .formatBytes(
                        totalEstimate)
                + " can be freed");
        });
    }).start();
}

    // ─────────────────────────────────────
    // START FULL CLEAN
    // Same as PC: run_full_clean()
    // ─────────────────────────────────────
 private void startFullClean() {
    if (isCleaning) return;
    isCleaning = true;

    // Clear log
    tvLog.setText("");
    resultBanner.setVisibility(View.GONE);
    cleanProgress.setVisibility(
        View.VISIBLE);
    tvCleanStatus.setText(
        "Cleaning in progress...");

    btnQuickClean.setEnabled(false);
    btnQuickClean.setText(
        "⏳  Cleaning...");
    btnQuickClean.setTextColor(
        getColor(R.color.bg_primary));
    btnAnalyze.setEnabled(false);

    // Run full clean
    cleaner.runFullClean(this);
}

    // ─────────────────────────────────────
    // CLEAN CALLBACK — onLog
    // Called for each log message
    // ─────────────────────────────────────
    @Override
    public void onLog(String message) {
        handler.post(() -> {
            tvLog.append(message + "\n");
            // Auto scroll to bottom
            scrollLog.post(() ->
                    scrollLog.fullScroll(
                            View.FOCUS_DOWN));
        });
    }

    // ─────────────────────────────────────
    // CLEAN CALLBACK — onDone
    // Called when cleaning is complete
    // ─────────────────────────────────────
    @Override
public void onDone(long totalFreed) {
    handler.post(() -> {
        isCleaning = false;

        cleanProgress.setVisibility(
            View.GONE);
        tvCleanStatus.setText(
            "✅ Cleaning complete!");

        btnQuickClean.setEnabled(true);
        btnQuickClean.setText(
            "🚀  Quick Clean");
        btnQuickClean.setTextColor(
            getColor(R.color.bg_primary));
        btnAnalyze.setEnabled(true);

        // Show result banner
        resultBanner.setVisibility(
            View.VISIBLE);
        tvResult.setText(
            "✅ Done! Freed "
            + PhoneCleanerHelper
                .formatBytes(totalFreed)
            + "\nRestart apps for "
            + "best results!");

        // Refresh storage info
        loadStorageInfo();
        estimateCleanSize();

        Toast.makeText(
            PhoneCleanerActivity.this,
            "🧹 Cleaned "
            + PhoneCleanerHelper
                .formatBytes(totalFreed)
            + " of junk!",
            Toast.LENGTH_LONG).show();
    });
}

    // ─────────────────────────────────────
    // CLEAN CALLBACK — onError
    // ─────────────────────────────────────
    @Override
    public void onError(String error) {
        handler.post(() -> {
            onLog("❌ Error: " + error);
        });
    }
}