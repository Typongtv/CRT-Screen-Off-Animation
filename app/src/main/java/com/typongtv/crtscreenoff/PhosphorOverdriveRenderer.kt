package com.typongtv.crtscreenoff

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
import android.graphics.RectF
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.view.animation.PathInterpolator
import kotlin.math.pow

/**
 * Mode 2: Enhanced Phosphor Overdrive Engine.
 *
 * Fully isolated from Mode 1 Cyanogen renderer.
 * - AGSL GPU RuntimeShader (API 33+) with barrel curvature, vertical squish motion blur,
 *   H-sync combing jitter, horizontal phosphor smear, and electron squeeze luminance overdrive.
 * - 3-level Magnetic Coil Resistance presets with inductive time dilation.
 * - Elastic tension & edge-hang wormhole dynamics.
 * - 100% flat slit hold & peak white filament burn.
 * - Fast-attack horizontal whip to pinpoint dot.
 * - Multi-line resonant harmonic raster blooms & flutter.
 * - Dedicated Cathode Dot Afterglow Persistence on pitch-black OLED glass.
 */
class PhosphorOverdriveRenderer(
    private val screenBitmap: Bitmap,
    private val style: CrtStyle = CrtStyle.CLASSIC_HORIZONTAL,
    private val density: Float,
    private val effectiveDurationMs: Long,
    private val totalAnimDuration: Long,
    private val flashIntensity: Float = 1.0f,
    private val afterglowMs: Long = 500L,
    private val coilResistance: CoilResistance = CoilResistance.EXTREME,
    private val enableMagneticStretch: Boolean = true,
    private val enableAnamorphicFlare: Boolean = true
) : CrtRenderer {

    // Pre-allocated paints for 60/120fps zero-allocation rendering
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val washPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
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
    private val coreBounds = RectF()
    private val redBounds = RectF()
    private val cyanBounds = RectF()

    // Fast-attack horizontal whip into center dot
    private val horizontalEase = PathInterpolator(0.70f, 0.00f, 0.25f, 1.0f)

    companion object {
        private const val CRT_AGSL_SOURCE = """
            uniform shader image;
            uniform float2 resolution;
            uniform float2 center;
            uniform float2 scale;
            uniform float verticalColorShift;
            uniform float verticalSquishBlur;
            uniform float scanlineStrength;
            uniform float scanlineDensity;
            uniform float hSyncJitter;
            uniform float phosphorSmear;
            uniform float phosphorOverdrive;
            uniform float magneticStretch;
            uniform float magneticStretchAxis;
            uniform float breakoutDistance;
            uniform float breakoutIntensity;
            uniform float time;

            vec4 main(vec2 fragCoord) {
                vec2 delta = fragCoord - center;
                vec2 halfExtents = 0.5 * resolution * max(scale, float2(0.0001, 0.0001));

                // Determine distance outside the primary collapsing frame
                float distOut = 0.0;
                bool isVerticalCollapse = (magneticStretchAxis < 0.5);

                if (isVerticalCollapse) {
                    // Collapsing vertically into a horizontal line:
                    // Constrain horizontally to frame width
                    if (abs(delta.x) > halfExtents.x) {
                        return vec4(0.0, 0.0, 0.0, 0.0);
                    }
                    distOut = max(0.0, abs(delta.y) - halfExtents.y);
                } else {
                    // Collapsing horizontally into a vertical line:
                    // Constrain vertically to frame height
                    if (abs(delta.y) > halfExtents.y) {
                        return vec4(0.0, 0.0, 0.0, 0.0);
                    }
                    distOut = max(0.0, abs(delta.x) - halfExtents.x);
                }

                // High-voltage micro-flutter (deflection coil inductive ringing)
                float flutter = 0.0;
                if (breakoutIntensity > 0.05 && distOut > 0.0) {
                    float freq = isVerticalCollapse ? fragCoord.x : fragCoord.y;
                    flutter = sin(freq * 0.15 + time * 65.0) * (1.8 * breakoutIntensity);
                }

                float effectiveBreakoutLimit = max(breakoutDistance + flutter, 0.0);

                // If outside the active breakout corona, discard fragment
                if (distOut > effectiveBreakoutLimit) {
                    return vec4(0.0, 0.0, 0.0, 0.0);
                }

                // Normalized frame coordinates [-1.0, 1.0] anchored within the collapsing frame
                vec2 uv = delta / halfExtents;
                vec2 clampedUv = clamp(uv, vec2(-1.0, -1.0), vec2(1.0, 1.0));

                // 1D Non-linear Elastic Wormhole Suction (interior content only):
                if (magneticStretch > 0.001 && distOut <= 0.001) {
                    float p = 1.0 - (magneticStretch * 0.65);
                    if (magneticStretchAxis < 0.5) {
                        float s = sign(clampedUv.y);
                        float ay = abs(clampedUv.y);
                        clampedUv.y = s * pow(clamp(ay, 0.0, 1.0), p);
                    } else {
                        float s = sign(clampedUv.x);
                        float ax = abs(clampedUv.x);
                        clampedUv.x = s * pow(clamp(ax, 0.0, 1.0), p);
                    }
                }

                // Subtle authentic spherical CRT glass curvature (barrel pincushion inside frame)
                if (distOut <= 0.001) {
                    vec2 curve = clampedUv * 0.028;
                    clampedUv = clampedUv + clampedUv * vec2(curve.y * curve.y, curve.x * curve.x);
                }

                vec2 normCoord = (clampedUv + 1.0) * 0.5 * resolution;

                // High-voltage deflection coil horizontal jitter (H-sync tear line combing)
                float jitter = sin(normCoord.y * 0.12) * hSyncJitter;
                vec2 greenCoord = normCoord + vec2(jitter, 0.0);

                // Exaggerated vertical Red / Cyan split:
                // Deflection yokes compress vertically -> intense vertical spectral separation
                vec2 redCoord = greenCoord - vec2(0.0, resolution.y * verticalColorShift);
                vec2 cyanCoord = greenCoord + vec2(0.0, resolution.y * verticalColorShift);

                if (!isVerticalCollapse) {
                    redCoord = greenCoord - vec2(resolution.x * verticalColorShift, 0.0);
                    cyanCoord = greenCoord + vec2(resolution.x * verticalColorShift, 0.0);
                }

                // Directional Squish Motion Blur: 5-tap linear directional blur kernel
                vec4 rSample = image.eval(redCoord);
                vec4 cSample = image.eval(cyanCoord);
                vec4 greenSample = image.eval(greenCoord);

                if (verticalSquishBlur > 0.5) {
                    vec2 blurStep = isVerticalCollapse ? vec2(0.0, verticalSquishBlur) : vec2(verticalSquishBlur, 0.0);

                    vec4 blurR = (
                        image.eval(redCoord - blurStep * 0.60) +
                        image.eval(redCoord - blurStep * 0.30) +
                        rSample +
                        image.eval(redCoord + blurStep * 0.30) +
                        image.eval(redCoord + blurStep * 0.60)
                    ) * 0.20;

                    vec4 blurC = (
                        image.eval(cyanCoord - blurStep * 0.60) +
                        image.eval(cyanCoord - blurStep * 0.30) +
                        cSample +
                        image.eval(cyanCoord + blurStep * 0.30) +
                        image.eval(cyanCoord + blurStep * 0.60)
                    ) * 0.20;

                    rSample = mix(rSample, blurR, 0.75);
                    cSample = mix(cSample, blurC, 0.75);
                }

                // Horizontal phosphor persistence trail (beam smear on high-contrast text)
                vec2 smearCoord = greenCoord - vec2(resolution.x * 0.012 * phosphorSmear, 0.0);
                vec4 smearSample = image.eval(smearCoord);

                // -------------------------------------------------------------
                // BREAKOUT CORONA ZONE (Outside the frame borders)
                // -------------------------------------------------------------
                if (distOut > 0.001) {
                    float decaySigma = max(effectiveBreakoutLimit * 0.40, 1.0);
                    float falloff = exp(-distOut / decaySigma) * breakoutIntensity;

                    float signSide = isVerticalCollapse ? delta.y : delta.x;

                    if (signSide < 0.0) {
                        // RED / CRIMSON CHANNEL BREAKOUT:
                        // High-voltage electron overshoot exciting red phosphor above frame (or left of frame)
                        float rLuma = dot(rSample.rgb, vec3(0.299, 0.587, 0.114));
                        float intensity = max(max(rSample.r * 1.35, rLuma * 1.10), 0.75 * breakoutIntensity);

                        vec3 redCorona = vec3(
                            intensity * 1.18,
                            intensity * 0.12,
                            intensity * 0.03
                        );
                        return vec4(redCorona * falloff, falloff);
                    } else {
                        // CYAN / ELECTRIC-BLUE CHANNEL BREAKOUT:
                        // High-voltage electron undershoot exciting cyan/blue phosphor below frame (or right of frame)
                        float cLuma = dot(cSample.rgb, vec3(0.299, 0.587, 0.114));
                        float intensity = max(max(cSample.b * 1.30, cLuma * 1.10), 0.75 * breakoutIntensity);

                        vec3 cyanCorona = vec3(
                            intensity * 0.05,
                            intensity * 0.82,
                            intensity * 1.18
                        );
                        return vec4(cyanCorona * falloff, falloff);
                    }
                }

                // -------------------------------------------------------------
                // INSIDE FRAME: Full composite with phosphor overdrive
                // -------------------------------------------------------------
                float r = rSample.r;
                float b = cSample.b;
                float g = mix(greenSample.g, cSample.g, 0.50);
                g = mix(g, max(g, smearSample.g), phosphorSmear * 0.60);

                vec3 rgb = vec3(r, g, b);

                // Cathode beam squeeze luminance overdrive (hot phosphor concentration surge)
                if (phosphorOverdrive > 0.01) {
                    float luma = dot(rgb, vec3(0.299, 0.587, 0.114));
                    // Overdrive pushes peak whites to dazzling luminescence before line snap
                    rgb = mix(rgb, vec3(luma * 1.50), phosphorOverdrive * 0.45);
                    rgb *= (1.0 + phosphorOverdrive * 0.85);
                }

                // Micro-phosphor raster scanline modulation
                if (scanlineStrength > 0.01) {
                    float scanCoord = isVerticalCollapse ? fragCoord.y : fragCoord.x;
                    float scan = (sin(scanCoord * scanlineDensity) + 1.0) * 0.5;
                    rgb *= (1.0 - (scanlineStrength * 0.35 * (1.0 - scan)));
                }

                return vec4(rgb, 1.0);
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
                float dist = length(fragCoord - center);
                if (dist > outerRadius) {
                    return vec4(0.0);
                }

                // Normalized radial distances
                float normCore = dist / max(coreRadius, 0.001);
                float normInner = dist / max(innerRadius, 0.001);
                float normHalo = dist / max(outerRadius, 0.001);

                // Phosphor Cooling Spectrum:
                // Sub-phase 0.0 -> 0.35: Pure Incandescent White Core with Electric Cyan Halo
                // Sub-phase 0.35 -> 0.70: Core cools into Saturated Cyan (#00E6FF), Halo deepens to Teal (#007A99)
                // Sub-phase 0.70 -> 1.0: Deep Teal Filament dying down to pitch black extinction
                vec3 coreColor = vec3(1.0, 1.0, 1.0);
                vec3 flareColor = vec3(0.55, 0.91, 1.0);  // Brilliant Electric Cyan #8CE8FF
                vec3 haloColor = vec3(0.27, 0.75, 0.94);   // Ambient Halo Glow #46BEF0

                if (enableCyan < 0.5) {
                    // Monochrome mode: Pure White Phosphor cooling
                    flareColor = vec3(1.0, 1.0, 1.0);
                    haloColor = vec3(0.85, 0.85, 0.85);
                } else if (coolPhase > 0.35) {
                    float coolT = clamp((coolPhase - 0.35) / 0.65, 0.0, 1.0);
                    // Core transitions from pure white to intense cyan
                    coreColor = mix(vec3(1.0, 1.0, 1.0), vec3(0.0, 0.90, 1.0), coolT * 0.90);
                    // Halo shifts from cyan into deep retro teal
                    haloColor = mix(vec3(0.27, 0.75, 0.94), vec3(0.0, 0.40, 0.55), coolT);
                    flareColor = mix(vec3(0.55, 0.91, 1.0), vec3(0.0, 0.75, 0.95), coolT);
                }

                // 1. Searing Incandescent Core: extreme density, cubic smooth edge
                float coreFactor = 0.0;
                if (dist <= coreRadius) {
                    coreFactor = pow(clamp(1.0 - normCore, 0.0, 1.0), 1.5);
                }

                // 2. Focused Inner Phosphor Flare (12 dp base, smooth quadratic falloff)
                float flareFactor = 0.0;
                if (dist <= innerRadius) {
                    flareFactor = pow(clamp(1.0 - normInner, 0.0, 1.0), 2.2);
                }

                // 3. Soft Outer Ambient Phosphor Halo (28 dp base, cubic inverse falloff)
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

    override fun draw(canvas: Canvas, width: Float, height: Float, progress: Float) {
        val w = width
        val h = height
        val t = progress
        val centerX = w / 2f
        val centerY = h / 2f

        val collapseRatio = if (afterglowMs > 0L) {
            (effectiveDurationMs.toFloat() / totalAnimDuration.toFloat()).coerceIn(0.2f, 0.95f)
        } else {
            1.0f
        }

        if (t <= collapseRatio) {
            val c = (t / collapseRatio).coerceIn(0f, 1f)

            // Stage 1: Vertical squash with selected coil resistance (0.0 -> 0.74)
            // Stage 1.5: Distinct Slit Hold & Peak White Flash (0.74 -> 0.79)
            // Stage 2: Fast-attack horizontal snap whip into center dot (0.79 -> 1.0)
            val squashEnd = 0.74f
            val slitHoldEnd = 0.79f
            val flatLineThickness = 0.006f // Razor-thin slit

            val scaleX: Float
            val scaleY: Float
            val alpha: Float
            val aberrAmount: Float

            var stretchAmount = 0f
            if (c <= squashEnd) {
                // Magnetic Coil Resistance: Viscous initial creep + high-inductance drop
                val u = (c / squashEnd).coerceIn(0f, 1f)
                // Felt creep lead-in: speed starts low at the very first frame and only ever increases (no early burst,
                // no mid-way slow-down): a gentle u^1.25 term holds the image clinging to the edges, then the steep
                // coil exponent snaps it loose into the slit.
                val creepWeight = coilResistance.initialCreep.coerceAtLeast(0.12f)
                val p1 = (creepWeight * u.pow(1.25f) + (1.0f - creepWeight) * u.pow(coilResistance.exponent)).coerceIn(0f, 1f)
                val droopMax = 0.06f

                if (enableMagneticStretch) {
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
                        val hangProgress = (u / uSnap).coerceIn(0f, 1f)
                        val strainedProgress = 1.0f - (1.0f - hangProgress).pow(2.2f)
                        // Edge droop accelerates from the first frame (slow creep clinging to the edges) toward the snap point
                        val edgeDroop = droopMax * hangProgress.pow(1.5f)

                        if (style == CrtStyle.CLASSIC_HORIZONTAL) {
                            scaleY = 1.0f - edgeDroop
                            scaleX = 1.0f + (0.09f * strainedProgress)
                        } else {
                            scaleX = 1.0f - edgeDroop
                            scaleY = 1.0f + (0.09f * strainedProgress)
                        }

                        val tension = kotlin.math.sin(strainedProgress * (Math.PI.toFloat() / 2f))
                        stretchAmount = maxStretch * tension
                    } else {
                        // Phase 2: Snap Loose & Terminal Velocity Plunge into Bezel-Aligned Slit
                        val releaseProgress = ((u - uSnap) / (1.0f - uSnap)).coerceIn(0f, 1f)
                        val snapK = releaseProgress.pow(3.6f)
                        val startScale = 1.0f - droopMax

                        if (style == CrtStyle.CLASSIC_HORIZONTAL) {
                            scaleY = startScale * (1.0f - snapK) + flatLineThickness * snapK
                            scaleX = 1.09f * (1.0f - snapK) + 1.0f * snapK
                        } else {
                            scaleX = startScale * (1.0f - snapK) + flatLineThickness * snapK
                            scaleY = 1.09f * (1.0f - snapK) + 1.0f * snapK
                        }

                        stretchAmount = maxStretch * (1.0f - releaseProgress.pow(2.2f))
                    }
                } else {
                    // Standard uniform scaling (linear collapse)
                    if (style == CrtStyle.CLASSIC_HORIZONTAL) {
                        scaleY = 1.0f - (p1 * (1.0f - flatLineThickness))
                        scaleX = 1.0f + (kotlin.math.sin(p1 * Math.PI.toFloat()) * 0.10f)
                    } else {
                        scaleX = 1.0f - (p1 * (1.0f - flatLineThickness))
                        scaleY = 1.0f + (kotlin.math.sin(p1 * Math.PI.toFloat()) * 0.10f)
                    }
                }

                alpha = 1.0f
                aberrAmount = (0.35f * u + 0.65f * p1) * flashIntensity
            } else if (c <= slitHoldEnd) {
                // Stage 1.5: 100% Fully squashed slit hold - burns with peak white flare (flush with bezels)
                if (style == CrtStyle.CLASSIC_HORIZONTAL) {
                    scaleY = flatLineThickness
                    scaleX = 1.0f
                } else {
                    scaleX = flatLineThickness
                    scaleY = 1.0f
                }
                alpha = 1.0f
                aberrAmount = flashIntensity * 0.9f
            } else {
                // Stage 2: Explosive horizontal snap whip into center dot starting cleanly from bezel edges
                val snapProgress = ((c - slitHoldEnd) / (1.0f - slitHoldEnd)).coerceIn(0f, 1f)
                val p2 = horizontalEase.getInterpolation(snapProgress)

                if (style == CrtStyle.CLASSIC_HORIZONTAL) {
                    scaleY = flatLineThickness * (1.0f - p2)
                    scaleX = (1.0f - p2).coerceAtLeast(0f)
                } else {
                    scaleX = flatLineThickness * (1.0f - p2)
                    scaleY = (1.0f - p2).coerceAtLeast(0f)
                }
                alpha = (1.0f - snapProgress).coerceIn(0f, 1f)
                aberrAmount = (1.0f - snapProgress) * flashIntensity * 0.6f
            }

            // Draw bitmap with AGSL GPU Distortion or Fallback Radial Aberration
            if (alpha > 0.01f && scaleX > 0.001f && scaleY > 0.001f) {
                val halfW = (w * scaleX.coerceAtLeast(0f)) / 2f
                val halfH = (h * scaleY.coerceAtLeast(0f)) / 2f
                val isHorizontal = (style == CrtStyle.CLASSIC_HORIZONTAL)

                // Base crowning offset around the equatorial beam
                val baseCrowningPx = (36f * density) * flashIntensity * 1.15f
                val currentBreakoutPx: Float
                val currentBreakoutIntensity: Float
                var surgeBell = 0f

                if (c <= squashEnd) {
                    val u = (c / squashEnd).coerceIn(0f, 1f)
                    val baselineProgress = u.pow(2.2f)

                    // Kinetic acceleration surge window:
                    // Activates as the collapse gains speed (u > 0.40), peaks during terminal plunge (u ~ 0.75-0.85),
                    // and gracefully decays back to 0 at u = 1.0 (entering the slit hold stage).
                    val surgeProgress = ((u - 0.40f) / 0.60f).coerceIn(0f, 1f)
                    surgeBell = kotlin.math.sin(surgeProgress * Math.PI.toFloat()).pow(1.5f)
                    val surgeBoostPx = baseCrowningPx * 1.35f * surgeBell

                    currentBreakoutPx = (baseCrowningPx * baselineProgress) + surgeBoostPx
                    currentBreakoutIntensity = ((0.25f + 0.75f * baselineProgress) + (0.35f * surgeBell)).coerceIn(0f, 1.25f) * flashIntensity
                } else if (c <= slitHoldEnd) {
                    // Stage 1.5: Slit Hold peak white flash - returns exactly to the current awesome beam crowning appearance!
                    currentBreakoutPx = baseCrowningPx
                    currentBreakoutIntensity = 1.0f * flashIntensity
                } else {
                    // Stage 2: Fast horizontal snap whip - breakout tapers inward with beam
                    val snapProgress = ((c - slitHoldEnd) / (1.0f - slitHoldEnd)).coerceIn(0f, 1f)
                    currentBreakoutPx = baseCrowningPx * (1.0f - snapProgress).pow(0.8f)
                    currentBreakoutIntensity = (1.0f - snapProgress) * flashIntensity
                }

                val breakoutX = if (isHorizontal) 0f else currentBreakoutPx
                val breakoutY = if (isHorizontal) currentBreakoutPx else 0f

                dstBounds.set(
                    centerX - halfW - breakoutX,
                    centerY - halfH - breakoutY,
                    centerX + halfW + breakoutX,
                    centerY + halfH + breakoutY
                )
                coreBounds.set(
                    centerX - halfW,
                    centerY - halfH,
                    centerX + halfW,
                    centerY + halfH
                )

                val shader = crtShader
                if (shader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val surgeFactor = 1.0f + (1.25f * surgeBell)
                    val verticalColorShiftAmount = 0.055f * aberrAmount * flashIntensity * surgeFactor
                    val verticalSquishBlurPx = (28f * density) * aberrAmount * flashIntensity * surgeFactor

                    val phosphorOverdriveAmount = if (c in (squashEnd * 0.35f)..slitHoldEnd) {
                        val washProg = ((c - squashEnd * 0.35f) / (slitHoldEnd - squashEnd * 0.35f)).coerceIn(0f, 1f)
                        washProg.pow(1.2f) * flashIntensity
                    } else {
                        0f
                    }

                    shader.setFloatUniform("resolution", w, h)
                    shader.setFloatUniform("center", centerX, centerY)
                    shader.setFloatUniform("scale", scaleX.coerceAtLeast(0.0001f), scaleY.coerceAtLeast(0.0001f))
                    shader.setFloatUniform("verticalColorShift", verticalColorShiftAmount)
                    shader.setFloatUniform("verticalSquishBlur", verticalSquishBlurPx)
                    shader.setFloatUniform("scanlineStrength", 0.30f * flashIntensity)
                    shader.setFloatUniform("scanlineDensity", 3.14159f / (1.5f * density))
                    shader.setFloatUniform("hSyncJitter", 1.25f * aberrAmount)
                    shader.setFloatUniform("phosphorSmear", 0.95f * aberrAmount)
                    shader.setFloatUniform("phosphorOverdrive", phosphorOverdriveAmount)
                    shader.setFloatUniform("magneticStretch", stretchAmount)
                    shader.setFloatUniform("magneticStretchAxis", if (isHorizontal) 0.0f else 1.0f)
                    shader.setFloatUniform("breakoutDistance", currentBreakoutPx)
                    shader.setFloatUniform("breakoutIntensity", currentBreakoutIntensity)
                    shader.setFloatUniform("time", c * 10f)

                    shaderPaint.shader = shader
                    shaderPaint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                    canvas.drawRect(dstBounds, shaderPaint)
                } else if (aberrAmount > 0.05f) {
                    // Fallback Linear Red/Cyan Vertical Shift with breakout beyond bounds
                    val verticalShiftPx = (currentBreakoutPx * 0.85f).coerceAtLeast(10f * density * aberrAmount)

                    redPaint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                    cyanPaint.alpha = (alpha * 255).toInt().coerceIn(0, 255)

                    if (isHorizontal) {
                        redBounds.set(
                            centerX - halfW,
                            centerY - halfH - verticalShiftPx,
                            centerX + halfW,
                            centerY + halfH - (verticalShiftPx * 0.25f)
                        )
                        canvas.drawBitmap(screenBitmap, null, redBounds, redPaint)

                        cyanBounds.set(
                            centerX - halfW,
                            centerY - halfH + (verticalShiftPx * 0.25f),
                            centerX + halfW,
                            centerY + halfH + verticalShiftPx
                        )
                        canvas.drawBitmap(screenBitmap, null, cyanBounds, cyanPaint)
                    } else {
                        redBounds.set(
                            centerX - halfW - verticalShiftPx,
                            centerY - halfH,
                            centerX + halfW - (verticalShiftPx * 0.25f),
                            centerY + halfH
                        )
                        canvas.drawBitmap(screenBitmap, null, redBounds, redPaint)

                        cyanBounds.set(
                            centerX - halfW + (verticalShiftPx * 0.25f),
                            centerY - halfH,
                            centerX + halfW + verticalShiftPx,
                            centerY + halfH
                        )
                        canvas.drawBitmap(screenBitmap, null, cyanBounds, cyanPaint)
                    }
                } else {
                    bitmapPaint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                    canvas.drawBitmap(screenBitmap, null, coreBounds, bitmapPaint)
                }

                // Phosphor Luminescence Overdrive Wash as image approaches line
                if (flashIntensity > 0.05f && c in (squashEnd * 0.55f)..slitHoldEnd) {
                    val washProgress = ((c - squashEnd * 0.55f) / (slitHoldEnd - squashEnd * 0.55f)).coerceIn(0f, 1f)
                    val surge = (washProgress.pow(1.3f) * 0.65f * flashIntensity).coerceIn(0f, 1f)
                    washPaint.color = withAlpha(Color.WHITE, surge)
                    canvas.drawRect(coreBounds, washPaint)
                }
            }

            // Phosphor Slit Bloom & Hot-Core Filament
            if (flashIntensity > 0.05f && c in (squashEnd * 0.85f)..1.0f) {
                val slitProg = if (c <= slitHoldEnd) {
                    ((c - squashEnd * 0.85f) / (slitHoldEnd - squashEnd * 0.85f)).coerceIn(0f, 1f)
                } else {
                    1.0f
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

                        val intensityBoost = flashIntensity.pow(1.15f)
                        val pinch = (1.0f - collapseFactor).pow(0.68f)

                        val baseSatelliteOffset = (2.2f + 1.3f * intensityBoost) * density
                        val baseMidOffset = (5.2f + 3.6f * intensityBoost) * density
                        val baseOuterOffset = (9.2f + 6.8f * intensityBoost) * density
                        val baseSupernovaOffset = (14.0f + 12.0f * (flashIntensity - 0.85f).coerceAtLeast(0f)) * density

                        val satelliteYOffset = baseSatelliteOffset * pinch
                        val midYOffset = baseMidOffset * pinch
                        val outerYOffset = baseOuterOffset * pinch
                        val supernovaYOffset = baseSupernovaOffset * pinch

                        val coreHalfW = halfW
                        val satelliteHalfW = (halfW * (0.88f - 0.15f * collapseFactor)).coerceAtLeast(0.5f)
                        val midHalfW = (halfW * (0.72f - 0.32f * collapseFactor)).coerceAtLeast(0.5f)
                        val outerHalfW = (halfW * (0.54f - 0.42f * collapseFactor)).coerceAtLeast(0.5f)
                        val supernovaHalfW = (halfW * (0.36f - 0.34f * collapseFactor)).coerceAtLeast(0.5f)

                        val supernovaFade = (1.0f - (collapseFactor / 0.32f)).coerceIn(0f, 1f).pow(1.6f)
                        val supernovaAlpha = ((flashIntensity - 0.80f).coerceAtLeast(0f) * 0.70f * slitProg * supernovaFade).coerceIn(0f, 0.45f)

                        val outerFade = (1.0f - (collapseFactor / 0.52f)).coerceIn(0f, 1f).pow(1.4f)
                        val outerAlpha = (slitIntensity * (0.35f + 0.15f * intensityBoost) * outerFade).coerceIn(0f, 0.55f)

                        val midFade = (1.0f - (collapseFactor / 0.74f)).coerceIn(0f, 1f).pow(1.3f)
                        val midAlpha = (slitIntensity * (0.50f + 0.22f * intensityBoost) * midFade).coerceIn(0f, 0.75f)

                        val satelliteFade = (1.0f - (collapseFactor / 0.92f)).coerceIn(0f, 1f).pow(1.2f)
                        val satelliteAlpha = (slitIntensity * (0.75f + 0.20f * intensityBoost) * satelliteFade).coerceIn(0f, 0.95f)

                        val coreAlpha = slitIntensity.coerceIn(0f, 1f)

                        val flutterPhase = c * 60f
                        val supernovaFlutter = kotlin.math.sin(flutterPhase * 2.1f + 2.4f) * (0.70f * density) * pinch
                        val outerFlutter = kotlin.math.sin(flutterPhase) * (0.45f * density) * pinch * flashIntensity.coerceIn(0.5f, 1.5f)
                        val midFlutter = kotlin.math.sin(flutterPhase * 1.5f + 1.2f) * (0.28f * density) * pinch * flashIntensity.coerceIn(0.5f, 1.5f)

                        val coreStroke = ((1.6f + 1.0f * flashIntensity) * density).coerceAtLeast(1.4f * density)
                        val satelliteStroke = ((1.2f + 0.40f * flashIntensity) * density).coerceAtLeast(1.1f * density)
                        val midStroke = ((1.0f + 0.35f * flashIntensity) * density).coerceAtLeast(0.95f * density)
                        val outerStroke = ((0.9f + 0.30f * flashIntensity) * density).coerceAtLeast(0.85f * density)
                        val supernovaStroke = ((0.8f + 0.25f * (flashIntensity - 0.85f).coerceAtLeast(0f)) * density).coerceAtLeast(0.75f * density)

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

                        if (coreAlpha > 0.01f && coreHalfW > 0.5f) {
                            drawHorizontalLine(
                                canvas = canvas, cx = centerX, y = centerY,
                                halfLen = coreHalfW, strokeWidth = coreStroke, alpha = coreAlpha,
                                shader = unitHWhiteShader,
                                isCore = true
                            )
                        }

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

                        val coreHalfH = halfH
                        val satelliteHalfH = (halfH * (0.88f - 0.15f * collapseFactor)).coerceAtLeast(0.5f)
                        val midHalfH = (halfH * (0.72f - 0.32f * collapseFactor)).coerceAtLeast(0.5f)
                        val outerHalfH = (halfH * (0.54f - 0.42f * collapseFactor)).coerceAtLeast(0.5f)
                        val supernovaHalfH = (halfH * (0.36f - 0.34f * collapseFactor)).coerceAtLeast(0.5f)

                        val supernovaFade = (1.0f - (collapseFactor / 0.32f)).coerceIn(0f, 1f).pow(1.6f)
                        val supernovaAlpha = ((flashIntensity - 0.80f).coerceAtLeast(0f) * 0.70f * slitProg * supernovaFade).coerceIn(0f, 0.45f)

                        val outerFade = (1.0f - (collapseFactor / 0.52f)).coerceIn(0f, 1f).pow(1.4f)
                        val outerAlpha = (slitIntensity * (0.35f + 0.15f * intensityBoost) * outerFade).coerceIn(0f, 0.55f)

                        val midFade = (1.0f - (collapseFactor / 0.74f)).coerceIn(0f, 1f).pow(1.3f)
                        val midAlpha = (slitIntensity * (0.50f + 0.22f * intensityBoost) * midFade).coerceIn(0f, 0.75f)

                        val satelliteFade = (1.0f - (collapseFactor / 0.92f)).coerceIn(0f, 1f).pow(1.2f)
                        val satelliteAlpha = (slitIntensity * (0.75f + 0.20f * intensityBoost) * satelliteFade).coerceIn(0f, 0.95f)

                        val coreAlpha = slitIntensity.coerceIn(0f, 1f)

                        val flutterPhase = c * 60f
                        val supernovaFlutter = kotlin.math.sin(flutterPhase * 2.1f + 2.4f) * (0.70f * density) * pinch
                        val outerFlutter = kotlin.math.sin(flutterPhase) * (0.45f * density) * pinch * flashIntensity.coerceIn(0.5f, 1.5f)
                        val midFlutter = kotlin.math.sin(flutterPhase * 1.5f + 1.2f) * (0.28f * density) * pinch * flashIntensity.coerceIn(0.5f, 1.5f)

                        val coreStroke = ((1.6f + 1.0f * flashIntensity) * density).coerceAtLeast(1.4f * density)
                        val satelliteStroke = ((1.2f + 0.40f * flashIntensity) * density).coerceAtLeast(1.1f * density)
                        val midStroke = ((1.0f + 0.35f * flashIntensity) * density).coerceAtLeast(0.95f * density)
                        val outerStroke = ((0.9f + 0.30f * flashIntensity) * density).coerceAtLeast(0.85f * density)
                        val supernovaStroke = ((0.8f + 0.25f * (flashIntensity - 0.85f).coerceAtLeast(0f)) * density).coerceAtLeast(0.75f * density)

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

    override fun release() {
        crtShader = null
        bitmapShader = null
        dotShader = null
    }
}
