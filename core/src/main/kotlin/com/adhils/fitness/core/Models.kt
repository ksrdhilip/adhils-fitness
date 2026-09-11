package com.adhils.fitness.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

val AppJson = Json { ignoreUnknownKeys = false; encodeDefaults = true }
fun newId(): String = UUID.randomUUID().toString()

@Serializable data class Profile(
    val name: String = "Dhilip", val goal: String = "Build muscle",
    val experience: String = "Beginner", val equipment: Set<String> = setOf("Dumbbells", "Bodyweight"),
    val days: Int = 3, val minutes: Int = 40, val unit: String = "lb",
    val excluded: Set<String> = emptySet(), val restrictions: String = "",
    val voice: Boolean = true, val experimentalCues: Boolean = false,
    val theme: String = "Dark", val onboardingComplete: Boolean = false,
    val bodyWeight: Double? = null
)
@Serializable data class Exercise(
    val id: String, val name: String, val pattern: String, val equipment: String,
    val muscles: String, val instructions: List<String>, val breathing: String,
    val camera: String? = null, val view: String = "Side", val timed: Boolean = false,
    val perHand: Boolean = false
)
@Serializable data class PlannedExercise(
    val exerciseId: String, val sets: Int = 3, val minReps: Int = 8, val maxReps: Int = 12,
    val weightKg: Double = 0.0, val seconds: Int = 30
)
@Serializable data class SetResult(
    val id: String = newId(), val exerciseId: String, val setIndex: Int,
    val reps: Int = 0, val weightKg: Double = 0.0, val seconds: Int = 0,
    val rpe: Int? = null, val warmup: Boolean = false, val notes: String = "",
    val savedAt: Long = System.currentTimeMillis(), val observations: List<String> = emptyList()
)
@Serializable data class Session(
    val id: String = newId(), val title: String = "Full Body A",
    val startedAt: Long = System.currentTimeMillis(), val finishedAt: Long? = null,
    val plan: List<PlannedExercise> = emptyList(), val results: List<SetResult> = emptyList(),
    val currentExercise: Int = 0, val revision: Int = 0, val restUntil: Long = 0
)
@Serializable data class Measurement(val at: Long = System.currentTimeMillis(), val weightKg: Double)
@Serializable data class ChatEntry(val role: String, val text: String, val at: Long = System.currentTimeMillis())
@Serializable data class SavedRoutine(val id: String = newId(), val name: String, val plan: List<PlannedExercise>)
@Serializable data class AppState(
    val schemaVersion: Int = 1, val profile: Profile = Profile(),
    val sessions: List<Session> = emptyList(), val measurements: List<Measurement> = emptyList(),
    val chats: List<ChatEntry> = emptyList(), val routines: List<SavedRoutine> = emptyList(),
    val videos: Map<String, String> = emptyMap()
) {
    val active: Session? get() = sessions.firstOrNull { it.finishedAt == null }
    fun validated(): AppState {
        require(schemaVersion == 1) { "Unsupported backup version" }
        require(profile.days in 1..7 && profile.minutes in 10..120)
        require(profile.unit in setOf("kg", "lb"))
        require(sessions.size <= 20000 && sessions.count { it.finishedAt == null } <= 1)
        require(sessions.map { it.id }.distinct().size == sessions.size)
        require(sessions.all { session ->
            session.plan.isNotEmpty() && session.plan.size <= 30 &&
            session.currentExercise in session.plan.indices &&
            session.plan.map { it.exerciseId }.distinct().size == session.plan.size &&
            session.plan.all { validPlan(it) } &&
            session.results.all { it.exerciseId in session.plan.map { p -> p.exerciseId } && validSet(it) } &&
            session.results.map { it.exerciseId to it.setIndex }.distinct().size == session.results.size
        }) { "Backup contains invalid workout data" }
        require(measurements.all { it.weightKg.isFinite() && it.weightKg in 1.0..650.0 })
        require(routines.all { it.plan.isNotEmpty() && it.plan.all(::validPlan) })
        return this
    }
}
fun validPlan(p: PlannedExercise) = Catalog.byId.containsKey(p.exerciseId) && p.sets in 1..10 &&
    p.minReps in 1..100 && p.maxReps in p.minReps..100 && p.weightKg.isFinite() && p.weightKg in 0.0..500.0 && p.seconds in 5..3600
fun validSet(s: SetResult) = s.setIndex in 0..9 && s.reps in 0..100 && s.seconds in 0..3600 &&
    s.weightKg.isFinite() && s.weightKg in 0.0..500.0 && (s.rpe == null || s.rpe in 1..10) && s.notes.length <= 2000
fun toKg(value: Double, unit: String) = if (unit == "lb") value / 2.2046226218 else value
fun fromKg(value: Double, unit: String) = if (unit == "lb") value * 2.2046226218 else value
