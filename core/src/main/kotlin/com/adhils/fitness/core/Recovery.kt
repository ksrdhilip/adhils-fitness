package com.adhils.fitness.core

import kotlin.math.roundToInt

data class MuscleStatus(
    val muscle: String,
    val recoveryPercent: Int,
    val workingSetsLast7Days: Int,
    val lastTrainedHoursAgo: Int?
)

object Recovery {
    val ALL_MUSCLES = listOf(
        "Quads", "Glutes", "Hamstrings", "Chest", "Back",
        "Shoulders", "Biceps", "Triceps", "Core", "Calves"
    )

    fun extractMuscles(e: Exercise): List<String> {
        val raw = e.muscles.lowercase()
        val matched = mutableListOf<String>()
        if ("quad" in raw || "thigh" in raw || e.pattern == "Squat") matched += "Quads"
        if ("glute" in raw || "hip" in raw) matched += "Glutes"
        if ("hamstring" in raw || "posterior" in raw || e.pattern == "Hinge") matched += "Hamstrings"
        if ("chest" in raw || "pec" in raw) matched += "Chest"
        if ("back" in raw || "lat" in raw || "rear delt" in raw || "trap" in raw) matched += "Back"
        if ("shoulder" in raw || "delt" in raw) matched += "Shoulders"
        if ("bicep" in raw || "forearm" in raw) matched += "Biceps"
        if ("tricep" in raw) matched += "Triceps"
        if ("core" in raw || "ab" in raw || "oblique" in raw || e.pattern == "Core") matched += "Core"
        if ("calf" in raw || "calves" in raw || "ankle" in raw) matched += "Calves"
        return matched.distinct()
    }

    fun calculate(state: AppState, nowMs: Long = System.currentTimeMillis()): List<MuscleStatus> {
        val completed = state.sessions.filter { it.finishedAt != null }
        val sevenDaysAgo = nowMs - 7L * 24 * 3600 * 1000
        val recoveryWindowMs = 72L * 3600 * 1000 // 72 hours for full recovery

        return ALL_MUSCLES.map { muscle ->
            var fatigue = 0.0
            var sets7d = 0
            var lastTrainedMs: Long? = null

            for (session in completed) {
                val finished = session.finishedAt ?: continue
                for (result in session.results) {
                    if (result.warmup) continue
                    val ex = Catalog.byId[result.exerciseId] ?: continue
                    val targetMuscles = extractMuscles(ex)
                    if (muscle !in targetMuscles) continue

                    val isPrimary = targetMuscles.firstOrNull() == muscle
                    val setTime = if (result.savedAt > 0) result.savedAt else finished
                    if (setTime >= sevenDaysAgo) sets7d++
                    if (lastTrainedMs == null || setTime > lastTrainedMs) lastTrainedMs = setTime

                    val ageMs = (nowMs - setTime).coerceAtLeast(0L)
                    if (ageMs < recoveryWindowMs) {
                        val rpeFactor = ((result.rpe ?: 8).coerceIn(5, 10)) / 8.0
                        val primaryWeight = if (isPrimary) 1.0 else 0.55
                        val baseStrain = 14.0 * rpeFactor * primaryWeight
                        val remainingFraction = 1.0 - (ageMs.toDouble() / recoveryWindowMs.toDouble())
                        fatigue += baseStrain * remainingFraction
                    }
                }
            }
            val recovery = (100.0 - fatigue).roundToInt().coerceIn(15, 100)
            val hoursAgo = lastTrainedMs?.let { ((nowMs - it).coerceAtLeast(0L) / 3600_000L).toInt() }
            MuscleStatus(
                muscle = muscle,
                recoveryPercent = recovery,
                workingSetsLast7Days = sets7d,
                lastTrainedHoursAgo = hoursAgo
            )
        }
    }

    fun freshestMuscles(state: AppState, count: Int = 4, nowMs: Long = System.currentTimeMillis()): List<String> =
        calculate(state, nowMs)
            .sortedWith(compareByDescending<MuscleStatus> { it.recoveryPercent }.thenBy { it.workingSetsLast7Days })
            .take(count)
            .map { it.muscle }

    /** Epley formula for Estimated One-Rep Max (1RM) in kg. */
    fun estimate1RMKg(weightKg: Double, reps: Int): Double {
        if (weightKg <= 0.0 || reps <= 0) return 0.0
        if (reps == 1) return weightKg
        return weightKg * (1.0 + reps.coerceAtMost(20) / 30.0)
    }

    /** Highest historical Estimated 1RM in kg for an exercise across all sessions. */
    fun best1RMKg(state: AppState, exerciseId: String): Double =
        state.sessions
            .flatMap { it.results }
            .filter { it.exerciseId == exerciseId && !it.warmup }
            .maxOfOrNull { estimate1RMKg(it.weightKg, it.reps) } ?: 0.0

    /** Total working-set volume (tonnage) in kg for a session. */
    fun sessionVolumeKg(session: Session): Double =
        session.results.filter { !it.warmup && it.weightKg > 0.0 && it.reps > 0 }.sumOf { result ->
            val multiplier = if (Catalog.byId[result.exerciseId]?.perHand == true) 2.0 else 1.0
            result.weightKg * result.reps * multiplier
        }

    /** Detects Personal Records (Weight PR, Estimated 1RM PR, Hold PR) against completed session history. */
    fun detectPRs(state: AppState, currentSessionId: String, result: SetResult): List<String> {
        if (result.warmup) return emptyList()
        val ex = Catalog.byId[result.exerciseId] ?: return emptyList()
        val pastSets = state.sessions
            .filter { it.finishedAt != null && it.id != currentSessionId }
            .flatMap { it.results }
            .filter { it.exerciseId == result.exerciseId && !it.warmup }
        if (pastSets.isEmpty()) return listOf("🏆 First Logged Set PR!")

        val badges = mutableListOf<String>()
        if (ex.timed) {
            val prevBestHold = pastSets.maxOfOrNull { it.seconds } ?: 0
            if (result.seconds > prevBestHold) badges += "🏆 New Hold Time PR (${result.seconds}s)!"
        } else if (result.weightKg > 0.0 && result.reps > 0) {
            val prevBestWeight = pastSets.maxOfOrNull { it.weightKg } ?: 0.0
            if (result.weightKg > prevBestWeight + 0.01) badges += "🔥 New Max Weight PR!"
            val prevBest1RM = pastSets.maxOfOrNull { estimate1RMKg(it.weightKg, it.reps) } ?: 0.0
            val new1RM = estimate1RMKg(result.weightKg, result.reps)
            if (new1RM > prevBest1RM + 0.05) badges += "🏆 New Estimated 1RM PR!"
        } else if (result.reps > 0) {
            val prevBestReps = pastSets.maxOfOrNull { it.reps } ?: 0
            if (result.reps > prevBestReps) badges += "💪 New Rep PR (${result.reps} reps)!"
        }
        return badges
    }
}
