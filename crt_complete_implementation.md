# ARCHITECTURAL SPECIFICATION: AUTHENTIC CRT SCREEN-OFF ENGINE (NON-ROOT)
# Target: Android 13+ (API 33+) | Framework: Jetpack Compose, Accessibility Engine & Good Lock Integration
# Source Reference: lucasa831/Xposed-ScreenOffAnimation (1:1 Animation Math & Behavior Replicated Without Root)

================================================================================
SECTION 1: GRADLE CONFIGURATION (build.gradle.kts)
================================================================================
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.retro.crteffect"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.retro.crteffect"
        minSdk = 33 // Required for modern WindowManager & SurfaceControl capabilities
        targetSdk = 34
        versionCode = 2
        versionName = "2.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.savedstate:savedstate-ktx:1.2.1")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Jetpack Compose BOM Integration Framework
    implementation(platform("androidx.compose:compose-bom:2024.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
}

================================================================================
SECTION 2: SYSTEM MANIFEST (AndroidManifest.xml)
================================================================================
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Allows displaying overlay elements when triggered -->
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

    <application
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.CRTScreenOff"
        tools:targetApi="tiramisu">

        <!-- 1. Configuration & Setup Dashboard -->
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <meta-data
                android:name="android.app.shortcuts"
                android:resource="@xml/shortcuts" />
        </activity>

        <!-- 2. Zero-Flicker Headless Trigger for Gestures / Open App / Shortcuts -->
        <activity
            android:name=".LockTriggerActivity"
            android:exported="true"
            android:excludeFromRecents="true"
            android:noHistory="true"
            android:taskAffinity=""
            android:launchMode="singleInstance"
            android:theme="@style/Theme.TransparentNoAnimation"
            android:label="@string/shortcut_lock_label">
            <!-- App Launcher: Appears in 'Start Application' / 'Open App' lists -->
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <intent-filter>
                <action android:name="com.retro.crteffect.TRIGGER_LOCK" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
            <!-- Enables Samsung Good Lock 'Home screen shortcuts' selection -->
            <intent-filter>
                <action android:name="android.intent.action.CREATE_SHORTCUT" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </activity>

        <!-- 3. Accessibility Service: Silent Frame Capture & Screen Lock Execution -->
        <service
            android:name=".CrtAccessibilityService"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
            android:exported="true">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_config" />
        </service>
    </application>
</manifest>

================================================================================
SECTION 3: APP SHORTCUTS CONFIGURATION (res/xml/shortcuts.xml)
================================================================================
<?xml version="1.0" encoding="utf-8"?>
<shortcuts xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- App Shortcut visible in Samsung Good Lock One Hand Operation + Action Picker -->
    <shortcut
        android:shortcutId="crt_lock_trigger"
        android:enabled="true"
        android:icon="@mipmap/ic_launcher"
        android:shortcutShortLabel="@string/shortcut_lock_label"
        android:shortcutLongLabel="@string/shortcut_lock_long_label">
        <intent
            android:action="com.retro.crteffect.TRIGGER_LOCK"
            android:targetPackage="com.retro.crteffect"
            android:targetClass="com.retro.crteffect.LockTriggerActivity" />
    </shortcut>
</shortcuts>

================================================================================
SECTION 4: ACCESSIBILITY CONFIGURATION (res/xml/accessibility_config.xml)
================================================================================
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault|flagRetrieveInteractiveWindows"
    android:canTakeScreenshot="true"
    android:canPerformGestures="false"
    android:description="@string/accessibility_desc" />

================================================================================
SECTION 5: STRINGS RESOURCE (res/values/strings.xml)
================================================================================
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">CRT Screen-Off</string>
    <string name="shortcut_lock_label">CRT Lock</string>
    <string name="shortcut_lock_long_label">Trigger CRT Screen-Off Animation</string>
    <string name="accessibility_desc">Authentic CRT Screen-Off Electron Beam Animation Engine. Captures the screen on command and performs global lock action.</string>
</resources>

================================================================================
SECTION 6: SERVICE LIFECYCLE DISPATCHER (ServiceLifecycleDispatcher.kt)
================================================================================
package com.retro.crteffect

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

/**
 * Resolves the ComposeView attachment requirement inside an AccessibilityService.
 * Without this adapter, ComposeView throws ClassCastException when searching for
 * ViewTreeLifecycleOwner, ViewTreeViewModelStoreOwner, and ViewTreeSavedStateRegistryOwner.
 */
class ServiceLifecycleDispatcher : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val viewModelStoreInstance = ViewModelStore()

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = viewModelStoreInstance

    fun attach() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun detach() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStoreInstance.clear()
    }
}

================================================================================
SECTION 7: EXACT XPOSED CRT ANIMATION PIPELINE (CrtAnimationEngine.kt)
================================================================================
package com.retro.crteffect

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.pow

enum class CrtStyle {
    CLASSIC_HORIZONTAL, // Exact crt_tv.xml: Vertical collapse to line -> Horizontal snap to dot
    CLASSIC_VERTICAL    // Exact crt_tv_vertical.xml: Horizontal collapse to line -> Vertical snap to dot
}

/**
 * Ultra-low-latency, hardware-accelerated CRT animation view replicating the EXACT
 * mathematical curves and timings from:
 * lucasa831/Xposed-ScreenOffAnimation (app/src/main/res/anim/crt_tv.xml & CRT.java)
 *
 * Uses direct canvas scale around viewport center with zero matrix displacement,
 * guaranteeing seamless, pixel-perfect alignment with the underlying screen at t=0.
 */
class CrtView(
    context: Context,
    private val screenBitmap: Bitmap,
    private val style: CrtStyle = CrtStyle.CLASSIC_HORIZONTAL,
    private val durationMs: Long = 350L,
    private val onAnimationComplete: () -> Unit
) : View(context) {

    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val flashPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val dstBounds = RectF()
    private val flashBounds = RectF()

    private var progress = 0f
    private var animator: ValueAnimator? = null
    private var isCompleted = false

    init {
        setBackgroundColor(Color.BLACK)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimation()
    }

    private fun startAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                progress = animation.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    finishOnce()
                }

                override fun onAnimationCancel(animation: Animator) {
                    finishOnce()
                }
            })
            start()
        }
    }

    private fun finishOnce() {
        if (!isCompleted) {
            isCompleted = true
            onAnimationComplete()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f || screenBitmap.isRecycled) return

        // 1. Fill entire screen with pure OLED black
        canvas.drawColor(Color.BLACK)

        val t = progress
        val scaleX: Float
        val scaleY: Float
        val alpha: Float

        val phaseSplit = 0.66f

        when (style) {
            CrtStyle.CLASSIC_HORIZONTAL -> {
                if (t <= phaseSplit) {
                    val p1 = (t / phaseSplit).pow(2)
                    scaleY = 1.0f - (p1 * 0.99f)
                    scaleX = 1.0f + (p1 * 0.50f)
                } else {
                    val p2 = ((t - phaseSplit) / (1f - phaseSplit)).pow(2)
                    scaleY = 0.01f * (1.0f - p2)
                    scaleX = 1.5f * (1.0f - p2)
                }
            }
            CrtStyle.CLASSIC_VERTICAL -> {
                if (t <= phaseSplit) {
                    val p1 = (t / phaseSplit).pow(2)
                    scaleX = 1.0f - (p1 * 0.99f)
                    scaleY = 1.0f + (p1 * 0.50f)
                } else {
                    val p2 = ((t - phaseSplit) / (1f - phaseSplit)).pow(2)
                    scaleX = 0.01f * (1.0f - p2)
                    scaleY = 1.5f * (1.0f - p2)
                }
            }
        }

        alpha = if (t > 0.80f) {
            (1.0f - ((t - 0.80f) / 0.20f)).coerceIn(0f, 1f)
        } else {
            1.0f
        }

        // 2. Draw bitmap with exact canvas scaling centered at (w/2, h/2)
        canvas.save()
        canvas.scale(scaleX.coerceAtLeast(0f), scaleY.coerceAtLeast(0f), w / 2f, h / 2f)
        dstBounds.set(0f, 0f, w, h)
        bitmapPaint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
        canvas.drawBitmap(screenBitmap, null, dstBounds, bitmapPaint)
        canvas.restore()

        // 3. Phosphor high-voltage white glow flare
        if (t in 0.50f..0.85f) {
            val flashAlpha = ((1.0f - ((t - 0.50f) / 0.35f)) * 0.35f).coerceIn(0f, 1f)
            flashPaint.alpha = (flashAlpha * 255).toInt().coerceIn(0, 255)

            val halfW = (w / 2f) * scaleX.coerceAtLeast(0f)
            val halfH = (h / 2f) * scaleY.coerceAtLeast(0f)
            flashBounds.set(
                (w / 2f) - halfW,
                (h / 2f) - halfH,
                (w / 2f) + halfW,
                (h / 2f) + halfH
            )
            canvas.drawRect(flashBounds, flashPaint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
        animator = null
    }
}

================================================================================
SECTION 8: ZERO-FLICKER GESTURE TRIGGER ACTIVITY (LockTriggerActivity.kt)
================================================================================
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
 *  - Inherits shortEdges display cutout mode to eliminate status bar insets shift
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

================================================================================
SECTION 9: ACCESSIBILITY SERVICE IMPLEMENTATION (CrtAccessibilityService.kt)
================================================================================
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
                // transitions to OFF, eliminating wallpaper/app flashing.
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

================================================================================
SECTION 10: USER DASHBOARD & GOOD LOCK SETUP (MainActivity.kt)
================================================================================

package com.retro.crteffect

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    private fun isAccessibilityEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        return enabledServices.any { it.resolveInfo.serviceInfo.packageName == packageName }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("crt_prefs", Context.MODE_PRIVATE)

        setContent {
            val scrollState = rememberScrollState()
            var speedMs by remember { mutableFloatStateOf(prefs.getInt("anim_speed", 350).toFloat()) }
            var selectedStyle by remember {
                mutableStateOf(
                    prefs.getString("anim_style", CrtStyle.CLASSIC_HORIZONTAL.name) ?: CrtStyle.CLASSIC_HORIZONTAL.name
                )
            }

            var hasOverlay by remember { mutableStateOf(Settings.canDrawOverlays(this@MainActivity)) }
            var isA11yEnabled by remember { mutableStateOf(isAccessibilityEnabled()) }

            // 3 Collapsible sub-card states (initially compact/collapsed)
            var expandedOptionA by remember { mutableStateOf(false) }
            var expandedOptionB by remember { mutableStateOf(false) }
            var expandedOptionC by remember { mutableStateOf(false) }

            // Re-check permissions when returning to activity
            DisposableEffect(Unit) {
                val callback = {
                    hasOverlay = Settings.canDrawOverlays(this@MainActivity)
                    isA11yEnabled = isAccessibilityEnabled()
                }
                callback()
                onDispose { }
            }

            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF64B5F6),
                    onPrimary = Color.Black,
                    secondaryContainer = Color(0xFF1E1E1E),
                    surface = Color(0xFF292929),
                    background = Color(0xFF121212)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(top = 16.dp, start = 20.dp, end = 20.dp, bottom = 24.dp)
                            .verticalScroll(scrollState),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "CRT Screen-Off Engine",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Text(
                            text = "Authentic Xposed electron-beam animation without root.",
                            fontSize = 14.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // ── Card 1: System Permissions ──
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "1. Required System Access",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    lineHeight = 18.sp,
                                    color = Color.White
                                )
                                Text(
                                    "Enable the Accessibility Service to silently capture your active screen and execute the screen lock.",
                                    fontSize = 12.5.sp,
                                    lineHeight = 16.sp,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                                )

                                // Accessibility Button
                                Button(
                                    onClick = {
                                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isA11yEnabled) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        if (isA11yEnabled) "Accessibility Service: Active ✓" else "Enable CRT Accessibility Service",
                                        color = if (isA11yEnabled) Color.White else Color.Black,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Overlay Button with responsive feedback
                                OutlinedButton(
                                    onClick = {
                                        if (hasOverlay) {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Overlay permission is already granted ✓",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        startActivity(
                                            Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:$packageName")
                                            )
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        if (hasOverlay) "Overlay Permission: Granted ✓" else "Grant Overlay Permission",
                                        color = if (hasOverlay) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // ── Card 2: Collapsible Gesture Setup Methods ──
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "2. Gesture Trigger Setup",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    lineHeight = 18.sp,
                                    color = Color.White
                                )
                                Text(
                                    "Tap an option below to view step-by-step setup instructions:",
                                    fontSize = 12.5.sp,
                                    lineHeight = 16.sp,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                                )

                                // ── Option A: Universal "Open App" Setup ──
                                SetupOptionCard(
                                    title = "Option A: Universal \"Open App\" Setup",
                                    subtitle = "Samsung Good Lock, Pixel Quick Tap, Moto, Xiaomi",
                                    isExpanded = expandedOptionA,
                                    onToggle = { expandedOptionA = !expandedOptionA }
                                ) {
                                    Text(
                                        "Works with any gesture feature supporting app launches:\n" +
                                                "• Samsung Good Lock: Select 'Start application' -> 'CRT Lock'\n" +
                                                "• Google Pixel: Settings -> System -> Gestures -> Quick Tap -> Open app -> 'CRT Lock'\n" +
                                                "• Motorola: Moto app -> Gestures -> Double tap back -> 'CRT Lock'\n" +
                                                "• Xiaomi/HyperOS: Additional settings -> Gesture shortcuts -> Back tap -> 'CRT Lock'",
                                        fontSize = 12.sp,
                                        color = Color(0xFFCBD5E1),
                                        lineHeight = 17.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // ── Option B: Samsung One Hand Operation + (Shortcuts) ──
                                SetupOptionCard(
                                    title = "Option B: Samsung One Hand Operation +",
                                    subtitle = "Mapped via Home screen shortcuts",
                                    isExpanded = expandedOptionB,
                                    onToggle = { expandedOptionB = !expandedOptionB }
                                ) {
                                    Text(
                                        "• Open Good Lock -> One Hand Operation +\n" +
                                                "• Choose your handle and swipe direction\n" +
                                                "• Select: 'Home screen shortcuts'\n" +
                                                "• Choose: 'CRT Screen-Off : CRT Lock'",
                                        fontSize = 12.sp,
                                        color = Color(0xFFCBD5E1),
                                        lineHeight = 17.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // ── Option C: Non-Samsung Devices (Edge Gestures / FNG) ──
                                SetupOptionCard(
                                    title = "Option C: Non-Samsung Devices",
                                    subtitle = "Edge Gestures & Fluid Navigation Gestures",
                                    isExpanded = expandedOptionC,
                                    onToggle = { expandedOptionC = !expandedOptionC }
                                ) {
                                    Text(
                                        "To get the exact same Good Lock edge-swipe experience on Pixel, Motorola, OnePlus, or Xiaomi devices:\n" +
                                                "• Install 'Edge Gestures' or 'Fluid Navigation Gestures (FNG)'\n" +
                                                "• Set edge swipe action -> Launch app or shortcut -> 'CRT Lock'",
                                        fontSize = 12.sp,
                                        color = Color(0xFFCBD5E1),
                                        lineHeight = 17.sp,
                                        modifier = Modifier.padding(bottom = 10.dp)
                                    )

                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                startActivity(
                                                    Intent(
                                                        Intent.ACTION_VIEW,
                                                        Uri.parse("https://play.google.com/store/apps/details?id=com.ss.edgegestures")
                                                    )
                                                )
                                            } catch (_: Exception) {}
                                        },
                                        modifier = Modifier.fillMaxWidth().height(36.dp)
                                    ) {
                                        Text("View Edge Gestures on Google Play", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // ── Card 3: Animation Preferences ──
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "3. Animation Style & Speed",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    lineHeight = 18.sp,
                                    color = Color.White
                                )

                                Text(
                                    "Direction: ${if (selectedStyle == CrtStyle.CLASSIC_HORIZONTAL.name) "Horizontal (Classic)" else "Vertical"}",
                                    fontSize = 12.5.sp,
                                    lineHeight = 16.sp,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
                                )
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    FilterChip(
                                        selected = selectedStyle == CrtStyle.CLASSIC_HORIZONTAL.name,
                                        onClick = {
                                            selectedStyle = CrtStyle.CLASSIC_HORIZONTAL.name
                                            prefs.edit().putString("anim_style", selectedStyle).apply()
                                        },
                                        label = { Text("Horizontal") },
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    FilterChip(
                                        selected = selectedStyle == CrtStyle.CLASSIC_VERTICAL.name,
                                        onClick = {
                                            selectedStyle = CrtStyle.CLASSIC_VERTICAL.name
                                            prefs.edit().putString("anim_style", selectedStyle).apply()
                                        },
                                        label = { Text("Vertical") }
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    "Animation Speed: ${speedMs.toInt()} ms",
                                    fontSize = 12.5.sp,
                                    lineHeight = 16.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Slider(
                                    value = speedMs,
                                    onValueChange = { speedMs = it },
                                    onValueChangeFinished = {
                                        prefs.edit().putInt("anim_speed", speedMs.toInt()).apply()
                                    },
                                    valueRange = 150f..800f,
                                    steps = 12
                                )
                            }
                        }

                        // Test Trigger Button
                        Button(
                            onClick = {
                                val intent = Intent(this@MainActivity, LockTriggerActivity::class.java).apply {
                                    action = "com.retro.crteffect.TRIGGER_LOCK"
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .padding(top = 4.dp)
                        ) {
                            Text("Test CRT Screen-Off Now", color = Color.Black, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact, dark-themed collapsible option card with responsive border and indicator.
 */
@Composable
fun SetupOptionCard(
    title: String,
    subtitle: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(
            1.dp,
            if (isExpanded) Color(0xFF64B5F6).copy(alpha = 0.6f) else Color(0xFF334155).copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            // First line: Card label aligned with the expanding arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    color = if (isExpanded) MaterialTheme.colorScheme.primary else Color.White,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (isExpanded) "▲" else "▼",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Subtitle with tightened vertical spacing directly beneath label
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(top = 1.dp)
            )

            // Stationary reveal: Expands downward from top without sliding content from behind the header
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = tween(220)
                ) + fadeIn(animationSpec = tween(180)),
                exit = shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = tween(200)
                ) + fadeOut(animationSpec = tween(150))
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = Color(0xFF334155), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    content()
                }
            }
        }
    }
}

================================================================================
SECTION 11: ZERO-FLICKER & CUTOUT STYLES (res/values/themes.xml)
================================================================================
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.CRTScreenOff" parent="android:Theme.Material.Light.NoActionBar" />

    <!-- Style to completely disable window entry and exit animations -->
    <style name="Animation.NoAnimation" parent="@android:style/Animation">
        <item name="android:activityOpenEnterAnimation">@null</item>
        <item name="android:activityOpenExitAnimation">@null</item>
        <item name="android:activityCloseEnterAnimation">@null</item>
        <item name="android:activityCloseExitAnimation">@null</item>
        <item name="android:taskOpenEnterAnimation">@null</item>
        <item name="android:taskOpenExitAnimation">@null</item>
        <item name="android:taskCloseEnterAnimation">@null</item>
        <item name="android:taskCloseExitAnimation">@null</item>
        <item name="android:windowEnterAnimation">@null</item>
        <item name="android:windowExitAnimation">@null</item>
    </style>

    <!-- Headless zero-flicker theme for gesture triggers -->
    <style name="Theme.TransparentNoAnimation" parent="@android:style/Theme.Translucent.NoTitleBar">
        <item name="android:windowIsTranslucent">true</item>
        <item name="android:windowBackground">@android:color/transparent</item>
        <item name="android:windowContentOverlay">@null</item>
        <item name="android:windowNoTitle">true</item>
        <item name="android:windowIsFloating">false</item>
        <item name="android:backgroundDimEnabled">false</item>
        <item name="android:windowAnimationStyle">@style/Animation.NoAnimation</item>
        <item name="android:windowDisablePreview">true</item>
        <item name="android:windowLayoutInDisplayCutoutMode">shortEdges</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="android:windowDrawsSystemBarBackgrounds">true</item>
    </style>
</resources>
