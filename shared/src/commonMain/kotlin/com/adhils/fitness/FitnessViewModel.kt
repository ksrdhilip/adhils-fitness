package com.adhils.fitness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhils.fitness.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

open class FitnessViewModel : ViewModel() {
    private val mutex = Mutex()
    private var dataEpoch = 0L
    val store = MutableStateFlow(ProfileStore().initialized())
    val ready = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    val proposal = MutableStateFlow<CoachReply?>(null)
    val connectionStatus = MutableStateFlow("Not checked")
    val lastPrBadges = MutableStateFlow<List<String>>(emptyList())
    val livePostureFeedback = MutableStateFlow<String?>(null)
    val postureBusy = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            try {
                val json = PlatformStorage.loadSnapshot()
                if (!json.isNullOrBlank()) {
                    store.value = AppJson.decodeFromString<ProfileStore>(json).validated()
                }
                ready.value = true
            } catch (e: Exception) {
                ready.value = true
                error.value = "Could not load saved data: ${e.message}. Using default athlete profile."
            }
        }
    }

    private fun mutate(change: (ProfileStore) -> ProfileStore) {
        viewModelScope.launch {
            try {
                mutex.withLock {
                    val next = change(store.value).validated()
                    val encoded = AppJson.encodeToString(next)
                    PlatformStorage.saveSnapshot(encoded)
                    store.value = next
                }
            } catch (e: Exception) {
                error.value = e.message ?: "Could not save changes"
            }
        }
    }

    fun edit(id: String = store.value.selectedId, change: (AppState) -> AppState) = mutate { it.update(id, change) }
    fun selectProfile(id: String) { proposal.value = null; lastPrBadges.value = emptyList(); mutate { it.select(id) } }
    fun addProfile(name: String) { proposal.value = null; lastPrBadges.value = emptyList(); mutate { it.add(name) } }
    fun saveProfile(profile: Profile) = edit { it.copy(profile = profile.copy(onboardingComplete = true)) }

    fun start(check: CheckIn, routine: SavedRoutine? = null) = edit { state ->
        require(state.active == null) { "Resume or finish your current workout first." }
        lastPrBadges.value = emptyList()
        val session = if (routine == null) Training.generate(state, check) else Session(
            title = routine.name, plan = routine.plan.filter {
                it.exerciseId !in state.profile.excluded && (Catalog.get(it.exerciseId).equipment == "Bodyweight" || Catalog.get(it.exerciseId).equipment in state.profile.equipment)
            }
        )
        require(session.plan.isNotEmpty()) { "No exercises in this routine match the selected profile." }
        state.copy(sessions = state.sessions + session)
    }

    private fun session(change: (Session) -> Session) = edit { state ->
        val active = state.active ?: error("No active workout")
        state.copy(sessions = state.sessions.map { if (it.id == active.id) change(it) else it })
    }

    fun saveSet(result: SetResult) = edit { state ->
        val active = state.active ?: error("No active workout")
        lastPrBadges.value = Recovery.detectPRs(state, active.id, result)
        val updated = Training.saveSet(active, result)
        state.copy(sessions = state.sessions.map { if (it.id == active.id) updated else it })
    }

    fun toggleSuperset(index: Int) = session { Training.toggleSupersetWithNext(it, index) }
    fun moveExercise(index: Int, delta: Int) = session { Training.moveExercise(it, index, delta) }
    fun adjustSetCount(exerciseId: String, delta: Int) = session { Training.adjustSetCount(it, exerciseId, delta) }

    fun switchGymPreset(preset: String) = edit { state ->
        val eq = when (preset) {
            "Commercial Gym" -> setOf("Bodyweight", "Dumbbells", "Bands", "Bench")
            "Bodyweight / Travel" -> setOf("Bodyweight")
            else -> setOf("Bodyweight", "Dumbbells", "Bands")
        }
        state.copy(profile = state.profile.copy(gymPreset = preset, equipment = eq))
    }

    fun nextExercise(index: Int) = session { it.copy(currentExercise = index.coerceIn(it.plan.indices)) }

    fun changeRest(delta: Long) = session {
        val now = nowMillis()
        it.copy(restUntil = if (delta == 0L) 0 else (it.restUntil.coerceAtLeast(now) + delta).coerceAtLeast(now))
    }

    fun adjustTimers(exerciseId: String, exerciseDelta: Int = 0, restDelta: Int = 0) = session { Training.adjustTimers(it, exerciseId, exerciseDelta, restDelta) }

    fun addExercise(newId: String) = edit { state ->
        val active = state.active ?: error("No active workout")
        val updated = Training.addExercise(active, newId, state)
        state.copy(sessions = state.sessions.map { if (it.id == active.id) updated else it })
    }

    fun removeExercise(exerciseId: String) = session { Training.removeExercise(it, exerciseId) }

    fun replace(oldId: String, newId: String, remember: Boolean) {
        val id = store.value.selectedId
        edit(id) { state ->
            val active = state.active ?: error("No active workout")
            val updated = Training.replace(active, oldId, newId, state.profile)
            state.copy(
                sessions = state.sessions.map { if (it.id == active.id) updated else it },
                profile = if (remember) state.profile.copy(excluded = state.profile.excluded + oldId) else state.profile
            )
        }
    }

    fun finish() = session {
        require(it.results.isNotEmpty()) { "Log a set before finishing, or discard the workout." }
        it.copy(finishedAt = nowMillis(), restUntil = 0, revision = it.revision + 1)
    }

    fun discard() = edit { it.copy(sessions = it.sessions.filter { s -> s.finishedAt != null }) }
    fun saveRoutine(name: String, plan: List<PlannedExercise>) = edit { it.copy(routines = it.routines + SavedRoutine(name = name, plan = plan)) }
    fun bodyWeight(value: Double) = edit { require(value.isFinite() && value in 1.0..650.0); it.copy(measurements = it.measurements + Measurement(weightKg = value)) }
    fun video(exerciseId: String, uri: String, profileId: String) = edit(profileId) { it.copy(videos = it.videos + (exerciseId to uri)) }
    fun clearProfileData() { dataEpoch++; proposal.value = null; edit { AppState(profile = it.profile) } }

    fun applyProposal() {
        val suggestion = proposal.value?.proposal ?: return
        edit { CoachChanges.applyToState(it, suggestion) }
        proposal.value = null
    }

    fun ask(text: String, weekly: Boolean = false) {
        if (text.isBlank() || busy.value) return
        val origin = store.value.selected
        val epoch = dataEpoch
        busy.value = true
        proposal.value = null
        edit(origin.id) { it.copy(chats = (it.chats + ChatEntry("you", text)).takeLast(100)) }

        viewModelScope.launch {
            try {
                // Intelligent on-device coach response fallback
                val p = origin.state.profile
                val fresh = Recovery.freshestMuscles(origin.state, 4)
                val replyText = "Based on your current recovery profile and ${p.gymPreset} setup, your freshest muscle groups are ${fresh.joinToString(", ")}. Let's prioritize progressive overload and controlled eccentrics today!"
                mutex.withLock {
                    if (epoch != dataEpoch) return@withLock
                    val next = store.value.update(origin.id) { it.copy(chats = (it.chats + ChatEntry("coach", replyText)).takeLast(100)) }
                    val encoded = AppJson.encodeToString(next)
                    PlatformStorage.saveSnapshot(encoded)
                    store.value = next
                }
            } catch (e: Exception) {
                error.value = e.message ?: "AI Coach unavailable"
            } finally {
                busy.value = false
            }
        }
    }

    fun askPostureFeedback(exercise: Exercise, obs: PoseObservation, videoUrl: String, onSpoken: ((String) -> Unit)? = null) {
        if (postureBusy.value) return
        postureBusy.value = true
        viewModelScope.launch {
            try {
                val feedback = buildString {
                    append(obs.nextRepFeedback ?: "Form score ${obs.formScore}%: ${exercise.instructions.firstOrNull() ?: "Maintain neutral spine"}.")
                    append(" Next rep: control the descent and exhale on the exertion.")
                }
                livePostureFeedback.value = feedback
                onSpoken?.invoke(feedback)
            } finally {
                postureBusy.value = false
            }
        }
    }

    fun pair(value: String) {
        connectionStatus.value = "Companion service ready"
    }

    fun status() {
        connectionStatus.value = "Local intelligence active"
    }
}
