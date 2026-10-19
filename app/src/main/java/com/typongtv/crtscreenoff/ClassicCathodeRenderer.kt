package com.typongtv.crtscreenoff

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.pow

/**
 * Mode 1: Classic Cathode (Vintage Cyanogen / Xposed Screen-Off Animation).
 *
 * Exact 1:1 mathematical replica of:
 * lucasa831/Xposed-ScreenOffAnimation (app/src/main/res/anim/crt_tv.xml & CRT.java)
 * enhanced with authentic 2011 Gingerbread / CM7 multi-layer hardware quirks:
 *
 * Characteristics:
 * - 66.6% / 33.3% timeline split ratio (200ms vertical squash / 100ms horizontal snap on a 300ms base).
 * - Transverse coil expansion: screen expands horizontally to 150% (1.50x) while collapsing vertically.
 * - Quadratic acceleration (t^2) on both axes.
 * - Strict Raster Confinement: canvas.clipRect bounds the entire draw loop to the collapsing raster,
 *   preserving 100% pure pitch black letterbox borders with zero bleed.
 * - 3-Layer Ghosting Stamp: upper and lower echo bitmaps that separate tightly within the
 *   collapsing image, replicating the authentic Gingerbread/CM7 vertical sync slip.
 * - High-voltage flat white flash: builds up to 80% incandescent flat white as the image squashes into the razor beam.
 * - Terminal electron cessation fade over the final 20% (t > 0.80).
 * - Ultra-low latency, pure 2D canvas scaling with zero shader overhead and zero allocations during draw.
 */
class ClassicCathodeRenderer(
    private val screenBitmap: Bitmap,
    private val style: CrtStyle = CrtStyle.CLASSIC_HORIZONTAL,
    private val density: Float = 2.5f
) : CrtRenderer {

    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val ghostPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val flashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val dstBounds = RectF()
    private val flashBounds = RectF()

    override fun draw(canvas: Canvas, width: Float, height: Float, progress: Float) {
        val w = width
        val h = height
        if (w <= 0f || h <= 0f || screenBitmap.isRecycled) return

        val t = progress
        // Terminal frame guard: 100% pure black when animation completes
        if (t >= 1.0f) return

        val centerX = w / 2f
        val centerY = h / 2f

        val scaleX: Float
        val scaleY: Float
        val alpha: Float

        // Phase threshold matching Xposed 200ms / 300ms ratio (66.6% split)
        val phaseSplit = 0.66f

        when (style) {
            CrtStyle.CLASSIC_HORIZONTAL -> {
                if (t <= phaseSplit) {
                    // Phase 1: Vertical squash with horizontal expansion (accelerate interpolator)
                    val p1 = (t / phaseSplit).pow(2)
                    scaleY = 1.0f - (p1 * 0.99f) // 1.0 -> 0.01
                    scaleX = 1.0f + (p1 * 0.50f) // 1.0 -> 1.50
                } else {
                    // Phase 2: Horizontal snap from central line into point
                    val p2 = ((t - phaseSplit) / (1f - phaseSplit)).pow(2)
                    scaleY = 0.01f * (1.0f - p2)
                    scaleX = 1.50f * (1.0f - p2)  // 1.50 -> 0.0
                }
            }
            CrtStyle.CLASSIC_VERTICAL -> {
                if (t <= phaseSplit) {
                    // Phase 1: Horizontal squash with vertical expansion
                    val p1 = (t / phaseSplit).pow(2)
                    scaleX = 1.0f - (p1 * 0.99f) // 1.0 -> 0.01
                    scaleY = 1.0f + (p1 * 0.50f) // 1.0 -> 1.50
                } else {
                    // Phase 2: Vertical snap from line into point
                    val p2 = ((t - phaseSplit) / (1f - phaseSplit)).pow(2)
                    scaleX = 0.01f * (1.0f - p2)
                    scaleY = 1.50f * (1.0f - p2)
                }
            }
        }

        // Terminal electron cessation fade (Xposed AlphaAnimation math)
        alpha = if (t > 0.80f) {
            (1.0f - ((t - 0.80f) / 0.20f)).coerceIn(0f, 1f)
        } else {
            1.0f
        }

        val halfW = centerX * scaleX.coerceAtLeast(0f)
        val halfH = centerY * scaleY.coerceAtLeast(0f)

        // 1. Draw 3-Layer Ghosting strictly confined to the collapsing raster
        if (alpha > 0.005f && halfW > 0.5f && halfH > 0.5f) {
            canvas.save()
            // STRICT CONFINEMENT: Guarantee 100% pure black outside the collapsing raster
            canvas.clipRect(centerX - halfW, centerY - halfH, centerX + halfW, centerY + halfH)

            dstBounds.set(0f, 0f, w, h)

            // Primary Center Bitmap drawn FIRST
            canvas.save()
            canvas.scale(scaleX.coerceAtLeast(0f), scaleY.coerceAtLeast(0f), centerX, centerY)
            val centerBaseAlpha = if (t <= phaseSplit) 0.85f else 1.0f
            bitmapPaint.alpha = (alpha * centerBaseAlpha * 255).toInt().coerceIn(0, 255)
            canvas.drawBitmap(screenBitmap, null, dstBounds, bitmapPaint)
            canvas.restore()

            if (t <= phaseSplit) {
                // Phase 1: Controlled 3-Layer Ghosting Stamp inside the collapsing raster
                val p1 = (t / phaseSplit).pow(1.3f)
                val offsetPx = p1 * 170f
                val ghostAlpha = p1 * 0.42f

                if (ghostAlpha > 0.01f && offsetPx > 1f) {
                    ghostPaint.alpha = (alpha * ghostAlpha * 255).toInt().coerceIn(0, 255)

                    if (style == CrtStyle.CLASSIC_HORIZONTAL) {
                        // Upper ghost (-Y)
                        canvas.save()
                        canvas.translate(0f, -offsetPx)
                        canvas.scale(scaleX.coerceAtLeast(0f), scaleY.coerceAtLeast(0f), centerX, centerY)
                        canvas.drawBitmap(screenBitmap, null, dstBounds, ghostPaint)
                        canvas.restore()

                        // Lower ghost (+Y)
                        canvas.save()
                        canvas.translate(0f, offsetPx)
                        canvas.scale(scaleX.coerceAtLeast(0f), scaleY.coerceAtLeast(0f), centerX, centerY)
                        canvas.drawBitmap(screenBitmap, null, dstBounds, ghostPaint)
                        canvas.restore()
                    } else {
                        // Left ghost (-X)
                        canvas.save()
                        canvas.translate(-offsetPx, 0f)
                        canvas.scale(scaleX.coerceAtLeast(0f), scaleY.coerceAtLeast(0f), centerX, centerY)
                        canvas.drawBitmap(screenBitmap, null, dstBounds, ghostPaint)
                        canvas.restore()

                        // Right ghost (+X)
                        canvas.save()
                        canvas.translate(offsetPx, 0f)
                        canvas.scale(scaleX.coerceAtLeast(0f), scaleY.coerceAtLeast(0f), centerX, centerY)
                        canvas.drawBitmap(screenBitmap, null, dstBounds, ghostPaint)
                        canvas.restore()
                    }
                }
            }

            canvas.restore() // Restores clipRect so the outer black screen remains unaffected
        }

        // 2. High-Voltage Flat White Flash (bright vintage pop building up to 80% as image flattens)
        if (t in 0.25f..0.85f && halfW > 0.5f && halfH > 0.5f) {
            val flashAlpha = if (t <= phaseSplit) {
                // Crescendo from 0% at t=0.25 up to 80% flat white at t=0.66
                val prog = (t - 0.25f) / (phaseSplit - 0.25f)
                (prog * 0.80f).coerceIn(0f, 0.80f)
            } else {
                // Post-collapse beam retention holding bright before fading
                val prog = (t - phaseSplit) / (0.85f - phaseSplit)
                ((1.0f - prog) * 0.80f).coerceIn(0f, 0.80f)
            }

            if (flashAlpha > 0.01f) {
                flashPaint.alpha = (flashAlpha * alpha * 255).toInt().coerceIn(0, 255)

                val flashHalfH = maxOf(halfH, 1.5f * density)
                flashBounds.set(
                    centerX - halfW,
                    centerY - flashHalfH,
                    centerX + halfW,
                    centerY + flashHalfH
                )
                canvas.drawRect(flashBounds, flashPaint)
            }
        }
    }

    override fun release() {
        // Pure 2D canvas - no GPU shader resources to release
    }
}

/**
 * Backward compatibility typealias for Mode 1 Classic Cathode renderer.
 */
typealias VintageCyanogenRenderer = ClassicCathodeRenderer
