package com.retro.crteffect

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random

data class ArcScanline(
    val yRatio: Float,
    val startXRatio: Float,
    val endXRatio: Float,
    val strokeWidthDp: Float
)

/**
 * High-voltage simulated capacitive breakdown scanlines that intermittently
 * arc across the screen with authentic CRT phosphor excitation aura and white core.
 */
@Composable
fun RandomScanlineArcOverlay(
    modifier: Modifier = Modifier,
    zIndex: Float = 18f
) {
    var activeArcLines by remember { mutableStateOf<List<ArcScanline>>(emptyList()) }
    val arcAlphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        while (isActive) {
            // Random dwell between 2.2s and 3.8s between discharge events
            delay(Random.nextLong(2200L, 3800L))

            val eventType = Random.nextInt(10)
            when {
                // 60% chance: Single crisp scanline arc
                eventType < 6 -> {
                    val yRatio = Random.nextFloat() * 0.88f + 0.06f
                    val isFullWidth = Random.nextFloat() < 0.35f
                    val startX = if (isFullWidth) 0f else (Random.nextFloat() * 0.25f)
                    val endX = if (isFullWidth) 1f else (startX + Random.nextFloat() * 0.55f + 0.35f).coerceAtMost(1f)
                    val thickness = Random.nextFloat() * 0.8f + 1.2f

                    activeArcLines = listOf(
                        ArcScanline(
                            yRatio = yRatio,
                            startXRatio = startX,
                            endXRatio = endX,
                            strokeWidthDp = thickness
                        )
                    )
                    arcAlphaAnim.snapTo(Random.nextFloat() * 0.25f + 0.70f)
                    arcAlphaAnim.animateTo(0f, tween(Random.nextInt(65, 115), easing = FastOutSlowInEasing))
                    activeArcLines = emptyList()
                }
                // 30% chance: Double-arc breakdown (first line strikes, immediate second line echo)
                eventType < 9 -> {
                    // Strike 1
                    val y1 = Random.nextFloat() * 0.88f + 0.06f
                    val startX1 = if (Random.nextBoolean()) 0f else Random.nextFloat() * 0.20f
                    val endX1 = if (Random.nextBoolean()) 1f else (startX1 + 0.40f + Random.nextFloat() * 0.45f).coerceAtMost(1f)

                    activeArcLines = listOf(
                        ArcScanline(
                            yRatio = y1,
                            startXRatio = startX1,
                            endXRatio = endX1,
                            strokeWidthDp = 1.4f
                        )
                    )
                    arcAlphaAnim.snapTo(0.85f)
                    arcAlphaAnim.animateTo(0f, tween(75, easing = LinearEasing))

                    // Micro pause between sparks (30ms - 65ms)
                    delay(Random.nextLong(30L, 65L))

                    // Strike 2 (either nearby harmonic or anywhere on screen)
                    val y2 = if (Random.nextBoolean()) {
                        (y1 + (Random.nextFloat() * 0.06f - 0.03f)).coerceIn(0.06f, 0.94f)
                    } else {
                        Random.nextFloat() * 0.88f + 0.06f
                    }
                    val startX2 = if (Random.nextBoolean()) 0f else Random.nextFloat() * 0.30f
                    val endX2 = if (Random.nextBoolean()) 1f else (startX2 + 0.45f + Random.nextFloat() * 0.40f).coerceAtMost(1f)

                    activeArcLines = listOf(
                        ArcScanline(
                            yRatio = y2,
                            startXRatio = startX2,
                            endXRatio = endX2,
                            strokeWidthDp = 1.8f
                        )
                    )
                    arcAlphaAnim.snapTo(0.95f)
                    arcAlphaAnim.animateTo(0f, tween(110, easing = FastOutSlowInEasing))
                    activeArcLines = emptyList()
                }
                // 10% chance: Twin split lines (two parallel horizontal arcs simultaneously)
                else -> {
                    val baseCenterY = Random.nextFloat() * 0.80f + 0.10f
                    val lineA = ArcScanline(
                        yRatio = baseCenterY,
                        startXRatio = 0f,
                        endXRatio = 1f,
                        strokeWidthDp = 1.3f
                    )
                    val lineB = ArcScanline(
                        yRatio = (baseCenterY + Random.nextFloat() * 0.025f + 0.008f).coerceAtMost(0.95f),
                        startXRatio = Random.nextFloat() * 0.15f,
                        endXRatio = (0.75f + Random.nextFloat() * 0.25f).coerceAtMost(1f),
                        strokeWidthDp = 1.1f
                    )
                    activeArcLines = listOf(lineA, lineB)
                    arcAlphaAnim.snapTo(0.75f)
                    arcAlphaAnim.animateTo(0f, tween(90, easing = FastOutSlowInEasing))
                    activeArcLines = emptyList()
                }
            }
        }
    }

    if (activeArcLines.isNotEmpty() && arcAlphaAnim.value > 0.005f) {
        val currentAlpha = arcAlphaAnim.value
        Canvas(
            modifier = modifier
                .fillMaxSize()
                .zIndex(zIndex)
        ) {
            for (line in activeArcLines) {
                val y = size.height * line.yRatio
                val xStart = size.width * line.startXRatio
                val xEnd = size.width * line.endXRatio

                // 1. Ultra-faint full-width horizontal retrace guide (gives the authentic scanline beam feel)
                drawLine(
                    color = Color.White.copy(alpha = currentAlpha * 0.15f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 0.75.dp.toPx()
                )

                // 2. Cyan phosphor excitation aura around the hot arc
                drawLine(
                    color = Color(0xFF00E5FF).copy(alpha = currentAlpha * 0.38f),
                    start = Offset(xStart, y),
                    end = Offset(xEnd, y),
                    strokeWidth = (line.strokeWidthDp + 2.4f).dp.toPx()
                )

                // 3. Hot blazing white core arc
                drawLine(
                    color = Color.White.copy(alpha = currentAlpha * 0.85f),
                    start = Offset(xStart, y),
                    end = Offset(xEnd, y),
                    strokeWidth = line.strokeWidthDp.dp.toPx()
                )
            }
        }
    }
}
