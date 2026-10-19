package com.typongtv.crtscreenoff

import android.content.SharedPreferences
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(
    prefs: SharedPreferences,
    onBack: () -> Unit,
    staggeredEntrance: Boolean = false
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val density = LocalDensity.current.density
    val chromaticSplitAnim = remember { Animatable(0f) }

    val dischargeShader = remember {
        try {
            RuntimeShader(MainActivity.DISCHARGE_AGSL)
        } catch (_: Throwable) {
            null
        }
    }

    var enableUiTwitching by remember {
        mutableStateOf(prefs.getBoolean("enable_ui_twitching", true))
    }
    var enableScanlineArcs by remember {
        mutableStateOf(prefs.getBoolean("enable_scanline_arcs", true))
    }
    var enableTouchStatic by remember {
        mutableStateOf(prefs.getBoolean("enable_touch_static", true))
    }

    // Physical CRT impact collision:
    // Fires after 260ms slide hits the left bezel (matching AboutScreen).
    // In tablet mode right bay, fires after all staggered cards settle into place (~1050ms).
    LaunchedEffect(Unit) {
        if (!enableUiTwitching) return@LaunchedEffect
        if (staggeredEntrance) {
            delay(1050L) // Wait for all staggered cards in the right bay to settle
        } else {
            delay(260L) // Wait for the 260ms linear slide to strike the left wall
        }
        // 1. Electron beam mis-convergence spike on impact
        chromaticSplitAnim.animateTo(1.0f, tween(45, easing = LinearEasing))
        // 2. High-energy flicker & twitch
        chromaticSplitAnim.animateTo(0.55f, tween(110, easing = FastOutSlowInEasing))
        // 3. Secondary elastic aftershock
        chromaticSplitAnim.animateTo(0.28f, tween(110, easing = FastOutSlowInEasing))
        // 4. Settles back into razor-sharp convergence
        chromaticSplitAnim.animateTo(0.0f, tween(160, easing = FastOutSlowInEasing))
    }

    val chromaticIntensity = chromaticSplitAnim.value

    val renderEffect = remember(chromaticIntensity) {
        if (chromaticIntensity > 0.001f && dischargeShader != null) {
            try {
                dischargeShader.setFloatUniform("chromaticOffset", chromaticIntensity * 28f * density)
                RenderEffect.createRuntimeShaderEffect(dischargeShader, "image").asComposeRenderEffect()
            } catch (_: Throwable) {
                null
            }
        } else {
            null
        }
    }

    CrtTouchStaticContainer(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
        enabled = enableTouchStatic
    ) {
        RandomScanlineArcOverlay(enabled = enableScanlineArcs, zIndex = 15f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .graphicsLayer {
                    this.renderEffect = renderEffect
                }
        ) {
            // ── Top Navigation Bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(42.dp)
                ) {
                    BackArrowIcon(tint = Color.White)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            HorizontalDivider(color = Color(0xFF262626), thickness = 1.dp)

            // ── Scrollable Content ──
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                // ── Hero Banner Card (Index 1) ──
                StaggeredCard(index = 1, enabled = staggeredEntrance) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                        border = BorderStroke(1.dp, Color(0xFF333333))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Interface & Retro Effects",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Tailor display atmosphere, beam jitter, and touch feedback for maximum comfort.",
                                fontSize = 12.sp,
                                lineHeight = 16.5.sp,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // ── Section Header & Main Toggles Card (Index 2) ──
                StaggeredCard(index = 2, enabled = staggeredEntrance) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "ANALOG NOISE & ARTIFACTS",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 2.dp, bottom = 10.dp)
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                            border = BorderStroke(1.dp, Color(0xFF383838))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Toggle 1: Chromatic Aberration & Beam Twitch
                                SettingsToggleRow(
                                    title = "Chromatic Aberration & Beam Twitch",
                                    activeSubtitle = "Randomized electron beam jitter, deflection coil twitching, and RGB color-split glitching on header title and test button.",
                                    inactiveSubtitle = "Optics stabilized. Header title and trigger button display razor-sharp with all chromatic distortion and jitter silenced.",
                                    checked = enableUiTwitching,
                                    onCheckedChange = { checked ->
                                        enableUiTwitching = checked
                                        prefs.edit().putBoolean("enable_ui_twitching", checked).apply()
                                    }
                                )

                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = Color(0xFF2E2E2E), thickness = 1.dp)
                                Spacer(modifier = Modifier.height(14.dp))

                                // Toggle 2: High-Voltage Scanline Breakdown Arcs
                                SettingsToggleRow(
                                    title = "High-Voltage Scanline Breakdown Arcs",
                                    activeSubtitle = "Atmospheric white core scanlines with cyan phosphor excitation arcs intermittently tearing across the display.",
                                    inactiveSubtitle = "High-voltage breakdown simulation silenced. Clean dark background without rogue scanline arcs.",
                                    checked = enableScanlineArcs,
                                    onCheckedChange = { checked ->
                                        enableScanlineArcs = checked
                                        prefs.edit().putBoolean("enable_scanline_arcs", checked).apply()
                                    }
                                )

                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = Color(0xFF2E2E2E), thickness = 1.dp)
                                Spacer(modifier = Modifier.height(14.dp))

                                // Toggle 3: Cathode Glass Electrostatic Discharge
                                SettingsToggleRow(
                                    title = "Cathode Glass Electrostatic Discharge",
                                    activeSubtitle = "Tactile crackling micro-arcs and dancing electrostatic sparks trace dynamically beneath your finger across the glass.",
                                    inactiveSubtitle = "Electrostatic discharge disabled. Silent, cold glass touch feedback without surface static sparks.",
                                    checked = enableTouchStatic,
                                    onCheckedChange = { checked ->
                                        enableTouchStatic = checked
                                        prefs.edit().putBoolean("enable_touch_static", checked).apply()
                                    }
                                )
                            }
                        }
                    }
                }

                // ── Reset to Defaults Action Button (Index 3) ──
                StaggeredCard(index = 3, enabled = staggeredEntrance) {
                    OutlinedButton(
                        onClick = {
                            enableUiTwitching = true
                            enableScanlineArcs = true
                            enableTouchStatic = true
                            prefs.edit()
                                .putBoolean("enable_ui_twitching", true)
                                .putBoolean("enable_scanline_arcs", true)
                                .putBoolean("enable_touch_static", true)
                                .apply()
                            Toast.makeText(context, "Visual effects restored to default", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF1E1E1E),
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color(0xFF383838))
                    ) {
                        Text(
                            text = "Restore Visual Effects to Default",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StaggeredCard(
    index: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (enabled) {
        CascadingEntrance(index = index, visible = true, modifier = modifier, content = content)
    } else {
        Box(modifier = modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    activeSubtitle: String,
    inactiveSubtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 18.sp,
                color = if (checked) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }

        Text(
            text = if (checked) activeSubtitle else inactiveSubtitle,
            fontSize = 11.5.sp,
            lineHeight = 15.sp,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(top = 4.dp, end = 40.dp)
        )
    }
}
