package com.sanda.datasaver;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * DataSaverManager — Core engine.
 * Activates and deactivates data saving
 * using a local VPN firewall.
 * Also updates home screen widget.
 */
public class DataSaverManager {

    private static final String TAG =
            "DataSaverManager";

    private final Context      context;
    private final PrefsManager prefs;

    public DataSaverManager(Context context) {
        this.context = context
                .getApplicationContext();
        this.prefs   =
                new PrefsManager(this.context);
    }

    // ─────────────────────────────────────
    // ACTIVATE
    // ─────────────────────────────────────
    public void activate() {
        Log.d(TAG,
                "Activating Sanda Data Saver...");

        try {
            Intent intent = new Intent(
                    context,
                    DataSaverVpnService.class);
            intent.setAction("START");
            context.startService(intent);
            prefs.setDataSaverOn(true);
            Log.d(TAG, "Data Saver ACTIVE.");

            // Update home screen widget
            DataSaverWidget
                    .refreshAllWidgets(context);

        } catch (Exception e) {
            Log.e(TAG,
                    "Activate error: "
                            + e.getMessage());
        }
    }

    // ─────────────────────────────────────
    // DEACTIVATE
    // ─────────────────────────────────────
    public void deactivate() {
        Log.d(TAG,
                "Deactivating Sanda Data Saver...");

        try {
            Intent intent = new Intent(
                    context,
                    DataSaverVpnService.class);
            intent.setAction("STOP");
            context.startService(intent);
            prefs.setDataSaverOn(false);
            Log.d(TAG, "Data Saver OFF.");

            // Update home screen widget
            DataSaverWidget
                    .refreshAllWidgets(context);

        } catch (Exception e) {
            Log.e(TAG,
                    "Deactivate error: "
                            + e.getMessage());
        }
    }

    // ─────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────
    public boolean isDataSaverOn() {
        return prefs.isDataSaverOn();
    }
}