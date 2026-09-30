package com.adhils.fitness

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.adhils.fitness.core.*
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneId

fun youtubePostureUrl(e: Exercise, savedUri: String?): String {
    if (!savedUri.isNullOrBlank() && (savedUri.startsWith("https://") || savedUri.startsWith("http://"))) {
        return savedUri
    }
    val q = URLEncoder.encode("${e.name} exercise proper form posture tutorial short", "UTF-8")
    return "https://m.youtube.com/results?search_query=$q"
}

@SuppressLint("SetJavaScriptEnabled")
@Composable fun ExercisePostureMediaCard(e: Exercise, state: AppState, vm: FitnessViewModel, onOpenCameraCoach: (() -> Unit)? = null) {
    val context = LocalContext.current
    val originProfile = remember { vm.store.value.selectedId }
    val savedUri = state.videos[e.id]
    val isLocalVideo = savedUri != null && (savedUri.startsWith("content://") || savedUri.startsWith("file://"))
    var playYouTube by remember(e.id) { mutableStateOf(false) }
    var currentWebUrl by remember(e.id, savedUri) { mutableStateOf(youtubePostureUrl(e, savedUri)) }
    var showUrlDialog by remember(e.id) { mutableStateOf(false) }
    var customUrlInput by remember(e.id, savedUri) {
        mutableStateOf(if (savedUri != null && savedUri.startsWith("http")) savedUri else "")
    }
    val postureFeedback by vm.livePostureFeedback.collectAsState()
    val postureBusy by vm.postureBusy.collectAsState()
    val chooseVideo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                vm.video(e.id, uri.toString(), originProfile)
                playYouTube = false
            }.onFailure { vm.error.value = "Could not retain access to that video. Try a file stored on this device." }
        }
    }
    PanelCard {
        if (playYouTube) {
            Box(Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(14.dp))) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            webChromeClient = WebChromeClient()
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    if (url.startsWith("http://") || url.startsWith("https://")) {
                                        currentWebUrl = url
                                        return false
                                    }
                                    return true
                                }
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    if (!url.isNullOrBlank()) currentWebUrl = url
                                }
                            }
                            loadUrl(youtubePostureUrl(e, savedUri))
                        }
                    },
                    onRelease = { it.destroy() },
                    modifier = Modifier.fillMaxSize()
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { playYouTube = false }, modifier = Modifier.weight(1f)) { Text("Show illustration") }
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
                    Text(if (currentWebUrl.contains("watch") || currentWebUrl.contains("shorts")) "Pin this video" else "Set YouTube link")
                }
            }
        } else if (isLocalVideo && savedUri != null) {
            AndroidView(factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoURI(Uri.parse(savedUri))
                    setMediaController(MediaController(ctx))
                    setOnPreparedListener { seekTo(1) }
                    setOnErrorListener { _, _, _ -> vm.error.value = "This personal video is no longer available. Attach it again."; true }
                }
            }, modifier = Modifier.fillMaxWidth().height(220.dp))
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
            SmallLabel("Tap Play to load YouTube posture videos right here · ${e.view.lowercase()} view for camera tracking")
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { vm.askPostureFeedback(e, PoseObservation(formScore = 92, status = "Reviewing reference video posture"), currentWebUrl) },
                enabled = !postureBusy,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (postureBusy) "AI reviewing video…" else "🤖 AI Video Posture Checkpoints")
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
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(youtubePostureUrl(e, savedUri)))
                runCatching { context.startActivity(intent) }
            }) {
                Icon(Icons.Default.OndemandVideo, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Open in YouTube")
            }
            TextButton(onClick = { showUrlDialog = true }) { Text("YouTube URL") }
            TextButton(onClick = { chooseVideo.launch(arrayOf("video/*")) }) {
                Text(if (isLocalVideo) "Replace file" else "Local video")
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

@Composable fun ExerciseScreen(id:String,state:AppState,vm:FitnessViewModel,onBack:()->Unit) {
    val e=Catalog.get(id)
    ScreenHeader(e.name,onBack)
    ExercisePostureMediaCard(e, state, vm)
    SmallLabel("${e.equipment} · ${e.muscles}")
    SectionTitle("How to perform")
    e.instructions.forEachIndexed {i,text->Text("${i+1}. $text")}
    SectionTitle("Breathing",e.breathing)
    PanelCard {SectionTitle("AI Camera & Posture placement","${e.view} view (${e.effectiveCamera} biomechanics). Keep your shoulders, hands, hips, and feet in frame. Use a stable stand.");SmallLabel("Tracks live joint angles and gives rep-by-rep posture corrections for ${e.name}.")}
    val alternatives=Catalog.alternatives(id,state.profile)
    if(alternatives.isNotEmpty()) {SectionTitle("Alternatives for this profile");alternatives.forEach {Text("• ${it.name}")}}
}
@Composable fun SummaryScreen(s:Session,state:AppState,vm:FitnessViewModel,onDone:()->Unit,onCoach:()->Unit) {
    ScreenHeader("Workout summary",onDone)
    SectionTitle(s.title,Instant.ofEpochMilli(s.startedAt).atZone(ZoneId.systemDefault()).toLocalDate().toString())
    val totalVolKg=remember(s.results) { Recovery.sessionVolumeKg(s) }
    PanelCard {
        Text("${s.results.size} logged sets · ${durationText((s.finishedAt ?: System.currentTimeMillis())-s.startedAt)} elapsed")
        Text("Total Session Volume (Tonnage): ${weightLabel(totalVolKg,state.profile)}",color=MaterialTheme.colorScheme.primary)
        SmallLabel("Elapsed time includes breaks and time away from the app.")
    }
    s.plan.forEach {p->val results=s.results.filter {it.exerciseId==p.exerciseId};val e=Catalog.get(p.exerciseId)
        if(results.isNotEmpty()) PanelCard {
            SectionTitle(e.name)
            results.sortedBy {it.setIndex}.forEach {r->
                val typeTag=if(r.setType!="Working") " [${r.setType}]" else ""
                SmallLabel("Set ${r.setIndex+1}$typeTag: "+if(e.timed) "${r.seconds} seconds" else "${weightLabel(r.weightKg,state.profile)}${if(e.perHand) " / hand" else ""} × ${r.reps}")
                r.observations.forEach { obs -> SmallLabel("  🎥 $obs") }
            }
            if(!e.timed) {
                val best1Rm=results.filter { !it.warmup }.maxOfOrNull { Recovery.estimate1RMKg(it.weightKg,it.reps) } ?: 0.0
                if(best1Rm>0.0) SmallLabel("Session Est. 1RM: ${weightLabel(best1Rm,state.profile)}")
                Text(Training.nextLoad(e.id,state).reason)
            }
        }
    }
    var saved by remember {mutableStateOf(false)}
    OutlinedButton(onClick={vm.saveRoutine(s.title,s.plan);saved=true},enabled=!saved,modifier=Modifier.fillMaxWidth()) {Text(if(saved) "Routine saved" else "Save as a routine")}
    OutlinedButton(onClick=onCoach,modifier=Modifier.fillMaxWidth()) {Text("Ask your coach")}
    PrimaryButton("Back to Today",onClick=onDone)
}
@Composable fun ProgressScreen(state:AppState,vm:FitnessViewModel) {
    val p=state.profile;val completed=state.sessions.filter {it.finishedAt!=null}
    val recovery=remember(state) { Recovery.calculate(state) }
    SectionTitle("Recovery & Analytics","${p.name} · Anatomical fatigue heat map, tonnage & 1RM trends.")
    MuscleHeatmapCard(recovery,defaultExpanded=true)
    if(completed.isEmpty()) EmptyState("Your first milestone awaits","Complete a workout to see your exercise trends, tonnage, and personal records.")
    else {
        val since=System.currentTimeMillis()-28L*24*3600*1000
        val recentVolKg=completed.filter {it.finishedAt!!>=since}.sumOf { Recovery.sessionVolumeKg(it) }
        val totalVolKg=completed.sumOf { Recovery.sessionVolumeKg(it) }
        PanelCard {
            Text("${completed.count {it.finishedAt!!>=since}} workouts in the last 28 days",style=MaterialTheme.typography.titleLarge)
            Text("28-Day Volume: ${weightLabel(recentVolKg,p)} · Lifetime Volume: ${weightLabel(totalVolKg,p)}",color=MaterialTheme.colorScheme.primary)
            SmallLabel("${completed.size} completed workouts · ${completed.sumOf {it.results.count {r->!r.warmup}}} working sets total")
        }
        val ids=completed.flatMap {it.results}.map {it.exerciseId}.distinct()
        var id by remember {mutableStateOf(ids.first())};var open by remember {mutableStateOf(false)}
        Box {OutlinedButton(onClick={open=true}) {Text(Catalog.get(id).name+" ▾")};DropdownMenu(open,{open=false}) {ids.forEach {key->DropdownMenuItem(text={Text(Catalog.get(key).name)},onClick={id=key;open=false})}}}
        val e=Catalog.get(id)
        val values=completed.mapNotNull {s->s.results.filter {it.exerciseId==id && !it.warmup}.takeIf {it.isNotEmpty()}?.let {sets->
            (s.finishedAt!! to if(e.timed) sets.maxOf {it.seconds}.toDouble() else fromKg(sets.maxOf {it.weightKg},p.unit))
        }}.sortedBy {it.first}
        val records=completed.flatMap {it.results}.filter {it.exerciseId==id && !it.warmup}
        val best1RmKg=Recovery.best1RMKg(state,id)
        PanelCard {
            if(records.isNotEmpty()) {
                Text(if(e.timed) "Longest hold · ${records.maxOf {it.seconds}} sec" else "Highest logged weight · ${weightLabel(records.maxOf {it.weightKg},p)}${if(e.perHand) " / hand" else ""}")
                if(!e.timed) {
                    Text("Estimated 1RM (Epley) · ${weightLabel(best1RmKg,p)}${if(e.perHand) " / hand" else ""}",color=MaterialTheme.colorScheme.primary)
                    Text("Most reps · ${records.maxOf {it.reps}}")
                }
            }
            if(values.size<2) SmallLabel("Log this exercise in another workout to see a trend.")
            else {
                val accent=MaterialTheme.colorScheme.primary;val grid=MaterialTheme.colorScheme.outline
                val low=values.minOf {it.second};val high=values.maxOf {it.second};val range=(high-low).coerceAtLeast(1.0)
                SmallLabel("Highest logged ${if(e.timed) "hold (seconds)" else "weight (${p.unit}${if(e.perHand) " / hand" else ""})"} per session")
                Canvas(Modifier.fillMaxWidth().height(150.dp)) {
                    drawLine(grid,Offset(8f,size.height-8),Offset(size.width-8,size.height-8),1.dp.toPx())
                    val first=values.first().first;val span=(values.last().first-first).coerceAtLeast(1)
                    val points=values.map {Offset(12+(it.first-first).toFloat()/span*(size.width-24),(size.height-16-((it.second-low)/range*(size.height-32))).toFloat())}
                    points.zipWithNext().forEach {(a,b)->drawLine(accent,a,b,3.dp.toPx())};points.forEach {drawCircle(accent,4.dp.toPx(),it)}
                }
                SmallLabel("Range: ${decimal(low)}–${decimal(high)} · ${values.size} sessions")
                values.takeLast(4).forEach {SmallLabel(Instant.ofEpochMilli(it.first).atZone(ZoneId.systemDefault()).toLocalDate().toString()+" · "+decimal(it.second))}
            }
        }
    }
    SectionTitle("Body weight")
    var weight by remember {mutableStateOf("")}
    OutlinedTextField(weight,{weight=it},label={Text("Weight (${p.unit})")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),singleLine=true)
    val kg=weight.toDoubleOrNull()?.let {toKg(it,p.unit)}
    Button(onClick={vm.bodyWeight(kg!!);weight=""},enabled=kg!=null && kg.isFinite() && kg in 1.0..650.0) {Text("Record weight")}
    state.measurements.takeLast(5).reversed().forEach {SmallLabel(Instant.ofEpochMilli(it.at).atZone(ZoneId.systemDefault()).toLocalDate().toString()+" · "+weightLabel(it.weightKg,p))}
}
