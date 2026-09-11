package com.adhils.fitness

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.adhils.fitness.core.*
import kotlinx.coroutines.delay

data class CameraSetDraft(val profileId:String,val sessionId:String,val exerciseId:String,val reps:Int,val seconds:Int,val observations:List<String>)
object CameraDraft { var value by mutableStateOf<CameraSetDraft?>(null) }

@Composable fun WorkoutScreen(state:AppState,s:Session,vm:FitnessViewModel,onBack:()->Unit,onExercise:(String)->Unit,onCamera:()->Unit,onFinish:()->Unit) {
    val plan=s.plan[s.currentExercise]; val e=Catalog.get(plan.exerciseId); val profile=state.profile
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(s.id) { while(true) { now=System.currentTimeMillis();delay(1000) } }
    ScreenHeader(s.title,onBack)
    SmallLabel("Exercise ${s.currentExercise+1} of ${s.plan.size} · ${durationText(now-s.startedAt)}")
    SectionTitle(e.name)
    val previous=state.sessions.filter {it.finishedAt!=null}.flatMap {it.results}.lastOrNull {it.exerciseId==e.id}
    if(previous!=null) SmallLabel("Last time: "+if(e.timed) "${previous.seconds} seconds" else "${weightLabel(previous.weightKg,profile)} × ${previous.reps}")
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick={onExercise(e.id)}) {Text("How to perform")}
        var replacing by remember(e.id) {mutableStateOf(false)}
        OutlinedButton(onClick={replacing=true},enabled=s.results.none {it.exerciseId==e.id}) {Text("Replace")}
        if(replacing) ReplacementDialog(e,state,{id,remember->vm.replace(e.id,id,remember);replacing=false},{replacing=false})
    }
    key(s.id,e.id) {
        var setIndex by remember {mutableIntStateOf((0 until plan.sets).firstOrNull { i->s.results.none {it.exerciseId==e.id && it.setIndex==i} } ?: 0)}
        var reps by remember {mutableStateOf("")}; var seconds by remember {mutableStateOf(plan.seconds.toString())}
        var weight by remember {mutableStateOf(decimal(fromKg(plan.weightKg,profile.unit)))}
        var effort by remember {mutableStateOf("")}; var notes by remember {mutableStateOf("")}
        var warmup by remember {mutableStateOf(false)}; var observations by remember {mutableStateOf(emptyList<String>())}
        val draft=CameraDraft.value
        LaunchedEffect(draft) {
            if(draft!=null && draft.profileId==vm.store.value.selectedId && draft.sessionId==s.id && draft.exerciseId==e.id) {
                reps=draft.reps.toString();seconds=draft.seconds.toString();observations=draft.observations;CameraDraft.value=null
            }
        }
        (0 until plan.sets).forEach { i ->
            val logged=s.results.find {it.exerciseId==e.id && it.setIndex==i}
            OutlinedButton(onClick={setIndex=i;reps=logged?.reps?.toString() ?: "";seconds=(logged?.seconds ?: plan.seconds).toString();weight=decimal(fromKg(logged?.weightKg ?: plan.weightKg,profile.unit));effort=logged?.rpe?.toString() ?: "";notes=logged?.notes ?: "";warmup=logged?.warmup ?: false;observations=logged?.observations ?: emptyList()},modifier=Modifier.fillMaxWidth()) {
                Text("${if(logged!=null) "✓" else if(i==setIndex) "●" else "○"} Set ${i+1}",Modifier.weight(1f))
                Text(if(logged!=null) {if(e.timed) "${logged.seconds}s" else "${weightLabel(logged.weightKg,profile)} × ${logged.reps}"} else if(e.timed) "${plan.seconds}s target" else "${plan.minReps}–${plan.maxReps} reps")
            }
        }
        PanelCard {
            Text("Review set ${setIndex+1}",style=MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                if(!e.timed) OutlinedTextField(weight,{weight=it},label={Text(profile.unit+if(e.perHand) " / hand" else " total")},modifier=Modifier.weight(1f),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),singleLine=true)
                OutlinedTextField(if(e.timed) seconds else reps,{if(e.timed) seconds=it else reps=it},label={Text(if(e.timed) "Seconds" else "Reps")},modifier=Modifier.weight(1f),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true)
            }
            OutlinedTextField(effort,{effort=it},label={Text("Effort / RPE 1–10 (optional)")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true)
            OutlinedTextField(notes,{notes=it.take(2000)},label={Text("Set notes (optional)")},modifier=Modifier.fillMaxWidth())
            Row(verticalAlignment=Alignment.CenterVertically) {Checkbox(warmup,{warmup=it});Text("Warm-up set")}
            observations.forEach {SmallLabel(it)}
            val amount=if(e.timed) seconds.toIntOrNull() else reps.toIntOrNull()
            val load=if(e.timed) 0.0 else weight.toDoubleOrNull()
            val valid=amount!=null && amount in 1..(if(e.timed) 3600 else 100) && load!=null && load.isFinite() && toKg(load,profile.unit) in 0.0..500.0 && (effort.isBlank() || effort.toIntOrNull() in 1..10)
            PrimaryButton("Save set & start rest",valid) {
                vm.saveSet(SetResult(exerciseId=e.id,setIndex=setIndex,reps=if(e.timed) 0 else amount!!,seconds=if(e.timed) amount!! else 0,
                    weightKg=toKg(load!!,profile.unit),rpe=effort.toIntOrNull(),warmup=warmup,notes=notes,observations=observations))
                if(setIndex<plan.sets-1) {setIndex++;reps="";effort="";notes="";observations=emptyList()}
            }
        }
        if(e.camera!=null) OutlinedButton(onClick=onCamera,modifier=Modifier.fillMaxWidth()) {Text("Use camera coach")}
    }
    if(s.restUntil>now) PanelCard {
        Text("Rest · ${durationText(s.restUntil-now)}",style=MaterialTheme.typography.headlineSmall)
        Row {TextButton(onClick={vm.changeRest(30_000)}) {Text("+30 sec")};TextButton(onClick={vm.changeRest(0)}) {Text("Skip rest")}}
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
        TextButton(onClick={vm.nextExercise(s.currentExercise-1)},enabled=s.currentExercise>0) {Text("← Previous")}
        TextButton(onClick={vm.nextExercise(s.currentExercise+1)},enabled=s.currentExercise<s.plan.lastIndex) {Text("Next exercise →")}
    }
    var finish by remember {mutableStateOf(false)}
    PrimaryButton("Finish workout",s.results.isNotEmpty()) {finish=true}
    if(finish) AlertDialog(onDismissRequest={finish=false},title={Text("Finish your workout?")},text={Text("${s.results.size} sets have been logged. Uncompleted sets will remain unlogged.")},confirmButton={TextButton(onClick={finish=false;onFinish()}) {Text("Finish")}},dismissButton={TextButton(onClick={finish=false}) {Text("Keep training")}})
}
@Composable fun ReplacementDialog(e:Exercise,state:AppState,onReplace:(String,Boolean)->Unit,onDismiss:()->Unit) {
    var reason by remember {mutableStateOf("Equipment unavailable")};var rememberChoice by remember {mutableStateOf(false)}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Replace ${e.name}")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        listOf("Equipment unavailable","Pain or discomfort","Too difficult","Prefer another exercise").forEach {r->Row {RadioButton(reason==r,{reason=r});Text(r,Modifier.padding(top=12.dp))}}
        if(reason=="Pain or discomfort") Text("Stop painful movements. This list is not an assessment of which exercises are safe for an injury.")
        Row {Checkbox(rememberChoice,{rememberChoice=it});Text("Exclude this exercise from future plans",Modifier.padding(top=12.dp))}
        val options=Catalog.alternatives(e.id,state.profile).filter {candidate->state.active?.plan?.none {it.exerciseId==candidate.id} ?: true}
        if(options.isEmpty()) Text("No compatible alternatives with this profile’s equipment and exclusions.")
        options.forEach {alt->OutlinedButton(onClick={onReplace(alt.id,rememberChoice)},modifier=Modifier.fillMaxWidth()) {Text(alt.name)}}
    }},confirmButton={TextButton(onClick=onDismiss) {Text("Cancel")}})
}
