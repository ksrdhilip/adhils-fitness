package com.adhils.fitness.core

import kotlin.test.*

/** Synthetic geometry validates state transitions, not accuracy on real human motion. */
class PoseEngineTest {
    private fun pose(exercise:String,turned:Boolean=false):List<Joint> {
        val p=MutableList(33) {Joint(.5f,.5f)}
        fun set(i:Int,x:Float,y:Float) {p[i]=Joint(x,y)}
        if(exercise=="press") {
            set(11,.35f,.3f);set(12,.65f,.3f);set(23,.4f,.6f);set(24,.6f,.6f)
            set(25,.4f,.75f);set(26,.6f,.75f);set(27,.4f,.9f);set(28,.6f,.9f)
            if(turned) {set(13,.35f,.2f);set(15,.35f,.07f);set(14,.65f,.2f);set(16,.65f,.07f)}
            else {set(13,.25f,.3f);set(15,.25f,.15f);set(14,.75f,.3f);set(16,.75f,.15f)}
        } else {
            set(11,.5f,.2f);set(13,.6f,.3f);set(15,.65f,.5f);set(23,.5f,.5f);set(25,.5f,.7f);set(27,.5f,.9f)
            if(exercise=="squat" && turned) {set(23,.3f,.68f);set(25,.55f,.65f);set(27,.55f,.9f)}
            if(exercise=="rdl" && turned) set(11,.8f,.5f)
            if(exercise=="pushup" || exercise=="plank") {
                val y=if(turned) .55f else .4f
                set(11,.3f,y);set(23,.65f,y);set(25,.8f,y);set(27,.92f,y)
                set(13,if(turned) .45f else .3f,if(turned) .65f else .6f);set(15,.3f,.8f)
            }
            listOf(11 to 12,13 to 14,15 to 16,23 to 24,25 to 26,27 to 28).forEach {(a,b)->p[b]=p[a].copy(x=p[a].x+.01f)}
        }
        return p
    }
    @Test fun completeCycleCountsOnceForEveryRepExercise() {
        listOf("squat","rdl","press","pushup").forEach {exercise->
            val engine=PoseEngine(exercise)
            var result=PoseObservation()
            for(t in 0L..4000L step 100L) result=engine.update(PoseFrame(t,pose(exercise,t in 1500L..2400L)))
            assertTrue(result.tracking,exercise);assertEquals(1,result.reps,exercise)
        }
    }
    @Test fun partialMotionAndLostTrackingDoNotCompleteARep() {
        val engine=PoseEngine("squat")
        for(t in 0L..2200L step 100L) engine.update(PoseFrame(t,pose("squat",t>=1500)))
        engine.update(PoseFrame(2300,emptyList()))
        var result=PoseObservation()
        for(t in 2400L..4000L step 100L) result=engine.update(PoseFrame(t,pose("squat")))
        assertEquals(0,result.reps)
    }
    @Test fun pauseDoesNotBridgeRepPhases() {
        val engine=PoseEngine("press")
        for(t in 0L..2200L step 100L) engine.update(PoseFrame(t,pose("press",t>=1500)))
        engine.pause()
        var result=PoseObservation()
        for(t in 2400L..4000L step 100L) result=engine.update(PoseFrame(t,pose("press")))
        assertEquals(0,result.reps)
    }
    @Test fun plankCountsOnlyContinuousVisibleIntervals() {
        val engine=PoseEngine("plank")
        var result=PoseObservation()
        for(t in 0L..2700L step 100L) result=engine.update(PoseFrame(t,pose("plank")))
        assertEquals(2,result.holdSeconds)
        val missing=engine.update(PoseFrame(5000,emptyList()))
        assertEquals(2,missing.holdSeconds)
        engine.pause()
        for(t in 6000L..6800L step 100L) result=engine.update(PoseFrame(t,pose("plank")))
        assertEquals(2,result.holdSeconds)
    }
    @Test fun wrongViewAndNonfiniteGeometryPauseTracking() {
        val engine=PoseEngine("press")
        assertFalse(engine.update(PoseFrame(1000,pose("squat"))).tracking)
        assertFalse(engine.update(PoseFrame(2000,pose("press"),Float.NaN)).tracking)
    }
    @Test fun movementCuesRequireOptInAndPersistentGeometry() {
        val points=pose("plank").toMutableList().apply {this[23]=Joint(.65f,.6f);this[24]=Joint(.66f,.6f)}
        for(enabled in listOf(false,true)) {
            val engine=PoseEngine("plank",enabled);val cues=mutableListOf<String>()
            for(t in 0L..3000L step 100L) engine.update(PoseFrame(t,points)).cue?.let(cues::add)
            if(enabled) assertEquals(listOf("Check your hip position"),cues) else assertTrue(cues.isEmpty())
        }
    }
    @Test fun fastLoweringCueSurvivesLongEnoughForPersistenceFilter() {
        val engine=PoseEngine("squat",true);val cues=mutableListOf<String>()
        for(t in 0L..3000L step 100L) engine.update(PoseFrame(t,pose("squat",t>=1000))).cue?.let(cues::add)
        assertContains(cues,"Slow the lowering phase")
    }
}
