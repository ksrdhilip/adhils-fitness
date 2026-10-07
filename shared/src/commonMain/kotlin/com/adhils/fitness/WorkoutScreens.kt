package com.adhils.fitness

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adhils.fitness.core.*
import kotlinx.coroutines.delay

data class CameraSetDraft(val profileId:String,val sessionId:String,val exerciseId:String,val reps:Int,val seconds:Int,val observations:List<String>)
object CameraDraft { var value by mutableStateOf<CameraSetDraft?>(null) }

@OptIn(ExperimentalLayoutApi::class)
@Composable fun WorkoutScreen(state:AppState,s:Session,vm:FitnessViewModel,onBack:()->Unit,onExercise:(String)->Unit,onCamera:()->Unit,onFinish:()->Unit) {
    val plan=s.plan[s.currentExercise]; val e=Catalog.get(plan.exerciseId); val profile=state.profile
    val prBadges by vm.lastPrBadges.collectAsState()
    var now by remember { mutableLongStateOf(nowMillis()) }
    var viewMode by rememberSaveable { mutableStateOf("stacked") } // "stacked" (Card Stack) or "focus" (Timer & Detail View)
    var trendExerciseId by remember { mutableStateOf<String?>(null) }
    var replacingExercise by remember { mutableStateOf<Exercise?>(null) }
    var adding by remember { mutableStateOf(false) }
    var finish by remember { mutableStateOf(false) }

    LaunchedEffect(s.id) { while(true) { now=nowMillis();delay(200) } }
    val draft=CameraDraft.value
    LaunchedEffect(draft) {
        if(draft!=null && draft.profileId==vm.store.value.selectedId && draft.sessionId==s.id) {
            val draftEx=Catalog.get(draft.exerciseId)
            val draftPlan=s.plan.find { it.exerciseId==draft.exerciseId }
            if(draftPlan!=null) {
                val nextSetIdx=(0 until draftPlan.sets).firstOrNull { i->s.results.none {it.exerciseId==draftEx.id && it.setIndex==i} } ?: (draftPlan.sets-1).coerceAtLeast(0)
                if(viewMode=="stacked" && (draft.reps>0 || draft.seconds>0)) {
                    vm.saveSet(SetResult(
                        exerciseId=draftEx.id,
                        setIndex=nextSetIdx,
                        reps=if(draftEx.timed) 0 else draft.reps,
                        seconds=if(draftEx.timed) draft.seconds else 0,
                        weightKg=draftPlan.weightKg,
                        observations=draft.observations
                    ))
                    CameraDraft.value=null
                }
            }
        }
    }

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

    ScreenHeader(cleanSessionTitle, onBack)

    // Prominent Fitbod Live Workout Timer Banner (Matching Screenshot 2)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF14151C),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262836)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFFFF2D55), androidx.compose.foundation.shape.CircleShape)
                    )
                    val elapsedSec = ((now - s.startedAt).coerceAtLeast(0) / 1000).toInt()
                    val hrs = elapsedSec / 3600
                    val mins = (elapsedSec % 3600) / 60
                    val secs = elapsedSec % 60
                    val mStr = if (mins < 10) "0$mins" else "$mins"
                    val sStr = if (secs < 10) "0$secs" else "$secs"
                    val timerStr = if (hrs > 0) "$hrs:$mStr:$sStr" else "0:$mStr:$sStr"
                    Text(
                        timerStr,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 30.sp
                    )
                }
                val sessionVolKg = Recovery.sessionVolumeKg(s)
                val completedExCount = s.plan.count { pItem -> s.results.any { it.exerciseId == pItem.exerciseId } }
                Text(
                    "$completedExCount of ${s.plan.size} Completed · Volume: ${weightLabel(sessionVolKg, profile)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E8E93),
                    fontWeight = FontWeight.Medium
                )
            }

            // Circular Stop Finish Button (Matching Screenshot 2)
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = Color(0xFFE83F5B),
                modifier = Modifier.size(50.dp),
                onClick = { finish = true }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Surface(shape = RoundedCornerShape(3.dp), color = Color.White, modifier = Modifier.size(16.dp)) {}
                }
            }
        }
    }

    // Galaxy Watch & Wearable Live Metrics Banner
    if (state.profile.healthSyncEnabled) {
        val wearable = HealthWearableManager.connectedDevice.collectAsState().value
        if (wearable != null) {
            PanelCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("⌚")
                        Column {
                            SmallLabel("${wearable.name} · LIVE VITALS")
                            Text("❤️ ${wearable.liveHeartRateBpm} bpm · 🔥 ${wearable.caloriesBurnedToday} kcal", fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("${wearable.batteryPct}% 🔋", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    // PR Celebration Banner
    if(prBadges.isNotEmpty()) {
        PanelCard {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    SmallLabel("PERSONAL RECORD ACHIEVED")
                    prBadges.forEach { badge ->
                        Text(badge,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,color=Mint)
                    }
                }
                TextButton(onClick={vm.lastPrBadges.value=emptyList()},modifier=Modifier.heightIn(min=48.dp)) { Text("Dismiss") }
            }
        }
    }

    // Mode Switcher + Add Exercise
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val isCompact = maxWidth < 420.dp
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = viewMode == "stacked",
                    onClick = { viewMode = "stacked" },
                    label = { Text(if (isCompact) "📋 All" else "📋 All Exercises", maxLines = 1) },
                    modifier = Modifier.heightIn(min = 44.dp)
                )
                FilterChip(
                    selected = viewMode == "focus",
                    onClick = { viewMode = "focus" },
                    label = { Text(if (isCompact) "⏱ Focus" else "⏱ Focus & Timers", maxLines = 1) },
                    modifier = Modifier.heightIn(min = 44.dp)
                )
            }
            OutlinedButton(
                onClick = { adding = true },
                enabled = s.plan.size < 30,
                modifier = Modifier.heightIn(min = 44.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("+ Exercise", maxLines = 1, softWrap = false)
            }
        }
    }

    if(viewMode=="stacked") {
        // STACKED EXERCISE CARDS VIEW
        s.plan.forEachIndexed { idx, pItem ->
            key(s.id, pItem.exerciseId) {
                StackedExerciseCard(
                    index=idx,
                    totalCount=s.plan.size,
                    plan=pItem,
                    session=s,
                    state=state,
                    vm=vm,
                    now=now,
                    onFocusTimer={ vm.nextExercise(idx); viewMode="focus" },
                    onOpenCamera={ vm.nextExercise(idx); onCamera() },
                    onOpenTrend={ trendExerciseId=pItem.exerciseId },
                    onOpenDetail={ onExercise(pItem.exerciseId) },
                    onReplace={ replacingExercise=Catalog.get(pItem.exerciseId) }
                )
            }
        }
    } else {
        // FOCUS & TIMERS VIEW
        val best1RmKg=remember(state.sessions,e.id) { Recovery.best1RMKg(state,e.id) }
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                SectionTitle("${s.currentExercise+1}/${s.plan.size} · ${e.name}")
                if(plan.supersetGroup!=null) {
                    val partners=s.plan.filter { it.supersetGroup==plan.supersetGroup && it.exerciseId!=e.id }.map { Catalog.get(it.exerciseId).name }
                    SmallLabel("⚡ Superset ${plan.supersetGroup}" + if(partners.isNotEmpty()) " with ${partners.joinToString()}" else "")
                }
            }
        }
        val previous=state.sessions.filter {it.finishedAt!=null}.flatMap {it.results}.lastOrNull {it.exerciseId==e.id}
        if(previous!=null || best1RmKg>0.0) {
            val lastStr=if(previous!=null) ("Last: "+if(e.timed) "${previous.seconds}s" else "${weightLabel(previous.weightKg,profile)} × ${previous.reps}") else ""
            val ormStr=if(!e.timed && best1RmKg>0.0) "Est. 1RM: ${weightLabel(best1RmKg,profile)}" else ""
            SmallLabel(listOf(lastStr,ormStr).filter { it.isNotBlank() }.joinToString(" · "))
        }
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick={trendExerciseId=e.id},modifier=Modifier.heightIn(min=48.dp)) {Text("📈 1RM & History")}
            OutlinedButton(onClick={onExercise(e.id)},modifier=Modifier.heightIn(min=48.dp)) {Text("Instructions")}
            OutlinedButton(onClick={replacingExercise=e},enabled=s.results.none {it.exerciseId==e.id},modifier=Modifier.heightIn(min=48.dp)) {Text("Swap")}
            if(s.currentExercise<s.plan.lastIndex || plan.supersetGroup!=null) {
                OutlinedButton(onClick={vm.toggleSuperset(s.currentExercise)},modifier=Modifier.heightIn(min=48.dp)) {
                    Text(if(plan.supersetGroup!=null) "Unlink Superset" else "⚡ Superset w/ next")
                }
            }
            if(s.plan.size>1 && s.results.none {it.exerciseId==e.id}) {
                TextButton(onClick={vm.removeExercise(e.id)},modifier=Modifier.heightIn(min=48.dp)) {Text("Delete")}
            }
        }
        ExercisePostureMediaCard(e, state, vm, onOpenCameraCoach = onCamera)
        // Inline Rest Timer for Current Exercise
        if (s.restUntil > 0L && (s.restUntil + 1200L > now)) {
            InlineRestTimerCard(
                restUntil = s.restUntil,
                now = now,
                totalSeconds = plan.restSeconds,
                onAdjust = { delta -> vm.changeRest(delta) },
                onSkip = { vm.changeRest(0) }
            )
        }
        key(s.id,e.id) {
            var setIndex by remember {mutableIntStateOf((0 until plan.sets).firstOrNull { i->s.results.none {it.exerciseId==e.id && it.setIndex==i} } ?: 0)}
            LaunchedEffect(plan.sets) { if(setIndex>=plan.sets) setIndex=(plan.sets-1).coerceAtLeast(0) }
            var reps by remember {mutableStateOf(plan.maxReps.toString())}; var seconds by remember {mutableStateOf(plan.seconds.toString())}
            var weight by remember {mutableStateOf(decimal(fromKg(plan.weightKg,profile.unit)))}
            var effort by remember {mutableStateOf("")}; var notes by remember {mutableStateOf("")}
            var warmup by remember {mutableStateOf(false)}
            var setType by remember {mutableStateOf("Working")}
            var observations by remember {mutableStateOf(emptyList<String>())}
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
                    if (exTimerRemaining in 1..5) {
                        WorkoutSoundManager.playCountdownBeep()
                    }
                    delay(1000)
                    val next = (exTimerRemaining-1).coerceAtLeast(0)
                    exTimerRemaining = next
                    if(e.timed) seconds=(plan.seconds-next).coerceAtLeast(1).toString()
                    if(next==0) {
                        WorkoutSoundManager.playTimerFinishedChime()
                        exTimerRunning=false
                        if(e.timed) seconds=plan.seconds.toString()
                    }
                }
            }
            LaunchedEffect(draft) {
                if(draft!=null && draft.profileId==vm.store.value.selectedId && draft.sessionId==s.id && draft.exerciseId==e.id) {
                    reps=draft.reps.toString();seconds=draft.seconds.toString();observations=draft.observations;CameraDraft.value=null
                }
            }
            PanelCard {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Column {
                        val isFinal5 = exTimerRunning && exTimerRemaining in 1..5
                        if (isFinal5) {
                            Text("🔥 FINAL ${exTimerRemaining}s · FINISH STRONG!", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                        } else {
                            SmallLabel("EXERCISE TIMER")
                        }
                        Text(
                            durationText(exTimerRemaining*1000L),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isFinal5) Color(0xFFFF5252) else MaterialTheme.colorScheme.primary
                        )
                        SmallLabel("Target: ${plan.seconds}s")
                    }
                    Row(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically) {
                        OutlinedButton(onClick={
                            vm.adjustTimers(e.id,exerciseDelta=-5)
                            exTimerRemaining=(exTimerRemaining-5).coerceAtLeast(5)
                        },modifier=Modifier.heightIn(min=48.dp),contentPadding=PaddingValues(horizontal=10.dp,vertical=6.dp)) {Text("-5s")}
                        Button(onClick={exTimerRunning=!exTimerRunning},modifier=Modifier.heightIn(min=48.dp),contentPadding=PaddingValues(horizontal=14.dp,vertical=6.dp)) {
                            Text(if(exTimerRunning) "Pause" else "Start")
                        }
                        OutlinedButton(onClick={
                            exTimerRunning=false
                            exTimerRemaining=plan.seconds
                            if(e.timed) seconds=plan.seconds.toString()
                        },modifier=Modifier.heightIn(min=48.dp),contentPadding=PaddingValues(horizontal=10.dp,vertical=6.dp)) {Text("Reset")}
                        OutlinedButton(onClick={
                            vm.adjustTimers(e.id,exerciseDelta=5)
                            exTimerRemaining=(exTimerRemaining+5).coerceAtMost(3600)
                        },modifier=Modifier.heightIn(min=48.dp),contentPadding=PaddingValues(horizontal=10.dp,vertical=6.dp)) {Text("+5s")}
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
                        },modifier=Modifier.heightIn(min=48.dp),contentPadding=PaddingValues(horizontal=12.dp,vertical=6.dp)) {Text("-5s")}
                        OutlinedButton(onClick={
                            vm.adjustTimers(e.id,restDelta=5)
                            if(s.restUntil>now) vm.changeRest(5_000)
                        },modifier=Modifier.heightIn(min=48.dp),contentPadding=PaddingValues(horizontal=12.dp,vertical=6.dp)) {Text("+5s")}
                    }
                }
            }
            (0 until plan.sets).forEach { i ->
                val logged=s.results.find {it.exerciseId==e.id && it.setIndex==i}
                OutlinedButton(onClick={
                    setIndex=i
                    reps=(logged?.reps ?: plan.maxReps).toString()
                    seconds=(logged?.seconds ?: plan.seconds).toString()
                    weight=decimal(fromKg(logged?.weightKg ?: plan.weightKg,profile.unit))
                    effort=logged?.rpe?.toString() ?: ""
                    notes=logged?.notes ?: ""
                    warmup=logged?.warmup ?: false
                    setType=logged?.setType ?: if(logged?.warmup==true) "Warm-up" else "Working"
                    observations=logged?.observations ?: emptyList()
                },modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {
                    val badge=if(logged!=null && logged.setType!="Working") " (${logged.setType})" else ""
                    Text("${if(logged!=null) "✓" else if(i==setIndex) "●" else "○"} Set ${i+1}$badge",Modifier.weight(1f))
                    Text(if(logged!=null) {if(e.timed) "${logged.seconds}s" else "${weightLabel(logged.weightKg,profile)} × ${logged.reps}"} else if(e.timed) "${plan.seconds}s target" else "${plan.minReps}–${plan.maxReps} reps")
                }
            }
            PanelCard {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Text("Review set ${setIndex+1}",style=MaterialTheme.typography.titleMedium)
                    if(!e.timed) {
                        val curW=weight.toDoubleOrNull()?.let { toKg(it,profile.unit) } ?: 0.0
                        val curR=reps.toIntOrNull() ?: plan.minReps
                        val est1Rm=Recovery.estimate1RMKg(curW,curR)
                        if(est1Rm>0.0) SmallLabel("Set Est. 1RM: ${weightLabel(est1Rm,profile)}")
                    }
                }
                SmallLabel("SET TYPE")
                FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    listOf("Working","Warm-up","Drop Set","AMRAP").forEach { t ->
                        FilterChip(selected=setType==t,onClick={
                            setType=t
                            warmup=(t=="Warm-up")
                            if(!e.timed && plan.weightKg>0.0) {
                                val baseDisplay=fromKg(plan.weightKg,profile.unit)
                                val step=profile.weightStep.coerceAtLeast(0.5)
                                when(t) {
                                    "Warm-up" -> {
                                        val w50=( kotlin.math.round((baseDisplay*0.6)/step)*step ).coerceAtLeast(step)
                                        weight=decimal(w50)
                                    }
                                    "Drop Set" -> {
                                        val w80=( kotlin.math.round((baseDisplay*0.8)/step)*step ).coerceAtLeast(step)
                                        weight=decimal(w80)
                                    }
                                    "Working" -> weight=decimal(baseDisplay)
                                }
                            }
                        },label={Text(t)})
                    }
                }
                if(!e.timed && plan.weightKg>0.0 && setType=="Warm-up") {
                    val baseDisplay=fromKg(plan.weightKg,profile.unit)
                    val step=profile.weightStep.coerceAtLeast(0.5)
                    val w50=(kotlin.math.round((baseDisplay*0.5)/step)*step).coerceAtLeast(step)
                    val w75=(kotlin.math.round((baseDisplay*0.75)/step)*step).coerceAtLeast(step)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick={weight=decimal(w50);reps="10"}) { Text("50% Ramp (${decimal(w50)} ${profile.unit})") }
                        OutlinedButton(onClick={weight=decimal(w75);reps="6"}) { Text("75% Ramp (${decimal(w75)} ${profile.unit})") }
                    }
                }
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    if(!e.timed) OutlinedTextField(weight,{weight=it},label={Text(profile.unit+if(e.perHand) " / hand" else " total")},modifier=Modifier.weight(1f),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),singleLine=true)
                    OutlinedTextField(if(e.timed) seconds else reps,{if(e.timed) seconds=it else reps=it},label={Text(if(e.timed) "Seconds" else if(setType=="AMRAP") "Max Reps (AMRAP)" else "Reps")},modifier=Modifier.weight(1f),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true)
                }
                OutlinedTextField(effort,{effort=it},label={Text("Effort / RPE 1–10 (optional)")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true)
                OutlinedTextField(notes,{notes=it.take(2000)},label={Text("Set notes (optional)")},modifier=Modifier.fillMaxWidth())
                observations.forEach {SmallLabel("🎥 $it")}
                val amount=if(e.timed) seconds.toIntOrNull() else reps.toIntOrNull()
                val load=if(e.timed) 0.0 else weight.toDoubleOrNull()
                val valid=amount!=null && amount in 1..(if(e.timed) 3600 else 100) && load!=null && load.isFinite() && toKg(load,profile.unit) in 0.0..500.0 && (effort.isBlank() || effort.toIntOrNull() in 1..10)
                val restLabel=if(plan.supersetGroup!=null) "Save set & switch superset" else "Save set & start ${plan.restSeconds}s rest"
                val keyboardController = LocalSoftwareKeyboardController.current
                val focusManager = LocalFocusManager.current
                PrimaryButton(restLabel,valid) {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    exTimerRunning=false
                    exTimerRemaining=plan.seconds
                    vm.saveSet(SetResult(exerciseId=e.id,setIndex=setIndex,reps=if(e.timed) 0 else amount!!,seconds=if(e.timed) amount!! else 0,
                        weightKg=toKg(load!!,profile.unit),rpe=effort.toIntOrNull(),warmup=warmup,setType=setType,notes=notes,observations=observations))
                    if(setIndex<plan.sets-1) {setIndex++;reps=plan.maxReps.toString();effort="";notes="";warmup=false;setType="Working";observations=emptyList()}
                }
            }
            FilledTonalButton(onClick=onCamera,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) {
                Text("🎥 Live Camera & AI Posture Coach (Rep-by-Rep Feedback)")
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            TextButton(onClick={vm.nextExercise(s.currentExercise-1)},enabled=s.currentExercise>0,modifier=Modifier.heightIn(min=48.dp)) {Text("← Previous")}
            TextButton(onClick={vm.nextExercise(s.currentExercise+1)},enabled=s.currentExercise<s.plan.lastIndex,modifier=Modifier.heightIn(min=48.dp)) {Text("Next exercise →")}
        }
    }

    PrimaryButton("Complete & Log Workout (${s.results.size} Sets)",s.results.isNotEmpty()) {finish=true}

    if(adding) AddExerciseDialog(state,{id->vm.addExercise(id);adding=false},{adding=false})
    replacingExercise?.let { targetEx ->
        ReplacementDialog(targetEx,state,{newId,remember->vm.replace(targetEx.id,newId,remember);replacingExercise=null},{replacingExercise=null})
    }
    trendExerciseId?.let { exId ->
        ExerciseTrendModal(exId,state,vm,onDismiss={trendExerciseId=null})
    }
    val nextWorkoutForDialog = remember(s.id, state.sessions) {
        runCatching {
            val completedSessions = if (state.sessions.any { it.id == s.id && it.finishedAt != null }) {
                state.sessions
            } else {
                state.sessions.filterNot { it.id == s.id } + s.copy(finishedAt = nowMillis())
            }
            val nextState = state.copy(sessions = completedSessions)
            Training.generate(nextState, CheckIn(minutes = state.profile.minutes, focus = "Freshest Muscles (Auto)"))
        }.getOrNull()
    }
    val dialogNextTitle = nextWorkoutForDialog?.title ?: "Next Scheduled Routine"

    if(finish) AlertDialog(
        onDismissRequest={finish=false},
        title={Text("Finish your workout?")},
        text={
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${s.results.size} sets have been logged. Uncompleted sets will remain unlogged.")
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("🗓", fontSize = 20.sp)
                        Column {
                            Text(
                                "Next workout is $dialogNextTitle",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (nextWorkoutForDialog != null) {
                                SmallLabel("${nextWorkoutForDialog.plan.size} planned exercises · ${state.profile.minutes} min")
                            }
                        }
                    }
                }
            }
        },
        confirmButton={TextButton(onClick={finish=false;onFinish()}) {Text("Finish & View Summary")}},
        dismissButton={TextButton(onClick={finish=false}) {Text("Keep training")}}
    )
}

@Composable
fun InlineRestTimerCard(
    restUntil: Long,
    now: Long,
    totalSeconds: Int,
    onAdjust: (Long) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val remainingMs = (restUntil - now).coerceAtLeast(0L)
    val remainingSec = ((remainingMs + 999L) / 1000L).toInt()
    val totalMs = (totalSeconds * 1000L).coerceAtLeast(1000L)
    val progress = ((totalMs - remainingMs).toFloat() / totalMs).coerceIn(0f, 1f)
    val isFinished = (remainingMs == 0L)
    val isLast5Sec = !isFinished && remainingSec in 1..5

    // Audio countdown trigger
    var lastBeepSecond by remember(restUntil) { mutableIntStateOf(-1) }
    LaunchedEffect(remainingSec, restUntil) {
        if (restUntil > 0L) {
            if (remainingSec in 1..5 && remainingSec != lastBeepSecond) {
                lastBeepSecond = remainingSec
                WorkoutSoundManager.playCountdownBeep()
            } else if (remainingSec == 0 && lastBeepSecond in 1..5) {
                lastBeepSecond = 0
                WorkoutSoundManager.playTimerFinishedChime()
            }
        }
    }

    val cardBg = when {
        isFinished -> Color(0xFF132A1C)
        isLast5Sec -> Color(0xFF2E1515)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val accentColor = when {
        isFinished -> Mint
        isLast5Sec -> Color(0xFFFF5252)
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = if (isLast5Sec) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF5252))
                 else if (isFinished) androidx.compose.foundation.BorderStroke(2.dp, Mint)
                 else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (isFinished) "✅" else if (isLast5Sec) "🔥" else "⏱", style = MaterialTheme.typography.titleMedium)
                    Column {
                        if (isFinished) {
                            Text("REST COMPLETE · GET READY!", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Mint)
                            Text("00:00", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Mint)
                        } else if (isLast5Sec) {
                            Text("FINAL ${remainingSec}s · PREPARE SET", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                            Text(durationText(remainingMs), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                        } else {
                            SmallLabel("ACTIVE REST TIMER")
                            Text("Rest · ${durationText(remainingMs)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = accentColor)
                        }
                    }
                }
                if (!isFinished) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { onAdjust(-5_000) },
                            modifier = Modifier.heightIn(min = 40.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) { Text("-5s") }
                        OutlinedButton(
                            onClick = { onAdjust(5_000) },
                            modifier = Modifier.heightIn(min = 40.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) { Text("+5s") }
                        TextButton(
                            onClick = onSkip,
                            modifier = Modifier.heightIn(min = 40.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) { Text("Skip") }
                    }
                }
            }

            // Visual Progress Bar
            LinearProgressIndicator(
                progress = { if (isFinished) 1f else progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )

            if (isLast5Sec) {
                Text(
                    "Starting next set in $remainingSec seconds! Take your grip.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF8A80),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun StackedExerciseCard(
    index:Int,
    totalCount:Int,
    plan:PlannedExercise,
    session:Session,
    state:AppState,
    vm:FitnessViewModel,
    now:Long,
    onFocusTimer:()->Unit,
    onOpenCamera:()->Unit,
    onOpenTrend:()->Unit,
    onOpenDetail:()->Unit,
    onReplace:()->Unit
) {
    val e=Catalog.get(plan.exerciseId)
    val profile=state.profile
    var showVideo by remember(e.id) { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val best1RmKg=remember(state.sessions,e.id) { Recovery.best1RMKg(state,e.id) }
    val pastSets=remember(state.sessions,e.id) {
        state.sessions.filter { it.finishedAt!=null }.flatMap { it.results }.filter { it.exerciseId==e.id && !it.warmup }
    }
    val completedCount=session.results.count { it.exerciseId==e.id }
    val isCurrent = (index == session.currentExercise)
    val isRestingHere = isCurrent && (session.restUntil > 0L && (session.restUntil + 1200L > now))
    val isFinal5 = isRestingHere && (session.restUntil - now) in 1..5000L

    val cardBorder = if (isFinal5) {
        Modifier.border(2.dp, Color(0xFFFF5252), RoundedCornerShape(18.dp))
    } else if (isRestingHere) {
        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp))
    } else {
        Modifier
    }

    PanelCard(modifier = cardBorder) {
        // Card Header: Video Thumbnail Cell + Title + Reorder & Context Menu
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            ExerciseThumbnail(
                exerciseId = e.id,
                modifier = Modifier.size(56.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                contentDescription = e.name,
                showVideoIndicator = true,
                isVideoExpanded = showVideo,
                onClick = { showVideo = !showVideo }
            )
            Column(Modifier.weight(1f).clickable { onOpenDetail() }) {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    Text(e.name,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                    if(plan.supersetGroup!=null) {
                        Surface(shape=androidx.compose.foundation.shape.RoundedCornerShape(6.dp),color=MaterialTheme.colorScheme.primaryContainer) {
                            Text("SUPERSET ${plan.supersetGroup}",Modifier.padding(horizontal=6.dp,vertical=2.dp),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
                val ormBadge=if(!e.timed && best1RmKg>0.0) " · 1RM ${weightLabel(best1RmKg,profile)}" else ""
                SmallLabel("${e.muscles} · ${e.equipment} · Rest ${plan.restSeconds}s$ormBadge")
            }
            // Reorder Up / Down & Context Menu (48x48dp touch targets)
            Row(verticalAlignment=Alignment.CenterVertically) {
                if(index>0) {
                    IconButton(onClick={vm.moveExercise(index,-1)},modifier=Modifier.size(40.dp)) { Text("↑",fontWeight=FontWeight.Bold) }
                }
                if(index<totalCount-1) {
                    IconButton(onClick={vm.moveExercise(index,1)},modifier=Modifier.size(40.dp)) { Text("↓",fontWeight=FontWeight.Bold) }
                }
                Box {
                    IconButton(onClick={showMenu=true},modifier=Modifier.size(48.dp)) { Text("⋯",fontWeight=FontWeight.Bold) }
                    DropdownMenu(expanded=showMenu,onDismissRequest={showMenu=false}) {
                        DropdownMenuItem(text={Text("📺 Toggle Posture Video")},onClick={showVideo=!showVideo;showMenu=false})
                        DropdownMenuItem(text={Text("🎥 Live AI Camera Rep Coach")},onClick={showMenu=false;onOpenCamera()})
                        DropdownMenuItem(text={Text("📈 History & 1RM Progression")},onClick={showMenu=false;onOpenTrend()})
                        DropdownMenuItem(text={Text("⏱ Focus & Exercise Timer")},onClick={showMenu=false;onFocusTimer()})
                        if(index<totalCount-1 || plan.supersetGroup!=null) {
                            DropdownMenuItem(
                                text={Text(if(plan.supersetGroup!=null) "Unlink Superset" else "⚡ Link Superset w/ Next")},
                                onClick={vm.toggleSuperset(index);showMenu=false}
                            )
                        }
                        DropdownMenuItem(text={Text("Rest Timer +5s (${plan.restSeconds+5}s)")},onClick={vm.adjustTimers(e.id,restDelta=5)})
                        DropdownMenuItem(text={Text("Rest Timer -5s (${(plan.restSeconds-5).coerceAtLeast(10)}s)")},onClick={vm.adjustTimers(e.id,restDelta=-5)})
                        if(completedCount==0) {
                            DropdownMenuItem(text={Text("🔄 Replace / Swap Exercise")},onClick={showMenu=false;onReplace()})
                            if(totalCount>1) {
                                DropdownMenuItem(text={Text("🗑 Remove Exercise")},onClick={showMenu=false;vm.removeExercise(e.id)})
                            }
                        }
                        DropdownMenuItem(text={Text("ℹ Form Instructions")},onClick={showMenu=false;onOpenDetail()})
                    }
                }
            }
        }

        // Inline Expandable YouTube Posture Video + AI Checkpoints
        if(showVideo) {
            ExercisePostureMediaCard(e, state, vm, onOpenCameraCoach = onOpenCamera)
        }

        // Inline Active Rest Timer for Current Workout
        if (isRestingHere) {
            InlineRestTimerCard(
                restUntil = session.restUntil,
                now = now,
                totalSeconds = plan.restSeconds,
                onAdjust = { delta -> vm.changeRest(delta) },
                onSkip = { vm.changeRest(0) }
            )
        }

        // Nested Set Logging Grid Header
        Row(Modifier.fillMaxWidth().padding(horizontal=4.dp),verticalAlignment=Alignment.CenterVertically) {
            SmallLabel("SET")
            Spacer(Modifier.width(24.dp))
            Box(Modifier.weight(1.1f)) { SmallLabel("PREVIOUS") }
            if(!e.timed) {
                Box(Modifier.weight(1f)) { SmallLabel(profile.unit.uppercase() + if(e.perHand) "/HAND" else "") }
            }
            Box(Modifier.weight(1f)) { SmallLabel(if(e.timed) "SECONDS" else "REPS") }
            Spacer(Modifier.width(8.dp))
            SmallLabel("LOG")
        }

        // Atomic Set Logging Rows
        (0 until plan.sets).forEach { setIdx ->
            val logged=session.results.find { it.exerciseId==e.id && it.setIndex==setIdx }
            val prevSet=pastSets.getOrNull(pastSets.size - plan.sets + setIdx) ?: pastSets.lastOrNull()
            SetLoggingRow(
                setIndex=setIdx,
                exercise=e,
                plan=plan,
                profile=profile,
                logged=logged,
                previous=prevSet,
                onSave={ res ->
                    vm.nextExercise(index)
                    vm.saveSet(res)
                }
            )
        }

        // Card Footer Actions (Compact 36dp single-row layout)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = { vm.adjustSetCount(e.id, 1) },
                enabled = plan.sets < 8,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("+ Set", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
            }
            if (plan.sets > 1 && completedCount < plan.sets) {
                OutlinedButton(
                    onClick = { vm.adjustSetCount(e.id, -1) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("- Set", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                }
            }
            OutlinedButton(
                onClick = onOpenCamera,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("🎥 AI", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick = onOpenTrend,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("📈 1RM", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
            }
            OutlinedButton(
                onClick = onFocusTimer,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("⏱", fontSize = 20.sp)
            }
        }
    }
}

@Composable fun SetLoggingRow(
    setIndex:Int,
    exercise:Exercise,
    plan:PlannedExercise,
    profile:Profile,
    logged:SetResult?,
    previous:SetResult?,
    onSave:(SetResult)->Unit
) {
    val defaultW=decimal(fromKg(logged?.weightKg ?: plan.weightKg,profile.unit))
    val defaultAmount=if(exercise.timed) (logged?.seconds ?: plan.seconds).toString() else (logged?.reps ?: plan.maxReps).toString()
    var weightText by remember(logged?.weightKg,plan.weightKg) { mutableStateOf(defaultW) }
    var amountText by remember(logged?.reps,logged?.seconds,plan.maxReps,plan.seconds) { mutableStateOf(defaultAmount) }
    var setType by remember(logged?.setType) { mutableStateOf(logged?.setType ?: if(logged?.warmup==true) "Warm-up" else "Working") }

    val rowBg=if(logged!=null) Color(0xFF16291F) else MaterialTheme.colorScheme.surfaceVariant
    Surface(shape=androidx.compose.foundation.shape.RoundedCornerShape(12.dp),color=rowBg,modifier=Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=6.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(8.dp)
        ) {
            // Set Number / Set Type Cycle Button (48x48dp)
            Surface(
                onClick={
                    val types=listOf("Working","Warm-up","Drop Set","AMRAP")
                    val next=types[(types.indexOf(setType)+1)%types.size]
                    setType=next
                    if(!exercise.timed && plan.weightKg>0.0) {
                        val base=fromKg(plan.weightKg,profile.unit)
                        val step=profile.weightStep.coerceAtLeast(0.5)
                        weightText=when(next) {
                            "Warm-up" -> decimal((kotlin.math.round((base*0.6)/step)*step).coerceAtLeast(step))
                            "Drop Set" -> decimal((kotlin.math.round((base*0.8)/step)*step).coerceAtLeast(step))
                            else -> decimal(base)
                        }
                    }
                },
                shape=androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                color=when(setType) {
                    "Warm-up" -> Color(0xFF3E2E14)
                    "Drop Set" -> Color(0xFF3B1520)
                    "AMRAP" -> Color(0xFF1F2937)
                    else -> MaterialTheme.colorScheme.surfaceContainer
                },
                modifier=Modifier.size(48.dp)
            ) {
                Box(contentAlignment=Alignment.Center) {
                    val label=when(setType) {
                        "Warm-up" -> "W"
                        "Drop Set" -> "D"
                        "AMRAP" -> "F"
                        else -> "${setIndex+1}"
                    }
                    Text(label,fontWeight=FontWeight.Bold)
                }
            }

            // Previous Historical Metric
            Box(Modifier.weight(1.1f)) {
                val prevStr=if(previous!=null) {
                    if(exercise.timed) "${previous.seconds}s"
                    else "${decimal(fromKg(previous.weightKg,profile.unit))} ${profile.unit} × ${previous.reps}"
                } else if(exercise.timed) "${plan.seconds}s" else "${plan.minReps}–${plan.maxReps}r"
                SmallLabel(prevStr)
            }

            val keyboardController = LocalSoftwareKeyboardController.current
            val focusManager = LocalFocusManager.current

            val amt=amountText.toIntOrNull()
            val wVal=if(exercise.timed) 0.0 else weightText.toDoubleOrNull()
            val valid=amt!=null && amt in 1..(if(exercise.timed) 3600 else 100) && wVal!=null && wVal.isFinite() && toKg(wVal,profile.unit) in 0.0..500.0

            val commitSet = {
                focusManager.clearFocus()
                keyboardController?.hide()
                if(valid) {
                    onSave(SetResult(
                        id=logged?.id ?: newId(),
                        exerciseId=exercise.id,
                        setIndex=setIndex,
                        reps=if(exercise.timed) 0 else amt!!,
                        seconds=if(exercise.timed) amt!! else 0,
                        weightKg=toKg(wVal!!,profile.unit),
                        warmup=(setType=="Warm-up"),
                        setType=setType,
                        observations=logged?.observations ?: emptyList()
                    ))
                }
            }

            // Weight Input
            if(!exercise.timed) {
                OutlinedTextField(
                    value=weightText,
                    onValueChange={weightText=it.take(8)},
                    singleLine=true,
                    keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal, imeAction=ImeAction.Next),
                    modifier=Modifier.weight(1f).heightIn(min=48.dp)
                )
            }

            // Reps / Seconds Input
            OutlinedTextField(
                value=amountText,
                onValueChange={amountText=it.take(6)},
                singleLine=true,
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number, imeAction=ImeAction.Done),
                keyboardActions=KeyboardActions(onDone={ commitSet() }),
                modifier=Modifier.weight(1f).heightIn(min=48.dp)
            )

            // Oversized 48x48dp Set Completion Checkmark
            Surface(
                onClick={ commitSet() },
                shape=androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                color=if(logged!=null) Mint else if(valid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier=Modifier.size(48.dp)
            ) {
                Box(contentAlignment=Alignment.Center) {
                    Text("✓",fontWeight=FontWeight.Bold,color=if(logged!=null) Dark else androidx.compose.ui.graphics.Color.White)
                }
            }
        }
    }
}

@Composable fun ExerciseTrendModal(exerciseId:String,state:AppState,vm:FitnessViewModel,onDismiss:()->Unit) {
    val e=Catalog.get(exerciseId)
    val p=state.profile
    val completed=remember(state.sessions) { state.sessions.filter { it.finishedAt!=null } }
    val records=remember(completed,exerciseId) { completed.flatMap { it.results }.filter { it.exerciseId==exerciseId && !it.warmup } }
    val best1RmKg=remember(state,exerciseId) { Recovery.best1RMKg(state,exerciseId) }
    val trendPoints=remember(completed,exerciseId) {
        completed.mapNotNull { s ->
            val sets=s.results.filter { it.exerciseId==exerciseId && !it.warmup }
            if(sets.isEmpty()) null
            else {
                val metric=if(e.timed) sets.maxOf { it.seconds }.toDouble()
                    else fromKg(sets.maxOf { Recovery.estimate1RMKg(it.weightKg,it.reps) },p.unit)
                s.finishedAt!! to metric
            }
        }.sortedBy { it.first }
    }
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text("${e.name} · Records & 1RM Trend")},
        text={
            Column(Modifier.heightIn(max=480.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                SmallLabel("${e.muscles} · ${e.equipment}")
                PanelCard {
                    SmallLabel("PERSONAL RECORDS")
                    if(records.isEmpty()) {
                        Text("Log your first working set of ${e.name} to establish your baseline 1RM and records.")
                    } else if(e.timed) {
                        Text("Longest Hold PR: ${records.maxOf { it.seconds }} sec",fontWeight=FontWeight.Bold,color=Mint)
                        SmallLabel("Total Logged Sets: ${records.size}")
                    } else {
                        Text("Estimated 1RM: ${weightLabel(best1RmKg,p)}${if(e.perHand) " / hand" else ""}",fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)
                        Text("Max Weight: ${weightLabel(records.maxOf { it.weightKg },p)} · Max Reps: ${records.maxOf { it.reps }}")
                    }
                }
                if(trendPoints.size>=2) {
                    PanelCard {
                        val low=trendPoints.minOf { it.second }
                        val high=trendPoints.maxOf { it.second }
                        val range=(high-low).coerceAtLeast(1.0)
                        SmallLabel(if(e.timed) "HOLD TIME PROGRESSION (SEC)" else "ESTIMATED 1-REP MAX (1RM) PROGRESSION (${p.unit.uppercase()})")
                        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(140.dp)) {
                            val first=trendPoints.first().first
                            val span=(trendPoints.last().first-first).coerceAtLeast(1L)
                            val pts=trendPoints.map {
                                androidx.compose.ui.geometry.Offset(
                                    12f+(it.first-first).toFloat()/span*(size.width-24f),
                                    (size.height-16f-((it.second-low)/range*(size.height-32f))).toFloat()
                                )
                            }
                            pts.zipWithNext().forEach { (a,b) -> drawLine(BrandRed,a,b,3.dp.toPx()) }
                            pts.forEach { drawCircle(Mint,5.dp.toPx(),it) }
                        }
                        SmallLabel("Range: ${decimal(low)} – ${decimal(high)} ${if(e.timed) "sec" else p.unit} across ${trendPoints.size} sessions")
                    }
                }
                SmallLabel("RECENT SESSION HISTORY")
                completed.reversed().forEach { s ->
                    val exSets = s.results.filter { it.exerciseId == exerciseId }
                    if (exSets.isNotEmpty()) {
                        val date = formatDate(s.startedAt)
                        val summary=exSets.sortedBy { it.setIndex }.joinToString(", ") { r ->
                            if(e.timed) "${r.seconds}s" else "${weightLabel(r.weightKg,p)}×${r.reps}"
                        }
                        Text("• $date: $summary",style=MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton={TextButton(onClick=onDismiss) {Text("Close")}}
    )
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
