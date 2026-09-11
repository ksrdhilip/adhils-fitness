package com.adhils.fitness.core

data class CheckIn(val energy: Int = 3, val soreness: Int = 0, val minutes: Int = 40)
data class Progression(val weightKg: Double, val reason: String)
object Training {
    fun nextLoad(id: String, state: AppState): Progression {
        val recent = state.sessions.filter { it.finishedAt != null }
            .sortedByDescending { it.finishedAt }.mapNotNull { s ->
                val plan = s.plan.find { it.exerciseId == id } ?: return@mapNotNull null
                val sets = s.results.filter { it.exerciseId == id && !it.warmup }
                if (sets.isEmpty()) null else plan to sets
            }.take(2)
        val last = recent.firstOrNull() ?: return Progression(0.0, "Start with a comfortable weight and record your first set.")
        val (plan, sets) = last
        val weight = sets.minOf { it.weightKg }
        if (weight == 0.0) return Progression(0.0, "Build controlled repetitions before adding weight.")
        val increment = toKg(if (state.profile.unit == "lb") 2.5 else 1.0, state.profile.unit)
        if (sets.size >= plan.sets && sets.all { it.reps >= plan.maxReps && it.rpe != null && it.rpe <= 8 })
            return Progression(weight + increment, "All working sets reached the upper target at effort 8 or below. Consider the smallest available increase.")
        if (recent.size == 2 && recent.all { (p, r) -> r.size >= p.sets && r.any { it.reps < p.minReps } })
            return Progression((weight - increment).coerceAtLeast(0.0), "Two completed sessions missed the lower target. Consider a small reduction.")
        return Progression(weight, "Keep the current weight and build toward the upper rep target.")
    }
    fun generate(state: AppState, check: CheckIn): Session {
        require(check.energy in 1..5 && check.soreness in 0..2 && check.minutes in 10..120)
        val p = state.profile
        val completed = state.sessions.count { it.finishedAt != null }
        val variant = completed % 2
        val ids = if (variant == 0) listOf("goblet-squat", "rdl", "press", "row", "plank")
            else listOf("reverse-lunge", "bridge", "floor-press", "curl", "dead-bug")
        val desired = when { check.minutes < 25 -> 3; check.minutes < 35 -> 4; else -> 5 }
        val chosen = ids.mapNotNull { id ->
            val e = Catalog.get(id)
            if (e.id !in p.excluded && (e.equipment == "Bodyweight" || e.equipment in p.equipment)) e
            else Catalog.exercises.firstOrNull { it.pattern == e.pattern && it.id !in p.excluded &&
                (it.equipment == "Bodyweight" || it.equipment in p.equipment) }
        }.distinctBy { it.id }.take(desired)
        require(chosen.isNotEmpty()) { "No compatible exercises. Update equipment or restrictions." }
        val easy = check.energy <= 2 || check.soreness == 2
        val sets = if (easy || check.minutes < 30 || p.experience == "Beginner") 2 else 3
        val repRange = when (p.goal) { "Get stronger" -> 5..8; "General fitness" -> 10..15; else -> 8..12 }
        val warmup = if ("march" !in p.excluded) listOf(PlannedExercise("march", 1, seconds = 120)) else emptyList()
        val coolDown = if ("cat-cow" !in p.excluded) listOf(PlannedExercise("cat-cow", 1, seconds = 60)) else emptyList()
        return Session(title = "Full Body ${if (variant == 0) "A" else "B"}${if (easy) " · Light" else ""}",
            plan = warmup + chosen.map {
                PlannedExercise(it.id, sets = if (it.timed) 2 else sets, minReps = repRange.first, maxReps = repRange.last,
                    weightKg = nextLoad(it.id, state).weightKg * if (easy) 0.8 else 1.0)
            } + coolDown)
    }
    fun saveSet(session: Session, result: SetResult): Session {
        require(session.finishedAt == null) { "Workout already finished" }
        val plan = session.plan.find { it.exerciseId == result.exerciseId } ?: error("Exercise not in workout")
        require(validSet(result) && result.setIndex < plan.sets)
        require(if (Catalog.get(result.exerciseId).timed) result.seconds > 0 else result.reps > 0)
        val existing = session.results.find { it.exerciseId == result.exerciseId && it.setIndex == result.setIndex }
        val value = result.copy(id = existing?.id ?: result.id)
        return session.copy(results = session.results.filterNot {
            it.exerciseId == result.exerciseId && it.setIndex == result.setIndex
        } + value, revision = session.revision + 1, restUntil = System.currentTimeMillis() + 90_000)
    }
    fun replace(session: Session, oldId: String, newId: String, profile: Profile): Session {
        require(session.finishedAt == null)
        require(session.results.none { it.exerciseId == oldId }) { "Completed sets cannot be replaced." }
        require(newId !in session.plan.map { it.exerciseId }) { "Exercise already in workout" }
        require(Catalog.alternatives(oldId, profile).any { it.id == newId }) { "Not a compatible replacement" }
        return session.copy(plan = session.plan.map { if (it.exerciseId == oldId) it.copy(exerciseId = newId, weightKg = 0.0) else it },
            revision = session.revision + 1)
    }
}
