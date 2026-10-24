package com.typongtv.crtscreenoff

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AboutScreen(
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

    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("crt_prefs", android.content.Context.MODE_PRIVATE) }
    val enableUiTwitching = remember { prefs.getBoolean("enable_ui_twitching", true) }
    val enableScanlineArcs = remember { prefs.getBoolean("enable_scanline_arcs", true) }
    val enableTouchStatic = remember { prefs.getBoolean("enable_touch_static", true) }

    // Physical CRT impact collision / card settle:
    // When opening on phone, fires after 260ms slide hits the left bezel.
    // In tablet mode right bay, fires after all staggered cards settle into place (~1150ms).
    LaunchedEffect(Unit) {
        if (!enableUiTwitching) return@LaunchedEffect
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
                // Peak offset at 28dp * density: text glyphs and lines intensely split into Red and Cyan ghosts
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
            .background(Color(0xFF121212)),
        enabled = enableTouchStatic
    ) {
        // High-voltage flickering scanline breakdown arcs
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
        // ── Top Navigation Bar (Badge removed as requested) ──
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
                text = "About CRT Screen-Off",
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
            // ── Hero Banner ──
            StaggeredCard(index = 1, enabled = staggeredEntrance) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                    border = BorderStroke(1.dp, Color(0xFF333333))
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF222222))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.splash_crt_logo),
                            contentDescription = "CRT Logo",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "CRT Screen-Off Engine",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Early Android Inspired Retro CRT Physics for Modern Devices",
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                    )

                    Text(
                        text = "Rootless • AGSL GPU Optics • 120Hz Hardware Acceleration",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFA78BFA),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

            // ── Section 1: Project Origins & Lineage ──
            StaggeredCard(index = 2, enabled = staggeredEntrance) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AboutSectionHeader(title = "Origins & Heritage")
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                        border = BorderStroke(1.dp, Color(0xFF383838))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Inspired by Early Android's ElectronBeam Legacy",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                lineHeight = 20.sp,
                                color = Color.White
                            )

                            Text(
                                text = "Back in the early days of Android, one visual touch captured everyone's imagination: the iconic CRT screen-off animation, handled natively inside the system display compositor.\n\n" +
                                        "As the display collapsed inward, the screen flared into a searing horizontal razor line before vanishing into pitch darkness. It was a beloved aesthetic that users held on to for so long, refusing to let go even as subsequent Android updates retired the feature from stock software. Bringing it back historically required device rooting or invasive system-level modifications that tripped hardware security fuses and broke safety integrity checks.\n\n" +
                                        "This project was born out of that deep nostalgic passion: reviving the legendary CRT screen-off experience completely rootless on Android 13 through Android 16+, blending official Android Accessibility APIs with high-performance hardware acceleration.",
                                fontSize = 14.sp,
                                lineHeight = 19.5.sp,
                                color = Color(0xFFCBD5E1),
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }

            // ── Section 2: Project Objective ──
            StaggeredCard(index = 3, enabled = staggeredEntrance) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AboutSectionHeader(title = "Project Objective")
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                        border = BorderStroke(1.dp, Color(0xFF383838))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Bridging Vintage CRT Charm with Modern Android Security",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                lineHeight = 20.sp,
                                color = Color.White
                            )

                            Text(
                                text = "Modern versions of Android (Android 13 through Android 16+) possess stringent security sandbox protections that previously rendered system-hooked screen-off animations obsolete. The objective of this project is twofold:\n\n" +
                                        "1. Complete Rootless Execution: Seamlessly capture the active screen and trigger the display sleep action using official Android Accessibility APIs, ensuring 100% Knox and Play Integrity safety with zero root permissions.\n\n" +
                                        "2. Triple-Engine Fidelity: Deliver three distinct physical interpretations of the classic collapse—starting with an authentic Classic Cathode mode crafted to faithfully mimic the original aesthetic, followed by Studio Deflection and Enhanced Phosphor Overdrive modes that upgrade and elevate the visual experience with advanced deflection dynamics and AGSL GPU optics.",
                                fontSize = 14.sp,
                                lineHeight = 19.5.sp,
                                color = Color(0xFFCBD5E1),
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }

            // ── Section 3: Triple Animation Engines ──
            StaggeredCard(index = 4, enabled = staggeredEntrance) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AboutSectionHeader(title = "Triple Animation Engines")
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                        border = BorderStroke(1.dp, Color(0xFF383838))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Engine 1
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF383838),
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("1", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Classic Cathode Engine",
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                            Text(
                                text = "• Faithfully mimics the nostalgic aesthetic of the early Android screen-off animation that fans held on to for years.\n" +
                                        "• Recreates the authentic 2D collapse with a bright phosphor flash and swift central snap.\n" +
                                        "• Strict raster confinement ensures letterbox borders remain pitch black throughout the animation.\n" +
                                        "• Fast, pure hardware canvas rendering with zero shader overhead, staying true to the original feel.",
                                fontSize = 13.5.sp,
                                lineHeight = 18.sp,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(start = 34.dp, top = 4.dp, bottom = 12.dp)
                            )

                            HorizontalDivider(color = Color(0xFF2E2E2E), thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            // Engine 2
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF383838),
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("2", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Studio Deflection Engine",
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                            Text(
                                text = "• Upgrades and enhances the visual appearance with refined magnetic deflection dynamics and natural pull curves.\n" +
                                        "• Features true-color directional smear along the collapse axis with subtle optical chromatic fringe.\n" +
                                        "• Dynamic crescendo luminance flare as the collapsing picture flattens into the central beam.\n" +
                                        "• Incandescent filament core with electric cyan halo zipping shut cleanly into pitch-black standby.",
                                fontSize = 13.5.sp,
                                lineHeight = 18.sp,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(start = 34.dp, top = 4.dp, bottom = 12.dp)
                            )

                            HorizontalDivider(color = Color(0xFF2E2E2E), thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            // Engine 3
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("3", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Enhanced Phosphor Overdrive Engine",
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "• Pushes the retro CRT aesthetic into modern GPU physics using real-time AGSL runtime shaders (Android 13+).\n" +
                                        "• Realistic electromagnetic deflection with customizable coil resistance and elastic border tension.\n" +
                                        "• Unconfined Chromatic Breakout: Crimson and Cyan electron gun fringes break free of the collapsing bitmap frame into the dark glass, surging outward during high-speed vertical collapse and locking into a glowing multi-spectral crowning halo around the horizontal beam.\n" +
                                        "• Spherical CRT tube curvature, anamorphic chromatic flare, and dynamic scanline combing.\n" +
                                        "• Lingering cathode phosphor afterglow that stays visible on the glass after complete shutdown.",
                                fontSize = 13.5.sp,
                                lineHeight = 18.sp,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(start = 34.dp, top = 4.dp)
                            )
                        }
                    }
                }
            }

            // ── Section 4: All Current Features ──
            StaggeredCard(index = 5, enabled = staggeredEntrance) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AboutSectionHeader(title = "Comprehensive Feature Suite")

                    // Feature 1
                    FeatureDetailCard(
                        category = "PHYSICS & OPTICS",
                        title = "3-Level Magnetic Coil Resistance",
                        description = "Simulates the physical inductive drag of copper deflection coils:\n" +
                                "• Classic (2.0x): Smooth quadratic collapse for the familiar retro Gingerbread feel.\n" +
                                "• Heavy Inductance (4.0x): Viscous magnetic crawl, heavy initial resistance, and explosive release.\n" +
                                "• Extreme Tension (6.0x): Sustained magnetic struggle, deep slow creep, and sudden guillotine plunge."
                    )

                    // Feature 2
                    FeatureDetailCard(
                        category = "OPTICS & SHADERS",
                        title = "AGSL GPU Chromatic Breakout & Kinetic Optics",
                        description = "Hardware-accelerated shader pipeline running on the device GPU:\n" +
                                "• Unconfined Asymmetric Breakout: Crimson gun overshoot above and Electric-Cyan undershoot below break free of the collapsing raster borders with soft exponential dispersion.\n" +
                                "• Velocity-Coupled Dynamic Surge: Chromatic fringes surge with exaggerated outward offset during high-speed vertical collapse, then gracefully lock into a glowing multi-spectral crowning halo around the horizontal beam.\n" +
                                "• Directional Squish Motion Blur, H-Sync scanline combing, and phosphor smear persistence."
                    )

                    // Feature 3
                    FeatureDetailCard(
                        category = "GEOMETRY",
                        title = "Magnetic Linear Stretch (Elastic Tension)",
                        description = "Screen borders cling tenaciously to the device bezels while the center contents pull inward into the equator before elastically snapping free, reproducing true analog vacuum tube behavior."
                    )

                    // Feature 4
                    FeatureDetailCard(
                        category = "LIGHT & PLASMA",
                        title = "Anamorphic Chromatic Flare (Prism Split)",
                        description = "Blinding 95% incandescent white beam flanked by cyan prism halos, featuring dynamic center swell and a glowing cathode bridge as the electron gun powers down."
                    )

                    // Feature 5
                    FeatureDetailCard(
                        category = "LUMINESCENCE",
                        title = "Flare & Bloom Overdrive Control",
                        description = "Fully customizable flash intensity slider ranging from subtle (0%) to dramatic standard (100%) up to blinding Supernova (150%) peak electron burst."
                    )

                    // Feature 6
                    FeatureDetailCard(
                        category = "AFTERGLOW",
                        title = "Independent Cathode Dot Afterglow",
                        description = "True phosphor decay persistence lasting from 100ms to 1500ms. The glowing center dot lingers isolated on the pitch-black OLED glass display after the raster collapses."
                    )

                    // Feature 7
                    FeatureDetailCard(
                        category = "ORIENTATION & SPEED",
                        title = "Direction & Timing Customization",
                        description = "Toggle between Classic Horizontal (vertical collapse to horizontal slit -> horizontal snap) and Classic Vertical (curtain style) with fine duration tuning between 400ms and 1200ms."
                    )

                    // Feature 8
                    FeatureDetailCard(
                        category = "AUTOMATION & INTEGRATION",
                        title = "Universal Gesture Integration (LockTriggerActivity)",
                        description = "Dedicated zero-UI trigger activity enabling seamless invocation from:\n" +
                                "• Samsung Good Lock & One Hand Operation +\n" +
                                "• Google Pixel Quick Tap (back tap)\n" +
                                "• Motorola Double Tap Back gestures\n" +
                                "• Xiaomi / HyperOS gesture shortcuts\n" +
                                "• Edge Gestures & Fluid Navigation Gestures (FNG)\n" +
                                "• Good Lock RegiStar, Key Remappers, and automation tools (Tasker, MacroDroid)."
                    )

                    // Feature 9
                    FeatureDetailCard(
                        category = "PERFORMANCE",
                        title = "Zero-Allocation Hardware Pipeline",
                        description = "Pre-allocated Paint objects, Shaders, and Matrices with zero memory allocations during draw frames, ensuring fluid 60Hz, 90Hz, and 120Hz refresh rates with zero frame drops."
                    )

                    // Feature 10
                    FeatureDetailCard(
                        category = "CONVENIENCE",
                        title = "Per-Section Restore to Default",
                        description = "Instant single-tap restoration buttons on each configuration card to easily reset back to factory values at any moment."
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Section 5: Footer / Acknowledgments ──
            StaggeredCard(index = 6, enabled = staggeredEntrance) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
                        border = BorderStroke(1.dp, Color(0xFF2A2A2A))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Crafted with Jetpack Compose & AGSL Shaders",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 16.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = "Dedicated to retro CRT television lovers & Android enthusiasts.",
                                fontSize = 12.sp,
                                lineHeight = 14.sp,
                                color = Color(0xFF6B7280),
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }
                    }

                    // ── Single-line v1.0.1 Rootless badge at very bottom with comfortable top padding ──
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 28.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "CRT Screen-Off Engine • v1.2.1",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
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
private fun AboutSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 16.5.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 2.dp, bottom = 8.dp, top = 6.dp)
    )
}

@Composable
private fun FeatureDetailCard(
    category: String,
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
                text = category,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.8.sp
            )
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(top = 3.dp, bottom = 5.dp)
            )
            Text(
                text = description,
                fontSize = 13.5.sp,
                lineHeight = 18.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}
