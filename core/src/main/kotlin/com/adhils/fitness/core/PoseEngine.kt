package com.adhils.fitness.core
import kotlin.math.*

data class Joint(val x: Float, val y: Float, val visibility: Float = 1f)
data class PoseFrame(val timeMs: Long, val joints: List<Joint>, val aspectRatio: Float = 1f)
data class PoseObservation(
    val reps: Int = 0, val holdSeconds: Int = 0, val tracking: Boolean = false,
    val status: String = "Position your whole body in view", val cue: String? = null,
    val joints: List<Joint> = emptyList(), val phase: String = "Ready",
    val formScore: Int = 100, val nextRepFeedback: String? = null,
    val primaryAngle: Int = 180, val hipAlignmentAngle: Int = 180,
    val symmetryDelta: Int = 0, val lastRepDurationMs: Long = 0
)

/** Experimental 2D estimates. No spine, injury, or load-safety inference. */
class PoseEngine(private val exercise: String, private val movementCues: Boolean = false) {
    private var filtered: List<Joint> = emptyList()
    private var previousTime = -1L
    private var lastGood = -1L
    private var stableSince = -1L
    private var candidate = ""
    private var candidateSince = 0L
    private var armed = false
    private var turned = false
    private var cycleStart = 0L
    private var count = 0
    private var holdMs = 0L
    private var lastCue = -10000L
    private var pendingCue: String? = null
    private var pendingSince = 0L
    private var tempoCueUntil = 0L
    private var minAngleInRep = 180
    private var minHipAngleInRep = 180
    private var maxSymmetryInRep = 0
    private var lastRepMs = 0L
    private var lastRepFeedback: String? = null
    private var lastFormScore = 100
    private var latest = PoseObservation()
    fun pause(): PoseObservation {
        armed = false; turned = false; stableSince = -1; previousTime = -1; filtered = emptyList(); candidate = ""; pendingCue = null
        tempoCueUntil = 0
        latest = latest.copy(tracking = false, status = "Tracking paused", cue = null, joints = emptyList())
        return latest
    }

    fun update(frame: PoseFrame): PoseObservation {
        val t = frame.timeMs
        if (t <= previousTime) return latest
        val dt = if (previousTime < 0) 0 else t - previousTime
        previousTime = t
        if (dt > 1500) {
            armed = false; turned = false; stableSince = -1; filtered = emptyList()
        }

        fun missing(reason: String): PoseObservation {
            armed = false; turned = false; candidate = ""; pendingCue = null; tempoCueUntil = 0
            stableSince = -1; filtered = emptyList()
            latest = PoseObservation(
                reps = count,
                holdSeconds = (holdMs / 1000).toInt(),
                tracking = false,
                status = reason,
                phase = "Paused",
                formScore = lastFormScore,
                nextRepFeedback = lastRepFeedback
            )
            return latest
        }

        val points = frame.joints
        if (points.size != 33 || !frame.aspectRatio.isFinite() || frame.aspectRatio <= 0) return missing("Move into view")
        if (points.any { !it.x.isFinite() || !it.y.isFinite() }) return missing("Tracking unavailable")

        val leftArm = listOf(11, 13, 15); val rightArm = listOf(12, 14, 16)
        val leftLeg = listOf(23, 25, 27); val rightLeg = listOf(24, 26, 28)
        val leftSide = leftArm + leftLeg; val rightSide = rightArm + rightLeg
        val side = if (leftSide.sumOf { points[it].visibility.toDouble() } >= rightSide.sumOf { points[it].visibility.toDouble() }) leftSide else rightSide

        // Validate joint visibility based on exercise biomechanics
        when (exercise) {
            "timed" -> {
                val inFrame = points[0].visibility > 0.25f ||
                        points[11].visibility > 0.25f || points[12].visibility > 0.25f ||
                        points[23].visibility > 0.25f || points[24].visibility > 0.25f ||
                        points[25].visibility > 0.25f || points[26].visibility > 0.25f
                if (!inFrame) return missing("Step into camera view")
            }
            "press" -> {
                val upperVisible = points[11].visibility > 0.35f && points[12].visibility > 0.35f &&
                        (points[13].visibility > 0.35f || points[14].visibility > 0.35f) &&
                        (points[15].visibility > 0.35f || points[16].visibility > 0.35f)
                if (!upperVisible) return missing("Keep shoulders and hands visible")
            }
            "curl" -> {
                val arm = if (leftArm.sumOf { points[it].visibility.toDouble() } >= rightArm.sumOf { points[it].visibility.toDouble() }) leftArm else rightArm
                if (points[arm[0]].visibility < 0.35f || points[arm[1]].visibility < 0.35f || points[arm[2]].visibility < 0.35f) {
                    return missing("Keep arms and hands visible")
                }
            }
            "squat", "rdl" -> {
                val leg = if (leftLeg.sumOf { points[it].visibility.toDouble() } >= rightLeg.sumOf { points[it].visibility.toDouble() }) leftLeg else rightLeg
                val hasShoulder = points[11].visibility > 0.30f || points[12].visibility > 0.30f
                if (!hasShoulder || points[leg[0]].visibility < 0.30f || points[leg[1]].visibility < 0.30f || points[leg[2]].visibility < 0.30f) {
                    return missing("Keep hips, knees and feet visible")
                }
            }
            "pushup" -> {
                val pushupReady = (points[11].visibility > 0.35f || points[12].visibility > 0.35f) &&
                        (points[13].visibility > 0.35f || points[14].visibility > 0.35f) &&
                        (points[23].visibility > 0.35f || points[24].visibility > 0.35f)
                if (!pushupReady) return missing("Keep shoulders, arms and hips visible")
            }
            "plank" -> {
                val plankReady = (points[11].visibility > 0.35f || points[12].visibility > 0.35f) &&
                        (points[23].visibility > 0.35f || points[24].visibility > 0.35f)
                if (!plankReady) return missing("Keep shoulders and hips visible")
            }
            else -> {
                if (side.take(4).any { points[it].visibility < 0.35f }) {
                    return missing("Keep body visible in frame")
                }
            }
        }

        val checkPoints = when (exercise) {
            "timed" -> emptyList()
            "press", "curl" -> leftArm + rightArm
            else -> side
        }
        if (checkPoints.any { points[it].visibility > 0.35f && (points[it].x !in 0.005f..0.995f || points[it].y !in 0.005f..0.995f) }) {
            return missing("Move back to fit within camera view")
        }

        filtered = if (filtered.size == 33) points.mapIndexed { i, pt ->
            Joint(filtered[i].x * 0.35f + pt.x * 0.65f, filtered[i].y * 0.35f + pt.y * 0.65f, pt.visibility)
        } else points
        val p = filtered

        fun length(a: Int, b: Int) = hypot((p[a].x - p[b].x) * frame.aspectRatio, p[a].y - p[b].y)
        val shoulder = side[0]; val elbow = side[1]; val wrist = side[2]
        val hip = side[3]; val knee = side[4]; val ankle = side[5]
        val torso = length(shoulder, hip)
        if (exercise != "timed" && torso < 0.040f) return missing("Move closer to the camera")

        val width = length(11, 12) / torso.coerceAtLeast(0.01f)
        var stanceTip: String? = null
        if (exercise == "pushup" || exercise == "plank") {
            val horizontal = abs(p[shoulder].x - p[hip].x) * frame.aspectRatio > abs(p[shoulder].y - p[hip].y) * 0.7f
            if (!horizontal) return missing("Use floor position shown in guide")
        } else if (exercise in setOf("squat", "rdl") && width > 0.88f && points[11].visibility > 0.75f && points[12].visibility > 0.75f) {
            stanceTip = "Tip: Stand slightly sideways for clearest angle tracking"
        }

        val settleLimit = if (exercise == "timed") 250L else 600L
        if (stableSince < 0) stableSince = t
        if (t - stableSince < settleLimit) {
            latest = PoseObservation(count, (holdMs / 1000).toInt(), false, "Locking on to body posture...", joints = p,
                formScore = lastFormScore, nextRepFeedback = lastRepFeedback)
            return latest
        }

        fun jointAngle(a: Int, b: Int, c: Int) = angle(p[a], p[b], p[c], frame.aspectRatio)
        val hipAlign = jointAngle(shoulder, hip, ankle).roundToInt()
        val symDelta = if (exercise in setOf("press", "pushup", "pull", "curl")) {
            abs(jointAngle(12, 14, 16) - jointAngle(11, 13, 15)).roundToInt()
        } else {
            abs(jointAngle(24, 26, 28) - jointAngle(23, 25, 27)).roundToInt()
        }

        var cue: String? = stanceTip
        val phase: String
        val currentPrimaryAngle: Int

        if (exercise == "plank" || exercise == "timed") {
            currentPrimaryAngle = hipAlign
            if (exercise == "plank") {
                if (dt in 1..250 && lastGood == t - dt) holdMs += dt
                if (hipAlign < 155) {
                    cue = "Check your hip position"
                    lastFormScore = 75
                    lastRepFeedback = "Hips at ${hipAlign}° — brace abs and glutes to straighten your body line."
                } else {
                    lastFormScore = 96
                    lastRepFeedback = "Strong plank alignment (${hipAlign}°) — breathe steadily."
                }
                phase = "Holding"
            } else {
                if (dt in 1..1500) holdMs += dt
                lastFormScore = 98
                lastRepFeedback = "Great rhythm — keep moving!"
                phase = "Tracking"
            }
        } else {
            val a = when (exercise) {
                "squat" -> jointAngle(hip, knee, ankle)
                "rdl" -> jointAngle(shoulder, hip, knee)
                "press" -> min(jointAngle(11, 13, 15), jointAngle(12, 14, 16))
                "curl" -> {
                    val activeArm = if (leftArm.sumOf { p[it].visibility.toDouble() } >= rightArm.sumOf { p[it].visibility.toDouble() }) leftArm else rightArm
                    jointAngle(activeArm[0], activeArm[1], activeArm[2])
                }
                else -> jointAngle(shoulder, elbow, wrist)
            }
            currentPrimaryAngle = a.roundToInt()
            if (armed) {
                minAngleInRep = min(minAngleInRep, currentPrimaryAngle)
                minHipAngleInRep = min(minHipAngleInRep, hipAlign)
                maxSymmetryInRep = max(maxSymmetryInRep, symDelta)
            }

            val top = when (exercise) {
                "press" -> a > 148 && (p[15].y < p[11].y || p[16].y < p[12].y)
                "curl" -> a < 95
                "squat" -> a > 146 && (width > 0.80f || hipAlign > 128)
                "rdl" -> a > 150
                "pushup" -> a > 148
                else -> a > 150
            }
            val bottom = when (exercise) {
                "press" -> a < 110
                "curl" -> a > 145
                "squat", "rdl" -> a < 122
                "pushup" -> a < 105
                else -> a < 115
            }

            val current = if (top) "Top" else if (bottom) "Bottom" else "Moving"
            if (current != candidate) { candidate = current; candidateSince = t }
            val sustained = t - candidateSince >= 120
            val start = if (exercise in setOf("press", "curl")) "Bottom" else "Top"
            val turn = if (exercise in setOf("press", "curl")) "Top" else "Bottom"

            if (sustained && current == start) {
                if (!armed) {
                    armed = true; cycleStart = t
                    minAngleInRep = currentPrimaryAngle; minHipAngleInRep = hipAlign; maxSymmetryInRep = symDelta
                } else if (turned) {
                    val repDur = t - cycleStart
                    if (repDur in 600..30000) {
                        count++
                        lastRepMs = repDur
                        var penalty = 0
                        if (repDur < 1200) penalty += 10
                        if (maxSymmetryInRep > 24) penalty += 10
                        if (exercise == "pushup" && minHipAngleInRep < 155) penalty += 15
                        if (exercise in setOf("squat", "rdl") && minAngleInRep > 115) penalty += 10
                        lastFormScore = (100 - penalty).coerceIn(60, 100)
                        lastRepFeedback = when {
                            exercise == "pushup" && minHipAngleInRep < 155 -> "Rep $count: Hips sagged (${minHipAngleInRep}°). Brace core to keep body straight on Rep ${count + 1}."
                            exercise == "press" && maxSymmetryInRep > 24 -> "Rep $count: Arms uneven by ${maxSymmetryInRep}°. Press both sides evenly on Rep ${count + 1}."
                            repDur < 1200 && exercise !in setOf("press", "curl") -> "Rep $count: Lowered fast (${formatDecimal(repDur / 1000.0)}s). Control the descent on Rep ${count + 1}."
                            exercise == "squat" && minAngleInRep > 112 -> "Rep $count: Bottom angle ${minAngleInRep}°. Sit slightly deeper on Rep ${count + 1}."
                            exercise == "rdl" && minAngleInRep > 112 -> "Rep $count: Hinge hips further back while keeping spine neutral on Rep ${count + 1}."
                            exercise == "curl" && minAngleInRep > 95 -> "Rep $count: Squeeze bicep fully at top on Rep ${count + 1}."
                            else -> "Rep $count: Great posture (${lastFormScore}% form, ${minAngleInRep}° depth)! Keep that control on Rep ${count + 1}."
                        }
                    }
                    turned = false; cycleStart = t
                    minAngleInRep = currentPrimaryAngle; minHipAngleInRep = hipAlign; maxSymmetryInRep = symDelta
                }
            }
            if (sustained && armed && current == turn && !turned) {
                turned = true
                if (t - cycleStart < 900 && exercise !in setOf("press", "curl")) tempoCueUntil = t + 1500
            }
            if (armed && t - cycleStart > 30000) { armed = false; turned = false }
            phase = current
            if (exercise == "pushup" && hipAlign < 150) cue = "Check your hip position"
            if (exercise == "press" && symDelta > 28) cue = "Check whether both arms move together"
        }

        lastGood = t
        if (cue == null && t < tempoCueUntil) cue = "Slow the lowering phase"
        if (cue != pendingCue) { pendingCue = cue; pendingSince = t }
        val spoken = if (movementCues && cue != null && t - pendingSince >= 500 && t - lastCue > 4000) { lastCue = t; cue } else null

        latest = PoseObservation(
            reps = count, holdSeconds = (holdMs / 1000).toInt(), tracking = true,
            status = "Tracking · $phase", cue = spoken, joints = p, phase = phase,
            formScore = lastFormScore, nextRepFeedback = lastRepFeedback,
            primaryAngle = currentPrimaryAngle, hipAlignmentAngle = hipAlign,
            symmetryDelta = symDelta, lastRepDurationMs = lastRepMs
        )
        return latest
    }
    companion object {
        fun angle(a:Joint,b:Joint,c:Joint,aspect:Float=1f):Double {
            val ux=(a.x-b.x).toDouble()*aspect; val uy=(a.y-b.y).toDouble()
            val vx=(c.x-b.x).toDouble()*aspect; val vy=(c.y-b.y).toDouble()
            val denominator=hypot(ux,uy)*hypot(vx,vy)
            if(denominator<1e-8) return 180.0
            return acos(((ux*vx+uy*vy)/denominator).coerceIn(-1.0,1.0)) * 180.0 / PI
        }
    }
}
