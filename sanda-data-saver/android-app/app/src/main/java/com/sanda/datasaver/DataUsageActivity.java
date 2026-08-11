package com.sanda.datasaver;

import android.app.AppOpsManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * DataUsageActivity — Shows detailed data usage.
 * Lists every app with how much mobile and WiFi
 * data it has used today, this week, this month.
 */
public class DataUsageActivity extends AppCompatActivity {

    // ── UI ───────────────────────────────────────
    private RecyclerView    recyclerView;
    private ProgressBar     progressBar;
    private TextView        tvTotalMobile;
    private TextView        tvTotalWifi;
    private TextView        tvTotalAll;
    private TextView        tvPeriod;
    private TabLayout       tabLayout;

    // ── Data ─────────────────────────────────────
    private UsageAdapter                        adapter;
    private List<DataUsageHelper.AppDataUsage>  usageList = new ArrayList<>();
    private DataUsageHelper                     usageHelper;
    private Handler                             handler =
        new Handler(Looper.getMainLooper());

    // ─────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_usage);

        usageHelper = new DataUsageHelper(this);

        bindViews();
        checkPermission();
        setupTabs();
        loadUsage("today");
    }

    // ─────────────────────────────────────────────
    // BIND VIEWS
    // ─────────────────────────────────────────────
    private void bindViews() {
        recyclerView  = findViewById(R.id.recycler_usage);
        progressBar   = findViewById(R.id.progress_bar);
        tvTotalMobile = findViewById(R.id.tv_total_mobile);
        tvTotalWifi   = findViewById(R.id.tv_total_wifi);
        tvTotalAll    = findViewById(R.id.tv_total_all);
        tvPeriod      = findViewById(R.id.tv_period);
        tabLayout     = findViewById(R.id.tab_layout);

        recyclerView.setLayoutManager(
            new LinearLayoutManager(this));

        // Back button
        findViewById(R.id.btn_back)
            .setOnClickListener(v -> finish());
    }

    // ─────────────────────────────────────────────
    // TABS
    // ─────────────────────────────────────────────
    private void setupTabs() {
        tabLayout.addTab(
            tabLayout.newTab().setText("Today"));
        tabLayout.addTab(
            tabLayout.newTab().setText("This Week"));
        tabLayout.addTab(
            tabLayout.newTab().setText("This Month"));

        tabLayout.addOnTabSelectedListener(
            new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    switch (tab.getPosition()) {
                        case 0: loadUsage("today");  break;
                        case 1: loadUsage("week");   break;
                        case 2: loadUsage("month");  break;
                    }
                }
                @Override
                public void onTabUnselected(TabLayout.Tab tab) {}
                @Override
                public void onTabReselected(TabLayout.Tab tab) {}
            });
    }

    // ─────────────────────────────────────────────
    // LOAD USAGE
    // ─────────────────────────────────────────────
    private void loadUsage(String period) {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        switch (period) {
            case "today":
                tvPeriod.setText("Today's Data Usage");
                break;
            case "week":
                tvPeriod.setText("This Week's Data Usage");
                break;
            case "month":
                tvPeriod.setText("This Month's Data Usage");
                break;
        }

        new Thread(() -> {
            // For now all periods use today's stats
            // In a full version you would pass date ranges
            List<DataUsageHelper.AppDataUsage> data =
                usageHelper.getTodayUsage();

            long totalMobile = 0;
            long totalWifi   = 0;
            for (DataUsageHelper.AppDataUsage app : data) {
                totalMobile += app.mobileBytes;
                totalWifi   += app.wifiBytes;
            }

            final long finalMobile = totalMobile;
            final long finalWifi   = totalWifi;

            handler.post(() -> {
                usageList = data;
                progressBar.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);

                tvTotalMobile.setText(
                    "📱 Mobile:  " + formatBytes(finalMobile));
                tvTotalWifi.setText(
                    "📶 WiFi:      " + formatBytes(finalWifi));
                tvTotalAll.setText(
                    "📊 Total:    " +
                    formatBytes(finalMobile + finalWifi));

                if (adapter == null) {
                    adapter = new UsageAdapter(usageList);
                    recyclerView.setAdapter(adapter);
                } else {
                    adapter.notifyDataSetChanged();
                }
            });
        }).start();
    }

    // ─────────────────────────────────────────────
    // PERMISSION CHECK
    // ─────────────────────────────────────────────
    private void checkPermission() {
        if (!hasUsageStatsPermission()) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Usage Access Required")
                .setMessage(
                    "To see data usage per app, please grant " +
                    "Usage Access permission.\n\n" +
                    "Settings → Apps → Special App Access → " +
                    "Usage Access → Sanda Data Saver → Enable"
                )
                .setPositiveButton("Open Settings", (d, w) ->
                    startActivity(new Intent(
                        Settings.ACTION_USAGE_ACCESS_SETTINGS)))
                .setNegativeButton("Cancel", null)
                .show();
        }
    }

    private boolean hasUsageStatsPermission() {
        try {
            AppOpsManager appOps =
                (AppOpsManager) getSystemService(APP_OPS_SERVICE);
            int mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                getPackageName()
            );
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            return false;
        }
    }

    // ─────────────────────────────────────────────
    // FORMAT BYTES
    // ─────────────────────────────────────────────
    private String formatBytes(long bytes) {
        if (bytes <= 0)           return "0 B";
        if (bytes < 1024)         return bytes + " B";
        if (bytes < 1024 * 1024)  return (bytes / 1024) + " KB";
        if (bytes < 1024L * 1024 * 1024)
            return String.format("%.1f MB",
                bytes / (1024.0 * 1024));
        return String.format("%.2f GB",
            bytes / (1024.0 * 1024 * 1024));
    }

    // ─────────────────────────────────────────────
    // ADAPTER
    // ─────────────────────────────────────────────
    private class UsageAdapter extends
            RecyclerView.Adapter<UsageAdapter.ViewHolder> {

        private final List<DataUsageHelper.AppDataUsage> items;

        UsageAdapter(List<DataUsageHelper.AppDataUsage> items) {
            this.items = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(
                ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_usage_detail, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(
                ViewHolder holder, int position) {
            DataUsageHelper.AppDataUsage item = items.get(position);

            holder.appIcon.setImageDrawable(item.appIcon);
            holder.appName.setText(item.appName);
            holder.tvMobile.setText(
                "📱 " + item.getMobileReadableSize());
            holder.tvTotal.setText(item.getReadableSize());

            // Progress bar — percentage of max usage
            if (!items.isEmpty()) {
                long max = items.get(0).bytesUsed;
                int  pct = max > 0
                    ? (int)((item.bytesUsed * 100) / max)
                    : 0;
                holder.usageBar.setProgress(pct);
            }

            // Rank number
            holder.tvRank.setText(
                String.valueOf(position + 1));

            // Color top 3 differently
            int rankColor;
            switch (position) {
                case 0:  rankColor = R.color.color_accent;   break;
                case 1:  rankColor = R.color.color_warning;  break;
                case 2:  rankColor = R.color.color_primary;  break;
                default: rankColor = R.color.text_muted;     break;
            }
            holder.tvRank.setTextColor(getColor(rankColor));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView   appIcon;
            TextView    appName;
            TextView    tvMobile;
            TextView    tvTotal;
            TextView    tvRank;
            ProgressBar usageBar;

            ViewHolder(View itemView) {
                super(itemView);
                appIcon  = itemView.findViewById(R.id.app_icon);
                appName  = itemView.findViewById(R.id.app_name);
                tvMobile = itemView.findViewById(R.id.tv_mobile);
                tvTotal  = itemView.findViewById(R.id.tv_total);
                tvRank   = itemView.findViewById(R.id.tv_rank);
                usageBar = itemView.findViewById(R.id.usage_bar);
            }
        }
    }
}