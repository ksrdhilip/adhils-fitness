package com.adhils.fitness

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adhils.fitness.core.*

@OptIn(ExperimentalLayoutApi::class)
@Composable fun ProfileScreen(original:Profile,onSave:(Profile)->Unit,onBack:(()->Unit)?) {
    var p by remember(original) { mutableStateOf(original) }
    ScreenHeader(if(original.onboardingComplete) "Edit profile" else "Make it yours",onBack)
    Text("Plans, workout history, progress, and AI suggestions belong to this profile.")
    OutlinedTextField(value=p.name,onValueChange={p=p.copy(name=it.take(80))},label={Text("Name")},modifier=Modifier.fillMaxWidth(),singleLine=true)
    SectionTitle("Your goal")
    FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        listOf("Build muscle","Get stronger","General fitness","Support weight management").forEach { goal -> FilterChip(selected=p.goal==goal,onClick={p=p.copy(goal=goal)},label={Text(goal)}) }
    }
    SectionTitle("Experience")
    FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        listOf("Beginner","Intermediate","Advanced").forEach { v -> FilterChip(selected=p.experience==v,onClick={p=p.copy(experience=v)},label={Text(v)}) }
    }
    SectionTitle("Gym Environment Profile","Switch instantly between saved gym environments.")
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        listOf(
            "Home Gym" to setOf("Bodyweight","Dumbbells","Bands"),
            "Commercial Gym" to setOf("Bodyweight","Dumbbells","Bands","Bench"),
            "Bodyweight / Travel" to setOf("Bodyweight")
        ).forEach { (preset,eq) ->
            FilterChip(
                selected=p.gymPreset==preset,
                onClick={p=p.copy(gymPreset=preset,equipment=eq)},
                label={Text(preset)},
                modifier=Modifier.heightIn(min=48.dp)
            )
        }
    }
    SectionTitle("Available Equipment","Grouped multi-select equipment filters for ${p.gymPreset}")
    PanelCard {
        listOf(
            Triple("Free Weights","Dumbbells","Adjustable or fixed dumbbells (goblet squats, presses, rows, curls)"),
            Triple("Benches & Racks","Bench","Flat or incline workout bench (bench press, hip thrusts, split squats, step-ups)"),
            Triple("Bands & Cables","Bands","Loop or handle resistance bands (band rows, face pulls, band RDLs, pressdowns)"),
            Triple("Bodyweight","Bodyweight","Calisthenics & floor movements (push-ups, planks, lunges, bridges)")
        ).forEach { (category,eqKey,desc) ->
            val checked=eqKey in p.equipment
            Surface(
                onClick={p=p.copy(equipment=if(checked) p.equipment-eqKey else p.equipment+eqKey)},
                shape=androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                color=MaterialTheme.colorScheme.surfaceVariant,
                modifier=Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min=56.dp).padding(horizontal=12.dp,vertical=10.dp),
                    verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(12.dp)
                ) {
                    Checkbox(checked=checked,onCheckedChange={c->p=p.copy(equipment=if(c) p.equipment+eqKey else p.equipment-eqKey)})
                    Column(Modifier.weight(1f)) {
                        Text("$category · $eqKey",fontWeight=androidx.compose.ui.text.font.FontWeight.Bold)
                        SmallLabel(desc)
                    }
                }
            }
        }
    }
    PanelCard {
        Text("Weekly goal · ${p.days} workouts")
        Slider(value=p.days.toFloat(),onValueChange={p=p.copy(days=it.toInt())},valueRange=1f..7f,steps=5)
        Text("Session length · ${p.minutes} minutes")
        Slider(value=p.minutes.toFloat(),onValueChange={p=p.copy(minutes=it.toInt())},valueRange=10f..90f,steps=15)
        SmallLabel("WEIGHT UNIT & INCREMENT STEP")
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("lb","kg").forEach { unit -> FilterChip(selected=p.unit==unit,onClick={p=p.copy(unit=unit)},label={Text(unit)}) } }
        FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            listOf(1.0,2.5,5.0,10.0).forEach { step ->
                FilterChip(selected=p.weightStep==step,onClick={p=p.copy(weightStep=step)},label={Text("${decimal(step)} ${p.unit} step")})
            }
        }
    }
    OutlinedTextField(value=p.restrictions,onValueChange={p=p.copy(restrictions=it.take(1000))},label={Text("Limitations or preferences (optional)")},modifier=Modifier.fillMaxWidth())
    Text("Use the exclusions below to prevent exercises from appearing. Notes are shared with your selected AI provider when you ask for coaching; they are not a medical assessment.")
    var exclusions by remember { mutableStateOf(false) }
    TextButton(onClick={exclusions=!exclusions}) {Text("Exercise exclusions (${p.excluded.size})")}
    if(exclusions) Catalog.exercises.forEach { e -> Row(Modifier.fillMaxWidth()) {
        Checkbox(checked=e.id in p.excluded,onCheckedChange={checked->p=p.copy(excluded=if(checked) p.excluded+e.id else p.excluded-e.id)})
        Text(e.name,Modifier.padding(top=12.dp))
    } }
    PrimaryButton("Save profile",p.name.isNotBlank()) {onSave(p)}
}
@Composable fun ProfilePicker(store:ProfileStore,onSelect:(String)->Unit,onAdd:(String)->Unit,onDismiss:()->Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest=onDismiss,title={Text("Switch profile")},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            store.profiles.forEach { record -> OutlinedButton(onClick={onSelect(record.id)},modifier=Modifier.fillMaxWidth()) {
                Text((if(record.id==store.selectedId) "✓ " else "")+record.state.profile.name)
            } }
            HorizontalDivider()
            OutlinedTextField(value=name,onValueChange={name=it.take(80)},label={Text("New profile name")},singleLine=true)
            Button(onClick={onAdd(name)},enabled=name.isNotBlank() && store.profiles.size<20) {Text("Add profile")}
            SmallLabel("Each profile keeps its own active workout. Profiles are not password-protected accounts.")
        }
    },confirmButton={TextButton(onClick=onDismiss) {Text("Done")}})
}
