package com.adhils.fitness

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.*
import com.adhils.fitness.core.*

val Mint = Color(0xFF30D158)
val BrandRed = Color(0xFFFF2D55)
val BrandPink = Color(0xFFFF6B8B)
val Dark = Color(0xFF000000)
val Panel = Color(0xFF1C1C1E)
val ElevatedCell = Color(0xFF2C2C2E)

private val DarkColors = darkColorScheme(
    primary = BrandRed, onPrimary = Color.White,
    background = Dark, surface = Dark,
    surfaceContainer = Panel, surfaceVariant = ElevatedCell,
    onSurface = Color(0xFFFFFFFF), onSurfaceVariant = Color(0xFF98989F),
    outline = Color(0xFF3A3A3C),
    secondary = Mint, onSecondary = Dark,
    secondaryContainer = Color(0xFF1E3A2B), onSecondaryContainer = Mint,
    primaryContainer = Color(0xFF3B1520), onPrimaryContainer = Color(0xFFFFB3C1)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFE01E45), onPrimary = Color.White,
    background = Color(0xFFF2F2F7), surface = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFE5E5EA),
    onSurface = Color(0xFF1C1C1E), onSurfaceVariant = Color(0xFF636366),
    outline = Color(0xFFC7C7CC),
    secondaryContainer = Color(0xFFD1F2D9), onSecondaryContainer = Color(0xFF115E27)
)

@Composable
fun FitnessTheme(theme: String, content: @Composable () -> Unit) {
    val dark = theme == "Dark" || (theme == "System" && isSystemInDarkTheme())
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}

@Composable
fun PanelCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(12.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (subtitle != null) Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun SmallLabel(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
}

@Composable
fun ScreenHeader(title: String, onBack: (() -> Unit)? = null, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (onBack != null) IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.ArrowBack, "Back") }
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        action?.invoke()
    }
}

/**
 * Beautiful, cross-platform exercise thumbnail displaying real movement badges and muscle categories.
 */
@Composable
fun ExerciseThumbnail(
    exerciseId: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    contentDescription: String? = null,
    showVideoIndicator: Boolean = false,
    isVideoExpanded: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val e = Catalog.get(exerciseId)
    val catColor = when (e.muscles.lowercase()) {
        "chest", "pecs" -> Color(0xFFFF2D55)
        "back", "lats" -> Color(0xFF007AFF)
        "legs", "quads", "hamstrings", "glutes" -> Color(0xFF34C759)
        "shoulders", "delts" -> Color(0xFFFF9500)
        "arms", "biceps", "triceps" -> Color(0xFFAF52DE)
        "core", "abs" -> Color(0xFF5856D6)
        else -> MaterialTheme.colorScheme.primary
    }

    var imageBitmap by remember(exerciseId) { mutableStateOf(ExerciseImageLoader.getCached(exerciseId)) }

    LaunchedEffect(exerciseId) {
        if (imageBitmap == null) {
            imageBitmap = ExerciseImageLoader.loadExerciseImage(exerciseId)
        }
    }

    val clickableMod = if (onClick != null) modifier.clip(shape).clickable { onClick() } else modifier.clip(shape)

    Box(
        modifier = clickableMod
            .background(
                Brush.linearGradient(
                    colors = listOf(catColor.copy(alpha = 0.28f), MaterialTheme.colorScheme.surfaceVariant)
                ),
                shape
            )
            .border(1.dp, catColor.copy(alpha = 0.35f), shape),
        contentAlignment = Alignment.Center
    ) {
        val currentBitmap = imageBitmap
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap,
                contentDescription = contentDescription ?: e.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))
                        )
                    )
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(4.dp)
            ) {
                Icon(
                    imageVector = when {
                        e.equipment == "Bodyweight" -> Icons.Default.DirectionsRun
                        e.timed -> Icons.Default.Timer
                        e.equipment == "Bands" -> Icons.Default.LinearScale
                        else -> Icons.Default.FitnessCenter
                    },
                    contentDescription = contentDescription ?: e.name,
                    tint = catColor,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = e.muscles.take(5).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = catColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    maxLines = 1
                )
            }
        }

        if (showVideoIndicator) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .background(
                        if (isVideoExpanded) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.7f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isVideoExpanded) {
                    Text(
                        "▾",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Show video",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ExerciseRow(
    p: PlannedExercise,
    onCameraClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val e = Catalog.get(p.exerciseId)
    Surface(onClick = onClick, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ExerciseThumbnail(
                exerciseId = e.id,
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(12.dp),
                contentDescription = e.name
            )
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(e.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    if (p.supersetGroup != null) {
                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Text("SUPERSET ${p.supersetGroup}", Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
                SmallLabel("${p.sets} sets · " + if (e.timed) "${p.seconds} sec" else "${p.minReps}–${p.maxReps} reps" + " · ${e.equipment} · ${e.muscles}")
            }
            if (onCameraClick != null) {
                IconButton(
                    onClick = onCameraClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Videocam,
                        "Start Camera Coach",
                        Modifier.size(24.dp),
                        tint = Mint
                    )
                }
            } else {
                Icon(Icons.Default.Videocam, "AI Camera supported", Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun EmptyState(title: String, message: String, icon: ImageVector = Icons.Default.FitnessCenter) {
    PanelCard {
        Icon(icon, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
        SectionTitle(title, message)
    }
}

fun decimal(value: Double): String = formatDecimal(value)
fun weightLabel(value: Double, p: Profile) = decimal(fromKg(value, p.unit)) + " " + p.unit

fun durationText(ms: Long): String {
    val s = (ms.coerceAtLeast(0) / 1000).toInt()
    val m = s / 60
    val sec = s % 60
    val mStr = if (m < 10) "0$m" else "$m"
    val sStr = if (sec < 10) "0$sec" else "$sec"
    return "$mStr:$sStr"
}

fun formatDate(millis: Long): String {
    val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(millis)
    val localDate = instant.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
    return localDate.toString()
}

@Composable
fun PlacementDrawing(exercise: Exercise) {
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.outline
    val cam = exercise.effectiveCamera
    Canvas(Modifier.fillMaxWidth().height(160.dp)) {
        val w = size.width
        val h = size.height
        drawLine(muted, Offset(w * .1f, h * .9f), Offset(w * .9f, h * .9f), 2.dp.toPx())
        val coords = if (cam in listOf("plank", "pushup")) listOf(.25f to .42f, .32f to .5f, .55f to .58f, .8f to .68f, .32f to .82f)
        else if (cam == "rdl") listOf(.38f to .2f, .42f to .33f, .58f to .48f, .55f to .68f, .55f to .88f, .42f to .66f)
        else listOf(.5f to .12f, .5f to .27f, .5f to .5f, .4f to .7f, .36f to .88f, .6f to .7f, .64f to .88f, .3f to .35f, .7f to .35f)
        val points = coords.map { Offset(it.first * w, it.second * h) }
        drawCircle(accent, 10.dp.toPx(), points[0])
        val edges = if (points.size == 5) listOf(0 to 1, 1 to 2, 2 to 3, 1 to 4)
        else if (points.size == 6) listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 1 to 5)
        else listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 2 to 5, 5 to 6, 1 to 7, 1 to 8)
        edges.forEach { (a, b) -> drawLine(accent, points[a], points[b], 6.dp.toPx(), StrokeCap.Round) }
    }
}

@Composable
fun MuscleHeatmapCard(
    statuses: List<MuscleStatus>,
    daysSinceLastWorkout: Int = 0,
    defaultExpanded: Boolean = false,
    onMuscleSelected: ((String) -> Unit)? = null
) {
    MuscleRecoveryView(
        statuses = statuses,
        daysSinceLastWorkout = daysSinceLastWorkout,
        defaultExpanded = defaultExpanded,
        onMuscleSelected = onMuscleSelected
    )
}
