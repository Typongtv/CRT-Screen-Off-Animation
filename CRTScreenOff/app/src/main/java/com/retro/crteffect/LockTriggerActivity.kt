package com.retro.crteffect

import android.accessibilityservice.AccessibilityService
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast

/**
 * Headless trigger activity designed specifically for Samsung Good Lock (One Hand Operation +)
 * and Home Screen launcher shortcuts.
 *
 * Configured with:
 *  - Theme.TransparentNoAnimation
 *  - showWhenLocked = true (enables triggering screen off even from the Lock Screen / Keyguard)
 *  - 0ms window transitions (no open/close animations or screen flicker)
 *  - Supports Intent.ACTION_CREATE_SHORTCUT for Good Lock's "Home screen shortcuts" picker
 *  - Handles both onCreate and onNewIntent to prevent dropped triggers
 */
class LockTriggerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(false)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        }
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        // 1. If called from Good Lock's "Home screen shortcuts" picker:
        if (intent?.action == Intent.ACTION_CREATE_SHORTCUT) {
            val shortcutIntent = Intent(this, LockTriggerActivity::class.java).apply {
                action = "com.retro.crteffect.TRIGGER_LOCK"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val resultIntent = Intent().apply {
                putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
                putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.shortcut_lock_label))
                putExtra(
                    Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(this@LockTriggerActivity, R.mipmap.ic_lock_launcher)
                )
            }
            setResult(RESULT_OK, resultIntent)
            finish()
            return
        }

        // 2. Disable all window entry/exit animations to prevent visual flicker
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        // 3. Dispatch screen-off trigger to running Accessibility Service
        val service = CrtAccessibilityService.instance
        val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isLocked = km?.isKeyguardLocked == true

        if (service != null) {
            if (isLocked) {
                // Device is currently on the Lock Screen / Keyguard:
                // Instantly put the screen to sleep (native 0ms sleep) without capture delay or errors
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
            } else {
                service.executeCrtShutdown()
            }
        } else {
            if (!isLocked) {
                Toast.makeText(this, "Please enable CRT Accessibility Service in Settings", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, MainActivity::class.java))
            }
        }

        finish()
    }
}
