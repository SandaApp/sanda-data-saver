package com.sanda.datasaver;

import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import androidx.annotation.RequiresApi;

/**
 * DataSaverTileService — Quick Settings tile.
 * Adds a toggle tile to the Android notification panel
 * (the tiles you see when you swipe down twice).
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class DataSaverTileService extends TileService {

    private static final String TAG = "TileService";

    private DataSaverManager manager;
    private PrefsManager     prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        manager = new DataSaverManager(this);
        prefs   = new PrefsManager(this);
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        updateTile();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        // Toggle data saver on tile click
        if (manager.isDataSaverOn()) {
            manager.deactivate();
            Log.d(TAG, "Tile: Data Saver turned OFF");
        } else {
            manager.activate();
            Log.d(TAG, "Tile: Data Saver turned ON");
        }
        updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) return;

        boolean isOn = manager.isDataSaverOn();

        tile.setState(isOn ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel(Constants.APP_NAME);
        tile.setSubtitle(isOn ? "ON — Saving Data" : "OFF");
        tile.setIcon(Icon.createWithResource(
            this,
            isOn ? R.drawable.ic_shield_on : R.drawable.ic_shield_off
        ));
        tile.updateTile();
    }
}