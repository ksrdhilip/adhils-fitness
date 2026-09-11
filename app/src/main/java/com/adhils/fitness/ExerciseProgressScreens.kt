package com.adhils.fitness

import android.content.Intent
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.adhils.fitness.core.*
import java.time.Instant
import java.time.ZoneId

@Composable fun ExerciseScreen(id:String,state:AppState,vm:FitnessViewModel,onBack:()->Unit) {
    val e=Catalog.get(id);val context=LocalContext.current
    val originProfile=remember {vm.store.value.selectedId}
    val chooseVideo=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {uri->if(uri!=null) {
        runCatching {context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);vm.video(id,uri.toString(),originProfile)}
            .onFailure {vm.error.value="Could not retain access to that video. Try a file stored on this device."}
    }}
    ScreenHeader(e.name,onBack)
    val video=state.videos[id]
    if(video!=null) AndroidView(factory={ctx->VideoView(ctx).apply {
        setVideoURI(Uri.parse(video));setMediaController(MediaController(ctx));setOnPreparedListener {seekTo(1)}
        setOnErrorListener {_,_,_->vm.error.value="This personal video is no longer available. Attach it again.";true}
    }},modifier=Modifier.fillMaxWidth().height(220.dp))
    else PanelCard {PlacementDrawing(e);SmallLabel("Placement illustration · ${e.view.lowercase()} view for camera-supported exercises")}
    TextButton(onClick={chooseVideo.launch(arrayOf("video/*"))}) {Text(if(video==null) "Attach your own demonstration video" else "Replace personal video")}
    SmallLabel("${e.equipment} · ${e.muscles}")
    SectionTitle("How to perform")
    e.instructions.forEachIndexed {i,text->Text("${i+1}. $text")}
    SectionTitle("Breathing",e.breathing)
    if(e.camera!=null) PanelCard {SectionTitle("Camera placement","${e.view} view. Keep your shoulders, hands, hips, and feet in frame. Use a stable stand.");SmallLabel("Supported variation: ${e.name}. Other variations may not track correctly.")}
    val alternatives=Catalog.alternatives(id,state.profile)
    if(alternatives.isNotEmpty()) {SectionTitle("Alternatives for this profile");alternatives.forEach {Text("• ${it.name}")}}
}
@Composable fun SummaryScreen(s:Session,state:AppState,vm:FitnessViewModel,onDone:()->Unit,onCoach:()->Unit) {
    ScreenHeader("Workout summary",onDone)
    SectionTitle(s.title,Instant.ofEpochMilli(s.startedAt).atZone(ZoneId.systemDefault()).toLocalDate().toString())
    PanelCard {
        Text("${s.results.size} logged sets · ${durationText((s.finishedAt ?: System.currentTimeMillis())-s.startedAt)} elapsed")
        SmallLabel("Elapsed time includes breaks and time away from the app.")
    }
    s.plan.forEach {p->val results=s.results.filter {it.exerciseId==p.exerciseId};val e=Catalog.get(p.exerciseId)
        if(results.isNotEmpty()) PanelCard {
            SectionTitle(e.name)
            results.sortedBy {it.setIndex}.forEach {r->SmallLabel("Set ${r.setIndex+1}: "+if(e.timed) "${r.seconds} seconds" else "${weightLabel(r.weightKg,state.profile)}${if(e.perHand) " / hand" else ""} × ${r.reps}")}
            if(!e.timed) Text(Training.nextLoad(e.id,state).reason)
        }
    }
    var saved by remember {mutableStateOf(false)}
    OutlinedButton(onClick={vm.saveRoutine(s.title,s.plan);saved=true},enabled=!saved,modifier=Modifier.fillMaxWidth()) {Text(if(saved) "Routine saved" else "Save as a routine")}
    OutlinedButton(onClick=onCoach,modifier=Modifier.fillMaxWidth()) {Text("Ask your coach")}
    PrimaryButton("Back to Today",onClick=onDone)
}
@Composable fun ProgressScreen(state:AppState,vm:FitnessViewModel) {
    val p=state.profile;val completed=state.sessions.filter {it.finishedAt!=null}
    SectionTitle("Your progress","${p.name} · Built one session at a time.")
    if(completed.isEmpty()) EmptyState("Your first milestone awaits","Complete a workout to see your exercise trends and personal records.")
    else {
        val since=System.currentTimeMillis()-28L*24*3600*1000
        PanelCard {
            Text("${completed.count {it.finishedAt!!>=since}} workouts in the last 28 days",style=MaterialTheme.typography.titleLarge)
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
        PanelCard {
            if(records.isNotEmpty()) {
                Text(if(e.timed) "Longest hold · ${records.maxOf {it.seconds}} sec" else "Highest logged weight · ${weightLabel(records.maxOf {it.weightKg},p)}${if(e.perHand) " / hand" else ""}")
                if(!e.timed) Text("Most reps · ${records.maxOf {it.reps}}")
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
