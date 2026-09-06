package com.right9code.anyhome.services

import android.os.PowerManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.os.Handler
import android.os.Looper
import com.right9code.anyhome.R

class CaffeineTileService : TileService() {

    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private val autoReleaseDelay = 2 * 60 * 60 * 1000L

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (wakeLock?.isHeld == true) {
            releaseWakeLock()
        } else {
            acquireWakeLock()
        }
        updateTile()
    }

    override fun onStopListening() {
        super.onStopListening()
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(PowerManager::class.java)
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AnyHome:Caffeine")
        wakeLock?.acquire(autoReleaseDelay)
        handler.postDelayed({
            if (wakeLock?.isHeld == true) {
                releaseWakeLock()
            }
        }, autoReleaseDelay)
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.release()
        } catch (e: Exception) {
            // ignore
        }
        wakeLock = null
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        if (wakeLock?.isHeld == true) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = getString(R.string.caffeine_on)
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = getString(R.string.caffeine_off)
        }
        tile.updateTile()
    }
}
