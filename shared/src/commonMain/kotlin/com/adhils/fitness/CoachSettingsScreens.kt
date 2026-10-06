package com.adhils.fitness

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.adhils.fitness.core.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CoachScreen(state: AppState, vm: FitnessViewModel, onOpenWorkout: (() -> Unit)? = null) {
    val busy by vm.busy.collectAsState()
    val suggestion by vm.proposal.collectAsState()
    var message by remember { mutableStateOf("") }
    var feeling by remember { mutableStateOf("Good") }
    var focus by remember { mutableStateOf("Freshest Muscles (Auto)") }
    var targetMuscles by remember { mutableStateOf(emptySet<String>()) }
    var minutes by remember { mutableIntStateOf(state.profile.minutes) }
    var showBuilder by remember { mutableStateOf(true) }
    val freshest = remember(state) { Recovery.freshestMuscles(state, 4) }
    val week = state.sessions.filter { it.finishedAt != null && it.finishedAt!! >= nowMillis() - 7 * 86400000L }

    SectionTitle("Your coach", "Advice & workout builder for ${state.profile.name} · ${state.profile.goal}")
    PanelCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SectionTitle("Customize today’s workout", if (state.active != null) "Active: ${state.active!!.title}" else "No workout started yet")
            TextButton(onClick = { showBuilder = !showBuilder }) { Text(if (showBuilder) "Hide" else "Show") }
        }
        if (showBuilder) {
            SmallLabel("HOW ARE YOU FEELING TODAY?")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Energized", "Good", "Tired", "Sore / Stiff", "Recovering").forEach { item ->
                    FilterChip(selected = feeling == item, onClick = { feeling = item }, label = { Text(item) })
                }
            }
            SmallLabel("WHAT IS YOUR FOCUS TODAY?")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "Freshest Muscles (Auto)",
                    "Full Body",
                    "Upper Body",
                    "Lower Body",
                    "Push (Chest & Shoulders)",
                    "Pull (Back & Biceps)",
                    "Shoulders & Arms",
                    "Core & Mobility"
                ).forEach { item ->
                    FilterChip(selected = focus == item, onClick = { focus = item }, label = { Text(item) })
                }
            }
            SmallLabel("TIME AVAILABLE: ${minutes}m")
            Slider(value = minutes.toFloat(), onValueChange = { minutes = it.toInt() }, valueRange = 15f..90f, steps = 14)
            PrimaryButton("Generate Workout With AI", !busy) {
                val targetNote = if (targetMuscles.isNotEmpty()) " Target muscles: ${targetMuscles.joinToString(", ")}."
                else if (focus.startsWith("Fresh") || focus.startsWith("Auto")) " Freshest recovered muscles: ${freshest.joinToString(", ")}."
                else ""
                val prompt = "I feel $feeling today. Focus: $focus. Time: $minutes min.$targetNote Propose today's workout plan."
                vm.ask(prompt, false)
            }
        }
    }
    suggestion?.let { reply ->
        PanelCard {
            SectionTitle("Coach Proposal", reply.explanation)
            reply.proposal?.let { change ->
                Text("Proposed: ${change.title ?: "Workout adjustments"} (${change.changes.size} exercises)")
                PrimaryButton("Accept & Replace Plan") { vm.applyProposal(); onOpenWorkout?.invoke() }
            }
        }
    }
    SectionTitle("Chat with Coach")
    state.chats.takeLast(10).forEach { entry ->
        PanelCard {
            SmallLabel(if (entry.role == "you") "YOU" else "AI COACH")
            Text(entry.text)
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Ask your coach anything...") }, modifier = Modifier.weight(1f))
        Button(onClick = { vm.ask(message); message = "" }, enabled = message.isNotBlank() && !busy) { Text("Send") }
    }
}

@Composable
fun SettingsScreen(state: AppState, vm: FitnessViewModel, onProfile: () -> Unit, onProfiles: () -> Unit) {
    val status by vm.connectionStatus.collectAsState()
    val busy by vm.busy.collectAsState()
    var pairing by remember { mutableStateOf(false) }
    var invitation by remember { mutableStateOf("") }
    var clear by remember { mutableStateOf(false) }

    SectionTitle("Settings", "Personalize your training and connect your devices.")
    PanelCard {
        SectionTitle(state.profile.name, "${state.profile.experience} · ${state.profile.days} days a week")
        Row {
            TextButton(onClick = onProfile) { Text("Edit profile") }
            TextButton(onClick = onProfiles) { Text("Switch or add profile") }
        }
        SmallLabel("Workouts, progress and coach context are kept separately for each profile.")
    }
    PanelCard {
        SectionTitle("Appearance & preferences")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Dark", "Light", "System").forEach { theme ->
                FilterChip(selected = state.profile.theme == theme, onClick = { vm.edit { it.copy(profile = it.profile.copy(theme = theme)) } }, label = { Text(theme) })
            }
        }
        SettingSwitch("Spoken movement cues", state.profile.voice) { value -> vm.edit { it.copy(profile = it.profile.copy(voice = value)) } }
        SettingSwitch("Experimental movement cues", state.profile.experimentalCues) { value -> vm.edit { it.copy(profile = it.profile.copy(experimentalCues = value)) } }
    }
    PanelCard {
        SectionTitle("Health & Wearables", if (state.profile.healthSyncEnabled) "Connected" else "Not connected")
        val syncText = if (state.profile.healthSyncEnabled) "Wearable Health Sync: Active" else "Wearable Health Sync: Disabled"
        Text(syncText)
        val wearable = HealthWearableManager.connectedDevice.collectAsState().value
        if (wearable != null && state.profile.healthSyncEnabled) {
            Text("${wearable.name} · ${wearable.batteryPct}% battery")
            Text("❤️ Live Heart Rate: ${wearable.liveHeartRateBpm} bpm · 🔥 Calories Today: ${wearable.caloriesBurnedToday} kcal")
            Text("👟 Steps Today: ${wearable.stepsToday} steps")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val nextSync = !state.profile.healthSyncEnabled
                    vm.edit { it.copy(profile = it.profile.copy(healthSyncEnabled = nextSync)) }
                    HealthWearableManager.setHealthSyncEnabled(nextSync)
                }
            ) {
                Text(if (state.profile.healthSyncEnabled) "Disconnect Health Sync" else "Connect Health Sync")
            }
        }
    }
    PanelCard {
        val notifStatus = if (state.profile.workoutPreviewEnabled) "Active" else "Disabled"
        SectionTitle("Workout Reminders & Notifications", notifStatus)
        SettingSwitch("Daily workout previews", state.profile.workoutPreviewEnabled) { enabled ->
            vm.edit { it.copy(profile = it.profile.copy(workoutPreviewEnabled = enabled)) }
            if (enabled) {
                WorkoutNotificationManager.scheduleWorkoutPreview(null, state.profile.workoutPreviewTime)
            } else {
                WorkoutNotificationManager.cancelWorkoutPreview(null)
            }
        }
        Text("Scheduled preview time: ${state.profile.workoutPreviewTime} on training days")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    WorkoutNotificationManager.sendWorkoutPreviewNotification(
                        previewTime = state.profile.workoutPreviewTime,
                        title = "WORKOUT PREVIEWS ACTIVE 🏋️",
                        message = "Scheduled for ${state.profile.workoutPreviewTime} on training days"
                    )
                }
            ) {
                Text("Test Notification")
            }
        }
    }
    PanelCard {
        SectionTitle("Reset & Maintenance")
        TextButton(onClick = { clear = true }) {
            Text("Clear ${state.profile.name}'s training data", color = MaterialTheme.colorScheme.error)
        }
        SmallLabel("ADhils Fitness 0.1.4 · Multiplatform")
    }

    if (clear) AlertDialog(
        onDismissRequest = { clear = false },
        title = { Text("Clear this profile's data?") },
        text = { Text("Remove ${state.profile.name}'s workouts, measurements, routines and chats. Profile settings are preserved.") },
        confirmButton = { TextButton(onClick = { vm.clearProfileData(); clear = false }) { Text("Clear data") } },
        dismissButton = { TextButton(onClick = { clear = false }) { Text("Cancel") } }
    )
}

@Composable
private fun SettingSwitch(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(label)
        Switch(checked = value, onCheckedChange = onChange)
    }
}
