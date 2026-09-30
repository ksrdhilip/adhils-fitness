package com.adhils.fitness

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adhils.fitness.core.*
import java.time.*
import java.time.format.DateTimeFormatter

@Composable fun TodayScreen(state:AppState,onStart:()->Unit,onResume:()->Unit,onExercise:(String)->Unit,onHistory:()->Unit) {
    val p=state.profile
    val preview=remember(state) { runCatching { Training.generate(state,CheckIn(minutes=p.minutes)) }.getOrNull() }
    val greeting=when(LocalTime.now().hour) {in 5..11->"Good morning";in 12..16->"Good afternoon";else->"Good evening"}
    Text("$greeting, ${p.name}",style=MaterialTheme.typography.titleMedium)
    SmallLabel(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d")))
    Column {
        Text("Make time",fontSize=36.sp,lineHeight=40.sp,fontWeight=FontWeight.Bold)
        Text("for stronger.",fontSize=36.sp,lineHeight=40.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)
    }
    PanelCard {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SmallLabel(if(state.active!=null) "READY WHEN YOU ARE" else "TODAY’S WORKOUT")
                Text(state.active?.title ?: preview?.title ?: "Set up your workout",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold)
                SmallLabel("About ${p.minutes} min · ${state.active?.plan?.size ?: preview?.plan?.size ?: 0} exercises")
            }
            Icon(Icons.Default.FitnessCenter,null,Modifier.size(48.dp),tint=MaterialTheme.colorScheme.primary)
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            p.equipment.take(2).forEach { SuggestionChip(onClick={},label={ Text(it) }) }
        }
        PrimaryButton(if(state.active!=null) "Resume workout →" else "Check in & start →",
            enabled=state.active!=null || preview!=null,onClick=if(state.active!=null) onResume else onStart)
    }
    val today=LocalDate.now()
    val monday=today.minusDays((today.dayOfWeek.value-1).toLong())
    val done=state.sessions.filter { it.finishedAt!=null }.map { Instant.ofEpochMilli(it.finishedAt!!).atZone(ZoneId.systemDefault()).toLocalDate() }
    val weekCount=done.count { !it.isBefore(monday) && it.isBefore(monday.plusDays(7)) }
    PanelCard {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text("Your week",fontWeight=FontWeight.SemiBold)
            SmallLabel("$weekCount of ${p.days} workouts")
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            (0..6).forEach { i ->
                val date=monday.plusDays(i.toLong()); val checked=date in done
                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    Box(Modifier.size(30.dp).background(if(checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, CircleShape),Alignment.Center) {
                        if(checked) Icon(Icons.Default.Check,null,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.onPrimary)
                        else Text(if(date==today) "•" else "",color=MaterialTheme.colorScheme.primary)
                    }
                    SmallLabel(listOf("M","T","W","T","F","S","S")[i])
                }
            }
        }
        TextButton(onClick=onHistory) { Text("View workout history") }
    }
    SectionTitle("Coming up")
    (state.active ?: preview)?.plan?.filter { Catalog.get(it.exerciseId).pattern !in setOf("Warm-up","Mobility") }?.take(3)?.forEach {
        ExerciseRow(it) { onExercise(it.exerciseId) }
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable fun CheckInScreen(p:Profile,onStart:(CheckIn)->Unit,onBack:()->Unit,onAskCoach:((CheckIn)->Unit)?=null) {
    var feeling by remember { mutableStateOf("Good") }
    var focus by remember { mutableStateOf("Full Body") }
    var energy by remember { mutableFloatStateOf(3f) }
    var soreness by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableFloatStateOf(p.minutes.toFloat()) }
    ScreenHeader("Your check-in",onBack)
    SectionTitle("Make today work for you","Tell us how you feel and your focus to customize today’s exercises.")
    PanelCard {
        Text("How are you feeling today?",fontWeight=FontWeight.SemiBold)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            listOf("Energized","Good","Tired","Sore / Stiff","Recovering").forEach { item ->
                FilterChip(selected=feeling==item,onClick={
                    feeling=item
                    energy=when(item) {"Energized"->5f;"Good"->3f;"Tired"->2f;"Sore / Stiff"->2f;else->2f}
                    if(item=="Sore / Stiff") soreness=2
                },label={Text(item)})
            }
        }
        Text("What is your focus today?",fontWeight=FontWeight.SemiBold)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            listOf("Full Body","Upper Body","Lower Body","Push (Chest & Shoulders)","Pull (Back & Biceps)","Core & Mobility").forEach { item ->
                FilterChip(selected=focus==item,onClick={focus=item},label={Text(item)})
            }
        }
        Text("Energy · ${energy.toInt()} of 5")
        Slider(value=energy,onValueChange={ energy=it },valueRange=1f..5f,steps=3)
        Text("Soreness")
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            listOf("Low","Medium","High").forEachIndexed { i,name -> FilterChip(selected=soreness==i,onClick={ soreness=i },label={Text(name)}) }
        }
        Text("Time available · ${minutes.toInt()} min")
        Slider(value=minutes,onValueChange={ minutes=it },valueRange=10f..90f,steps=15)
    }
    if(energy<=2 || soreness==2 || feeling in setOf("Tired","Sore / Stiff","Recovering")) {
        Text("Today’s plan will use lighter volume and recovery-friendly sets.")
    }
    if(p.restrictions.isNotBlank()) Text("Your note: ${p.restrictions}. Excluded exercises are filtered; review the plan before lifting.")
    val check=CheckIn(energy.toInt(),soreness,minutes.toInt(),feeling,focus)
    PrimaryButton("Start ${focus.substringBefore(" (")} workout") { onStart(check) }
    if(onAskCoach!=null) {
        OutlinedButton(onClick={ onAskCoach(check) },modifier=Modifier.fillMaxWidth()) {
            Text("Ask AI Coach to customize today’s exercises")
        }
    }
}
@Composable fun LibraryScreen(state:AppState,onExercise:(String)->Unit,onRoutine:(SavedRoutine)->Unit,onSession:(String)->Unit) {
    var tab by remember { mutableIntStateOf(0) }
    SectionTitle("Workouts","Your training, organized.")
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        listOf("Exercises","Routines","History").forEachIndexed { i,t -> FilterChip(selected=tab==i,onClick={tab=i},label={Text(t)}) }
    }
    when(tab) {
        0 -> {
            var query by remember { mutableStateOf("") }
            OutlinedTextField(value=query,onValueChange={query=it},label={Text("Find an exercise")},modifier=Modifier.fillMaxWidth(),singleLine=true)
            Catalog.exercises.filter { it.name.contains(query,true) || it.muscles.contains(query,true) }.forEach {
                ExerciseRow(PlannedExercise(it.id)) { onExercise(it.id) }
            }
        }
        1 -> if(state.routines.isEmpty()) EmptyState("Make it your routine","Save a completed workout as a routine from its summary.")
            else state.routines.forEach { r -> PanelCard { SectionTitle(r.name,"${r.plan.size} exercises"); PrimaryButton("Start routine",state.active==null) {onRoutine(r)} } }
        2 -> if(state.sessions.none {it.finishedAt!=null}) EmptyState("Your history starts here","Complete your first workout to see it here.")
            else state.sessions.filter {it.finishedAt!=null}.reversed().forEach { s ->
                PanelCard { SectionTitle(s.title,"${s.results.size} logged sets · "+Instant.ofEpochMilli(s.startedAt).atZone(ZoneId.systemDefault()).toLocalDate())
                    TextButton(onClick={onSession(s.id)}) {Text("View summary →")} }
            }
    }
}
