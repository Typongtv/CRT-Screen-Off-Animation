package com.retro.crteffect

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.PathInterpolator
import kotlin.math.pow

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
    CLASSIC("Classic (2.0x)", 2.0f, 1.0f, 0.05f, "Familiar Gingerbread / Xposed quadratic feel"),
    HEAVY("Heavy Inductance (4.0x)", 3.8f, 1.40f, 0.20f, "Heavy magnetic drag, viscous crawl & explosive drop"),
    EXTREME("Extreme Tension (6.0x)", 5.5f, 1.85f, 0.32f, "Sustained magnetic struggle, deep slow creep & guillotine plunge")
}

/**
 * Ultra-low-latency, hardware-accelerated CRT animation engine.
 *
 * Mode 1: Authentic Xposed Engine (enhancedOverdrive = false)
 *  - Exact 1:1 mathematical replica of lucasa831/Xposed-ScreenOffAnimation (crt_tv.xml & CRT.java).
 *  - Exact 0.66 phase split ratio, quadratic acceleration, 35% restrained flash.
 *
 * Mode 2: Enhanced Phosphor Overdrive (enhancedOverdrive = true)
 *  - AGSL GPU RuntimeShader (API 33+) with barrel glass curvature, exaggerated vertical
 *    chromatic split (Red up, Cyan down), vertical squish motion blur, H-sync line combing jitter,
 *    horizontal phosphor streak smear, and electron squeeze luminance overdrive.
 *  - 3-level Magnetic Coil Resistance presets with inductive time dilation and visible creep.
 *  - 100% Flat Slit Pause & Peak Phosphor Overdrive Hold.
 *  - Fast-attack Horizontal Whip to center dot.
 *  - Independent Cathode Dot Afterglow Persistence on pitch-black OLED glass.
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

    private val density = context.resources.displayMetrics.density

    // Pre-allocated paints for 60/120fps zero-allocation rendering
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val classicFlashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    }
    private val washPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    }
    private val sparkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    }
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val dotBloomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    }

    // Multi-line raster harmonic paints and matrix (Zero heap allocations in onDraw)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    }
    private val coreLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val shaderMatrix = Matrix()

    // Unit shaders (0.0 to 1.0) along X and Y axes with feathered ends
    private val unitHWhiteShader = LinearGradient(
        0f, 0f, 1f, 0f,
        intArrayOf(0x00FFFFFF, 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0x00FFFFFF),
        floatArrayOf(0f, 0.08f, 0.92f, 1.0f),
        Shader.TileMode.CLAMP
    )
    private val unitHCyanShader = LinearGradient(
        0f, 0f, 1f, 0f,
        intArrayOf(0x0000E6FF, 0xFF00E6FF.toInt(), 0xFF00E6FF.toInt(), 0x0000E6FF),
        floatArrayOf(0f, 0.15f, 0.85f, 1.0f),
        Shader.TileMode.CLAMP
    )
    private val unitHInnerShader = LinearGradient(
        0f, 0f, 1f, 0f,
        intArrayOf(0x00E0F8FF, 0xFFE0F8FF.toInt(), 0xFFE0F8FF.toInt(), 0x00E0F8FF),
        floatArrayOf(0f, 0.10f, 0.90f, 1.0f),
        Shader.TileMode.CLAMP
    )

    private val unitVWhiteShader = LinearGradient(
        0f, 0f, 0f, 1f,
        intArrayOf(0x00FFFFFF, 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0x00FFFFFF),
        floatArrayOf(0f, 0.08f, 0.92f, 1.0f),
        Shader.TileMode.CLAMP
    )
    private val unitVCyanShader = LinearGradient(
        0f, 0f, 0f, 1f,
        intArrayOf(0x0000E6FF, 0xFF00E6FF.toInt(), 0xFF00E6FF.toInt(), 0x0000E6FF),
        floatArrayOf(0f, 0.15f, 0.85f, 1.0f),
        Shader.TileMode.CLAMP
    )
    private val unitVInnerShader = LinearGradient(
        0f, 0f, 0f, 1f,
        intArrayOf(0x00E0F8FF, 0xFFE0F8FF.toInt(), 0xFFE0F8FF.toInt(), 0x00E0F8FF),
        floatArrayOf(0f, 0.10f, 0.90f, 1.0f),
        Shader.TileMode.CLAMP
    )

    // Fallback Canvas Chromatic Aberration paints (center-anchored radial scale)
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

    // AGSL RuntimeShader for API 33+ GPU Barrel Curvature & Radial Chromatic Aberration
    private var crtShader: RuntimeShader? = null
    private var bitmapShader: BitmapShader? = null
    private val shaderPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

    // AGSL GPU Cathode Dot Shader (Zero heap allocations, tri-spectral phosphor cooling)
    private var dotShader: RuntimeShader? = null
    private val dotShaderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dotBounds = RectF()

    private val dstBounds = RectF()
    private val redBounds = RectF()
    private val cyanBounds = RectF()
    private val flashBounds = RectF()
    private val bloomBounds = RectF()
    private val coreBounds = RectF()

    // Mode 2: Fast-attack horizontal whip into center dot
    private val horizontalEase = PathInterpolator(0.70f, 0.00f, 0.25f, 1.0f)

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

    companion object {
        private const val CRT_AGSL_SOURCE = """
            uniform shader image;
            uniform float2 resolution;
            uniform float verticalColorShift;
            uniform float verticalSquishBlur;
            uniform float scanlineStrength;
            uniform float scanlineDensity;
            uniform float hSyncJitter;
            uniform float phosphorSmear;
            uniform float phosphorOverdrive;
            uniform float magneticStretch;
            uniform float magneticStretchAxis;

            vec4 main(vec2 fragCoord) {
                // Center-relative normalized coordinates [-1.0, 1.0]
                vec2 uv = (fragCoord / resolution) * 2.0 - 1.0;

                // 1D Non-linear Elastic Wormhole Suction:
                // Anchored at edges (ay == 1.0 -> 1.0, clings to bezels), sucking interior content toward center equator line
                // Zero cross-axis distortion: strictly 1D vertical suction across full screen width
                if (magneticStretch > 0.001) {
                    float p = 1.0 - (magneticStretch * 0.65);
                    if (magneticStretchAxis < 0.5) {
                        float s = sign(uv.y);
                        float ay = abs(uv.y);
                        uv.y = s * pow(clamp(ay, 0.0, 1.0), p);
                    } else {
                        float s = sign(uv.x);
                        float ax = abs(uv.x);
                        uv.x = s * pow(clamp(ax, 0.0, 1.0), p);
                    }
                }

                // High-frequency H-Sync jitter & interlaced line-combing artifact from CRT magnetic field collapse
                float lineParity = (mod(floor(fragCoord.y), 2.0) - 0.5) * 2.0;
                float hCombing = lineParity * 0.0055 * hSyncJitter;
                float hWobble = sin(fragCoord.y * 0.15) * 0.0070 * hSyncJitter;
                float totalHDisplacement = hCombing + hWobble;

                // Flat-Panel Planar Coordinates with H-sync line combing (zero barrel fishbowl curvature)
                vec2 linearUV = uv;
                linearUV.x += totalHDisplacement;
                if (abs(linearUV.x) > 1.05 || abs(linearUV.y) > 1.05) {
                    return vec4(0.0, 0.0, 0.0, 0.0);
                }

                // Anamorphic Linear Chromatic Aberration:
                // Directional channel separation along collapse axis (Red shifted one way, Cyan/Blue the other)
                vec2 redUV = uv;
                vec2 blueUV = uv;
                if (magneticStretchAxis < 0.5) {
                    // Vertical collapse: Shift along Y, H-combing along X
                    redUV.x += totalHDisplacement * 1.25;
                    redUV.y -= verticalColorShift;
                    blueUV.x += totalHDisplacement * 0.75;
                    blueUV.y += verticalColorShift;
                } else {
                    // Horizontal collapse: Shift along X, V-combing along Y
                    redUV.x -= verticalColorShift;
                    redUV.y += totalHDisplacement * 1.25;
                    blueUV.x += verticalColorShift;
                    blueUV.y += totalHDisplacement * 0.75;
                }

                vec2 greenUV = linearUV;

                vec2 redCoord = (redUV + 1.0) * 0.5 * resolution;
                vec2 greenCoord = (greenUV + 1.0) * 0.5 * resolution;
                vec2 blueCoord = (blueUV + 1.0) * 0.5 * resolution;

                // SQUISH MOTION BLUR & EXAGGERATED TRUE-COLOR PIXEL GHOSTING ALONG COLLAPSE AXIS:
                // Evaluates the actual picture pixel colors across dynamic displacement offsets
                vec4 centerSample = image.eval(greenCoord);
                vec4 ghostColor = centerSample;

                if (verticalSquishBlur > 0.5) {
                    vec2 blurStep = (magneticStretchAxis < 0.5) ? vec2(0.0, verticalSquishBlur) : vec2(verticalSquishBlur, 0.0);
                    vec2 bUp1 = greenCoord - blurStep * 0.75;
                    vec2 bUp2 = greenCoord - blurStep * 1.65;
                    vec2 bDn1 = greenCoord + blurStep * 0.75;
                    vec2 bDn2 = greenCoord + blurStep * 1.65;

                    // Strongly visible true-color pixel ghost echoes (prominent vertical offset):
                    ghostColor = centerSample * 0.36 +
                                 (image.eval(bUp1) + image.eval(bDn1)) * 0.24 +
                                 (image.eval(bUp2) + image.eval(bDn2)) * 0.08;
                }

                // Horizontal phosphor persistence trail
                vec2 smearCoord = greenCoord - vec2(resolution.x * 0.012 * phosphorSmear, 0.0);
                vec4 smearSample = image.eval(smearCoord);
                vec3 finalRGB = mix(ghostColor.rgb, max(ghostColor.rgb, smearSample.rgb), phosphorSmear * 0.40);

                // Prominent Optical Prism Convergence (Red & Cyan/Blue split):
                // Distinct, rich chromatic separation tearing edges and widgets apart
                if (verticalColorShift > 0.001) {
                    float rEdge = image.eval(redCoord).r;
                    float bEdge = image.eval(blueCoord).b;
                    vec3 chromaEdge = vec3(rEdge, finalRGB.g, bEdge);
                    finalRGB = mix(finalRGB, chromaEdge, 0.65);
                }

                float a = centerSample.a;

                // Refined Cathode Beam Luminance Overdrive:
                // High-voltage excitement flaring highlights into radiant incandescent glow (not a flat whiteout slab!)
                if (phosphorOverdrive > 0.01) {
                    float luma = dot(finalRGB, vec3(0.299, 0.587, 0.114));
                    // Flare highlights electric white while keeping color richness in midtones and darks
                    vec3 highlightFlare = mix(finalRGB, vec3(1.0), phosphorOverdrive * 0.48);
                    finalRGB = mix(finalRGB, highlightFlare * (1.0 + phosphorOverdrive * 0.65), clamp(luma * 1.35, 0.2, 1.0));
                }

                // Micro-phosphor raster scanline modulation
                if (scanlineStrength > 0.01) {
                    float scan = sin(fragCoord.y * scanlineDensity) * 0.5 + 0.5;
                    float scanMod = 1.0 - (scan * scanlineStrength);
                    r *= scanMod;
                    g *= scanMod;
                    b *= scanMod;
                }

                // Soft horizontal edge feathering: eliminates hard 90° rectangular clipping
                float hEdgeDist = abs(uv.x);
                if (hEdgeDist > 0.88) {
                    float edgeFade = smoothstep(1.02, 0.88, hEdgeDist);
                    a *= edgeFade;
                }

                return clamp(vec4(r, g, b, a), 0.0, 1.0);
            }
        """

        private const val DOT_AGSL_SOURCE = """
            uniform float2 center;
            uniform float coreRadius;
            uniform float innerRadius;
            uniform float outerRadius;
            uniform float coolPhase;
            uniform float persistence;
            uniform float intensity;
            uniform float enableCyan;

            vec4 main(vec2 fragCoord) {
                float dist = distance(fragCoord, center);
                if (dist > outerRadius) {
                    return vec4(0.0);
                }

                // Phosphor Cooling Spectrum:
                // Searing Incandescent White (6500K) -> Vibrant Electric Cyan (#8CE8FF) -> Deep Spectral Teal (#0088AA)
                vec3 cWhite = vec3(1.0, 1.0, 1.0);
                vec3 cCyan = vec3(0.55, 0.91, 1.0);
                vec3 cTeal = vec3(0.0, 0.53, 0.67);

                vec3 coreColor;
                if (enableCyan < 0.5) {
                    coreColor = cWhite;
                } else if (coolPhase < 0.25) {
                    float f = coolPhase / 0.25;
                    coreColor = mix(cWhite, mix(cWhite, cCyan, 0.35), f);
                } else if (coolPhase < 0.65) {
                    float f = (coolPhase - 0.25) / 0.40;
                    coreColor = mix(mix(cWhite, cCyan, 0.35), cCyan, f);
                } else {
                    float f = (coolPhase - 0.65) / 0.35;
                    coreColor = mix(cCyan, cTeal, f);
                }

                vec3 flareColor = (enableCyan > 0.5) ? cCyan : cWhite;
                vec3 haloColor = (enableCyan > 0.5) ? mix(cCyan, cTeal, 0.4) : cWhite;

                // 1. Piercing Incandescent Electron Pinpoint Core (2.4 dp base)
                float coreFactor = 0.0;
                if (dist <= coreRadius) {
                    coreFactor = smoothstep(coreRadius, 0.0, dist);
                }

                // 2. Focused Inner Phosphor Flare (12 dp base, smooth quadratic falloff)
                float flareFactor = 0.0;
                if (dist <= innerRadius) {
                    float normFlare = dist / innerRadius;
                    flareFactor = pow(clamp(1.0 - normFlare, 0.0, 1.0), 2.2);
                }

                // 3. Soft Outer Ambient Phosphor Halo (28 dp base, cubic inverse falloff: feathering into darkness with zero harsh edge)
                float normHalo = dist / outerRadius;
                float haloFactor = pow(clamp(1.0 - normHalo, 0.0, 1.0), 3.2);

                // Optical luminance hierarchy:
                // Searing pinpoint core (1.0) + vibrant flare (0.70) + delicate ambient reflection (0.14)
                float coreLuma = coreFactor * 1.0;
                float flareLuma = flareFactor * 0.70;
                float haloLuma = haloFactor * 0.14;

                vec3 rgb = coreColor * coreLuma + flareColor * flareLuma + haloColor * haloLuma;
                float alpha = clamp((coreLuma + flareLuma + haloLuma) * persistence * intensity, 0.0, 1.0);

                return vec4(rgb * alpha, alpha);
            }
        """
    }

    init {
        setBackgroundColor(Color.BLACK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!screenBitmap.isRecycled) {
                try {
                    val shader = RuntimeShader(CRT_AGSL_SOURCE)
                    val bmpShader = BitmapShader(screenBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                    shader.setInputShader("image", bmpShader)
                    crtShader = shader
                    bitmapShader = bmpShader
                } catch (_: Exception) {
                    crtShader = null
                    bitmapShader = null
                }
            }
            try {
                val dShader = RuntimeShader(DOT_AGSL_SOURCE)
                dotShader = dShader
                dotShaderPaint.shader = dShader
            } catch (_: Exception) {
                dotShader = null
            }
        }
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
            onAnimationComplete()
        }
    }

    private fun withAlpha(color: Int, alphaFraction: Float): Int {
        val a = (255 * alphaFraction.coerceIn(0f, 1f)).toInt()
        return (color and 0x00FFFFFF) or (a shl 24)
    }

    /**
     * Renders a discrete horizontal harmonic raster line with smooth gradient feathered tips.
     * Zero heap allocations: scales and translates pre-allocated unit shader via shaderMatrix.
     */
    private fun drawHorizontalLine(
        canvas: Canvas,
        cx: Float,
        y: Float,
        halfLen: Float,
        strokeWidth: Float,
        alpha: Float,
        shader: Shader,
        isCore: Boolean = false
    ) {
        if (halfLen <= 0.5f || alpha <= 0.01f || strokeWidth <= 0.1f) return
        val xStart = cx - halfLen
        val xEnd = cx + halfLen
        val len = xEnd - xStart

        shaderMatrix.reset()
        shaderMatrix.setScale(len, 1f)
        shaderMatrix.postTranslate(xStart, y)
        shader.setLocalMatrix(shaderMatrix)

        val paint = if (isCore) coreLinePaint else linePaint
        paint.shader = shader
        paint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
        paint.strokeWidth = strokeWidth
        canvas.drawLine(xStart, y, xEnd, y, paint)
    }

    /**
     * Renders a discrete vertical harmonic raster line with smooth gradient feathered tips.
     * Zero heap allocations: scales and translates pre-allocated unit shader via shaderMatrix.
     */
    private fun drawVerticalLine(
        canvas: Canvas,
        x: Float,
        cy: Float,
        halfLen: Float,
        strokeWidth: Float,
        alpha: Float,
        shader: Shader,
        isCore: Boolean = false
    ) {
        if (halfLen <= 0.5f || alpha <= 0.01f || strokeWidth <= 0.1f) return
        val yStart = cy - halfLen
        val yEnd = cy + halfLen
        val len = yEnd - yStart

        shaderMatrix.reset()
        shaderMatrix.setScale(1f, len)
        shaderMatrix.postTranslate(x, yStart)
        shader.setLocalMatrix(shaderMatrix)

        val paint = if (isCore) coreLinePaint else linePaint
        paint.shader = shader
        paint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
        paint.strokeWidth = strokeWidth
        canvas.drawLine(x, yStart, x, yEnd, paint)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f || screenBitmap.isRecycled) return

        // 1. Fill entire screen with pure OLED black
        canvas.drawColor(Color.BLACK)

        val t = progress
        val centerX = w / 2f
        val centerY = h / 2f

        if (!enhancedOverdrive) {
            // =========================================================================
            // MODE 1: REFINED RETRO CRT ENGINE
            // True-Color Pixel Ghosting, Delayed Refined Flare & Distinct Radiant Beam Zip
            // =========================================================================
            // Rebalanced timeline: 56% for vertical collapse, 44% for radiant beam zip & cathode dot
            val phaseSplit = 0.56f
            val isHorizontal = style == CrtStyle.CLASSIC_HORIZONTAL

            val scaleX: Float
            val scaleY: Float

            if (isHorizontal) {
                if (t <= phaseSplit) {
                    // Phase 1: Vertical squish to razor line
                    val p1 = (t / phaseSplit).pow(2)
                    scaleY = 1.0f - (p1 * 0.992f)
                    scaleX = 1.0f + (p1 * 0.45f)
                } else {
                    // Phase 2: Distinct, deliberate horizontal zip shut into center dot
                    val zipT = ((t - phaseSplit) / (1f - phaseSplit)).coerceIn(0f, 1f)
                    val p2 = zipT.pow(1.45f)
                    scaleY = 0.008f * (1.0f - p2)
                    scaleX = 1.45f * (1.0f - p2)
                }
            } else {
                if (t <= phaseSplit) {
                    val p1 = (t / phaseSplit).pow(2)
                    scaleX = 1.0f - (p1 * 0.992f)
                    scaleY = 1.0f + (p1 * 0.45f)
                } else {
                    val zipT = ((t - phaseSplit) / (1f - phaseSplit)).coerceIn(0f, 1f)
                    val p2 = zipT.pow(1.45f)
                    scaleX = 0.008f * (1.0f - p2)
                    scaleY = 1.45f * (1.0f - p2)
                }
            }

            // Alpha stays solid through the zip, only fading out gently during the final cathode dot extinction
            val alpha = if (t > 0.92f) {
                (1.0f - ((t - 0.92f) / 0.08f)).coerceIn(0f, 1f)
            } else {
                1.0f
            }

            val u = (t / phaseSplit).coerceIn(0f, 1f)
            val displacementNorm = if (t <= phaseSplit) u.pow(1.3f) else (1.0f - ((t - phaseSplit) / (1f - phaseSplit))).coerceIn(0f, 1f)

            // 1. Draw Bitmap with True-Color Pixel Ghosting & Subtle Prism Fringe
            if (alpha > 0.01f && scaleX > 0.001f && scaleY > 0.001f) {
                canvas.save()
                canvas.scale(scaleX.coerceAtLeast(0f), scaleY.coerceAtLeast(0f), centerX, centerY)
                dstBounds.set(0f, 0f, w, h)

                val shader = crtShader
                if (shader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Exaggerated optical prism shift (0.092 UV) & chaotic vertical ghosting
                    val colorShiftAmount = 0.092f * displacementNorm
                    val squishBlurPx = (65f * density) * displacementNorm
                    // Delayed overdrive: only surges in final ~25% of squash (u >= 0.70)
                    val overdriveAmount = if (u >= 0.70f) (((u - 0.70f) / 0.30f).pow(1.4f) * 1.15f).coerceIn(0f, 1.15f) else 0f

                    shader.setFloatUniform("resolution", w, h)
                    shader.setFloatUniform("verticalColorShift", colorShiftAmount)
                    shader.setFloatUniform("verticalSquishBlur", squishBlurPx)
                    shader.setFloatUniform("scanlineStrength", 0.35f * displacementNorm)
                    shader.setFloatUniform("scanlineDensity", 3.14159f / (1.5f * density))
                    shader.setFloatUniform("hSyncJitter", 1.65f * displacementNorm)
                    shader.setFloatUniform("phosphorSmear", 1.15f * displacementNorm)
                    shader.setFloatUniform("phosphorOverdrive", overdriveAmount)
                    shader.setFloatUniform("magneticStretch", 0f)
                    shader.setFloatUniform("magneticStretchAxis", if (isHorizontal) 0.0f else 1.0f)

                    shaderPaint.shader = shader
                    shaderPaint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                    canvas.drawRect(dstBounds, shaderPaint)
                } else if (displacementNorm > 0.05f) {
                    // CPU Canvas Fallback: Exaggerated Ghosting & Red/Cyan Prism Separation
                    val splitPx = (34f * density) * displacementNorm
                    val halfW = w / 2f
                    val halfH = h / 2f

                    // 1. Center true-color pass
                    bitmapPaint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                    canvas.drawBitmap(screenBitmap, null, dstBounds, bitmapPaint)

                    // 2. Wide true-color ghost echoes
                    if (isHorizontal) {
                        redBounds.set(centerX - halfW, centerY - halfH - splitPx, centerX + halfW, centerY + halfH - splitPx)
                        cyanBounds.set(centerX - halfW, centerY - halfH + splitPx, centerX + halfW, centerY + halfH + splitPx)
                    } else {
                        redBounds.set(centerX - halfW - splitPx, centerY - halfH, centerX + halfW - splitPx, centerY + halfH)
                        cyanBounds.set(centerX - halfW + splitPx, centerY - halfH, centerX + halfW + splitPx, centerY + halfH)
                    }
                    val ghostAlpha = (alpha * 0.45f * displacementNorm).coerceIn(0f, 0.55f)
                    bitmapPaint.alpha = (ghostAlpha * 255).toInt().coerceIn(0, 255)
                    canvas.drawBitmap(screenBitmap, null, redBounds, bitmapPaint)
                    canvas.drawBitmap(screenBitmap, null, cyanBounds, bitmapPaint)

                    // 3. Chromatic Red and Cyan fringe overlay
                    redPaint.alpha = (alpha * 0.35f * displacementNorm * 255).toInt().coerceIn(0, 255)
                    cyanPaint.alpha = (alpha * 0.35f * displacementNorm * 255).toInt().coerceIn(0, 255)
                    canvas.drawBitmap(screenBitmap, null, redBounds, redPaint)
                    canvas.drawBitmap(screenBitmap, null, cyanBounds, cyanPaint)
                } else {
                    bitmapPaint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                    canvas.drawBitmap(screenBitmap, null, dstBounds, bitmapPaint)
                }

                // Delayed, refined phosphor luminance wash (only surges in final 25% of collapse: u >= 0.75)
                if (u >= 0.75f && t <= phaseSplit) {
                    val washProg = ((u - 0.75f) / 0.25f).coerceIn(0f, 1f)
                    val surge = (washProg.pow(1.5f) * 0.58f).coerceIn(0f, 0.58f) // Toned down to 58%
                    washPaint.color = withAlpha(Color.WHITE, surge)
                    canvas.drawRect(dstBounds, washPaint)
                }

                canvas.restore()
            }

            // 2. Delayed, Toned-Down Additive Flash Bloom (peaks at 0.60 radiance at t = phaseSplit)
            if (t in 0.46f..0.72f) {
                val flashAlpha = if (t <= phaseSplit) {
                    (((t - 0.46f) / (phaseSplit - 0.46f)).pow(1.4f) * 0.60f).coerceIn(0f, 0.60f)
                } else {
                    ((1.0f - ((t - phaseSplit) / (0.72f - phaseSplit))) * 0.60f).coerceIn(0f, 0.60f)
                }
                classicFlashPaint.alpha = (flashAlpha * 255).toInt().coerceIn(0, 255)

                val halfW = centerX * scaleX.coerceAtLeast(0f)
                val halfH = centerY * scaleY.coerceAtLeast(0f)
                flashBounds.set(
                    centerX - halfW,
                    centerY - halfH,
                    centerX + halfW,
                    centerY + halfH
                )
                canvas.drawRect(flashBounds, classicFlashPaint)
            }

            // 3. Obvious Radiant Beam Zip across display (t >= 0.50 until cathode dot transition at 0.94)
            if (t >= 0.50f) {
                val beamAlpha = if (t <= phaseSplit) {
                    ((t - 0.50f) / (phaseSplit - 0.50f)).coerceIn(0f, 1f)
                } else {
                    (1.0f - ((t - phaseSplit) / (0.94f - phaseSplit))).coerceIn(0f, 1f)
                }

                if (isHorizontal) {
                    val halfW = centerX * scaleX.coerceAtLeast(0f)
                    if (halfW > 0.5f) {
                        val coreStroke = 2.6f * density
                        val haloStroke = 5.8f * density

                        // Electric cyan prism halo
                        drawHorizontalLine(
                            canvas = canvas, cx = centerX, y = centerY,
                            halfLen = halfW, strokeWidth = haloStroke,
                            alpha = beamAlpha * 0.65f, shader = unitHCyanShader
                        )
                        // Pure white incandescent core filament
                        drawHorizontalLine(
                            canvas = canvas, cx = centerX, y = centerY,
                            halfLen = halfW, strokeWidth = coreStroke,
                            alpha = beamAlpha, shader = unitHWhiteShader,
                            isCore = true
                        )
                    }
                } else {
                    val halfH = centerY * scaleY.coerceAtLeast(0f)
                    if (halfH > 0.5f) {
                        val coreStroke = 2.6f * density
                        val haloStroke = 5.8f * density

                        drawVerticalLine(
                            canvas = canvas, x = centerX, cy = centerY,
                            halfLen = halfH, strokeWidth = haloStroke,
                            alpha = beamAlpha * 0.65f, shader = unitVCyanShader
                        )
                        drawVerticalLine(
                            canvas = canvas, x = centerX, cy = centerY,
                            halfLen = halfH, strokeWidth = coreStroke,
                            alpha = beamAlpha, shader = unitVWhiteShader,
                            isCore = true
                        )
                    }
                }
            }

            // 4. Distinct Terminal Cathode Spark Dot (t >= 0.88 to 1.00)
            if (t >= 0.88f) {
                val sparkLife = ((t - 0.88f) / 0.12f).coerceIn(0f, 1f)
                val persistence = (1.0f - sparkLife).pow(1.2f)
                drawCathodeDot(
                    canvas = canvas,
                    cx = centerX,
                    cy = centerY,
                    coolPhase = sparkLife * 0.35f,
                    persistence = persistence,
                    intensity = 1.0f
                )
            }

            return
        }

        // =============================================================================
        // MODE 2: ENHANCED PHOSPHOR OVERDRIVE
        // (MAGNETIC RESISTANCE, AGSL GPU CURVATURE & RADIAL ABERRATION, SLIT PAUSE & AFTERGLOW)
        // =============================================================================
        val collapseRatio = if (afterglowMs > 0L) {
            (effectiveDurationMs.toFloat() / totalAnimDuration.toFloat()).coerceIn(0.2f, 0.95f)
        } else {
            1.0f
        }

        if (t <= collapseRatio) {
            val c = (t / collapseRatio).coerceIn(0f, 1f)

            // Stage 1: Vertical squash with selected coil resistance (0.0 -> 0.67)
            // Stage 1.5: Distinct Slit Hold & Peak White Flash (0.67 -> 0.74)
            // Stage 2: Fast-attack horizontal snap whip (0.74 -> 1.0)
            val squashEnd = 0.67f
            val slitHoldEnd = 0.74f
            val flatLineThickness = 0.006f // Razor-thin slit

            val scaleX: Float
            val scaleY: Float
            val alpha: Float
            val aberrAmount: Float

            var stretchAmount = 0f
            if (c <= squashEnd) {
                // Magnetic Coil Resistance: Viscous initial creep + high-inductance drop
                val u = (c / squashEnd).coerceIn(0f, 1f)
                val p1 = coilResistance.initialCreep * u + (1.0f - coilResistance.initialCreep) * u.pow(coilResistance.exponent)

                if (enableMagneticStretch) {
                    // Elastic Tension & Edge-Hang dynamics:
                    // Top and bottom edges cling to the display bezels while inner content is sucked vertically into the equator.
                    // Extended hold window gives the inward suction momentum before viscously decelerating against the resistance.
                    val uSnap = when (coilResistance) {
                        CoilResistance.CLASSIC -> 0.72f
                        CoilResistance.HEAVY -> 0.82f
                        CoilResistance.EXTREME -> 0.88f
                    }

                    val maxStretch = when (coilResistance) {
                        CoilResistance.CLASSIC -> 0.55f
                        CoilResistance.HEAVY -> 0.82f
                        CoilResistance.EXTREME -> 1.00f
                    }

                    if (u <= uSnap) {
                        // Phase 1: Edge Hang & Wormhole Suction Struggle
                        // Top & bottom edges hang stubbornly to physical bezels while interior content gathers inward momentum
                        val hangProgress = (u / uSnap).coerceIn(0f, 1f)

                        // Viscous resistance curve: starts gathering inward momentum, then visibly strains & slows down
                        // as peak magnetic tension is reached, prolonging the stretch before breaking loose
                        val strainedProgress = 1.0f - (1.0f - hangProgress).pow(2.2f)
                        val edgeDroop = 0.035f * hangProgress.pow(2.0f)

                        if (style == CrtStyle.CLASSIC_HORIZONTAL) {
                            scaleY = 1.0f - edgeDroop
                            scaleX = 1.0f + (0.09f * strainedProgress)
                        } else {
                            scaleX = 1.0f - edgeDroop
                            scaleY = 1.0f + (0.09f * strainedProgress)
                        }

                        // Internal 1D vertical suction builds to maximum tension as edges hold on
                        val tension = kotlin.math.sin(strainedProgress * (Math.PI.toFloat() / 2f))
                        stretchAmount = maxStretch * tension
                    } else {
                        // Phase 2: Snap Loose & Terminal Velocity Plunge
                        // Magnetic tension breaks! Stored potential energy releases in an explosive whip into the razor slit
                        val releaseProgress = ((u - uSnap) / (1.0f - uSnap)).coerceIn(0f, 1f)
                        val snapK = releaseProgress.pow(3.6f) // Explosive high-order acceleration

                        val startScale = 1.0f - 0.035f

                        if (style == CrtStyle.CLASSIC_HORIZONTAL) {
                            scaleY = startScale * (1.0f - snapK) + flatLineThickness * snapK
                            scaleX = 1.09f * (1.0f - snapK) + 1.35f * snapK
                        } else {
                            scaleX = startScale * (1.0f - snapK) + flatLineThickness * snapK
                            scaleY = 1.09f * (1.0f - snapK) + 1.35f * snapK
                        }

                        stretchAmount = maxStretch * (1.0f - releaseProgress.pow(2.2f))
                    }
                } else {
                    // Standard uniform scaling (linear collapse)
                    if (style == CrtStyle.CLASSIC_HORIZONTAL) {
                        scaleY = 1.0f - (p1 * (1.0f - flatLineThickness))
                        scaleX = 1.0f + (p1 * 0.35f)
                    } else {
                        scaleX = 1.0f - (p1 * (1.0f - flatLineThickness))
                        scaleY = 1.0f + (p1 * 0.35f)
                    }
                }

                alpha = 1.0f
                aberrAmount = (0.35f * u + 0.65f * p1) * flashIntensity
            } else if (c <= slitHoldEnd) {
                // Stage 1.5: 100% Fully squashed slit hold - burns with peak white flare
                if (style == CrtStyle.CLASSIC_HORIZONTAL) {
                    scaleY = flatLineThickness // held perfectly flat!
                    scaleX = 1.35f             // held full width!
                } else {
                    scaleX = flatLineThickness
                    scaleY = 1.35f
                }
                alpha = 1.0f
                aberrAmount = flashIntensity * 0.9f
            } else {
                // Stage 2: Explosive horizontal snap whip into center dot
                val snapProgress = ((c - slitHoldEnd) / (1.0f - slitHoldEnd)).coerceIn(0f, 1f)
                val p2 = horizontalEase.getInterpolation(snapProgress)

                if (style == CrtStyle.CLASSIC_HORIZONTAL) {
                    scaleY = flatLineThickness * (1.0f - p2)
                    scaleX = 1.35f * (1.0f - p2)
                } else {
                    scaleX = flatLineThickness * (1.0f - p2)
                    scaleY = 1.35f * (1.0f - p2)
                }
                alpha = (1.0f - snapProgress).coerceIn(0f, 1f)
                aberrAmount = (1.0f - snapProgress) * flashIntensity * 0.6f
            }

            // Draw bitmap with AGSL GPU Distortion or Fallback Radial Aberration
            if (alpha > 0.01f && scaleX > 0.001f && scaleY > 0.001f) {
                canvas.save()
                canvas.scale(scaleX.coerceAtLeast(0f), scaleY.coerceAtLeast(0f), centerX, centerY)
                dstBounds.set(0f, 0f, w, h)

                val shader = crtShader
                if (shader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Option 1: GPU AGSL RuntimeShader (Exaggerated Red/Cyan split + Squish blur + H-sync)
                    val verticalColorShiftAmount = 0.12f * aberrAmount * flashIntensity
                    val verticalSquishBlurPx = (45f * density) * aberrAmount * flashIntensity

                    val phosphorOverdriveAmount = if (c in (squashEnd * 0.35f)..slitHoldEnd) {
                        val washProg = ((c - squashEnd * 0.35f) / (slitHoldEnd - squashEnd * 0.35f)).coerceIn(0f, 1f)
                        washProg.pow(1.2f) * flashIntensity * 1.35f
                    } else {
                        0f
                    }

                    shader.setFloatUniform("resolution", w, h)
                    shader.setFloatUniform("verticalColorShift", verticalColorShiftAmount)
                    shader.setFloatUniform("verticalSquishBlur", verticalSquishBlurPx)
                    shader.setFloatUniform("scanlineStrength", 0.35f * flashIntensity)
                    shader.setFloatUniform("scanlineDensity", 3.14159f / (1.5f * density))
                    shader.setFloatUniform("hSyncJitter", 1.75f * aberrAmount)
                    shader.setFloatUniform("phosphorSmear", 1.20f * aberrAmount)
                    shader.setFloatUniform("phosphorOverdrive", phosphorOverdriveAmount)
                    shader.setFloatUniform("magneticStretch", stretchAmount)
                    shader.setFloatUniform("magneticStretchAxis", if (style == CrtStyle.CLASSIC_HORIZONTAL) 0.0f else 1.0f)

                    shaderPaint.shader = shader
                    shaderPaint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                    canvas.drawRect(dstBounds, shaderPaint)
                } else if (aberrAmount > 0.05f) {
                    // Option 2: Fallback Linear Red/Cyan Vertical Shift
                    val verticalShiftPx = (40f * density) * aberrAmount * flashIntensity

                    redPaint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                    cyanPaint.alpha = (alpha * 255).toInt().coerceIn(0, 255)

                    // Red channel: shifted UP linearly (zero radial distortion)
                    val redHalfW = w / 2f
                    val redHalfH = h / 2f
                    redBounds.set(
                        centerX - redHalfW,
                        centerY - redHalfH - verticalShiftPx,
                        centerX + redHalfW,
                        centerY + redHalfH - verticalShiftPx
                    )
                    canvas.drawBitmap(screenBitmap, null, redBounds, redPaint)

                    // Cyan channel: shifted DOWN linearly (zero radial distortion)
                    val cyanHalfW = w / 2f
                    val cyanHalfH = h / 2f
                    cyanBounds.set(
                        centerX - cyanHalfW,
                        centerY - cyanHalfH + verticalShiftPx,
                        centerX + cyanHalfW,
                        centerY + cyanHalfH + verticalShiftPx
                    )
                    canvas.drawBitmap(screenBitmap, null, cyanBounds, cyanPaint)
                } else {
                    // Standard pass at t=0
                    bitmapPaint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                    canvas.drawBitmap(screenBitmap, null, dstBounds, bitmapPaint)
                }

                // Phosphor Luminescence Overdrive Wash as image approaches line
                if (flashIntensity > 0.05f && c in (squashEnd * 0.55f)..slitHoldEnd) {
                    val washProgress = ((c - squashEnd * 0.55f) / (slitHoldEnd - squashEnd * 0.55f)).coerceIn(0f, 1f)
                    val surge = (washProgress.pow(1.3f) * 0.65f * flashIntensity).coerceIn(0f, 1f)
                    washPaint.color = withAlpha(Color.WHITE, surge)
                    canvas.drawRect(dstBounds, washPaint)
                }
                canvas.restore()
            }

            // Phosphor Slit Bloom & Hot-Core Filament
            // Non-Linear Anamorphic Tapering, Dynamic Energy Swell & Cathode Bridge
            if (flashIntensity > 0.05f && c in (squashEnd * 0.85f)..1.0f) {
                val slitProg = if (c <= slitHoldEnd) {
                    ((c - squashEnd * 0.85f) / (slitHoldEnd - squashEnd * 0.85f)).coerceIn(0f, 1f)
                } else {
                    1.0f // Maintain maximum incandescent luminance throughout the collapse into the dot!
                }
                val slitIntensity = slitProg * flashIntensity

                val collapseProgress = if (c > slitHoldEnd) {
                    ((c - slitHoldEnd) / (1.0f - slitHoldEnd)).coerceIn(0f, 1f)
                } else {
                    0f
                }
                val collapseFactor = if (c > slitHoldEnd) horizontalEase.getInterpolation(collapseProgress) else 0f
                val energyConcentration = collapseFactor.pow(0.75f)

                if (style == CrtStyle.CLASSIC_HORIZONTAL) {
                    val halfW = centerX * scaleX.coerceAtLeast(0f)

                    if (halfW > 0.5f || energyConcentration > 0.05f) {
                        val centerSparkAlpha = (slitIntensity * (0.85f + 0.15f * energyConcentration)).coerceIn(0f, 1f)

                        // 1. DYNAMIC CORONA EXPANSION: Spacing expands with Overdrive intensity (Subtle -> Dramatic -> Supernova)
                        val intensityBoost = flashIntensity.pow(1.15f)
                        val pinch = (1.0f - collapseFactor).pow(0.68f)

                        // Distinct, non-overlapping harmonic raster offsets:
                        // Guaranteed clean dark gaps between all filaments across high-DPI screens
                        val baseSatelliteOffset = (2.2f + 1.3f * intensityBoost) * density
                        val baseMidOffset = (5.2f + 3.6f * intensityBoost) * density
                        val baseOuterOffset = (9.2f + 6.8f * intensityBoost) * density
                        val baseSupernovaOffset = (14.0f + 12.0f * (flashIntensity - 0.85f).coerceAtLeast(0f)) * density

                        val satelliteYOffset = baseSatelliteOffset * pinch
                        val midYOffset = baseMidOffset * pinch
                        val outerYOffset = baseOuterOffset * pinch
                        val supernovaYOffset = baseSupernovaOffset * pinch

                        // 2. DIFFERENTIAL CASCADING LENGTHS (Stepped needle spindle taper)
                        val coreHalfW = halfW
                        val satelliteHalfW = (halfW * (0.88f - 0.15f * collapseFactor)).coerceAtLeast(0.5f)
                        val midHalfW = (halfW * (0.72f - 0.32f * collapseFactor)).coerceAtLeast(0.5f)
                        val outerHalfW = (halfW * (0.54f - 0.42f * collapseFactor)).coerceAtLeast(0.5f)
                        val supernovaHalfW = (halfW * (0.36f - 0.34f * collapseFactor)).coerceAtLeast(0.5f)

                        // 3. STAGGERED OUTSIDE-IN EXTINCTION (Sequential alpha collapse)
                        // Pair 4: Supernova Overdrive Arcs (Extinguishes early at 32% collapse)
                        val supernovaFade = (1.0f - (collapseFactor / 0.32f)).coerceIn(0f, 1f).pow(1.6f)
                        val supernovaAlpha = ((flashIntensity - 0.80f).coerceAtLeast(0f) * 0.70f * slitProg * supernovaFade).coerceIn(0f, 0.45f)

                        // Pair 3: Outer Secondary Harmonics (Extinguishes at 52% collapse)
                        val outerFade = (1.0f - (collapseFactor / 0.52f)).coerceIn(0f, 1f).pow(1.4f)
                        val outerAlpha = (slitIntensity * (0.35f + 0.15f * intensityBoost) * outerFade).coerceIn(0f, 0.55f)

                        // Pair 2: Mid Resonant Harmonics (Extinguishes at 74% collapse)
                        val midFade = (1.0f - (collapseFactor / 0.74f)).coerceIn(0f, 1f).pow(1.3f)
                        val midAlpha = (slitIntensity * (0.50f + 0.22f * intensityBoost) * midFade).coerceIn(0f, 0.75f)

                        // Pair 1: Inner Satellites (Merges into core, extinguishes at 92% collapse)
                        val satelliteFade = (1.0f - (collapseFactor / 0.92f)).coerceIn(0f, 1f).pow(1.2f)
                        val satelliteAlpha = (slitIntensity * (0.75f + 0.20f * intensityBoost) * satelliteFade).coerceIn(0f, 0.95f)

                        // Center Core Filament (Persists at 100% until pinpoint dot ignition)
                        val coreAlpha = slitIntensity.coerceIn(0f, 1f)

                        // 4. HIGH-VOLTAGE COIL FREQUENCY JITTER (Resonant hum & magnetic flutter)
                        val flutterPhase = c * 60f
                        val supernovaFlutter = kotlin.math.sin(flutterPhase * 2.1f + 2.4f) * (0.70f * density) * pinch
                        val outerFlutter = kotlin.math.sin(flutterPhase) * (0.45f * density) * pinch * flashIntensity.coerceIn(0.5f, 1.5f)
                        val midFlutter = kotlin.math.sin(flutterPhase * 1.5f + 1.2f) * (0.28f * density) * pinch * flashIntensity.coerceIn(0.5f, 1.5f)

                        // 5. STROKE THICKNESSES (Modulated by Overdrive intensity)
                        val coreStroke = ((1.6f + 1.0f * flashIntensity) * density).coerceAtLeast(1.4f * density)
                        val satelliteStroke = ((1.2f + 0.40f * flashIntensity) * density).coerceAtLeast(1.1f * density)
                        val midStroke = ((1.0f + 0.35f * flashIntensity) * density).coerceAtLeast(0.95f * density)
                        val outerStroke = ((0.9f + 0.30f * flashIntensity) * density).coerceAtLeast(0.85f * density)
                        val supernovaStroke = ((0.8f + 0.25f * (flashIntensity - 0.85f).coerceAtLeast(0f)) * density).coerceAtLeast(0.75f * density)

                        // 6. DRAW PARALLEL HARMONIC RASTER FILAMENTS (Outside-in rendering order)
                        // Pair 4: Supernova Extreme Overdrive Arcs (Only present at high overdrive)
                        if (flashIntensity > 0.85f && supernovaAlpha > 0.01f && enableAnamorphicFlare) {
                            drawHorizontalLine(
                                canvas = canvas, cx = centerX, y = centerY - supernovaYOffset - supernovaFlutter,
                                halfLen = supernovaHalfW, strokeWidth = supernovaStroke, alpha = supernovaAlpha,
                                shader = unitHCyanShader
                            )
                            drawHorizontalLine(
                                canvas = canvas, cx = centerX, y = centerY + supernovaYOffset + supernovaFlutter,
                                halfLen = supernovaHalfW, strokeWidth = supernovaStroke, alpha = supernovaAlpha,
                                shader = unitHCyanShader
                            )
                        }

                        // Pair 3: Outer Secondary Harmonics (Electric cyan)
                        if (flashIntensity > 0.35f && outerAlpha > 0.01f && enableAnamorphicFlare) {
                            drawHorizontalLine(
                                canvas = canvas, cx = centerX, y = centerY - outerYOffset + outerFlutter,
                                halfLen = outerHalfW, strokeWidth = outerStroke, alpha = outerAlpha,
                                shader = unitHCyanShader
                            )
                            drawHorizontalLine(
                                canvas = canvas, cx = centerX, y = centerY + outerYOffset - outerFlutter,
                                halfLen = outerHalfW, strokeWidth = outerStroke, alpha = outerAlpha,
                                shader = unitHCyanShader
                            )
                        }

                        // Pair 2: Mid Resonant Harmonics (Saturated Cyan / White phosphor)
                        if (midAlpha > 0.01f) {
                            val midShader = if (enableAnamorphicFlare) unitHCyanShader else unitHInnerShader
                            drawHorizontalLine(
                                canvas = canvas, cx = centerX, y = centerY - midYOffset + midFlutter,
                                halfLen = midHalfW, strokeWidth = midStroke, alpha = midAlpha,
                                shader = midShader
                            )
                            drawHorizontalLine(
                                canvas = canvas, cx = centerX, y = centerY + midYOffset - midFlutter,
                                halfLen = midHalfW, strokeWidth = midStroke, alpha = midAlpha,
                                shader = midShader
                            )
                        }

                        // Pair 1: Inner Satellites (Brilliant Ice-White / Soft Cyan)
                        if (satelliteAlpha > 0.01f) {
                            drawHorizontalLine(
                                canvas = canvas, cx = centerX, y = centerY - satelliteYOffset,
                                halfLen = satelliteHalfW, strokeWidth = satelliteStroke, alpha = satelliteAlpha,
                                shader = unitHInnerShader
                            )
                            drawHorizontalLine(
                                canvas = canvas, cx = centerX, y = centerY + satelliteYOffset,
                                halfLen = satelliteHalfW, strokeWidth = satelliteStroke, alpha = satelliteAlpha,
                                shader = unitHInnerShader
                            )
                        }

                        // Central Filament (Incandescent Pure White Searing Core)
                        if (coreAlpha > 0.01f && coreHalfW > 0.5f) {
                            drawHorizontalLine(
                                canvas = canvas, cx = centerX, y = centerY,
                                halfLen = coreHalfW, strokeWidth = coreStroke, alpha = coreAlpha,
                                shader = unitHWhiteShader,
                                isCore = true
                            )
                        }

                        // 7. CATHODE PINPOINT DOT (Ignites only when slit reaches center pinpoint)
                        if (halfW <= 3.5f * density || collapseFactor >= 0.96f) {
                            val pinProgress = if (halfW <= 3.5f * density) 1.0f else ((collapseFactor - 0.96f) / 0.04f).coerceIn(0f, 1f)
                            drawCathodeDot(
                                canvas = canvas,
                                cx = centerX,
                                cy = centerY,
                                coolPhase = 0f,
                                persistence = pinProgress,
                                intensity = centerSparkAlpha
                            )
                        }
                    }
                } else {
                    val halfH = centerY * scaleY.coerceAtLeast(0f)

                    if (halfH > 0.5f || energyConcentration > 0.05f) {
                        val centerSparkAlpha = (slitIntensity * (0.85f + 0.15f * energyConcentration)).coerceIn(0f, 1f)

                        // 1. DYNAMIC CORONA EXPANSION (Vertical collapse mode)
                        val intensityBoost = flashIntensity.pow(1.15f)
                        val pinch = (1.0f - collapseFactor).pow(0.68f)

                        val baseSatelliteOffset = (2.2f + 1.3f * intensityBoost) * density
                        val baseMidOffset = (5.2f + 3.6f * intensityBoost) * density
                        val baseOuterOffset = (9.2f + 6.8f * intensityBoost) * density
                        val baseSupernovaOffset = (14.0f + 12.0f * (flashIntensity - 0.85f).coerceAtLeast(0f)) * density

                        val satelliteXOffset = baseSatelliteOffset * pinch
                        val midXOffset = baseMidOffset * pinch
                        val outerXOffset = baseOuterOffset * pinch
                        val supernovaXOffset = baseSupernovaOffset * pinch

                        // 2. DIFFERENTIAL CASCADING LENGTHS
                        val coreHalfH = halfH
                        val satelliteHalfH = (halfH * (0.88f - 0.15f * collapseFactor)).coerceAtLeast(0.5f)
                        val midHalfH = (halfH * (0.72f - 0.32f * collapseFactor)).coerceAtLeast(0.5f)
                        val outerHalfH = (halfH * (0.54f - 0.42f * collapseFactor)).coerceAtLeast(0.5f)
                        val supernovaHalfH = (halfH * (0.36f - 0.34f * collapseFactor)).coerceAtLeast(0.5f)

                        // 3. STAGGERED OUTSIDE-IN EXTINCTION
                        val supernovaFade = (1.0f - (collapseFactor / 0.32f)).coerceIn(0f, 1f).pow(1.6f)
                        val supernovaAlpha = ((flashIntensity - 0.80f).coerceAtLeast(0f) * 0.70f * slitProg * supernovaFade).coerceIn(0f, 0.45f)

                        val outerFade = (1.0f - (collapseFactor / 0.52f)).coerceIn(0f, 1f).pow(1.4f)
                        val outerAlpha = (slitIntensity * (0.35f + 0.15f * intensityBoost) * outerFade).coerceIn(0f, 0.55f)

                        val midFade = (1.0f - (collapseFactor / 0.74f)).coerceIn(0f, 1f).pow(1.3f)
                        val midAlpha = (slitIntensity * (0.50f + 0.22f * intensityBoost) * midFade).coerceIn(0f, 0.75f)

                        val satelliteFade = (1.0f - (collapseFactor / 0.92f)).coerceIn(0f, 1f).pow(1.2f)
                        val satelliteAlpha = (slitIntensity * (0.75f + 0.20f * intensityBoost) * satelliteFade).coerceIn(0f, 0.95f)

                        val coreAlpha = slitIntensity.coerceIn(0f, 1f)

                        // 4. HIGH-VOLTAGE COIL FREQUENCY JITTER
                        val flutterPhase = c * 60f
                        val supernovaFlutter = kotlin.math.sin(flutterPhase * 2.1f + 2.4f) * (0.70f * density) * pinch
                        val outerFlutter = kotlin.math.sin(flutterPhase) * (0.45f * density) * pinch * flashIntensity.coerceIn(0.5f, 1.5f)
                        val midFlutter = kotlin.math.sin(flutterPhase * 1.5f + 1.2f) * (0.28f * density) * pinch * flashIntensity.coerceIn(0.5f, 1.5f)

                        // 5. STROKE THICKNESSES
                        val coreStroke = ((1.6f + 1.0f * flashIntensity) * density).coerceAtLeast(1.4f * density)
                        val satelliteStroke = ((1.2f + 0.40f * flashIntensity) * density).coerceAtLeast(1.1f * density)
                        val midStroke = ((1.0f + 0.35f * flashIntensity) * density).coerceAtLeast(0.95f * density)
                        val outerStroke = ((0.9f + 0.30f * flashIntensity) * density).coerceAtLeast(0.85f * density)
                        val supernovaStroke = ((0.8f + 0.25f * (flashIntensity - 0.85f).coerceAtLeast(0f)) * density).coerceAtLeast(0.75f * density)

                        // 6. DRAW PARALLEL HARMONIC RASTER FILAMENTS
                        if (flashIntensity > 0.85f && supernovaAlpha > 0.01f && enableAnamorphicFlare) {
                            drawVerticalLine(
                                canvas = canvas, x = centerX - supernovaXOffset - supernovaFlutter, cy = centerY,
                                halfLen = supernovaHalfH, strokeWidth = supernovaStroke, alpha = supernovaAlpha,
                                shader = unitVCyanShader
                            )
                            drawVerticalLine(
                                canvas = canvas, x = centerX + supernovaXOffset + supernovaFlutter, cy = centerY,
                                halfLen = supernovaHalfH, strokeWidth = supernovaStroke, alpha = supernovaAlpha,
                                shader = unitVCyanShader
                            )
                        }

                        if (flashIntensity > 0.35f && outerAlpha > 0.01f && enableAnamorphicFlare) {
                            drawVerticalLine(
                                canvas = canvas, x = centerX - outerXOffset + outerFlutter, cy = centerY,
                                halfLen = outerHalfH, strokeWidth = outerStroke, alpha = outerAlpha,
                                shader = unitVCyanShader
                            )
                            drawVerticalLine(
                                canvas = canvas, x = centerX + outerXOffset - outerFlutter, cy = centerY,
                                halfLen = outerHalfH, strokeWidth = outerStroke, alpha = outerAlpha,
                                shader = unitVCyanShader
                            )
                        }

                        if (midAlpha > 0.01f) {
                            val midShader = if (enableAnamorphicFlare) unitVCyanShader else unitVInnerShader
                            drawVerticalLine(
                                canvas = canvas, x = centerX - midXOffset + midFlutter, cy = centerY,
                                halfLen = midHalfH, strokeWidth = midStroke, alpha = midAlpha,
                                shader = midShader
                            )
                            drawVerticalLine(
                                canvas = canvas, x = centerX + midXOffset - midFlutter, cy = centerY,
                                halfLen = midHalfH, strokeWidth = midStroke, alpha = midAlpha,
                                shader = midShader
                            )
                        }

                        if (satelliteAlpha > 0.01f) {
                            drawVerticalLine(
                                canvas = canvas, x = centerX - satelliteXOffset, cy = centerY,
                                halfLen = satelliteHalfH, strokeWidth = satelliteStroke, alpha = satelliteAlpha,
                                shader = unitVInnerShader
                            )
                            drawVerticalLine(
                                canvas = canvas, x = centerX + satelliteXOffset, cy = centerY,
                                halfLen = satelliteHalfH, strokeWidth = satelliteStroke, alpha = satelliteAlpha,
                                shader = unitVInnerShader
                            )
                        }

                        if (coreAlpha > 0.01f && coreHalfH > 0.5f) {
                            drawVerticalLine(
                                canvas = canvas, x = centerX, cy = centerY,
                                halfLen = coreHalfH, strokeWidth = coreStroke, alpha = coreAlpha,
                                shader = unitVWhiteShader,
                                isCore = true
                            )
                        }

                        // 7. CATHODE PINPOINT DOT
                        if (halfH <= 3.5f * density || collapseFactor >= 0.96f) {
                            val pinProgress = if (halfH <= 3.5f * density) 1.0f else ((collapseFactor - 0.96f) / 0.04f).coerceIn(0f, 1f)
                            drawCathodeDot(
                                canvas = canvas,
                                cx = centerX,
                                cy = centerY,
                                coolPhase = 0f,
                                persistence = pinProgress,
                                intensity = centerSparkAlpha
                            )
                        }
                    }
                }
            }
        } else {
            // Sub-phase B: Dedicated Cathode Dot Afterglow Phase (screen is completely black)
            if (flashIntensity > 0.02f) {
                val a = ((t - collapseRatio) / (1.0f - collapseRatio)).coerceIn(0f, 1f)
                // Authentic Phosphor Persistence Curve:
                // Smooth exponential tail that remains clearly visible throughout 80% of duration
                val persistence = (1.0f - a).pow(1.15f)

                if (persistence > 0.005f) {
                    drawCathodeDot(
                        canvas = canvas,
                        cx = centerX,
                        cy = centerY,
                        coolPhase = a,
                        persistence = persistence,
                        intensity = flashIntensity
                    )
                }
            }
        }
    }

    /**
     * Authentic CRT Cathode Dot & Phosphor Afterglow.
     * Pure, continuous optical radial emission simulating concentrated electron beam impact:
     * 1. Piercing Incandescent Electron Pinpoint Core (2.4 dp)
     * 2. Focused Inner Phosphor Flare (12 dp, white core -> spectral electric cyan #8CE8FF -> feather)
     * 3. Soft Outer Ambient Phosphor Halo (28 dp, cubic inverse falloff internal glass reflection)
     * 4. Smooth Tri-Spectral Phosphor Cooling (pure white -> vibrant cyan -> deep teal -> extinction)
     * Zero heap allocations in onDraw.
     */
    private fun drawCathodeDot(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        coolPhase: Float,
        persistence: Float,
        intensity: Float
    ) {
        if (persistence <= 0.002f || intensity <= 0.002f) return

        val coreRadius = ((2.4f * density) * (0.35f + 0.65f * persistence)).coerceAtLeast(0.6f * density)
        val innerRadius = ((12.0f * density) * (0.40f + 0.60f * persistence)).coerceAtLeast(1.5f * density)
        val outerRadius = ((28.0f * density) * (0.50f + 0.50f * persistence)).coerceAtLeast(3.0f * density)

        val shader = dotShader
        if (shader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            shader.setFloatUniform("center", cx, cy)
            shader.setFloatUniform("coreRadius", coreRadius)
            shader.setFloatUniform("innerRadius", innerRadius)
            shader.setFloatUniform("outerRadius", outerRadius)
            shader.setFloatUniform("coolPhase", coolPhase.coerceIn(0f, 1f))
            shader.setFloatUniform("persistence", persistence.coerceIn(0f, 1f))
            shader.setFloatUniform("intensity", intensity.coerceIn(0f, 1.5f))
            shader.setFloatUniform("enableCyan", if (enableAnamorphicFlare) 1.0f else 0.0f)

            dotBounds.set(cx - outerRadius, cy - outerRadius, cx + outerRadius, cy + outerRadius)
            canvas.drawRect(dotBounds, dotShaderPaint)
            return
        }

        // Zero-Allocation CPU Fallback Canvas
        val coreA = (persistence.pow(1.1f) * intensity).coerceIn(0f, 1f)
        val innerA = (persistence.pow(1.2f) * 0.70f * intensity).coerceIn(0f, 1f)
        val outerA = (persistence.pow(1.4f) * 0.14f * intensity).coerceIn(0f, 1f)

        if (outerA > 0.005f) {
            sparkPaint.color = if (enableAnamorphicFlare) {
                Color.argb((outerA * 255).toInt(), 70, 190, 240)
            } else {
                withAlpha(Color.WHITE, outerA)
            }
            canvas.drawCircle(cx, cy, outerRadius, sparkPaint)
        }

        if (innerA > 0.005f) {
            dotBloomPaint.color = if (enableAnamorphicFlare) {
                Color.argb((innerA * 255).toInt(), 140, 232, 255)
            } else {
                withAlpha(Color.WHITE, innerA)
            }
            canvas.drawCircle(cx, cy, innerRadius, dotBloomPaint)
        }

        if (coreA > 0.005f) {
            corePaint.color = if (enableAnamorphicFlare && coolPhase > 0.35f) {
                Color.argb((coreA * 255).toInt(), 140, 232, 255)
            } else {
                withAlpha(Color.WHITE, coreA)
            }
            canvas.drawCircle(cx, cy, coreRadius, corePaint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
        animator = null
    }
}
