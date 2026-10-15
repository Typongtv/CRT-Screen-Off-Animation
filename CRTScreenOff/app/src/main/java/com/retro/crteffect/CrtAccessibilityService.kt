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

        // Exclusively system broadcasts: prevents Android 14+ SecurityException without needing flags
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenStateReceiver, filter)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    private var lastCapturedBitmap: Bitmap? = null

    private fun getFallbackBitmap(): Bitmap {
        val last = lastCapturedBitmap
        if (last != null && !last.isRecycled) {
            return last
        }
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels.coerceAtLeast(1080)
        val height = metrics.heightPixels.coerceAtLeast(1920)
        val fallback = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(fallback)
        canvas.drawColor(android.graphics.Color.BLACK)
        return fallback
    }

    private fun attemptScreenshot(
        durationMs: Long,
        onOverlayReady: (() -> Unit)?,
        isRetry: Boolean = false
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                takeScreenshot(
                    Display.DEFAULT_DISPLAY,
                    mainExecutor,
                    object : TakeScreenshotCallback {
                        override fun onSuccess(screenshotResult: ScreenshotResult) {
                            val hardwareBuffer = screenshotResult.hardwareBuffer
                            val colorSpace = screenshotResult.colorSpace
                            val bitmap = try {
                                Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)
                            } catch (_: Throwable) {
                                null
                            } finally {
                                hardwareBuffer.close()
                            }

                            if (bitmap != null) {
                                lastCapturedBitmap = bitmap
                                showOverlayAndAnimate(bitmap, durationMs, onOverlayReady)
                            } else if (!isRetry) {
                                // Fast 35ms retry if hardware buffer wrap encountered transient lock
                                mainHandler.postDelayed({
                                    attemptScreenshot(durationMs, onOverlayReady, isRetry = true)
                                }, 35L)
                            } else {
                                // Guaranteed fallback: animate with fallback frame so CRT effect always fires
                                showOverlayAndAnimate(getFallbackBitmap(), durationMs, onOverlayReady)
                            }
                        }

                        override fun onFailure(errorCode: Int) {
                            if (!isRetry) {
                                // Fast 35ms retry if Samsung side-key window transition interfered with capture
                                mainHandler.postDelayed({
                                    attemptScreenshot(durationMs, onOverlayReady, isRetry = true)
                                }, 35L)
                            } else {
                                // Guaranteed fallback: animate with fallback frame so CRT effect always fires
                                showOverlayAndAnimate(getFallbackBitmap(), durationMs, onOverlayReady)
                            }
                        }
                    }
                )
            } catch (_: Throwable) {
                if (!isRetry) {
                    mainHandler.postDelayed({
                        attemptScreenshot(durationMs, onOverlayReady, isRetry = true)
                    }, 35L)
                } else {
                    showOverlayAndAnimate(getFallbackBitmap(), durationMs, onOverlayReady)
                }
            }
        } else {
            showOverlayAndAnimate(getFallbackBitmap(), durationMs, onOverlayReady)
        }
    }

    /**
     * Executes the Non-Root Silent Capture & CRT Animation Sequence:
     * 1. Uses AccessibilityService.takeScreenshot (API 30+) without user confirmation prompts.
     * 2. Renders the retro CRT Electron Beam animation in high-performance CrtView.
     * 3. Locks the physical screen via GLOBAL_ACTION_LOCK_SCREEN (API 28+).
     * 4. Holds the pure black OLED overlay for 600ms while the display powers down,
     *    eliminating the flash of the underlying app before sleep.
     * 5. Invokes [onOverlayReady] the moment the overlay view is attached, allowing trigger
     *    activities to finish without exposing a 1-2 frame window destruction glitch.
     */
    fun executeCrtShutdown(onOverlayReady: (() -> Unit)? = null) {
        val enableCrtAnimation = prefs.getBoolean("enable_crt_animation", true)
        if (!enableCrtAnimation) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            onOverlayReady?.invoke()
            return
        }

        val now = SystemClock.uptimeMillis()
        val enhancedOverdrive = prefs.getBoolean("enable_enhanced_overdrive", false)
        val durationMs = if (enhancedOverdrive) {
            prefs.getInt("anim_speed_overdrive", 500).toLong()
        } else {
            prefs.getInt("anim_speed", 500).toLong()
        }
        val afterglowMs = prefs.getInt("afterglow_duration", 800).toLong()
        val resistanceName = prefs.getString("coil_resistance", CoilResistance.HEAVY.name)
        val coilResistance = try {
            CoilResistance.valueOf(resistanceName ?: CoilResistance.HEAVY.name)
        } catch (_: Exception) {
            CoilResistance.HEAVY
        }
        val enableMagneticStretch = prefs.getBoolean("enable_magnetic_stretch", false)
        val stretchBonus = if (enhancedOverdrive && enableMagneticStretch) 1.20f else 1.0f
        val effectiveDurationMs = if (enhancedOverdrive) (durationMs.toFloat() * coilResistance.timeMultiplier * stretchBonus).toLong() else durationMs
        val totalDurationMs = if (enhancedOverdrive && afterglowMs > 0L) (effectiveDurationMs + afterglowMs) else effectiveDurationMs

        val debounceLimit = (totalDurationMs + 400L).coerceIn(400L, 2500L)
        if (isAnimating) {
            // Debounce rapid duplicate gestures while animation is in flight
            if (now - animStartTime < debounceLimit) {
                onOverlayReady?.invoke()
                return
            }
            safelyRemoveOverlay()
        }

        isAnimating = true
        animStartTime = now

        // Safety Watchdog: Failsafe to guarantee overlay removal even if system sleep is interrupted
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            if (isAnimating) {
                performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                safelyRemoveOverlay()
            }
        }, totalDurationMs + 2000L)

        attemptScreenshot(durationMs, onOverlayReady, isRetry = false)
    }

    private fun showOverlayAndAnimate(capturedBitmap: Bitmap, durationMs: Long, onOverlayReady: (() -> Unit)? = null) {
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

        val enhancedOverdrive = prefs.getBoolean("enable_enhanced_overdrive", false)
        val flashIntensity = prefs.getFloat("flash_intensity", 1.0f)
        val afterglowMs = prefs.getInt("afterglow_duration", 800).toLong()
        val resistanceName = prefs.getString("coil_resistance", CoilResistance.HEAVY.name)
        val coilResistance = try {
            CoilResistance.valueOf(resistanceName ?: CoilResistance.HEAVY.name)
        } catch (_: Exception) {
            CoilResistance.HEAVY
        }

        val enableMagneticStretch = prefs.getBoolean("enable_magnetic_stretch", false)
        val enableAnamorphicFlare = prefs.getBoolean("enable_anamorphic_flare", true)

        crtView = CrtView(
            context = this,
            screenBitmap = capturedBitmap,
            style = style,
            durationMs = durationMs,
            enhancedOverdrive = enhancedOverdrive,
            flashIntensity = flashIntensity,
            afterglowMs = afterglowMs,
            coilResistance = coilResistance,
            enableMagneticStretch = enableMagneticStretch,
            enableAnamorphicFlare = enableAnamorphicFlare,
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
            crtView?.post {
                onOverlayReady?.invoke()
            }
        } catch (_: Exception) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            safelyRemoveOverlay()
            onOverlayReady?.invoke()
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
