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
    CLASSIC_HORIZONTAL, // Exact Xposed crt_tv.xml: Vertical squash to line -> Horizontal snap to dot
    CLASSIC_VERTICAL    // Exact Xposed crt_tv_vertical.xml: Horizontal squash to line -> Vertical snap to dot
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

        // Phase threshold matching Xposed 200ms / 300ms ratio (66.6% split)
        val phaseSplit = 0.66f

        when (style) {
            CrtStyle.CLASSIC_HORIZONTAL -> {
                if (t <= phaseSplit) {
                    // Phase 1: Vertical squash with slight horizontal expansion (accelerate interpolator)
                    val p1 = (t / phaseSplit).pow(2)
                    scaleY = 1.0f - (p1 * 0.99f) // 1.0 -> 0.01
                    scaleX = 1.0f + (p1 * 0.50f) // 1.0 -> 1.5
                } else {
                    // Phase 2: Horizontal snap from central line into point
                    val p2 = ((t - phaseSplit) / (1f - phaseSplit)).pow(2)
                    scaleY = 0.01f * (1.0f - p2)
                    scaleX = 1.5f * (1.0f - p2)  // 1.5 -> 0.0
                }
            }
            CrtStyle.CLASSIC_VERTICAL -> {
                if (t <= phaseSplit) {
                    // Phase 1: Horizontal squash with vertical expansion
                    val p1 = (t / phaseSplit).pow(2)
                    scaleX = 1.0f - (p1 * 0.99f) // 1.0 -> 0.01
                    scaleY = 1.0f + (p1 * 0.50f) // 1.0 -> 1.5
                } else {
                    // Phase 2: Vertical snap from line into point
                    val p2 = ((t - phaseSplit) / (1f - phaseSplit)).pow(2)
                    scaleX = 0.01f * (1.0f - p2)
                    scaleY = 1.5f * (1.0f - p2)
                }
            }
        }

        // Terminal electron cessation fade (Xposed AlphaAnimation math)
        alpha = if (t > 0.80f) {
            (1.0f - ((t - 0.80f) / 0.20f)).coerceIn(0f, 1f)
        } else {
            1.0f
        }

        // 2. Draw bitmap with exact canvas scaling centered at (w/2, h/2)
        // At t=0, scaleX=1 and scaleY=1, mapping bitmap directly onto full view bounds with zero shift
        canvas.save()
        canvas.scale(scaleX.coerceAtLeast(0f), scaleY.coerceAtLeast(0f), w / 2f, h / 2f)
        dstBounds.set(0f, 0f, w, h)
        bitmapPaint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
        canvas.drawBitmap(screenBitmap, null, dstBounds, bitmapPaint)
        canvas.restore()

        // 3. Phosphor high-voltage white glow flare during beam concentration
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
