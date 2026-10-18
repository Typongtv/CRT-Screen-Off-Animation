package com.typongtv.crtscreenoff

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SpecialAcknowledgmentScreen(
    onBack: () -> Unit,
    staggeredEntrance: Boolean = false
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current.density
    val chromaticSplitAnim = remember { Animatable(0f) }

    val dischargeShader = remember {
        try {
            RuntimeShader(MainActivity.DISCHARGE_AGSL)
        } catch (e: Throwable) {
            null
        }
    }

    // Physical CRT impact collision / card settle:
    // When opening on phone, fires after 260ms slide hits the left bezel.
    // In tablet mode right bay, fires after all staggered cards settle into place (~1150ms).
    LaunchedEffect(Unit) {
        if (staggeredEntrance) {
            delay(1150L) // Wait for all staggered cards in the right bay to settle
        } else {
            delay(260L) // Wait for the 260ms linear slide to strike the left wall
        }
        // 1. Violent electron beam mis-convergence spike on impact
        chromaticSplitAnim.animateTo(1.0f, tween(45, easing = LinearEasing))
        // 2. High-energy flicker & twitch
        chromaticSplitAnim.animateTo(0.55f, tween(110, easing = FastOutSlowInEasing))
        // 3. Secondary elastic aftershock
        chromaticSplitAnim.animateTo(0.28f, tween(110, easing = FastOutSlowInEasing))
        // 4. Settles back into razor-sharp convergence
        chromaticSplitAnim.animateTo(0.0f, tween(160, easing = FastOutSlowInEasing))
    }

    val chromaticIntensity = chromaticSplitAnim.value

    // Intense chromatic split along the horizontal axis (zero layout displacement)
    val renderEffect = remember(chromaticIntensity) {
        if (chromaticIntensity > 0.001f && dischargeShader != null) {
            try {
                // Peak offset at 28dp * density: text glyphs and cards intensely split into Red and Cyan ghosts
                dischargeShader.setFloatUniform("chromaticOffset", chromaticIntensity * 28f * density)
                RenderEffect.createRuntimeShaderEffect(dischargeShader, "image").asComposeRenderEffect()
            } catch (e: Throwable) {
                null
            }
        } else {
            null
        }
    }

    CrtTouchStaticContainer(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        // High-voltage flickering scanline breakdown arcs
        RandomScanlineArcOverlay(zIndex = 15f)

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
                    BackArrowIcon(
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Special Acknowledgment",
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
                // ── Group 1: Header & Hero Tribute Card ──
                StaggeredCard(index = 1, enabled = staggeredEntrance) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // ── Big Bold Header outside Tribute Card (No background / No card) ──
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 2.dp, bottom = 12.dp, top = 2.dp)
                        ) {
                            SparkleIcon(
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(9.dp))
                            Text(
                                text = "CO-ENGINEERED WITH GOOGLE ANTIGRAVITY",
                                fontSize = 19.5.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 24.sp,
                                letterSpacing = 0.4.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // ── Hero Tribute Card (#292929) ──
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF292929)),
                            border = BorderStroke(1.dp, Color(0xFF3E3E3E))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp)
                            ) {
                                Text(
                                    text = "A Tribute to Retro Television History",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 17.sp,
                                    lineHeight = 21.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Text(
                                    text = "Proudly co-developed with Google Antigravity — translating analog electron-beam physics into hardware-accelerated AGSL runtime shaders at 120Hz.\n\n" +
                                            "From mathematical deflection tuning to architecting a Knox-safe, zero-allocation pipeline on Android 16, this project stands as a tribute to retro television history co-crafted with next-generation AI.",
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }
                }

                // ── Group 2: Milestones & Pillars 1-2 ──
                StaggeredCard(index = 2, enabled = staggeredEntrance) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Co-Creation Milestones",
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 2.dp, bottom = 8.dp, top = 4.dp)
                        )

                        // Pillar 1: AGSL Shaders
                        AcknowledgmentPillarCard(
                            title = "120Hz AGSL GPU Shader Engine",
                            description = "Engineered real-time AGSL GPU runtime shaders to computationally model cathode tube curvature, dual-harmonic magnetic deflection resistance, dynamic scanline jitter, and rich phosphor afterglow."
                        )

                        // Pillar 2: Knox-Safe Rootless
                        AcknowledgmentPillarCard(
                            title = "100% Knox-Safe & Rootless Execution",
                            description = "Overcame modern Android 13–16+ security sandbox constraints by pairing official Android Accessibility APIs with a headless gesture trigger activity, ensuring zero root requirements and 100% Play Integrity preservation."
                        )
                    }
                }

                // ── Group 3: Pillars 3-4 ──
                StaggeredCard(index = 3, enabled = staggeredEntrance) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Pillar 3: Zero-Allocation Pipeline
                        AcknowledgmentPillarCard(
                            title = "Zero-Allocation Hardware Pipeline",
                            description = "Pre-allocated Paint buffers, native Shaders, and Matrices across the rendering loop to guarantee fluid 60Hz, 90Hz, and 120Hz refresh rates with zero garbage-collection stutter."
                        )

                        // Pillar 4: Human-AI Collaboration
                        AcknowledgmentPillarCard(
                            title = "Human Vision & AI Pair Programming",
                            description = "From initial nostalgic concept to low-level byte buffers, matrix mathematics, and refined Jetpack Compose UI architecture, Google Antigravity served as the dedicated AI pair programmer bringing this vision into production reality."
                        )
                    }
                }

                // ── Group 4: Bottom Credit Card ──
                StaggeredCard(index = 4, enabled = staggeredEntrance) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(6.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
                            border = BorderStroke(1.dp, Color(0xFF2A2A2A))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "CRT Screen-Off Engine",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Crafted with human vision and next-generation intelligence.",
                                    fontSize = 12.5.sp,
                                    lineHeight = 15.sp,
                                    color = Color(0xFF6B7280),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))
                    }
                }
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
private fun AcknowledgmentPillarCard(
    title: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        border = BorderStroke(1.dp, Color(0xFF333333))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = description,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}
