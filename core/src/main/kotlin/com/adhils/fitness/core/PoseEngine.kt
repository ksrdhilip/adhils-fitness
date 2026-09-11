package com.adhils.fitness.core
import kotlin.math.*

data class Joint(val x: Float, val y: Float, val visibility: Float = 1f)
data class PoseFrame(val timeMs: Long, val joints: List<Joint>, val aspectRatio: Float = 1f)
data class PoseObservation(val reps: Int = 0, val holdSeconds: Int = 0, val tracking: Boolean = false,
    val status: String = "Position your whole body in view", val cue: String? = null,
    val joints: List<Joint> = emptyList(), val phase: String = "Ready")

/** Experimental 2D estimates. No spine, injury, or load-safety inference.
 * Movement cues are OFF by default until device/real-motion validation. */
class PoseEngine(private val exercise: String, private val movementCues: Boolean = false) {
    private var filtered: List<Joint> = emptyList()
    private var previousTime = -1L
    private var lastGood = -1L
    private var stableSince = -1L
    private var candidate = ""
    private var candidateSince = 0L
    private var armed = false
    private var turned = false
    private var cycleStart = 0L
    private var count = 0
    private var holdMs = 0L
    private var lastCue = -10000L
    private var pendingCue: String? = null
    private var pendingSince = 0L
    private var tempoCueUntil = 0L
    private var latest = PoseObservation()
    fun pause(): PoseObservation {
        armed=false; turned=false; stableSince=-1; previousTime=-1; filtered=emptyList(); candidate=""; pendingCue=null
        tempoCueUntil=0
        latest=latest.copy(tracking=false,status="Tracking paused",cue=null,joints=emptyList())
        return latest
    }
    fun update(frame: PoseFrame): PoseObservation {
        val t=frame.timeMs
        if(t<=previousTime) return latest
        val dt=if(previousTime<0) 0 else t-previousTime
        previousTime=t
        if(dt>500) { armed=false; turned=false; stableSince=-1; filtered=emptyList() }
        fun missing(reason:String):PoseObservation {
            armed=false; turned=false; stableSince=-1; candidate=""; filtered=emptyList(); pendingCue=null
            tempoCueUntil=0
            latest=PoseObservation(count,(holdMs/1000).toInt(),false,reason,phase="Paused")
            return latest
        }
        val points=frame.joints
        if(points.size!=33 || !frame.aspectRatio.isFinite() || frame.aspectRatio<=0) return missing("Move into view")
        if(points.any { !it.x.isFinite() || !it.y.isFinite() }) return missing("Tracking unavailable")
        val left=listOf(11,13,15,23,25,27); val right=listOf(12,14,16,24,26,28)
        val side=if(left.sumOf { points[it].visibility.toDouble() }>=right.sumOf { points[it].visibility.toDouble() }) left else right
        val required=if(exercise=="press") left+right else side
        if(required.any { points[it].visibility<.65f }) return missing("Keep shoulders, hands, hips and feet visible")
        if(required.any { points[it].x !in .025f.. .975f || points[it].y !in .025f.. .975f })
            return missing("Move back to fit your whole body")
        filtered=if(filtered.size==33) points.mapIndexed { i,p ->
            Joint(filtered[i].x*.4f+p.x*.6f,filtered[i].y*.4f+p.y*.6f,p.visibility) } else points
        val p=filtered
        fun length(a:Int,b:Int)=hypot((p[a].x-p[b].x)*frame.aspectRatio,p[a].y-p[b].y)
        val shoulder=side[0]; val elbow=side[1]; val wrist=side[2]
        val hip=side[3]; val knee=side[4]; val ankle=side[5]
        val torso=length(shoulder,hip)
        if(torso<.065f) return missing("Move closer while keeping your whole body visible")
        val width=length(11,12)/torso
        if(exercise=="press" && width<.5f) return missing("Face the camera for shoulder press")
        if(exercise!="press" && width>.85f && points[11].visibility>.75f && points[12].visibility>.75f)
            return missing("Turn sideways to the camera")
        val horizontal=abs(p[shoulder].x-p[hip].x)*frame.aspectRatio>abs(p[shoulder].y-p[hip].y)
        if((exercise=="pushup" || exercise=="plank") && !horizontal)
            return missing("Use the floor position shown in the guide")
        if(stableSince<0) stableSince=t
        if(t-stableSince<600) {
            latest=PoseObservation(count,(holdMs/1000).toInt(),false,"Hold position while tracking settles",joints=p)
            return latest
        }
        fun jointAngle(a:Int,b:Int,c:Int)=angle(p[a],p[b],p[c],frame.aspectRatio)
        var cue:String?=null
        val phase:String
        if(exercise=="plank") {
            if(dt in 1..250 && lastGood==t-dt) holdMs+=dt
            if(jointAngle(shoulder,hip,ankle)<155) cue="Check your hip position"
            phase="Holding"
        } else {
            val a=when(exercise) {
                "squat" -> jointAngle(hip,knee,ankle)
                "rdl" -> jointAngle(shoulder,hip,knee)
                else -> jointAngle(shoulder,elbow,wrist)
            }
            val top=if(exercise=="press") a>150 && p[wrist].y<p[shoulder].y else a>155
            val bottom=a<when(exercise) { "squat","rdl" -> 115; "press" -> 105; else -> 100 }
            val current=if(top) "Top" else if(bottom) "Bottom" else "Moving"
            if(current!=candidate) { candidate=current; candidateSince=t }
            val sustained=t-candidateSince>=140
            val start=if(exercise=="press") "Bottom" else "Top"
            val turn=if(exercise=="press") "Top" else "Bottom"
            if(sustained && current==start) {
                if(!armed) { armed=true; cycleStart=t }
                else if(turned) {
                    if(t-cycleStart in 700..30000) count++
                    turned=false; cycleStart=t
                }
            }
            if(sustained && armed && current==turn && !turned) {
                turned=true
                if(t-cycleStart<900 && exercise!="press") tempoCueUntil=t+1500
            }
            if(armed && t-cycleStart>30000) { armed=false; turned=false }
            phase=current
            if(exercise=="pushup" && jointAngle(shoulder,hip,ankle)<150) cue="Check your hip position"
            if(exercise=="press" && abs(jointAngle(12,14,16)-jointAngle(11,13,15))>28)
                cue="Check whether both arms move together"
        }
        lastGood=t
        if(cue==null && t<tempoCueUntil) cue="Slow the lowering phase"
        if(cue!=pendingCue) { pendingCue=cue; pendingSince=t }
        val spoken=if(movementCues && cue!=null && t-pendingSince>=650 && t-lastCue>5000) { lastCue=t; cue } else null
        latest=PoseObservation(count,(holdMs/1000).toInt(),true,"Tracking · $phase",spoken,p,phase)
        return latest
    }
    companion object {
        fun angle(a:Joint,b:Joint,c:Joint,aspect:Float=1f):Double {
            val ux=(a.x-b.x).toDouble()*aspect; val uy=(a.y-b.y).toDouble()
            val vx=(c.x-b.x).toDouble()*aspect; val vy=(c.y-b.y).toDouble()
            val denominator=hypot(ux,uy)*hypot(vx,vy)
            if(denominator<1e-8) return 180.0
            return Math.toDegrees(acos(((ux*vx+uy*vy)/denominator).coerceIn(-1.0,1.0)))
        }
    }
}
