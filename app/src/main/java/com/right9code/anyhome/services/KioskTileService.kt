package com.right9code.anyhome.services

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.right9code.anyhome.R
import com.right9code.anyhome.data.PreferencesManager

class KioskTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        PreferencesManager.kioskEnabled = !PreferencesManager.kioskEnabled
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        if (PreferencesManager.kioskEnabled) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = getString(R.string.kiosk_tile_on)
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = getString(R.string.kiosk_tile_off)
        }
        tile.updateTile()
    }
}
