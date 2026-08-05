package com.sanda.datasaver;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;

/**
 * SettingsActivity — App settings screen.
 * Theme, notifications, auto-start,
 * data limit and contact options.
 */
public class SettingsActivity extends AppCompatActivity {

    // ── Core ──────────────────────────────────
    private PrefsManager prefs;

    // ── Theme ─────────────────────────────────
    private RadioButton  rbSystem;
    private RadioButton  rbDark;
    private RadioButton  rbLight;

    // ── Switches ──────────────────────────────
    private SwitchMaterial switchNotifications;
    private SwitchMaterial switchAutoStart;
    private SwitchMaterial switchDataWarning;

    // ── Data Limit ────────────────────────────
    private TextView    tvDataLimit;
    private LinearLayout rowDataLimit;

    // ─────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = new PrefsManager(this);

        bindViews();
        loadCurrentSettings();
        setupListeners();
    }

    // ─────────────────────────────────────────
    // BIND VIEWS
    // ─────────────────────────────────────────
    private void bindViews() {
        // Back button
        findViewById(R.id.btn_back)
                .setOnClickListener(v -> finish());

        // Theme radio buttons
        rbSystem = findViewById(R.id.rb_system);
        rbDark   = findViewById(R.id.rb_dark);
        rbLight  = findViewById(R.id.rb_light);

        // Switches
        switchNotifications =
                findViewById(R.id.switch_notifications);
        switchAutoStart =
                findViewById(R.id.switch_auto_start);
        switchDataWarning =
                findViewById(R.id.switch_data_warning);

        // Data limit row
        tvDataLimit  = findViewById(R.id.tv_data_limit);
        rowDataLimit = findViewById(R.id.row_data_limit);

        // Contact buttons
        findViewById(R.id.btn_email)
                .setOnClickListener(v -> sendEmail());
        findViewById(R.id.btn_rate)
                .setOnClickListener(v -> rateApp());
        findViewById(R.id.btn_share)
                .setOnClickListener(v -> shareApp());
        findViewById(R.id.btn_privacy)
                .setOnClickListener(v -> openPrivacy());
    }

    // ─────────────────────────────────────────
    // LOAD CURRENT SETTINGS
    // ─────────────────────────────────────────
    private void loadCurrentSettings() {
        // Load theme setting
        int themeMode = prefs.getThemeMode();
        switch (themeMode) {
            case AppCompatDelegate.MODE_NIGHT_YES:
                rbDark.setChecked(true);
                break;
            case AppCompatDelegate.MODE_NIGHT_NO:
                rbLight.setChecked(true);
                break;
            default:
                rbSystem.setChecked(true);
                break;
        }

        // Load switches
        switchNotifications.setChecked(
                prefs.isNotificationsEnabled());
        switchAutoStart.setChecked(
                prefs.isAutoStartEnabled());
        switchDataWarning.setChecked(
                prefs.isDataWarningEnabled());

        // Load data limit
        tvDataLimit.setText(
                prefs.getDailyLimitMb() + " MB");

        // Show/hide data limit row
        rowDataLimit.setVisibility(
                prefs.isDataWarningEnabled()
                        ? View.VISIBLE
                        : View.GONE);
    }

    // ─────────────────────────────────────────
    // SETUP LISTENERS
    // ─────────────────────────────────────────
    private void setupListeners() {

        // ── Theme Selection ───────────────────
        RadioGroup themeGroup =
                findViewById(R.id.theme_group);

        themeGroup.setOnCheckedChangeListener(
                (group, checkedId) -> {
                    int mode;
                    if (checkedId == R.id.rb_dark) {
                        mode = AppCompatDelegate
                                .MODE_NIGHT_YES;
                    } else if (checkedId == R.id.rb_light) {
                        mode = AppCompatDelegate
                                .MODE_NIGHT_NO;
                    } else {
                        mode = AppCompatDelegate
                                .MODE_NIGHT_FOLLOW_SYSTEM;
                    }
                    prefs.setThemeMode(mode);
                    AppCompatDelegate.setDefaultNightMode(mode);
                    Toast.makeText(this,
                            "Theme updated",
                            Toast.LENGTH_SHORT).show();
                    recreate(); // ← ADD THIS ONE LINE
                });

        // ── Notifications ─────────────────────
        switchNotifications
                .setOnCheckedChangeListener(
                        (btn, checked) -> {
                            prefs.setNotificationsEnabled(
                                    checked);
                            Toast.makeText(this,
                                    checked
                                            ? "Notifications ON"
                                            : "Notifications OFF",
                                    Toast.LENGTH_SHORT).show();
                        });

        // ── Auto Start ────────────────────────
        switchAutoStart
                .setOnCheckedChangeListener(
                        (btn, checked) -> {
                            prefs.setAutoStartEnabled(
                                    checked);
                            Toast.makeText(this,
                                    checked
                                            ? "Auto-start ON"
                                            : "Auto-start OFF",
                                    Toast.LENGTH_SHORT).show();
                        });

        // ── Data Warning ──────────────────────
        switchDataWarning
                .setOnCheckedChangeListener(
                        (btn, checked) -> {
                            prefs.setDataWarningEnabled(
                                    checked);
                            rowDataLimit.setVisibility(
                                    checked
                                            ? View.VISIBLE
                                            : View.GONE);
                        });

        // ── Data Limit Row ────────────────────
        rowDataLimit.setOnClickListener(
                v -> showDataLimitPicker());
    }

    // ─────────────────────────────────────────
    // DATA LIMIT PICKER
    // ─────────────────────────────────────────
    private void showDataLimitPicker() {
        String[] options = {
                "100 MB", "250 MB", "500 MB",
                "1 GB",   "2 GB",   "5 GB",
                "Unlimited"
        };
        int[] values = {
                100, 250, 500,
                1024, 2048, 5120,
                -1
        };

        new androidx.appcompat.app.AlertDialog
                .Builder(this)
                .setTitle("Daily Data Limit Warning")
                .setItems(options, (dialog, which) -> {
                    int selected = values[which];
                    prefs.setDailyLimitMb(selected);
                    tvDataLimit.setText(
                            selected == -1
                                    ? "Unlimited"
                                    : options[which]);
                    Toast.makeText(this,
                            "Limit set to " + options[which],
                            Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    // ─────────────────────────────────────────
    // CONTACT ACTIONS
    // ─────────────────────────────────────────
    private void sendEmail() {
        Intent intent = new Intent(
                Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL,
                new String[]{"sandadatasaver@gmail.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT,
                "Sanda Data Saver - Support");
        try {
            startActivity(Intent.createChooser(
                    intent, "Send Email"));
        } catch (Exception e) {
            Toast.makeText(this,
                    "No email app found",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void rateApp() {
        try {
            startActivity(new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id="
                            + getPackageName())));
        } catch (Exception e) {
            startActivity(new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                            "https://play.google.com/store/apps/details?id="
                                    + getPackageName())));
        }
    }

    private void shareApp() {
        Intent intent = new Intent(
                Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT,
                Constants.APP_NAME);
        intent.putExtra(Intent.EXTRA_TEXT,
                "Check out " + Constants.APP_NAME +
                        " by " + Constants.APP_AUTHOR +
                        "!\n\nSave your mobile hotspot data" +
                        " with one tap.\n\n" +
                        "https://play.google.com/store/apps" +
                        "/details?id=" + getPackageName()
        );
        startActivity(Intent.createChooser(
                intent, "Share " + Constants.APP_NAME));
    }

    private void openPrivacy() {
        new androidx.appcompat.app.AlertDialog
                .Builder(this)
                .setTitle("Privacy Policy")
                .setMessage(
                        Constants.APP_NAME + " Privacy Policy\n\n" +
                                "This app does NOT:\n" +
                                "❌ Collect personal data\n" +
                                "❌ Send data to any server\n" +
                                "❌ Track your location\n" +
                                "❌ Read messages or contacts\n\n" +
                                "This app DOES:\n" +
                                "✅ Read data usage stats (on-device only)\n" +
                                "✅ Monitor network state\n" +
                                "✅ Run a background service\n\n" +
                                "All data stays on your device.\n\n" +
                                Constants.APP_COPYRIGHT
                )
                .setPositiveButton("Close", null)
                .show();
    }   // ← closes openPrivacy()

}   // ← THIS MUST BE THE LAST LINE - closes the class