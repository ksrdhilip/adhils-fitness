package com.adhils.fitness.core

import kotlin.math.roundToInt

data class CheckIn(
    val energy: Int = 3, val soreness: Int = 0, val minutes: Int = 40,
    val feeling: String = "Good", val focus: String = "Full Body",
    val targetMuscles: Set<String> = emptySet(), val supersets: Boolean = false
)
data class Progression(val weightKg: Double, val reason: String)
object Training {
    fun roundToStep(weightKg: Double, profile: Profile): Double {
        if (weightKg <= 0.0) return 0.0
        val step = profile.weightStep.takeIf { it.isFinite() && it >= 0.5 } ?: if (profile.unit == "lb") 2.5 else 1.0
        val inUserUnit = fromKg(weightKg, profile.unit)
        val rounded = (inUserUnit / step).roundToInt() * step
        return toKg(rounded.coerceAtLeast(0.0), profile.unit)
    }
    fun nextLoad(id: String, state: AppState): Progression {
        val recent = state.sessions.filter { it.finishedAt != null }
            .sortedByDescending { it.finishedAt }.mapNotNull { s ->
                val plan = s.plan.find { it.exerciseId == id } ?: return@mapNotNull null
                val sets = s.results.filter { it.exerciseId == id && !it.warmup }
                if (sets.isEmpty()) null else plan to sets
            }.take(2)
        val last = recent.firstOrNull() ?: return Progression(0.0, "Start with a comfortable weight and record your first set.")
        val (plan, sets) = last
        val weight = sets.filter { it.reps >= plan.minReps }.maxOfOrNull { it.weightKg } ?: sets.maxOf { it.weightKg }
        if (weight == 0.0) return Progression(0.0, "Build controlled repetitions before adding weight.")
        val stepUnit = state.profile.weightStep.takeIf { it.isFinite() && it >= 0.5 } ?: if (state.profile.unit == "lb") 2.5 else 1.0
        val increment = toKg(stepUnit, state.profile.unit)
        if (sets.size >= plan.sets && sets.all { it.reps >= plan.maxReps && it.rpe != null && it.rpe <= 8 })
            return Progression(weight + increment, "All working sets reached the upper target at effort 8 or below (+${decimalStr(stepUnit)} ${state.profile.unit}).")
        if (recent.size == 2 && recent.all { (p, r) -> r.size >= p.sets && r.any { it.reps < p.minReps } })
            return Progression((weight - increment).coerceAtLeast(0.0), "Two completed sessions missed the lower target. Consider a small reduction.")
        return Progression(weight, "Keep the current weight and build toward the upper rep target.")
    }
    private fun decimalStr(v: Double) = if (v % 1.0 < 0.05) "%.0f".format(java.util.Locale.US, v) else "%.1f".format(java.util.Locale.US, v)
    fun generate(state: AppState, check: CheckIn): Session {
        require(check.energy in 1..5 && check.soreness in 0..2 && check.minutes in 10..120)
        val p = state.profile
        val completed = state.sessions.count { it.finishedAt != null }
        val variant = completed % 4
        val desired = when { check.minutes < 25 -> 3; check.minutes < 35 -> 4; check.minutes < 50 -> 5; else -> 6 }
        val compatible = Catalog.exercises.filter {
            it.id !in p.excluded && (it.equipment == "Bodyweight" || it.equipment in p.equipment) &&
                it.pattern !in setOf("Warm-up", "Mobility")
        }
        val focusKey = check.focus.substringBefore(" (").trim()
        val chosen = when {
            check.targetMuscles.isNotEmpty() -> {
                val matching = compatible.filter { ex -> Recovery.extractMuscles(ex).any { it in check.targetMuscles } }
                val rotated = if (matching.isNotEmpty()) matching.drop((variant * 2) % matching.size) + matching.take((variant * 2) % matching.size) else compatible
                rotated.distinctBy { it.id }.take(desired)
            }
            focusKey == "Fresh Muscle Groups" || focusKey == "Fresh Muscles" -> {
                val fresh = Recovery.freshestMuscles(state, 4).toSet()
                val matching = compatible.filter { ex -> Recovery.extractMuscles(ex).firstOrNull() in fresh }
                val rotated = if (matching.isNotEmpty()) matching.drop((variant * 2) % matching.size) + matching.take((variant * 2) % matching.size) else compatible
                rotated.distinctBy { it.id }.take(desired)
            }
            focusKey == "Full Body" || focusKey.isEmpty() -> {
                val ids = when (variant) {
                    0 -> listOf("goblet-squat", "rdl", "press", "row", "plank", "lateral-raise")
                    1 -> listOf("reverse-lunge", "bridge", "floor-press", "curl", "dead-bug", "tricep-extension")
                    2 -> listOf("sumo-squat", "single-leg-rdl", "arnold-press", "hammer-curl", "side-plank", "calf-raise")
                    else -> listOf("bulgarian-split-squat", "hip-thrust", "bench-press", "chest-supported-row", "bird-dog", "front-raise")
                }
                ids.mapNotNull { id ->
                    val e = Catalog.get(id)
                    if (e.id !in p.excluded && (e.equipment == "Bodyweight" || e.equipment in p.equipment)) e
                    else compatible.firstOrNull { it.pattern == e.pattern }
                }.distinctBy { it.id }.take(desired)
            }
            else -> {
                val primaryPatterns = when (focusKey) {
                    "Upper Body" -> listOf("Push", "Pull", "Carry", "Core")
                    "Lower Body" -> listOf("Squat", "Hinge", "Carry", "Core")
                    "Push" -> listOf("Push", "Core")
                    "Pull" -> listOf("Pull", "Hinge", "Core")
                    "Core & Mobility" -> listOf("Core", "Carry")
                    else -> listOf("Squat", "Hinge", "Push", "Pull", "Core")
                }
                val pool = if (focusKey == "Core & Mobility") {
                    Catalog.exercises.filter {
                        it.id !in p.excluded && (it.equipment == "Bodyweight" || it.equipment in p.equipment) &&
                            it.pattern in setOf("Core", "Mobility", "Carry") && it.id !in setOf("march", "cat-cow")
                    }
                } else compatible.filter { it.pattern in primaryPatterns }
                val rotated = if (pool.isNotEmpty()) pool.drop((variant * 2) % pool.size) + pool.take((variant * 2) % pool.size) else compatible
                rotated.distinctBy { it.id }.take(desired)
            }
        }
        require(chosen.isNotEmpty()) { "No compatible exercises. Update equipment or restrictions." }
        val easy = check.energy <= 2 || check.soreness == 2 || check.feeling in setOf("Tired", "Sore / Stiff", "Recovering")
        val energized = check.feeling == "Energized" && check.energy >= 4 && check.soreness == 0
        val sets = when {
            easy || check.minutes < 30 || p.experience == "Beginner" -> 2
            energized && check.minutes >= 45 -> 4
            else -> 3
        }
        val repRange = when (p.goal) { "Get stronger" -> 5..8; "General fitness" -> 10..15; else -> 8..12 }
        val warmup = if ("march" !in p.excluded) listOf(PlannedExercise("march", 1, seconds = 120, restSeconds = 45)) else emptyList()
        val coolDown = if ("cat-cow" !in p.excluded) listOf(PlannedExercise("cat-cow", 1, seconds = 60, restSeconds = 45)) else emptyList()
        val label = listOf("A", "B", "C", "D")[variant]
        val baseTitle = when {
            check.targetMuscles.isNotEmpty() -> check.targetMuscles.take(2).joinToString(" & ") + " Focus"
            focusKey.isBlank() -> "Full Body $label"
            else -> "$focusKey $label"
        }
        val groupLabels = listOf("A", "B", "C", "D")
        val mainPlan = chosen.mapIndexed { idx, it ->
            val rawWeight = nextLoad(it.id, state).weightKg * if (easy) 0.8 else 1.0
            val group = if (check.supersets && chosen.size >= 2 && (idx / 2) < chosen.size / 2) groupLabels.getOrNull(idx / 2) else null
            PlannedExercise(it.id, sets = if (it.timed) 2 else sets, minReps = repRange.first, maxReps = repRange.last,
                weightKg = if (easy) roundToStep(rawWeight, p) else rawWeight, seconds = 45, restSeconds = 45, supersetGroup = group)
        }
        return Session(title = "$baseTitle${if (easy) " · Light" else if (energized) " · Strong" else ""}",
            plan = warmup + mainPlan + coolDown)
    }
    fun saveSet(session: Session, result: SetResult): Session {
        require(session.finishedAt == null) { "Workout already finished" }
        val plan = session.plan.find { it.exerciseId == result.exerciseId } ?: error("Exercise not in workout")
        require(validSet(result) && result.setIndex < plan.sets)
        require(if (Catalog.get(result.exerciseId).timed) result.seconds > 0 else result.reps > 0)
        val existing = session.results.find { it.exerciseId == result.exerciseId && it.setIndex == result.setIndex }
        val value = result.copy(id = existing?.id ?: result.id)
        val updatedResults = session.results.filterNot {
            it.exerciseId == result.exerciseId && it.setIndex == result.setIndex
        } + value
        // If part of a Superset, advance to the partner exercise in the superset if it has uncompleted sets for this round
        var nextIndex = session.currentExercise
        var restMs = plan.restSeconds * 1000L
        if (plan.supersetGroup != null) {
            val partners = session.plan.mapIndexedNotNull { idx, p -> if (p.supersetGroup == plan.supersetGroup) idx to p else null }
            val nextPartner = partners.firstOrNull { (idx, p) ->
                idx != session.currentExercise && updatedResults.none { it.exerciseId == p.exerciseId && it.setIndex == result.setIndex }
            }
            if (nextPartner != null) {
                nextIndex = nextPartner.first
                restMs = 15_000L // Quick 15s transition between superset exercises
            } else {
                val firstIncomplete = partners.firstOrNull { (_, p) ->
                    (0 until p.sets).any { sIdx -> updatedResults.none { it.exerciseId == p.exerciseId && it.setIndex == sIdx } }
                }
                if (firstIncomplete != null) nextIndex = firstIncomplete.first
            }
        }
        return session.copy(results = updatedResults, currentExercise = nextIndex, revision = session.revision + 1,
            restUntil = System.currentTimeMillis() + restMs)
    }
    fun toggleSupersetWithNext(session: Session, index: Int): Session {
        require(session.finishedAt == null && index in 0 until session.plan.lastIndex)
        val current = session.plan[index]
        val next = session.plan[index + 1]
        val newGroup = if (current.supersetGroup != null && current.supersetGroup == next.supersetGroup) null else "S${index + 1}"
        val updated = session.plan.mapIndexed { idx, p ->
            if (idx == index || idx == index + 1) p.copy(supersetGroup = newGroup) else p
        }
        return session.copy(plan = updated, revision = session.revision + 1)
    }
    fun adjustTimers(session: Session, exerciseId: String, exerciseDelta: Int = 0, restDelta: Int = 0): Session {
        require(session.finishedAt == null)
        return session.copy(plan = session.plan.map {
            if (it.exerciseId == exerciseId) it.copy(
                seconds = (it.seconds + exerciseDelta).coerceIn(5, 3600),
                restSeconds = (it.restSeconds + restDelta).coerceIn(5, 600)
            ) else it
        }, revision = session.revision + 1)
    }
    fun replace(session: Session, oldId: String, newId: String, profile: Profile): Session {
        require(session.finishedAt == null)
        require(session.results.none { it.exerciseId == oldId }) { "Completed sets cannot be replaced." }
        require(newId !in session.plan.map { it.exerciseId }) { "Exercise already in workout" }
        require(Catalog.byId.containsKey(newId) && newId !in profile.excluded) { "Not a compatible replacement" }
        return session.copy(plan = session.plan.map { if (it.exerciseId == oldId) it.copy(exerciseId = newId, weightKg = 0.0) else it },
            revision = session.revision + 1)
    }
    fun addExercise(session: Session, newId: String, state: AppState): Session {
        require(session.finishedAt == null && session.plan.size < 30)
        require(Catalog.byId.containsKey(newId) && newId !in session.plan.map { it.exerciseId }) { "Exercise is already in this workout" }
        val e = Catalog.get(newId)
        val added = PlannedExercise(newId, sets = if (e.timed) 2 else 3, weightKg = nextLoad(newId, state).weightKg, seconds = 45, restSeconds = 45)
        return session.copy(plan = session.plan + added, revision = session.revision + 1)
    }
    fun removeExercise(session: Session, exerciseId: String): Session {
        require(session.finishedAt == null && session.plan.size > 1) { "Workout must keep at least one exercise." }
        require(session.results.none { it.exerciseId == exerciseId }) { "Completed sets cannot be removed." }
        val updated = session.plan.filterNot { it.exerciseId == exerciseId }
        return session.copy(plan = updated, currentExercise = session.currentExercise.coerceIn(updated.indices), revision = session.revision + 1)
    }
    fun moveExercise(session: Session, index: Int, delta: Int): Session {
        require(session.finishedAt == null)
        val target = index + delta
        if (index !in session.plan.indices || target !in session.plan.indices) return session
        val mutable = session.plan.toMutableList()
        val item = mutable.removeAt(index)
        mutable.add(target, item)
        val newCurrent = when (session.currentExercise) {
            index -> target
            target -> index
            else -> session.currentExercise
        }
        return session.copy(plan = mutable, currentExercise = newCurrent, revision = session.revision + 1)
    }
    fun adjustSetCount(session: Session, exerciseId: String, delta: Int): Session {
        require(session.finishedAt == null)
        val maxLogged = (session.results.filter { it.exerciseId == exerciseId }.maxOfOrNull { it.setIndex } ?: -1) + 1
        return session.copy(
            plan = session.plan.map { p ->
                if (p.exerciseId == exerciseId) {
                    val nextSets = (p.sets + delta).coerceIn(maxLogged.coerceAtLeast(1), 8)
                    p.copy(sets = nextSets)
                } else p
            },
            revision = session.revision + 1
        )
    }
}
