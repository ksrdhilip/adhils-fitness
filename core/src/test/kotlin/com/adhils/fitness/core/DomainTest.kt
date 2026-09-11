package com.adhils.fitness.core
import kotlin.test.*
class DomainTest {
    @Test fun plansRespectEquipmentAndExclusions() {
        val state=AppState(profile=Profile(equipment=setOf("Bodyweight"),excluded=setOf("pushup","march")))
        val plan=Training.generate(state,CheckIn(minutes=20)).plan
        assertTrue(plan.none { Catalog.get(it.exerciseId).equipment=="Dumbbells" || it.exerciseId in state.profile.excluded })
        assertTrue(plan.filter { !Catalog.get(it.exerciseId).timed }.all { it.sets==2 })
    }
    @Test fun repeatedSaveDoesNotDuplicateSet() {
        val s=Session(plan=listOf(PlannedExercise("rdl")))
        val r=SetResult(exerciseId="rdl",setIndex=0,reps=10)
        val twice=Training.saveSet(Training.saveSet(s,r),r.copy(id=newId(),reps=12))
        assertEquals(1,twice.results.size); assertEquals(r.id,twice.results.single().id)
        assertEquals(12,twice.results.single().reps)
    }
    @Test fun progressionRequiresEffortAndAllSets() {
        val plan=PlannedExercise("rdl",weightKg=10.0)
        fun state(effort:Int?)=AppState(sessions=listOf(Session(plan=listOf(plan),finishedAt=123,
            results=(0..2).map { SetResult(exerciseId="rdl",setIndex=it,reps=12,weightKg=10.0,rpe=effort) })))
        assertEquals(10.0,Training.nextLoad("rdl",state(null)).weightKg)
        assertEquals(10.0,Training.nextLoad("rdl",state(10)).weightKg)
        assertTrue(Training.nextLoad("rdl",state(7)).weightKg>10)
    }
    @Test fun staleAndDestructiveAiChangesAreRejected() {
        val session=Training.saveSet(Session(plan=listOf(PlannedExercise("rdl"))),
            SetResult(exerciseId="rdl",setIndex=2,reps=10))
        assertFails { CoachChanges.apply(session,CoachProposal(session.id,0,"shorter",listOf(PlanChange("rdl",2)))) }
        assertFails { CoachChanges.apply(session,CoachProposal(session.id,session.revision,"shorter",listOf(PlanChange("rdl",2)))) }
        assertFails { CoachChanges.apply(session,CoachProposal(session.id,session.revision,"more",listOf(PlanChange("rdl",4)))) }
    }
    @Test fun profileSwitchingIsolatesHistoryAndProgression() {
        val initial=ProfileStore().initialized()
        val firstId=initial.selectedId
        val workout=Session(plan=listOf(PlannedExercise("rdl")),finishedAt=123,
            results=(0..2).map { SetResult(exerciseId="rdl",setIndex=it,reps=12,weightKg=20.0,rpe=7) })
        val updated=initial.update { it.copy(sessions=listOf(workout)) }.add("Another person")
        assertTrue(updated.selected.state.sessions.isEmpty())
        assertEquals(0.0,Training.nextLoad("rdl",updated.selected.state).weightKg)
        assertEquals(workout,updated.select(firstId).selected.state.sessions.single())
        val originUpdate=updated.update(firstId) { it.copy(chats=listOf(ChatEntry("coach","Private reply"))) }
        assertTrue(originUpdate.selected.state.chats.isEmpty())
    }
    @Test fun encryptedBackupRoundTripRejectsWrongPasswordAndTampering() {
        val state=ProfileStore().initialized().add("Second")
        val data=Backup.encrypt(state,"a-long-password".toCharArray())
        assertEquals(state,Backup.decrypt(data,"a-long-password".toCharArray()))
        assertFails { Backup.decrypt(data,"wrong-password".toCharArray()) }
        data[data.lastIndex]=(data.last().toInt() xor 1).toByte()
        assertFails { Backup.decrypt(data,"a-long-password".toCharArray()) }
    }
    @Test fun invalidNumericInputAndDuplicateActiveSessionsAreRejected() {
        assertFalse(validSet(SetResult(exerciseId="rdl",setIndex=0,weightKg=Double.NaN)))
        val s=Session(plan=listOf(PlannedExercise("rdl")))
        assertFails { AppState(sessions=listOf(s,s.copy(id="other"))).validated() }
    }
    @Test fun losingVisibilityNeverAddsRepsAndPausesHold() {
        val engine=PoseEngine("plank")
        val output=engine.update(PoseFrame(1000,List(33) { Joint(.5f,.5f,0f) }))
        assertFalse(output.tracking); assertEquals(0,output.reps); assertEquals(0,output.holdSeconds)
        assertFalse(engine.pause().tracking)
    }
    @Test fun aspectRatioCorrectsJointAngles() {
        assertEquals(90.0,PoseEngine.angle(Joint(0f,0f),Joint(.5f,0f),Joint(.5f,.5f),2f),.001)
    }
}
