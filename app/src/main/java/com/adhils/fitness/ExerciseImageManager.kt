package com.adhils.fitness

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adhils.fitness.core.Catalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Manages downloading, local disk caching, and memory caching of real exercise demonstration images
 * sourced from the open-source public domain free-exercise-db repository.
 */
object ExerciseImageManager {
    private const val BASE_URL = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val memoryCache = ConcurrentHashMap<String, Bitmap>()

    // Exercise ID to free-exercise-db path mapping
    private val EXERCISE_IMAGE_PATHS = mapOf(
        "goblet-squat" to "Goblet_Squat/0.jpg",
        "bodyweight-squat" to "Bodyweight_Squat/0.jpg",
        "reverse-lunge" to "Dumbbell_Rear_Lunge/0.jpg",
        "dumbbell-split-squat" to "Split_Squat_with_Dumbbells/0.jpg",
        "bulgarian-split-squat" to "Split_Squat_with_Dumbbells/0.jpg",
        "sumo-squat" to "Plie_Dumbbell_Squat/0.jpg",
        "step-up" to "Barbell_Step_Ups/0.jpg",
        "band-squat" to "Box_Squat_with_Bands/0.jpg",
        "wall-sit" to "Bodyweight_Squat/0.jpg",
        "rdl" to "Romanian_Deadlift/0.jpg",
        "bridge" to "Barbell_Glute_Bridge/0.jpg",
        "single-leg-rdl" to "Stiff-Legged_Dumbbell_Deadlift/0.jpg",
        "hip-thrust" to "Barbell_Hip_Thrust/0.jpg",
        "single-leg-bridge" to "Single_Leg_Glute_Bridge/0.jpg",
        "dumbbell-swing" to "One-Arm_Kettlebell_Swings/0.jpg",
        "band-rdl" to "Stiff-Legged_Dumbbell_Deadlift/0.jpg",
        "good-morning" to "Good_Morning/0.jpg",
        "press" to "Dumbbell_Shoulder_Press/0.jpg",
        "pushup" to "Pushups/0.jpg",
        "floor-press" to "Floor_Press/0.jpg",
        "wall-pushup" to "Pushups/0.jpg",
        "bench-press" to "Dumbbell_Bench_Press/0.jpg",
        "incline-press" to "Incline_Dumbbell_Press/0.jpg",
        "incline-pushup" to "Incline_Push-Up/0.jpg",
        "knee-pushup" to "Pushups/0.jpg",
        "pike-pushup" to "Pushups/0.jpg",
        "arnold-press" to "Arnold_Dumbbell_Press/0.jpg",
        "band-press" to "Bench_Press_-_With_Bands/0.jpg",
        "band-chest-press" to "Bench_Press_-_With_Bands/0.jpg",
        "row" to "One-Arm_Dumbbell_Row/0.jpg",
        "curl" to "Dumbbell_Alternate_Bicep_Curl/0.jpg",
        "band-row" to "Upright_Row_-_With_Bands/0.jpg",
        "band-pull-apart" to "Band_Pull_Apart/0.jpg",
        "bent-over-row" to "Bent_Over_Barbell_Row/0.jpg",
        "chest-supported-row" to "Dumbbell_Incline_Row/0.jpg",
        "hammer-curl" to "Alternate_Hammer_Curl/0.jpg",
        "incline-curl" to "Alternate_Incline_Dumbbell_Curl/0.jpg",
        "reverse-fly" to "Barbell_Rear_Delt_Row/0.jpg",
        "band-face-pull" to "Face_Pull/0.jpg",
        "band-curl" to "Close-Grip_EZ-Bar_Curl_with_Band/0.jpg",
        "superman-pull" to "Superman/0.jpg",
        "plank" to "Plank/0.jpg",
        "bird-dog" to "Superman/0.jpg",
        "dead-bug" to "Dead_Bug/0.jpg",
        "side-plank" to "Push_Up_to_Side_Plank/0.jpg",
        "hollow-hold" to "Plank/0.jpg",
        "mountain-climber" to "Mountain_Climbers/0.jpg",
        "russian-twist" to "Russian_Twist/0.jpg",
        "suitcase-hold" to "Farmers_Walk/0.jpg",
        "pallof-press" to "Pallof_Press/0.jpg",
        "calf-raise" to "Standing_Dumbbell_Calf_Raise/0.jpg",
        "lateral-raise" to "Seated_Side_Lateral_Raise/0.jpg",
        "weighted-calf-raise" to "Barbell_Seated_Calf_Raise/0.jpg",
        "front-raise" to "Front_Dumbbell_Raise/0.jpg",
        "tricep-extension" to "Cable_Incline_Triceps_Extension/0.jpg",
        "tricep-kickback" to "Cable_One_Arm_Tricep_Extension/0.jpg",
        "bench-dip" to "Bench_Dips/0.jpg",
        "shrug" to "Barbell_Shrug/0.jpg",
        "band-pushdown" to "Triceps_Pushdown/0.jpg",
        "band-lateral-raise" to "Lateral_Raise_-_With_Bands/0.jpg",
        "march" to "Fast_Skipping/0.jpg",
        "arm-circles" to "Arm_Circles/0.jpg",
        "hip-openers" to "Standing_Hip_Circles/0.jpg",
        "inchworm" to "Inchworm/0.jpg",
        "cat-cow" to "Cat_Stretch/0.jpg",
        "thoracic-rotation" to "Worlds_Greatest_Stretch/0.jpg",
        "worlds-greatest-stretch" to "Worlds_Greatest_Stretch/0.jpg",
        "childs-pose" to "Childs_Pose/0.jpg"
    )

    // Fallback pattern images
    private val PATTERN_FALLBACKS = mapOf(
        "Squat" to "Bodyweight_Squat/0.jpg",
        "Hinge" to "Romanian_Deadlift/0.jpg",
        "Push" to "Pushups/0.jpg",
        "Pull" to "One-Arm_Dumbbell_Row/0.jpg",
        "Core" to "Plank/0.jpg",
        "Accessory" to "Front_Dumbbell_Raise/0.jpg",
        "Warm-up" to "Step-up_with_Knee_Raise/0.jpg",
        "Mobility" to "Worlds_Greatest_Stretch/0.jpg"
    )

    fun getImageUrl(exerciseId: String): String {
        val relPath = EXERCISE_IMAGE_PATHS[exerciseId]
            ?: runCatching {
                val e = Catalog.get(exerciseId)
                PATTERN_FALLBACKS[e.pattern]
            }.getOrNull()
            ?: "Pushups/0.jpg"
        return BASE_URL + relPath
    }

    suspend fun loadExerciseBitmap(context: Context, exerciseId: String): Bitmap? = withContext(Dispatchers.IO) {
        val cachedMem = memoryCache[exerciseId]
        if (cachedMem != null) return@withContext cachedMem

        val cacheDir = File(context.cacheDir, "exercise_thumbs").apply { mkdirs() }
        val diskFile = File(cacheDir, "$exerciseId.jpg")

        if (diskFile.exists() && diskFile.length() > 0) {
            val bitmap = BitmapFactory.decodeFile(diskFile.absolutePath)
            if (bitmap != null) {
                memoryCache[exerciseId] = bitmap
                return@withContext bitmap
            }
        }

        // Fetch over network
        val url = getImageUrl(exerciseId)
        try {
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bytes = response.body?.bytes()
                if (bytes != null && bytes.isNotEmpty()) {
                    FileOutputStream(diskFile).use { it.write(bytes) }
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        memoryCache[exerciseId] = bitmap
                        return@withContext bitmap
                    }
                }
            }
        } catch (_: Exception) {
            // Silently fall back to placeholder
        }
        null
    }
}

/**
 * Beautiful, responsive exercise demonstration thumbnail.
 * Replaces plain right-arrow (>) or play icons with real exercise imagery.
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
    val context = LocalContext.current
    var bitmap by remember(exerciseId) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(exerciseId) {
        bitmap = ExerciseImageManager.loadExerciseBitmap(context, exerciseId)
    }

    val clickableMod = if (onClick != null) {
        modifier
            .clip(shape)
            .clickable { onClick() }
    } else {
        modifier.clip(shape)
    }

    Box(
        modifier = clickableMod
            .background(MaterialTheme.colorScheme.surfaceVariant, shape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), shape),
        contentAlignment = Alignment.Center
    ) {
        val currentBitmap = bitmap
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = contentDescription ?: exerciseId,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Subtle dark scrim so any overlay icons remain legible
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f))
                        )
                    )
            )
        } else {
            // Placeholder fallback with subtle athletic gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Overlay video/instruction indicator if requested
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
