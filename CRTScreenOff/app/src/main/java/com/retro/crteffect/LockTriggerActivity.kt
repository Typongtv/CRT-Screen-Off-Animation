package com.retro.crteffect

import android.app.Activity
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
 *  - 0ms window transitions (no open/close animations or screen flicker)
 *  - Supports Intent.ACTION_CREATE_SHORTCUT for Good Lock's "Home screen shortcuts" picker
 *  - Handles both onCreate and onNewIntent to prevent dropped triggers
 */
class LockTriggerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
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
                    Intent.ShortcutIconResource.fromContext(this@LockTriggerActivity, R.mipmap.ic_launcher)
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
        if (service != null) {
            service.executeCrtShutdown()
        } else {
            Toast.makeText(this, "Please enable CRT Accessibility Service in Settings", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, MainActivity::class.java))
        }

        finish()
    }
}
