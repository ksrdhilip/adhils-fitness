package com.adhils.fitness

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adhils.fitness.core.*

@Composable
fun FitnessApp(vm: FitnessViewModel = remember { FitnessViewModel() }, initialRoute: String? = null) {
    val store by vm.store.collectAsState()
    val ready by vm.ready.collectAsState()
    val error by vm.error.collectAsState()
    val record = store.selected

    FitnessTheme(record.state.profile.theme) {
        if (!ready) {
            Surface(Modifier.fillMaxSize()) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else {
            key(record.id) {
                AppContent(vm, store, record.state, initialRoute)
            }
        }
        error?.let { text ->
            AlertDialog(
                onDismissRequest = { vm.error.value = null },
                title = { Text("Could not complete that action") },
                text = { Text(text) },
                confirmButton = { TextButton(onClick = { vm.error.value = null }) { Text("OK") } }
            )
        }
    }
}

@Composable
private fun AppContent(vm: FitnessViewModel, store: ProfileStore, state: AppState, initialRoute: String? = null) {
    var route by rememberSaveable { mutableStateOf(initialRoute ?: if (state.profile.onboardingComplete) "today" else "welcome") }
    val navStack = remember { mutableStateListOf<String>() }
    var profiles by remember { mutableStateOf(false) }
    var leave by remember { mutableStateOf(false) }
    var discard by remember { mutableStateOf(false) }
    val pageScroll = remember(route, if (route == "active") state.active?.currentExercise else null) { ScrollState(0) }
    val roots = listOf("today", "progress", "workouts", "coach", "settings")
    val labels = listOf("Workout", "Body", "Log", "AI Coach", "Gym")
    val icons = listOf(Icons.Default.FitnessCenter, Icons.Default.Accessibility, Icons.Default.History, Icons.Default.Videocam, Icons.Default.Settings)

    fun navigateTo(dest: String) {
        if (route != dest) {
            navStack.add(route)
            route = dest
        }
    }

    fun back() {
        if (navStack.isNotEmpty()) {
            route = navStack.removeAt(navStack.lastIndex)
        } else {
            route = if (route == "active") "today" else "today"
        }
    }

    fun showExercise(id: String) {
        navigateTo("exercise:$id")
    }

    fun showHowTo(id: String) {
        navigateTo("howto:$id")
    }

    PlatformBackHandler(route !in roots && route != "welcome") {
        back()
    }

    // Keep screen awake while workout is started/active or camera is open
    val isWorkoutActive = state.active != null || route == "active" || route.startsWith("camera")
    DisposableEffect(isWorkoutActive) {
        PlatformScreen.setKeepScreenOn(isWorkoutActive)
        onDispose {
            PlatformScreen.setKeepScreenOn(false)
        }
    }

    if (route == "welcome") {
        WelcomeScreen(
            onStart = { route = "onboarding_quiz" },
            onLogIn = { if (store.profiles.size > 1) profiles = true else route = "onboarding_quiz" }
        )
        return
    }

    if (route.startsWith("onboarding_quiz")) {
        val initialStep = route.substringAfter(":", "").toIntOrNull() ?: 0
        OnboardingQuestionnaire(
            initialProfile = state.profile,
            initialStep = initialStep,
            onFinish = {
                vm.saveProfile(it)
                route = "today"
            },
            onBackToWelcome = {
                route = "welcome"
            }
        )
        return
    }

    if (route.startsWith("camera")) {
        val exerciseIdParam = route.substringAfter(":", "").takeIf { it.isNotBlank() && it != "camera" }
        val currentExercise = exerciseIdParam?.let { Catalog.byId[it] }
            ?: state.active?.let { s -> s.plan.getOrNull(s.currentExercise)?.exerciseId?.let { Catalog.byId[it] } }
            ?: Catalog.byId["goblet-squat"]
            ?: Catalog.exercises.first()

        CameraCoachScreen(
            exercise = currentExercise,
            state = state,
            vm = vm,
            onBack = ::back
        ) { reps, seconds, notes ->
            state.active?.let { s ->
                CameraDraft.value = CameraSetDraft(store.selectedId, s.id, currentExercise.id, reps, seconds, notes)
            }
            back()
        }
        return
    }

    BoxWithConstraints {
        val tablet = maxWidth >= 600.dp
        Scaffold(bottomBar = {
            if (!tablet && route in roots) NavigationBar {
                roots.forEachIndexed { i, name ->
                    NavigationBarItem(
                        selected = route == name,
                        onClick = {
                            navStack.clear()
                            route = name
                        },
                        icon = { Icon(icons[i], labels[i]) },
                        label = { Text(labels[i], maxLines = 1) }
                    )
                }
            }
        }) { padding ->
            Row(Modifier.fillMaxSize().padding(padding)) {
                if (tablet && route in roots) NavigationRail {
                    roots.forEachIndexed { i, name ->
                        NavigationRailItem(
                            selected = route == name,
                            onClick = {
                                navStack.clear()
                                route = name
                            },
                            icon = { Icon(icons[i], labels[i]) },
                            label = { Text(labels[i]) }
                        )
                    }
                }
                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(pageScroll).padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(Modifier.widthIn(max = 700.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        if (route in roots) ScreenHeader("ADhils Fitness", action = {
                            FilledTonalButton(onClick = { profiles = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text(state.profile.name.ifBlank { "Athlete" }.take(18) + " ▾")
                            }
                        })
                        when {
                            route == "profile" -> ProfileScreen(state.profile, onSave = { vm.saveProfile(it); back() }, onBack = if (state.profile.onboardingComplete) ({ back() }) else ({ route = "welcome" }))
                            route == "today" -> TodayScreen(
                                state,
                                vm,
                                { navigateTo("checkin") },
                                { navigateTo("active") },
                                ::showExercise,
                                { navigateTo("workouts") },
                                onCameraExercise = { exId ->
                                    navigateTo("camera:$exId")
                                }
                            )
                            route == "checkin" -> CheckInScreen(state.profile, { vm.start(it); navigateTo("active") }, { back() }) { check ->
                                val targetNote = if (check.targetMuscles.isNotEmpty()) " Target muscles: ${check.targetMuscles.joinToString(", ")}."
                                else if (check.focus.startsWith("Fresh") || check.focus.startsWith("Auto")) " Freshest recovered muscles: ${Recovery.freshestMuscles(state, 4).joinToString(", ")}."
                                else ""
                                val prompt = "I am feeling ${check.feeling} today (energy ${check.energy}/5, soreness ${check.soreness}/2). My focus today is ${check.focus} and I have ${check.minutes} minutes.$targetNote Please propose today's workout exercise list using the available catalog exercises (with replacePlan: true)."
                                vm.ask(prompt, false)
                                navigateTo("coach")
                            }
                            route == "workouts" -> LibraryScreen(
                                state,
                                ::showExercise,
                                { vm.start(CheckIn(minutes = state.profile.minutes), it); navigateTo("active") },
                                { navigateTo("summary:$it") },
                                onCameraExercise = { exId ->
                                    navigateTo("camera:$exId")
                                }
                            )
                            route == "active" -> {
                                val s = state.active ?: run {
                                    vm.start(CheckIn(minutes = state.profile.minutes))
                                    state.active
                                }
                                if (s != null) {
                                    WorkoutScreen(
                                        state = state,
                                        s = s,
                                        vm = vm,
                                        onBack = ::back,
                                        onExercise = ::showExercise,
                                        onCamera = { navigateTo("camera") },
                                        onFinish = {
                                            vm.finish()
                                            val estCalories = (s.results.size * 28).coerceIn(80, 800)
                                            PlatformHealth.syncWorkout(s.id, estCalories)
                                            navigateTo("summary:${s.id}")
                                        }
                                    )
                                } else {
                                    EmptyState("Getting your workout ready", "Your sets are saved as you train.")
                                }
                            }
                            route.startsWith("exercise:") -> ExerciseScreen(
                                id = route.substringAfter(":"),
                                state = state,
                                vm = vm,
                                onBack = ::back,
                                onOpenHowTo = ::showHowTo,
                                onOpenCameraCoach = {
                                    navigateTo("camera:" + route.substringAfter(":"))
                                }
                            )
                            route.startsWith("howto:") -> {
                                val raw = route.substringAfter(":")
                                val exId = raw.substringBefore(":")
                                val tabIndex = raw.substringAfter(":", "").toIntOrNull() ?: 0
                                HowToScreen(
                                    exerciseId = exId,
                                    onBack = ::back,
                                    youtubeUrl = state.videos[exId],
                                    initialTab = tabIndex
                                )
                            }
                            route.startsWith("summary:") -> state.sessions.find { it.id == route.substringAfter(":") }?.let { SummaryScreen(it, state, vm, { navStack.clear(); route = "today" }, { navigateTo("coach") }) }
                            route == "progress" -> ProgressScreen(state, vm)
                            route == "coach" -> CoachScreen(state, vm, { navigateTo("active") })
                            route == "settings" -> SettingsScreen(state, vm, { navigateTo("profile") }, { profiles = true })
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
    if (profiles) ProfilePicker(store, onSelect = { vm.selectProfile(it); profiles = false }, onAdd = { vm.addProfile(it); profiles = false }, onDismiss = { profiles = false })
    if (leave) AlertDialog(
        onDismissRequest = { leave = false },
        title = { Text("Leave this workout?") },
        text = { Text("Your completed sets are saved for ${state.profile.name}. You can resume from Today.") },
        confirmButton = { TextButton(onClick = { leave = false; route = "today" }) { Text("Save and leave") } },
        dismissButton = {
            Row {
                TextButton(onClick = { leave = false }) { Text("Continue") }
                TextButton(onClick = { leave = false; discard = true }) { Text("Discard") }
            }
        }
    )
    if (discard) AlertDialog(
        onDismissRequest = { discard = false },
        title = { Text("Discard this workout?") },
        text = { Text("This removes its logged sets from this profile.") },
        confirmButton = { TextButton(onClick = { vm.discard(); discard = false; route = "today" }) { Text("Discard workout") } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text("Keep it") } }
    )
}
