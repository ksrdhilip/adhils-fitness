package com.adhils.fitness

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.adhils.fitness.core.Joint
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun AndroidCameraPreview(
    modifier: Modifier,
    isFrontCamera: Boolean,
    onPermissionGranted: (Boolean) -> Unit,
    onPoseDetected: (List<Joint>) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        onPermissionGranted(granted)
    }

    LaunchedEffect(hasPermission) {
        onPermissionGranted(hasPermission)
    }

    if (!hasPermission) {
        Box(
            modifier = modifier.background(Color(0xFF141416)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Camera permission required", color = Color.White)
                Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant Permission")
                }
            }
        }
        return
    }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(isFrontCamera, hasPermission) {
        val executor = Executors.newSingleThreadExecutor()
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val disposed = AtomicBoolean(false)
        var cameraProvider: ProcessCameraProvider? = null
        var detector: PoseLandmarker? = null

        try {
            detector = PoseLandmarker.createFromOptions(
                context,
                PoseLandmarker.PoseLandmarkerOptions.builder()
                    .setBaseOptions(BaseOptions.builder().setModelAssetPath("pose_landmarker_lite.task").build())
                    .setRunningMode(RunningMode.VIDEO)
                    .setNumPoses(1)
                    .setMinPoseDetectionConfidence(0.5f)
                    .setMinPosePresenceConfidence(0.5f)
                    .setMinTrackingConfidence(0.5f)
                    .build()
            )
        } catch (_: Throwable) {}

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            if (disposed.get()) return@addListener
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(640, 480))
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                var lastAnalysisTime = 0L

                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    if (disposed.get() || detector == null) {
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    try {
                        val now = SystemClock.uptimeMillis()
                        // Run inference at ~25 FPS max
                        if (now - lastAnalysisTime >= 40) {
                            lastAnalysisTime = now
                            val rotation = imageProxy.imageInfo.rotationDegrees
                            val bmp = imageProxy.toBitmap()

                            val matrix = Matrix().apply {
                                if (rotation != 0) postRotate(rotation.toFloat())
                                if (isFrontCamera) postScale(-1f, 1f)
                            }

                            val orientedBmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                            val mpImage = BitmapImageBuilder(orientedBmp).build()
                            val result = detector?.detectForVideo(mpImage, now)

                            val landmarks = result?.landmarks()?.firstOrNull()
                            if (landmarks != null && landmarks.isNotEmpty()) {
                                val joints = landmarks.map { lm ->
                                    Joint(lm.x(), lm.y(), lm.visibility().orElse(0.85f))
                                }
                                mainExecutor.execute {
                                    if (!disposed.get()) onPoseDetected(joints)
                                }
                            } else {
                                mainExecutor.execute {
                                    if (!disposed.get()) onPoseDetected(emptyList())
                                }
                            }
                        }
                    } catch (_: Throwable) {
                    } finally {
                        imageProxy.close()
                    }
                }

                val cameraSelector = if (isFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
            } catch (_: Throwable) {}
        }, mainExecutor)

        onDispose {
            disposed.set(true)
            cameraProvider?.unbindAll()
            executor.execute {
                try {
                    detector?.close()
                } catch (_: Throwable) {}
            }
            executor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}
