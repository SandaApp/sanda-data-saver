package com.sanda.datasaver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * BootReceiver — Auto-starts Sanda Data Saver
 * when the phone boots up.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        Log.d(TAG, "Boot received: " + action);

        PrefsManager prefs = new PrefsManager(context);

        // Only auto-start if user has enabled it
        if (!prefs.isAutoStartEnabled()) {
            Log.d(TAG, "Auto-start disabled. Skipping.");
            return;
        }

        // Start the background service
        Intent serviceIntent =
            new Intent(context, DataSaverService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }

        Log.d(TAG, "Sanda Data Saver service started on boot.");

        // If data saver was ON before reboot, re-activate
        if (prefs.isDataSaverOn()) {
            DataSaverManager manager = new DataSaverManager(context);
            manager.activate();
            Log.d(TAG, "Data saver re-activated after boot.");
        }
    }
}