package com.retro.crteffect

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    private fun isAccessibilityEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        return enabledServices.any { it.resolveInfo.serviceInfo.packageName == packageName }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("crt_prefs", Context.MODE_PRIVATE)

        setContent {
            val scrollState = rememberScrollState()
            var speedMs by remember { mutableFloatStateOf(prefs.getInt("anim_speed", 350).toFloat()) }
            var selectedStyle by remember {
                mutableStateOf(
                    prefs.getString("anim_style", CrtStyle.CLASSIC_HORIZONTAL.name) ?: CrtStyle.CLASSIC_HORIZONTAL.name
                )
            }

            var hasOverlay by remember { mutableStateOf(Settings.canDrawOverlays(this@MainActivity)) }
            var isA11yEnabled by remember { mutableStateOf(isAccessibilityEnabled()) }

            // 3 Collapsible sub-card states (initially compact/collapsed)
            var expandedOptionA by remember { mutableStateOf(false) }
            var expandedOptionB by remember { mutableStateOf(false) }
            var expandedOptionC by remember { mutableStateOf(false) }

            // Re-check permissions when returning to activity
            DisposableEffect(Unit) {
                val callback = {
                    hasOverlay = Settings.canDrawOverlays(this@MainActivity)
                    isA11yEnabled = isAccessibilityEnabled()
                }
                callback()
                onDispose { }
            }

            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF64B5F6),
                    onPrimary = Color.Black,
                    secondaryContainer = Color(0xFF1E1E1E),
                    surface = Color(0xFF292929),
                    background = Color(0xFF121212)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(top = 16.dp, start = 20.dp, end = 20.dp, bottom = 24.dp)
                            .verticalScroll(scrollState),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "CRT Screen-Off Engine",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 28.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Text(
                            text = "Authentic Xposed electron-beam animation without root.",
                            fontSize = 12.5.sp,
                            lineHeight = 15.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(bottom = 14.dp)
                        )

                        // ── Card 1: System Permissions ──
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isA11yEnabled) Color(0xFF143828) else MaterialTheme.colorScheme.surface
                            ),
                            border = if (isA11yEnabled) BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.7f)) else null
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "1. Required System Access",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.5.sp,
                                        lineHeight = 17.sp,
                                        color = if (isA11yEnabled) Color(0xFF34D399) else Color.White
                                    )
                                    if (isA11yEnabled) {
                                        Text(
                                            "✓",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.5.sp,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                }
                                Text(
                                    "Enable the Accessibility Service to silently capture your active screen and execute the screen lock.",
                                    fontSize = 12.sp,
                                    lineHeight = 15.sp,
                                    color = if (isA11yEnabled) Color(0xFFA7F3D0) else Color(0xFF94A3B8),
                                    modifier = Modifier.padding(top = 1.dp, bottom = 10.dp)
                                )

                                // Accessibility Button
                                Button(
                                    onClick = {
                                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isA11yEnabled) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        if (isA11yEnabled) "Accessibility Service: Active ✓" else "Enable Accessibility Service",
                                        color = if (isA11yEnabled) Color.White else Color.Black,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Overlay Button with responsive feedback
                                OutlinedButton(
                                    onClick = {
                                        if (hasOverlay) {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Overlay permission is already granted ✓",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        startActivity(
                                            Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:$packageName")
                                            )
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        if (hasOverlay) "Overlay Permission: Granted ✓" else "Grant Overlay Permission",
                                        color = if (hasOverlay) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // ── Card 2: Collapsible Gesture Setup Methods ──
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    "2. Gesture Trigger Setup",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.5.sp,
                                    lineHeight = 17.sp,
                                    color = Color.White
                                )
                                Text(
                                    "Tap an option below to view step-by-step setup instructions:",
                                    fontSize = 12.sp,
                                    lineHeight = 15.sp,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(top = 1.dp, bottom = 8.dp)
                                )

                                // ── Option A: Universal "Open App" Setup ──
                                SetupOptionCard(
                                    title = "Option A: Universal \"Open App\" Setup",
                                    subtitle = "Samsung Good Lock, Pixel Quick Tap, Moto, Xiaomi",
                                    isExpanded = expandedOptionA,
                                    onToggle = { expandedOptionA = !expandedOptionA }
                                ) {
                                    Text(
                                        "Works with any gesture feature supporting app launches:\n" +
                                                "• Samsung Good Lock: Select 'Start application' -> 'CRT Lock'\n" +
                                                "• Google Pixel: Settings -> System -> Gestures -> Quick Tap -> Open app -> 'CRT Lock'\n" +
                                                "• Motorola: Moto app -> Gestures -> Double tap back -> 'CRT Lock'\n" +
                                                "• Xiaomi/HyperOS: Additional settings -> Gesture shortcuts -> Back tap -> 'CRT Lock'",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFFCBD5E1),
                                        lineHeight = 15.5.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // ── Option B: Samsung One Hand Operation + (Shortcuts) ──
                                SetupOptionCard(
                                    title = "Option B: Samsung One Hand Operation +",
                                    subtitle = "Mapped via Home screen shortcuts",
                                    isExpanded = expandedOptionB,
                                    onToggle = { expandedOptionB = !expandedOptionB }
                                ) {
                                    Text(
                                        "• Open Good Lock -> One Hand Operation +\n" +
                                                "• Choose your handle and swipe direction\n" +
                                                "• Select: 'Home screen shortcuts'\n" +
                                                "• Choose: 'CRT Screen-Off : CRT Lock'",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFFCBD5E1),
                                        lineHeight = 15.5.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // ── Option C: Non-Samsung Devices (Edge Gestures / FNG) ──
                                SetupOptionCard(
                                    title = "Option C: Non-Samsung Devices",
                                    subtitle = "Edge Gestures & Fluid Navigation Gestures",
                                    isExpanded = expandedOptionC,
                                    onToggle = { expandedOptionC = !expandedOptionC }
                                ) {
                                    Text(
                                        "To get the exact same Good Lock edge-swipe experience on Pixel, Motorola, OnePlus, or Xiaomi devices:\n" +
                                                "• Install 'Edge Gestures' or 'Fluid Navigation Gestures (FNG)'\n" +
                                                "• Set edge swipe action -> Launch app or shortcut -> 'CRT Lock'",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFFCBD5E1),
                                        lineHeight = 15.5.sp,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                startActivity(
                                                    Intent(
                                                        Intent.ACTION_VIEW,
                                                        Uri.parse("https://play.google.com/store/apps/details?id=com.ss.edgegestures")
                                                    )
                                                )
                                            } catch (_: Exception) {}
                                        },
                                        modifier = Modifier.fillMaxWidth().height(34.dp)
                                    ) {
                                        Text("View Edge Gestures on Google Play", fontSize = 11.5.sp)
                                    }
                                }
                            }
                        }

                        // ── Card 3: Animation Preferences ──
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    "3. Animation Style & Speed",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.5.sp,
                                    lineHeight = 17.sp,
                                    color = Color.White
                                )

                                Text(
                                    "Direction: ${if (selectedStyle == CrtStyle.CLASSIC_HORIZONTAL.name) "Horizontal (Classic)" else "Vertical"}",
                                    fontSize = 12.sp,
                                    lineHeight = 15.sp,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                                )
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    FilterChip(
                                        selected = selectedStyle == CrtStyle.CLASSIC_HORIZONTAL.name,
                                        onClick = {
                                            selectedStyle = CrtStyle.CLASSIC_HORIZONTAL.name
                                            prefs.edit().putString("anim_style", selectedStyle).apply()
                                        },
                                        label = { Text("Horizontal") },
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    FilterChip(
                                        selected = selectedStyle == CrtStyle.CLASSIC_VERTICAL.name,
                                        onClick = {
                                            selectedStyle = CrtStyle.CLASSIC_VERTICAL.name
                                            prefs.edit().putString("anim_style", selectedStyle).apply()
                                        },
                                        label = { Text("Vertical") }
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    "Animation Speed: ${speedMs.toInt()} ms",
                                    fontSize = 12.sp,
                                    lineHeight = 15.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Slider(
                                    value = speedMs,
                                    onValueChange = { speedMs = it },
                                    onValueChangeFinished = {
                                        prefs.edit().putInt("anim_speed", speedMs.toInt()).apply()
                                    },
                                    valueRange = 150f..800f,
                                    steps = 12
                                )
                            }
                        }

                        // Test Trigger Button
                        Button(
                            onClick = {
                                val intent = Intent(this@MainActivity, LockTriggerActivity::class.java).apply {
                                    action = "com.retro.crteffect.TRIGGER_LOCK"
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .padding(top = 4.dp)
                        ) {
                            Text("Test CRT Screen-Off Now", color = Color.Black, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact, dark-themed collapsible option card with responsive border and indicator.
 */
@Composable
fun SetupOptionCard(
    title: String,
    subtitle: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        border = BorderStroke(
            1.dp,
            if (isExpanded) Color(0xFF64B5F6).copy(alpha = 0.6f) else Color(0xFF383838)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Area: Clickable with ripple strictly confined to the header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // First line: Card label aligned strictly with the expanding arrow on the first line
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        lineHeight = 15.sp,
                        color = if (isExpanded) MaterialTheme.colorScheme.primary else Color.White,
                        modifier = Modifier
                            .weight(1f)
                            .alignByBaseline()
                    )
                    Text(
                        text = if (isExpanded) "▲" else "▼",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .alignByBaseline()
                    )
                }

                // Subtitle with tightened vertical spacing directly beneath label
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(top = 1.dp)
                )
            }

            // Content Container: Positioned below the header, completely outside the toggle ripple
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = tween(220)
                ) + fadeIn(animationSpec = tween(180)),
                exit = shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = tween(200)
                ) + fadeOut(animationSpec = tween(150))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
                ) {
                    HorizontalDivider(color = Color(0xFF383838), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(6.dp))
                    content()
                }
            }
        }
    }
}
