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

enum class CoilResistance(
    val label: String,
    val exponent: Float,
    val timeMultiplier: Float,
    val initialCreep: Float,
    val subtitle: String
) {
    CLASSIC("Classic (2.0x)", 2.0f, 1.0f, 0.05f, "Familiar Gingerbread / retro quadratic feel"),
    HEAVY("Heavy Inductance (4.0x)", 3.8f, 1.40f, 0.20f, "Heavy magnetic drag, viscous crawl & explosive drop"),
    EXTREME("Extreme Tension (6.0x)", 5.5f, 1.85f, 0.32f, "Sustained magnetic struggle, deep slow creep & guillotine plunge")
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
 * - [CyanogenRenderer] (Mode 1: Retro CRT Screen-Off Animation)
 * - [PhosphorOverdriveRenderer] (Mode 2: Modern AGSL GPU phosphor physics & magnetic coil overdrive)
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
    private val onAnimationComplete: () -> Unit
) : View(context) {

    private var progress = 0f
    private var animator: ValueAnimator? = null
    private var isCompleted = false

    // Effective duration: coil inductance time multiplier scales the squashing resistance phase.
    // When magnetic linear stretch is enabled, adds +20% duration padding to allow elastic strain to build momentum
    private val effectiveDurationMs: Long = if (enhancedOverdrive) {
        val stretchBonus = if (enableMagneticStretch) 1.20f else 1.0f
        (durationMs.toFloat() * coilResistance.timeMultiplier * stretchBonus).toLong()
    } else {
        durationMs
    }

    // Total duration: in classic mode it's durationMs; in enhanced mode it accounts for resistance time dilation & afterglow
    private val totalAnimDuration: Long = if (enhancedOverdrive && afterglowMs > 0L) {
        effectiveDurationMs + afterglowMs
    } else {
        effectiveDurationMs
    }

    private val renderer: CrtRenderer = if (enhancedOverdrive) {
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
    } else {
        CyanogenRenderer(
            screenBitmap = screenBitmap,
            style = style,
            density = context.resources.displayMetrics.density
        )
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
