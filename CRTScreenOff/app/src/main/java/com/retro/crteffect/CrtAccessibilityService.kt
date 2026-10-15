package com.retro.crteffect

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Display
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

class CrtAccessibilityService : AccessibilityService() {

    companion object {
        var instance: CrtAccessibilityService? = null
            private set
    }

    private lateinit var windowManager: WindowManager
    private lateinit var prefs: SharedPreferences
    private val mainHandler = Handler(Looper.getMainLooper())
    private var crtView: CrtView? = null
    private var isAnimating = false
    private var animStartTime = 0L

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // When user wakes the screen, guarantee the overlay is removed
            safelyRemoveOverlay()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = getSharedPreferences("crt_prefs", Context.MODE_PRIVATE)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenStateReceiver, filter)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    /**
     * Executes the Non-Root Silent Capture & CRT Animation Sequence:
     * 1. Uses AccessibilityService.takeScreenshot (API 30+) without user confirmation prompts.
     * 2. Renders the exact Xposed CRT Electron Beam animation in high-performance CrtView.
     * 3. Locks the physical screen via GLOBAL_ACTION_LOCK_SCREEN (API 28+).
     * 4. Holds the pure black OLED overlay for 600ms while the display powers down,
     *    eliminating the flash of the underlying app before sleep.
     */
    fun executeCrtShutdown() {
        val now = SystemClock.uptimeMillis()
        if (isAnimating) {
            // Debounce rapid duplicate gestures within 1.5s
            if (now - animStartTime < 1500L) {
                return
            }
            safelyRemoveOverlay()
        }

        isAnimating = true
        animStartTime = now

        val durationMs = prefs.getInt("anim_speed", 350).toLong()

        // Safety Watchdog: Failsafe to guarantee overlay removal even if system sleep is interrupted
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            if (isAnimating) {
                performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                safelyRemoveOverlay()
            }
        }, durationMs + 1200L)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(screenshotResult: ScreenshotResult) {
                        val hardwareBuffer = screenshotResult.hardwareBuffer
                        val colorSpace = screenshotResult.colorSpace
                        val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)
                        hardwareBuffer.close()

                        if (bitmap != null) {
                            showOverlayAndAnimate(bitmap, durationMs)
                        } else {
                            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                            safelyRemoveOverlay()
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                        safelyRemoveOverlay()
                    }
                }
            )
        } else {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            safelyRemoveOverlay()
        }
    }

    private fun showOverlayAndAnimate(capturedBitmap: Bitmap, durationMs: Long) {
        val styleName = prefs.getString("anim_style", CrtStyle.CLASSIC_HORIZONTAL.name)
        val style = try {
            CrtStyle.valueOf(styleName ?: CrtStyle.CLASSIC_HORIZONTAL.name)
        } catch (_: Exception) {
            CrtStyle.CLASSIC_HORIZONTAL
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        crtView = CrtView(
            context = this,
            screenBitmap = capturedBitmap,
            style = style,
            durationMs = durationMs,
            onAnimationComplete = {
                // 1. Trigger the native system lock screen action immediately
                performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)

                // 2. Keep the pitch-black OLED overlay active for 600ms while display power
                // transitions to OFF. This eliminates the race condition where the underlying
                // wallpaper/app flashes back into view before the screen turns completely dark.
                mainHandler.postDelayed({
                    safelyRemoveOverlay()
                }, 600L)
            }
        )

        try {
            windowManager.addView(crtView, params)
        } catch (_: Exception) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            safelyRemoveOverlay()
        }
    }

    private fun safelyRemoveOverlay() {
        mainHandler.removeCallbacksAndMessages(null)
        crtView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            crtView = null
        }
        isAnimating = false
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (_: Exception) {}
        safelyRemoveOverlay()
    }
}
