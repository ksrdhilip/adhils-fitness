package com.adhils.fitness

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adhils.fitness.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FitnessViewModel(app:Application):AndroidViewModel(app) {
    private val repository=FitnessRepository((app as FitnessApplication).database.snapshot(),java.io.File(app.filesDir,"adhils-fitness-snapshot.json"))
    val companion=CompanionClient(app)
    private val mutex=Mutex()
    private var dataEpoch=0L
    val store=MutableStateFlow(ProfileStore().initialized())
    val ready=MutableStateFlow(false)
    val error=MutableStateFlow<String?>(null)
    val busy=MutableStateFlow(false)
    val proposal=MutableStateFlow<CoachReply?>(null)
    val connectionStatus=MutableStateFlow("Not checked")
    val lastPrBadges=MutableStateFlow<List<String>>(emptyList())
    val livePostureFeedback=MutableStateFlow<String?>(null)
    val postureBusy=MutableStateFlow(false)
    init { viewModelScope.launch {
        try { store.value=repository.load(); ready.value=true }
        catch(e:Exception) { error.value="Could not load saved data: ${e.message}. Restart the app; data has not been replaced." }
    } }
    private fun mutate(change:(ProfileStore)->ProfileStore) { viewModelScope.launch {
        try { mutex.withLock { val next=change(store.value).validated(); repository.save(next); store.value=next } }
        catch(e:Exception) { error.value=e.message ?: "Could not save changes" }
    } }
    fun edit(id:String=store.value.selectedId,change:(AppState)->AppState)=mutate { it.update(id,change) }
    fun selectProfile(id:String) { proposal.value=null; lastPrBadges.value=emptyList(); mutate { it.select(id) } }
    fun addProfile(name:String) { proposal.value=null; lastPrBadges.value=emptyList(); mutate { it.add(name) } }
    fun saveProfile(profile:Profile)=edit { it.copy(profile=profile.copy(onboardingComplete=true)) }
    fun start(check:CheckIn, routine:SavedRoutine?=null) = edit { state ->
        require(state.active==null) { "Resume or finish your current workout first." }
        lastPrBadges.value=emptyList()
        val session=if(routine==null) Training.generate(state,check) else Session(title=routine.name,plan=routine.plan.filter {
            it.exerciseId !in state.profile.excluded && (Catalog.get(it.exerciseId).equipment=="Bodyweight" || Catalog.get(it.exerciseId).equipment in state.profile.equipment)
        })
        require(session.plan.isNotEmpty()) { "No exercises in this routine match the selected profile." }
        state.copy(sessions=state.sessions+session)
    }
    private fun session(change:(Session)->Session)=edit { state ->
        val active=state.active ?: error("No active workout")
        state.copy(sessions=state.sessions.map { if(it.id==active.id) change(it) else it })
    }
    fun saveSet(result:SetResult)=edit { state ->
        val active=state.active ?: error("No active workout")
        lastPrBadges.value=Recovery.detectPRs(state,active.id,result)
        val updated=Training.saveSet(active,result)
        state.copy(sessions=state.sessions.map { if(it.id==active.id) updated else it })
    }
    fun toggleSuperset(index:Int)=session { Training.toggleSupersetWithNext(it,index) }
    fun moveExercise(index:Int,delta:Int)=session { Training.moveExercise(it,index,delta) }
    fun adjustSetCount(exerciseId:String,delta:Int)=session { Training.adjustSetCount(it,exerciseId,delta) }
    fun switchGymPreset(preset:String)=edit { state ->
        val eq=when(preset) {
            "Commercial Gym" -> setOf("Bodyweight","Dumbbells","Bands","Bench")
            "Bodyweight / Travel" -> setOf("Bodyweight")
            else -> setOf("Bodyweight","Dumbbells","Bands")
        }
        state.copy(profile=state.profile.copy(gymPreset=preset,equipment=eq))
    }
    fun nextExercise(index:Int)=session { it.copy(currentExercise=index.coerceIn(it.plan.indices)) }
    fun changeRest(delta:Long)=session {
        it.copy(restUntil=if(delta==0L) 0 else (it.restUntil.coerceAtLeast(System.currentTimeMillis())+delta).coerceAtLeast(System.currentTimeMillis()))
    }
    fun adjustTimers(exerciseId:String,exerciseDelta:Int=0,restDelta:Int=0)=session { Training.adjustTimers(it,exerciseId,exerciseDelta,restDelta) }
    fun addExercise(newId:String)=edit { state ->
        val active=state.active ?: error("No active workout")
        val updated=Training.addExercise(active,newId,state)
        state.copy(sessions=state.sessions.map { if(it.id==active.id) updated else it })
    }
    fun removeExercise(exerciseId:String)=session { Training.removeExercise(it,exerciseId) }
    fun replace(oldId:String,newId:String,remember:Boolean) {
        val id=store.value.selectedId
        edit(id) { state ->
            val active=state.active ?: error("No active workout")
            val updated=Training.replace(active,oldId,newId,state.profile)
            state.copy(sessions=state.sessions.map { if(it.id==active.id) updated else it },
                profile=if(remember) state.profile.copy(excluded=state.profile.excluded+oldId) else state.profile)
        }
    }
    fun finish()=session { require(it.results.isNotEmpty()) { "Log a set before finishing, or discard the workout." }; it.copy(finishedAt=System.currentTimeMillis(),restUntil=0,revision=it.revision+1) }
    fun discard()=edit { it.copy(sessions=it.sessions.filter { s -> s.finishedAt!=null }) }
    fun saveRoutine(name:String,plan:List<PlannedExercise>)=edit { it.copy(routines=it.routines+SavedRoutine(name=name,plan=plan)) }
    fun bodyWeight(value:Double)=edit { require(value.isFinite() && value in 1.0..650.0); it.copy(measurements=it.measurements+Measurement(weightKg=value)) }
    fun video(exerciseId:String,uri:String,profileId:String)=edit(profileId) { it.copy(videos=it.videos+(exerciseId to uri)) }
    fun clearProfileData() { dataEpoch++;proposal.value=null; edit { AppState(profile=it.profile) } }
    fun applyProposal() {
        val suggestion=proposal.value?.proposal ?: return
        edit { CoachChanges.applyToState(it,suggestion) }; proposal.value=null
    }
    fun ask(text:String,weekly:Boolean=false) {
        if(text.isBlank() || busy.value) return
        val origin=store.value.selected
        val epoch=dataEpoch
        busy.value=true; proposal.value=null
        edit(origin.id) { it.copy(chats=(it.chats+ChatEntry("you",text)).takeLast(100)) }
        viewModelScope.launch {
            try {
                val p=origin.state.profile
                val catalogList=Catalog.exercises.filter {
                    it.id !in p.excluded && (it.equipment=="Bodyweight" || it.equipment in p.equipment)
                }.map { CatalogExerciseSummary(it.id,it.name,it.pattern,it.equipment,it.muscles,it.timed) }
                val request=CoachRequest(profileId=origin.id,message=text.take(4000),profile=p,
                    session=origin.state.active,recentSessions=origin.state.sessions.filter { it.finishedAt!=null }.takeLast(12),
                    provider=companion.provider,weekly=weekly,conversation=origin.state.chats.takeLast(8),
                    availableExercises=catalogList)
                val reply=companion.coach(request)
                mutex.withLock {
                    if(epoch!=dataEpoch) return@withLock
                    // Do not attach a reply to a different profile after switching.
                    val next=store.value.update(origin.id) { it.copy(chats=(it.chats+ChatEntry("coach",reply.explanation)).takeLast(100)) }
                    repository.save(next); store.value=next
                    if(store.value.selectedId==origin.id) proposal.value=reply
                }
            } catch(e:Exception) { error.value=e.message ?: "PC companion unavailable" }
            finally { busy.value=false }
        }
    }
    fun askPostureFeedback(exercise:Exercise,obs:PoseObservation,videoUrl:String,onSpoken:((String)->Unit)?=null) {
        if(postureBusy.value) return
        val origin=store.value.selected
        postureBusy.value=true
        viewModelScope.launch {
            try {
                val p=origin.state.profile
                val prompt=buildString {
                    append("LIVE POSTURE COACHING REQUEST for ${exercise.name} (${exercise.pattern}, muscles: ${exercise.muscles}). ")
                    append("Reference posture video: $videoUrl. ")
                    append("Reference form instructions: ${exercise.instructions.joinToString(" | ")}. Breathing: ${exercise.breathing}. ")
                    append("Live camera joint telemetry: ")
                    if(exercise.timed) append("Hold time=${obs.holdSeconds}s, ") else append("Completed reps=${obs.reps}, ")
                    append("Form Score=${obs.formScore}/100, ")
                    append("Primary Joint Angle=${obs.primaryAngle} deg, ")
                    append("Hip-Torso Alignment=${obs.hipAlignmentAngle} deg, ")
                    append("Left/Right Symmetry Delta=${obs.symmetryDelta} deg, ")
                    append("Last Rep Tempo=${obs.lastRepDurationMs}ms, ")
                    append("On-device sensor cue='${obs.nextRepFeedback ?: obs.cue ?: obs.status}'. ")
                    append("Give a concise, encouraging 1 to 2 sentence real-time posture correction telling the user specifically how to fix their posture on the VERY NEXT REP based on the reference video and joint angles. Do not propose workout plan changes (set proposal to null).")
                }
                val request=CoachRequest(profileId=origin.id,message=prompt.take(4000),profile=p,
                    session=origin.state.active,recentSessions=origin.state.sessions.filter { it.finishedAt!=null }.takeLast(4),
                    provider=companion.provider,weekly=false,conversation=emptyList(),availableExercises=emptyList())
                val reply=companion.coach(request)
                val feedback=reply.explanation.trim()
                livePostureFeedback.value=feedback
                onSpoken?.invoke(feedback)
            } catch(_:Exception) {
                val fallback=buildString {
                    append(obs.nextRepFeedback ?: "Form score ${obs.formScore}%: ${exercise.instructions.firstOrNull() ?: "Keep controlled posture"}")
                    append(" Reference cue: ${exercise.instructions.getOrElse(1) { exercise.breathing }}")
                }
                livePostureFeedback.value=fallback
                onSpoken?.invoke(fallback)
            } finally {
                postureBusy.value=false
            }
        }
    }
    fun pair(value:String) { viewModelScope.launch {
        busy.value=true
        try { connectionStatus.value=companion.pair(value) } catch(e:Exception) { error.value=e.message ?: "Pairing failed" }
        finally { busy.value=false }
    } }
    fun status() { viewModelScope.launch {
        try {
            val result=companion.status()
            val spent=result["spentUsd"]?.toString() ?: "0"
            val limit=result["limitUsd"]?.toString() ?: "5"
            connectionStatus.value="Connected · API usage $spent / $limit USD"
        } catch(e:Exception) { connectionStatus.value=e.message ?: "PC unavailable" }
    } }
    suspend fun export(password:CharArray):ByteArray=withContext(Dispatchers.Default) { Backup.encrypt(store.value,password) }
    suspend fun decodeBackup(bytes:ByteArray,password:CharArray):ProfileStore=withContext(Dispatchers.Default) { Backup.decrypt(bytes,password) }
    fun restore(value:ProfileStore) { dataEpoch++;proposal.value=null; mutate { value.validated() } }
}
