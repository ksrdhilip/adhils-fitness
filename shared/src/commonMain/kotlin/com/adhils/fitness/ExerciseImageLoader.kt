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

    private const val CDN_ANIM_BASE = "https://cdn.jsdelivr.net/gh/omercotkd/exercises-gifs@main/assets/"
    private const val CDN_GYM_BASE = "https://cdn.jsdelivr.net/gh/JahelCuadrado/ExerciseGymGifsDB@v1.1.0/"

    private val EXERCISE_ANIMATION_URLS = mapOf(
        "goblet-squat" to "${CDN_ANIM_BASE}1760.gif",
        "bodyweight-squat" to "${CDN_ANIM_BASE}3543.gif",
        "reverse-lunge" to "${CDN_ANIM_BASE}0078.gif",
        "dumbbell-split-squat" to "${CDN_ANIM_BASE}0099.gif",
        "bulgarian-split-squat" to "${CDN_ANIM_BASE}0099.gif",
        "sumo-squat" to "${CDN_ANIM_BASE}3142.gif",
        "step-up" to "${CDN_ANIM_BASE}0431.gif",
        "band-squat" to "${CDN_ANIM_BASE}1004.gif",
        "wall-sit" to "${CDN_GYM_BASE}glutes/march-sit-wall.gif",
        "rdl" to "${CDN_ANIM_BASE}0085.gif",
        "bridge" to "${CDN_ANIM_BASE}1409.gif",
        "single-leg-rdl" to "${CDN_ANIM_BASE}1756.gif",
        "hip-thrust" to "${CDN_ANIM_BASE}3236.gif",
        "single-leg-bridge" to "${CDN_ANIM_BASE}3645.gif",
        "dumbbell-swing" to "${CDN_ANIM_BASE}0549.gif",
        "band-rdl" to "${CDN_ANIM_BASE}1009.gif",
        "good-morning" to "${CDN_ANIM_BASE}0044.gif",
        "press" to "${CDN_ANIM_BASE}0426.gif",
        "pushup" to "${CDN_ANIM_BASE}0662.gif",
        "floor-press" to "${CDN_ANIM_BASE}0065.gif",
        "wall-pushup" to "${CDN_ANIM_BASE}0492.gif",
        "bench-press" to "${CDN_ANIM_BASE}0289.gif",
        "incline-press" to "${CDN_ANIM_BASE}0047.gif",
        "incline-pushup" to "${CDN_ANIM_BASE}0492.gif",
        "knee-pushup" to "${CDN_ANIM_BASE}3211.gif",
        "pike-pushup" to "${CDN_ANIM_BASE}1296.gif",
        "arnold-press" to "${CDN_ANIM_BASE}2137.gif",
        "band-press" to "${CDN_ANIM_BASE}0997.gif",
        "band-chest-press" to "${CDN_GYM_BASE}pectorals/resistance-band-seated-chest-press.gif",
        "row" to "${CDN_ANIM_BASE}0292.gif",
        "curl" to "${CDN_ANIM_BASE}0285.gif",
        "band-row" to "${CDN_GYM_BASE}upper-back/resistance-band-seated-straight-back-row.gif",
        "band-pull-apart" to "${CDN_GYM_BASE}delts/band-reverse-fly.gif",
        "bent-over-row" to "${CDN_ANIM_BASE}0027.gif",
        "chest-supported-row" to "${CDN_ANIM_BASE}0049.gif",
        "hammer-curl" to "${CDN_ANIM_BASE}1648.gif",
        "incline-curl" to "${CDN_ANIM_BASE}0072.gif",
        "reverse-fly" to "${CDN_ANIM_BASE}0075.gif",
        "band-face-pull" to "${CDN_GYM_BASE}delts/band-reverse-fly.gif",
        "band-curl" to "${CDN_ANIM_BASE}0968.gif",
        "superman-pull" to "${CDN_ANIM_BASE}0803.gif",
        "plank" to "${CDN_ANIM_BASE}0464.gif",
        "bird-dog" to "${CDN_GYM_BASE}abs/dead-bug.gif",
        "dead-bug" to "${CDN_ANIM_BASE}0276.gif",
        "side-plank" to "${CDN_ANIM_BASE}3544.gif",
        "hollow-hold" to "${CDN_ANIM_BASE}0464.gif",
        "mountain-climber" to "${CDN_ANIM_BASE}0630.gif",
        "russian-twist" to "${CDN_ANIM_BASE}0687.gif",
        "suitcase-hold" to "${CDN_ANIM_BASE}2133.gif",
        "pallof-press" to "${CDN_ANIM_BASE}0979.gif",
        "calf-raise" to "${CDN_ANIM_BASE}1372.gif",
        "lateral-raise" to "${CDN_ANIM_BASE}0334.gif",
        "weighted-calf-raise" to "${CDN_ANIM_BASE}0088.gif",
        "front-raise" to "${CDN_ANIM_BASE}0310.gif",
        "tricep-extension" to "${CDN_ANIM_BASE}1722.gif",
        "tricep-kickback" to "${CDN_ANIM_BASE}0333.gif",
        "bench-dip" to "${CDN_ANIM_BASE}0129.gif",
        "shrug" to "${CDN_ANIM_BASE}0406.gif",
        "band-pushdown" to "${CDN_ANIM_BASE}0201.gif",
        "band-lateral-raise" to "${CDN_ANIM_BASE}0977.gif",
        "arm-circles" to "${CDN_GYM_BASE}delts/band-front-lateral-raise.gif",
        "hip-openers" to "${CDN_ANIM_BASE}0980.gif",
        "inchworm" to "${CDN_ANIM_BASE}1471.gif",
        "cat-cow" to "${CDN_ANIM_BASE}1363.gif",
        "thoracic-rotation" to "${CDN_ANIM_BASE}0984.gif",
        "worlds-greatest-stretch" to "${CDN_ANIM_BASE}1410.gif",
        "childs-pose" to "${CDN_ANIM_BASE}1494.gif",
        "march" to "${CDN_ANIM_BASE}0598.gif"
    )

    private const val CDN_VIDEO_BASE = "https://cdn.jsdelivr.net/gh/ksrdhilip/adhils-fitness@main/videos/"
    private const val RAW_VIDEO_BASE = "https://raw.githubusercontent.com/ksrdhilip/adhils-fitness/main/videos/"

    fun getVideoUrl(exerciseId: String): String {
        return "$CDN_VIDEO_BASE$exerciseId.mp4"
    }

    fun hasVideo(exerciseId: String): Boolean = EXERCISE_ANIMATION_URLS.containsKey(exerciseId)

    private val animationCache = mutableMapOf<String, List<ExerciseAnimationFrame>>()

    fun getAnimationUrl(exerciseId: String): String? = EXERCISE_ANIMATION_URLS[exerciseId]

    fun getCachedAnimation(exerciseId: String): List<ExerciseAnimationFrame>? = animationCache[exerciseId]

    suspend fun loadExerciseAnimation(exerciseId: String): List<ExerciseAnimationFrame>? {
        animationCache[exerciseId]?.let { return it }
        val url = getAnimationUrl(exerciseId) ?: return null
        val frames = PlatformImageLoader.loadExerciseAnimation(exerciseId, url)
        if (frames != null && frames.isNotEmpty()) {
            mutex.withLock {
                animationCache[exerciseId] = frames
            }
        }
        return frames
    }

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

