package com.sanda.datasaver;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AppListActivity — Shows ALL installed apps.
 * User can check/uncheck which apps to block
 * when Data Saver is ON.
 */
public class AppListActivity extends AppCompatActivity {

    // ── UI ───────────────────────────────────────
    private RecyclerView   recyclerView;
    private EditText       searchBox;
    private ProgressBar    progressBar;
    private TextView       tvAppCount;
    private MaterialButton btnSave;
    private LinearLayout   filterRow;

    // ── Data ─────────────────────────────────────
    private AppAdapter     adapter;
    private List<AppItem>  allApps  = new ArrayList<>();
    private List<AppItem>  filtered = new ArrayList<>();
    private PrefsManager   prefs;
    private PackageManager pm;
    private Handler        handler  =
            new Handler(Looper.getMainLooper());

    // ── Filter State ──────────────────────────────
    private String  currentFilter  = "user";
    private boolean showSystemApps = false;

    // ─────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_list);

        prefs = new PrefsManager(this);
        pm    = getPackageManager();

        bindViews();
        setupSearch();
        setupFilters();
        setupSaveButton();
        loadApps();
    }

    // ─────────────────────────────────────────────
    // BIND VIEWS
    // ─────────────────────────────────────────────
    private void bindViews() {
        recyclerView = findViewById(R.id.recycler_apps);
        searchBox    = findViewById(R.id.search_box);
        progressBar  = findViewById(R.id.progress_bar);
        tvAppCount   = findViewById(R.id.tv_app_count);
        btnSave      = findViewById(R.id.btn_save);
        filterRow    = findViewById(R.id.filter_row);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(this));

        // Back button
        findViewById(R.id.btn_back)
                .setOnClickListener(v -> finish());

        // Toggle system apps button
        TextView btnToggleSystem =
                findViewById(R.id.btn_toggle_system);
        btnToggleSystem.setOnClickListener(v -> {
            showSystemApps = !showSystemApps;
            btnToggleSystem.setText(
                    showSystemApps
                            ? "  Hide System  "
                            : "  System Apps  "
            );
            applyFilter();
        });
    }

    // ─────────────────────────────────────────────
    // SEARCH
    // ─────────────────────────────────────────────
    private void setupSearch() {
        searchBox.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start,
                            int count, int after) {}

                    @Override
                    public void onTextChanged(
                            CharSequence s, int start,
                            int before, int count) {
                        applyFilter();
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s) {}
                });
    }

    // ─────────────────────────────────────────────
    // FILTER TABS
    // ─────────────────────────────────────────────
    private void setupFilters() {
        TextView btnAll     =
                findViewById(R.id.filter_all);
        TextView btnBlocked =
                findViewById(R.id.filter_blocked);
        TextView btnUser    =
                findViewById(R.id.filter_user);

        btnAll.setOnClickListener(v -> {
            currentFilter = "all";
            highlightFilter(btnAll,
                    btnBlocked, btnUser);
            applyFilter();
        });

        btnBlocked.setOnClickListener(v -> {
            currentFilter = "blocked";
            highlightFilter(btnBlocked,
                    btnAll, btnUser);
            applyFilter();
        });

        btnUser.setOnClickListener(v -> {
            currentFilter = "user";
            highlightFilter(btnUser,
                    btnAll, btnBlocked);
            applyFilter();
        });

        // Default: User tab active
        highlightFilter(btnUser,
                btnAll, btnBlocked);
    }

    private void highlightFilter(
            TextView active,
            TextView... inactive) {
        active.setBackgroundResource(
                R.drawable.filter_tab_active);
        active.setTextColor(
                getColor(R.color.text_on_primary));
        for (TextView tv : inactive) {
            tv.setBackgroundResource(
                    R.drawable.filter_tab_inactive);
            tv.setTextColor(
                    getColor(R.color.text_muted));
        }
    }

    // ─────────────────────────────────────────────
    // SAVE BUTTON
    // ─────────────────────────────────────────────
    private void setupSaveButton() {
        btnSave.setOnClickListener(
                v -> saveChanges());
    }

    private void saveChanges() {
        List<String> blocked = new ArrayList<>();
        for (AppItem item : allApps) {
            if (item.isBlocked) {
                blocked.add(item.packageName);
            }
        }
        prefs.saveBlockedApps(blocked);
        Toast.makeText(this,
                "✅ Saved! " + blocked.size() +
                        " apps blocked.",
                Toast.LENGTH_SHORT).show();
        finish();
    }

    // ─────────────────────────────────────────────
    // LOAD APPS
    // ─────────────────────────────────────────────
    private void loadApps() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        List<String> blockedList =
                prefs.getBlockedApps();

        new Thread(() -> {

            List<PackageInfo> packages;
            if (Build.VERSION.SDK_INT
                    >= Build.VERSION_CODES.TIRAMISU) {
                packages =
                        pm.getInstalledPackages(
                                PackageManager
                                        .PackageInfoFlags
                                        .of(0));
            } else {
                packages =
                        pm.getInstalledPackages(0);
            }

            List<AppItem> items =
                    new ArrayList<>();

            for (PackageInfo pkgInfo : packages) {

                ApplicationInfo info =
                        pkgInfo.applicationInfo;
                if (info == null) continue;

                // Only skip our own app
                if (info.packageName.equals(
                        getPackageName())) continue;

                boolean hasLauncher =
                        pm.getLaunchIntentForPackage(
                                info.packageName) != null;

                boolean hasSystemFlag =
                        (info.flags &
                                ApplicationInfo.FLAG_SYSTEM)
                                != 0;

                // User app = has launcher
                // OR no system flag
                boolean isUserApp =
                        hasLauncher || !hasSystemFlag;

                boolean isBlocked =
                        blockedList.contains(
                                info.packageName);

                try {
                    String appName = pm
                            .getApplicationLabel(info)
                            .toString();

                    android.graphics.drawable
                            .Drawable icon;
                    try {
                        icon = pm
                                .getApplicationIcon(
                                        info);
                    } catch (Exception e) {
                        icon = pm
                                .getDefaultActivityIcon();
                    }

                    AppItem item     = new AppItem();
                    item.packageName =
                            info.packageName;
                    item.appName     = appName;
                    item.icon        = icon;
                    item.isBlocked   = isBlocked;
                    item.isUserApp   = isUserApp;
                    items.add(item);

                } catch (Exception e) {
                    // Skip apps we cannot load
                }
            }

            // Sort: blocked first
            // then user apps before system
            // then alphabetical
            Collections.sort(items, (a, b) -> {
                if (a.isBlocked != b.isBlocked) {
                    return a.isBlocked ? -1 : 1;
                }
                if (a.isUserApp != b.isUserApp) {
                    return a.isUserApp ? -1 : 1;
                }
                return a.appName
                        .compareToIgnoreCase(
                                b.appName);
            });

            allApps = items;

            handler.post(() -> {
                progressBar.setVisibility(
                        View.GONE);
                recyclerView.setVisibility(
                        View.VISIBLE);
                applyFilter();
            });
        }).start();
    }

    // ─────────────────────────────────────────────
    // APPLY FILTER
    // ─────────────────────────────────────────────
    private void applyFilter() {
        String query = searchBox.getText()
                .toString().toLowerCase().trim();

        filtered.clear();

        for (AppItem item : allApps) {

            // System filter
            if (!showSystemApps
                    && !item.isUserApp) continue;

            // Tab filter
            switch (currentFilter) {
                case "blocked":
                    if (!item.isBlocked) continue;
                    break;
                case "user":
                    if (!item.isUserApp) continue;
                    break;
                default:
                    break;
            }

            // Search filter
            if (!query.isEmpty()) {
                boolean nameMatch =
                        item.appName
                                .toLowerCase()
                                .contains(query);
                boolean pkgMatch =
                        item.packageName
                                .toLowerCase()
                                .contains(query);
                if (!nameMatch && !pkgMatch)
                    continue;
            }

            filtered.add(item);
        }

        tvAppCount.setText(
                filtered.size() + " apps");

        if (adapter == null) {
            adapter = new AppAdapter(filtered);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    // ─────────────────────────────────────────────
    // APP ITEM MODEL
    // ─────────────────────────────────────────────
    public static class AppItem {
        public String   packageName;
        public String   appName;
        public android.graphics.drawable
                .Drawable   icon;
        public boolean  isBlocked;
        public boolean  isUserApp;
    }

    // ─────────────────────────────────────────────
    // ADAPTER
    // ─────────────────────────────────────────────
    private class AppAdapter extends
            RecyclerView.Adapter<
                    AppAdapter.ViewHolder> {

        private final List<AppItem> items;

        AppAdapter(List<AppItem> items) {
            this.items = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(
                ViewGroup parent, int viewType) {
            View view = LayoutInflater
                    .from(parent.getContext())
                    .inflate(
                            R.layout.item_app,
                            parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(
                ViewHolder holder, int position) {
            AppItem item = items.get(position);

            holder.appName.setText(item.appName);
            holder.packageName.setText(
                    item.packageName);
            holder.appIcon.setImageDrawable(
                    item.icon);

            // Show system badge for
            // non user apps
            holder.tvSystem.setVisibility(
                    item.isUserApp
                            ? View.GONE
                            : View.VISIBLE);

            // Dim unchecked apps slightly
            holder.itemView.setAlpha(
                    item.isBlocked ? 1.0f : 0.75f);

            // Reset listener before setting
            // checked state to avoid loop
            holder.checkBox
                    .setOnCheckedChangeListener(null);
            holder.checkBox.setChecked(
                    item.isBlocked);
            holder.checkBox
                    .setOnCheckedChangeListener(
                            (btn, checked) -> {
                                item.isBlocked = checked;
                                holder.itemView.setAlpha(
                                        checked
                                                ? 1.0f
                                                : 0.75f);
                            });

            // Row click also toggles checkbox
            holder.itemView.setOnClickListener(
                    v -> {
                        item.isBlocked =
                                !item.isBlocked;
                        holder.checkBox.setChecked(
                                item.isBlocked);
                    });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends
                RecyclerView.ViewHolder {
            android.widget.ImageView appIcon;
            TextView  appName;
            TextView  packageName;
            TextView  tvSystem;
            CheckBox  checkBox;

            ViewHolder(View itemView) {
                super(itemView);
                appIcon     = itemView
                        .findViewById(R.id.app_icon);
                appName     = itemView
                        .findViewById(R.id.app_name);
                packageName = itemView
                        .findViewById(
                                R.id.app_package);
                tvSystem    = itemView
                        .findViewById(
                                R.id.tv_system_badge);
                checkBox    = itemView
                        .findViewById(
                                R.id.app_checkbox);
            }
        }
    }
}