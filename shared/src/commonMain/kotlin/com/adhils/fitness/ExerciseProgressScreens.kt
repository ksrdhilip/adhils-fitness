package com.adhils.fitness

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adhils.fitness.core.*

fun youtubePostureUrl(e: Exercise, savedUri: String?): String {
    if (!savedUri.isNullOrBlank() && (savedUri.startsWith("https://") || savedUri.startsWith("http://"))) {
        return savedUri
    }
    val cleanName = e.name.replace("&", "and").replace("/", " ")
    val query = "$cleanName exercise proper form posture tutorial short"
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .joinToString("+")
    return "https://m.youtube.com/results?search_query=$query"
}

@Composable
fun ExercisePostureMediaCard(e: Exercise, state: AppState, vm: FitnessViewModel, onOpenCameraCoach: (() -> Unit)? = null) {
    val originProfile = remember { vm.store.value.selectedId }
    val savedUri = state.videos[e.id]
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    var playYouTube by remember(e.id) { mutableStateOf(false) }
    var currentWebUrl by remember(e.id, savedUri) { mutableStateOf(youtubePostureUrl(e, savedUri)) }
    var showUrlDialog by remember(e.id) { mutableStateOf(false) }
    var customUrlInput by remember(e.id, savedUri) {
        mutableStateOf(if (savedUri != null && savedUri.startsWith("http")) savedUri else "")
    }
    val postureFeedback by vm.livePostureFeedback.collectAsState()
    val postureBusy by vm.postureBusy.collectAsState()

    PanelCard {
        if (playYouTube) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                PlatformWebView(
                    url = youtubePostureUrl(e, savedUri),
                    modifier = Modifier.fillMaxSize(),
                    onUrlChange = { newUrl ->
                        currentWebUrl = newUrl
                    }
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { playYouTube = false }, modifier = Modifier.weight(1f)) {
                    Text("Show illustration")
                }
                OutlinedButton(
                    onClick = {
                        if (currentWebUrl.contains("watch") || currentWebUrl.contains("shorts") || currentWebUrl.contains("youtu.be")) {
                            vm.video(e.id, currentWebUrl, originProfile)
                        } else {
                            showUrlDialog = true
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (currentWebUrl.contains("watch") || currentWebUrl.contains("shorts") || currentWebUrl.contains("youtu.be"))
                            "Pin this video"
                        else
                            "Set YouTube link"
                    )
                }
            }
        } else {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                PlacementDrawing(e)
                FilledTonalButton(
                    onClick = { playYouTube = true },
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play posture video")
                    Spacer(Modifier.width(8.dp))
                    Text("Play YouTube Posture Video")
                }
            }
            SmallLabel("Tap Play to search & load YouTube tutorials directly · ${e.view.lowercase()} view for camera tracking")
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { vm.askPostureFeedback(e, PoseObservation(formScore = 92, status = "Reviewing reference posture"), youtubePostureUrl(e, savedUri)) },
                enabled = !postureBusy,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (postureBusy) "AI reviewing posture…" else "🤖 AI Posture Guide")
            }
            if (onOpenCameraCoach != null) {
                Button(onClick = onOpenCameraCoach, modifier = Modifier.weight(1f)) {
                    Text("🎥 Live Rep Coach")
                }
            }
        }

        postureFeedback?.let { tip ->
            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SmallLabel("AI POSTURE COACH · NEXT REP FOCUS")
                    Text(tip, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = {
                val urlToOpen = youtubePostureUrl(e, savedUri)
                runCatching { uriHandler.openUri(urlToOpen) }
            }) {
                Icon(Icons.Default.OndemandVideo, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Open in YouTube")
            }
            TextButton(onClick = { showUrlDialog = true }) {
                Text(if (savedUri != null && savedUri.startsWith("http")) "Custom link" else "YouTube URL")
            }
        }
    }

    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("YouTube posture link for ${e.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste a YouTube video or Short link to open directly when you tap Play, or leave blank to search YouTube automatically.")
                    OutlinedTextField(
                        value = customUrlInput,
                        onValueChange = { customUrlInput = it.take(500) },
                        label = { Text("https://www.youtube.com/watch?v=...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = customUrlInput.trim()
                    if (trimmed.startsWith("https://") || trimmed.startsWith("http://")) {
                        vm.video(e.id, trimmed, originProfile)
                    } else if (trimmed.isEmpty()) {
                        vm.edit(originProfile) { it.copy(videos = it.videos - e.id) }
                    }
                    showUrlDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showUrlDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun ExerciseScreen(
    id: String,
    state: AppState,
    vm: FitnessViewModel,
    onBack: () -> Unit,
    onOpenCameraCoach: (() -> Unit)? = null
) {
    val e = Catalog.get(id)
    ScreenHeader(e.name, onBack)
    ExercisePostureMediaCard(e, state, vm, onOpenCameraCoach = onOpenCameraCoach)
    SmallLabel("${e.equipment} · ${e.muscles}")
    SectionTitle("How to perform")
    e.instructions.forEachIndexed { i, text -> Text("${i + 1}. $text") }
    SectionTitle("Breathing", e.breathing)
    PanelCard {
        SectionTitle("AI Camera & Posture placement", "${e.view} view (${e.effectiveCamera} biomechanics). Keep your shoulders, hands, hips, and feet in frame. Use a stable stand.")
        SmallLabel("Tracks live joint angles and gives rep-by-rep posture corrections for ${e.name}.")
    }
    val alternatives = Catalog.alternatives(id, state.profile)
    if (alternatives.isNotEmpty()) {
        SectionTitle("Alternatives for this profile")
        alternatives.forEach { Text("• ${it.name}") }
    }
}

@Composable
fun SummaryScreen(s: Session, state: AppState, vm: FitnessViewModel, onDone: () -> Unit, onCoach: () -> Unit) {
    ScreenHeader("Workout summary", onDone)
    val cleanSessionTitle = remember(s.title) {
        if (s.title.contains("Fresh Muscle") || s.title.matches(Regex(".* [A-D]( · .*)?"))) {
            val chosenEx = s.plan.mapNotNull { Catalog.byId[it.exerciseId] }
            if (chosenEx.isNotEmpty()) {
                val clean = Training.computeWorkoutTitle(chosenEx)
                val suffix = if (s.title.contains(" · Light")) " · Light" else if (s.title.contains(" · Strong")) " · Strong" else ""
                "$clean$suffix"
            } else s.title
        } else s.title
    }
    SectionTitle(cleanSessionTitle, formatDate(s.startedAt))

    val nextWorkout = remember(s.id, state.sessions) {
        runCatching {
            val completedSessions = if (state.sessions.any { it.id == s.id && it.finishedAt != null }) {
                state.sessions
            } else {
                state.sessions.filterNot { it.id == s.id } + s.copy(finishedAt = s.finishedAt ?: nowMillis())
            }
            val nextState = state.copy(sessions = completedSessions)
            Training.generate(nextState, CheckIn(minutes = state.profile.minutes, focus = "Freshest Muscles (Auto)"))
        }.getOrNull()
    }
    val nextWorkoutTitle = nextWorkout?.title ?: "Next Scheduled Routine"

    // Celebratory Next Workout Card
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🚀", fontSize = 20.sp)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "GREAT SESSION COMPLETED!",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Mint
                    )
                    Text(
                        "Next workout is $nextWorkoutTitle",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            if (nextWorkout != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallLabel("${nextWorkout.plan.size} planned exercises · ${state.profile.minutes} min")
                    val freshMuscles = remember(state) { Recovery.freshestMuscles(state, 3) }
                    if (freshMuscles.isNotEmpty()) {
                        Text(
                            "Focus: ${freshMuscles.joinToString(", ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    val totalVolKg = remember(s.results) { Recovery.sessionVolumeKg(s) }
    PanelCard {
        Text("${s.results.size} logged sets · ${durationText((s.finishedAt ?: nowMillis()) - s.startedAt)} elapsed")
        Text("Total Session Volume (Tonnage): ${weightLabel(totalVolKg, state.profile)}", color = MaterialTheme.colorScheme.primary)
        SmallLabel("Elapsed time includes breaks and time away from the app.")
    }
    s.plan.forEach { p ->
        val results = s.results.filter { it.exerciseId == p.exerciseId }
        val e = Catalog.get(p.exerciseId)
        if (results.isNotEmpty()) PanelCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExerciseThumbnail(
                    exerciseId = e.id,
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentDescription = e.name
                )
                Column(Modifier.weight(1f)) {
                    SectionTitle(e.name)
                }
            }
            results.sortedBy { it.setIndex }.forEach { r ->
                val typeTag = if (r.setType != "Working") " [${r.setType}]" else ""
                SmallLabel("Set ${r.setIndex + 1}$typeTag: " + if (e.timed) "${r.seconds} seconds" else "${weightLabel(r.weightKg, state.profile)}${if (e.perHand) " / hand" else ""} × ${r.reps}")
                r.observations.forEach { obs -> SmallLabel("  🎥 $obs") }
            }
            if (!e.timed) {
                val best1Rm = results.filter { !it.warmup }.maxOfOrNull { Recovery.estimate1RMKg(it.weightKg, it.reps) } ?: 0.0
                if (best1Rm > 0.0) SmallLabel("Session Est. 1RM: ${weightLabel(best1Rm, state.profile)}")
                Text(Training.nextLoad(e.id, state).reason)
            }
        }
    }
    var saved by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { vm.saveRoutine(s.title, s.plan); saved = true }, enabled = !saved, modifier = Modifier.fillMaxWidth()) { Text(if (saved) "Routine saved" else "Save as a routine") }
    OutlinedButton(onClick = onCoach, modifier = Modifier.fillMaxWidth()) { Text("Ask your coach") }
    PrimaryButton("Back to Today · Up next: $nextWorkoutTitle", onClick = onDone)
}

@Composable
fun ProgressScreen(state: AppState, vm: FitnessViewModel) {
    val p = state.profile
    val completed = state.sessions.filter { it.finishedAt != null }
    val recovery = remember(state) { Recovery.calculate(state) }
    val daysSinceLastWorkout = remember(completed) {
        val last = completed.maxOfOrNull { it.finishedAt ?: 0L } ?: 0L
        if (last == 0L) 0 else ((nowMillis() - last) / (24L * 3600 * 1000)).toInt()
    }
    MuscleHeatmapCard(recovery, daysSinceLastWorkout = daysSinceLastWorkout, defaultExpanded = true)
    if (completed.isEmpty()) EmptyState("Your first milestone awaits", "Complete a workout to see your exercise trends, tonnage, and personal records.")
    else {
        val since = nowMillis() - 28L * 24 * 3600 * 1000
        val recentVolKg = completed.filter { it.finishedAt!! >= since }.sumOf { Recovery.sessionVolumeKg(it) }
        val totalVolKg = completed.sumOf { Recovery.sessionVolumeKg(it) }
        PanelCard {
            Text("${completed.count { it.finishedAt!! >= since }} workouts in the last 28 days", style = MaterialTheme.typography.titleLarge)
            Text("28-Day Volume: ${weightLabel(recentVolKg, p)} · Lifetime Volume: ${weightLabel(totalVolKg, p)}", color = MaterialTheme.colorScheme.primary)
            SmallLabel("${completed.size} completed workouts · ${completed.sumOf { it.results.count { r -> !r.warmup } }} working sets total")
        }
        val ids = completed.flatMap { it.results }.map { it.exerciseId }.distinct()
        var id by remember { mutableStateOf(ids.first()) }
        var open by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(onClick = { open = true }) { Text(Catalog.get(id).name + " ▾") }
            DropdownMenu(open, { open = false }) {
                ids.forEach { key -> DropdownMenuItem(text = { Text(Catalog.get(key).name) }, onClick = { id = key; open = false }) }
            }
        }
        val e = Catalog.get(id)
        val values = completed.mapNotNull { s ->
            s.results.filter { it.exerciseId == id && !it.warmup }.takeIf { it.isNotEmpty() }?.let { sets ->
                (s.finishedAt!! to if (e.timed) sets.maxOf { it.seconds }.toDouble() else fromKg(sets.maxOf { it.weightKg }, p.unit))
            }
        }.sortedBy { it.first }
        val records = completed.flatMap { it.results }.filter { it.exerciseId == id && !it.warmup }
        val best1RmKg = Recovery.best1RMKg(state, id)
        PanelCard {
            if (records.isNotEmpty()) {
                Text(if (e.timed) "Longest hold · ${records.maxOf { it.seconds }} sec" else "Highest logged weight · ${weightLabel(records.maxOf { it.weightKg }, p)}${if (e.perHand) " / hand" else ""}")
                if (!e.timed) {
                    Text("Estimated 1RM (Epley) · ${weightLabel(best1RmKg, p)}${if (e.perHand) " / hand" else ""}", color = MaterialTheme.colorScheme.primary)
                    Text("Most reps · ${records.maxOf { it.reps }}")
                }
            }
            if (values.size < 2) SmallLabel("Log this exercise in another workout to see a trend.")
            else {
                val accent = MaterialTheme.colorScheme.primary
                val grid = MaterialTheme.colorScheme.outline
                val low = values.minOf { it.second }
                val high = values.maxOf { it.second }
                val range = (high - low).coerceAtLeast(1.0)
                SmallLabel("Highest logged ${if (e.timed) "hold (seconds)" else "weight (${p.unit}${if (e.perHand) " / hand" else ""})"} per session")
                Canvas(Modifier.fillMaxWidth().height(150.dp)) {
                    drawLine(grid, Offset(8f, size.height - 8), Offset(size.width - 8, size.height - 8), 1.dp.toPx())
                    val first = values.first().first
                    val span = (values.last().first - first).coerceAtLeast(1)
                    val points = values.map { Offset(12 + (it.first - first).toFloat() / span * (size.width - 24), (size.height - 16 - ((it.second - low) / range * (size.height - 32))).toFloat()) }
                    points.zipWithNext().forEach { (a, b) -> drawLine(accent, a, b, 3.dp.toPx()) }
                    points.forEach { drawCircle(accent, 4.dp.toPx(), it) }
                }
                SmallLabel("Range: ${decimal(low)}–${decimal(high)} · ${values.size} sessions")
                values.takeLast(4).forEach { SmallLabel(formatDate(it.first) + " · " + decimal(it.second)) }
            }
        }
    }
    SectionTitle("Body weight")
    var weight by remember { mutableStateOf("") }
    OutlinedTextField(weight, { weight = it }, label = { Text("Weight (${p.unit})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
    val kg = weight.toDoubleOrNull()?.let { toKg(it, p.unit) }
    Button(onClick = { vm.bodyWeight(kg!!); weight = "" }, enabled = kg != null && kg.isFinite() && kg in 1.0..650.0) { Text("Record weight") }
    state.measurements.takeLast(5).reversed().forEach { SmallLabel(formatDate(it.at) + " · " + weightLabel(it.weightKg, p)) }
}
