package com.sanda.datasaver;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * SandaApp — Global Application class.
 * Runs once when the app process starts.
 * Sets up notification channels here.
 */
public class SandaApp extends Application {

    public static final String CHANNEL_ID_SERVICE  = "sanda_service_channel";
    public static final String CHANNEL_ID_ALERTS   = "sanda_alerts_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();

        // Apply saved theme on startup
        PrefsManager prefs = new PrefsManager(this);
        AppCompatDelegate.setDefaultNightMode(
                prefs.getThemeMode());
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager =
                getSystemService(NotificationManager.class);

            // Persistent service notification channel
            NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID_SERVICE,
                "Sanda Data Saver Service",
                NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription(
                "Keeps Sanda Data Saver running in the background");
            serviceChannel.setShowBadge(false);
            manager.createNotificationChannel(serviceChannel);

            // Alert notifications channel
            NotificationChannel alertChannel = new NotificationChannel(
                CHANNEL_ID_ALERTS,
                "Sanda Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            alertChannel.setDescription(
                "Notifications when Data Saver turns ON or OFF");
            manager.createNotificationChannel(alertChannel);
        }
    }
}