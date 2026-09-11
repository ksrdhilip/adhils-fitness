package com.adhils.fitness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adhils.fitness.core.*

class MainActivity:ComponentActivity() {
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { FitnessApp() }
    }
}
@Composable fun FitnessApp(vm:FitnessViewModel=viewModel()) {
    val store by vm.store.collectAsState()
    val ready by vm.ready.collectAsState()
    val error by vm.error.collectAsState()
    val record=store.selected
    val context=LocalContext.current
    val dark=record.state.profile.theme=="Dark" || (record.state.profile.theme=="System" && isSystemInDarkTheme())
    DisposableEffect(dark) {
        val bar=if(dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT) else SystemBarStyle.light(android.graphics.Color.TRANSPARENT,android.graphics.Color.TRANSPARENT)
        (context as? ComponentActivity)?.enableEdgeToEdge(statusBarStyle=bar,navigationBarStyle=bar)
        onDispose {}
    }
    FitnessTheme(record.state.profile.theme) {
        if(!ready) Surface(Modifier.fillMaxSize()) { Box(contentAlignment=Alignment.Center) { CircularProgressIndicator() } }
        else key(record.id) { AppContent(vm,store,record.state) }
        error?.let { text -> AlertDialog(onDismissRequest={vm.error.value=null},title={Text("Could not complete that action")},
            text={Text(text)},confirmButton={TextButton(onClick={vm.error.value=null}) {Text("OK")}}) }
    }
}
@Composable private fun AppContent(vm:FitnessViewModel,store:ProfileStore,state:AppState) {
    var route by rememberSaveable { mutableStateOf(if(state.profile.onboardingComplete) "today" else "profile") }
    var previous by rememberSaveable { mutableStateOf("today") }
    var profiles by remember { mutableStateOf(false) }
    var leave by remember { mutableStateOf(false) }
    var discard by remember { mutableStateOf(false) }
    val pageScroll=remember(route,if(route=="active") state.active?.currentExercise else null) {ScrollState(0)}
    val roots=listOf("today","workouts","progress","coach","settings")
    val labels=listOf("Today","Workouts","Progress","Coach","Settings")
    val icons=listOf(Icons.Default.Home,Icons.Default.FitnessCenter,Icons.Default.BarChart,Icons.Default.ChatBubbleOutline,Icons.Default.Settings)
    fun showExercise(id:String) { previous=route; route="exercise:$id" }
    fun back() { route=when { route=="camera"->"active"; route.startsWith("exercise:")->previous; else->"today" } }
    BackHandler(route !in roots) { if(route=="active") leave=true else back() }
    BoxWithConstraints {
        val tablet=maxWidth>=600.dp
        Scaffold(bottomBar={ if(!tablet && route in roots) NavigationBar {
            roots.forEachIndexed { i,name -> NavigationBarItem(selected=route==name,onClick={route=name},
                icon={Icon(icons[i],labels[i])},label={Text(labels[i],maxLines=1)}) }
        } }) { padding ->
            Row(Modifier.fillMaxSize().padding(padding)) {
                if(tablet && route in roots) NavigationRail {
                    roots.forEachIndexed { i,name -> NavigationRailItem(selected=route==name,onClick={route=name},
                        icon={Icon(icons[i],labels[i])},label={Text(labels[i])}) }
                }
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(pageScroll).padding(horizontal=20.dp,vertical=12.dp),
                    horizontalAlignment=Alignment.CenterHorizontally) {
                    Column(Modifier.widthIn(max=700.dp).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                        if(route in roots) ScreenHeader("ADhils Fitness",action={
                            FilledTonalButton(onClick={profiles=true}) { Text(state.profile.name.take(18)+" ▾") }
                        })
                        when {
                            route=="profile" -> ProfileScreen(state.profile,onSave={vm.saveProfile(it);route="today"},onBack=if(state.profile.onboardingComplete) ({route="settings"}) else null)
                            route=="today" -> TodayScreen(state,{route="checkin"},{route="active"},::showExercise,{route="workouts"})
                            route=="checkin" -> CheckInScreen(state.profile,{vm.start(it);route="active"},{route="today"})
                            route=="workouts" -> LibraryScreen(state,::showExercise,{vm.start(CheckIn(minutes=state.profile.minutes),it);route="active"},{route="summary:$it"})
                            route=="active" -> state.active?.let { s -> WorkoutScreen(state,s,vm,{leave=true},::showExercise,{route="camera"},{vm.finish();route="summary:${s.id}"}) }
                                ?: EmptyState("Getting your workout ready","Your sets are saved as you train.")
                            route=="camera" -> state.active?.let { s -> CameraCoachScreen(Catalog.get(s.plan[s.currentExercise].exerciseId),state.profile,{route="active"}) { reps,seconds,notes ->
                                CameraDraft.value=CameraSetDraft(store.selectedId,s.id,s.plan[s.currentExercise].exerciseId,reps,seconds,notes);route="active"
                            } }
                            route.startsWith("exercise:") -> ExerciseScreen(route.substringAfter(":"),state,vm,::back)
                            route.startsWith("summary:") -> state.sessions.find {it.id==route.substringAfter(":")}?.let { SummaryScreen(it,state,vm,{route="today"},{route="coach"}) }
                            route=="progress" -> ProgressScreen(state,vm)
                            route=="coach" -> CoachScreen(state,vm)
                            route=="settings" -> SettingsScreen(state,vm,{route="profile"},{profiles=true})
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
    if(profiles) ProfilePicker(store,onSelect={vm.selectProfile(it);profiles=false},onAdd={vm.addProfile(it);profiles=false},onDismiss={profiles=false})
    if(leave) AlertDialog(onDismissRequest={leave=false},title={Text("Leave this workout?")},text={Text("Your completed sets are saved for ${state.profile.name}. You can resume from Today.")},
        confirmButton={TextButton(onClick={leave=false;route="today"}) {Text("Save and leave")}},
        dismissButton={Row {TextButton(onClick={leave=false}) {Text("Continue")};TextButton(onClick={leave=false;discard=true}) {Text("Discard")}}})
    if(discard) AlertDialog(onDismissRequest={discard=false},title={Text("Discard this workout?")},text={Text("This removes its logged sets from this profile.")},
        confirmButton={TextButton(onClick={vm.discard();discard=false;route="today"}) {Text("Discard workout")}},dismissButton={TextButton(onClick={discard=false}) {Text("Keep it")}})
}
