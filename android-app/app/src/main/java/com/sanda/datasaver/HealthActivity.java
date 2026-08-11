package com.sanda.datasaver;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.List;

/**
 * HealthActivity — Shows health reminders to distract excessive gaming/social media
 * and focus on scripture, prayer, constructive beneficial activities.
 */
public class HealthActivity extends AppCompatActivity {

    private PrefsManager prefs;
    private HealthReminderManager healthManager;
    private TextView tvCurrentTip, tvNextInfo;
    private SwitchMaterial switchHealth;
    private MaterialButton btnTestNotif, btnInterval30, btnInterval60, btnInterval120;
    private RecyclerView recyclerTips;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme
        try {
            PrefsManager tmp = new PrefsManager(this);
            AppCompatDelegate.setDefaultNightMode(tmp.getThemeMode());
        } catch (Exception e) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health);

        prefs = new PrefsManager(this);
        healthManager = new HealthReminderManager(this);

        bindViews();
        setupUI();
        loadTips();
    }

    private void bindViews() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        tvCurrentTip = findViewById(R.id.tv_current_tip);
        tvNextInfo = findViewById(R.id.tv_next_info);
        switchHealth = findViewById(R.id.switch_health);
        btnTestNotif = findViewById(R.id.btn_test_notif);
        btnInterval30 = findViewById(R.id.btn_interval_30);
        btnInterval60 = findViewById(R.id.btn_interval_60);
        btnInterval120 = findViewById(R.id.btn_interval_120);
        recyclerTips = findViewById(R.id.recycler_tips);

        recyclerTips.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupUI() {
        HealthTip current = healthManager.getCurrentTip();
        if (current != null) {
            tvCurrentTip.setText(current.getFullMessage());
        }

        int interval = healthManager.getIntervalMin();
        tvNextInfo.setText("Next reminder in " + interval + " min — " + healthManager.getNextTip().emoji + " " + healthManager.getNextTip().title);

        switchHealth.setChecked(healthManager.isEnabled());
        switchHealth.setOnCheckedChangeListener((buttonView, isChecked) -> {
            healthManager.setEnabled(isChecked);
            tvNextInfo.setText(isChecked ? "Enabled — Next in " + healthManager.getIntervalMin() + " min" : "Disabled — No reminders");
        });

        btnTestNotif.setOnClickListener(v -> {
            healthManager.showHealthNotification();
            tvCurrentTip.setText(healthManager.getCurrentTip().getFullMessage());
        });

        View.OnClickListener intervalListener = v -> {
            int min = 60;
            if (v == btnInterval30) min = 30;
            else if (v == btnInterval60) min = 60;
            else if (v == btnInterval120) min = 120;
            healthManager.setIntervalMin(min);
            tvNextInfo.setText("Interval set to " + min + " min — Next: " + healthManager.getNextTip().emoji + " " + healthManager.getNextTip().title);
            updateIntervalButtons(min);
        };

        btnInterval30.setOnClickListener(intervalListener);
        btnInterval60.setOnClickListener(intervalListener);
        btnInterval120.setOnClickListener(intervalListener);

        updateIntervalButtons(interval);
    }

    private void updateIntervalButtons(int current) {
        btnInterval30.setStrokeWidth(current == 30 ? 3 : 1);
        btnInterval60.setStrokeWidth(current == 60 ? 3 : 1);
        btnInterval120.setStrokeWidth(current == 120 ? 3 : 1);
    }

    private void loadTips() {
        List<HealthTip> all = HealthTipsProvider.getAllTips();
        HealthTipsAdapter adapter = new HealthTipsAdapter(all);
        recyclerTips.setAdapter(adapter);
    }

    // Simple adapter inner class
    private static class HealthTipsAdapter extends RecyclerView.Adapter<HealthTipsAdapter.VH> {
        private List<HealthTip> tips;

        HealthTipsAdapter(List<HealthTip> tips) {
            this.tips = tips;
        }

        @Override
        public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View v = android.view.LayoutInflater.from(parent.getContext()).inflate(R.layout.item_health_tip, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            HealthTip t = tips.get(position);
            holder.tvEmoji.setText(t.emoji);
            holder.tvCategory.setText(t.category);
            holder.tvTitle.setText(t.title);
            holder.tvMessage.setText(t.message);
            holder.tvScripture.setText("📖 " + t.scriptureRef + " — \"" + t.scriptureText + "\"");
            holder.tvAction.setText("✅ " + t.action);
        }

        @Override
        public int getItemCount() {
            return tips.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvEmoji, tvCategory, tvTitle, tvMessage, tvScripture, tvAction;
            VH(View itemView) {
                super(itemView);
                tvEmoji = itemView.findViewById(R.id.tv_emoji);
                tvCategory = itemView.findViewById(R.id.tv_category);
                tvTitle = itemView.findViewById(R.id.tv_title);
                tvMessage = itemView.findViewById(R.id.tv_message);
                tvScripture = itemView.findViewById(R.id.tv_scripture);
                tvAction = itemView.findViewById(R.id.tv_action);
            }
        }
    }
}
