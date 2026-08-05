package com.sanda.datasaver;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

/**
 * ScheduleActivity
 * Lets users set:
 * 1. Data usage alert limits
 * 2. Auto schedule timer
 */
public class ScheduleActivity
        extends AppCompatActivity {

    // ── UI — Alerts ───────────────────────
    private SwitchMaterial switchAlerts;
    private TextView       tvUsageSummary;
    private TextView       tvCurrentLimit;
    private ProgressBar    usageAlertProgress;
    private EditText       etCustomLimit;
    private MaterialButton btnLimit500;
    private MaterialButton btnLimit1gb;
    private MaterialButton btnLimit2gb;
    private MaterialButton btnLimit5gb;

    // ── UI — Schedule ─────────────────────
    private SwitchMaterial switchSchedule;
    private MaterialButton btnOnTime;
    private MaterialButton btnOffTime;
    private MaterialButton btnSaveSchedule;
    private TextView       tvScheduleStatus;

    // ── Day Buttons ───────────────────────
    private MaterialButton btnMon;
    private MaterialButton btnTue;
    private MaterialButton btnWed;
    private MaterialButton btnThu;
    private MaterialButton btnFri;
    private MaterialButton btnSat;
    private MaterialButton btnSun;

    // ── Core ──────────────────────────────
    private SchedulerManager      scheduler;
    private DataUsageAlertManager alertManager;
    private DataUsageHelper       usageHelper;
    private Handler               handler;

    // ── State ─────────────────────────────
    private int     onHour    = 8;
    private int     onMinute  = 0;
    private int     offHour   = 18;
    private int     offMinute = 0;
    private boolean[] days    =
            {true, true, true,
                    true, true, false, false};
    private int selectedLimitMb = 1024;

    // ─────────────────────────────────────
    @Override
    protected void onCreate(
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(
                R.layout.activity_schedule);

        scheduler    =
                new SchedulerManager(this);
        alertManager =
                new DataUsageAlertManager(this);
        usageHelper  =
                new DataUsageHelper(this);
        handler      =
                new Handler(
                        Looper.getMainLooper());

        bindViews();
        loadCurrentSettings();
        loadUsageData();
    }

    // ─────────────────────────────────────
    // BIND VIEWS
    // ─────────────────────────────────────
    private void bindViews() {
        // Back button
        findViewById(R.id.btn_back)
                .setOnClickListener(
                        v -> finish());

        // ── Alert Views ───────────────────
        switchAlerts =
                findViewById(R.id.switch_alerts);
        tvUsageSummary =
                findViewById(
                        R.id.tv_usage_summary);
        tvCurrentLimit =
                findViewById(
                        R.id.tv_current_limit);
        usageAlertProgress =
                findViewById(
                        R.id.usage_alert_progress);
        etCustomLimit =
                findViewById(
                        R.id.et_custom_limit);
        btnLimit500 =
                findViewById(R.id.btn_limit_500);
        btnLimit1gb =
                findViewById(R.id.btn_limit_1gb);
        btnLimit2gb =
                findViewById(R.id.btn_limit_2gb);
        btnLimit5gb =
                findViewById(R.id.btn_limit_5gb);

        // Limit button listeners
        btnLimit500.setOnClickListener(
                v -> selectLimit(500));
        btnLimit1gb.setOnClickListener(
                v -> selectLimit(1024));
        btnLimit2gb.setOnClickListener(
                v -> selectLimit(2048));
        btnLimit5gb.setOnClickListener(
                v -> selectLimit(5120));

        // Alert switch
        switchAlerts
                .setOnCheckedChangeListener(
                        (btn, checked) -> {
                            alertManager.setEnabled(
                                    checked);
                            if (checked) {
                                alertManager
                                        .setDailyLimitMb(
                                                selectedLimitMb);
                                Toast.makeText(this,
                                                "✅ Data alerts enabled!",
                                                Toast.LENGTH_SHORT)
                                        .show();
                            } else {
                                Toast.makeText(this,
                                                "Data alerts disabled",
                                                Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });

        // ── Schedule Views ────────────────
        switchSchedule =
                findViewById(
                        R.id.switch_schedule);
        btnOnTime =
                findViewById(R.id.btn_on_time);
        btnOffTime =
                findViewById(R.id.btn_off_time);
        btnSaveSchedule =
                findViewById(
                        R.id.btn_save_schedule);
        tvScheduleStatus =
                findViewById(
                        R.id.tv_schedule_status);

        // Day buttons
        btnMon =
                findViewById(R.id.btn_day_mon);
        btnTue =
                findViewById(R.id.btn_day_tue);
        btnWed =
                findViewById(R.id.btn_day_wed);
        btnThu =
                findViewById(R.id.btn_day_thu);
        btnFri =
                findViewById(R.id.btn_day_fri);
        btnSat =
                findViewById(R.id.btn_day_sat);
        btnSun =
                findViewById(R.id.btn_day_sun);

        // Time picker buttons
        btnOnTime.setOnClickListener(
                v -> showTimePicker(true));
        btnOffTime.setOnClickListener(
                v -> showTimePicker(false));

        // Day toggle buttons
        setupDayButton(btnMon, 0);
        setupDayButton(btnTue, 1);
        setupDayButton(btnWed, 2);
        setupDayButton(btnThu, 3);
        setupDayButton(btnFri, 4);
        setupDayButton(btnSat, 5);
        setupDayButton(btnSun, 6);

        // Save schedule button
        btnSaveSchedule.setOnClickListener(
                v -> saveSchedule());

        // Schedule switch
        switchSchedule
                .setOnCheckedChangeListener(
                        (btn, checked) -> {
                            updateScheduleStatus();
                        });
    }

    // ─────────────────────────────────────
    // SETUP DAY BUTTON TOGGLE
    // ─────────────────────────────────────
    private void setupDayButton(
            MaterialButton btn, int dayIndex) {
        btn.setOnClickListener(v -> {
            days[dayIndex] =
                    !days[dayIndex];
            updateDayButton(
                    btn, days[dayIndex]);
        });
    }

    private void updateDayButton(
            MaterialButton btn,
            boolean active) {
        btn.setBackgroundTintList(
                android.content.res
                        .ColorStateList.valueOf(
                                active
                                        ? getColor(
                                        R.color.color_primary)
                                        : getColor(
                                        R.color.bg_tertiary)));
    }

    // ─────────────────────────────────────
    // SHOW TIME PICKER
    // ─────────────────────────────────────
    private void showTimePicker(
            boolean isOnTime) {
        int hour   = isOnTime
                ? onHour : offHour;
        int minute = isOnTime
                ? onMinute : offMinute;

        TimePickerDialog picker =
                new TimePickerDialog(
                        this,
                        (view, h, m) -> {
                            if (isOnTime) {
                                onHour   = h;
                                onMinute = m;
                                btnOnTime.setText(
                                        SchedulerManager
                                                .formatTime(
                                                        h, m));
                            } else {
                                offHour   = h;
                                offMinute = m;
                                btnOffTime.setText(
                                        SchedulerManager
                                                .formatTime(
                                                        h, m));
                            }
                            updateScheduleStatus();
                        },
                        hour, minute, false);

        picker.setTitle(isOnTime
                ? "Turn ON Data Saver at:"
                : "Turn OFF Data Saver at:");
        picker.show();
    }

    // ─────────────────────────────────────
    // SELECT LIMIT
    // ─────────────────────────────────────
    private void selectLimit(int mb) {
        selectedLimitMb = mb;
        alertManager.setDailyLimitMb(mb);
        updateLimitButtons();
        updateCurrentLimitDisplay();
        Toast.makeText(this,
                "Daily limit set to "
                        + (mb >= 1024
                        ? (mb / 1024) + " GB"
                        : mb + " MB"),
                Toast.LENGTH_SHORT).show();
    }

    private void updateLimitButtons() {
        int primary = getColor(
                R.color.color_primary);
        int inactive = getColor(
                R.color.bg_tertiary);

        btnLimit500.setBackgroundTintList(
                android.content.res
                        .ColorStateList.valueOf(
                                selectedLimitMb == 500
                                        ? primary : inactive));
        btnLimit1gb.setBackgroundTintList(
                android.content.res
                        .ColorStateList.valueOf(
                                selectedLimitMb == 1024
                                        ? primary : inactive));
        btnLimit2gb.setBackgroundTintList(
                android.content.res
                        .ColorStateList.valueOf(
                                selectedLimitMb == 2048
                                        ? primary : inactive));
        btnLimit5gb.setBackgroundTintList(
                android.content.res
                        .ColorStateList.valueOf(
                                selectedLimitMb == 5120
                                        ? primary : inactive));
    }

    private void updateCurrentLimitDisplay() {
        String limitText;
        if (selectedLimitMb >= 1024) {
            limitText = (selectedLimitMb
                    / 1024) + " GB";
        } else {
            limitText = selectedLimitMb
                    + " MB";
        }
        tvCurrentLimit.setText(
                "✅ Current limit: " + limitText);
    }

    // ─────────────────────────────────────
    // UPDATE SCHEDULE STATUS
    // ─────────────────────────────────────
    private void updateScheduleStatus() {
        boolean enabled =
                switchSchedule.isChecked();
        if (enabled) {
            tvScheduleStatus.setText(
                    "⏰ Schedule: ON at "
                            + SchedulerManager
                            .formatTime(
                                    onHour, onMinute)
                            + "  |  OFF at "
                            + SchedulerManager
                            .formatTime(
                                    offHour, offMinute));
            tvScheduleStatus.setTextColor(
                    getColor(
                            R.color.color_success));
        } else {
            tvScheduleStatus.setText(
                    "Schedule is OFF");
            tvScheduleStatus.setTextColor(
                    getColor(
                            R.color.text_muted));
        }
    }

    // ─────────────────────────────────────
    // SAVE SCHEDULE
    // ─────────────────────────────────────
    private void saveSchedule() {
        boolean enabled =
                switchSchedule.isChecked();

        // Check custom limit
        String customText =
                etCustomLimit.getText()
                        .toString().trim();
        if (!customText.isEmpty()) {
            try {
                int customMb =
                        Integer.parseInt(
                                customText);
                if (customMb > 0) {
                    selectLimit(customMb);
                }
            } catch (NumberFormatException e) {
                // Ignore invalid input
            }
        }

        // Save alert settings
        alertManager.setEnabled(
                switchAlerts.isChecked());
        alertManager.setDailyLimitMb(
                selectedLimitMb);

        // Save schedule
        scheduler.saveSchedule(
                enabled,
                onHour, onMinute,
                offHour, offMinute,
                days);

        updateScheduleStatus();

        Toast.makeText(this,
                enabled
                        ? "✅ Schedule saved! Data Saver"
                          + " will activate at "
                          + SchedulerManager
                            .formatTime(
                                    onHour, onMinute)
                        : "✅ Settings saved!",
                Toast.LENGTH_LONG).show();
    }

    // ─────────────────────────────────────
    // LOAD CURRENT SETTINGS
    // ─────────────────────────────────────
    private void loadCurrentSettings() {
        // Load alert settings
        switchAlerts.setChecked(
                alertManager.isEnabled());
        selectedLimitMb =
                alertManager.getDailyLimitMb();
        updateLimitButtons();
        updateCurrentLimitDisplay();

        // Load schedule settings
        switchSchedule.setChecked(
                scheduler.isEnabled());
        onHour    = scheduler.getOnHour();
        onMinute  = scheduler.getOnMinute();
        offHour   = scheduler.getOffHour();
        offMinute = scheduler.getOffMinute();
        days      = scheduler.getActiveDays();

        // Update time buttons
        btnOnTime.setText(
                SchedulerManager.formatTime(
                        onHour, onMinute));
        btnOffTime.setText(
                SchedulerManager.formatTime(
                        offHour, offMinute));

        // Update day buttons
        MaterialButton[] dayBtns = {
                btnMon, btnTue, btnWed,
                btnThu, btnFri, btnSat, btnSun
        };
        for (int i = 0; i < 7; i++) {
            updateDayButton(
                    dayBtns[i], days[i]);
        }

        updateScheduleStatus();
    }

    // ─────────────────────────────────────
    // LOAD USAGE DATA
    // ─────────────────────────────────────
    private void loadUsageData() {
        new Thread(() -> {
            long usedBytes =
                    usageHelper
                            .getTotalMobileBytesToday();
            long usedMb =
                    usedBytes / (1024 * 1024);
            int limitMb =
                    alertManager.getDailyLimitMb();
            int percent = limitMb > 0
                    ? (int) ((usedMb * 100)
                             / limitMb)
                    : 0;

            final String summary =
                    usedMb + " MB used of "
                            + limitMb + " MB ("
                            + Math.min(percent, 100)
                            + "%)";
            final int finalPercent =
                    Math.min(percent, 100);

            handler.post(() -> {
                tvUsageSummary.setText(
                        summary);
                usageAlertProgress
                        .setProgress(
                                finalPercent);

                // Change color based on level
                int color;
                if (finalPercent >= 80) {
                    color = getColor(
                            R.color.color_accent);
                } else if (
                        finalPercent >= 50) {
                    color = getColor(
                            R.color.color_warning);
                } else {
                    color = getColor(
                            R.color.color_primary);
                }
                usageAlertProgress
    .setProgressTintList(
    android.content.res
        .ColorStateList
        .valueOf(color));
            });
        }).start();
    }
}