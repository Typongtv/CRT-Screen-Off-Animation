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
    private val density: Float = 2.5f,
    private val deflectionDynamics: DeflectionDynamics = DeflectionDynamics.STYLIZED_CREEP
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
        strokeCap = Paint.Cap.BUTT
    }
    private val beamHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 140, 232, 255) // Electric cyan #8CE8FF
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
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

        // Dynamic timeline split: standard collapse gives 72% for instant quad collapse, stylized creep gives 78%
        val phaseSplit = deflectionDynamics.phaseSplit

        // Fully dark final frame: nothing remains of the beam at the end of the timeline
        if (t >= 1.0f) return

        if (t <= phaseSplit) {
            // ── Phase 1: Vertical Deflection Collapse into Blazing Razor Slit ──
            val u = (t / phaseSplit).coerceIn(0f, 1f)
            // Easing profile:
            // - STANDARD: classic quadratic collapse (u^2.0) dropping immediately from frame 1
            // - STYLIZED_CREEP: slow inductive creep clinging to bezels before terminal plunge
            val p1 = when (deflectionDynamics) {
                DeflectionDynamics.STANDARD -> u.pow(2.0f).coerceIn(0f, 1f)
                DeflectionDynamics.STYLIZED_CREEP -> (0.14f * u.pow(1.2f) + 0.86f * u.pow(4.5f)).coerceIn(0f, 1f)
            }

            val scaleX: Float
            val scaleY: Float
            when (style) {
                CrtStyle.CLASSIC_HORIZONTAL -> {
                    scaleY = 1.0f - (p1 * 0.992f)
                    // No horizontal breathing: width stays locked so the zip begins exactly at the bezel edges
                    scaleX = 1.0f
                }
                CrtStyle.CLASSIC_VERTICAL -> {
                    scaleX = 1.0f - (p1 * 0.992f)
                    scaleY = 1.0f
                }
            }

            // Directional true-color smear distance along collapse vector
            val blurDistance = (36f * density) * p1

            // Subtle optical chromatic fringe (max 0.045f)
            val dim = if (style == CrtStyle.CLASSIC_HORIZONTAL) h else w
            val fringeAmount = dim * 0.045f * p1

            // Phosphor Overdrive: Exponential surge as the resistance gives way
            val luminanceSurge = if (u >= 0.40f) {
                ((u - 0.40f) / 0.60f).pow(1.3f) * 1.50f
            } else {
                0f
            }

            // Additive Phosphor Wash (PorterDuff.Mode.ADD): peaks at 0.92 pure additive white at t = 0.58 as image flattens
            val additiveWashAlpha = if (u >= 0.65f) {
                val prog = (u - 0.65f) / 0.35f
                (prog.pow(1.5f) * 0.92f).coerceIn(0f, 0.92f)
            } else {
                0f
            }

            // Render Collapsing Frame with Multi-Tap True-Color Ghosting & Subtle Fringe
            if (scaleX > 0.001f && scaleY > 0.001f) {
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
                    shaderPaint.alpha = 255
                    canvas.drawRect(dstBounds, shaderPaint)
                } else {
                    // Canvas 2D Fallback
                    if (fringeAmount > 1.0f) {
                        redPaint.alpha = 255
                        cyanPaint.alpha = 255

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
                        bitmapPaint.alpha = 255
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
        } else {
            // ── Phase 2: Radiant Beam Zip & Needle-Point Dissipation into Pure Darkness ──
            val zipT = ((t - phaseSplit) / (1f - phaseSplit)).coerceIn(0f, 1f)

            // Quick ease-out collapse: fast shrink from the bezels, then a needle that vanishes at the center (no dot, no bounce)
            val beamLengthFraction = (1.0f - zipT).pow(2.2f).coerceIn(0f, 1f)

            // Dissipation envelope tied to the beam LENGTH (not time): brightness only fades once it is a short needle
            val fadeX = (beamLengthFraction / 0.15f).coerceIn(0f, 1f)
            val envelope = fadeX * fadeX * (3f - 2f * fadeX)

            // Thickness starts matched to the phase-1 razor slab (no pop at hand-off) and tapers with the length
            val slabPx = (if (style == CrtStyle.CLASSIC_HORIZONTAL) h else w) * 0.008f
            val taper = beamLengthFraction.pow(0.5f) * envelope
            val haloStroke = maxOf(4.5f * density, slabPx * 0.9f) * taper
            val coreStroke = maxOf(2.0f * density, slabPx * 0.6f) * taper

            val beamAlpha = envelope

            if (beamLengthFraction > 0.001f && beamAlpha > 0.01f && coreStroke > 0.05f) {
                if (style == CrtStyle.CLASSIC_HORIZONTAL) {
                    val halfW = centerX * beamLengthFraction

                    // Electric Cyan Halo filament (tapered stroke)
                    beamHaloPaint.strokeWidth = haloStroke
                    beamHaloPaint.alpha = (beamAlpha * 0.80f * 255).toInt().coerceIn(0, 255)
                    canvas.drawLine(centerX - halfW, centerY, centerX + halfW, centerY, beamHaloPaint)

                    // Incandescent White Core filament (tapered stroke)
                    beamCorePaint.strokeWidth = coreStroke
                    beamCorePaint.alpha = (beamAlpha * 255).toInt().coerceIn(0, 255)
                    canvas.drawLine(centerX - halfW, centerY, centerX + halfW, centerY, beamCorePaint)
                } else {
                    val halfH = centerY * beamLengthFraction

                    // Electric Cyan Halo filament (tapered stroke)
                    beamHaloPaint.strokeWidth = haloStroke
                    beamHaloPaint.alpha = (beamAlpha * 0.80f * 255).toInt().coerceIn(0, 255)
                    canvas.drawLine(centerX, centerY - halfH, centerX, centerY + halfH, beamHaloPaint)

                    // Incandescent White Core filament (tapered stroke)
                    beamCorePaint.strokeWidth = coreStroke
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
