package com.adhils.fitness

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adhils.fitness.core.*
import kotlinx.datetime.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayScreen(
    state: AppState,
    vm: FitnessViewModel,
    onStart: () -> Unit,
    onResume: () -> Unit,
    onExercise: (String) -> Unit,
    onHistory: () -> Unit,
    onCameraExercise: ((String) -> Unit)? = null
) {
    val p = state.profile
    val activeSession = state.active
    val initialSplit = remember(activeSession?.id, activeSession?.title) {
        activeSession?.let { act ->
            val t = act.title
            when {
                t.contains("Lower Body", ignoreCase = true) -> "Lower Body"
                t.contains("Upper Body", ignoreCase = true) -> "Upper Body"
                t.contains("Push", ignoreCase = true) || t.contains("Chest", ignoreCase = true) -> "Push (Chest & Shoulders)"
                t.contains("Pull", ignoreCase = true) || t.contains("Back", ignoreCase = true) -> "Pull (Back & Biceps)"
                t.contains("Shoulders", ignoreCase = true) || t.contains("Arms", ignoreCase = true) -> "Shoulders & Arms"
                t.contains("Core", ignoreCase = true) || t.contains("Mobility", ignoreCase = true) -> "Core & Mobility"
                t.contains("Full Body", ignoreCase = true) -> "Full Body"
                else -> "Freshest Muscles (Auto)"
            }
        } ?: "Freshest Muscles (Auto)"
    }
    val initialMinutes = remember(activeSession?.id, activeSession?.plan?.size, p.minutes) {
        activeSession?.let { act ->
            val mainCount = act.plan.count { it.exerciseId !in setOf("march", "cat-cow") }
            when {
                mainCount <= 3 -> 20
                mainCount == 4 -> 30
                mainCount == 5 -> 45
                else -> 60
            }
        } ?: p.minutes
    }

    var quickFocus by remember(activeSession?.id) { mutableStateOf(initialSplit) }
    var quickMuscles by remember(activeSession?.id) { mutableStateOf(emptySet<String>()) }
    var quickMinutes by remember(activeSession?.id, initialMinutes) { mutableIntStateOf(initialMinutes) }
    var quickSupersets by remember { mutableStateOf(false) }
    var showGymMenu by remember { mutableStateOf(false) }
    var showSplitMenu by remember { mutableStateOf(false) }
    var showMusclePicker by remember { mutableStateOf(false) }
    var showDurationMenu by remember { mutableStateOf(false) }

    val quickCheck = remember(quickFocus, quickMuscles, quickMinutes, quickSupersets) {
        CheckIn(energy = 3, soreness = 0, minutes = quickMinutes, feeling = "Good", focus = quickFocus, targetMuscles = quickMuscles, supersets = quickSupersets)
    }
    val preview = remember(state, quickCheck) { runCatching { Training.generate(state, quickCheck) }.getOrNull() }
    val recovery = remember(state) { Recovery.calculate(state) }

    val plannedExercises = (activeSession ?: preview)?.plan ?: emptyList()
    val musclesCount = remember(plannedExercises) {
        plannedExercises.mapNotNull { Catalog.byId[it.exerciseId] }
            .flatMap { Recovery.extractMuscles(it) }
            .distinct().size.coerceAtLeast(1)
    }
    val freshMuscles = remember(recovery) { Recovery.freshestMuscles(state, 3) }

    val activeDisplayTitle = remember(activeSession, preview) {
        activeSession?.let { act ->
            if (act.title.contains("Fresh Muscle") || act.title.matches(Regex(".* [A-D]( · .*)?"))) {
                val chosenEx = act.plan.mapNotNull { Catalog.byId[it.exerciseId] }
                if (chosenEx.isNotEmpty()) {
                    val clean = Training.computeWorkoutTitle(chosenEx)
                    val suffix = if (act.title.contains(" · Light")) " · Light" else if (act.title.contains(" · Strong")) " · Strong" else ""
                    "$clean$suffix"
                } else act.title
            } else act.title
        } ?: preview?.title ?: "Full Body Strength"
    }

    // 1. Fitbod Today Workout Plan Overview Header (Matching Screenshot 1)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallLabel("${(p.name.ifBlank { "ATHLETE" }).uppercase()}'S PLAN · TODAY")
            IconButton(onClick = onStart, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Tune, contentDescription = "Workout Generator Preferences", tint = Color(0xFF8E8E93))
            }
        }

        Text(
            text = activeDisplayTitle.uppercase(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color.White,
            fontSize = 26.sp
        )

        Text(
            text = "${plannedExercises.size} Exercises • $musclesCount Muscles",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF8E8E93)
        )

        // Pill Selection Row
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box {
                FilterChip(
                    selected = false,
                    onClick = { showDurationMenu = true },
                    label = { Text("${quickMinutes}m ▾", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.heightIn(min = 40.dp)
                )
                DropdownMenu(expanded = showDurationMenu, onDismissRequest = { showDurationMenu = false }) {
                    listOf(20, 30, 40, 45, 60, 75).forEach { m ->
                        DropdownMenuItem(
                            text = { Text((if (quickMinutes == m) "✓ " else "") + "${m} min") },
                            onClick = {
                                quickMinutes = m
                                showDurationMenu = false
                                val nextCheck = quickCheck.copy(minutes = m)
                                vm.regenerateWorkout(nextCheck)
                            }
                        )
                    }
                }
            }

            Box {
                FilterChip(
                    selected = false,
                    onClick = { showGymMenu = true },
                    label = { Text("${p.gymPreset} ▾", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.heightIn(min = 40.dp)
                )
                DropdownMenu(expanded = showGymMenu, onDismissRequest = { showGymMenu = false }) {
                    listOf("Home Gym", "Commercial Gym", "Bodyweight / Travel").forEach { preset ->
                        DropdownMenuItem(
                            text = { Text((if (p.gymPreset == preset) "✓ " else "") + preset) },
                            onClick = {
                                showGymMenu = false
                                vm.switchGymPreset(preset, quickCheck)
                            }
                        )
                    }
                }
            }

            Box {
                FilterChip(
                    selected = quickMuscles.isEmpty(),
                    onClick = { showSplitMenu = true },
                    label = { Text(quickFocus.substringBefore(" (") + " ▾", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.heightIn(min = 40.dp)
                )
                DropdownMenu(expanded = showSplitMenu, onDismissRequest = { showSplitMenu = false }) {
                    listOf(
                        "Freshest Muscles (Auto)",
                        "Full Body",
                        "Upper Body",
                        "Lower Body",
                        "Push (Chest & Shoulders)",
                        "Pull (Back & Biceps)",
                        "Shoulders & Arms",
                        "Core & Mobility"
                    ).forEach { sp ->
                        DropdownMenuItem(
                            text = { Text((if (quickFocus == sp && quickMuscles.isEmpty()) "✓ " else "") + sp) },
                            onClick = {
                                quickFocus = sp
                                quickMuscles = emptySet()
                                showSplitMenu = false
                                val nextCheck = quickCheck.copy(focus = sp, targetMuscles = emptySet())
                                vm.regenerateWorkout(nextCheck)
                            }
                        )
                    }
                }
            }

            FilterChip(
                selected = quickMuscles.isNotEmpty() || showMusclePicker,
                onClick = { showMusclePicker = !showMusclePicker },
                label = { Text(if (quickMuscles.isEmpty()) "Target Muscles ▾" else "Muscles (${quickMuscles.size}) ▾", fontWeight = FontWeight.Bold) },
                modifier = Modifier.heightIn(min = 40.dp)
            )
        }

        if (showMusclePicker) {
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallLabel("PICK THE MUSCLE GROUPS YOU WANT TO WORK OUT")
                val recoveryMap = remember(recovery) { recovery.associate { it.muscle to it.recoveryPercent } }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Recovery.ALL_MUSCLES.forEach { m ->
                        val sel = m in quickMuscles
                        AnatomicalMuscleCard(
                            muscle = m,
                            recoveryPct = recoveryMap[m] ?: 100,
                            isSelected = sel,
                            onClick = {
                                val nextMuscles = if (sel) quickMuscles - m else quickMuscles + m
                                quickMuscles = nextMuscles
                                val nextCheck = quickCheck.copy(targetMuscles = nextMuscles)
                                vm.regenerateWorkout(nextCheck)
                            }
                        )
                    }
                }
            }
        }

        // AI Coach Guidance Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF191B26),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282B3C)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF252838),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🤖", fontSize = 18.sp)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "AI COACH OVERVIEW",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Mint
                    )
                    Text(
                        if (state.active != null) "Session in progress. Tap any exercise to log sets or start live AI Camera Coach."
                        else "Balanced for your freshest muscles (${freshMuscles.joinToString(", ")}). Ready to train?",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCCD0DF)
                    )
                }
            }
        }
    }

    // 2. Timeline Connected Exercise List (Matching Screenshot 1)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionTitle("Today's Exercises", "Tap exercise to open Exercise Log, form videos, or AI Camera Coach")

        plannedExercises.forEachIndexed { index, planned ->
            val e = Catalog.get(planned.exerciseId)
            val isLast = index == plannedExercises.lastIndex

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Left Timeline Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(28.dp).padding(top = 16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (state.active?.results?.any { it.exerciseId == e.id } == true) Mint else Color(0xFF2C2D3A),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (state.active?.results?.any { it.exerciseId == e.id } == true) {
                                Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp), tint = Color.Black)
                            } else {
                                Text("${index + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                    if (!isLast) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(56.dp)
                                .background(Color(0xFF2C2D3A))
                        )
                    }
                }

                // Exercise Row Card
                Surface(
                    onClick = {
                        if (state.active == null) {
                            vm.start(quickCheck)
                        }
                        onExercise(planned.exerciseId)
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1E202B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3142)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ExerciseThumbnail(
                            exerciseId = e.id,
                            modifier = Modifier.size(54.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentDescription = e.name
                        )

                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = e.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            val setsText = "${planned.sets} Sets • " + if (e.timed) "${planned.seconds}s" else "${planned.minReps}–${planned.maxReps} Reps" +
                                (if (planned.weightKg > 0.0) " • ${weightLabel(planned.weightKg, p)}" else " • Bodyweight")
                            Text(
                                text = setsText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFFF2D55)
                            )
                            SmallLabel("${e.muscles}")
                        }

                        if (onCameraExercise != null) {
                            IconButton(
                                onClick = { onCameraExercise(planned.exerciseId) },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "AI Camera Coach",
                                    tint = Mint,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 3. Prominent Bottom Start Workout Button (Fitbod Coral/Pink-Red)
    val hasLoggedSets = state.active?.results?.isNotEmpty() == true
    Button(
        onClick = {
            if (state.active == null) {
                vm.start(quickCheck)
            }
            onResume()
        },
        enabled = state.active != null || plannedExercises.isNotEmpty(),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFE83F5B),
            contentColor = Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        Icon(
            imageVector = if (hasLoggedSets) Icons.Default.PlayArrow else Icons.Default.FitnessCenter,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (hasLoggedSets) "Resume Active Workout (In Progress) →"
            else "Start Workout (${plannedExercises.size} Exercises)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }

    val completedSessions = remember(state) { state.sessions.filter { it.finishedAt != null } }
    val daysSinceLastWorkout = remember(completedSessions) {
        val last = completedSessions.maxOfOrNull { it.finishedAt ?: 0L } ?: 0L
        if (last == 0L) 0 else ((nowMillis() - last) / (24L * 3600 * 1000)).toInt()
    }
    MuscleHeatmapCard(recovery, daysSinceLastWorkout = daysSinceLastWorkout)

    val now = Clock.System.now()
    val tz = TimeZone.currentSystemDefault()
    val today = now.toLocalDateTime(tz).date
    val dayOfWeekNum = today.dayOfWeek.isoDayNumber // 1 is Monday, 7 is Sunday
    val monday = today.minus(DatePeriod(days = dayOfWeekNum - 1))
    val done = state.sessions.filter { it.finishedAt != null }.map {
        Instant.fromEpochMilliseconds(it.finishedAt!!).toLocalDateTime(tz).date
    }
    val weekCount = done.count { it >= monday && it < monday.plus(DatePeriod(days = 7)) }
    PanelCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Weekly Goal Streak", fontWeight = FontWeight.Bold)
            SmallLabel("$weekCount of ${p.days} workouts")
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            (0..6).forEach { i ->
                val date = monday.plus(DatePeriod(days = i))
                val checked = date in done
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(34.dp).background(if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, CircleShape), Alignment.Center) {
                        if (checked) Icon(Icons.Default.Check, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimary)
                        else Text(if (date == today) "•" else "", color = MaterialTheme.colorScheme.primary)
                    }
                    SmallLabel(listOf("M", "T", "W", "T", "F", "S", "S")[i])
                }
            }
        }
        TextButton(onClick = onHistory, modifier = Modifier.heightIn(min = 48.dp)) { Text("View Workout Log & Saved Routines →") }
    }
}

@Composable
fun CheckInScreen(p: Profile, onStart: (CheckIn) -> Unit, onCancel: () -> Unit, onAskCoach: (CheckIn) -> Unit) {
    var feeling by remember { mutableStateOf("Good") }
    var soreness by remember { mutableIntStateOf(0) }
    var energy by remember { mutableIntStateOf(3) }
    var minutes by remember { mutableIntStateOf(p.minutes) }
    var focus by remember { mutableStateOf("Freshest Muscles (Auto)") }
    var targetMuscles by remember { mutableStateOf(emptySet<String>()) }
    var supersets by remember { mutableStateOf(false) }

    ScreenHeader("Daily Check-in", onCancel)
    PanelCard {
        SectionTitle("How are you feeling?")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Fresh", "Good", "Tired").forEach { f ->
                FilterChip(selected = feeling == f, onClick = { feeling = f }, label = { Text(f) })
            }
        }
        SectionTitle("Soreness Level (0 = None, 2 = High)")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (0..2).forEach { s ->
                FilterChip(selected = soreness == s, onClick = { soreness = s }, label = { Text(listOf("None", "Mild", "High")[s]) })
            }
        }
        SectionTitle("Energy (1 to 5)")
        Slider(value = energy.toFloat(), onValueChange = { energy = it.toInt() }, valueRange = 1f..5f, steps = 3)
        SectionTitle("Available Time: $minutes min")
        Slider(value = minutes.toFloat(), onValueChange = { minutes = it.toInt() }, valueRange = 15f..90f, steps = 14)
        SectionTitle("Focus Area")
        listOf("Freshest Muscles (Auto)", "Full Body", "Upper Body", "Lower Body", "Push (Chest & Shoulders)", "Pull (Back & Biceps)", "Core & Mobility").forEach { f ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = focus == f, onClick = { focus = f })
                Text(f)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = supersets, onCheckedChange = { supersets = it })
            Text("Include Supersets for time efficiency")
        }
        PrimaryButton("Generate Today's Workout") {
            onStart(CheckIn(energy = energy, soreness = soreness, minutes = minutes, feeling = feeling, focus = focus, targetMuscles = targetMuscles, supersets = supersets))
        }
        OutlinedButton(onClick = {
            onAskCoach(CheckIn(energy = energy, soreness = soreness, minutes = minutes, feeling = feeling, focus = focus, targetMuscles = targetMuscles, supersets = supersets))
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Propose with AI Coach")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: AppState,
    onExercise: (String) -> Unit,
    onRoutine: (SavedRoutine) -> Unit,
    onSession: (String) -> Unit,
    onCameraExercise: ((String) -> Unit)? = null
) {
    var tab by remember { mutableIntStateOf(0) }
    PrimaryTabRow(selectedTabIndex = tab) {
        Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Exercises") })
        Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Routines") })
        Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("History") })
    }
    when (tab) {
        0 -> {
            Catalog.exercises.forEach { e ->
                ExerciseRow(
                    p = PlannedExercise(exerciseId = e.id, sets = 3, minReps = 10, maxReps = 10, seconds = 30),
                    onCameraClick = onCameraExercise?.let { cb -> { cb(e.id) } },
                    onClick = { onExercise(e.id) }
                )
            }
        }
        1 -> {
            if (state.routines.isEmpty()) EmptyState("Make it your routine", "Save a completed workout as a routine from its summary.")
            else state.routines.forEach { r ->
                PanelCard {
                    SectionTitle(r.name, "${r.plan.size} exercises")
                    PrimaryButton("Start routine", state.active == null) { onRoutine(r) }
                }
            }
        }
        2 -> {
            if (state.sessions.none { it.finishedAt != null }) EmptyState("Your history starts here", "Complete your first workout to see it here.")
            else state.sessions.filter { it.finishedAt != null }.reversed().forEach { s ->
                val displayTitle = if (s.title.contains("Fresh Muscle") || s.title.matches(Regex(".* [A-D]( · .*)?"))) {
                    val chosenEx = s.plan.mapNotNull { Catalog.byId[it.exerciseId] }
                    if (chosenEx.isNotEmpty()) {
                        val clean = Training.computeWorkoutTitle(chosenEx)
                        val suffix = if (s.title.contains(" · Light")) " · Light" else if (s.title.contains(" · Strong")) " · Strong" else ""
                        "$clean$suffix"
                    } else s.title
                } else s.title
                PanelCard {
                    SectionTitle(displayTitle, "${s.results.size} logged sets · " + formatDate(s.startedAt))
                    TextButton(onClick = { onSession(s.id) }) { Text("View summary →") }
                }
            }
        }
    }
}
