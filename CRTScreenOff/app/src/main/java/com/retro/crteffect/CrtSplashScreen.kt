package com.retro.crteffect

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Vintage Electrifying CRT Intro Animation.
 *
 * Simulates an authentic high-voltage cathode ray tube ignition & calibration sequence:
 * 1. Cold Cathode Strike (0ms - 450ms): Pinpoint cathode spark -> razor horizontal beam slit -> degauss flash.
 * 2. Convergence Hunting & Glitch (450ms - 1850ms): Logo materializes with dynamic RGB chromatic split,
 *    horizontal sync jitter, tape-tracking slice displacement, 50Hz hum bar, and high-voltage flicker.
 * 3. Harmonic Lock & Phosphor Overdrive (1850ms - 2600ms): Chromatic channels snap into convergence,
 *    radiating an electric cyan phosphor bloom and anamorphic horizontal lens flare.
 * 4. Staggered Hand-off (2600ms - 3000ms): Signals parent to start cascading UI elements before smooth fade-out.
 *
 * Tap anywhere during intro to skip immediately.
 */
@Composable
fun CrtSplashScreen(
    onStartCascade: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeline = remember { Animatable(0f) }
    var frameCount by remember { mutableLongStateOf(0L) }
    val interactionSource = remember { MutableInteractionSource() }

    // Drive continuous frame loop for high-frequency analog CRT noise & micro-jitter
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { frameCount++ }
        }
    }

    // Drive master 3000ms timeline
    LaunchedEffect(Unit) {
        timeline.animateTo(
            targetValue = 3000f,
            animationSpec = tween(
                durationMillis = 3000,
                easing = LinearEasing
            )
        )
        onStartCascade()
        onDismiss()
    }

    val t = timeline.value

    // Master container opacity (remains solid black; screen collapses organically via CRT collapse)
    val masterAlpha = 1f

    // Skip helper
    val skipIntro = {
        onStartCascade()
        onDismiss()
    }

    // ── Phase 1 Calculations: Cathode Ignition (0ms .. 450ms) ──
    val ignitionDotRadius = when {
        t in 100f..220f -> ((t - 100f) / 120f).coerceIn(0f, 1f) * 7f
        else -> 0f
    }
    val ignitionSlitWidth = when {
        t in 200f..340f -> ((t - 200f) / 140f).coerceIn(0f, 1f)
        else -> 0f
    }
    val degaussFlashAlpha = when {
        t in 320f..460f -> {
            val p = (t - 320f) / 140f
            (1f - p) * 0.95f
        }
        else -> 0f
    }

    // ── Phase 4 Calculations: Symmetrical CRT Power-Off Collapse (2480ms .. 3000ms) ──
    val logoScaleY = when {
        t < 2480f -> 1f
        t in 2480f..2640f -> {
            val p = ((t - 2480f) / 160f).coerceIn(0f, 1f)
            1f - FastOutSlowInEasing.transform(p)
        }
        else -> 0f
    }

    val collapseSlitWidth = when {
        t in 2620f..2800f -> {
            val p = ((t - 2620f) / 180f).coerceIn(0f, 1f)
            1f - p
        }
        else -> 0f
    }

    val collapseDotRadius = when {
        t in 2780f..2980f -> {
            val p = ((t - 2780f) / 200f).coerceIn(0f, 1f)
            (1f - p) * 7f
        }
        else -> 0f
    }

    val collapseDotAlpha = when {
        t in 2780f..2980f -> {
            val p = ((t - 2780f) / 200f).coerceIn(0f, 1f)
            1f - p
        }
        else -> 0f
    }

    // ── Phase 2 Calculations: Glitch Spikes & Chromatic Aberration (450ms .. 1850ms) ──
    var spikeIntensity = 0f
    var shakeX = 0f
    var shakeY = 0f
    var glitchSliceY1 = 0f
    var glitchSliceHeight1 = 0f
    var glitchSliceOffset1 = 0f
    var glitchSliceY2 = 0f
    var glitchSliceHeight2 = 0f
    var glitchSliceOffset2 = 0f

    if (t in 450f..1850f) {
        // Spike 1 (620ms .. 760ms): Horizontal deflection kick
        if (t in 620f..760f) {
            val factor = sin(((t - 620f) / 140f) * PI.toFloat()).coerceIn(0f, 1f)
            spikeIntensity = maxOf(spikeIntensity, factor * 0.75f)
            shakeX += sin(frameCount * 1.8f) * 10f * factor
            shakeY += cos(frameCount * 2.2f) * 4f * factor
            glitchSliceY1 = 0.38f
            glitchSliceHeight1 = 0.08f
            glitchSliceOffset1 = sin(frameCount * 2.5f) * 22f * factor
        }

        // Spike 2 (1040ms .. 1220ms): Vertical sync flutter & intense tracking glitch
        if (t in 1040f..1220f) {
            val factor = sin(((t - 1040f) / 180f) * PI.toFloat()).coerceIn(0f, 1f)
            spikeIntensity = maxOf(spikeIntensity, factor * 1.0f)
            shakeX += cos(frameCount * 2.4f) * 14f * factor
            shakeY += sin(frameCount * 1.9f) * 7f * factor
            glitchSliceY1 = 0.48f
            glitchSliceHeight1 = 0.12f
            glitchSliceOffset1 = -cos(frameCount * 3.1f) * 28f * factor
            glitchSliceY2 = 0.65f
            glitchSliceHeight2 = 0.07f
            glitchSliceOffset2 = sin(frameCount * 2.0f) * 18f * factor
        }

        // Spike 3 (1440ms .. 1560ms): Resonant dampening burst
        if (t in 1440f..1560f) {
            val factor = sin(((t - 1440f) / 120f) * PI.toFloat()).coerceIn(0f, 1f)
            spikeIntensity = maxOf(spikeIntensity, factor * 0.55f)
            shakeX += sin(frameCount * 1.6f) * 7f * factor
            shakeY += cos(frameCount * 2.1f) * 3f * factor
            glitchSliceY1 = 0.52f
            glitchSliceHeight1 = 0.06f
            glitchSliceOffset1 = cos(frameCount * 2.7f) * 14f * factor
        }
    }

    // Base chromatic separation with organic analog breathing
    val baseSplit = if (t in 450f..1850f) {
        sin(t * 0.022f) * 3.5f + (sin(frameCount * 0.8f) * 1.5f)
    } else 0f

    val totalChromaX = (baseSplit + (spikeIntensity * 24f))
    val totalChromaY = (spikeIntensity * 4.5f)

    // Tube hum bar position (rolls smoothly from top to bottom)
    val humProgress = ((t * 0.45f) % 1000f) / 1000f

    // High-voltage micro-flicker (subtle brightness noise)
    val voltageFlicker = if (t in 450f..1850f && spikeIntensity > 0.1f) {
        0.88f + (sin(frameCount * 3.7f) * 0.12f)
    } else {
        0.98f + (sin(frameCount * 0.9f) * 0.02f)
    }

    // Logo base visibility (fades in smoothly from the degauss flash; holds until power-off collapse)
    val logoBaseAlpha = when {
        t < 360f -> 0f
        t in 360f..500f -> ((t - 360f) / 140f).coerceIn(0f, 1f)
        t in 500f..2640f -> 1f
        else -> 0f
    } * voltageFlicker

    // ── Phase 3 Calculations: Harmonic Lock & Phosphor Bloom (1850ms .. 2600ms) ──
    val lockProgress = when {
        t in 1850f..2150f -> ((t - 1850f) / 300f).coerceIn(0f, 1f)
        t > 2150f -> 1f
        else -> 0f
    }
    // Damped settling as convergence locks
    val effectiveChromaX = totalChromaX * (1f - lockProgress)
    val effectiveChromaY = totalChromaY * (1f - lockProgress)

    // Phosphor Overdrive Bloom: bursts at lock (1950ms .. 2480ms), decays cleanly before collapse
    val bloomIntensity = when {
        t in 1950f..2150f -> {
            val p = (t - 1950f) / 200f
            sin(p * PI.toFloat() * 0.5f)
        }
        t in 2150f..2480f -> {
            val p = (t - 2150f) / 330f
            1f - (p * 0.70f) // Soft persistent glow after peak
        }
        t in 2480f..2620f -> {
            val p = (t - 2480f) / 140f
            0.30f * (1f - p) // Smooth fade down as beam collapses
        }
        else -> 0f
    }

    // Anamorphic flare streak across the logo
    val flareAlpha = when {
        t in 2000f..2400f -> {
            val p = (t - 2000f) / 400f
            sin(p * PI.toFloat()).coerceIn(0f, 1f)
        }
        else -> 0f
    }

    // Pre-allocated Color Filters for zero-allocation chromatic channel separation
    val redFilter = remember {
        ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )))
    }
    val cyanFilter = remember {
        ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
            0f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0E))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = skipIntro
            )
            .graphicsLayer {
                alpha = masterAlpha
            },
        contentAlignment = Alignment.Center
    ) {
        // ── Background Phosphor Bloom Halo (Phase 3) ──
        if (bloomIntensity > 0.01f) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = bloomIntensity }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.width * 0.55f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.38f),
                            Color(0xFF00B0FF).copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = maxRadius
                    ),
                    center = center,
                    radius = maxRadius
                )
            }
        }

        // ── The Logo Cluster with Chromatic Split & Micro Shake ──
        if (logoBaseAlpha > 0.01f && logoScaleY > 0.005f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .wrapContentHeight()
                    .graphicsLayer {
                        translationX = shakeX
                        translationY = shakeY
                        scaleY = logoScaleY
                    },
                contentAlignment = Alignment.Center
            ) {
                val logoPainter = painterResource(R.drawable.splash_crt_logo)

                // 1. Red Channel Layer (shifted left/up)
                if (effectiveChromaX > 0.5f) {
                    Image(
                        painter = logoPainter,
                        contentDescription = null,
                        colorFilter = redFilter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .graphicsLayer {
                                translationX = -effectiveChromaX
                                translationY = -effectiveChromaY
                                alpha = (logoBaseAlpha * 0.85f).coerceIn(0f, 1f)
                                blendMode = BlendMode.Screen
                            }
                    )
                }

                // 2. Cyan Channel Layer (shifted right/down)
                if (effectiveChromaX > 0.5f) {
                    Image(
                        painter = logoPainter,
                        contentDescription = null,
                        colorFilter = cyanFilter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .graphicsLayer {
                                translationX = effectiveChromaX
                                translationY = effectiveChromaY
                                alpha = (logoBaseAlpha * 0.85f).coerceIn(0f, 1f)
                                blendMode = BlendMode.Screen
                            }
                    )
                }

                // 3. Crisp Center Hero Logo
                Image(
                    painter = logoPainter,
                    contentDescription = "CRT Screen Off Splash",
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .graphicsLayer {
                            alpha = logoBaseAlpha
                        }
                )

                // 4. Anamorphic Horizontal Flare (Phase 3 Hero Glint)
                if (flareAlpha > 0.01f) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .graphicsLayer { alpha = flareAlpha }
                    ) {
                        val midY = size.height / 2f
                        val flareBrush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF00E5FF).copy(alpha = 0.55f),
                                Color.White.copy(alpha = 0.95f),
                                Color(0xFF00E5FF).copy(alpha = 0.55f),
                                Color.Transparent
                            )
                        )
                        drawLine(
                            brush = flareBrush,
                            start = Offset(0f, midY),
                            end = Offset(size.width, midY),
                            strokeWidth = 3.5.dp.toPx()
                        )
                    }
                }
            }
        }

        // ── Phase 1: Cathode Strike & Phase 4: Symmetrical CRT Power-Off Collapse ──
        if (t < 480f || t >= 2620f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f

                // Degauss phosphor wash (Phase 1 ignition)
                if (degaussFlashAlpha > 0.01f) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = degaussFlashAlpha),
                                Color(0xFF00E5FF).copy(alpha = degaussFlashAlpha * 0.65f),
                                Color(0xFF0C0C0E).copy(alpha = degaussFlashAlpha * 0.25f)
                            ),
                            center = Offset(cx, cy),
                            radius = size.width * 0.8f
                        )
                    )
                }

                // Pinpoint cathode spark dot (Phase 1 ignition)
                if (ignitionDotRadius > 0.1f) {
                    drawCircle(
                        color = Color(0xFF00E5FF).copy(alpha = 0.75f),
                        radius = (ignitionDotRadius + 6f).dp.toPx(),
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = ignitionDotRadius.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                }

                // Laser horizontal beam slit expansion (Phase 1 ignition)
                if (ignitionSlitWidth > 0.01f) {
                    val halfWidth = (size.width / 2f) * ignitionSlitWidth
                    val slitBrush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF00E5FF),
                            Color.White,
                            Color(0xFF00E5FF),
                            Color.Transparent
                        ),
                        startX = cx - halfWidth,
                        endX = cx + halfWidth
                    )
                    // Glow halo line
                    drawLine(
                        brush = slitBrush,
                        start = Offset(cx - halfWidth, cy),
                        end = Offset(cx + halfWidth, cy),
                        strokeWidth = 5.dp.toPx()
                    )
                    // Core white beam
                    drawLine(
                        color = Color.White,
                        start = Offset(cx - halfWidth * 0.85f, cy),
                        end = Offset(cx + halfWidth * 0.85f, cy),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // Laser horizontal beam slit collapse (Phase 4 symmetrical reverse)
                if (collapseSlitWidth > 0.005f) {
                    val halfWidth = (size.width / 2f) * collapseSlitWidth
                    val slitBrush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF00E5FF),
                            Color.White,
                            Color(0xFF00E5FF),
                            Color.Transparent
                        ),
                        startX = cx - halfWidth,
                        endX = cx + halfWidth
                    )
                    // Glow halo line
                    drawLine(
                        brush = slitBrush,
                        start = Offset(cx - halfWidth, cy),
                        end = Offset(cx + halfWidth, cy),
                        strokeWidth = 5.dp.toPx()
                    )
                    // Core white beam
                    drawLine(
                        color = Color.White,
                        start = Offset(cx - halfWidth * 0.85f, cy),
                        end = Offset(cx + halfWidth * 0.85f, cy),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // Pinpoint cathode spark dot extinction (Phase 4 symmetrical reverse)
                if (collapseDotRadius > 0.1f) {
                    drawCircle(
                        color = Color(0xFF00E5FF).copy(alpha = 0.75f * collapseDotAlpha),
                        radius = (collapseDotRadius + 6f).dp.toPx(),
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = collapseDotAlpha),
                        radius = collapseDotRadius.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                }
            }
        }

        // ── Vintage CRT Overlays: Scanlines, Tape Tearing Slices & Vignette ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Horizontal Glitch Slice Displacement Bands (during spikes)
            if (glitchSliceHeight1 > 0f) {
                val sliceTop = h * glitchSliceY1
                val sliceH = h * glitchSliceHeight1
                drawRect(
                    color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                    topLeft = Offset(glitchSliceOffset1, sliceTop),
                    size = Size(w, sliceH)
                )
                // Tape noise tick lines inside the slice
                var lineY = sliceTop
                while (lineY < sliceTop + sliceH) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.25f),
                        start = Offset(glitchSliceOffset1, lineY),
                        end = Offset(w + glitchSliceOffset1, lineY),
                        strokeWidth = 1.dp.toPx()
                    )
                    lineY += 4.dp.toPx()
                }
            }

            if (glitchSliceHeight2 > 0f) {
                val sliceTop = h * glitchSliceY2
                val sliceH = h * glitchSliceHeight2
                drawRect(
                    color = Color(0xFFFF0055).copy(alpha = 0.12f),
                    topLeft = Offset(glitchSliceOffset2, sliceTop),
                    size = Size(w, sliceH)
                )
            }

            // 2. Rolling 50Hz Cathode Hum Bar
            if (t in 400f..2600f) {
                val barCenterY = h * humProgress
                val barRadius = 90.dp.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.045f),
                            Color.Transparent
                        ),
                        startY = barCenterY - barRadius,
                        endY = barCenterY + barRadius
                    ),
                    topLeft = Offset(0f, barCenterY - barRadius),
                    size = Size(w, barRadius * 2f)
                )
            }

            // 3. Fine CRT Scanline Texture Grid (subtle, authentic)
            val scanlineStep = 3.5.dp.toPx()
            var y = 0f
            while (y < h) {
                drawLine(
                    color = Color.Black.copy(alpha = 0.24f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
                y += scanlineStep
            }

            // 4. CRT Curved Glass Shadow Vignette
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.55f),
                        Color.Black.copy(alpha = 0.90f)
                    ),
                    center = Offset(w / 2f, h / 2f),
                    radius = (w / 2f) * 1.35f
                )
            )
        }
    }
}
