package com.typongtv.crtscreenoff

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random

private data class TouchStaticArc(
    val yOffsetDp: Float,
    val xStartRatio: Float,
    val xEndRatio: Float,
    val thicknessDp: Float,
    val brightness: Float,
    val colorType: Int // 0: Pure incandescent white, 1: White core + cyan halo, 2: Brilliant electric spark
)

/**
 * Interactive electrostatic CRT faceplate container.
 * Wraps content so that touch/drag/scroll events are monitored in PointerEventPass.Initial
 * WITHOUT consuming any events. Children (verticalScroll, sliders, buttons, cards) receive all
 * gestures with 100% fluid responsiveness while generating subtle, rapid crackling white and cyan
 * electrostatic scanline sparks directly under the user's finger.
 *
 * Configured uniformly across all screens (Dashboard, About, Special Acknowledgment) with
 * subtle, whisper-thin micro-arcs and rapid dancing static feedback.
 */
@Composable
fun CrtTouchStaticContainer(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    subtleMode: Boolean = true,
    content: @Composable () -> Unit
) {
    if (!enabled) {
        Box(modifier = modifier.fillMaxSize()) {
            content()
        }
        return
    }

    var touchPosition by remember { mutableStateOf<Offset?>(null) }
    var isTouching by remember { mutableStateOf(false) }
    var currentArcs by remember { mutableStateOf<List<TouchStaticArc>>(emptyList()) }
    val staticAlphaAnim = remember { Animatable(0f) }

    // Dynamic electrostatic arc regeneration loop while finger is on the glass
    LaunchedEffect(isTouching) {
        if (isTouching) {
            staticAlphaAnim.snapTo(1f)
            while (isActive && isTouching) {
                // Subtle, non-distracting 1-2 whisper-thin micro-arcs
                val arcCount = Random.nextInt(1, 3)
                val newArcs = ArrayList<TouchStaticArc>(arcCount)

                for (i in 0 until arcCount) {
                    val yJitterDp = Random.nextFloat() * 28f - 14f // Tight +/- 14dp
                    val isFullWidth = Random.nextFloat() < 0.15f

                    val xStart = if (isFullWidth) 0f else (Random.nextFloat() * 0.28f)
                    val xEnd = if (isFullWidth) 1f else (xStart + Random.nextFloat() * 0.50f + 0.35f).coerceAtMost(1f)
                    val thickness = Random.nextFloat() * 0.35f + 0.75f
                    val brightness = Random.nextFloat() * 0.25f + 0.50f
                    val colorType = Random.nextInt(3)

                    newArcs.add(
                        TouchStaticArc(
                            yOffsetDp = yJitterDp,
                            xStartRatio = xStart,
                            xEndRatio = xEnd,
                            thicknessDp = thickness,
                            brightness = brightness,
                            colorType = colorType
                        )
                    )
                }
                currentArcs = newArcs
                // Rapid electrifying crackle (24ms-45ms) matched across all screens
                val dwell = Random.nextLong(24L, 45L)
                delay(dwell)
            }
        } else {
            // Instant electrostatic snap extinction when finger lifts
            staticAlphaAnim.animateTo(0f, tween(45, easing = FastOutSlowInEasing))
            currentArcs = emptyList()
            touchPosition = null
        }
    }

    val currentAlpha = staticAlphaAnim.value

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Passive preview pass: reads position without consuming touch events
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        val activePointer = event.changes.firstOrNull { it.pressed }

                        if (activePointer != null) {
                            touchPosition = activePointer.position
                            isTouching = true
                        } else {
                            isTouching = false
                        }
                    }
                }
            }
    ) {
        // 1. Content underneath (receives 100% of scrolls, drags, slider movements, and clicks without interference)
        content()

        // 2. High-voltage electrostatic arc canvas (NO pointerInput on this canvas: completely click-through)
        val touch = touchPosition
        if (touch != null && currentAlpha > 0.01f && currentArcs.isNotEmpty()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(25f)
            ) {
                val density = this.density

                // Note: Circular gradient blob has been removed as requested.
                // Purely crisp horizontal electrostatic scanlines with zero UI occlusion.

                // High-voltage localized static scanlines
                for (arc in currentArcs) {
                    val arcY = (touch.y + arc.yOffsetDp * density).coerceIn(0f, size.height)
                    val xStart = size.width * arc.xStartRatio
                    val xEnd = size.width * arc.xEndRatio
                    val arcAlpha = currentAlpha * arc.brightness

                    when (arc.colorType) {
                        // 0: Pure incandescent white beam
                        0 -> {
                            // Retrace guide
                            drawLine(
                                color = Color.White.copy(alpha = arcAlpha * 0.12f),
                                start = Offset(0f, arcY),
                                end = Offset(size.width, arcY),
                                strokeWidth = 0.75.dp.toPx()
                            )
                            // White aura
                            drawLine(
                                color = Color(0xFFE2E8F0).copy(alpha = arcAlpha * 0.28f),
                                start = Offset(xStart, arcY),
                                end = Offset(xEnd, arcY),
                                strokeWidth = (arc.thicknessDp + 1.8f).dp.toPx()
                            )
                            // Blazing white filament core
                            drawLine(
                                color = Color.White.copy(alpha = arcAlpha * 0.85f),
                                start = Offset(xStart, arcY),
                                end = Offset(xEnd, arcY),
                                strokeWidth = arc.thicknessDp.dp.toPx()
                            )
                        }
                        // 1: Hot white core with cyan phosphor halo
                        1 -> {
                            // Retrace guide
                            drawLine(
                                color = Color.White.copy(alpha = arcAlpha * 0.10f),
                                start = Offset(0f, arcY),
                                end = Offset(size.width, arcY),
                                strokeWidth = 0.75.dp.toPx()
                            )
                            // Cyan excitation aura
                            drawLine(
                                color = Color(0xFF00E5FF).copy(alpha = arcAlpha * 0.30f),
                                start = Offset(xStart, arcY),
                                end = Offset(xEnd, arcY),
                                strokeWidth = (arc.thicknessDp + 2.2f).dp.toPx()
                            )
                            // Hot white filament core
                            drawLine(
                                color = Color.White.copy(alpha = arcAlpha * 0.82f),
                                start = Offset(xStart, arcY),
                                end = Offset(xEnd, arcY),
                                strokeWidth = (arc.thicknessDp * 0.85f).dp.toPx()
                            )
                        }
                        // 2: Intense brilliant white spark line with subtle cyan edge
                        else -> {
                            // Retrace guide
                            drawLine(
                                color = Color(0xFF00E5FF).copy(alpha = arcAlpha * 0.12f),
                                start = Offset(0f, arcY),
                                end = Offset(size.width, arcY),
                                strokeWidth = 0.75.dp.toPx()
                            )
                            // Bright white glow
                            drawLine(
                                color = Color.White.copy(alpha = arcAlpha * 0.35f),
                                start = Offset(xStart, arcY),
                                end = Offset(xEnd, arcY),
                                strokeWidth = (arc.thicknessDp + 1.8f).dp.toPx()
                            )
                            // Blinding white filament core
                            drawLine(
                                color = Color(0xFFFFFFFF).copy(alpha = arcAlpha * 0.90f),
                                start = Offset(xStart, arcY),
                                end = Offset(xEnd, arcY),
                                strokeWidth = arc.thicknessDp.dp.toPx()
                            )
                        }
                    }
                }
            }
        }
    }
}
