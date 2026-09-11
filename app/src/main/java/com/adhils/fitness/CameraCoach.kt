package com.adhils.fitness

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.adhils.fitness.core.*
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.delay

private class CameraProcessor(context:Context,private val exercise:Exercise,private val cues:Boolean,
    private val front:Boolean,private val deliver:(Bitmap,PoseObservation,Long)->Unit,private val error:(String)->Unit) : ImageAnalysis.Analyzer {
    private val setup=PoseEngine(exercise.camera!!)
    private var engine=PoseEngine(requireNotNull(exercise.camera),cues)
    private var last=PoseObservation()
    @Volatile var started=false
    @Volatile var paused=false
    @Volatile var reset=false
    private var lastAt=-1L
    private var nativeFailure=false
    private val detector=PoseLandmarker.createFromOptions(context,PoseLandmarker.PoseLandmarkerOptions.builder()
        .setBaseOptions(BaseOptions.builder().setModelAssetPath("pose_landmarker_lite.task").build())
        .setRunningMode(RunningMode.VIDEO).setNumPoses(1).setMinPoseDetectionConfidence(.65f)
        .setMinPosePresenceConfidence(.65f).setMinTrackingConfidence(.65f).build())
    override fun analyze(image:ImageProxy) {
        try {
            if(nativeFailure) return
            val at=SystemClock.uptimeMillis()
            if(at-lastAt<60) return
            lastAt=at
            val analyzed=analyzeCameraFrame(image.toBitmap(),image.imageInfo.rotationDegrees,front) {
                detector.detectForVideo(it,at)
            }
            val result=analyzed.result
            val joints=result.landmarks().firstOrNull()?.map {Joint(it.x(),it.y(),it.visibility().orElse(0f))} ?: emptyList()
            val frame=PoseFrame(at,joints,analyzed.preview.width.toFloat()/analyzed.preview.height)
            if(reset) {engine=PoseEngine(exercise.camera!!,cues);reset=false}
            val observation=when {
                !started -> setup.update(frame)
                paused -> {engine.pause();last.copy(tracking=false,status="Paused",cue=null,joints=emptyList())}
                else -> engine.update(frame).also {last=it}
            }
            // Display the exact frame analyzed, so landmarks and image cannot drift.
            val shown=observation.copy(joints=if(front) observation.joints.map {it.copy(x=1f-it.x)} else observation.joints)
            deliver(analyzed.preview,shown,SystemClock.uptimeMillis()-at)
        } catch(e:Exception) {error(e.message ?: "Camera analysis failed")}
        catch(e:LinkageError) {nativeFailure=true;error("Camera tracking could not load on this device. Continue with manual logging.")}
        finally {image.close()}
    }
    fun close() {detector.close()}
}

@Composable fun CameraCoachScreen(exercise:Exercise,profile:Profile,onBack:()->Unit,onEnd:(Int,Int,List<String>)->Unit) {
    val context=LocalContext.current
    val owner=LocalLifecycleOwner.current
    // Google's 0.10.26 vision AAR ships an ARM64 library only.
    val supported=android.os.Build.SUPPORTED_ABIS.firstOrNull()=="arm64-v8a"
    var permission by remember {mutableStateOf(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)}
    val askPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {permission=it}
    var front by remember {mutableStateOf(false)}
    var bitmap by remember {mutableStateOf<Bitmap?>(null)}
    var observation by remember {mutableStateOf(PoseObservation())}
    var failure by remember {mutableStateOf<String?>(null)}
    var processor by remember {mutableStateOf<CameraProcessor?>(null)}
    var started by remember {mutableStateOf(false)}
    var paused by remember {mutableStateOf(false)}
    var countingDown by remember {mutableIntStateOf(0)}
    var latency by remember {mutableLongStateOf(0)}
    var visibleCue by remember {mutableStateOf<String?>(null)}
    val observations=remember {linkedSetOf<String>()}
    var tts by remember {mutableStateOf<TextToSpeech?>(null)}
    DisposableEffect(Unit) {
        var voice:TextToSpeech?=null
        voice=TextToSpeech(context) {status->if(status==TextToSpeech.SUCCESS) voice?.language=Locale.US}
        tts=voice
        onDispose {voice?.stop();voice?.shutdown()}
    }
    DisposableEffect(owner) {
        val listener=LifecycleEventObserver {_,event->if(event==Lifecycle.Event.ON_STOP) {
            paused=true;processor?.paused=true;countingDown=0;tts?.stop()
        }}
        owner.lifecycle.addObserver(listener)
        onDispose {owner.lifecycle.removeObserver(listener)}
    }
    DisposableEffect(permission,front) {
        val disposed=AtomicBoolean(false)
        val executor=Executors.newSingleThreadExecutor()
        val main=ContextCompat.getMainExecutor(context)
        var cameraProvider:ProcessCameraProvider?=null
        var analyzer:CameraProcessor?=null
        fun report(message:String) {main.execute {if(!disposed.get()) failure=message}}
        if(permission && supported) {
            val future=ProcessCameraProvider.getInstance(context)
            future.addListener({
                if(!disposed.get()) try {
                    val provider=future.get();cameraProvider=provider
                    // Load the native library/model away from the UI thread. Cleanup is
                    // queued on this same executor, including when setup is interrupted.
                    executor.execute {
                        if(!disposed.get()) try {
                            val created=CameraProcessor(context,exercise,profile.experimentalCues,front,{frame,obs,ms->
                                main.execute {
                                    if(!disposed.get()) {bitmap=frame;observation=obs;latency=ms
                                        if(started && !paused && !obs.tracking) observations.add("Tracking was interrupted; review the count.")
                                        obs.cue?.let {cue->visibleCue=cue;observations.add(cue);if(profile.voice && started && !paused) tts?.speak(cue,TextToSpeech.QUEUE_FLUSH,null,"movement")}
                                    }
                                }
                            },::report)
                            analyzer=created
                            main.execute {
                                if(!disposed.get()) try {
                                    processor=created
                                    val useCase=ImageAnalysis.Builder().setTargetResolution(android.util.Size(640,480))
                                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                                    useCase.setAnalyzer(executor,created)
                                    provider.bindToLifecycle(owner,if(front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA,useCase)
                                } catch(e:Exception) {report(e.message ?: "Could not start camera")}
                            }
                        } catch(e:Exception) {report(e.message ?: "Could not load camera tracking")}
                        catch(e:LinkageError) {report("Camera tracking could not load on this device. Continue with manual logging.")}
                    }
                } catch(e:Exception) {failure=e.message ?: "Could not start camera"}
            },main)
        }
        onDispose {disposed.set(true);cameraProvider?.unbindAll();processor=null;executor.execute {analyzer?.close()};executor.shutdown()}
    }
    LaunchedEffect(countingDown) {
        if(countingDown>0) {delay(1000);if(countingDown>0) {if(countingDown==1) {processor?.reset=true;processor?.started=true;processor?.paused=false;started=true;paused=false};countingDown--}}
    }
    LaunchedEffect(visibleCue) {if(visibleCue!=null) {delay(4000);visibleCue=null}}
    fun finishOrBack() {
        processor?.paused=true;tts?.stop()
        if(started) onEnd(observation.reps,observation.holdSeconds,observations.toList()) else onBack()
    }
    BackHandler {finishOrBack()}
    ScreenHeader("Camera coach",::finishOrBack)
    SectionTitle(exercise.name,if(profile.experimentalCues) "Movement cues · preview" else "Rep tracking · preview")
    if(!supported) {
        EmptyState("Use your Samsung device", "Live camera tracking is available in the ARM64 build for your S22 and Tab A9+. This emulator supports workout logging and screen previews.")
        PrimaryButton("Continue with manual logging",onClick=onBack)
    } else if(!permission) {
        EmptyState("Use your camera","Camera access is used only for on-device movement tracking.")
        PrimaryButton("Enable camera") {askPermission.launch(Manifest.permission.CAMERA)}
        OutlinedButton(onClick=onBack,modifier=Modifier.fillMaxWidth()) {Text("Continue without camera")}
    } else {
        val frame=bitmap
        Box(Modifier.fillMaxWidth().heightIn(max=440.dp).aspectRatio(frame?.let {it.width.toFloat()/it.height} ?: .75f).background(Color(0xFF17221C)),Alignment.Center) {
            if(frame!=null) Image(frame.asImageBitmap(),"Live camera preview",Modifier.fillMaxSize(),contentScale=ContentScale.FillBounds)
            else Text("Opening camera…")
            val joints=observation.joints
            if(joints.size==33) Canvas(Modifier.fillMaxSize()) {
                val edges=listOf(11 to 12,11 to 13,13 to 15,12 to 14,14 to 16,11 to 23,12 to 24,23 to 24,23 to 25,25 to 27,24 to 26,26 to 28)
                fun point(i:Int)=Offset(joints[i].x*size.width,joints[i].y*size.height)
                edges.filter {(a,b)->joints[a].visibility>.65f && joints[b].visibility>.65f}.forEach {(a,b)->drawLine(Mint,point(a),point(b),3.dp.toPx())}
                edges.flatMap {listOf(it.first,it.second)}.distinct().filter {joints[it].visibility>.65f}.forEach {drawCircle(Mint,4.dp.toPx(),point(it))}
            }
            if(countingDown>0) Text(countingDown.toString(),fontSize=64.sp,color=Mint)
        }
        failure?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        if(!started) {
            Text("${exercise.view} view · Keep your whole body visible. Set your device on a stable support.")
            Text(observation.status)
            Row {TextButton(onClick={front=!front;failure=null;bitmap=null}) {Text("Flip camera")};TextButton(onClick=onBack) {Text("Use manual logging")}}
            PrimaryButton("Start tracked set",observation.tracking && failure==null && countingDown==0) {countingDown=3}
        } else {
            Text(if(exercise.timed) durationText(observation.holdSeconds*1000L) else observation.reps.toString(),style=MaterialTheme.typography.displayLarge,color=MaterialTheme.colorScheme.primary)
            SmallLabel(if(exercise.timed) "TRACKED HOLD TIME" else "ESTIMATED REPS")
            PanelCard {Text(if(paused || !observation.tracking) observation.status else visibleCue ?: observation.status)}
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick={paused=!paused;processor?.paused=paused;tts?.stop()},modifier=Modifier.weight(1f)) {Text(if(paused) "Resume" else "Pause")}
                Button(onClick={processor?.paused=true;tts?.stop();onEnd(observation.reps,observation.holdSeconds,observations.toList())},modifier=Modifier.weight(1f)) {Text("End set")}
            }
        }
        SmallLabel("Video stays on your device · ${latency}ms analysis")
        SmallLabel("Counts are editable. Camera measurements do not assess spinal alignment or safe lifting loads.")
    }
}
