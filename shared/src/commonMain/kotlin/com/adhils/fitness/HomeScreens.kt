package com.adhils.fitness

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    var quickFocus by remember { mutableStateOf("Freshest Muscles (Auto)") }
    var quickMuscles by remember { mutableStateOf(emptySet<String>()) }
    var quickMinutes by remember(p.minutes) { mutableIntStateOf(p.minutes) }
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

    // Top Gym Profile Bar + Consolidated Filter Pills
    PanelCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box {
                FilledTonalButton(onClick = { showGymMenu = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Default.FitnessCenter, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${p.gymPreset} ▾", fontWeight = FontWeight.Bold)
                }
                DropdownMenu(expanded = showGymMenu, onDismissRequest = { showGymMenu = false }) {
                    listOf("Home Gym", "Commercial Gym", "Bodyweight / Travel").forEach { preset ->
                        DropdownMenuItem(
                            text = { Text((if (p.gymPreset == preset) "✓ " else "") + preset) },
                            onClick = { vm.switchGymPreset(preset); showGymMenu = false }
                        )
                    }
                }
            }
            TextButton(onClick = onStart, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Check-in & AI ⚙")
            }
        }
        SmallLabel("WORKOUT GENERATOR FILTERS")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box {
                FilterChip(
                    selected = true,
                    onClick = { showSplitMenu = true },
                    label = { Text(quickFocus.substringBefore(" (") + " ▾") },
                    modifier = Modifier.heightIn(min = 42.dp)
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
                            text = { Text((if (quickFocus == sp) "✓ " else "") + sp) },
                            onClick = { quickFocus = sp; showSplitMenu = false }
                        )
                    }
                }
            }
            FilterChip(
                selected = quickMuscles.isNotEmpty() || showMusclePicker,
                onClick = { showMusclePicker = !showMusclePicker },
                label = { Text(if (quickMuscles.isEmpty()) "Target Muscles ▾" else "Muscles (${quickMuscles.size}) ▾") },
                modifier = Modifier.heightIn(min = 42.dp)
            )
            Box {
                FilterChip(
                    selected = false,
                    onClick = { showDurationMenu = true },
                    label = { Text("${quickMinutes}m ▾") },
                    modifier = Modifier.heightIn(min = 42.dp)
                )
                DropdownMenu(expanded = showDurationMenu, onDismissRequest = { showDurationMenu = false }) {
                    listOf(20, 30, 40, 45, 60, 75).forEach { m ->
                        DropdownMenuItem(
                            text = { Text("${m} min") },
                            onClick = { quickMinutes = m; showDurationMenu = false }
                        )
                    }
                }
            }
            FilterChip(
                selected = quickSupersets,
                onClick = { quickSupersets = !quickSupersets },
                label = { Text(if (quickSupersets) "⚡ Supersets ON" else "⚡ Supersets OFF") },
                modifier = Modifier.heightIn(min = 42.dp)
            )
        }
        if (showMusclePicker) {
            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallLabel("PICK THE MUSCLE GROUPS YOU WANT TO WORK OUT")
                val recoveryMap = remember(recovery) { recovery.associate { it.muscle to it.recoveryPercent } }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Recovery.ALL_MUSCLES.forEach { m ->
                        val sel = m in quickMuscles
                        AnatomicalMuscleCard(
                            muscle = m,
                            recoveryPct = recoveryMap[m] ?: 100,
                            isSelected = sel,
                            onClick = { quickMuscles = if (sel) quickMuscles - m else quickMuscles + m }
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        val activeDisplayTitle = remember(state.active) {
            state.active?.let { act ->
                if (act.title.contains("Fresh Muscle") || act.title.matches(Regex(".* [A-D]( · .*)?"))) {
                    val chosenEx = act.plan.mapNotNull { Catalog.byId[it.exerciseId] }
                    if (chosenEx.isNotEmpty()) {
                        val clean = Training.computeWorkoutTitle(chosenEx)
                        val suffix = if (act.title.contains(" · Light")) " · Light" else if (act.title.contains(" · Strong")) " · Strong" else ""
                        "$clean$suffix"
                    } else act.title
                } else act.title
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SmallLabel(if (state.active != null) "ACTIVE WORKOUT IN PROGRESS" else "GENERATED WORKOUT")
                Text(activeDisplayTitle ?: preview?.title ?: "Set up your workout", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                SmallLabel("${state.active?.plan?.size ?: preview?.plan?.size ?: 0} Exercises · ${quickMinutes} Min · ${p.equipment.joinToString(", ")}")
            }
        }
        PrimaryButton(
            text = if (state.active != null) "Resume Active Workout →" else "Start Workout (${preview?.plan?.size ?: 0} Exercises) →",
            enabled = state.active != null || preview != null,
            onClick = {
                if (state.active != null) onResume()
                else {
                    vm.start(quickCheck)
                    onResume()
                }
            }
        )
    }

    SectionTitle("Workout Exercises", "Tap card for posture guide & 1RM, or tap camera to start AI Coach")
    (state.active ?: preview)?.plan?.forEach { planned ->
        ExerciseRow(
            p = planned,
            onCameraClick = onCameraExercise?.let { cb -> { cb(planned.exerciseId) } },
            onClick = { onExercise(planned.exerciseId) }
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
