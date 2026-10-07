package com.adhils.fitness

import androidx.compose.ui.graphics.ImageBitmap
import com.adhils.fitness.core.Catalog
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object ExerciseImageLoader {
    private const val BASE_URL = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/"
    private val memoryCache = mutableMapOf<String, ImageBitmap>()
    private val mutex = Mutex()

    private val EXERCISE_IMAGE_PATHS = mapOf(
        "goblet-squat" to "Goblet_Squat/0.jpg",
        "bodyweight-squat" to "Bodyweight_Squat/0.jpg",
        "reverse-lunge" to "Crossover_Reverse_Lunge/0.jpg",
        "dumbbell-split-squat" to "Barbell_Side_Split_Squat/0.jpg",
        "bulgarian-split-squat" to "Barbell_Side_Split_Squat/0.jpg",
        "sumo-squat" to "Plie_Dumbbell_Squat/0.jpg",
        "step-up" to "Barbell_Step_Ups/0.jpg",
        "band-squat" to "Box_Squat_with_Bands/0.jpg",
        "wall-sit" to "Bodyweight_Squat/0.jpg",
        "rdl" to "Romanian_Deadlift/0.jpg",
        "bridge" to "Barbell_Glute_Bridge/0.jpg",
        "single-leg-rdl" to "Romanian_Deadlift/0.jpg",
        "hip-thrust" to "Barbell_Glute_Bridge/0.jpg",
        "single-leg-bridge" to "Single_Leg_Glute_Bridge/0.jpg",
        "dumbbell-swing" to "Kettlebell_Sumo_High_Pull/0.jpg",
        "band-rdl" to "Romanian_Deadlift/0.jpg",
        "good-morning" to "Good_Morning/0.jpg",
        "press" to "Dumbbell_Shoulder_Press/0.jpg",
        "pushup" to "Pushups/0.jpg",
        "floor-press" to "Floor_Press/0.jpg",
        "wall-pushup" to "Pushups/0.jpg",
        "bench-press" to "Decline_Dumbbell_Bench_Press/0.jpg",
        "incline-press" to "Incline_Dumbbell_Press/0.jpg",
        "incline-pushup" to "Pushups/0.jpg",
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
        "chest-supported-row" to "One-Arm_Dumbbell_Row/0.jpg",
        "hammer-curl" to "Alternate_Hammer_Curl/0.jpg",
        "incline-curl" to "Alternate_Incline_Dumbbell_Curl/0.jpg",
        "reverse-fly" to "Barbell_Rear_Delt_Row/0.jpg",
        "band-face-pull" to "Band_Pull_Apart/0.jpg",
        "band-curl" to "Close-Grip_EZ-Bar_Curl_with_Band/0.jpg",
        "superman-pull" to "One-Arm_Dumbbell_Row/0.jpg",
        "plank" to "Plank/0.jpg",
        "bird-dog" to "Plank/0.jpg",
        "dead-bug" to "Dead_Bug/0.jpg",
        "side-plank" to "Push_Up_to_Side_Plank/0.jpg",
        "hollow-hold" to "Plank/0.jpg",
        "mountain-climber" to "Mountain_Climbers/0.jpg",
        "russian-twist" to "Russian_Twist/0.jpg",
        "suitcase-hold" to "Farmers_Walk/0.jpg",
        "pallof-press" to "Pallof_Press/0.jpg",
        "calf-raise" to "Rocking_Standing_Calf_Raise/0.jpg",
        "lateral-raise" to "Seated_Side_Lateral_Raise/0.jpg",
        "weighted-calf-raise" to "Barbell_Seated_Calf_Raise/0.jpg",
        "front-raise" to "Front_Dumbbell_Raise/0.jpg",
        "tricep-extension" to "Cable_Incline_Triceps_Extension/0.jpg",
        "tricep-kickback" to "Cable_One_Arm_Tricep_Extension/0.jpg",
        "bench-dip" to "Bench_Dips/0.jpg",
        "shrug" to "Barbell_Shrug/0.jpg",
        "band-pushdown" to "Cable_Incline_Triceps_Extension/0.jpg",
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

    fun getFolder(exerciseId: String): String {
        val relPath = EXERCISE_IMAGE_PATHS[exerciseId]
            ?: runCatching {
                val e = Catalog.get(exerciseId)
                PATTERN_FALLBACKS[e.pattern]
            }.getOrNull()
            ?: "Pushups/0.jpg"
        return relPath.substringBeforeLast("/")
    }

    fun getImageUrl(exerciseId: String): String = getFrameUrl(exerciseId, 0)

    fun getFrameUrl(exerciseId: String, frame: Int): String {
        val folder = getFolder(exerciseId)
        val validFrame = if (frame in 0..1) frame else 0
        return "$BASE_URL$folder/$validFrame.jpg"
    }

    fun getCached(exerciseId: String): ImageBitmap? = getCachedFrame(exerciseId, 0)

    fun getCachedFrame(exerciseId: String, frame: Int): ImageBitmap? {
        val folder = getFolder(exerciseId)
        val key = "${folder.replace("/", "_")}_$frame"
        return memoryCache[key] ?: if (frame == 0) memoryCache[folder.replace("/", "_")] else null
    }

    suspend fun loadExerciseImage(exerciseId: String): ImageBitmap? = loadExerciseFrame(exerciseId, 0)

    suspend fun loadExerciseFrame(exerciseId: String, frame: Int): ImageBitmap? {
        val folder = getFolder(exerciseId)
        val key = "${folder.replace("/", "_")}_$frame"
        memoryCache[key]?.let { return it }
        val url = getFrameUrl(exerciseId, frame)
        val loaded = PlatformImageLoader.loadExerciseImage(key, url)
        if (loaded != null) {
            mutex.withLock {
                memoryCache[key] = loaded
                if (frame == 0) memoryCache[folder.replace("/", "_")] = loaded
            }
        }
        return loaded
    }
}

