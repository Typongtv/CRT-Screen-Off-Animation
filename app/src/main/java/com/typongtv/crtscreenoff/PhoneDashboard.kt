package com.typongtv.crtscreenoff

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
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
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
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
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Checks whether the CRT Screen-Off accessibility service is actively granted and enabled.
 */
fun isAccessibilityServiceEnabled(context: Context): Boolean {
    if (CrtAccessibilityService.instance != null) return true
    val enabledServicesSetting = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: ""
    if (enabledServicesSetting.contains(context.packageName)) return true
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    return enabledServices.any { it.resolveInfo.serviceInfo.packageName == context.packageName }
}

/**
 * Isolated, dedicated phone single-column UI for CRT Screen-Off Engine.
 * Encapsulates the entire mobile layout, cascading animations, subpage overlays,
 * and exit gestures.
 */
@Composable
fun PhoneDashboard(
    prefs: SharedPreferences,
    onMoveTaskToBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var showSplash by rememberSaveable { mutableStateOf(!MainActivity.hasShownSplash) }
    var startCascade by rememberSaveable { mutableStateOf(MainActivity.hasShownSplash) }

    // Secondary residual flyback discharge after cards cascade in (2 flickers -> 1.0s rest -> delayed long discharge)
    val dischargeAnim = remember { Animatable(0f) }
    LaunchedEffect(startCascade) {
        if (startCascade) {
            // Wait for all 6 items to complete their cascade ascent (5 * 95ms + 650ms ≈ 1125ms)
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

    val dischargeShader = remember {
        try {
            RuntimeShader(MainActivity.DISCHARGE_AGSL)
        } catch (e: Throwable) {
            null
        }
    }

    val headerShader = remember {
        try {
            RuntimeShader(MainActivity.DISCHARGE_AGSL)
        } catch (e: Throwable) {
            null
        }
    }

    val headerGlitchAnim = remember { Animatable(0f) }

    // Organic randomized chromatic aberration split loop for the fixed header title
    LaunchedEffect(Unit) {
        while (isActive) {
            // Quiet dwell time between 2.8s and 5.2s
            delay(Random.nextLong(2800L, 5200L))

            when (Random.nextInt(3)) {
                0 -> {
                    // Twin-pulse convergence twitch: sharp split -> brief rest -> softer echo
                    headerGlitchAnim.animateTo(0.95f, tween(36, easing = LinearEasing))
                    headerGlitchAnim.animateTo(0.12f, tween(75, easing = FastOutSlowInEasing))
                    headerGlitchAnim.animateTo(0.65f, tween(32, easing = LinearEasing))
                    headerGlitchAnim.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
                }
                1 -> {
                    // Analog beam drift & snap: slow horizontal creep, then snaps sharp
                    headerGlitchAnim.animateTo(0.85f, tween(130, easing = FastOutSlowInEasing))
                    headerGlitchAnim.animateTo(0f, tween(40, easing = LinearEasing))
                }
                2 -> {
                    // Deflection harmonic ripple: 3 rapid micro-flickers
                    repeat(3) {
                        headerGlitchAnim.animateTo(0.70f, tween(26, easing = LinearEasing))
                        headerGlitchAnim.animateTo(0f, tween(45, easing = LinearEasing))
                        delay(20L)
                    }
                }
            }
        }
    }

    // Capacitive breakdown scanline arc state (subtle random high-voltage traces across the viewport)
    var activeArcLines by remember { mutableStateOf<List<CapacitorArcLine>>(emptyList()) }
    val arcAlphaAnim = remember { Animatable(0f) }

    LaunchedEffect(startCascade) {
        if (!startCascade) return@LaunchedEffect
        while (isActive) {
            // Random pause between 2.2s and 3.8s between discharge events
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
                        CapacitorArcLine(
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
                        CapacitorArcLine(
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
                        CapacitorArcLine(
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
                    val lineA = CapacitorArcLine(
                        yRatio = baseCenterY,
                        startXRatio = 0f,
                        endXRatio = 1f,
                        strokeWidthDp = 1.3f
                    )
                    val lineB = CapacitorArcLine(
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

    val scrollState = rememberScrollState()
    var headerHeightPx by remember { mutableIntStateOf(0) }
    var footerHeightPx by remember { mutableIntStateOf(0) }
    var enableCrtAnimation by remember {
        mutableStateOf(prefs.getBoolean("enable_crt_animation", true))
    }
    var speedMs by remember { mutableFloatStateOf(prefs.getInt("anim_speed", 700).toFloat().coerceIn(400f, 1200f)) }
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
        mutableFloatStateOf(prefs.getFloat("flash_intensity", 1.0f))
    }
    var afterglowDurationMs by remember {
        mutableFloatStateOf(prefs.getInt("afterglow_duration", 800).toFloat())
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
        mutableFloatStateOf(prefs.getInt("anim_speed_overdrive", 500).toFloat().coerceIn(100f, 1000f))
    }

    var hasOverlay by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var isA11yEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var expandedCard1 by remember { mutableStateOf(!isA11yEnabled) }

    // Immediate resume detection: updates permission states when returning from system settings
    val lifecycleOwner = LocalLifecycleOwner.current
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
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Continuous polling fallback while awaiting first-time activation
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

    // Card 2 master collapsible state & 3 sub-card states (initially compact/collapsed)
    var expandedCard2 by remember { mutableStateOf(false) }
    var expandedOptionA by remember { mutableStateOf(false) }
    var expandedOptionB by remember { mutableStateOf(false) }
    var expandedOptionC by remember { mutableStateOf(false) }

    var showAboutPage by remember { mutableStateOf(false) }
    var showAcknowledgmentPage by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    // Geeky Capacitor Discharge Advisory Banner state
    val hideAdvisoryPref = prefs.getBoolean("hide_anode_bleed_notice", false)
    var showAdvisoryBanner by remember { mutableStateOf(false) }
    var dontShowAdvisoryAgain by remember { mutableStateOf(prefs.getBoolean("hide_anode_bleed_notice", false)) }

    // Slide down banner from behind the header after staggered cards settle
    LaunchedEffect(startCascade) {
        if (startCascade && !hideAdvisoryPref) {
            delay(900L)
            showAdvisoryBanner = true
        }
    }

    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    val coroutineScope = rememberCoroutineScope()
    var exitPromptJob by remember { mutableStateOf<Job?>(null) }
    var showExitPrompt by remember { mutableStateOf(false) }
    var isExiting by remember { mutableStateOf(false) }

    // Reset exit prompt if user navigates away or backgrounds app
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                showExitPrompt = false
                exitPromptJob?.cancel()
                isExiting = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // System back navigation handler: dismisses subpages or moves task to back on double-tap with animated header panel
    BackHandler(enabled = true) {
        if (showSplash || isExiting) {
            return@BackHandler
        }
        if (showAcknowledgmentPage) {
            showAcknowledgmentPage = false
        } else if (showAboutPage) {
            showAboutPage = false
        } else if (menuExpanded) {
            menuExpanded = false
        } else {
            val currentTime = System.currentTimeMillis()
            if (showExitPrompt && (currentTime - lastBackPressTime < 3000L)) {
                // Double tap: immediately slide up & hide the exit card before exiting so cards return to rest positions
                isExiting = true
                showExitPrompt = false
                exitPromptJob?.cancel()
                coroutineScope.launch {
                    delay(280L) // Settle slide-up animation so cards return to rest positions
                    onMoveTaskToBack()
                    isExiting = false
                }
            } else {
                // First tap: trigger the elegant slide-down exit panel
                lastBackPressTime = currentTime
                showExitPrompt = true
                if (scrollState.value > 0) {
                    coroutineScope.launch {
                        scrollState.animateScrollTo(0)
                    }
                }
                exitPromptJob?.cancel()
                exitPromptJob = coroutineScope.launch {
                    delay(3000L) // Sits for a brief 3 seconds
                    showExitPrompt = false // Slides back up elegantly
                }
            }
        }
    }

    // Re-check permissions when returning to activity
    DisposableEffect(Unit) {
        val callback = {
            hasOverlay = Settings.canDrawOverlays(context)
            val a11y = isAccessibilityServiceEnabled(context)
            if (a11y != isA11yEnabled) {
                isA11yEnabled = a11y
                if (a11y) {
                    expandedCard1 = false // Auto-collapse once granted
                }
            }
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
                val dischargeValue = dischargeAnim.value
                val densityObj = LocalDensity.current
                val density = densityObj.density
                val headerHeightDp = with(densityObj) { headerHeightPx.toDp() }
                val footerHeightDp = with(densityObj) { footerHeightPx.toDp() }

                val dischargeShakeX = if (dischargeValue > 0.001f) {
                    sin(dischargeValue * 28f) * 7f * dischargeValue * density
                } else 0f
                val dischargeShakeY = if (dischargeValue > 0.001f) {
                    cos(dischargeValue * 36f) * 3f * dischargeValue * density
                } else 0f

                val dischargeRenderEffect = remember(dischargeValue) {
                    if (dischargeValue > 0.001f && dischargeShader != null) {
                        try {
                            dischargeShader.setFloatUniform("chromaticOffset", dischargeValue * 14f * density)
                            RenderEffect.createRuntimeShaderEffect(dischargeShader, "image").asComposeRenderEffect()
                        } catch (e: Throwable) {
                            null
                        }
                    } else {
                        null
                    }
                }

                // Header title chromatic split math & RenderEffect
                val glitchVal = headerGlitchAnim.value
                val headerChromaticOffset = glitchVal * 10f * density
                val headerShakeX = if (glitchVal > 0.01f) sin(glitchVal * 20f) * 1.6f * density * glitchVal else 0f
                val headerRenderEffect = remember(glitchVal) {
                    if (glitchVal > 0.005f && headerShader != null) {
                        try {
                            headerShader.setFloatUniform("chromaticOffset", headerChromaticOffset)
                            RenderEffect.createRuntimeShaderEffect(headerShader, "image").asComposeRenderEffect()
                        } catch (e: Throwable) {
                            null
                        }
                    } else {
                        null
                    }
                }

                // Dashboard container: unified deflection coil shake, chromatic RenderEffect & subtle touch static
                CrtTouchStaticContainer(
                    subtleMode = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = dischargeShakeX
                            translationY = dischargeShakeY
                            renderEffect = dischargeRenderEffect
                        }
                ) {
                    val exitCardClearancePx = with(densityObj) { 80.dp.roundToPx() }

                    // ── 1. Scrollable Dashboard Content (Cards & Test Button scroll behind header) ──
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(bottom = 24.dp)
                            .verticalScroll(scrollState),
                        horizontalAlignment = Alignment.Start
                    ) {
                        // Spacer clearing the fixed header + 39.dp clearance gap (14.dp base + 25.dp downward offset)
                        Spacer(modifier = Modifier.height(headerHeightDp.coerceAtLeast(75.dp) + 39.dp))

                        // ── Dashboard Cards Container with 20.dp horizontal padding ──
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            // ── Slide-Down Exit Prompt Card (Matches dashboard cards width & 16.dp corner radius) ──
                            AnimatedVisibility(
                                visible = showExitPrompt,
                                enter = slideInVertically(
                                    initialOffsetY = { -it - exitCardClearancePx },
                                    animationSpec = tween(450, easing = CubicBezierEasing(0.08f, 0.85f, 0.15f, 1.0f))
                                ) + expandVertically(
                                    expandFrom = Alignment.Top,
                                    animationSpec = tween(450, easing = CubicBezierEasing(0.08f, 0.85f, 0.15f, 1.0f))
                                ),
                                exit = slideOutVertically(
                                    targetOffsetY = { -it - exitCardClearancePx },
                                    animationSpec = tween(280, easing = CubicBezierEasing(0.35f, 0.0f, 0.15f, 1.0f))
                                ) + shrinkVertically(
                                    shrinkTowards = Alignment.Top,
                                    animationSpec = tween(280, easing = CubicBezierEasing(0.35f, 0.0f, 0.15f, 1.0f))
                                )
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 14.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFAD4647)
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFFC25253))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 14.dp, horizontal = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            LogoutIcon(
                                                tint = Color(0xFFFFFFFF),
                                                rotation = 180f,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Press back again to exit",
                                                color = Color(0xFFFFFFFF),
                                                fontSize = 18.sp,
                                                lineHeight = 22.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                textAlign = TextAlign.Center,
                                                letterSpacing = 0.3.sp
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

                            // ── Card 3: Dual Engine Deck ──
                            CascadingEntrance(index = 3, visible = startCascade) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 14.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                            if (enableCrtAnimation)
                                                "Select the active CRT physics engine to configure below:"
                                            else
                                                "CRT screen-off animation is disabled. Screen will sleep with native display lock.",
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

                            // ── Card 4: Cyanogen Inspired ──
                            CascadingEntrance(index = 4, visible = startCascade) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 14.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (!enableEnhancedOverdrive && enableCrtAnimation) Color(0xFF1E2836) else MaterialTheme.colorScheme.surface
                                    ),
                                    border = if (!enableEnhancedOverdrive && enableCrtAnimation) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
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
                                            modifier = Modifier.padding(top = 1.dp, bottom = 4.dp)
                                        )

                                        if (enableEnhancedOverdrive) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                                                border = BorderStroke(1.dp, Color(0xFF333333))
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text(
                                                        "Phosphor Overdrive CRT Engine Active",
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                    Text(
                                                        "Multi-line magnetic raster bloom, chromatic flare prism split, inductive coil resistance & afterglow persistence.",
                                                        fontSize = 11.sp,
                                                        lineHeight = 13.sp,
                                                        color = Color(0xFF6B7280),
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    )
                                                }
                                            }
                                        } else {
                                            Column(modifier = Modifier.alpha(if (enableCrtAnimation) 1f else 0.38f)) {
                                                Text(
                                                    "Direction: ${if (selectedStyle == CrtStyle.CLASSIC_HORIZONTAL.name) "Horizontal (Classic)" else "Vertical"}",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    lineHeight = 18.sp,
                                                    color = Color(0xFF94A3B8),
                                                    modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
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

                                                Spacer(modifier = Modifier.height(10.dp))

                                                Text(
                                                    "Animation Speed: ${speedMs.toInt()} ms",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    lineHeight = 18.sp,
                                                    color = Color(0xFF94A3B8)
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

                                                Spacer(modifier = Modifier.height(10.dp))

                                                // Deflection Dynamics (Classic Standard vs Stylized Creep)
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
                                                                    fontSize = 12.sp,
                                                                    maxLines = 1,
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    textAlign = TextAlign.Center
                                                                )
                                                            },
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(10.dp))

                                                // Restore to Default button for Card 4
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
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color(0xFF1E1E1E),
                                                        contentColor = Color.White
                                                    ),
                                                    border = BorderStroke(1.dp, Color(0xFF383838)),
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(44.dp)
                                                ) {
                                                    Text(
                                                        "Restore to Default",
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // ── Card 5: Enhanced Phosphor Overdrive ──
                            CascadingEntrance(index = 5, visible = startCascade) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 14.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (enableEnhancedOverdrive && enableCrtAnimation) Color(0xFF1E2836) else MaterialTheme.colorScheme.surface
                                    ),
                                    border = if (enableEnhancedOverdrive && enableCrtAnimation) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
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
                                                        "CyanogenMod Retro CRT Engine Active",
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                    Text(
                                                        "CM7/CM9 ElectronBeam inspired: dynamic vertical squish, true-color prism split, delayed crescendo flare & clean horizontal zip shut.",
                                                        fontSize = 11.sp,
                                                        lineHeight = 13.sp,
                                                        color = Color(0xFF6B7280),
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    )
                                                }
                                            }
                                        } else {
                                            Column(modifier = Modifier.alpha(if (enableCrtAnimation) 1f else 0.38f)) {
                                                Spacer(modifier = Modifier.height(10.dp))

                                                // Dedicated Overdrive Animation Speed Slider
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

                                                Spacer(modifier = Modifier.height(10.dp))

                                                // 3 Presets Magnetic Coil Resistance Selector
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

                                                Spacer(modifier = Modifier.height(8.dp))

                                                // Magnetic Linear Stretch (Elastic Tension) Toggle
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

                                                // ── Separator 1: Deflection Mechanics → Beam Optics ──
                                                Spacer(modifier = Modifier.height(10.dp))
                                                HorizontalDivider(color = Color(0xFF475569), thickness = 1.2.dp)
                                                Spacer(modifier = Modifier.height(10.dp))

                                                // Anamorphic Plasma Flare (Cyan & White) Toggle
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

                                                Spacer(modifier = Modifier.height(8.dp))

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
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    lineHeight = 18.sp,
                                                    color = Color(0xFF94A3B8)
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

                                                // ── Separator 2: Beam Optics → Cathode Afterglow ──
                                                Spacer(modifier = Modifier.height(10.dp))
                                                HorizontalDivider(color = Color(0xFF475569), thickness = 1.2.dp)
                                                Spacer(modifier = Modifier.height(10.dp))

                                                // Afterglow Duration Slider
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

                                                // ── Separator 3: CRT Settings → Reset Action ──
                                                Spacer(modifier = Modifier.height(12.dp))
                                                HorizontalDivider(color = Color(0xFF475569), thickness = 1.2.dp)
                                                Spacer(modifier = Modifier.height(12.dp))

                                                // Restore to Default button for Card 5
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
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color(0xFF1E1E1E),
                                                        contentColor = Color.White
                                                    ),
                                                    border = BorderStroke(1.dp, Color(0xFF383838)),
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(44.dp)
                                                ) {
                                                    Text(
                                                        "Restore to Default",
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Clearance spacer so the lowest card can scroll completely clear of the fixed bottom footer
                        Spacer(modifier = Modifier.height(footerHeightDp.coerceAtLeast(86.dp)))
                    }

                    // ── 2. Fixed Stationary Header (Staggers in at index 0, then remains stationary) ──
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
                                        .background(Color(0xFF121212))
                                        .statusBarsPadding()
                                        .zIndex(2f)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 16.dp, start = 20.dp, end = 20.dp, bottom = 14.dp)
                                            .onGloballyPositioned { coordinates ->
                                                headerHeightPx = coordinates.size.height
                                            }
                                    ) {
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
                                                    color = Color(0xFFE2E8F0),
                                                    modifier = Modifier
                                                        .padding(bottom = 2.dp)
                                                        .graphicsLayer {
                                                            translationX = headerShakeX
                                                            renderEffect = headerRenderEffect
                                                        }
                                                )
                                                Text(
                                                    text = "Cyanogen-inspired electron-beam animation without root.",
                                                    fontSize = 12.5.sp,
                                                    lineHeight = 15.sp,
                                                    color = Color(0xFF94A3B8)
                                                )
                                            }

                                            // 3-dot overflow menu
                                            Box {
                                                IconButton(
                                                    onClick = { menuExpanded = true },
                                                    enabled = !showAboutPage && !showAcknowledgmentPage,
                                                    modifier = Modifier.size(38.dp)
                                                ) {
                                                    MoreVertIcon(
                                                        tint = Color(0xFFE2E8F0)
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
                                                                "Capacitor Advisory",
                                                                color = Color.White,
                                                                fontSize = 14.sp,
                                                                fontWeight = FontWeight.Medium
                                                            )
                                                        },
                                                        leadingIcon = {
                                                            AdvisoryIcon(
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        },
                                                        onClick = {
                                                            menuExpanded = false
                                                            dontShowAdvisoryAgain = prefs.getBoolean("hide_anode_bleed_notice", false)
                                                            showAdvisoryBanner = true
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text(
                                                                "Special Acknowledgment",
                                                                color = Color.White,
                                                                fontSize = 14.sp,
                                                                fontWeight = FontWeight.Medium
                                                            )
                                                        },
                                                        leadingIcon = {
                                                            SparkleIcon(
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        },
                                                        onClick = {
                                                            menuExpanded = false
                                                            showAboutPage = false
                                                            showAcknowledgmentPage = true
                                                        }
                                                    )
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
                                                            showAcknowledgmentPage = false
                                                            showAboutPage = true
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Dynamic gradient feather when content scrolls behind the fixed header
                                val featherAlpha = (scrollState.value / 35f).coerceIn(0f, 1f)
                                if (featherAlpha > 0.01f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(14.dp)
                                            .graphicsLayer { alpha = featherAlpha }
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color(0xFF121212),
                                                        Color(0xFF121212).copy(alpha = 0.5f),
                                                        Color.Transparent
                                                    )
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }

                    // Dimming scrim for the advisory banner (dims out background so the banner pops)
                    AnimatedVisibility(
                        visible = showAdvisoryBanner,
                        enter = fadeIn(animationSpec = tween(320)),
                        exit = fadeOut(animationSpec = tween(220)),
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(7f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.62f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    // Tapping outside the banner does NOT dismiss it
                                }
                        )
                    }

                    // ── 2.5 Floating Capacitor Discharge Advisory Banner ──
                    // Sits full screen width, behind the fixed header (zIndex 8f vs header 10f), sliding down OVER the cards (cards at 0f)
                    val bannerTopClearance = (headerHeightDp + 26.dp).coerceAtLeast(106.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = bannerTopClearance)
                            .zIndex(8f)
                    ) {
                        AnimatedVisibility(
                            visible = showAdvisoryBanner,
                            enter = slideInVertically(
                                initialOffsetY = { -it - headerHeightPx - 120 },
                                animationSpec = tween(420, easing = FastOutSlowInEasing)
                            ),
                            exit = slideOutVertically(
                                targetOffsetY = { -it - headerHeightPx - 120 },
                                animationSpec = tween(420, easing = FastOutSlowInEasing)
                            ),
                            modifier = Modifier.fillMaxWidth()
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
                                                    // Trigger single smooth slide-out transition without hesitation
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
                                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                                color = Color(0xFF292929),
                                border = BorderStroke(1.dp, Color(0xFF3E3E3E)),
                                shadowElevation = 8.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp)
                                ) {
                                    // Label - Single line, #be6060, no icon, restored ':P' emoticon on the right
                                    Text(
                                        text = "CAPACITOR DISCHARGE ADVISORY :P",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                        color = Color(0xFFBE6060),
                                        maxLines = 1,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    // Body Text
                                    Text(
                                        text = "Notice the Test Button twitching and white scanlines arcing across your screen? Don’t panic—your display is 100% safe.\n\nThat button is wired directly in parallel with the main flyback transformer and is essentially sweating 30 kilovolts of pure ungrounded terror, waiting for your finger to close the circuit and discharge the beast. The sporadic scanline micro-arcs are simply simulated AnodeBleed flux leaking across the raster buffer while it holds back the collapse.",
                                        fontSize = 12.5.sp,
                                        lineHeight = 17.5.sp,
                                        color = Color(0xFFCBD5E1),
                                        modifier = Modifier.padding(bottom = 10.dp)
                                    )

                                    // Surface Charge Warning Callout
                                    Text(
                                        text = buildAnnotatedString {
                                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFFDF9595))) {
                                                append("⚠️ Surface Charge Warning: ")
                                            }
                                            append("Touching the glass triggers mild electrostatic discharges. The deflection yoke is currently mistaking your finger for an ungrounded screwdriver, bleeding high-voltage static slightly beyond OSHA tolerances and ionizing your fingerprint ridges on contact. It tickles, doesn't it?")
                                        },
                                        fontSize = 12.sp,
                                        lineHeight = 16.5.sp,
                                        color = Color(0xFFD68E8E),
                                        modifier = Modifier.padding(bottom = 14.dp)
                                    )

                                    // Single Bottom Action Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Don't show this again circular check toggle
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
                                                    .border(
                                                        width = 1.5.dp,
                                                        color = if (dontShowAdvisoryAgain) MaterialTheme.colorScheme.primary else Color(0xFF64748B),
                                                        shape = CircleShape
                                                    )
                                                    .background(
                                                        color = if (dontShowAdvisoryAgain) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent,
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (dontShowAdvisoryAgain) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(7.dp))
                                            Text(
                                                text = "Don't show this again",
                                                fontSize = 12.sp,
                                                color = if (dontShowAdvisoryAgain) Color(0xFFE2E8F0) else Color(0xFF94A3B8)
                                            )
                                        }

                                        // Dismiss Button in app's main blue accent color
                                        Button(
                                            onClick = { dismissBanner() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = Color.Black
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxHeight(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Understood",
                                                    fontSize = 12.5.sp,
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
                                            .size(width = 32.dp, height = 3.dp)
                                            .background(Color(0xFF4A4A4A), RoundedCornerShape(2.dp))
                                    )
                                }
                            }
                        }
                    }

                    // ── 3. Fixed Stationary Footer (Houses Test CRT Screen-Off button with upward shadow) ──
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .zIndex(10f)
                    ) {
                        CascadingEntrance(index = 5, visible = startCascade) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Upward gradient feather / shadow as cards scroll behind the fixed footer
                                val canScrollForward = scrollState.canScrollForward
                                val upwardShadowAlpha = if (canScrollForward) 1f else 0.45f
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(20.dp)
                                        .graphicsLayer { alpha = upwardShadowAlpha }
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color.Transparent,
                                                    Color(0xFF121212).copy(alpha = 0.55f),
                                                    Color(0xFF121212)
                                                )
                                            )
                                        )
                                    )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF121212))
                                        .navigationBarsPadding()
                                        .onGloballyPositioned { coordinates ->
                                            footerHeightPx = coordinates.size.height
                                        }
                                        .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp)
                                ) {
                                    DischargingTestButton(
                                        onClick = {
                                            val intent = Intent(context, LockTriggerActivity::class.java).apply {
                                                action = "com.typongtv.crtscreenoff.TRIGGER_LOCK"
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                // Residual discharge electric scanline & luma twitch overlay
                if (dischargeValue > 0.001f) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(15f)
                            .graphicsLayer { alpha = dischargeValue * 0.22f }
                    ) {
                        drawRect(Color(0xFF00E5FF).copy(alpha = 0.12f))
                        val tearY = size.height * 0.45f
                        drawLine(
                            color = Color.White.copy(alpha = 0.65f),
                            start = Offset(0f, tearY),
                            end = Offset(size.width, tearY),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }

                // Capacitive breakdown scanline arc overlay (subtle intermittent high-voltage tears across the whole viewport)
                if (activeArcLines.isNotEmpty() && arcAlphaAnim.value > 0.005f) {
                    val currentAlpha = arcAlphaAnim.value
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(18f)
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

                // About Page Overlay
                AnimatedVisibility(
                    visible = showAboutPage,
                    enter = slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(260, easing = LinearEasing)
                    ) + fadeIn(animationSpec = tween(140, easing = LinearEasing)),
                    exit = slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(240, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(180)),
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(20f)
                ) {
                    AboutScreen(onBack = { showAboutPage = false })
                }

                // Special Acknowledgment Page Overlay
                AnimatedVisibility(
                    visible = showAcknowledgmentPage,
                    enter = slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(260, easing = LinearEasing)
                    ) + fadeIn(animationSpec = tween(140, easing = LinearEasing)),
                    exit = slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(240, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(180)),
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(20f)
                ) {
                    SpecialAcknowledgmentScreen(onBack = { showAcknowledgmentPage = false })
                }

                // Vintage electrifying CRT intro splash with cathode strike, glitch & bloom
                AnimatedVisibility(
                    visible = showSplash,
                    enter = EnterTransition.None,
                    exit = fadeOut(animationSpec = tween(350)),
                    modifier = Modifier.zIndex(30f)
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

/**
 * Staggered cascading entrance wrapper that smoothly slides items upwards
 * from deep beneath the viewport with 3D plane tilt, non-linear alpha bloom,
 * and decelerating magnetic physics.
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
            delay(index * 95L)
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 650,
                    easing = CubicBezierEasing(0.08f, 0.85f, 0.15f, 1.0f)
                )
            )
        }
    }

    val p = animProgress.value
    val density = LocalDensity.current.density

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                // Decoupled non-linear alpha bloom: stays hidden at launch, then blooms in smoothly
                alpha = ((p / 0.65f).coerceIn(0f, 1f)).pow(1.6f)

                // Snappy vintage travel distance (280dp)
                translationY = (1f - p) * 280f * density

                // Z-axis scale elevation (0.92 -> 1.0)
                val scale = 0.92f + (p * 0.08f)
                scaleX = scale
                scaleY = scale

                // Subtle 3D perspective tilt
                cameraDistance = 16f * density
                rotationX = (1f - p) * 8.0f
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

/**
 * Vintage Overloaded Cathode Trigger Button.
 *
 * Sits in an obsidian gunmetal chassis with crisp white typography.
 * Runs the exact same AGSL chromatic glitch shader & deflection shudder as the dashboard,
 * but driven by an organic coroutine loop with randomized intervals and variable discharge
 * profiles (micro-twitch, twin-arc breakdown, delayed sputtering capacitive drain) so it
 * never plays the exact same mechanical loop.
 */
@Composable
fun DischargingTestButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape? = null
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val buttonShape = shape ?: if (isTablet) RoundedCornerShape(10.dp) else CircleShape

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val density = LocalDensity.current.density
    val dischargeShader = remember {
        try {
            RuntimeShader(MainActivity.DISCHARGE_AGSL)
        } catch (e: Throwable) {
            null
        }
    }

    val dischargeAnim = remember { Animatable(0f) }

    // Organic randomized discharge loop
    LaunchedEffect(Unit) {
        while (isActive) {
            // Unpredictable quiet rest between 2.2s and 4.6s
            val restDuration = Random.nextLong(2200L, 4600L)
            delay(restDuration)

            // Randomize the personality of this specific discharge event
            val eventType = Random.nextInt(3) // 0: Micro-twitch, 1: Twin-arc strike, 2: Deep lazy buzz
            val intensity = Random.nextFloat() * 0.45f + 0.55f // 0.55x .. 1.0x

            when (eventType) {
                0 -> {
                    // Quick sharp micro-glitch
                    dischargeAnim.animateTo(intensity * 0.70f, tween(30, easing = LinearEasing))
                    dischargeAnim.animateTo(0f, tween(160, easing = FastOutSlowInEasing))
                }
                1 -> {
                    // Classic twin-arc breakdown
                    dischargeAnim.animateTo(intensity, tween(35, easing = LinearEasing))
                    dischargeAnim.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
                    delay(Random.nextLong(60L, 100L))
                    dischargeAnim.animateTo(intensity * 0.60f, tween(30, easing = LinearEasing))
                    dischargeAnim.animateTo(0f, tween(210, easing = FastOutSlowInEasing))
                }
                2 -> {
                    // Deep, sputtering capacitive drain
                    dischargeAnim.animateTo(intensity * 0.90f, tween(50, easing = LinearEasing))
                    dischargeAnim.animateTo(0f, tween(380, easing = FastOutSlowInEasing))
                }
            }
        }
    }

    val dischargeIntensity = if (isPressed) 1.0f else dischargeAnim.value

    // Exact same deflection coil micro-shake math as dashboard
    val shakeX = if (dischargeIntensity > 0.001f) {
        sin(dischargeIntensity * 28f) * 7f * dischargeIntensity * density
    } else 0f
    val shakeY = if (dischargeIntensity > 0.001f) {
        cos(dischargeIntensity * 36f) * 3f * dischargeIntensity * density
    } else 0f

    // Exact same AGSL chromatic aberration RenderEffect as dashboard
    val renderEffect = remember(dischargeIntensity) {
        if (dischargeIntensity > 0.001f && dischargeShader != null) {
            try {
                dischargeShader.setFloatUniform("chromaticOffset", dischargeIntensity * 14f * density)
                RenderEffect.createRuntimeShaderEffect(dischargeShader, "image").asComposeRenderEffect()
            } catch (e: Throwable) {
                null
            }
        } else {
            null
        }
    }

    // Static anchored chassis border (stable, excluded from shader and deflection shake)
    val borderColor = if (isPressed) Color(0xFF444444) else Color(0xFF383838)
    val containerColor = if (isPressed) Color(0xFF333333) else Color(0xFF292929)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer {
                if (isPressed) {
                    scaleX = 0.985f
                    scaleY = 0.985f
                }
            }
            .clip(buttonShape)
            .background(containerColor)
            .border(BorderStroke(1.dp, borderColor), buttonShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Inner Content Container: Anchored inside the frame, receives all AGSL chromatic glitches & coil shudders
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = shakeX
                    translationY = shakeY
                    this.renderEffect = renderEffect
                },
            contentAlignment = Alignment.Center
        ) {
            // High-voltage electric scanline overlay inside button during organic spontaneous discharge
            if (!isPressed && dischargeIntensity > 0.05f) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val tearY = size.height * 0.48f
                    drawLine(
                        color = Color.White.copy(alpha = dischargeIntensity * 0.70f),
                        start = Offset(0f, tearY),
                        end = Offset(size.width, tearY),
                        strokeWidth = 1.8.dp.toPx()
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "⚡",
                    fontSize = 14.sp,
                    color = if (dischargeIntensity > 0.1f) Color(0xFF00E5FF) else Color(0xFF64B5F6),
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(
                    text = "Test CRT Screen-Off Now",
                    color = Color.White,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}

/**
 * Represents a momentary capacitive breakdown scanline arc that glitches
 * across the viewport due to simulated high-voltage capacitor instability.
 */
data class CapacitorArcLine(
    val yRatio: Float,
    val startXRatio: Float,
    val endXRatio: Float,
    val strokeWidthDp: Float
)
