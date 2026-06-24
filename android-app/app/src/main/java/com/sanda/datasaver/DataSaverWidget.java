package com.sanda.datasaver;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.widget.RemoteViews;

/**
 * DataSaverWidget — Home Screen Widget.
 * Shows current Data Saver status and
 * provides a one-tap toggle button.
 */
public class DataSaverWidget
        extends AppWidgetProvider {

    // ── Action for toggle ─────────────────
    private static final String
            ACTION_TOGGLE_WIDGET =
            "com.sanda.datasaver.WIDGET_TOGGLE";

    // ─────────────────────────────────────
    @Override
    public void onUpdate(
            Context context,
            AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {

        for (int widgetId : appWidgetIds) {
            updateWidget(
                    context,
                    appWidgetManager,
                    widgetId);
        }
    }

    // ─────────────────────────────────────
    @Override
    public void onReceive(
            Context context,
            Intent intent) {
        super.onReceive(context, intent);

        if (ACTION_TOGGLE_WIDGET.equals(
                intent.getAction())) {
            handleToggle(context);
        }
    }

    // ─────────────────────────────────────
    // HANDLE TOGGLE FROM WIDGET
    // ─────────────────────────────────────
    private void handleToggle(
            Context context) {
        PrefsManager prefs =
                new PrefsManager(context);
        DataSaverManager manager =
                new DataSaverManager(context);

        if (prefs.isDataSaverOn()) {
            // Turn OFF
            manager.deactivate();
        } else {
            // Check if VPN permission
            // is already granted
            Intent vpnIntent =
                    VpnService.prepare(context);
            if (vpnIntent == null) {
                // Permission already granted
                // Activate directly
                manager.activate();
            } else {
                // Need VPN permission
                // Open main activity
                Intent openApp = new Intent(
                        context,
                        MainActivity.class);
                openApp.setFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent
                                .FLAG_ACTIVITY_CLEAR_TASK);
                openApp.putExtra(
                        "auto_toggle", true);
                context.startActivity(openApp);
                return;
            }
        }

        // Update all widgets
        refreshAllWidgets(context);
    }

    // ─────────────────────────────────────
    // UPDATE A SINGLE WIDGET
    // ─────────────────────────────────────
    private static void updateWidget(
            Context context,
            AppWidgetManager manager,
            int widgetId) {

        PrefsManager prefs =
                new PrefsManager(context);
        boolean isOn =
                prefs.isDataSaverOn();

        RemoteViews views = new RemoteViews(
                context.getPackageName(),
                R.layout.widget_layout);

        // ── Update status text ────────────
        if (isOn) {
            views.setTextViewText(
                    R.id.widget_status,
                    "🛡️ DATA SAVER: ON");
            views.setTextColor(
                    R.id.widget_status,
                    0xFF00FF88); // green
            views.setTextViewText(
                    R.id.widget_btn_text,
                    "✅  TAP TO DEACTIVATE");
        } else {
            views.setTextViewText(
                    R.id.widget_status,
                    "DATA SAVER: OFF");
            views.setTextColor(
                    R.id.widget_status,
                    0xFFFF6B6B); // red
            views.setTextViewText(
                    R.id.widget_btn_text,
                    "🛡️  TAP TO ACTIVATE");
        }

        // ── Toggle button click ───────────
        Intent toggleIntent = new Intent(
                context,
                DataSaverWidget.class);
        toggleIntent.setAction(
                ACTION_TOGGLE_WIDGET);
        PendingIntent togglePending =
                PendingIntent.getBroadcast(
                        context, 0,
                        toggleIntent,
                        PendingIntent
                                .FLAG_UPDATE_CURRENT
                                | PendingIntent
                                .FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(
                R.id.widget_toggle,
                togglePending);

        // ── Open app on status tap ────────
        Intent openApp = new Intent(
                context, MainActivity.class);
        PendingIntent openPending =
                PendingIntent.getActivity(
                        context, 1, openApp,
                        PendingIntent
                                .FLAG_UPDATE_CURRENT
                                | PendingIntent
                                .FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(
                R.id.widget_status,
                openPending);

        // ── Apply update ──────────────────
        manager.updateAppWidget(
                widgetId, views);
    }

    // ─────────────────────────────────────
    // REFRESH ALL WIDGETS
    // Call this after toggle to update
    // all widget instances on home screen
    // ─────────────────────────────────────
    public static void refreshAllWidgets(
            Context context) {
        AppWidgetManager manager =
                AppWidgetManager.getInstance(
                        context);
        ComponentName widget =
                new ComponentName(
                        context,
                        DataSaverWidget.class);
        int[] ids =
                manager.getAppWidgetIds(widget);

        for (int id : ids) {
            updateWidget(
                    context, manager, id);
        }
    }
}