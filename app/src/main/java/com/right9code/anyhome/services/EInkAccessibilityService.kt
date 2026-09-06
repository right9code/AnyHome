package com.right9code.anyhome.services

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class EInkAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // No-op
    }

    override fun onInterrupt() {
        // No-op
    }

    fun lockScreen() {
        performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
    }
}
