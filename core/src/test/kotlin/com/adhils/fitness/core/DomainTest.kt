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
        val ramped=AppState(sessions=listOf(Session(plan=listOf(plan),finishedAt=124,results=listOf(
            SetResult(exerciseId="rdl",setIndex=0,reps=10,weightKg=10.0,rpe=7),
            SetResult(exerciseId="rdl",setIndex=1,reps=10,weightKg=12.5,rpe=8),
            SetResult(exerciseId="rdl",setIndex=2,reps=10,weightKg=12.5,rpe=8)))))
        assertEquals(12.5,Training.nextLoad("rdl",ramped).weightKg)
    }
    @Test fun staleAndDestructiveAiChangesAreRejectedWhileValidExerciseUpdatesSucceed() {
        val session=Training.saveSet(Session(plan=listOf(PlannedExercise("rdl"))),
            SetResult(exerciseId="rdl",setIndex=2,reps=10))
        assertFails { CoachChanges.apply(session,CoachProposal(session.id,0,"shorter",changes=listOf(PlanChange("rdl",2)))) }
        assertFails { CoachChanges.apply(session,CoachProposal(session.id,session.revision,"shorter",changes=listOf(PlanChange("rdl",2)))) }
        assertFails { CoachChanges.apply(session,CoachProposal(session.id,session.revision,"invalid",changes=listOf(PlanChange("invented",3)))) }
        val expanded=CoachChanges.apply(session,CoachProposal(session.id,session.revision,"add exercises",changes=listOf(PlanChange("rdl",4),PlanChange("curl",3))))
        assertEquals(listOf("rdl","curl"),expanded.plan.map { it.exerciseId })
        assertEquals(4,expanded.plan.first().sets)
        val adjusted=Training.adjustTimers(expanded,"curl",exerciseDelta=-5,restDelta=5)
        assertEquals(40,adjusted.plan.last().seconds)
        assertEquals(50,adjusted.plan.last().restSeconds)
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
    @Test fun timedExerciseAccumulatesHoldSmoothly() {
        val engine = PoseEngine("timed")
        val joints = MutableList(33) { Joint(0.5f, 0.5f, 0.8f) }
        // First frame to initialize
        engine.update(PoseFrame(1000, joints))
        // After 250ms settling
        val obs1 = engine.update(PoseFrame(1300, joints))
        assertTrue(obs1.tracking)
        val obs2 = engine.update(PoseFrame(2300, joints))
        assertEquals(1, obs2.holdSeconds)
    }
    @Test fun aspectRatioCorrectsJointAngles() {
        assertEquals(90.0,PoseEngine.angle(Joint(0f,0f),Joint(.5f,0f),Joint(.5f,.5f),2f),.001)
    }
    @Test fun catalogAndAllFourWorkoutVariantsAreValid() {
        assertEquals(Catalog.exercises.size,Catalog.byId.size)
        assertTrue(Catalog.exercises.size>=60)
        assertTrue(Catalog.exercises.all { it.equipment in setOf("Bodyweight","Dumbbells","Bands","Bench") })
        assertTrue(Catalog.exercises.all { it.camera==null || it.camera in setOf("squat","rdl","press","pushup","plank","timed","curl") })
        assertTrue(Catalog.exercises.all { it.effectiveCamera in setOf("squat","rdl","press","pushup","plank","pull","timed","curl") })
        for(eq in listOf(setOf("Bodyweight"),setOf("Bodyweight","Dumbbells"),setOf("Bodyweight","Bands"),setOf("Bodyweight","Dumbbells","Bench"))) {
            var state=AppState(profile=Profile(equipment=eq))
            for(v in 0..3) {
                val generated=Training.generate(state,CheckIn(minutes=55))
                state=state.copy(sessions=state.sessions+generated.copy(finishedAt=1000L+v)).validated()
            }
        }
    }
    @Test fun recoveryEngine1RmPrsSupersetsAndTargetMusclesWork() {
        val now=System.currentTimeMillis()
        val pastSession=Session(
            id="past-1",
            plan=listOf(PlannedExercise("goblet-squat"),PlannedExercise("pushup")),
            startedAt=now-3_600_000L,
            finishedAt=now-1_800_000L,
            results=listOf(
                SetResult(exerciseId="goblet-squat",setIndex=0,reps=10,weightKg=20.0,rpe=9,savedAt=now-1_800_000L),
                SetResult(exerciseId="goblet-squat",setIndex=1,reps=10,weightKg=20.0,rpe=9,savedAt=now-1_800_000L),
                SetResult(exerciseId="goblet-squat",setIndex=2,reps=10,weightKg=20.0,rpe=9,savedAt=now-1_800_000L)
            )
        )
        val state=AppState(sessions=listOf(pastSession))
        val recovery=Recovery.calculate(state,now)
        val quads=recovery.first { it.muscle=="Quads" }
        val chest=recovery.first { it.muscle=="Chest" }
        assertTrue(quads.recoveryPercent<80)
        assertEquals(100,chest.recoveryPercent)
        assertTrue("Quads" !in Recovery.freshestMuscles(state,4,now))

        // Epley 1RM & PR detection
        val est1Rm=Recovery.estimate1RMKg(20.0,10)
        assertTrue(est1Rm>26.0)
        val prBadges=Recovery.detectPRs(state,"active-1",SetResult(exerciseId="goblet-squat",setIndex=0,reps=12,weightKg=25.0))
        assertTrue(prBadges.any { it.contains("Max Weight PR") })
        assertTrue(prBadges.any { it.contains("1RM PR") })

        // Target muscle multi-select & Supersets
        val supersetSession=Training.generate(state,CheckIn(minutes=35,focus="Fresh Muscle Groups",targetMuscles=setOf("Chest","Back"),supersets=true))
        assertTrue(supersetSession.plan.any { it.supersetGroup=="A" })
        val firstEx=supersetSession.plan.first { it.supersetGroup=="A" }
        val firstIdx=supersetSession.plan.indexOf(firstEx)
        val afterFirstSet=Training.saveSet(supersetSession.copy(currentExercise=firstIdx),SetResult(exerciseId=firstEx.exerciseId,setIndex=0,reps=10,weightKg=10.0))
        assertNotEquals(firstIdx,afterFirstSet.currentExercise)
    }

    @Test fun workoutTitlesUseDescriptiveAnatomyGroupsAndNeverRawABCD() {
        val state = AppState(profile = Profile(equipment = setOf("Bodyweight", "Dumbbells", "Bench")))

        // 1. Freshest muscle group title shouldn't have "ABCD"
        val freshSession = Training.generate(state, CheckIn(focus = "Fresh Muscle Groups"))
        assertFalse(freshSession.title.contains("Fresh Muscle Groups A"))
        assertFalse(freshSession.title.contains("Fresh Muscle Groups B"))
        assertFalse(freshSession.title.contains("Fresh Muscle Groups C"))
        assertFalse(freshSession.title.contains("Fresh Muscle Groups D"))
        assertTrue(freshSession.title.contains("Upper Body") || freshSession.title.contains("Lower Body") || freshSession.title.contains("Full Body") || freshSession.title.contains("Push") || freshSession.title.contains("Pull"))

        // 2. Full Body shouldn't have "Full Body A" etc
        val fullBodySession = Training.generate(state, CheckIn(focus = "Full Body"))
        assertFalse(fullBodySession.title.matches(Regex(".*Full Body [A-D].*")))
        assertTrue(fullBodySession.title.startsWith("Full Body ("))

        // 3. Explicit Target Muscles
        val chestSession = Training.generate(state, CheckIn(targetMuscles = setOf("Chest")))
        assertTrue(chestSession.title.startsWith("Chest Focus"))

        val backBicepsSession = Training.generate(state, CheckIn(targetMuscles = setOf("Back", "Biceps")))
        assertTrue(backBicepsSession.title.startsWith("Upper Body (Back & Biceps)"))

        val legSession = Training.generate(state, CheckIn(targetMuscles = setOf("Quads", "Glutes")))
        assertTrue(legSession.title.startsWith("Lower Body (Quads & Glutes)"))

        val fullBodyTarget = Training.generate(state, CheckIn(targetMuscles = setOf("Quads", "Chest")))
        assertTrue(fullBodyTarget.title.startsWith("Full Body (Quads & Chest)"))

        // 4. Focus splits
        val upperSession = Training.generate(state, CheckIn(focus = "Upper Body"))
        assertTrue(upperSession.title.startsWith("Upper Body") || upperSession.title.startsWith("Push") || upperSession.title.startsWith("Pull") || upperSession.title.contains("Shoulders"))

        val lowerSession = Training.generate(state, CheckIn(focus = "Lower Body"))
        assertTrue(lowerSession.title.startsWith("Lower Body"))

        val coreSession = Training.generate(state, CheckIn(focus = "Core & Mobility"))
        assertEquals("Core & Mobility", coreSession.title)
    }
}
