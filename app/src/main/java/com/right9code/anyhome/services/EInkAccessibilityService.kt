package com.right9code.anyhome.services

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class EInkAccessibilityService : AccessibilityService() {

    companion object {
        var instance: EInkAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // No-op
    }

    override fun onInterrupt() {
        // No-op
    }

    fun lockScreen(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
    }
}
