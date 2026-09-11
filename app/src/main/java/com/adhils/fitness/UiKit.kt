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

val Mint=Color(0xFFB4F5D5)
val Dark=Color(0xFF101614)
val Panel=Color(0xFF1B2420)
private val DarkColors=darkColorScheme(primary=Mint,onPrimary=Dark,background=Dark,surface=Dark,
    surfaceContainer=Panel,surfaceVariant=Panel,onSurface=Color(0xFFF2F6F3),onSurfaceVariant=Color(0xFFB1BDB6),
    outline=Color(0xFF38473E),secondary=Color(0xFFE6B85A),secondaryContainer=Color(0xFF2C4839),onSecondaryContainer=Mint,
    primaryContainer=Color(0xFF254936),onPrimaryContainer=Mint)
private val LightColors=lightColorScheme(primary=Color(0xFF176B47),onPrimary=Color.White,background=Color(0xFFF5F8F5),
    surface=Color(0xFFF5F8F5),surfaceContainer=Color(0xFFE7EEE8),surfaceVariant=Color(0xFFE7EEE8),onSurface=Dark,
    onSurfaceVariant=Color(0xFF475C4E),outline=Color(0xFF708778),secondaryContainer=Color(0xFFD1E8D7),onSecondaryContainer=Color(0xFF164A2B))
@Composable fun FitnessTheme(theme:String,content:@Composable ()->Unit) {
    val dark=theme=="Dark" || (theme=="System" && isSystemInDarkTheme())
    MaterialTheme(colorScheme=if(dark) DarkColors else LightColors,content=content)
}
@Composable fun PanelCard(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit) {
    Surface(modifier=modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),color=MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
    }
}
@Composable fun PrimaryButton(text:String,enabled:Boolean=true,onClick:()->Unit) {
    Button(onClick=onClick,enabled=enabled,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp),
        shape=RoundedCornerShape(14.dp),contentPadding=PaddingValues(12.dp)) {
        Text(text,fontWeight=FontWeight.SemiBold,fontSize=16.sp)
    }
}
@Composable fun SectionTitle(title:String,subtitle:String?=null) {
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Text(title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
        if(subtitle!=null) Text(subtitle,color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodyMedium)
    }
}
@Composable fun SmallLabel(text:String) { Text(text,color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.labelMedium) }
@Composable fun ScreenHeader(title:String,onBack:(()->Unit)?=null,action:(@Composable ()->Unit)?=null) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        if(onBack!=null) IconButton(onClick=onBack) { Icon(Icons.Default.ArrowBack,"Back") }
        Text(title,Modifier.weight(1f),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
        action?.invoke()
    }
}
@Composable fun ExerciseRow(p:PlannedExercise,onClick:()->Unit) {
    val e=Catalog.get(p.exerciseId)
    Surface(onClick=onClick,shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.surfaceVariant,RoundedCornerShape(10.dp)),Alignment.Center) {
                Icon(if(e.timed) Icons.Default.Timer else Icons.Default.FitnessCenter,null,tint=MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f)) {
                Text(e.name,style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Medium)
                SmallLabel("${p.sets} sets · "+if(e.timed) "${p.seconds} sec" else "${p.minReps}–${p.maxReps} reps")
            }
            if(e.camera!=null) Icon(Icons.Default.Videocam,"Camera supported",Modifier.size(20.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
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
    Canvas(Modifier.fillMaxWidth().height(160.dp)) {
        val w=size.width; val h=size.height
        drawLine(muted,Offset(w*.1f,h*.9f),Offset(w*.9f,h*.9f),2.dp.toPx())
        val coords=if(exercise.camera in listOf("plank","pushup")) listOf(.25f to .42f,.32f to .5f,.55f to .58f,.8f to .68f,.32f to .82f)
            else if(exercise.camera=="rdl") listOf(.38f to .2f,.42f to .33f,.58f to .48f,.55f to .68f,.55f to .88f,.42f to .66f)
            else listOf(.5f to .12f,.5f to .27f,.5f to .5f,.4f to .7f,.36f to .88f,.6f to .7f,.64f to .88f,.3f to .35f,.7f to .35f)
        val points=coords.map { Offset(it.first*w,it.second*h) }
        drawCircle(accent,10.dp.toPx(),points[0])
        val edges=if(points.size==5) listOf(0 to 1,1 to 2,2 to 3,1 to 4)
            else if(points.size==6) listOf(0 to 1,1 to 2,2 to 3,3 to 4,1 to 5)
            else listOf(0 to 1,1 to 2,2 to 3,3 to 4,2 to 5,5 to 6,1 to 7,1 to 8)
        edges.forEach { (a,b)->drawLine(accent,points[a],points[b],6.dp.toPx(),StrokeCap.Round) }
    }
}
