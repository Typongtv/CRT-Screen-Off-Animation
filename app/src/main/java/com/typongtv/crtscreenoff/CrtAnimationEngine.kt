package com.typongtv.crtscreenoff

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.view.animation.LinearInterpolator

enum class CrtStyle {
    CLASSIC_HORIZONTAL, // Vertical collapse to line -> Horizontal snap to dot
    CLASSIC_VERTICAL    // Horizontal collapse to line -> Vertical snap to dot
}

enum class DeflectionDynamics(
    val label: String,
    val exponent: Float,
    val timeMultiplier: Float,
    val phaseSplit: Float,
    val subtitle: String
) {
    STANDARD(
        label = "Classic Standard",
        exponent = 2.0f,
        timeMultiplier = 0.70f,
        phaseSplit = 0.72f,
        subtitle = "Linear-quadratic collapse (2.0x) with immediate drop & snappy beam zip"
    ),
    STYLIZED_CREEP(
        label = "Stylized Creep",
        exponent = 4.5f,
        timeMultiplier = 1.0f,
        phaseSplit = 0.78f,
        subtitle = "Viscous inductive drag (4.5x) with sustained creep & sudden razor plunge"
    )
}

enum class CrtAnimationMode(
    val id: Int,
    val prefValue: String,
    val title: String,
    val subtitle: String
) {
    CLASSIC_CATHODE(
        id = 1,
        prefValue = "classic_cathode",
        title = "MODE 1: CLASSIC CATHODE",
        subtitle = "Pure hardware canvas collapse with transverse coil flare & phosphor flash"
    ),
    STUDIO_DEFLECTION(
        id = 2,
        prefValue = "studio_deflection",
        title = "MODE 2: STUDIO DEFLECTION",
        subtitle = "Bezel-locked magnetic sweep with inductive dynamics & needle dissipation"
    ),
    PHOSPHOR_OVERDRIVE(
        id = 3,
        prefValue = "phosphor_overdrive",
        title = "MODE 3: PHOSPHOR OVERDRIVE",
        subtitle = "Real-time AGSL GPU phosphor simulation, coil resistance & afterglow"
    );

    companion object {
        const val PREF_KEY = "crt_animation_mode"

        fun fromPreferences(prefs: android.content.SharedPreferences): CrtAnimationMode {
            val modeStr = prefs.getString(PREF_KEY, null)
            if (modeStr != null) {
                return when (modeStr) {
                    "classic_cathode", "vintage_cyanogen" -> CLASSIC_CATHODE
                    "studio_deflection", "cyanogen_studio" -> STUDIO_DEFLECTION
                    "phosphor_overdrive" -> PHOSPHOR_OVERDRIVE
                    else -> entries.firstOrNull { it.prefValue == modeStr } ?: STUDIO_DEFLECTION
                }
            }
            // Backward-compatibility fallback for legacy boolean
            val legacyOverdrive = prefs.getBoolean("enable_enhanced_overdrive", false)
            return if (legacyOverdrive) PHOSPHOR_OVERDRIVE else STUDIO_DEFLECTION
        }
    }
}

enum class CoilResistance(
    val label: String,
    val exponent: Float,
    val timeMultiplier: Float,
    val initialCreep: Float,
    val subtitle: String
) {
    CLASSIC("Classic (2.0x)", 2.0f, 1.0f, 0.05f, "Familiar Gingerbread / retro quadratic feel"),
    HEAVY("Heavy Inductance (4.0x)", 4.6f, 1.50f, 0.20f, "Heavy magnetic drag, viscous crawl & explosive drop"),
    EXTREME("Extreme Tension (6.0x)", 6.2f, 2.00f, 0.32f, "Sustained magnetic struggle, deep slow creep & guillotine plunge")
}

/**
 * Common rendering interface for CRT screen-off animation engines.
 */
interface CrtRenderer {
    fun draw(canvas: Canvas, width: Float, height: Float, progress: Float)
    fun release() {}
}

/**
 * Ultra-low-latency, hardware-accelerated CRT animation view host.
 *
 * Delegates rendering to:
 * - [ClassicCathodeRenderer] (Mode 1: Classic Cathode Screen-Off Animation)
 * - [StudioDeflectionRenderer] (Mode 2: Studio Deflection Dynamics)
 * - [PhosphorOverdriveRenderer] (Mode 3: Modern AGSL GPU phosphor physics & magnetic coil overdrive)
 */
class CrtView(
    context: Context,
    private val screenBitmap: Bitmap,
    private val style: CrtStyle = CrtStyle.CLASSIC_HORIZONTAL,
    private val durationMs: Long = 350L,
    private val enhancedOverdrive: Boolean = false,
    private val flashIntensity: Float = 1.0f,
    private val afterglowMs: Long = 500L,
    private val coilResistance: CoilResistance = CoilResistance.EXTREME,
    private val enableMagneticStretch: Boolean = true,
    private val enableAnamorphicFlare: Boolean = true,
    private val deflectionDynamics: DeflectionDynamics = DeflectionDynamics.STYLIZED_CREEP,
    private val animationMode: CrtAnimationMode? = null,
    private val onAnimationComplete: () -> Unit
) : View(context) {

    private var progress = 0f
    private var animator: ValueAnimator? = null
    private var isCompleted = false

    // Resolve active animation mode: explicit parameter takes priority, then fallback to enhancedOverdrive
    val resolvedMode: CrtAnimationMode = animationMode ?: if (enhancedOverdrive) {
        CrtAnimationMode.PHOSPHOR_OVERDRIVE
    } else {
        CrtAnimationMode.STUDIO_DEFLECTION
    }

    // Effective duration:
    // - In Classic Cathode: exact 1:1 user slider timing
    // - In Studio Deflection: deflection dynamics time multiplier ensures standard collapse stays punchy
    // - In Phosphor Overdrive: coil inductance time multiplier & magnetic stretch dilation
    private val effectiveDurationMs: Long = when (resolvedMode) {
        CrtAnimationMode.CLASSIC_CATHODE -> durationMs
        CrtAnimationMode.STUDIO_DEFLECTION -> (durationMs.toFloat() * deflectionDynamics.timeMultiplier).toLong()
        CrtAnimationMode.PHOSPHOR_OVERDRIVE -> {
            val stretchBonus = if (enableMagneticStretch) 1.20f else 1.0f
            (durationMs.toFloat() * coilResistance.timeMultiplier * stretchBonus).toLong()
        }
    }

    // Total duration: in classic cathode and studio deflection it's effectiveDurationMs; in overdrive it accounts for resistance dilation & afterglow
    private val totalAnimDuration: Long = if (resolvedMode == CrtAnimationMode.PHOSPHOR_OVERDRIVE && afterglowMs > 0L) {
        effectiveDurationMs + afterglowMs
    } else {
        effectiveDurationMs
    }

    private val renderer: CrtRenderer = when (resolvedMode) {
        CrtAnimationMode.CLASSIC_CATHODE -> {
            ClassicCathodeRenderer(
                screenBitmap = screenBitmap,
                style = style,
                density = context.resources.displayMetrics.density
            )
        }
        CrtAnimationMode.STUDIO_DEFLECTION -> {
            StudioDeflectionRenderer(
                screenBitmap = screenBitmap,
                style = style,
                density = context.resources.displayMetrics.density,
                deflectionDynamics = deflectionDynamics
            )
        }
        CrtAnimationMode.PHOSPHOR_OVERDRIVE -> {
            PhosphorOverdriveRenderer(
                screenBitmap = screenBitmap,
                style = style,
                density = context.resources.displayMetrics.density,
                effectiveDurationMs = effectiveDurationMs,
                totalAnimDuration = totalAnimDuration,
                flashIntensity = flashIntensity,
                afterglowMs = afterglowMs,
                coilResistance = coilResistance,
                enableMagneticStretch = enableMagneticStretch,
                enableAnamorphicFlare = enableAnamorphicFlare
            )
        }
    }

    init {
        setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimation()
    }

    private fun startAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = totalAnimDuration
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
            renderer.release()
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

        // 2. Delegate to active renderer
        renderer.draw(canvas, w, h, progress)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
        animator = null
        renderer.release()
    }
}
