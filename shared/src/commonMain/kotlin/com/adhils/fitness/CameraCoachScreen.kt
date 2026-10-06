package com.adhils.fitness

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adhils.fitness.core.*
import kotlinx.coroutines.delay

enum class CameraCoachState {
    READY,
    COUNTDOWN,
    TRACKING,
    PAUSED
}

@Composable
fun CameraCoachScreen(
    exercise: Exercise,
    state: AppState,
    vm: FitnessViewModel,
    onBack: () -> Unit,
    onSaveSet: (reps: Int, seconds: Int, notes: List<String>) -> Unit
) {
    var isFrontCamera by remember { mutableStateOf(true) }
    var coachState by remember { mutableStateOf(CameraCoachState.READY) }
    var countdownDuration by remember { mutableIntStateOf(10) }
    var countdownRemaining by remember { mutableIntStateOf(10) }
    var reps by remember { mutableIntStateOf(0) }
    var holdSeconds by remember { mutableIntStateOf(0) }
    var soundEnabled by remember { mutableStateOf(true) }
    var cameraPermissionGranted by remember { mutableStateOf(true) }
    val observations = remember { mutableStateListOf<String>() }

    // Live body pose joints detected from camera (held in a dedicated state read only by Canvas draw)
    val detectedJointsState = remember { mutableStateOf<List<Joint>>(emptyList()) }
    var bodyDetectedInFrame by remember { mutableStateOf(false) }
    var fullBodyInFrame by remember { mutableStateOf(false) }
    var stableDetectionCount by remember { mutableIntStateOf(0) }
    var autoStartTriggered by remember { mutableStateOf(false) }
    var lastHudUpdateMs by remember { mutableLongStateOf(0L) }

    // Prevent screen lock / display sleep during camera coaching
    DisposableEffect(Unit) {
        PlatformScreen.setKeepScreenOn(true)
        onDispose {
            if (state.active == null) {
                PlatformScreen.setKeepScreenOn(false)
            }
        }
    }

    // Exercise-specific form tips
    val formTips = remember(exercise.id) {
        val tips = mutableListOf<String>()
        exercise.instructions.getOrNull(0)?.let { tips.add("Setup: $it") }
        exercise.instructions.getOrNull(1)?.let { tips.add("Movement: $it") }
        tips.add(exercise.breathing)
        when (exercise.pattern.lowercase()) {
            "squat" -> tips.add("Keep chest tall · Drive through your heels · Knees track with toes")
            "hinge" -> tips.add("Hinge at the hips · Keep a neutral spine · Push hips back")
            "push" -> tips.add("Brace core · Keep elbows at 45 degrees · Full lockout at top")
            "pull" -> tips.add("Lead with elbows · Squeeze shoulder blades · Controlled return")
            else -> tips.add("Control the tempo · Avoid bouncing · Steady breathing")
        }
        tips
    }

    var activeTipIndex by remember { mutableIntStateOf(0) }

    val poseEngine = remember(exercise.effectiveCamera) {
        PoseEngine(exercise.effectiveCamera, movementCues = true)
    }
    var liveEngineObs by remember { mutableStateOf(PoseObservation()) }

    // Pause pose engine when state changes to PAUSED
    LaunchedEffect(coachState) {
        if (coachState == CameraCoachState.PAUSED) {
            liveEngineObs = poseEngine.pause()
        }
    }

    // High-performance pose callback
    val onPoseDetectedCallback = remember {
        { joints: List<Joint> ->
            // Store joints for draw phase only when native hardware overlay is not available (Android)
            if (!isNativeCameraOverlaySupported) {
                detectedJointsState.value = joints
            }

            val hasHead = joints.isNotEmpty() && joints[0].visibility > 0.35f
            val hasUpper = if (joints.size > 24) {
                ((joints[11].visibility > 0.35f || joints[12].visibility > 0.35f) &&
                (joints[23].visibility > 0.35f || joints[24].visibility > 0.35f)) || hasHead
            } else hasHead

            val exerciseNeedsFeet = exercise.effectiveCamera in setOf("squat", "rdl")
            val hasFull = if (exerciseNeedsFeet) {
                hasUpper && (joints[25].visibility > 0.30f || joints[26].visibility > 0.30f)
            } else {
                hasUpper
            }

            // Hands-free auto-start triggers smoothly from 15 stable frames (~0.5s)
            if (coachState == CameraCoachState.READY && !autoStartTriggered) {
                if (hasFull) {
                    stableDetectionCount += 1
                    if (stableDetectionCount >= 15) {
                        autoStartTriggered = true
                        countdownRemaining = 5
                        if (soundEnabled) PlatformSound.playCountdownBeep()
                        coachState = CameraCoachState.COUNTDOWN
                    }
                } else {
                    stableDetectionCount = (stableDetectionCount - 2).coerceAtLeast(0)
                }
            }

            // Real-time rep and form tracking via PoseEngine
            val nowMs = kotlin.time.TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds
            var obs: PoseObservation? = null
            if (coachState == CameraCoachState.TRACKING && joints.size == 33) {
                val updatedObs = poseEngine.update(PoseFrame(timeMs = nowMs, joints = joints, aspectRatio = 9f / 16f))
                obs = updatedObs

                if (!exercise.timed && updatedObs.reps > reps) {
                    reps = updatedObs.reps
                    if (soundEnabled) PlatformSound.playCountdownBeep()
                    updatedObs.cue?.let { observations.add("Rep $reps: $it") }
                }
                if (exercise.timed && updatedObs.holdSeconds > holdSeconds) {
                    holdSeconds = updatedObs.holdSeconds
                }
            }

            // Throttle HUD recomposition updates to 10 Hz (every 100ms) or state transitions
            val stateChanged = obs != null && (obs.reps != liveEngineObs.reps || obs.tracking != liveEngineObs.tracking || obs.phase != liveEngineObs.phase)
            if (nowMs - lastHudUpdateMs >= 100L || stateChanged) {
                lastHudUpdateMs = nowMs
                if (bodyDetectedInFrame != hasUpper) bodyDetectedInFrame = hasUpper
                if (fullBodyInFrame != hasFull) fullBodyInFrame = hasFull
                if (obs != null) liveEngineObs = obs
            }
        }
    }

    // Timer effect for countdown
    LaunchedEffect(coachState) {
        if (coachState == CameraCoachState.COUNTDOWN) {
            while (countdownRemaining > 0) {
                if (soundEnabled) PlatformSound.playCountdownBeep()
                delay(1000)
                countdownRemaining -= 1
            }
            if (soundEnabled) PlatformSound.playTimerFinishedChime()
            coachState = CameraCoachState.TRACKING
        }
    }

    // Timer effect for hold duration & rotating form cues while tracking
    LaunchedEffect(coachState) {
        if (coachState == CameraCoachState.TRACKING) {
            var elapsedInTracking = 0
            while (coachState == CameraCoachState.TRACKING) {
                delay(1000)
                elapsedInTracking += 1
                if (exercise.timed && !liveEngineObs.tracking) {
                    holdSeconds += 1
                }
                // Rotate helpful coaching cue every 6 seconds
                if (elapsedInTracking % 6 == 0 && formTips.isNotEmpty()) {
                    activeTipIndex = (activeTipIndex + 1) % formTips.size
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F14))
            .systemBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "Camera Coach: ${exercise.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
        }

        // Viewfinder Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF141416))
                .border(2.dp, Color(0xFFFF2D55).copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Live Native Camera View
            PlatformCameraPreview(
                modifier = Modifier.fillMaxSize(),
                isFrontCamera = isFrontCamera,
                onPermissionGranted = { granted ->
                    cameraPermissionGranted = granted
                },
                onPoseDetected = onPoseDetectedCallback
            )

            // Real-Time Green Skeletal Joint Tracking Overlay (Android Canvas; iOS renders natively via CoreAnimation)
            if (!isNativeCameraOverlaySupported) {
                SkeletonTrackingOverlay(
                    jointsProvider = { detectedJointsState.value },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Top Telemetry HUD
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Tracking Status & Score Pill
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (fullBodyInFrame || (coachState == CameraCoachState.TRACKING && liveEngineObs.tracking)) Mint.copy(alpha = 0.7f) else Color(0xFFFF9500).copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            coachState == CameraCoachState.TRACKING && liveEngineObs.tracking -> Mint
                                            coachState == CameraCoachState.COUNTDOWN -> Color(0xFFFFCC00)
                                            fullBodyInFrame -> Mint
                                            bodyDetectedInFrame -> Color(0xFFFFCC00)
                                            else -> Color(0xFFFF9500)
                                        }
                                    )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = when (coachState) {
                                    CameraCoachState.READY -> when {
                                        fullBodyInFrame -> "FULL BODY LOCKED"
                                        bodyDetectedInFrame -> "STEP BACK FOR FULL VIEW"
                                        else -> "STAND IN ${exercise.view.uppercase()} VIEW"
                                    }
                                    CameraCoachState.COUNTDOWN -> "GET READY (${countdownRemaining}s)"
                                    CameraCoachState.TRACKING -> {
                                        if (liveEngineObs.tracking) "TRACKING · ${liveEngineObs.phase.uppercase()}"
                                        else when {
                                            liveEngineObs.status.contains("shoulders", ignoreCase = true) -> "FIT FULL BODY IN VIEW"
                                            liveEngineObs.status.contains("Move back", ignoreCase = true) -> "STEP BACK"
                                            liveEngineObs.status.contains("Move closer", ignoreCase = true) -> "STEP CLOSER"
                                            liveEngineObs.status.contains("Turn sideways", ignoreCase = true) -> "TURN SIDEWAYS"
                                            liveEngineObs.status.contains("Face the camera", ignoreCase = true) -> "FACE CAMERA"
                                            liveEngineObs.status.contains("floor position", ignoreCase = true) -> "FLOOR POSITION"
                                            liveEngineObs.status.contains("settles", ignoreCase = true) -> "HOLD STILL"
                                            else -> liveEngineObs.status.take(22).uppercase()
                                        }
                                    }
                                    CameraCoachState.PAUSED -> "PAUSED"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    coachState == CameraCoachState.TRACKING && liveEngineObs.tracking -> Mint
                                    coachState == CameraCoachState.COUNTDOWN -> Color(0xFFFFCC00)
                                    fullBodyInFrame -> Mint
                                    else -> Color.White
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Metric Pill (Reps or Hold Time) + Live Form Score
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.75f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (exercise.timed) {
                                    "⏱️ Hold: ${holdSeconds}s"
                                } else {
                                    "🏋️ Reps: $reps"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            if (coachState == CameraCoachState.TRACKING && liveEngineObs.formScore < 100) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Form ${liveEngineObs.formScore}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (liveEngineObs.formScore >= 80) Mint else Color(0xFFFFCC00),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Controls: Sound Mute & Camera Flip
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Audio Mute/Unmute Toggle Button
                    IconButton(
                        onClick = { soundEnabled = !soundEnabled },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                    ) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (soundEnabled) "Mute Audio" else "Unmute Audio",
                            tint = if (soundEnabled) Mint else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Camera Switch / Flip Button
                    IconButton(
                        onClick = { isFrontCamera = !isFrontCamera },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Switch Camera",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Center Big Countdown Display
            if (coachState == CameraCoachState.COUNTDOWN) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(3.dp, Mint),
                    modifier = Modifier.size(120.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$countdownRemaining",
                            fontSize = 60.sp,
                            fontWeight = FontWeight.Black,
                            color = Mint
                        )
                    }
                }
            }

            // Quick Interactive Rep Counter Controls (during tracking or paused, for rep-based exercises)
            if (!exercise.timed && (coachState == CameraCoachState.TRACKING || coachState == CameraCoachState.PAUSED)) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Increment Rep
                        FilledIconButton(
                            onClick = {
                                reps += 1
                                if (soundEnabled) PlatformSound.playCountdownBeep()
                            },
                            modifier = Modifier.size(54.dp),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Mint,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Rep", modifier = Modifier.size(28.dp))
                        }

                        // Decrement Rep
                        if (reps > 0) {
                            FilledTonalIconButton(
                                onClick = { reps -= 1 },
                                modifier = Modifier.size(44.dp),
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = Color.Black.copy(alpha = 0.7f),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease Rep", modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            // Bottom Form Guidance Cue Banner
            val cueText = when (coachState) {
                CameraCoachState.READY -> when {
                    fullBodyInFrame -> "🟢 Ready! Auto-starting in ${((15 - stableDetectionCount).coerceAtLeast(0) / 10) + 1}s... or tap Start (${countdownDuration}s)"
                    bodyDetectedInFrame -> "👤 Step back slightly so your legs & feet fit in view · Green lines will lock on!"
                    else -> "Set phone down & stand back in ${exercise.view.lowercase()} view · Green lines will track your body!"
                }
                CameraCoachState.COUNTDOWN -> "In position · Starting in $countdownRemaining..."
                CameraCoachState.TRACKING -> {
                    liveEngineObs.cue ?: liveEngineObs.nextRepFeedback ?: formTips.getOrNull(activeTipIndex) ?: "Maintain steady breathing & form."
                }
                CameraCoachState.PAUSED -> "Workout paused. Tap Resume to continue or Log Set to finish."
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(14.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E1F29).copy(alpha = 0.94f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (coachState == CameraCoachState.TRACKING || bodyDetectedInFrame) Mint.copy(alpha = 0.4f) else Color(0xFFFF2D55).copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = when (coachState) {
                            CameraCoachState.READY -> if (bodyDetectedInFrame) "🟢" else "📷"
                            CameraCoachState.COUNTDOWN -> "⏱️"
                            CameraCoachState.TRACKING -> "💡"
                            CameraCoachState.PAUSED -> "⏸️"
                        },
                        fontSize = 20.sp
                    )
                    Text(
                        text = cueText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Action Buttons Row & Setup Delay Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (coachState) {
                CameraCoachState.READY -> {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        Text("Cancel", color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }

                    // Delay Duration Switcher (5s / 10s)
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = Color(0xFF1C1D24),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            listOf(5, 10).forEach { sec ->
                                val selected = countdownDuration == sec
                                FilterChip(
                                    selected = selected,
                                    onClick = { countdownDuration = sec },
                                    label = { Text("${sec}s", fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Mint,
                                        selectedLabelColor = Color.Black,
                                        containerColor = Color.Transparent,
                                        labelColor = Color.White
                                    ),
                                    border = null,
                                    modifier = Modifier.height(44.dp)
                                )
                            }
                        }
                    }

                    // Primary Start Set Button with generous walk-back countdown
                    Button(
                        onClick = {
                            countdownRemaining = countdownDuration
                            coachState = CameraCoachState.COUNTDOWN
                        },
                        modifier = Modifier
                            .weight(1.7f)
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2D55))
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Start (${countdownDuration}s)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                CameraCoachState.COUNTDOWN -> {
                    OutlinedButton(
                        onClick = {
                            coachState = CameraCoachState.READY
                            autoStartTriggered = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        Text("Cancel Countdown", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }

                CameraCoachState.TRACKING, CameraCoachState.PAUSED -> {
                    OutlinedButton(
                        onClick = {
                            coachState = if (coachState == CameraCoachState.TRACKING) {
                                CameraCoachState.PAUSED
                            } else {
                                CameraCoachState.TRACKING
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        Icon(
                            imageVector = if (coachState == CameraCoachState.TRACKING) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (coachState == CameraCoachState.TRACKING) "Pause" else "Resume",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            PlatformSound.playTimerFinishedChime()
                            val summaryNotes = buildList {
                                add("${exercise.name} completed with Camera Coach")
                                if (exercise.timed) add("Hold time: ${holdSeconds}s") else add("Reps: $reps")
                            }
                            onSaveSet(reps, holdSeconds, summaryNotes)
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2D55))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Log ${if (exercise.timed) "${holdSeconds}s" else "$reps Reps"}",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * High-performance skeletal overlay that evaluates joint positions exclusively in the Draw phase.
 * Passing `jointsProvider` skips Recomposition and Layout phases entirely, maintaining solid 60 FPS.
 */
@Composable
private fun SkeletonTrackingOverlay(
    jointsProvider: () -> List<Joint>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val joints = jointsProvider()
        if (joints.size <= 28) return@Canvas

        val edges = listOf(
            11 to 12, // Shoulders
            11 to 13, 13 to 15, // Left Arm
            12 to 14, 14 to 16, // Right Arm
            11 to 23, 12 to 24, // Torso
            23 to 24, // Hips
            23 to 25, 25 to 27, // Left Leg
            24 to 26, 26 to 28  // Right Leg
        )

        fun pt(idx: Int): Offset {
            val j = joints[idx]
            val isLandscape = size.width > size.height
            val rCam = if (isLandscape) 16f / 9f else 9f / 16f
            val rScreen = size.width / size.height
            val px: Float
            val py: Float
            if (rScreen < rCam) {
                val scaledWidth = size.height * rCam
                val cropX = (scaledWidth - size.width) / 2f
                px = j.x * scaledWidth - cropX
                py = j.y * size.height
            } else {
                val scaledHeight = size.width / rCam
                val cropY = (scaledHeight - size.height) / 2f
                px = j.x * size.width
                py = j.y * scaledHeight - cropY
            }
            return Offset(px, py)
        }

        // Draw connecting green bones
        edges.forEach { (a, b) ->
            if (a < joints.size && b < joints.size) {
                val ja = joints[a]
                val jb = joints[b]
                if (ja.visibility > 0.35f && jb.visibility > 0.35f) {
                    drawLine(
                        color = Mint,
                        start = pt(a),
                        end = pt(b),
                        strokeWidth = 3.5.dp.toPx()
                    )
                }
            }
        }

        // Draw joint circles
        listOf(11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28).forEach { idx ->
            if (idx < joints.size && joints[idx].visibility > 0.35f) {
                drawCircle(
                    color = Mint,
                    radius = 5.dp.toPx(),
                    center = pt(idx)
                )
            }
        }

        // Head/Nose tracker ring
        if (joints[0].visibility > 0.35f) {
            drawCircle(
                color = Mint.copy(alpha = 0.75f),
                radius = 20.dp.toPx(),
                center = pt(0),
                style = Stroke(width = 2.5.dp.toPx())
            )
        }
    }
}

