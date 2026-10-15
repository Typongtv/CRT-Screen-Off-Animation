package com.typongtv.crtscreenoff

import android.accessibilityservice.AccessibilityService
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.Toast

/**
 * Headless trigger activity designed for Samsung Good Lock (One Hand Operation +)
 * and Home Screen launcher shortcuts.
 *
 * Configured with:
 *  - FLAG_NOT_FOCUSABLE + FLAG_NOT_TOUCHABLE: Never steals focus from active app (Zero Blink)
 *  - Theme.TransparentNoAnimation with windowIsFloating: Never pushes active app back (Zero Dock Peek)
 *  - Stays alive until CrtView overlay is attached to prevent gaps
 */
class LockTriggerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Critical: Never steal focus from active app.
        // Stops underlying app from pausing/redrawing (eliminates the blink)
        // and stops WindowManager from treating this as a task switch (eliminates dock peek).
        window.addFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        disableTransitions()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        disableTransitions()
        setIntent(intent)
        handleIntent(intent)
    }

    private fun disableTransitions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    override fun finish() {
        super.finish()
        disableTransitions()
    }

    private fun handleIntent(intent: Intent?) {
        // 1. If called from Good Lock's "Home screen shortcuts" picker:
        if (intent?.action == Intent.ACTION_CREATE_SHORTCUT) {
            val shortcutIntent = Intent(this, LockTriggerActivity::class.java).apply {
                action = "com.typongtv.crtscreenoff.TRIGGER_LOCK"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
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

        disableTransitions()

        // 2. Dispatch screen-off trigger to running Accessibility Service
        val service = CrtAccessibilityService.instance

        if (service != null) {
            var hasFinished = false
            val finishAction = {
                if (!hasFinished) {
                    hasFinished = true
                    finish()
                }
            }
            // Failsafe timeout in case overlay attachment callback is delayed
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({ finishAction() }, 1200L)

            service.executeCrtShutdown(onOverlayReady = finishAction)
        } else {
            val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            if (km?.isKeyguardLocked != true) {
                Toast.makeText(this, "Please enable CRT Accessibility Service in Settings", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, MainActivity::class.java))
            }
            finish()
        }
    }
}


