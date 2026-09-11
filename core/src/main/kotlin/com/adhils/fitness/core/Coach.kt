package com.adhils.fitness.core

import kotlinx.serialization.Serializable

@Serializable data class PlanChange(val exerciseId: String, val sets: Int)
@Serializable data class CoachProposal(val sessionId: String, val revision: Int, val reason: String,
    val changes: List<PlanChange> = emptyList())
@Serializable data class CoachReply(val explanation: String, val proposal: CoachProposal? = null,
    val provider: String = "", val usageUsd: Double? = null)
@Serializable data class CoachRequest(val requestId: String = newId(), val profileId: String, val message: String,
    val profile: Profile, val session: Session? = null, val recentSessions: List<Session> = emptyList(),
    val provider: String = "codex", val weekly: Boolean = false, val conversation: List<ChatEntry> = emptyList())
object CoachChanges {
    fun apply(session: Session, proposal: CoachProposal): Session {
        require(session.finishedAt == null && session.id == proposal.sessionId && session.revision == proposal.revision) {
            "This suggestion is out of date. Ask the coach to refresh it."
        }
        require(proposal.changes.isNotEmpty() && proposal.changes.size <= session.plan.size)
        require(proposal.changes.map { it.exerciseId }.distinct().size == proposal.changes.size)
        proposal.changes.forEach { change ->
            require(change.sets in 1..5)
            val plan = session.plan.find { it.exerciseId == change.exerciseId } ?: error("Unknown workout exercise")
            require(change.sets <= plan.sets) { "AI may only reduce planned sets in this version." }
            val logged = session.results.filter { it.exerciseId == change.exerciseId }.maxOfOrNull { it.setIndex + 1 } ?: 0
            require(change.sets >= logged) { "Completed sets cannot be removed." }
        }
        return session.copy(plan = session.plan.map { p ->
            proposal.changes.find { it.exerciseId == p.exerciseId }?.let { p.copy(sets = it.sets) } ?: p
        }, revision = session.revision + 1)
    }
}
