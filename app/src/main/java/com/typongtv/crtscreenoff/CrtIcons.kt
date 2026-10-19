package com.typongtv.crtscreenoff

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun MoreVertIcon(
    tint: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val radius = 2.dp.toPx()
        val centerX = size.width / 2f
        drawCircle(color = tint, radius = radius, center = Offset(centerX, size.height * 0.25f))
        drawCircle(color = tint, radius = radius, center = Offset(centerX, size.height * 0.50f))
        drawCircle(color = tint, radius = radius, center = Offset(centerX, size.height * 0.75f))
    }
}

@Composable
fun BackArrowIcon(
    tint: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val strokeWidth = 2.4.dp.toPx()
        val path = Path().apply {
            moveTo(size.width * 0.60f, size.height * 0.25f)
            lineTo(size.width * 0.35f, size.height * 0.5f)
            lineTo(size.width * 0.60f, size.height * 0.75f)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        drawLine(
            color = tint,
            start = Offset(size.width * 0.35f, size.height * 0.5f),
            end = Offset(size.width * 0.72f, size.height * 0.5f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun InfoIcon(
    tint: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val strokeWidth = 1.8.dp.toPx()
        drawCircle(
            color = tint,
            radius = (size.minDimension / 2f) - (strokeWidth / 2f),
            center = Offset(size.width / 2f, size.height / 2f),
            style = Stroke(width = strokeWidth)
        )
        // Dot
        drawCircle(
            color = tint,
            radius = strokeWidth * 0.75f,
            center = Offset(size.width / 2f, size.height * 0.32f)
        )
        // Stem
        drawLine(
            color = tint,
            start = Offset(size.width / 2f, size.height * 0.46f),
            end = Offset(size.width / 2f, size.height * 0.72f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Normal Material dynamic chevron icon that smoothly rotates between 0° (down) and 180° (up).
 */
@Composable
fun ChevronIcon(
    isExpanded: Boolean,
    tint: Color = Color(0xFF94A3B8),
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "ChevronRotation"
    )
    Canvas(
        modifier = modifier
            .size(18.dp)
            .graphicsLayer { rotationZ = rotation }
    ) {
        val strokeWidth = 1.9.dp.toPx()
        val path = Path().apply {
            moveTo(size.width * 0.26f, size.height * 0.40f)
            lineTo(size.width * 0.50f, size.height * 0.64f)
            lineTo(size.width * 0.74f, size.height * 0.40f)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

/**
 * Material-style Advisory / Warning triangle icon with rounded stroke and exclamation mark.
 */
@Composable
fun WarningIcon(
    tint: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val strokeWidth = 1.8.dp.toPx()
        val path = Path().apply {
            moveTo(size.width * 0.50f, size.height * 0.16f)
            lineTo(size.width * 0.88f, size.height * 0.82f)
            lineTo(size.width * 0.12f, size.height * 0.82f)
            close()
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        // Exclamation stem
        drawLine(
            color = tint,
            start = Offset(size.width * 0.50f, size.height * 0.42f),
            end = Offset(size.width * 0.50f, size.height * 0.62f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        // Exclamation dot
        drawCircle(
            color = tint,
            radius = strokeWidth * 0.70f,
            center = Offset(size.width * 0.50f, size.height * 0.73f)
        )
    }
}

@Composable
fun AdvisoryIcon(
    tint: Color = Color.White,
    modifier: Modifier = Modifier
) = WarningIcon(tint = tint, modifier = modifier)

/**
 * Material-style 4-point AI sparkle/star icon for Special Acknowledgment.
 */
@Composable
fun SparkleIcon(
    tint: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val strokeWidth = 1.8.dp.toPx()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = (size.minDimension / 2f) * 0.82f

        val path = Path().apply {
            moveTo(cx, cy - r)
            quadraticTo(cx, cy, cx + r, cy)
            quadraticTo(cx, cy, cx, cy + r)
            quadraticTo(cx, cy, cx - r, cy)
            quadraticTo(cx, cy, cx, cy - r)
            close()
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

/**
 * Material Symbols Outlined Logout icon.
 */
@Composable
fun LogoutIcon(
    tint: Color = Color.White,
    rotation: Float = 0f,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .size(18.dp)
            .graphicsLayer { rotationZ = rotation }
    ) {
        val strokeWidth = 1.9.dp.toPx()
        // Bracket doorway on left
        val doorPath = Path().apply {
            moveTo(size.width * 0.48f, size.height * 0.16f)
            lineTo(size.width * 0.20f, size.height * 0.16f)
            lineTo(size.width * 0.20f, size.height * 0.84f)
            lineTo(size.width * 0.48f, size.height * 0.84f)
        }
        drawPath(
            path = doorPath,
            color = tint,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        // Horizontal arrow shaft
        val cy = size.height * 0.50f
        drawLine(
            color = tint,
            start = Offset(size.width * 0.38f, cy),
            end = Offset(size.width * 0.84f, cy),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        // Arrow head pointing right
        val headPath = Path().apply {
            moveTo(size.width * 0.64f, size.height * 0.30f)
            lineTo(size.width * 0.84f, cy)
            lineTo(size.width * 0.64f, size.height * 0.70f)
        }
        drawPath(
            path = headPath,
            color = tint,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

/**
 * Mechanical gear Settings icon with center hub and 6 radial teeth.
 */
@Composable
fun SettingsIcon(
    tint: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val strokeWidth = 1.8.dp.toPx()
        val rOuter = size.minDimension * 0.44f
        val rInner = size.minDimension * 0.32f
        val rHole = size.minDimension * 0.16f

        // Central hub circle
        drawCircle(
            color = tint,
            radius = rHole,
            center = Offset(cx, cy),
            style = Stroke(width = strokeWidth)
        )

        // 6 radial gear teeth
        val numTeeth = 6
        for (i in 0 until numTeeth) {
            val angle = Math.toRadians(i * (360.0 / numTeeth))
            val cos = kotlin.math.cos(angle).toFloat()
            val sin = kotlin.math.sin(angle).toFloat()
            drawLine(
                color = tint,
                start = Offset(cx + cos * rInner, cy + sin * rInner),
                end = Offset(cx + cos * rOuter, cy + sin * rOuter),
                strokeWidth = strokeWidth * 1.5f,
                cap = StrokeCap.Round
            )
        }

        // Rim circle connecting the teeth roots
        drawCircle(
            color = tint,
            radius = rInner,
            center = Offset(cx, cy),
            style = Stroke(width = strokeWidth)
        )
    }
}
