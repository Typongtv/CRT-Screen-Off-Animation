package com.typongtv.crtscreenoff

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import kotlin.math.pow

/**
 * Mode 1: Retro CRT Screen-Off Animation (inspired by CyanogenMod / AOSP ElectronBeam).
 *
 * 1. Inductive Magnetic Resistance: viscous initial creep resisting the pull (drops only ~1.5% at u=0.30,
 *    only ~14% at u=0.60), violently accelerating into a terminal-velocity plunge into the razor beam.
 * 2. Multi-Tap True-Color Directional Smear: samples full-color RGB along the collapse vector (+/- blur),
 *    organically preserving wallpaper blues, widgets, and app icons.
 * 3. Subtle Optical Chromatic Fringe: dialed down to 0.045f max dispersion along high-contrast boundaries.
 * 4. Crescendo Luminance Overdrive: exponential phosphor surge as the collapse snaps loose.
 * 5. Additive Phosphor Wash (PorterDuff.Mode.ADD): pure white photon surge peaking at 0.92 at t = 0.66 as image flattens.
 * 6. Radiant Beam Zip: incandescent white core + electric cyan halo zipping shut into pure blackness without a cathode dot.
 *
 * Guaranteed 100% isolated from Mode 2 (Enhanced Phosphor Overdrive).
 */
class CyanogenRenderer(
    private val screenBitmap: Bitmap,
    private val style: CrtStyle = CrtStyle.CLASSIC_HORIZONTAL,
    private val density: Float = 2.5f
) : CrtRenderer {

    // Pre-allocated paints for zero-allocation 60/120fps rendering
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

    // Additive Phosphor Wash (pure light addition, replaces flat milky overlay)
    private val additiveFlashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    }

    // Radiant beam zip filament paints
    private val beamCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val beamHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 140, 232, 255) // Electric cyan #8CE8FF
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    }

    // Fallback Canvas 2D chromatic convergence paints
    private val redPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
    }
    private val cyanPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(floatArrayOf(
            0f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    }

    // Pre-allocated bounds
    private val dstBounds = RectF()
    private val redBounds = RectF()
    private val cyanBounds = RectF()

    // Dedicated Mode 1 AGSL RuntimeShader (isolated from Mode 2)
    private var mode1Shader: RuntimeShader? = null
    private var mode1BitmapShader: BitmapShader? = null
    private val shaderPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

    companion object {
        private const val MODE1_AGSL_SOURCE = """
            uniform shader image;
            uniform float2 resolution;
            uniform float2 axisDir;
            uniform float blurDistance;
            uniform float fringeAmount;
            uniform float luminanceSurge;

            vec4 main(vec2 fragCoord) {
                vec2 normCoord = fragCoord;

                // 1. Multi-tap true-color ghost sampling along collapse axis
                vec2 offset = axisDir * blurDistance;
                vec4 sampleCenter = image.eval(normCoord);
                vec4 sampleUp     = image.eval(normCoord - offset);
                vec4 sampleDown   = image.eval(normCoord + offset);
                vec3 ghostColor   = sampleCenter.rgb * 0.50 + (sampleUp.rgb + sampleDown.rgb) * 0.25;

                // 2. Subtle optical chromatic fringe (scaled to ~0.045 max, gently blended on edges)
                vec2 fringeOffset = axisDir * fringeAmount;
                float r = image.eval(normCoord - fringeOffset).r;
                float b = image.eval(normCoord + fringeOffset).b;
                vec3 rgb = mix(ghostColor, vec3(r, ghostColor.g, b), 0.35);

                // 3. Phosphor Overdrive: Exponential luminance surge starting at u = 0.25
                if (luminanceSurge > 0.001) {
                    rgb *= (1.0 + luminanceSurge);
                    float luma = dot(rgb, vec3(0.299, 0.587, 0.114));
                    // Highlights blow out into blinding incandescent white
                    float highlightBloom = smoothstep(0.60, 1.20, luma) * 0.55 * (luminanceSurge / 1.50);
                    rgb += vec3(highlightBloom);
                }

                return vec4(clamp(rgb, 0.0, 1.0), 1.0);
            }
        """
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !screenBitmap.isRecycled) {
            try {
                val s = RuntimeShader(MODE1_AGSL_SOURCE)
                val bmpS = BitmapShader(screenBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                s.setInputShader("image", bmpS)
                mode1Shader = s
                mode1BitmapShader = bmpS
            } catch (_: Exception) {
                mode1Shader = null
                mode1BitmapShader = null
            }
        }
    }

    override fun draw(canvas: Canvas, width: Float, height: Float, progress: Float) {
        val w = width
        val h = height
        val t = progress
        val centerX = w / 2f
        val centerY = h / 2f

        // Timeline Split: 0.58f allocates 58% to viscous vertical drop and 42% dedicated to fluid horizontal zip
        val phaseSplit = 0.58f
        val scaleX: Float
        val scaleY: Float
        val zipT: Float
        val blurDistance: Float
        val fringeAmount: Float
        val luminanceSurge: Float
        val additiveWashAlpha: Float

        if (t <= phaseSplit) {
            val u = (t / phaseSplit).coerceIn(0f, 1f)
            // Calibrated resistance curve:
            // At u=0.30: 1.5% drop (visibly clings to top/bottom bezels)
            // At u=0.60: 14.0% drop (viscous sluggish struggle against coils)
            // From u=0.70 (25% drop) -> 1.00: violently breaks free & plunges into razor slit
            val p1 = (0.16f * u.pow(2.2f) + 0.84f * u.pow(4.4f)).coerceIn(0f, 1f)
            zipT = 0f

            when (style) {
                CrtStyle.CLASSIC_HORIZONTAL -> {
                    scaleY = 1.0f - (p1 * 0.992f)
                    // Flare gently during squish (barrel compression), settling to exactly 1.0f at razor collapse so horizontal zip starts at bezel edges
                    scaleX = 1.0f + (kotlin.math.sin(p1 * Math.PI.toFloat()) * 0.12f)
                }
                CrtStyle.CLASSIC_VERTICAL -> {
                    scaleX = 1.0f - (p1 * 0.992f)
                    scaleY = 1.0f + (kotlin.math.sin(p1 * Math.PI.toFloat()) * 0.12f)
                }
            }

            // Directional true-color smear distance along collapse vector
            blurDistance = (36f * density) * p1

            // Subtle optical chromatic fringe (max 0.045f)
            val dim = if (style == CrtStyle.CLASSIC_HORIZONTAL) h else w
            fringeAmount = dim * 0.045f * p1

            // Phosphor Overdrive: Exponential surge as the resistance gives way
            luminanceSurge = if (u >= 0.40f) {
                ((u - 0.40f) / 0.60f).pow(1.3f) * 1.50f
            } else {
                0f
            }

            // Additive Phosphor Wash (PorterDuff.Mode.ADD): peaks at 0.92 pure additive white at t = 0.58 as image flattens
            additiveWashAlpha = if (u >= 0.65f) {
                val prog = (u - 0.65f) / 0.35f
                (prog.pow(1.5f) * 0.92f).coerceIn(0f, 0.92f)
            } else {
                0f
            }
        } else {
            // Phase 2 (t > 0.58): Distinct, deliberate radiant beam zip shut
            zipT = ((t - phaseSplit) / (1f - phaseSplit)).coerceIn(0f, 1f)
            val p2 = zipT.pow(1.6f)

            when (style) {
                CrtStyle.CLASSIC_HORIZONTAL -> {
                    scaleY = 0.008f * (1.0f - zipT)
                    scaleX = (1.0f - p2).coerceAtLeast(0f)
                }
                CrtStyle.CLASSIC_VERTICAL -> {
                    scaleX = 0.008f * (1.0f - zipT)
                    scaleY = (1.0f - p2).coerceAtLeast(0f)
                }
            }

            blurDistance = (36f * density) * (1.0f - zipT)
            val dim = if (style == CrtStyle.CLASSIC_HORIZONTAL) h else w
            fringeAmount = dim * 0.045f * (1.0f - zipT)
            luminanceSurge = (1.0f - zipT).pow(1.1f) * 1.50f

            // Additive wash rides the collapsing line into extinction
            additiveWashAlpha = ((1.0f - zipT).pow(1.2f) * 0.92f).coerceIn(0f, 0.92f)
        }

        // Maintain solid alpha (1.0f) throughout zip, fading out as beam snaps completely shut into darkness (t > 0.95)
        val alpha = if (t > 0.95f) {
            (1.0f - ((t - 0.95f) / 0.05f)).coerceIn(0f, 1f)
        } else {
            1.0f
        }

        // Render Collapsing Frame with Multi-Tap True-Color Ghosting & Subtle Fringe
        if (alpha > 0.01f && scaleX > 0.001f && scaleY > 0.001f) {
            canvas.save()
            canvas.scale(scaleX.coerceAtLeast(0f), scaleY.coerceAtLeast(0f), centerX, centerY)
            dstBounds.set(0f, 0f, w, h)

            val shader = mode1Shader
            if (shader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                shader.setFloatUniform("resolution", w, h)
                shader.setFloatUniform(
                    "axisDir",
                    if (style == CrtStyle.CLASSIC_HORIZONTAL) 0.0f else 1.0f,
                    if (style == CrtStyle.CLASSIC_HORIZONTAL) 1.0f else 0.0f
                )
                shader.setFloatUniform("blurDistance", blurDistance)
                shader.setFloatUniform("fringeAmount", fringeAmount)
                shader.setFloatUniform("luminanceSurge", luminanceSurge)

                shaderPaint.shader = shader
                shaderPaint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                canvas.drawRect(dstBounds, shaderPaint)
            } else {
                // Canvas 2D Fallback
                if (fringeAmount > 1.0f) {
                    redPaint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                    cyanPaint.alpha = (alpha * 255).toInt().coerceIn(0, 255)

                    val pOffsetPx = fringeAmount * 0.5f
                    redBounds.set(
                        if (style == CrtStyle.CLASSIC_HORIZONTAL) 0f else -pOffsetPx,
                        if (style == CrtStyle.CLASSIC_HORIZONTAL) -pOffsetPx else 0f,
                        if (style == CrtStyle.CLASSIC_HORIZONTAL) w else w - pOffsetPx,
                        if (style == CrtStyle.CLASSIC_HORIZONTAL) h - pOffsetPx else h
                    )
                    cyanBounds.set(
                        if (style == CrtStyle.CLASSIC_HORIZONTAL) 0f else pOffsetPx,
                        if (style == CrtStyle.CLASSIC_HORIZONTAL) pOffsetPx else 0f,
                        if (style == CrtStyle.CLASSIC_HORIZONTAL) w else w + pOffsetPx,
                        if (style == CrtStyle.CLASSIC_HORIZONTAL) h + pOffsetPx else h
                    )
                    canvas.drawBitmap(screenBitmap, null, redBounds, redPaint)
                    canvas.drawBitmap(screenBitmap, null, cyanBounds, cyanPaint)
                } else {
                    bitmapPaint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                    canvas.drawBitmap(screenBitmap, null, dstBounds, bitmapPaint)
                }
            }

            // Additive Phosphor Wash (PorterDuff.Mode.ADD): builds up crescendo, peaking at 0.92 pure additive white
            if (additiveWashAlpha > 0.01f) {
                additiveFlashPaint.alpha = (additiveWashAlpha * 255).toInt().coerceIn(0, 255)
                canvas.drawRect(dstBounds, additiveFlashPaint)
            }

            canvas.restore()
        }

        // Radiant Beam Zip (Phase 2)
        if (t > phaseSplit) {
            // Filament maintains solid burning intensity until the final 6% snap into extinction
            val beamAlpha = if (zipT > 0.94f) {
                (1.0f - ((zipT - 0.94f) / 0.06f)).coerceIn(0f, 1f)
            } else {
                1.0f
            }

            if (style == CrtStyle.CLASSIC_HORIZONTAL) {
                val halfW = centerX * scaleX.coerceAtLeast(0f)
                if (halfW > 0.5f && beamAlpha > 0.01f) {
                    // Electric Cyan Halo filament
                    beamHaloPaint.strokeWidth = 4.5f * density
                    beamHaloPaint.alpha = (beamAlpha * 0.75f * 255).toInt().coerceIn(0, 255)
                    canvas.drawLine(centerX - halfW, centerY, centerX + halfW, centerY, beamHaloPaint)

                    // Incandescent White Core filament
                    beamCorePaint.strokeWidth = 2.0f * density
                    beamCorePaint.alpha = (beamAlpha * 255).toInt().coerceIn(0, 255)
                    canvas.drawLine(centerX - halfW, centerY, centerX + halfW, centerY, beamCorePaint)
                }
            } else {
                val halfH = centerY * scaleY.coerceAtLeast(0f)
                if (halfH > 0.5f && beamAlpha > 0.01f) {
                    // Electric Cyan Halo filament
                    beamHaloPaint.strokeWidth = 4.5f * density
                    beamHaloPaint.alpha = (beamAlpha * 0.75f * 255).toInt().coerceIn(0, 255)
                    canvas.drawLine(centerX, centerY - halfH, centerX, centerY + halfH, beamHaloPaint)

                    // Incandescent White Core filament
                    beamCorePaint.strokeWidth = 2.0f * density
                    beamCorePaint.alpha = (beamAlpha * 255).toInt().coerceIn(0, 255)
                    canvas.drawLine(centerX, centerY - halfH, centerX, centerY + halfH, beamCorePaint)
                }
            }
        }
    }

    override fun release() {
        mode1Shader = null
        mode1BitmapShader = null
    }
}

/**
 * Backward compatibility typealiases for Mode 1 renderer.
 */
typealias AuthenticXposedRenderer = CyanogenRenderer
typealias CyanogenCrtRenderer = CyanogenRenderer
