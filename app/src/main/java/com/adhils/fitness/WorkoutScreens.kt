package com.adhils.fitness

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.adhils.fitness.core.*
import kotlinx.coroutines.delay

data class CameraSetDraft(val profileId:String,val sessionId:String,val exerciseId:String,val reps:Int,val seconds:Int,val observations:List<String>)
object CameraDraft { var value by mutableStateOf<CameraSetDraft?>(null) }

@OptIn(ExperimentalLayoutApi::class)
@Composable fun WorkoutScreen(state:AppState,s:Session,vm:FitnessViewModel,onBack:()->Unit,onExercise:(String)->Unit,onCamera:()->Unit,onFinish:()->Unit) {
    val plan=s.plan[s.currentExercise]; val e=Catalog.get(plan.exerciseId); val profile=state.profile
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(s.id) { while(true) { now=System.currentTimeMillis();delay(1000) } }
    ScreenHeader(s.title,onBack)
    SmallLabel("Exercise ${s.currentExercise+1} of ${s.plan.size} · ${durationText(now-s.startedAt)}")
    SectionTitle(e.name)
    val previous=state.sessions.filter {it.finishedAt!=null}.flatMap {it.results}.lastOrNull {it.exerciseId==e.id}
    if(previous!=null) SmallLabel("Last time: "+if(e.timed) "${previous.seconds} seconds" else "${weightLabel(previous.weightKg,profile)} × ${previous.reps}")
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick={onExercise(e.id)}) {Text("How to perform")}
        var replacing by remember(e.id) {mutableStateOf(false)}
        OutlinedButton(onClick={replacing=true},enabled=s.results.none {it.exerciseId==e.id}) {Text("Replace")}
        if(replacing) ReplacementDialog(e,state,{id,remember->vm.replace(e.id,id,remember);replacing=false},{replacing=false})
        var adding by remember {mutableStateOf(false)}
        OutlinedButton(onClick={adding=true},enabled=s.plan.size<30) {Text("+ Add exercise")}
        if(adding) AddExerciseDialog(state,{id->vm.addExercise(id);adding=false},{adding=false})
        if(s.plan.size>1 && s.results.none {it.exerciseId==e.id}) {
            TextButton(onClick={vm.removeExercise(e.id)}) {Text("Remove")}
        }
    }
    ExercisePostureMediaCard(e, state, vm)
    key(s.id,e.id) {
        var setIndex by remember {mutableIntStateOf((0 until plan.sets).firstOrNull { i->s.results.none {it.exerciseId==e.id && it.setIndex==i} } ?: 0)}
        LaunchedEffect(plan.sets) { if(setIndex>=plan.sets) setIndex=(plan.sets-1).coerceAtLeast(0) }
        var reps by remember {mutableStateOf("")}; var seconds by remember {mutableStateOf(plan.seconds.toString())}
        var weight by remember {mutableStateOf(decimal(fromKg(plan.weightKg,profile.unit)))}
        var effort by remember {mutableStateOf("")}; var notes by remember {mutableStateOf("")}
        var warmup by remember {mutableStateOf(false)}; var observations by remember {mutableStateOf(emptyList<String>())}
        var exTimerRunning by remember {mutableStateOf(false)}
        var exTimerRemaining by remember {mutableIntStateOf(plan.seconds)}
        LaunchedEffect(plan.seconds) {
            if(!exTimerRunning) {
                exTimerRemaining=plan.seconds
                if(e.timed) seconds=plan.seconds.toString()
            }
        }
        LaunchedEffect(exTimerRunning, exTimerRemaining) {
            if(exTimerRunning && exTimerRemaining>0) {
                delay(1000)
                exTimerRemaining=(exTimerRemaining-1).coerceAtLeast(0)
                if(e.timed) seconds=(plan.seconds-exTimerRemaining).coerceAtLeast(1).toString()
                if(exTimerRemaining==0) {
                    exTimerRunning=false
                    if(e.timed) seconds=plan.seconds.toString()
                }
            }
        }
        val draft=CameraDraft.value
        LaunchedEffect(draft) {
            if(draft!=null && draft.profileId==vm.store.value.selectedId && draft.sessionId==s.id && draft.exerciseId==e.id) {
                reps=draft.reps.toString();seconds=draft.seconds.toString();observations=draft.observations;CameraDraft.value=null
            }
        }
        PanelCard {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                Column {
                    SmallLabel("EXERCISE TIMER")
                    Text(durationText(exTimerRemaining*1000L),style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)
                    SmallLabel("Target: ${plan.seconds}s")
                }
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically) {
                    OutlinedButton(onClick={
                        vm.adjustTimers(e.id,exerciseDelta=-5)
                        exTimerRemaining=(exTimerRemaining-5).coerceAtLeast(5)
                    },contentPadding=PaddingValues(horizontal=10.dp,vertical=6.dp)) {Text("-5s")}
                    Button(onClick={exTimerRunning=!exTimerRunning},contentPadding=PaddingValues(horizontal=14.dp,vertical=6.dp)) {
                        Text(if(exTimerRunning) "Pause" else "Start")
                    }
                    OutlinedButton(onClick={
                        exTimerRunning=false
                        exTimerRemaining=plan.seconds
                        if(e.timed) seconds=plan.seconds.toString()
                    },contentPadding=PaddingValues(horizontal=10.dp,vertical=6.dp)) {Text("Reset")}
                    OutlinedButton(onClick={
                        vm.adjustTimers(e.id,exerciseDelta=5)
                        exTimerRemaining=(exTimerRemaining+5).coerceAtMost(3600)
                    },contentPadding=PaddingValues(horizontal=10.dp,vertical=6.dp)) {Text("+5s")}
                }
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                Column {
                    SmallLabel("REST TIMER DEFAULT")
                    Text("${plan.restSeconds}s (${durationText(plan.restSeconds*1000L)})",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
                }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick={
                        vm.adjustTimers(e.id,restDelta=-5)
                        if(s.restUntil>now) vm.changeRest(-5_000)
                    },contentPadding=PaddingValues(horizontal=12.dp,vertical=6.dp)) {Text("-5s")}
                    OutlinedButton(onClick={
                        vm.adjustTimers(e.id,restDelta=5)
                        if(s.restUntil>now) vm.changeRest(5_000)
                    },contentPadding=PaddingValues(horizontal=12.dp,vertical=6.dp)) {Text("+5s")}
                }
            }
        }
        if(s.restUntil>now) PanelCard {
            Text("Rest · ${durationText(s.restUntil-now)}",style=MaterialTheme.typography.headlineSmall,color=MaterialTheme.colorScheme.primary)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick={vm.changeRest(-5_000)}) {Text("-5 sec")}
                OutlinedButton(onClick={vm.changeRest(5_000)}) {Text("+5 sec")}
                OutlinedButton(onClick={vm.changeRest(30_000)}) {Text("+30 sec")}
                TextButton(onClick={vm.changeRest(0)}) {Text("Skip rest")}
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
            PrimaryButton("Save set & start ${plan.restSeconds}s rest",valid) {
                exTimerRunning=false
                exTimerRemaining=plan.seconds
                vm.saveSet(SetResult(exerciseId=e.id,setIndex=setIndex,reps=if(e.timed) 0 else amount!!,seconds=if(e.timed) amount!! else 0,
                    weightKg=toKg(load!!,profile.unit),rpe=effort.toIntOrNull(),warmup=warmup,notes=notes,observations=observations))
                if(setIndex<plan.sets-1) {setIndex++;reps="";effort="";notes="";observations=emptyList()}
            }
        }
        if(e.camera!=null) OutlinedButton(onClick=onCamera,modifier=Modifier.fillMaxWidth()) {Text("Use camera coach")}
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
    var search by remember {mutableStateOf("")}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Replace ${e.name}")},text={Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        listOf("Equipment unavailable","Pain or discomfort","Too difficult","Prefer another exercise").forEach {r->Row {RadioButton(reason==r,{reason=r});Text(r,Modifier.padding(top=12.dp))}}
        if(reason=="Pain or discomfort") Text("Stop painful movements. This list is not an assessment of which exercises are safe for an injury.")
        Row {Checkbox(rememberChoice,{rememberChoice=it});Text("Exclude this exercise from future plans",Modifier.padding(top=12.dp))}
        OutlinedTextField(value=search,onValueChange={search=it},label={Text("Search all exercises")},singleLine=true,modifier=Modifier.fillMaxWidth())
        val existingIds=state.active?.plan?.map {it.exerciseId}?.toSet() ?: emptySet()
        val options=if(search.isBlank()) {
            val samePattern=Catalog.alternatives(e.id,state.profile).filter {it.id !in existingIds}
            val others=Catalog.exercises.filter {it.id !in existingIds && it.id !in state.profile.excluded && (it.equipment=="Bodyweight" || it.equipment in state.profile.equipment) && it !in samePattern}
            (samePattern + others).take(20)
        } else {
            Catalog.exercises.filter {it.id !in existingIds && it.id !in state.profile.excluded && (it.name.contains(search,true) || it.muscles.contains(search,true) || it.pattern.contains(search,true))}.take(25)
        }
        if(options.isEmpty()) Text("No matching exercises.")
        options.forEach {alt->OutlinedButton(onClick={onReplace(alt.id,rememberChoice)},modifier=Modifier.fillMaxWidth()) {Text("${alt.name} (${alt.pattern})")}}
    }},confirmButton={TextButton(onClick=onDismiss) {Text("Cancel")}})
}
@Composable fun AddExerciseDialog(state:AppState,onAdd:(String)->Unit,onDismiss:()->Unit) {
    var search by remember {mutableStateOf("")}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Add exercise to workout")},text={Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value=search,onValueChange={search=it},label={Text("Search by name, muscle, or pattern")},singleLine=true,modifier=Modifier.fillMaxWidth())
        val existingIds=state.active?.plan?.map {it.exerciseId}?.toSet() ?: emptySet()
        val options=Catalog.exercises.filter {
            it.id !in existingIds && it.id !in state.profile.excluded &&
                (search.isNotBlank() || it.equipment=="Bodyweight" || it.equipment in state.profile.equipment) &&
                (search.isBlank() || it.name.contains(search,true) || it.muscles.contains(search,true) || it.pattern.contains(search,true))
        }.take(30)
        if(options.isEmpty()) Text("No matching exercises available.")
        options.forEach {ex->OutlinedButton(onClick={onAdd(ex.id)},modifier=Modifier.fillMaxWidth()) {Text("${ex.name} · ${ex.muscles}")}}
    }},confirmButton={TextButton(onClick=onDismiss) {Text("Cancel")}})
}
