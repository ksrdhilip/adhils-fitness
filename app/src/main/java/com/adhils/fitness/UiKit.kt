package com.adhils.fitness

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import com.adhils.fitness.core.*

val Mint=Color(0xFF30D158)
val FitbodRed=Color(0xFFFF2D55)
val FitbodPink=Color(0xFFFF6B8B)
val Dark=Color(0xFF000000)
val Panel=Color(0xFF1C1C1E)
val ElevatedCell=Color(0xFF2C2C2E)
private val DarkColors=darkColorScheme(
    primary=FitbodRed,onPrimary=Color.White,
    background=Dark,surface=Dark,
    surfaceContainer=Panel,surfaceVariant=ElevatedCell,
    onSurface=Color(0xFFFFFFFF),onSurfaceVariant=Color(0xFF98989F),
    outline=Color(0xFF3A3A3C),
    secondary=Mint,onSecondary=Dark,
    secondaryContainer=Color(0xFF1E3A2B),onSecondaryContainer=Mint,
    primaryContainer=Color(0xFF3B1520),onPrimaryContainer=Color(0xFFFFB3C1)
)
private val LightColors=lightColorScheme(primary=Color(0xFFE01E45),onPrimary=Color.White,background=Color(0xFFF2F2F7),
    surface=Color(0xFFFFFFFF),surfaceContainer=Color(0xFFFFFFFF),surfaceVariant=Color(0xFFE5E5EA),onSurface=Color(0xFF1C1C1E),
    onSurfaceVariant=Color(0xFF636366),outline=Color(0xFFC7C7CC),secondaryContainer=Color(0xFFD1F2D9),onSecondaryContainer=Color(0xFF115E27))
@Composable fun FitnessTheme(theme:String,content:@Composable ()->Unit) {
    val dark=theme=="Dark" || (theme=="System" && isSystemInDarkTheme())
    MaterialTheme(colorScheme=if(dark) DarkColors else LightColors,content=content)
}
@Composable fun PanelCard(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit) {
    Surface(modifier=modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
    }
}
@Composable fun PrimaryButton(text:String,enabled:Boolean=true,onClick:()->Unit) {
    Button(onClick=onClick,enabled=enabled,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp),
        shape=RoundedCornerShape(14.dp),contentPadding=PaddingValues(12.dp)) {
        Text(text,fontWeight=FontWeight.Bold,fontSize=16.sp)
    }
}
@Composable fun SectionTitle(title:String,subtitle:String?=null) {
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Text(title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
        if(subtitle!=null) Text(subtitle,color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodyMedium)
    }
}
@Composable fun SmallLabel(text:String) { Text(text,color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.labelMedium) }
@Composable fun ScreenHeader(title:String,onBack:(()->Unit)?=null,action:(@Composable ()->Unit)?=null) {
    Row(Modifier.fillMaxWidth().heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        if(onBack!=null) IconButton(onClick=onBack,modifier=Modifier.size(48.dp)) { Icon(Icons.Default.ArrowBack,"Back") }
        Text(title,Modifier.weight(1f),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
        action?.invoke()
    }
}
@Composable fun ExerciseRow(p:PlannedExercise,onClick:()->Unit) {
    val e=Catalog.get(p.exerciseId)
    Surface(onClick=onClick,shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.fillMaxWidth().heightIn(min=64.dp).padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(52.dp).background(MaterialTheme.colorScheme.surfaceVariant,RoundedCornerShape(12.dp)),Alignment.Center) {
                Icon(if(e.timed) Icons.Default.Timer else Icons.Default.PlayCircleOutline,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(26.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    Text(e.name,style=MaterialTheme.typography.bodyLarge,fontWeight=FontWeight.SemiBold)
                    if(p.supersetGroup!=null) {
                        Surface(shape=RoundedCornerShape(6.dp),color=MaterialTheme.colorScheme.primaryContainer) {
                            Text("SUPERSET ${p.supersetGroup}",Modifier.padding(horizontal=6.dp,vertical=2.dp),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
                SmallLabel("${p.sets} sets · "+if(e.timed) "${p.seconds} sec" else "${p.minReps}–${p.maxReps} reps"+" · ${e.equipment} · ${e.muscles}")
            }
            Icon(Icons.Default.Videocam,"AI Camera supported",Modifier.size(22.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@Composable fun EmptyState(title:String,message:String,icon:ImageVector=Icons.Default.FitnessCenter) {
    PanelCard {
        Icon(icon,null,Modifier.size(32.dp),tint=MaterialTheme.colorScheme.primary)
        SectionTitle(title,message)
    }
}
fun decimal(value:Double):String = if(value%1.0<.05) "%.0f".format(java.util.Locale.US,value) else "%.1f".format(java.util.Locale.US,value)
fun weightLabel(value:Double,p:Profile)=decimal(fromKg(value,p.unit))+" "+p.unit
fun durationText(ms:Long):String {
    val s=(ms.coerceAtLeast(0)/1000).toInt()
    return "%02d:%02d".format(s/60,s%60)
}
@Composable fun PlacementDrawing(exercise:Exercise) {
    val accent=MaterialTheme.colorScheme.primary
    val muted=MaterialTheme.colorScheme.outline
    val cam=exercise.effectiveCamera
    Canvas(Modifier.fillMaxWidth().height(160.dp)) {
        val w=size.width; val h=size.height
        drawLine(muted,Offset(w*.1f,h*.9f),Offset(w*.9f,h*.9f),2.dp.toPx())
        val coords=if(cam in listOf("plank","pushup")) listOf(.25f to .42f,.32f to .5f,.55f to .58f,.8f to .68f,.32f to .82f)
            else if(cam=="rdl") listOf(.38f to .2f,.42f to .33f,.58f to .48f,.55f to .68f,.55f to .88f,.42f to .66f)
            else listOf(.5f to .12f,.5f to .27f,.5f to .5f,.4f to .7f,.36f to .88f,.6f to .7f,.64f to .88f,.3f to .35f,.7f to .35f)
        val points=coords.map { Offset(it.first*w,it.second*h) }
        drawCircle(accent,10.dp.toPx(),points[0])
        val edges=if(points.size==5) listOf(0 to 1,1 to 2,2 to 3,1 to 4)
            else if(points.size==6) listOf(0 to 1,1 to 2,2 to 3,3 to 4,1 to 5)
            else listOf(0 to 1,1 to 2,2 to 3,3 to 4,2 to 5,5 to 6,1 to 7,1 to 8)
        edges.forEach { (a,b)->drawLine(accent,points[a],points[b],6.dp.toPx(),StrokeCap.Round) }
    }
}

// Fitbod Heat Map Color Gradient:
// Acute Fatigue (<50% recovery) -> Vibrant Fitbod Red/Pink (#FF2D55)
// Moderate Fatigue (50-79% recovery) -> Warm Rose Pink (#FF6B8B)
// Full Recovery (80-100% recovery) -> Translucent Dark Gray (#3A3A3C) on avatar
private fun avatarHeatmapColor(percent:Int):Color = when {
    percent<50 -> Color(0xFFFF2D55)
    percent<80 -> Color(0xFFFF6B8B)
    else -> Color(0xFF3A3A3C)
}

private fun badgeStatusColor(percent:Int):Color = when {
    percent<50 -> Color(0xFFFF2D55)
    percent<80 -> Color(0xFFFF9F0A)
    else -> Color(0xFF30D158)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun MuscleHeatmapCard(statuses:List<MuscleStatus>,defaultExpanded:Boolean=false) {
    val byName=remember(statuses) { statuses.associateBy { it.muscle } }
    fun pct(m:String)=byName[m]?.recoveryPercent ?: 100
    val avg=remember(statuses) { if(statuses.isEmpty()) 100 else statuses.sumOf { it.recoveryPercent }/statuses.size }
    val fresh=remember(statuses) { statuses.filter { it.recoveryPercent>=80 }.sortedByDescending { it.recoveryPercent }.take(4).map { it.muscle } }
    var expanded by remember { mutableStateOf(defaultExpanded) }
    var selectedMuscle by remember { mutableStateOf<String?>(null) }
    PanelCard {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SmallLabel("FITBOD MUSCLE RECOVERY HEAT MAP")
                Text("Body Recovery · $avg%",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                if(fresh.isNotEmpty()) SmallLabel("Fresh muscle groups: ${fresh.joinToString(", ")}")
            }
            TextButton(onClick={expanded=!expanded},modifier=Modifier.heightIn(min=48.dp)) {
                Text(if(expanded) "Collapse ▴" else "Details ▾")
            }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(10.dp).background(Color(0xFFFF2D55),RoundedCornerShape(5.dp)))
                SmallLabel("Fatigued (<50%)")
            }
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(10.dp).background(Color(0xFFFF6B8B),RoundedCornerShape(5.dp)))
                SmallLabel("Recovering (50–79%)")
            }
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(10.dp).background(Color(0xFF3A3A3C),RoundedCornerShape(5.dp)))
                SmallLabel("Fresh (80–100%)")
            }
        }
        val headOutline=Color(0xFF48484A)
        Canvas(Modifier.fillMaxWidth().height(195.dp)) {
            val w=size.width; val h=size.height
            val fCx=w*0.27f; val bCx=w*0.73f
            // Front and Back Heads
            drawCircle(headOutline,radius=h*0.065f,center=Offset(fCx,h*0.11f))
            drawCircle(headOutline,radius=h*0.065f,center=Offset(bCx,h*0.11f))
            // FRONT BODY
            drawCircle(avatarHeatmapColor(pct("Shoulders")),radius=h*0.055f,center=Offset(fCx-w*0.085f,h*0.24f))
            drawCircle(avatarHeatmapColor(pct("Shoulders")),radius=h*0.055f,center=Offset(fCx+w*0.085f,h*0.24f))
            drawRoundRect(avatarHeatmapColor(pct("Chest")),topLeft=Offset(fCx-w*0.065f,h*0.20f),size=androidx.compose.ui.geometry.Size(w*0.13f,h*0.12f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(10f,10f))
            drawRoundRect(avatarHeatmapColor(pct("Core")),topLeft=Offset(fCx-w*0.055f,h*0.33f),size=androidx.compose.ui.geometry.Size(w*0.11f,h*0.17f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(8f,8f))
            drawRoundRect(avatarHeatmapColor(pct("Biceps")),topLeft=Offset(fCx-w*0.115f,h*0.29f),size=androidx.compose.ui.geometry.Size(w*0.038f,h*0.16f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(8f,8f))
            drawRoundRect(avatarHeatmapColor(pct("Biceps")),topLeft=Offset(fCx+w*0.077f,h*0.29f),size=androidx.compose.ui.geometry.Size(w*0.038f,h*0.16f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(8f,8f))
            drawRoundRect(avatarHeatmapColor(pct("Quads")),topLeft=Offset(fCx-w*0.062f,h*0.52f),size=androidx.compose.ui.geometry.Size(w*0.052f,h*0.22f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(10f,10f))
            drawRoundRect(avatarHeatmapColor(pct("Quads")),topLeft=Offset(fCx+w*0.010f,h*0.52f),size=androidx.compose.ui.geometry.Size(w*0.052f,h*0.22f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(10f,10f))
            drawRoundRect(avatarHeatmapColor(pct("Calves")),topLeft=Offset(fCx-w*0.055f,h*0.76f),size=androidx.compose.ui.geometry.Size(w*0.042f,h*0.17f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(8f,8f))
            drawRoundRect(avatarHeatmapColor(pct("Calves")),topLeft=Offset(fCx+w*0.013f,h*0.76f),size=androidx.compose.ui.geometry.Size(w*0.042f,h*0.17f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(8f,8f))

            // BACK BODY
            drawCircle(avatarHeatmapColor(pct("Shoulders")),radius=h*0.055f,center=Offset(bCx-w*0.085f,h*0.24f))
            drawCircle(avatarHeatmapColor(pct("Shoulders")),radius=h*0.055f,center=Offset(bCx+w*0.085f,h*0.24f))
            drawRoundRect(avatarHeatmapColor(pct("Back")),topLeft=Offset(bCx-w*0.068f,h*0.20f),size=androidx.compose.ui.geometry.Size(w*0.136f,h*0.26f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(10f,10f))
            drawRoundRect(avatarHeatmapColor(pct("Triceps")),topLeft=Offset(bCx-w*0.115f,h*0.29f),size=androidx.compose.ui.geometry.Size(w*0.038f,h*0.16f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(8f,8f))
            drawRoundRect(avatarHeatmapColor(pct("Triceps")),topLeft=Offset(bCx+w*0.077f,h*0.29f),size=androidx.compose.ui.geometry.Size(w*0.038f,h*0.16f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(8f,8f))
            drawRoundRect(avatarHeatmapColor(pct("Glutes")),topLeft=Offset(bCx-w*0.064f,h*0.47f),size=androidx.compose.ui.geometry.Size(w*0.128f,h*0.11f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(12f,12f))
            drawRoundRect(avatarHeatmapColor(pct("Hamstrings")),topLeft=Offset(bCx-w*0.062f,h*0.59f),size=androidx.compose.ui.geometry.Size(w*0.052f,h*0.16f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(10f,10f))
            drawRoundRect(avatarHeatmapColor(pct("Hamstrings")),topLeft=Offset(bCx+w*0.010f,h*0.59f),size=androidx.compose.ui.geometry.Size(w*0.052f,h*0.16f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(10f,10f))
            drawRoundRect(avatarHeatmapColor(pct("Calves")),topLeft=Offset(bCx-w*0.055f,h*0.76f),size=androidx.compose.ui.geometry.Size(w*0.042f,h*0.17f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(8f,8f))
            drawRoundRect(avatarHeatmapColor(pct("Calves")),topLeft=Offset(bCx+w*0.013f,h*0.76f),size=androidx.compose.ui.geometry.Size(w*0.042f,h*0.17f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(8f,8f))
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceAround) {
            SmallLabel("FRONT (Chest · Quads · Core · Biceps)")
            SmallLabel("BACK (Back · Glutes · Hamstrings · Triceps)")
        }
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            statuses.forEach { st ->
                Surface(
                    onClick={ selectedMuscle=if(selectedMuscle==st.muscle) null else st.muscle; expanded=true },
                    shape=RoundedCornerShape(10.dp),
                    color=if(selectedMuscle==st.muscle) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(Modifier.heightIn(min=40.dp).padding(horizontal=12.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(8.dp).background(badgeStatusColor(st.recoveryPercent),RoundedCornerShape(4.dp)))
                        Text("${st.muscle} ${st.recoveryPercent}%",style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.SemiBold)
                    }
                }
            }
        }
        if(expanded) {
            HorizontalDivider(color=MaterialTheme.colorScheme.outline)
            SmallLabel("GRANULAR MUSCLE RECOVERY STATUS (TAP TO FILTER)")
            val shownList=if(selectedMuscle!=null) statuses.filter { it.muscle==selectedMuscle } else statuses
            shownList.forEach { st ->
                Column(Modifier.fillMaxWidth().padding(vertical=4.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        Text(st.muscle,fontWeight=FontWeight.SemiBold)
                        val recText=if(st.lastTrainedHoursAgo!=null) "${st.recoveryPercent}% · ${st.workingSetsLast7Days} sets (7d) · ${st.lastTrainedHoursAgo}h ago"
                            else "${st.recoveryPercent}% · Fully recovered (${st.workingSetsLast7Days} sets in 7d)"
                        SmallLabel(recText)
                    }
                    LinearProgressIndicator(
                        progress={ st.recoveryPercent / 100f },
                        modifier=Modifier.fillMaxWidth().height(8.dp),
                        color=badgeStatusColor(st.recoveryPercent),
                        trackColor=MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}
