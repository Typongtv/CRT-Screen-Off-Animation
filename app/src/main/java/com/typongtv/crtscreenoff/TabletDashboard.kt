package com.typongtv.crtscreenoff

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random


/**
 * Tablet Studio Console (Master-Detail Workstation)
 *
 * Dedicated widescreen two-pane workstation experience for tablets and unfolded foldables in landscape.
 * Left Panel: Fixed ~390dp workstation column housing Fixed Header, Required System Access, Screen-Off Setup,
 * Tactile Dual Engine Deck, Live Engine Telemetry & Parameter HUD, and sticky Test button.
 * Right Panel: Expansive Parameter Bays (Cyanogen Retro vs Phosphor Overdrive) & In-Place Subpages.
 */
@Composable
fun TabletDashboard(
    prefs: SharedPreferences,
    onMoveTaskToBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val densityObj = LocalDensity.current
    val density = densityObj.density

    var showSplash by rememberSaveable { mutableStateOf(!MainActivity.hasShownSplash) }
    var startCascade by rememberSaveable { mutableStateOf(MainActivity.hasShownSplash) }

    // Secondary residual flyback discharge after cards cascade in (2 flickers -> 1.0s rest -> delayed long discharge)
    val dischargeAnim = remember { Animatable(0f) }
    LaunchedEffect(startCascade) {
        if (startCascade) {
            // Wait for all items to complete their cascade ascent (4 * 95ms + 650ms ≈ 1030ms)
            delay(1150L)

            // ── Pulse 1: Primary High-Voltage Discharge ──
            dischargeAnim.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(38, easing = LinearEasing)
            )
            dischargeAnim.animateTo(
                targetValue = 0.0f,
                animationSpec = tween(190, easing = FastOutSlowInEasing)
            )

            // Inter-pulse inductive coil rebound gap
            delay(75L)

            // ── Pulse 2: Resonant Harmonic Echo / Ringback ──
            dischargeAnim.animateTo(
                targetValue = 0.65f,
                animationSpec = tween(32, easing = LinearEasing)
            )
            dischargeAnim.animateTo(
                targetValue = 0.0f,
                animationSpec = tween(230, easing = FastOutSlowInEasing)
            )

            // ── Deceptive Calm / Grounding Rest (1.0 second) ──
            delay(1000L)

            // ── Pulse 3: Delayed Deep Capacitive Discharge (Lingering shudder) ──
            dischargeAnim.animateTo(
                targetValue = 0.90f,
                animationSpec = tween(48, easing = LinearEasing)
            )
            dischargeAnim.animateTo(
                targetValue = 0.0f,
                animationSpec = tween(440, easing = FastOutSlowInEasing)
            )
        }
    }

    // ── SharedPreferences Reactive States ──
    var enableCrtAnimation by remember {
        mutableStateOf(prefs.getBoolean("enable_crt_animation", true))
    }
    var speedMs by remember {
        mutableFloatStateOf(prefs.getInt("anim_speed", 600).toFloat().coerceIn(400f, 1200f))
    }
    var selectedStyle by remember {
        mutableStateOf(
            prefs.getString("anim_style", CrtStyle.CLASSIC_HORIZONTAL.name) ?: CrtStyle.CLASSIC_HORIZONTAL.name
        )
    }
    var selectedDeflectionDynamics by remember {
        mutableStateOf(
            prefs.getString("deflection_dynamics", DeflectionDynamics.STYLIZED_CREEP.name)
                ?: DeflectionDynamics.STYLIZED_CREEP.name
        )
    }
    var enableEnhancedOverdrive by remember {
        mutableStateOf(prefs.getBoolean("enable_enhanced_overdrive", false))
    }
    var flashIntensity by remember {
        mutableFloatStateOf(prefs.getFloat("flash_intensity", 1.0f).coerceIn(0.0f, 2.0f))
    }
    var afterglowDurationMs by remember {
        mutableFloatStateOf(prefs.getInt("afterglow_duration", 800).toFloat().coerceIn(0f, 1500f))
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
    var speedOverdriveMs by remember {
        mutableFloatStateOf(prefs.getInt("anim_speed_overdrive", 500).toFloat().coerceIn(150f, 1000f))
    }

    // ── Permissions States ──
    var hasOverlay by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var isA11yEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var expandedCard1 by remember { mutableStateOf(!isA11yEnabled) }

    // Resume detection
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val wasEnabled = isA11yEnabled
                isA11yEnabled = isAccessibilityServiceEnabled(context)
                hasOverlay = Settings.canDrawOverlays(context)
                if (!wasEnabled && isA11yEnabled) {
                    expandedCard1 = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Polling fallback
    LaunchedEffect(isA11yEnabled) {
        if (!isA11yEnabled) {
            while (!isA11yEnabled && isActive) {
                delay(600L)
                if (isAccessibilityServiceEnabled(context)) {
                    isA11yEnabled = true
                    hasOverlay = Settings.canDrawOverlays(context)
                    expandedCard1 = false
                    break
                }
            }
        }
    }

    // ── Card 2 Collapsible States ──
    var expandedCard2 by remember { mutableStateOf(false) }
    var expandedOptionA by remember { mutableStateOf(false) }
    var expandedOptionB by remember { mutableStateOf(false) }
    var expandedOptionC by remember { mutableStateOf(false) }

    // ── Subpages & Overlays ──
    var showAboutPage by remember { mutableStateOf(false) }
    var showAcknowledgmentPage by remember { mutableStateOf(false) }
    var aboutTrigger by remember { mutableIntStateOf(0) }
    var acknowledgmentTrigger by remember { mutableIntStateOf(0) }
    var menuExpanded by remember { mutableStateOf(false) }

    // ── Capacitor Discharge Advisory Banner ──
    val hideAdvisoryPref = prefs.getBoolean("hide_anode_bleed_notice", false)
    var showAdvisoryBanner by remember { mutableStateOf(false) }
    var dontShowAdvisoryAgain by remember { mutableStateOf(prefs.getBoolean("hide_anode_bleed_notice", false)) }

    LaunchedEffect(Unit) {
        if (!hideAdvisoryPref) {
            delay(800L)
            showAdvisoryBanner = true
        }
    }

    // ── Exit Prompt & Back Handler ──
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    val coroutineScope = rememberCoroutineScope()
    var exitPromptJob by remember { mutableStateOf<Job?>(null) }
    var showExitPrompt by remember { mutableStateOf(false) }
    var isExiting by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                showExitPrompt = false
                exitPromptJob?.cancel()
                isExiting = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val leftScrollState = rememberScrollState()
    val rightScrollState = rememberScrollState()

    BackHandler(enabled = true) {
        if (showSplash || isExiting) return@BackHandler
        if (showAcknowledgmentPage) {
            showAcknowledgmentPage = false
        } else if (showAboutPage) {
            showAboutPage = false
        } else if (menuExpanded) {
            menuExpanded = false
        } else if (showAdvisoryBanner) {
            showAdvisoryBanner = false
        } else {
            val currentTime = System.currentTimeMillis()
            if (showExitPrompt && (currentTime - lastBackPressTime < 3000L)) {
                isExiting = true
                showExitPrompt = false
                exitPromptJob?.cancel()
                coroutineScope.launch {
                    delay(280L)
                    onMoveTaskToBack()
                    isExiting = false
                }
            } else {
                lastBackPressTime = currentTime
                showExitPrompt = true
                if (leftScrollState.value > 0) {
                    coroutineScope.launch { leftScrollState.animateScrollTo(0) }
                }
                exitPromptJob?.cancel()
                exitPromptJob = coroutineScope.launch {
                    delay(3000L)
                    showExitPrompt = false
                }
            }
        }
    }

    // ── Shaders & CRT Glitch Logic ──
    val headerShader = remember {
        try {
            RuntimeShader(MainActivity.DISCHARGE_AGSL)
        } catch (_: Throwable) {
            null
        }
    }

    val headerGlitchAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(Random.nextLong(2800L, 5200L))
            when (Random.nextInt(3)) {
                0 -> {
                    headerGlitchAnim.animateTo(0.95f, tween(36, easing = LinearEasing))
                    headerGlitchAnim.animateTo(0.12f, tween(75, easing = FastOutSlowInEasing))
                    headerGlitchAnim.animateTo(0.65f, tween(32, easing = LinearEasing))
                    headerGlitchAnim.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
                }
                1 -> {
                    headerGlitchAnim.animateTo(0.85f, tween(130, easing = FastOutSlowInEasing))
                    headerGlitchAnim.animateTo(0f, tween(40, easing = LinearEasing))
                }
                2 -> {
                    repeat(3) {
                        headerGlitchAnim.animateTo(0.70f, tween(26, easing = LinearEasing))
                        headerGlitchAnim.animateTo(0f, tween(45, easing = LinearEasing))
                        delay(20L)
                    }
                }
            }
        }
    }

    var headerHeightPx by remember { mutableIntStateOf(0) }
    var footerHeightPx by remember { mutableIntStateOf(0) }

    val headerHeightDp = with(densityObj) { headerHeightPx.toDp() }
    val footerHeightDp = with(densityObj) { footerHeightPx.toDp() }

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

                // Full-screen Touch Static & Scanline Arc Container
                CrtTouchStaticContainer(
                    subtleMode = true,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Two-Pane Master-Detail Workstation Layout
                    Row(modifier = Modifier.fillMaxSize()) {

                        // ══════════════════════════════════════════════════════════════
                        // ── LEFT PANEL: STUDIO CONSOLE (Fixed 390dp Width) ──
                        // ══════════════════════════════════════════════════════════════
                        Box(
                            modifier = Modifier
                                .width(390.dp)
                                .fillMaxHeight()
                                .background(Color(0xFF141414))
                        ) {
                            // ── 1. Left Panel Scrollable Content ──
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                                    .navigationBarsPadding()
                                    .padding(bottom = 16.dp)
                                    .verticalScroll(leftScrollState),
                                horizontalAlignment = Alignment.Start
                            ) {
                                // Spacer clearing fixed header
                                Spacer(modifier = Modifier.height(headerHeightDp.coerceAtLeast(80.dp) + 16.dp))

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    // ── Slide-Down Exit Prompt Card ──
                                    AnimatedVisibility(
                                        visible = showExitPrompt,
                                        enter = slideInVertically(
                                            initialOffsetY = { -it },
                                            animationSpec = tween(450, easing = CubicBezierEasing(0.08f, 0.85f, 0.15f, 1.0f))
                                        ) + expandVertically(expandFrom = Alignment.Top),
                                        exit = slideOutVertically(
                                            targetOffsetY = { -it },
                                            animationSpec = tween(280, easing = CubicBezierEasing(0.35f, 0.0f, 0.15f, 1.0f))
                                        ) + shrinkVertically(shrinkTowards = Alignment.Top)
                                    ) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 12.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFAD4647)),
                                            border = BorderStroke(1.dp, Color(0xFFC25253))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 12.dp, horizontal = 14.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                LogoutIcon(tint = Color.White, rotation = 180f, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Press back again to exit",
                                                    color = Color.White,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
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
                                            containerColor = if (isA11yEnabled && expandedCard1) Color(0xFF192520)
                                            else if (isA11yEnabled) Color(0xFF141D18)
                                            else MaterialTheme.colorScheme.surface
                                        ),
                                        border = if (isA11yEnabled) BorderStroke(
                                            1.dp,
                                            Color(0xFF3B7A60).copy(alpha = if (expandedCard1) 0.5f else 0.3f)
                                        ) else null
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            // Header: Clickable to toggle Card 1 expansion
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { expandedCard1 = !expandedCard1 }
                                                    .padding(14.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        "Required System Access",
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 18.sp,
                                                        lineHeight = 22.sp,
                                                        color = if (isA11yEnabled && expandedCard1) Color(0xFF86BF9E) else Color.White,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    ChevronIcon(
                                                        isExpanded = expandedCard1,
                                                        tint = if (isA11yEnabled) Color(0xFF5A9E7F) else Color(0xFF94A3B8),
                                                        modifier = Modifier.padding(start = 6.dp)
                                                    )
                                                }

                                                Text(
                                                    if (isA11yEnabled && !expandedCard1)
                                                        "All required permissions active ✓ (Tap to manage)"
                                                    else
                                                        "Enable the Accessibility Service to silently capture your active screen and execute the screen lock.",
                                                    fontSize = 12.sp,
                                                    lineHeight = 15.sp,
                                                    color = if (isA11yEnabled && !expandedCard1) Color(0xFF87BE9F)
                                                    else if (isA11yEnabled) Color(0xFF9CB8AA)
                                                    else Color(0xFF94A3B8),
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            }

                                            // Collapsible Buttons Content
                                            AnimatedVisibility(
                                                visible = expandedCard1,
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
                                                    // Accessibility Button
                                                    Button(
                                                        onClick = {
                                                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                                        },
                                                        shape = RoundedCornerShape(10.dp),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = if (isA11yEnabled) Color(0xFF26543F) else MaterialTheme.colorScheme.primary
                                                        ),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            if (isA11yEnabled) "Accessibility Service: Active ✓" else "Enable Accessibility Service",
                                                            color = if (isA11yEnabled) Color(0xFFE2E8F0) else Color.Black,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height(10.dp))

                                                    // Overlay Button with responsive feedback
                                                    OutlinedButton(
                                                        onClick = {
                                                            if (hasOverlay) {
                                                                Toast.makeText(
                                                                    context,
                                                                    "Overlay permission is already granted ✓",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                            context.startActivity(
                                                                Intent(
                                                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                                    Uri.parse("package:${context.packageName}")
                                                                )
                                                            )
                                                        },
                                                        shape = RoundedCornerShape(10.dp),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            if (hasOverlay) "Overlay Permission: Granted ✓" else "Grant Overlay Permission",
                                                            color = if (hasOverlay) Color(0xFF5A9E7F) else MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
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
                                                        "Screen-Off Trigger Setup",
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 18.sp,
                                                        lineHeight = 22.sp,
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
                                                    "Tap an option below for gesture, key, & automation setups:",
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

                                                    // ── Option A: Samsung Exclusive (One UI & Good Lock) ──
                                                    SetupOptionCard(
                                                        title = "Option A: Samsung Exclusive",
                                                        subtitle = "One Hand Operation + (Gestures) & RegiStar (Side Key)",
                                                        isExpanded = expandedOptionA,
                                                        onToggle = { expandedOptionA = !expandedOptionA }
                                                    ) {
                                                        Text(
                                                            "• Good Lock (One Hand Operation +) [Recommended Gesture]:\n" +
                                                                    "  1. Open One Hand Operation +\n" +
                                                                    "  2. Select your gesture handle (Left or Right) & swipe direction\n" +
                                                                    "  3. Tap action and choose 'Start application' (Do NOT use 'Home screen shortcut' to prevent desktop redraws)\n" +
                                                                    "  4. Select 'CRT Lock' from the application list\n\n" +
                                                                    "• Good Lock (RegiStar) [Physical Side Key / Back-Tap]:\n" +
                                                                    "  1. Open Good Lock -> RegiStar\n" +
                                                                    "  2. Select 'Side-Key press and hold action' or 'Back-Tap action'\n" +
                                                                    "  3. Choose 'Open app' -> Select 'CRT Lock'",
                                                            fontSize = 11.5.sp,
                                                            color = Color(0xFFCBD5E1),
                                                            lineHeight = 16.sp
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height(6.dp))

                                                    // ── Option B: Non-Samsung Devices ──
                                                    SetupOptionCard(
                                                        title = "Option B: Non-Samsung Devices",
                                                        subtitle = "Edge Gestures, Pixel Quick Tap, Motorola, Xiaomi",
                                                        isExpanded = expandedOptionB,
                                                        onToggle = { expandedOptionB = !expandedOptionB }
                                                    ) {
                                                        Text(
                                                            "• Edge Gestures (Pixel, OnePlus, Nothing, Sony, Moto):\n" +
                                                                    "  Gives the identical edge-swipe gesture experience as Good Lock:\n" +
                                                                    "  1. Install Edge Gestures or Fluid Navigation Gestures (FNG)\n" +
                                                                    "  2. Add an edge handle and set swipe action -> Launch app -> 'CRT Lock'\n\n" +
                                                                    "• Google Pixel (Quick Tap):\n" +
                                                                    "  Settings -> System -> Gestures -> Quick Tap -> Open app -> 'CRT Lock'\n\n" +
                                                                    "• Motorola (Quick Launch):\n" +
                                                                    "  Moto app -> Gestures -> Double tap back -> Launch app -> 'CRT Lock'\n\n" +
                                                                    "• Xiaomi / Redmi / POCO (HyperOS / MIUI):\n" +
                                                                    "  Settings -> Additional settings -> Gesture shortcuts -> Back tap -> Open app -> 'CRT Lock'",
                                                            fontSize = 11.5.sp,
                                                            color = Color(0xFFCBD5E1),
                                                            lineHeight = 16.sp,
                                                            modifier = Modifier.padding(bottom = 8.dp)
                                                        )

                                                        OutlinedButton(
                                                            onClick = {
                                                                try {
                                                                    context.startActivity(
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

                                                    Spacer(modifier = Modifier.height(6.dp))

                                                    // ── Option C: Universal & Automation Methods ──
                                                    SetupOptionCard(
                                                        title = "Option C: Universal & Automation",
                                                        subtitle = "Hardware Key Remappers, Tasker, MacroDroid",
                                                        isExpanded = expandedOptionC,
                                                        onToggle = { expandedOptionC = !expandedOptionC }
                                                    ) {
                                                        Text(
                                                            "• Hardware Key Remapping (All Android Devices):\n" +
                                                                    "  1. Install Button Mapper or Key Mapper from Google Play\n" +
                                                                    "  2. Choose a hardware key (e.g. Volume long-press or double-press)\n" +
                                                                    "  3. Set action: Launch Application -> 'CRT Lock'\n\n" +
                                                                    "• Automation Broadcast (Zero-UI Background Trigger):\n" +
                                                                    "  Tasker, MacroDroid, or Automate can trigger the CRT effect with zero screen flicker or window overhead:\n" +
                                                                    "  • Action: com.typongtv.crtscreenoff.TRIGGER_LOCK\n" +
                                                                    "  • Package: com.typongtv.crtscreenoff",
                                                            fontSize = 11.5.sp,
                                                            color = Color(0xFFCBD5E1),
                                                            lineHeight = 16.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                    // ── Card 3: Tactile Dual Engine Deck ──
                                    CascadingEntrance(index = 3, visible = startCascade) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 12.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                                        border = BorderStroke(1.dp, Color(0xFF383838))
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "Dual Engine Deck",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 18.sp,
                                                    lineHeight = 22.sp,
                                                    color = Color.White
                                                )
                                                // Master CRT toggle
                                                Switch(
                                                    checked = enableCrtAnimation,
                                                    onCheckedChange = {
                                                        enableCrtAnimation = it
                                                        prefs.edit().putBoolean("enable_crt_animation", it).apply()
                                                    },
                                                    modifier = Modifier.height(24.dp)
                                                )
                                            }

                                            Text(
                                                "Select the active CRT physics engine to configure in the right bay:",
                                                fontSize = 12.sp,
                                                lineHeight = 15.sp,
                                                color = Color(0xFF94A3B8),
                                                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                                            )

                                            // Mode 1: Cyanogen Retro Button
                                            val isMode1Active = !enableEnhancedOverdrive
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        enableEnhancedOverdrive = false
                                                        prefs.edit().putBoolean("enable_enhanced_overdrive", false).apply()
                                                    },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isMode1Active) Color(0xFF132332) else Color(0xFF161616),
                                                border = BorderStroke(
                                                    1.2.dp,
                                                    if (isMode1Active) MaterialTheme.colorScheme.primary else Color(0xFF2A2A2A)
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isMode1Active) MaterialTheme.colorScheme.primary else Color(0xFF4A4A4A))
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "MODE 1: CYANOGEN RETRO",
                                                            fontSize = 12.sp,
                                                            lineHeight = 14.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            letterSpacing = 0.5.sp,
                                                            color = if (isMode1Active) MaterialTheme.colorScheme.primary else Color(0xFFCBD5E1)
                                                        )
                                                        Text(
                                                            text = "CM7/CM9 Classic vintage electron beam collapse",
                                                            fontSize = 10.5.sp,
                                                            lineHeight = 13.sp,
                                                            color = Color(0xFF94A3B8),
                                                            modifier = Modifier.padding(top = 1.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Mode 2: Phosphor Overdrive Button
                                            val isMode2Active = enableEnhancedOverdrive
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        enableEnhancedOverdrive = true
                                                        prefs.edit().putBoolean("enable_enhanced_overdrive", true).apply()
                                                    },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isMode2Active) Color(0xFF132332) else Color(0xFF161616),
                                                border = BorderStroke(
                                                    1.2.dp,
                                                    if (isMode2Active) MaterialTheme.colorScheme.primary else Color(0xFF2A2A2A)
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isMode2Active) MaterialTheme.colorScheme.primary else Color(0xFF4A4A4A))
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "MODE 2: PHOSPHOR OVERDRIVE",
                                                            fontSize = 12.sp,
                                                            lineHeight = 14.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            letterSpacing = 0.5.sp,
                                                            color = if (isMode2Active) MaterialTheme.colorScheme.primary else Color(0xFFCBD5E1)
                                                        )
                                                        Text(
                                                            text = "Multi-line raster bloom & cathode dot afterglow",
                                                            fontSize = 10.5.sp,
                                                            lineHeight = 13.sp,
                                                            color = Color(0xFF94A3B8),
                                                            modifier = Modifier.padding(top = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                }

                                // Clearance spacer so the lowest card can scroll completely clear of the fixed bottom footer
                                Spacer(modifier = Modifier.height(footerHeightDp.coerceAtLeast(80.dp)))
                            }

                            // ── 2. Left Panel Fixed Stationary Header ──
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
                                    .zIndex(10f)
                            ) {
                                CascadingEntrance(index = 0, visible = startCascade) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF141414))
                                            .statusBarsPadding()
                                            .zIndex(2f)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 12.dp)
                                                .onGloballyPositioned { coordinates ->
                                                    headerHeightPx = coordinates.size.height
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                val glitchVal = (headerGlitchAnim.value + dischargeAnim.value).coerceAtMost(1.0f)
                                                val headerChromaticOffset = glitchVal * 10f * density
                                                val headerShakeX = if (glitchVal > 0.01f) sin(glitchVal * 20f) * 1.6f * density * glitchVal else 0f
                                                val headerRenderEffect = remember(glitchVal) {
                                                    if (glitchVal > 0.005f && headerShader != null) {
                                                        try {
                                                            headerShader.setFloatUniform("chromaticOffset", headerChromaticOffset)
                                                            RenderEffect.createRuntimeShaderEffect(headerShader, "image").asComposeRenderEffect()
                                                        } catch (_: Throwable) {
                                                            null
                                                        }
                                                    } else null
                                                }

                                                Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                                                    Text(
                                                        text = "CRT Screen-Off Engine",
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        lineHeight = 24.sp,
                                                        color = Color(0xFFE2E8F0),
                                                        modifier = Modifier
                                                            .padding(bottom = 2.dp)
                                                            .graphicsLayer {
                                                                translationX = headerShakeX
                                                                renderEffect = headerRenderEffect
                                                            }
                                                    )
                                                    Text(
                                                        text = "Retro Cathode Ray Tube Animation Engine",
                                                        fontSize = 11.5.sp,
                                                        lineHeight = 14.sp,
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                }

                                                // 3-dot overflow menu
                                                Box {
                                                    IconButton(
                                                        onClick = { menuExpanded = true },
                                                        modifier = Modifier.size(34.dp)
                                                    ) {
                                                        MoreVertIcon(tint = Color(0xFFE2E8F0))
                                                    }
                                                    DropdownMenu(
                                                        expanded = menuExpanded,
                                                        onDismissRequest = { menuExpanded = false },
                                                        modifier = Modifier
                                                            .background(Color(0xFF222222))
                                                            .border(1.dp, Color(0xFF383838), RoundedCornerShape(8.dp))
                                                    ) {
                                                        DropdownMenuItem(
                                                            text = { Text("Capacitor Advisory", color = Color.White, fontSize = 13.5.sp) },
                                                            leadingIcon = { AdvisoryIcon(tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) },
                                                            onClick = {
                                                                menuExpanded = false
                                                                dontShowAdvisoryAgain = prefs.getBoolean("hide_anode_bleed_notice", false)
                                                                showAdvisoryBanner = true
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text("Special Acknowledgment", color = Color.White, fontSize = 13.5.sp) },
                                                            leadingIcon = { SparkleIcon(tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) },
                                                            onClick = {
                                                                menuExpanded = false
                                                                showAboutPage = false
                                                                showAcknowledgmentPage = true
                                                                acknowledgmentTrigger++
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text("About", color = Color.White, fontSize = 13.5.sp) },
                                                            leadingIcon = { InfoIcon(tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) },
                                                            onClick = {
                                                                menuExpanded = false
                                                                showAcknowledgmentPage = false
                                                                showAboutPage = true
                                                                aboutTrigger++
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Gradient feather below header
                                    val featherAlpha = (leftScrollState.value / 35f).coerceIn(0f, 1f)
                                    if (featherAlpha > 0.01f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(12.dp)
                                                .graphicsLayer { alpha = featherAlpha }
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(Color(0xFF141414), Color(0xFF141414).copy(alpha = 0.5f), Color.Transparent)
                                                    )
                                                )
                                        )
                                    }
                                }
                            }
                        }

                        // ── 3. Left Panel Fixed Stationary Footer (Test Button) ──
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .zIndex(10f)
                        ) {
                            CascadingEntrance(index = 4, visible = startCascade) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    val canScrollForward = leftScrollState.canScrollForward
                                    val upwardShadowAlpha = if (canScrollForward) 1f else 0.45f
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(16.dp)
                                            .graphicsLayer { alpha = upwardShadowAlpha }
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color.Transparent, Color(0xFF141414).copy(alpha = 0.55f), Color(0xFF141414))
                                                )
                                            )
                                    )

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF141414))
                                            .navigationBarsPadding()
                                            .onGloballyPositioned { coordinates ->
                                                footerHeightPx = coordinates.size.height
                                            }
                                            .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 12.dp)
                                    ) {
                                        DischargingTestButton(
                                            onClick = {
                                                val intent = Intent(context, LockTriggerActivity::class.java).apply {
                                                    action = "com.typongtv.crtscreenoff.TRIGGER_LOCK"
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                context.startActivity(intent)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }

                        // ══════════════════════════════════════════════════════════════
                        // ── VERTICAL DIVIDER ──
                        // ══════════════════════════════════════════════════════════════
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(Color(0xFF262626))
                        )

                        // ══════════════════════════════════════════════════════════════
                        // ── RIGHT PANEL: FULL PARAMETER BAY WORKSTATION ──
                        // ══════════════════════════════════════════════════════════════
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(Color(0xFF101010))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                                    .navigationBarsPadding()
                                    .verticalScroll(rightScrollState)
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                if (!enableEnhancedOverdrive) {
                                    // ────────────────────────────────────────────────────────
                                    // ── MODE 1: CYANOGEN RETRO PARAMETER BAY ──
                                    // ────────────────────────────────────────────────────────
                                    CascadingEntrance(index = 1, visible = startCascade) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "Cyanogen Inspired",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 18.sp,
                                                    lineHeight = 22.sp,
                                                    color = if (!enableEnhancedOverdrive && enableCrtAnimation) MaterialTheme.colorScheme.primary else Color.White,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            Text(
                                                "CM7/CM9 classic CRT collapse with directional snap & CyanogenMod-inspired timing.",
                                                fontSize = 11.sp,
                                                lineHeight = 13.5.sp,
                                                color = Color(0xFF94A3B8),
                                                modifier = Modifier.padding(top = 1.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = Color(0xFF262626), thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Section 1: Direction Deck
                                    CascadingEntrance(index = 2, visible = startCascade) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF292929)),
                                            border = BorderStroke(1.dp, Color(0xFF3E3E3E))
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Text(
                                                    "Direction: ${if (selectedStyle == CrtStyle.CLASSIC_HORIZONTAL.name) "Horizontal (Classic)" else "Vertical"}",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    lineHeight = 18.sp,
                                                    color = Color(0xFF94A3B8),
                                                    modifier = Modifier.padding(bottom = 8.dp)
                                                )
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    FilterChip(
                                                        selected = selectedStyle == CrtStyle.CLASSIC_HORIZONTAL.name,
                                                        onClick = {
                                                            if (enableCrtAnimation) {
                                                                selectedStyle = CrtStyle.CLASSIC_HORIZONTAL.name
                                                                prefs.edit().putString("anim_style", selectedStyle).apply()
                                                            }
                                                        },
                                                        label = {
                                                            Text(
                                                                "Horizontal",
                                                                fontSize = 16.sp,
                                                                modifier = Modifier.fillMaxWidth(),
                                                                textAlign = TextAlign.Center
                                                            )
                                                        },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    FilterChip(
                                                        selected = selectedStyle == CrtStyle.CLASSIC_VERTICAL.name,
                                                        onClick = {
                                                            if (enableCrtAnimation) {
                                                                selectedStyle = CrtStyle.CLASSIC_VERTICAL.name
                                                                prefs.edit().putString("anim_style", selectedStyle).apply()
                                                            }
                                                        },
                                                        label = {
                                                            Text(
                                                                "Vertical",
                                                                fontSize = 16.sp,
                                                                modifier = Modifier.fillMaxWidth(),
                                                                textAlign = TextAlign.Center
                                                            )
                                                        },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Section 2: Animation Speed Slider
                                    CascadingEntrance(index = 3, visible = startCascade) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF292929)),
                                            border = BorderStroke(1.dp, Color(0xFF3E3E3E))
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Text(
                                                    "Animation Speed: ${speedMs.toInt()} ms",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    lineHeight = 18.sp,
                                                    color = Color(0xFF94A3B8)
                                                )
                                                Text(
                                                    "Duration of the vertical squeeze, incandescent flare crescendo, and horizontal zip shut.",
                                                    fontSize = 11.sp,
                                                    lineHeight = 13.5.sp,
                                                    color = Color(0xFF6B7280),
                                                    modifier = Modifier.padding(bottom = 2.dp)
                                                )

                                                Slider(
                                                    value = speedMs,
                                                    enabled = enableCrtAnimation,
                                                    onValueChange = { speedMs = it },
                                                    onValueChangeFinished = {
                                                        prefs.edit().putInt("anim_speed", speedMs.toInt()).apply()
                                                    },
                                                    valueRange = 400f..1200f,
                                                    steps = 7
                                                )

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("400 ms (Fast)", fontSize = 11.sp, color = Color(0xFF64748B))
                                                    Text("600 ms (Default)", fontSize = 11.sp, color = Color(0xFF64748B))
                                                    Text("1200 ms (Slow-mo)", fontSize = 11.sp, color = Color(0xFF64748B))
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Section 3: Deflection Dynamics Deck
                                    CascadingEntrance(index = 4, visible = startCascade) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF292929)),
                                            border = BorderStroke(1.dp, Color(0xFF3E3E3E))
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                val currentDynamics = try {
                                                    DeflectionDynamics.valueOf(selectedDeflectionDynamics)
                                                } catch (_: Exception) {
                                                    DeflectionDynamics.STYLIZED_CREEP
                                                }
                                                Text(
                                                    "Deflection Dynamics: ${currentDynamics.label}",
                                                    fontSize = 14.sp,
                                                    lineHeight = 18.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFFE2E8F0)
                                                )
                                                Text(
                                                    currentDynamics.subtitle,
                                                    fontSize = 11.sp,
                                                    lineHeight = 13.5.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(top = 1.dp, bottom = 6.dp)
                                                )

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    DeflectionDynamics.values().forEach { dynamics ->
                                                        FilterChip(
                                                            selected = selectedDeflectionDynamics == dynamics.name,
                                                            onClick = {
                                                                if (enableCrtAnimation) {
                                                                    selectedDeflectionDynamics = dynamics.name
                                                                    prefs.edit().putString("deflection_dynamics", dynamics.name).apply()
                                                                }
                                                            },
                                                            label = {
                                                                Text(
                                                                    dynamics.label,
                                                                    fontSize = 11.sp,
                                                                    maxLines = 1,
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    textAlign = TextAlign.Center
                                                                )
                                                            },
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Section 4 & 5: Architecture Spec Card & Restore Button
                                    CascadingEntrance(index = 5, visible = startCascade) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(14.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131D26)),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Text(
                                                        "CyanogenMod Retro Architecture Spec",
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 12.sp,
                                                        lineHeight = 15.sp,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        "Mode 1 executes the authentic 2011 CM7 ElectronBeam state machine: " +
                                                                "captures the active frame buffer without root, compresses with dynamic vertical aspect-ratio squish, " +
                                                                "excites an incandescent center beam flare, and collapses smoothly into complete pitch-black OLED standby.",
                                                        fontSize = 11.sp,
                                                        lineHeight = 14.sp,
                                                        color = Color(0xFFCBD5E1)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))

                                            // Section 5: Restore to Defaults Button
                                            Button(
                                                onClick = {
                                                    speedMs = 700f
                                                    selectedStyle = CrtStyle.CLASSIC_HORIZONTAL.name
                                                    selectedDeflectionDynamics = DeflectionDynamics.STYLIZED_CREEP.name
                                                    prefs.edit()
                                                        .putInt("anim_speed", 700)
                                                        .putString("anim_style", CrtStyle.CLASSIC_HORIZONTAL.name)
                                                        .putString("deflection_dynamics", DeflectionDynamics.STYLIZED_CREEP.name)
                                                        .apply()
                                                    Toast.makeText(context, "Cyanogen settings restored to default", Toast.LENGTH_SHORT).show()
                                                },
                                                enabled = enableCrtAnimation,
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E), contentColor = Color.White),
                                                border = BorderStroke(1.dp, Color(0xFF383838)),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth().height(44.dp)
                                            ) {
                                                Text("Restore to Default", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                } else {
                                    // ────────────────────────────────────────────────────────
                                    // ── MODE 2: ENHANCED PHOSPHOR OVERDRIVE PARAMETER BAY ──
                                    // ────────────────────────────────────────────────────────
                                    // Mode 2 Header in Right Panel
                                    CascadingEntrance(index = 1, visible = startCascade) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "Enhanced Phosphor Overdrive",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 18.sp,
                                                    lineHeight = 22.sp,
                                                    color = if (enableEnhancedOverdrive && enableCrtAnimation) MaterialTheme.colorScheme.primary else Color.White,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            Text(
                                                "RGB chromatic aberration, ease-in resistance & cathode afterglow.",
                                                fontSize = 11.sp,
                                                lineHeight = 13.5.sp,
                                                color = Color(0xFF94A3B8),
                                                modifier = Modifier.padding(top = 1.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = Color(0xFF262626), thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Sections 1 & 2: Collapse Direction Deck & Speed Slider
                                    CascadingEntrance(index = 2, visible = startCascade) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(14.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF292929)),
                                                border = BorderStroke(1.dp, Color(0xFF3E3E3E))
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Text(
                                                        "Direction: ${if (selectedStyle == CrtStyle.CLASSIC_HORIZONTAL.name) "Horizontal (Classic)" else "Vertical"}",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        lineHeight = 18.sp,
                                                        color = Color(0xFF94A3B8),
                                                        modifier = Modifier.padding(bottom = 8.dp)
                                                    )
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        FilterChip(
                                                            selected = selectedStyle == CrtStyle.CLASSIC_HORIZONTAL.name,
                                                            onClick = {
                                                                if (enableCrtAnimation) {
                                                                    selectedStyle = CrtStyle.CLASSIC_HORIZONTAL.name
                                                                    prefs.edit().putString("anim_style", selectedStyle).apply()
                                                                }
                                                            },
                                                            label = {
                                                                Text(
                                                                    "Horizontal",
                                                                    fontSize = 16.sp,
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    textAlign = TextAlign.Center
                                                                )
                                                            },
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        FilterChip(
                                                            selected = selectedStyle == CrtStyle.CLASSIC_VERTICAL.name,
                                                            onClick = {
                                                                if (enableCrtAnimation) {
                                                                    selectedStyle = CrtStyle.CLASSIC_VERTICAL.name
                                                                    prefs.edit().putString("anim_style", selectedStyle).apply()
                                                                }
                                                            },
                                                            label = {
                                                                Text(
                                                                    "Vertical",
                                                                    fontSize = 16.sp,
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    textAlign = TextAlign.Center
                                                                )
                                                            },
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(14.dp))

                                            // Section 2: Overdrive Collapse Speed Slider
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(14.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF292929)),
                                                border = BorderStroke(1.dp, Color(0xFF3E3E3E))
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Text(
                                                        "Overdrive Collapse Speed: ${speedOverdriveMs.toInt()} ms",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        lineHeight = 18.sp,
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                    Text(
                                                        "Duration of the active multi-line magnetic raster collapse before afterglow.",
                                                        fontSize = 11.sp,
                                                        lineHeight = 13.5.sp,
                                                        color = Color(0xFF6B7280),
                                                        modifier = Modifier.padding(bottom = 2.dp)
                                                    )
                                                    Slider(
                                                        value = speedOverdriveMs,
                                                        enabled = enableCrtAnimation,
                                                        onValueChange = { speedOverdriveMs = it },
                                                        onValueChangeFinished = {
                                                            prefs.edit().putInt("anim_speed_overdrive", speedOverdriveMs.toInt()).apply()
                                                        },
                                                        valueRange = 100f..1000f,
                                                        steps = 8
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Section 3: Magnetic Coil Resistance Deck (Harmonized below Collapse Speed)
                                    CascadingEntrance(index = 3, visible = startCascade) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF292929)),
                                            border = BorderStroke(1.dp, Color(0xFF3E3E3E))
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                val currentResistance = try {
                                                    CoilResistance.valueOf(selectedResistance)
                                                } catch (_: Exception) {
                                                    CoilResistance.EXTREME
                                                }
                                                Text(
                                                    "Magnetic Coil Resistance: ${currentResistance.label}",
                                                    fontSize = 14.sp,
                                                    lineHeight = 18.sp,
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
                                                                if (enableCrtAnimation) {
                                                                    selectedResistance = res.name
                                                                    prefs.edit().putString("coil_resistance", res.name).apply()
                                                                }
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
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Section 4: Side-by-side Flare & Bloom Overdrive + Cathode Afterglow Duration
                                    CascadingEntrance(index = 4, visible = startCascade) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            // Left card: Flare & Bloom Overdrive
                                            val intensityPercent = (flashIntensity * 100).toInt()
                                            val intensityTag = when {
                                                flashIntensity <= 0.05f -> "Off (0%)"
                                                flashIntensity <= 0.5f -> "Subtle ($intensityPercent%)"
                                                flashIntensity <= 1.0f -> "Dramatic ($intensityPercent%)"
                                                else -> "Supernova ($intensityPercent%)"
                                            }
                                            Card(
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(14.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF292929)),
                                                border = BorderStroke(1.dp, Color(0xFF3E3E3E))
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Text(
                                                        "Flare & Bloom Overdrive: $intensityTag",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        lineHeight = 18.sp,
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                    Text(
                                                        "Peak white filament discharge bloom intensity.",
                                                        fontSize = 11.sp,
                                                        lineHeight = 13.5.sp,
                                                        color = Color(0xFF6B7280),
                                                        modifier = Modifier.padding(bottom = 2.dp)
                                                    )
                                                    Slider(
                                                        value = flashIntensity,
                                                        enabled = enableCrtAnimation,
                                                        onValueChange = { flashIntensity = it },
                                                        onValueChangeFinished = {
                                                            prefs.edit().putFloat("flash_intensity", flashIntensity).apply()
                                                        },
                                                        valueRange = 0.0f..1.5f,
                                                        steps = 14
                                                    )
                                                }
                                            }

                                            // Right card: Cathode Afterglow Duration
                                            Card(
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(14.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF292929)),
                                                border = BorderStroke(1.dp, Color(0xFF3E3E3E))
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Text(
                                                        "Cathode Afterglow Duration: ${afterglowDurationMs.toInt()} ms",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        lineHeight = 18.sp,
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                    Text(
                                                        "Independent center dot persistence on pitch-black OLED display.",
                                                        fontSize = 11.sp,
                                                        lineHeight = 13.5.sp,
                                                        color = Color(0xFF6B7280),
                                                        modifier = Modifier.padding(bottom = 2.dp)
                                                    )
                                                    Slider(
                                                        value = afterglowDurationMs,
                                                        enabled = enableCrtAnimation,
                                                        onValueChange = { afterglowDurationMs = it },
                                                        onValueChangeFinished = {
                                                            prefs.edit().putInt("afterglow_duration", afterglowDurationMs.toInt()).apply()
                                                        },
                                                        valueRange = 100f..1500f,
                                                        steps = 13
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Sections 5 & 6: Hardware Optic Toggles + Restore Button
                                    CascadingEntrance(index = 5, visible = startCascade) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(14.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF292929)),
                                                border = BorderStroke(1.dp, Color(0xFF3E3E3E))
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    // Toggle 1: Magnetic Linear Stretch
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            "Magnetic Linear Stretch (Elastic Tension)",
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 14.sp,
                                                            lineHeight = 18.sp,
                                                            color = if (enableMagneticStretch) MaterialTheme.colorScheme.primary else Color.White,
                                                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                                                        )
                                                        Switch(
                                                            checked = enableMagneticStretch,
                                                            enabled = enableCrtAnimation,
                                                            onCheckedChange = {
                                                                enableMagneticStretch = it
                                                                prefs.edit().putBoolean("enable_magnetic_stretch", it).apply()
                                                            }
                                                        )
                                                    }
                                                    Text(
                                                        "Screen edges cling to the bezels while content pulls vertically into the center equator before snapping loose.",
                                                        fontSize = 11.sp,
                                                        lineHeight = 13.5.sp,
                                                        color = Color(0xFF94A3B8),
                                                        modifier = Modifier.padding(top = 1.dp)
                                                    )

                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    HorizontalDivider(color = Color(0xFF383838), thickness = 1.dp)
                                                    Spacer(modifier = Modifier.height(12.dp))

                                                    // Toggle 2: Anamorphic Flare
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            "Anamorphic Chromatic Flare (Prism Split)",
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 14.sp,
                                                            lineHeight = 18.sp,
                                                            color = if (enableAnamorphicFlare) MaterialTheme.colorScheme.primary else Color.White,
                                                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                                                        )
                                                        Switch(
                                                            checked = enableAnamorphicFlare,
                                                            enabled = enableCrtAnimation,
                                                            onCheckedChange = {
                                                                enableAnamorphicFlare = it
                                                                prefs.edit().putBoolean("enable_anamorphic_flare", it).apply()
                                                            }
                                                        )
                                                    }
                                                    Text(
                                                        "95% incandescent white beam + cyan prism fringe, dynamic center swell & cathode bridge.",
                                                        fontSize = 11.sp,
                                                        lineHeight = 13.5.sp,
                                                        color = Color(0xFF94A3B8),
                                                        modifier = Modifier.padding(top = 1.dp)
                                                    )

                                                    Spacer(modifier = Modifier.height(10.dp))

                                                    // AGSL GPU Optics Info Pill
                                                    Card(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                                    ) {
                                                        Text(
                                                            "Optics: Exaggerated Red/Cyan Vertical Split + Squish Blur + H-Sync Combing",
                                                            fontSize = 12.sp,
                                                            lineHeight = 15.sp,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            fontWeight = FontWeight.Medium,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))

                                            // Section 7: Restore to Defaults Button
                                            Button(
                                                onClick = {
                                                    speedOverdriveMs = 500f
                                                    selectedResistance = CoilResistance.HEAVY.name
                                                    enableMagneticStretch = false
                                                    enableAnamorphicFlare = true
                                                    flashIntensity = 1.0f
                                                    afterglowDurationMs = 800f
                                                    prefs.edit()
                                                        .putInt("anim_speed_overdrive", 500)
                                                        .putString("coil_resistance", CoilResistance.HEAVY.name)
                                                        .putBoolean("enable_magnetic_stretch", false)
                                                        .putBoolean("enable_anamorphic_flare", true)
                                                        .putFloat("flash_intensity", 1.0f)
                                                        .putInt("afterglow_duration", 800)
                                                        .apply()
                                                    Toast.makeText(context, "Overdrive settings restored to default", Toast.LENGTH_SHORT).show()
                                                },
                                                enabled = enableCrtAnimation,
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E), contentColor = Color.White),
                                                border = BorderStroke(1.dp, Color(0xFF383838)),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth().height(44.dp)
                                            ) {
                                                Text("Restore to Default", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }
                            }

                            // ── In-Place Subpages Overlay inside Right Panel ──
                            androidx.compose.animation.AnimatedVisibility(
                                visible = showAboutPage,
                                enter = fadeIn(tween(200)),
                                exit = fadeOut(tween(180)),
                                modifier = Modifier.fillMaxSize().zIndex(20f)
                            ) {
                                key(aboutTrigger) {
                                    AboutScreen(
                                        onBack = { showAboutPage = false },
                                        staggeredEntrance = true
                                    )
                                }
                            }

                            androidx.compose.animation.AnimatedVisibility(
                                visible = showAcknowledgmentPage,
                                enter = fadeIn(tween(200)),
                                exit = fadeOut(tween(180)),
                                modifier = Modifier.fillMaxSize().zIndex(20f)
                            ) {
                                key(acknowledgmentTrigger) {
                                    SpecialAcknowledgmentScreen(
                                        onBack = { showAcknowledgmentPage = false },
                                        staggeredEntrance = true
                                    )
                                }
                            }

                            // ── Right Panel Scrim for Capacitor Discharge Advisory ──
                            androidx.compose.animation.AnimatedVisibility(
                                visible = showAdvisoryBanner,
                                enter = fadeIn(animationSpec = tween(300)),
                                exit = fadeOut(animationSpec = tween(200)),
                                modifier = Modifier.fillMaxSize().zIndex(25f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.65f))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            // Tapping outside the banner does NOT dismiss it
                                        }
                                )
                            }

                            // ── Right Panel Slide-Down Capacitor Advisory Panel ──
                            androidx.compose.animation.AnimatedVisibility(
                                visible = showAdvisoryBanner,
                                enter = slideInVertically(
                                    initialOffsetY = { -it - 80 },
                                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(300)),
                                exit = slideOutVertically(
                                    targetOffsetY = { -it - 80 },
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(200)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
                                    .statusBarsPadding()
                                    .padding(horizontal = 24.dp, vertical = 16.dp)
                                    .zIndex(30f)
                            ) {
                                val dismissBanner = {
                                    prefs.edit().putBoolean("hide_anode_bleed_notice", dontShowAdvisoryAgain).apply()
                                    showAdvisoryBanner = false
                                }

                                val dragOffsetY = remember { Animatable(0f) }

                                LaunchedEffect(showAdvisoryBanner) {
                                    if (showAdvisoryBanner) {
                                        dragOffsetY.snapTo(0f)
                                    }
                                }

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer {
                                            translationY = dragOffsetY.value
                                        }
                                        .pointerInput(Unit) {
                                            val dismissThresholdPx = with(densityObj) { 64.dp.toPx() }
                                            detectVerticalDragGestures(
                                                onDragStart = { },
                                                onDragEnd = {
                                                    if (dragOffsetY.value < -dismissThresholdPx) {
                                                        dismissBanner()
                                                    } else {
                                                        coroutineScope.launch {
                                                            dragOffsetY.animateTo(
                                                                targetValue = 0f,
                                                                animationSpec = spring(
                                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                                    stiffness = Spring.StiffnessMediumLow
                                                                )
                                                            )
                                                        }
                                                    }
                                                },
                                                onDragCancel = {
                                                    coroutineScope.launch {
                                                        dragOffsetY.animateTo(
                                                            targetValue = 0f,
                                                            animationSpec = spring(Spring.DampingRatioMediumBouncy)
                                                        )
                                                    }
                                                },
                                                onVerticalDrag = { change, dragAmount ->
                                                    change.consume()
                                                    val current = dragOffsetY.value
                                                    val target = if (current + dragAmount > 0f) {
                                                        (current + dragAmount * 0.15f).coerceAtMost(16f)
                                                    } else {
                                                        current + dragAmount
                                                    }
                                                    coroutineScope.launch {
                                                        dragOffsetY.snapTo(target)
                                                    }
                                                }
                                            )
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF242424),
                                    border = BorderStroke(1.dp, Color(0xFF3E3E3E)),
                                    shadowElevation = 14.dp
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp)
                                    ) {
                                        Text(
                                            text = "CAPACITOR DISCHARGE ADVISORY :P",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp,
                                            color = Color(0xFFBE6060),
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )

                                        Text(
                                            text = "Notice the Test Button twitching and white scanlines arcing across your screen? Don’t panic—your display is 100% safe.\n\nThat button is wired directly in parallel with the main flyback transformer and is essentially sweating 30 kilovolts of pure ungrounded terror, waiting for your finger to close the circuit and discharge the beast.",
                                            fontSize = 12.5.sp,
                                            lineHeight = 17.5.sp,
                                            color = Color(0xFFCBD5E1),
                                            modifier = Modifier.padding(bottom = 10.dp)
                                        )

                                        Text(
                                            text = buildAnnotatedString {
                                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFFDF9595))) {
                                                    append("⚠️ Surface Charge Warning: ")
                                                }
                                                append("Touching the glass triggers mild electrostatic discharges. The deflection yoke is currently mistaking your finger for an ungrounded screwdriver, bleeding high-voltage static slightly beyond OSHA tolerances.")
                                            },
                                            fontSize = 12.sp,
                                            lineHeight = 16.5.sp,
                                            color = Color(0xFFD68E8E),
                                            modifier = Modifier.padding(bottom = 14.dp)
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        val updated = !dontShowAdvisoryAgain
                                                        dontShowAdvisoryAgain = updated
                                                        prefs.edit().putBoolean("hide_anode_bleed_notice", updated).apply()
                                                    }
                                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .border(1.5.dp, if (dontShowAdvisoryAgain) MaterialTheme.colorScheme.primary else Color(0xFF64748B), CircleShape)
                                                        .background(if (dontShowAdvisoryAgain) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (dontShowAdvisoryAgain) {
                                                        Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(7.dp))
                                                Text(text = "Don't show this again", fontSize = 12.sp, color = Color(0xFFCBD5E1))
                                            }

                                            Button(
                                                onClick = { dismissBanner() },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier.fillMaxHeight(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "Understood",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        textAlign = TextAlign.Center,
                                                        style = LocalTextStyle.current.copy(
                                                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                                                includeFontPadding = false
                                                            )
                                                        )
                                                    )
                                                }
                                            }
                                        }

                                        // Subtle swipe-up hint pill at the bottom
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterHorizontally)
                                                .padding(top = 10.dp)
                                                .size(width = 36.dp, height = 3.dp)
                                                .background(Color(0xFF4A4A4A), RoundedCornerShape(2.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Vintage electrifying CRT intro splash with cathode strike, glitch & bloom
                AnimatedVisibility(
                    visible = showSplash,
                    enter = EnterTransition.None,
                    exit = fadeOut(animationSpec = tween(350)),
                    modifier = Modifier.zIndex(50f)
                ) {
                    CrtSplashScreen(
                        onStartCascade = {
                            MainActivity.hasShownSplash = true
                            startCascade = true
                        },
                        onDismiss = {
                            MainActivity.hasShownSplash = true
                            showSplash = false
                        }
                    )
                }
            }
        }
    }
}
