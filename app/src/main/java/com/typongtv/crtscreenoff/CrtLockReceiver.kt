package com.typongtv.crtscreenoff

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Headless, zero-window Broadcast Receiver for automation apps (Tasker, MacroDroid, Edge Gestures)
 * and ADB shell triggers.
 *
 * Usage:
 *   am broadcast -a com.typongtv.crtscreenoff.TRIGGER_LOCK
 */
class CrtLockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "com.typongtv.crtscreenoff.TRIGGER_LOCK") {
            val service = CrtAccessibilityService.instance
            service?.executeCrtShutdown()
        }
    }
}
