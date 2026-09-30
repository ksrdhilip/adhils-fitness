package com.adhils.fitness.core

import kotlinx.serialization.Serializable

@Serializable data class CatalogExerciseSummary(
    val id: String, val name: String, val pattern: String, val equipment: String,
    val muscles: String, val timed: Boolean = false
)
@Serializable data class PlanChange(
    val exerciseId: String, val sets: Int = 3,
    val minReps: Int? = null, val maxReps: Int? = null,
    val seconds: Int? = null, val restSeconds: Int? = null
)
@Serializable data class CoachProposal(
    val sessionId: String? = null, val revision: Int = 0, val reason: String,
    val title: String? = null, val replacePlan: Boolean = false,
    val changes: List<PlanChange> = emptyList()
)
@Serializable data class CoachReply(
    val explanation: String, val proposal: CoachProposal? = null,
    val provider: String = "", val usageUsd: Double? = null
)
@Serializable data class CoachRequest(
    val requestId: String = newId(), val profileId: String, val message: String,
    val profile: Profile, val session: Session? = null, val recentSessions: List<Session> = emptyList(),
    val provider: String = "codex", val weekly: Boolean = false, val conversation: List<ChatEntry> = emptyList(),
    val availableExercises: List<CatalogExerciseSummary> = emptyList()
)
object CoachChanges {
    private fun toPlanned(c: PlanChange, existing: PlannedExercise?, state: AppState?): PlannedExercise {
        val minR = (c.minReps ?: existing?.minReps ?: 8).coerceIn(1, 100)
        val maxR = (c.maxReps ?: existing?.maxReps ?: 12).coerceIn(minR, 100)
        val sec = (c.seconds ?: existing?.seconds ?: 45).coerceIn(5, 3600)
        val rest = (c.restSeconds ?: existing?.restSeconds ?: 45).coerceIn(5, 600)
        val weight = existing?.weightKg ?: (state?.let { Training.nextLoad(c.exerciseId, it).weightKg } ?: 0.0)
        return PlannedExercise(
            exerciseId = c.exerciseId, sets = c.sets.coerceIn(1, 10),
            minReps = minR, maxReps = maxR, weightKg = weight, seconds = sec, restSeconds = rest
        )
    }
    fun apply(session: Session, proposal: CoachProposal, state: AppState? = null): Session {
        require(session.finishedAt == null && (proposal.sessionId.isNullOrBlank() || session.id == proposal.sessionId) && session.revision == proposal.revision) {
            "This suggestion is out of date. Ask the coach to refresh it."
        }
        require(proposal.changes.isNotEmpty() && proposal.changes.size <= 20)
        require(proposal.changes.map { it.exerciseId }.distinct().size == proposal.changes.size)
        proposal.changes.forEach { change ->
            require(Catalog.byId.containsKey(change.exerciseId)) { "Unknown workout exercise: ${change.exerciseId}" }
            require(change.sets in 1..5) { "Sets must be between 1 and 5." }
            val logged = session.results.filter { it.exerciseId == change.exerciseId }.maxOfOrNull { it.setIndex + 1 } ?: 0
            require(change.sets >= logged) { "Completed sets cannot be removed." }
        }
        val hasNewExercises = proposal.changes.any { c -> session.plan.none { it.exerciseId == c.exerciseId } }
        val newPlan = if (proposal.replacePlan || hasNewExercises || proposal.changes.size >= 2) {
            val lockedLogged = session.plan.filter { p ->
                session.results.any { it.exerciseId == p.exerciseId } && proposal.changes.none { it.exerciseId == p.exerciseId }
            }
            val proposed = proposal.changes.map { c ->
                toPlanned(c, session.plan.find { it.exerciseId == c.exerciseId }, state)
            }
            (lockedLogged + proposed).distinctBy { it.exerciseId }.take(30)
        } else {
            session.plan.map { p ->
                proposal.changes.find { it.exerciseId == p.exerciseId }?.let { c -> toPlanned(c, p, state) } ?: p
            }
        }
        require(newPlan.isNotEmpty())
        return session.copy(
            title = proposal.title?.takeIf { it.isNotBlank() }?.take(80) ?: session.title,
            plan = newPlan,
            currentExercise = session.currentExercise.coerceIn(newPlan.indices),
            revision = session.revision + 1
        )
    }
    fun applyToState(state: AppState, proposal: CoachProposal): AppState {
        val active = state.active
        return if (active != null) {
            val updated = apply(active, proposal, state)
            state.copy(sessions = state.sessions.map { if (it.id == active.id) updated else it })
        } else {
            require(proposal.changes.isNotEmpty() && proposal.changes.size <= 20)
            require(proposal.changes.map { it.exerciseId }.distinct().size == proposal.changes.size)
            proposal.changes.forEach { change ->
                require(Catalog.byId.containsKey(change.exerciseId)) { "Unknown workout exercise: ${change.exerciseId}" }
                require(change.sets in 1..5) { "Sets must be between 1 and 5." }
            }
            val plan = proposal.changes.map { c -> toPlanned(c, null, state) }
            val created = Session(
                title = proposal.title?.takeIf { it.isNotBlank() }?.take(80) ?: "Coach Custom Workout",
                plan = plan
            )
            state.copy(sessions = state.sessions + created)
        }
    }
}
