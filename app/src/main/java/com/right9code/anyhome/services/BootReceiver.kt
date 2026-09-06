package com.right9code.anyhome.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.right9code.anyhome.data.PreferencesManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!PreferencesManager.kioskEnabled) return
        val pkg = PreferencesManager.kioskPackage ?: return
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(pkg) ?: return
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
    }
}
