package com.retro.crteffect

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.PixelCopy
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

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
            var showSplash by remember { mutableStateOf(true) }
            var startCascade by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(1900)
                startCascade = true
                delay(100)
                showSplash = false
            }

            val scrollState = rememberScrollState()
            var speedMs by remember { mutableFloatStateOf(prefs.getInt("anim_speed", 500).toFloat()) }
            var selectedStyle by remember {
                mutableStateOf(
                    prefs.getString("anim_style", CrtStyle.CLASSIC_HORIZONTAL.name) ?: CrtStyle.CLASSIC_HORIZONTAL.name
                )
            }
            var enableEnhancedOverdrive by remember {
                mutableStateOf(prefs.getBoolean("enable_enhanced_overdrive", false))
            }
            var flashIntensity by remember {
                mutableFloatStateOf(prefs.getFloat("flash_intensity", 1.0f))
            }
            var afterglowDurationMs by remember {
                mutableFloatStateOf(prefs.getInt("afterglow_duration", 1000).toFloat())
            }
            var selectedResistance by remember {
                mutableStateOf(
                    prefs.getString("coil_resistance", CoilResistance.HEAVY.name) ?: CoilResistance.HEAVY.name
                )
            }
            var enableMagneticStretch by remember {
                mutableStateOf(prefs.getBoolean("enable_magnetic_stretch", false))
            }
            var enableAnamorphicFlare by remember {
                mutableStateOf(prefs.getBoolean("enable_anamorphic_flare", true))
            }

            var hasOverlay by remember { mutableStateOf(Settings.canDrawOverlays(this@MainActivity)) }
            var isA11yEnabled by remember { mutableStateOf(isAccessibilityEnabled()) }

            // Card 2 master collapsible state & 3 sub-card states (initially compact/collapsed)
            var expandedCard2 by remember { mutableStateOf(false) }
            var expandedOptionA by remember { mutableStateOf(false) }
            var expandedOptionB by remember { mutableStateOf(false) }
            var expandedOptionC by remember { mutableStateOf(false) }

            var showAboutPage by remember { mutableStateOf(false) }
            var menuExpanded by remember { mutableStateOf(false) }

            // Intercept system back gestures when About page is open
            BackHandler(enabled = showAboutPage) {
                showAboutPage = false
            }

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
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(top = 16.dp, start = 20.dp, end = 20.dp, bottom = 24.dp)
                            .verticalScroll(scrollState),
                        horizontalAlignment = Alignment.Start
                    ) {
                        CascadingEntrance(index = 0, visible = startCascade) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
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
                                }

                                // 3-dot overflow menu
                                Box {
                                    IconButton(
                                        onClick = { menuExpanded = true },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        MoreVertIcon(
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false },
                                        modifier = Modifier
                                            .background(Color(0xFF222222))
                                            .border(1.dp, Color(0xFF383838), RoundedCornerShape(8.dp))
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    "About",
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            },
                                            leadingIcon = {
                                                InfoIcon(
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                showAboutPage = true
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // ── Card 1: System Permissions ──
                        CascadingEntrance(index = 1, visible = startCascade) {
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
                    }

                    // ── Card 2: Collapsible Gesture Setup Methods ──
                    CascadingEntrance(index = 2, visible = startCascade) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Header: Clickable to toggle Card 2 expansion
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expandedCard2 = !expandedCard2 }
                                        .padding(14.dp)
                                ) {
                                    // First line: Card title on left, dynamic material chevron on right
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "2. Gesture Trigger Setup",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.5.sp,
                                            lineHeight = 17.sp,
                                            color = if (expandedCard2) MaterialTheme.colorScheme.primary else Color.White,
                                            modifier = Modifier.weight(1f)
                                        )
                                        ChevronIcon(
                                            isExpanded = expandedCard2,
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.padding(start = 6.dp)
                                        )
                                    }

                                    // Subtitle directly beneath the first line
                                    Text(
                                        "Tap an option below to view step-by-step setup instructions:",
                                        fontSize = 12.sp,
                                        lineHeight = 15.sp,
                                        color = Color(0xFF94A3B8),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                // Collapsible Sub-Cards Content
                                AnimatedVisibility(
                                    visible = expandedCard2,
                                    enter = expandVertically(
                                        expandFrom = Alignment.Top,
                                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                                    ) + fadeIn(animationSpec = tween(180)),
                                    exit = shrinkVertically(
                                        shrinkTowards = Alignment.Top,
                                        animationSpec = tween(200, easing = FastOutSlowInEasing)
                                    ) + fadeOut(animationSpec = tween(150))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                                    ) {
                                        HorizontalDivider(color = Color(0xFF383838), thickness = 0.8.dp)
                                        Spacer(modifier = Modifier.height(10.dp))

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
                            }
                        }
                    }

                    // ── Card 3: Animation Preferences ──
                    CascadingEntrance(index = 3, visible = startCascade) {
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

                                Spacer(modifier = Modifier.height(10.dp))

                                // Restore to Default button for Card 3
                                Button(
                                    onClick = {
                                        speedMs = 500f
                                        selectedStyle = CrtStyle.CLASSIC_HORIZONTAL.name
                                        prefs.edit()
                                            .putInt("anim_speed", 500)
                                            .putString("anim_style", CrtStyle.CLASSIC_HORIZONTAL.name)
                                            .apply()
                                        Toast.makeText(this@MainActivity, "Card 3 restored to default", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1E1E1E),
                                        contentColor = Color.White
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFF383838)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                ) {
                                    Text(
                                        "Restore to Default",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // ── Card 4: Enhanced Phosphor Overdrive (Optional) ──
                    CascadingEntrance(index = 4, visible = startCascade) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (enableEnhancedOverdrive) Color(0xFF1E2836) else MaterialTheme.colorScheme.surface
                            ),
                            border = if (enableEnhancedOverdrive) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            "4. Enhanced Phosphor Overdrive",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.5.sp,
                                            lineHeight = 17.sp,
                                            color = if (enableEnhancedOverdrive) MaterialTheme.colorScheme.primary else Color.White
                                        )
                                        Text(
                                            "RGB chromatic aberration, ease-in resistance & cathode afterglow.",
                                            fontSize = 11.sp,
                                            lineHeight = 13.5.sp,
                                            color = Color(0xFF94A3B8),
                                            modifier = Modifier.padding(top = 1.dp)
                                        )
                                    }
                                    Switch(
                                        checked = enableEnhancedOverdrive,
                                        onCheckedChange = {
                                            enableEnhancedOverdrive = it
                                            prefs.edit().putBoolean("enable_enhanced_overdrive", it).apply()
                                        }
                                    )
                                }

                                if (!enableEnhancedOverdrive) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                                        border = BorderStroke(1.dp, Color(0xFF333333))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                "Authentic CyanogenMod Engine Active",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                            Text(
                                                "Original quadratic timing, exaggerated RGB channel split, crescendo phosphor flare, and radiant beam snap.",
                                                fontSize = 11.sp,
                                                lineHeight = 13.sp,
                                                color = Color(0xFF6B7280),
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // 3 Presets Magnetic Coil Resistance Selector
                                    val currentResistance = try {
                                        CoilResistance.valueOf(selectedResistance)
                                    } catch (_: Exception) {
                                        CoilResistance.EXTREME
                                    }

                                    Text(
                                        "Magnetic Coil Resistance: ${currentResistance.label}",
                                        fontSize = 12.sp,
                                        lineHeight = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFE2E8F0)
                                    )
                                    Text(
                                        currentResistance.subtitle,
                                        fontSize = 11.sp,
                                        lineHeight = 13.5.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 1.dp, bottom = 6.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        CoilResistance.values().forEach { res ->
                                            FilterChip(
                                                selected = selectedResistance == res.name,
                                                onClick = {
                                                    selectedResistance = res.name
                                                    prefs.edit().putString("coil_resistance", res.name).apply()
                                                },
                                                label = {
                                                    Text(
                                                        when (res) {
                                                            CoilResistance.CLASSIC -> "2.0x Classic"
                                                            CoilResistance.HEAVY -> "4.0x Heavy"
                                                            CoilResistance.EXTREME -> "6.0x Extreme"
                                                        },
                                                        fontSize = 11.sp,
                                                        maxLines = 1
                                                    )
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Magnetic Linear Stretch (Elastic Tension) Toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text(
                                                "Magnetic Linear Stretch (Elastic Tension)",
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 12.sp,
                                                lineHeight = 15.sp,
                                                color = if (enableMagneticStretch) MaterialTheme.colorScheme.primary else Color.White
                                            )
                                            Text(
                                                "Screen edges cling to the bezels while content pulls vertically into the center equator before snapping loose.",
                                                fontSize = 10.5.sp,
                                                lineHeight = 13.sp,
                                                color = Color(0xFF94A3B8),
                                                modifier = Modifier.padding(top = 1.dp)
                                            )
                                        }
                                        Switch(
                                            checked = enableMagneticStretch,
                                            onCheckedChange = {
                                                enableMagneticStretch = it
                                                prefs.edit().putBoolean("enable_magnetic_stretch", it).apply()
                                            }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Anamorphic Plasma Flare (Cyan & White) Toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text(
                                                "Anamorphic Chromatic Flare (Prism Split)",
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 12.sp,
                                                lineHeight = 15.sp,
                                                color = if (enableAnamorphicFlare) MaterialTheme.colorScheme.primary else Color.White
                                            )
                                            Text(
                                                "95% incandescent white beam + cyan prism fringe, dynamic center swell & cathode bridge.",
                                                fontSize = 10.5.sp,
                                                lineHeight = 13.sp,
                                                color = Color(0xFF94A3B8),
                                                modifier = Modifier.padding(top = 1.dp)
                                            )
                                        }
                                        Switch(
                                            checked = enableAnamorphicFlare,
                                            onCheckedChange = {
                                                enableAnamorphicFlare = it
                                                prefs.edit().putBoolean("enable_anamorphic_flare", it).apply()
                                            }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // AGSL GPU Optics Info Pill
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                    ) {
                                        Text(
                                            "Optics: Exaggerated Red/Cyan Vertical Split + Squish Blur + H-Sync Combing",
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Overdrive Intensity Slider
                                    val intensityPercent = (flashIntensity * 100).toInt()
                                    val intensityTag = when {
                                        flashIntensity <= 0.05f -> "Off (0%)"
                                        flashIntensity <= 0.5f -> "Subtle ($intensityPercent%)"
                                        flashIntensity <= 1.0f -> "Dramatic ($intensityPercent%)"
                                        else -> "Supernova ($intensityPercent%)"
                                    }
                                    Text(
                                        "Flare & Bloom Overdrive: $intensityTag",
                                        fontSize = 12.sp,
                                        lineHeight = 15.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Slider(
                                        value = flashIntensity,
                                        onValueChange = { flashIntensity = it },
                                        onValueChangeFinished = {
                                            prefs.edit().putFloat("flash_intensity", flashIntensity).apply()
                                        },
                                        valueRange = 0.0f..1.5f,
                                        steps = 14
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Afterglow Duration Slider
                                    Text(
                                        "Cathode Afterglow Duration: ${afterglowDurationMs.toInt()} ms",
                                        fontSize = 12.sp,
                                        lineHeight = 15.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Text(
                                        "Independent center dot persistence on pitch-black OLED display.",
                                        fontSize = 10.5.sp,
                                        lineHeight = 13.sp,
                                        color = Color(0xFF6B7280),
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                    Slider(
                                        value = afterglowDurationMs,
                                        onValueChange = { afterglowDurationMs = it },
                                        onValueChangeFinished = {
                                            prefs.edit().putInt("afterglow_duration", afterglowDurationMs.toInt()).apply()
                                        },
                                        valueRange = 100f..1500f,
                                        steps = 13
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Restore to Default button for Card 4
                                Button(
                                    onClick = {
                                        enableEnhancedOverdrive = false
                                        selectedResistance = CoilResistance.HEAVY.name
                                        enableMagneticStretch = false
                                        enableAnamorphicFlare = true
                                        flashIntensity = 1.0f
                                        afterglowDurationMs = 1000f
                                        prefs.edit()
                                            .putBoolean("enable_enhanced_overdrive", false)
                                            .putString("coil_resistance", CoilResistance.HEAVY.name)
                                            .putBoolean("enable_magnetic_stretch", false)
                                            .putBoolean("enable_anamorphic_flare", true)
                                            .putFloat("flash_intensity", 1.0f)
                                            .putInt("afterglow_duration", 1000)
                                            .apply()
                                        Toast.makeText(this@MainActivity, "Card 4 restored to default", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1E1E1E),
                                        contentColor = Color.White
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFF383838)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                ) {
                                    Text(
                                        "Restore to Default",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Test CRT Screen-Off Button (Full-width)
                    CascadingEntrance(index = 5, visible = startCascade) {
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
                                .padding(top = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Test CRT Screen-Off Now", color = Color.Black, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    }

                    // About Page Overlay
                    AnimatedVisibility(
                        visible = showAboutPage,
                        enter = slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(280, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(200)),
                        exit = slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(240, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(180)),
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(2f)
                    ) {
                        AboutScreen(onBack = { showAboutPage = false })
                    }

                    // 2-second exclusive splash screen with unmasked CRT_SplashScreen.png
                    AnimatedVisibility(
                        visible = showSplash,
                        enter = EnterTransition.None,
                        exit = fadeOut(animationSpec = tween(350)),
                        modifier = Modifier.zIndex(10f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF121212)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.splash_crt_logo),
                                contentDescription = "CRT Screen Off Splash",
                                modifier = Modifier
                                    .fillMaxWidth(0.72f)
                                    .wrapContentHeight()
                            )
                        }
                    }
                }
            }
        }
    }
}
}

/**
 * Staggered cascading entrance wrapper that smoothly slides items upwards
 * from bottom to top with an elegant alpha fade and decelerating physics.
 */
@Composable
fun CascadingEntrance(
    index: Int,
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            delay(index * 75L)
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 580,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = animProgress.value
                translationY = (1f - animProgress.value) * 150f * density
            }
    ) {
        content()
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
                // First line: Card label aligned strictly with the dynamic material chevron
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        lineHeight = 15.sp,
                        color = if (isExpanded) MaterialTheme.colorScheme.primary else Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    ChevronIcon(
                        isExpanded = isExpanded,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.padding(start = 6.dp)
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
