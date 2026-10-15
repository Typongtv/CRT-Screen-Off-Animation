package com.retro.crteffect

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .statusBarsPadding()
            .navigationBarsPadding()
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
                    tint = MaterialTheme.colorScheme.primary
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
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Authentic Retro Cathode-Ray Tube Physics for Modern Android",
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                    )

                    Text(
                        text = "100% Rootless • AGSL GPU Optics • 120Hz Hardware Acceleration",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFA78BFA),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // ── Section 1: Project Origins & Lineage ──
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
                        text = "Based on lucasa831's Fork of Xposed-ScreenOffAnimation",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        color = Color.White
                    )

                    Text(
                        text = "The original Xposed Screen-Off animation module was created by developer zst123 on GitHub. Developer lucasa831 later forked it and provided valuable updates.\n\n" +
                                "This project was built upon the forked open-source Xposed-ScreenOffAnimation project by lucasa831. While the classic Xposed implementation required device rooting and system-level framework injection, this app revives that iconic electron-beam collapse completely rootless on Android 13 through Android 16+.",
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        color = Color(0xFFCBD5E1),
                        modifier = Modifier.padding(top = 6.dp, bottom = 10.dp)
                    )

                    // Compact Clickable GitHub Repository Card (lucasa831)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://github.com/lucasa831/Xposed-ScreenOffAnimation")
                                    )
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF142436)),
                        border = BorderStroke(1.dp, Color(0xFF64B5F6).copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                        ) {
                            Text(
                                text = "lucasa831 / Xposed-ScreenOffAnimation",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                lineHeight = 17.sp
                            )
                            Text(
                                text = "https://github.com/lucasa831/Xposed-ScreenOffAnimation",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline,
                                lineHeight = 15.sp,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                            Text(
                                text = "Tap to open lucasa831's forked repository on GitHub",
                                fontSize = 12.5.sp,
                                color = Color(0xFF94A3B8),
                                lineHeight = 15.sp,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Compact Upstream Original Creator Reference (zst123)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://github.com/zst123/Xposed-ScreenOffAnimation")
                                    )
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF181C24)),
                        border = BorderStroke(1.dp, Color(0xFF383838))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                        ) {
                            Text(
                                text = "zst123 / Xposed-ScreenOffAnimation",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFCBD5E1),
                                lineHeight = 17.sp
                            )
                            Text(
                                text = "https://github.com/zst123/Xposed-ScreenOffAnimation",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline,
                                lineHeight = 15.sp,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                            Text(
                                text = "Tap to open zst123's original upstream project on GitHub",
                                fontSize = 12.5.sp,
                                color = Color(0xFF94A3B8),
                                lineHeight = 15.sp,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }
                    }
                }
            }

            // ── Section 2: Project Objective ──
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
                        text = "Modern versions of Android (Android 13 through Android 16+) possess stringent security sandbox protections that previously rendered framework-hooked screen-off animations obsolete. The objective of this project is twofold:\n\n" +
                                "1. Complete Rootless Execution: Seamlessly capture the active screen and trigger the display sleep action using official Android Accessibility APIs, ensuring 100% Knox/Play Integrity safety.\n\n" +
                                "2. Dual-Engine Fidelity: Give users the exact mathematical 1:1 original Xposed animation, while also offering an ultra-sophisticated, physics-based Enhanced Phosphor Overdrive engine engineered with modern GPU shaders.",
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        color = Color(0xFFCBD5E1),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            // ── Section 3: Dual Animation Engines ──
            AboutSectionHeader(title = "Dual Animation Engines")
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
                            text = "Authentic 1:1 Xposed Engine",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                    Text(
                        text = "• Exact 1:1 replication of lucasa831's CRT.java and crt_tv.xml algorithms.\n" +
                                "• Strict 0.66 phase split ratio (initial horizontal collapse to line, followed by rapid squeeze to dot).\n" +
                                "• Authentic 35% restrained white flash burst during electron beam deceleration.\n" +
                                "• Quadratic acceleration curve faithfully matching the vintage Gingerbread / JellyBean era.",
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
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("2", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
                        text = "• AGSL GPU Runtime Shaders (Android 13+) rendering true physical CRT optics in real-time.\n" +
                                "• Simulated electron deflection yoke with realistic electromagnetic inductance and resistance.\n" +
                                "• Spherical cathode tube curvature, RGB prism chromatic split, and dynamic scanline combing.\n" +
                                "• Lingering cathode phosphor persistence that stays visible after the screen turns pitch-black.",
                        fontSize = 13.5.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(start = 34.dp, top = 4.dp)
                    )
                }
            }

            // ── Section 4: All Current Features ──
            AboutSectionHeader(title = "Comprehensive Feature Suite")

            // Feature 1
            FeatureDetailCard(
                category = "PHYSICS & OPTICS",
                title = "3-Level Magnetic Coil Resistance",
                description = "Simulates the physical inductive drag of copper deflection coils:\n" +
                        "• Classic (2.0x): Smooth quadratic collapse for the familiar Xposed feel.\n" +
                        "• Heavy Inductance (4.0x): Viscous magnetic crawl, heavy initial resistance, and explosive release.\n" +
                        "• Extreme Tension (6.0x): Sustained magnetic struggle, deep slow creep, and sudden guillotine plunge."
            )

            // Feature 2
            FeatureDetailCard(
                category = "OPTICS & SHADERS",
                title = "AGSL GPU Chromatic Aberration & Optics",
                description = "Hardware-accelerated shader pipeline running on the device GPU:\n" +
                        "• Exaggerated Red-Up / Cyan-Down vertical beam splitting as deflection coils collapse.\n" +
                        "• Dynamic vertical squish motion blur.\n" +
                        "• H-Sync horizontal scanline combing jitter and phosphor streak smear."
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
                description = "Toggle between Classic Horizontal (vertical collapse to horizontal slit -> horizontal snap) and Classic Vertical (curtain style) with fine duration tuning between 150ms and 800ms."
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
                        "• Home screen launcher shortcuts and third-party automation tools."
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

            Spacer(modifier = Modifier.height(8.dp))

            // ── Footer / Acknowledgments ──
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

            // ── Single-line v1.0 Rootless badge at very bottom with comfortable top padding ──
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
                        text = "CRT Screen-Off Engine • v1.0 Rootless",
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
