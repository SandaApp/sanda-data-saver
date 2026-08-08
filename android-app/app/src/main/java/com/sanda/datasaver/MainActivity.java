package com.sanda.datasaver;  
  
import android.Manifest;  
import android.app.AppOpsManager;  
import android.content.Intent;  
import android.content.pm.PackageManager;  
import android.net.VpnService;  
import android.os.Build;  
import android.os.Bundle;  
import android.os.Handler;  
import android.os.Looper;  
import android.provider.Settings;  
import android.view.View;  
import android.view.animation.AnimationUtils;  
import android.widget.ImageView;  
import android.widget.LinearLayout;  
import android.widget.TextView;  
import android.widget.Toast;  
  
import androidx.appcompat.app.AppCompatActivity;  
import androidx.appcompat.app.AppCompatDelegate;  
import androidx.core.app.ActivityCompat;  
import androidx.core.content.ContextCompat;  
  
import com.google.android.material.card.MaterialCardView;  
import com.google.android.material.switchmaterial.SwitchMaterial;  
  
import java.util.List;  
  
/**  
 * MainActivity — The main screen of Sanda Data Saver.  
 * Shows the big toggle, data usage summary, and quick settings.  
 */  
public class MainActivity  
        extends AppCompatActivity {  
  
    // ── UI Elements ───────────────────────  
    private SwitchMaterial   mainSwitch;  
    private TextView         tvStatus;  
    private TextView         tvStatusDetail;  
    private TextView         tvTotalUsage;  
    private TextView         tvMobileUsage;  
    private LinearLayout     usageList;  
    private ImageView        ivLogo;  
    private MaterialCardView cardStatus;  
  
    // ── Core ──────────────────────────────  
    private DataSaverManager manager;  
    private PrefsManager     prefs;  
    private DataUsageHelper  usageHelper;  
    private Handler          handler;  
  
    // ── Permission Request Codes ──────────  
    private static final int REQ_USAGE_STATS  = 100;  
    private static final int REQ_NOTIFICATIONS = 101;  
    private static final int REQ_VPN           = 200;  
  
    // ── State ─────────────────────────────  
    private boolean pendingActivation = false;  
    private boolean isUpdatingUI      = false;  
  
    // ─────────────────────────────────────  
    @Override  
    protected void onCreate(Bundle savedInstanceState) {  
  
        // ── Apply saved theme FIRST ───────  
        try {  
            PrefsManager tmpPrefs = new PrefsManager(this);  
            int themeMode = tmpPrefs.getThemeMode();  
            if (themeMode == -1) {  
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);  
            } else {  
                AppCompatDelegate.setDefaultNightMode(themeMode);  
            }  
        } catch (Exception e) {  
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);  
        }  
        // ─────────────────────────────────  
  
        super.onCreate(savedInstanceState);  
        // FIX: Show new Sanda logo splash longer (was too fast) - 1.2 sec delay so user sees new logo
        try { Thread.sleep(2500); } catch (InterruptedException ignored) {}
        setContentView(R.layout.activity_main);  
  
        // Initialize core objects  
        manager     = new DataSaverManager(this);  
        prefs       = new PrefsManager(this);  
        usageHelper = new DataUsageHelper(this);  
        handler     = new Handler(Looper.getMainLooper());  
  
        bindViews();
        checkPermissions();
        setupToggle();
        updateUI(prefs.isDataSaverOn());
        loadDataUsage();
        startBackgroundService();

        // Start Health Reminders (distract excessive gaming/social, focus on scripture/prayer)
        try {
            HealthReminderManager hm = new HealthReminderManager(this);
            if (hm.isEnabled()) {
                hm.scheduleNext();
            }
        } catch (Exception e) {
            // Ignore health init errors
        }
    }
  
    // ─────────────────────────────────────  
    // BIND VIEWS  
    // ─────────────────────────────────────  
    private void bindViews() {  
        mainSwitch     = findViewById(R.id.main_switch);  
        tvStatus       = findViewById(R.id.tv_status);  
        tvStatusDetail = findViewById(R.id.tv_status_detail);  
        tvTotalUsage   = findViewById(R.id.tv_total_usage);  
        tvMobileUsage  = findViewById(R.id.tv_mobile_usage);  
        usageList      = findViewById(R.id.usage_list);  
        ivLogo         = findViewById(R.id.iv_logo);  
        cardStatus     = findViewById(R.id.card_status);  
  
        // Manage Blocked Apps button  
        findViewById(R.id.btn_manage_apps)  
                .setOnClickListener(v ->  
                        startActivity(new Intent(  
                                this,  
                                AppListActivity.class)));  
  
        // Data Usage button  
        findViewById(R.id.btn_data_usage)  
                .setOnClickListener(v ->  
                        startActivity(new Intent(  
                                this,  
                                DataUsageActivity.class)));  
  
        // Settings button  
        findViewById(R.id.btn_settings)  
                .setOnClickListener(v ->  
                        showSettings());  
  
        // About button  
        findViewById(R.id.btn_about)  
                .setOnClickListener(v ->  
                        showAbout());  
  
        // Phone Cleaner button  
        findViewById(R.id.btn_phone_cleaner)  
                .setOnClickListener(v ->  
                        startActivity(new Intent(  
                                this,  
                                PhoneCleanerActivity.class)));  
  
        // Schedule and Alerts button
        findViewById(R.id.btn_schedule)
                .setOnClickListener(v ->
                        startActivity(new Intent(
                                this,
                                ScheduleActivity.class)));

        // Health Reminders button - NEW
        findViewById(R.id.btn_health)
                .setOnClickListener(v ->
                        startActivity(new Intent(
                                this,
                                HealthActivity.class)));
    }
  
    // ─────────────────────────────────────  
    // TOGGLE SETUP  
    // ─────────────────────────────────────  
    private void setupToggle() {  
        // Set initial state without triggering the listener  
        isUpdatingUI = true;  
        mainSwitch.setChecked(prefs.isDataSaverOn());  
        isUpdatingUI = false;  
  
        mainSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {  
            // Ignore if we are just updating UI programmatically  
            if (isUpdatingUI) return;  
  
            if (isChecked) {  
                // Check VPN permission  
                Intent vpnIntent = VpnService.prepare(this);  
                if (vpnIntent != null) {  
                    pendingActivation = true;  
                    startActivityForResult(vpnIntent, REQ_VPN);  
                } else {  
                    activateDataSaver();  
                }  
            } else {  
                deactivateDataSaver();  
            }  
        });  
    }  
  
    // ─────────────────────────────────────  
    // VPN PERMISSION RESULT  
    // ─────────────────────────────────────  
    @Override  
    protected void onActivityResult(  
            int requestCode,  
            int resultCode,  
            Intent data) {  
        super.onActivityResult(requestCode, resultCode, data);  
  
        if (requestCode == REQ_VPN) {  
            if (resultCode == RESULT_OK) {  
                activateDataSaver();  
            } else {  
                pendingActivation = false;  
                isUpdatingUI = true;  
                mainSwitch.setChecked(false);  
                isUpdatingUI = false;  
                Toast.makeText(this,  
                    "⚠️ VPN permission needed to block apps automatically.",  
                    Toast.LENGTH_LONG).show();  
            }  
        }  
  
        if (requestCode == REQ_USAGE_STATS) {  
            loadDataUsage();  
        }  
    }  
  
    // ─────────────────────────────────────  
    // ACTIVATE DATA SAVER  
    // ─────────────────────────────────────  
    private void activateDataSaver() {  
        ivLogo.startAnimation(  
            AnimationUtils.loadAnimation(this, R.anim.pulse));  
  
        new Thread(() -> {  
            manager.activate();  
            handler.post(() -> {  
                updateUI(true);  
                Toast.makeText(this,  
                    "🛡️ Data Saver ON — apps blocked automatically!",  
                    Toast.LENGTH_SHORT).show();  
            });  
        }).start();  
    }  
  
    // ─────────────────────────────────────  
    // DEACTIVATE DATA SAVER  
    // ─────────────────────────────────────  
    private void deactivateDataSaver() {  
        ivLogo.startAnimation(  
            AnimationUtils.loadAnimation(this, R.anim.pulse));  
  
        new Thread(() -> {  
            manager.deactivate();  
            handler.post(() -> {  
                updateUI(false);  
                Toast.makeText(this,  
                    "✅ Data Saver OFF",  
                    Toast.LENGTH_SHORT).show();  
            });  
        }).start();  
    }  
  
    // ─────────────────────────────────────  
    // UPDATE UI  
    // ─────────────────────────────────────  
    private void updateUI(boolean isOn) {  
        // Set flag so onCheckedChanged does NOT fire  
        isUpdatingUI = true;  
  
        if (isOn) {  
            tvStatus.setText("DATA SAVER: ON");  
            tvStatus.setTextColor(getColor(R.color.color_success));  
            tvStatusDetail.setText("🛡️ Your hotspot data is protected");  
            cardStatus.setCardBackgroundColor(getColor(R.color.card_active));  
        } else {  
            tvStatus.setText("DATA SAVER: OFF");  
            tvStatus.setTextColor(getColor(R.color.color_accent));  
            tvStatusDetail.setText("Tap the switch to protect your data");  
            cardStatus.setCardBackgroundColor(getColor(R.color.card_inactive));  
        }  
  
        mainSwitch.setChecked(isOn);  
  
        // Reset flag after small delay  
        handler.postDelayed(() -> {  
            isUpdatingUI = false;  
        }, 200);  
    }  
  
    // ─────────────────────────────────────  
    // LOAD DATA USAGE  
    // ─────────────────────────────────────  
    private void loadDataUsage() {  
        if (!hasUsageStatsPermission()) {  
            tvTotalUsage.setText("Grant usage permission to see stats");  
            return;  
        }  
  
        new Thread(() -> {  
            List<DataUsageHelper.AppDataUsage> usageData = usageHelper.getTodayUsage();  
            long totalMobile = usageHelper.getTotalMobileBytesToday();  
  
            handler.post(() -> {  
                tvMobileUsage.setText("Mobile data today: " + formatBytes(totalMobile));  
  
                this.usageList.removeAllViews();  
                int count = Math.min(5, usageData.size());  
                for (int i = 0; i < count; i++) {  
                    addUsageRow(usageData.get(i));  
                }  
            });  
        }).start();  
    }  
  
    private void addUsageRow(DataUsageHelper.AppDataUsage app) {  
        View row = getLayoutInflater().inflate(R.layout.item_usage_row, usageList, false);  
  
        ImageView icon = row.findViewById(R.id.app_icon);  
        TextView name  = row.findViewById(R.id.app_name);  
        TextView usage = row.findViewById(R.id.app_usage);  
  
        icon.setImageDrawable(app.appIcon);  
        name.setText(app.appName);  
        usage.setText(app.getReadableSize());  
  
        usageList.addView(row);  
    }  
  
    // ─────────────────────────────────────  
    // PERMISSIONS  
    // ─────────────────────────────────────  
    private void checkPermissions() {  
        // Only ask for usage permission once  
        boolean alreadyAsked = prefs.getPrefs().getBoolean("usage_asked", false);  
  
        if (!hasUsageStatsPermission() && !alreadyAsked) {  
            showUsagePermissionDialog();  
            prefs.getPrefs().edit().putBoolean("usage_asked", true).apply();  
        }  
  
        // Notification permission Android 13+  
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {  
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)  
                    != PackageManager.PERMISSION_GRANTED) {  
                ActivityCompat.requestPermissions(this,  
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},  
                    REQ_NOTIFICATIONS);  
            }  
        }  
    }  
  
    private boolean hasUsageStatsPermission() {  
        try {  
            AppOpsManager appOps = (AppOpsManager) getSystemService(APP_OPS_SERVICE);  
            int mode = appOps.checkOpNoThrow(  
                AppOpsManager.OPSTR_GET_USAGE_STATS,  
                android.os.Process.myUid(),  
                getPackageName());  
            return mode == AppOpsManager.MODE_ALLOWED;  
        } catch (Exception e) {  
            return false;  
        }  
    }  
  
    private void showUsagePermissionDialog() {  
        new androidx.appcompat.app.AlertDialog.Builder(this)  
            .setTitle("Permission Required")  
            .setMessage("Sanda Data Saver needs Usage Access permission to show your data usage per app.\n\nTap OK to open settings, then find \"Sanda Data Saver\" and enable it.")  
            .setPositiveButton("OK", (d, w) -> {  
                Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);  
                startActivityForResult(intent, REQ_USAGE_STATS);  
            })  
            .setNegativeButton("Skip", null)  
            .show();  
    }  
  
    // ─────────────────────────────────────  
    // SETTINGS  
    // ─────────────────────────────────────  
    private void showSettings() {  
        startActivity(new Intent(this, SettingsActivity.class));  
    }  
  
    // ─────────────────────────────────────  
    // ABOUT DIALOG (Tweak 4 Updated!)  
    // ─────────────────────────────────────  
    private void showAbout() {  
        android.app.Dialog dialog = new android.app.Dialog(this);  
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);  
        dialog.setCancelable(true);  
  
        // Main Root container matches full screen and uses a 10% transparent dark background (#E60D1117)  
        android.widget.LinearLayout root = new android.widget.LinearLayout(this);  
        root.setOrientation(android.widget.LinearLayout.VERTICAL);  
        root.setBackgroundColor(android.graphics.Color.parseColor("#E60D1117"));  
        root.setPadding(60, 100, 60, 60);  
        root.setGravity(android.view.Gravity.CENTER_HORIZONTAL);  
  
        // Wrap elements inside a ScrollView so text remains perfectly legible and scrollable on any device resolution  
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);  
        android.widget.LinearLayout.LayoutParams scrollParams = new android.widget.LinearLayout.LayoutParams(  
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);  
        scrollView.setLayoutParams(scrollParams);  
        scrollView.setVerticalScrollBarEnabled(false);  
  
        android.widget.LinearLayout container = new android.widget.LinearLayout(this);  
        container.setOrientation(android.widget.LinearLayout.VERTICAL);  
        container.setGravity(android.view.Gravity.CENTER_HORIZONTAL);  
  
        // App Icon - New Sanda Logo  
        android.widget.ImageView icon = new android.widget.ImageView(this);  
        icon.setImageResource(R.drawable.sanda_logo);  
        android.widget.LinearLayout.LayoutParams iconParams = new android.widget.LinearLayout.LayoutParams(160, 160);  
        iconParams.gravity = android.view.Gravity.CENTER;  
        iconParams.bottomMargin = 40;  
        icon.setLayoutParams(iconParams);  
        container.addView(icon);  
  
        // App Name  
        android.widget.TextView tvName = new android.widget.TextView(this);  
        tvName.setText(Constants.APP_NAME);  
        tvName.setTextColor(android.graphics.Color.parseColor("#00C9FF"));  
        tvName.setTextSize(26);  
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);  
        tvName.setGravity(android.view.Gravity.CENTER);  
        tvName.setPadding(0, 0, 0, 8);  
        container.addView(tvName);  
  
        // Tagline  
        android.widget.TextView tvTagline = new android.widget.TextView(this);  
        tvTagline.setText(Constants.APP_TAGLINE);  
        tvTagline.setTextColor(android.graphics.Color.parseColor("#8B949E"));  
        tvTagline.setTextSize(14);  
        tvTagline.setGravity(android.view.Gravity.CENTER);  
        tvTagline.setPadding(0, 0, 0, 15);  
        container.addView(tvTagline);  
  
        // ✝️ Ministry Statement (Added as requested)  
        android.widget.TextView tvMinistry = new android.widget.TextView(this);  
        tvMinistry.setText("✝️ Distributed free to the glory of Jesus Christ");  
        tvMinistry.setTextColor(android.graphics.Color.parseColor("#00FF88"));  
        tvMinistry.setTextSize(15);  
        tvMinistry.setTypeface(null, android.graphics.Typeface.BOLD_ITALIC);  
        tvMinistry.setGravity(android.view.Gravity.CENTER);  
        tvMinistry.setPadding(0, 0, 0, 30);  
        container.addView(tvMinistry);  
  
        // Divider 1  
        android.view.View divider1 = new android.view.View(this);  
        android.widget.LinearLayout.LayoutParams divParams = new android.widget.LinearLayout.LayoutParams(  
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 2);  
        divider1.setLayoutParams(divParams);  
        divider1.setBackgroundColor(android.graphics.Color.parseColor("#00C9FF"));  
        container.addView(divider1);  
  
        // Info Rows  
        addInfoRow(container, "Version", Constants.APP_VERSION);  
        addInfoRow(container, "Author", Constants.APP_AUTHOR);  
        addInfoRow(container, "Contact", "sandadatasaver@gmail.com");  
  
        // Divider 2  
        android.view.View divider2 = new android.view.View(this);  
        android.widget.LinearLayout.LayoutParams div2Params = new android.widget.LinearLayout.LayoutParams(  
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 2);  
        div2Params.topMargin = 20;  
        divider2.setLayoutParams(div2Params);  
        divider2.setBackgroundColor(android.graphics.Color.parseColor("#21262D"));  
        container.addView(divider2);  
  
        // Copyright  
        android.widget.TextView tvCopy = new android.widget.TextView(this);  
        tvCopy.setText(Constants.APP_COPYRIGHT);  
        tvCopy.setTextColor(android.graphics.Color.parseColor("#8B949E"));  
        tvCopy.setTextSize(11);  
        tvCopy.setGravity(android.view.Gravity.CENTER);  
        tvCopy.setPadding(0, 16, 0, 20);  
        container.addView(tvCopy);  
  
        scrollView.addView(container);  
        root.addView(scrollView);  
  
        // Close Button  
        android.widget.Button btnClose = new android.widget.Button(this);  
        btnClose.setText("Close");  
        btnClose.setTextColor(android.graphics.Color.parseColor("#0D1117"));  
        btnClose.setBackgroundColor(android.graphics.Color.parseColor("#00C9FF"));  
        android.widget.LinearLayout.LayoutParams btnParams = new android.widget.LinearLayout.LayoutParams(  
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 120);  
        btnParams.topMargin = 20;  
        btnClose.setLayoutParams(btnParams);  
        btnClose.setOnClickListener(v -> dialog.dismiss());  
        root.addView(btnClose);  
  
        dialog.setContentView(root);  
  
        if (dialog.getWindow() != null) {  
            // Make the dialog window expand to MATCH_PARENT width and height for a premium fullscreen look  
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(  
                    android.graphics.Color.parseColor("#E60D1117")));  
            dialog.getWindow().setLayout(  
                android.view.WindowManager.LayoutParams.MATCH_PARENT,  
                android.view.WindowManager.LayoutParams.MATCH_PARENT);  
        }  
  
        dialog.show();  
    }  
  
    // ─────────────────────────────────────  
    // ADD INFO ROW HELPER  
    // ─────────────────────────────────────  
    private void addInfoRow(  
            android.widget.LinearLayout parent,  
            String label,  
            String value) {  
  
        android.widget.LinearLayout row = new android.widget.LinearLayout(this);  
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);  
        row.setPadding(0, 16, 0, 4);  
  
        android.widget.TextView tvLabel = new android.widget.TextView(this);  
        tvLabel.setText(label + ":");  
        tvLabel.setTextColor(android.graphics.Color.parseColor("#00C9FF"));  
        tvLabel.setTextSize(13);  
        tvLabel.setTypeface(null, android.graphics.Typeface.BOLD);  
        android.widget.LinearLayout.LayoutParams labelParams = new android.widget.LinearLayout.LayoutParams(  
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 0.35f);  
        tvLabel.setLayoutParams(labelParams);  
        row.addView(tvLabel);  
  
        android.widget.TextView tvValue = new android.widget.TextView(this);  
        tvValue.setText(value);  
        tvValue.setTextColor(android.graphics.Color.parseColor("#E6EDF3"));  
        tvValue.setTextSize(13);  
        android.widget.LinearLayout.LayoutParams valueParams = new android.widget.LinearLayout.LayoutParams(  
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 0.65f);  
        tvValue.setLayoutParams(valueParams);  
        row.addView(tvValue);  
  
        parent.addView(row);  
    }  
  
    // ─────────────────────────────────────  
    // START BACKGROUND SERVICE  
    // ─────────────────────────────────────  
    private void startBackgroundService() {  
        if (!isServiceRunning(DataSaverService.class)) {  
            Intent intent = new Intent(this, DataSaverService.class);  
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {  
                startForegroundService(intent);  
            } else {  
                startService(intent);  
            }  
        }  
    }  
  
    private boolean isServiceRunning(Class<?> serviceClass) {  
        android.app.ActivityManager mgr = (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);  
        for (android.app.ActivityManager.RunningServiceInfo service  
                : mgr.getRunningServices(Integer.MAX_VALUE)) {  
            if (serviceClass.getName().equals(service.service.getClassName())) {  
                return true;  
            }  
        }  
        return false;  
    }  
  
    // ─────────────────────────────────────  
    // HELPERS  
    // ─────────────────────────────────────  
    private String formatBytes(long bytes) {  
        if (bytes <= 0) return "0 B";  
        if (bytes < 1024) return bytes + " B";  
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";  
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));  
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));  
    }  
  
    // ─────────────────────────────────────  
    // LIFECYCLE  
    // ─────────────────────────────────────  
    @Override  
    protected void onResume() {  
        super.onResume();  
        // Update UI without triggering the toggle listener  
        updateUI(prefs.isDataSaverOn());  
        loadDataUsage();  
    }  
}