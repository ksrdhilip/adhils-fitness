package com.adhils.fitness

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun RealisticExerciseMotionPlayer(
    exerciseId: String,
    modifier: Modifier = Modifier,
    showThumbnails: Boolean = true,
    showSpeedBadge: Boolean = true,
    speed: Float = 1.0f,
    onFrameSelected: ((Int) -> Unit)? = null
) {
    var isPlaying by remember(exerciseId) { mutableStateOf(true) }
    var currentSpeed by remember(exerciseId, speed) { mutableStateOf(speed) }

    val videoUrl = remember(exerciseId) { ExerciseImageLoader.getVideoUrl(exerciseId) }
    val hasVideo = remember(exerciseId) { ExerciseImageLoader.hasVideo(exerciseId) }

    // Fallback 2-keyframe images
    var frame0 by remember(exerciseId) { mutableStateOf(ExerciseImageLoader.getCachedFrame(exerciseId, 0)) }
    var frame1 by remember(exerciseId) { mutableStateOf(ExerciseImageLoader.getCachedFrame(exerciseId, 1)) }
    var fallbackKeyframe by remember(exerciseId) { mutableIntStateOf(0) }
    var inspectingKeyframe by remember(exerciseId) { mutableStateOf<Int?>(null) }

    // Asynchronously load fallback thumbnails
    LaunchedEffect(exerciseId) {
        if (frame0 == null) frame0 = ExerciseImageLoader.loadExerciseFrame(exerciseId, 0)
        if (frame1 == null) frame1 = ExerciseImageLoader.loadExerciseFrame(exerciseId, 1)
    }

    // Fallback loop when only keyframes are available (and not video)
    LaunchedEffect(exerciseId, isPlaying, currentSpeed, hasVideo) {
        if (!hasVideo && isPlaying) {
            val keyframeDuration = (1200 / currentSpeed).toLong().coerceIn(600, 2500)
            while (true) {
                delay(keyframeDuration)
                fallbackKeyframe = if (fallbackKeyframe == 0) 1 else 0
            }
        }
    }

    val effectiveThumb0 = frame0
    val effectiveThumb1 = frame1 ?: frame0

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(290.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF14151C))
            .clickable {
                if (inspectingKeyframe != null) {
                    inspectingKeyframe = null
                    isPlaying = true
                } else {
                    isPlaying = !isPlaying
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // 1. Poster / base image (instant visual before video buffers)
        val posterBitmap = frame0 ?: frame1
        if (posterBitmap != null) {
            Image(
                bitmap = posterBitmap,
                contentDescription = "Exercise Poster",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Hardware-accelerated native looping video player (Fitbod quality)
        if (hasVideo && inspectingKeyframe == null) {
            PlatformVideoPlayer(
                videoUrl = videoUrl,
                modifier = Modifier.fillMaxSize(),
                isPlaying = isPlaying,
                speed = currentSpeed,
                contentDescription = "Realistic Exercise Video Demonstration"
            )
        } else if (inspectingKeyframe != null) {
            // When user taps a keyframe thumbnail, inspect that exact pose
            val inspectBitmap = if (inspectingKeyframe == 0) frame0 else (frame1 ?: frame0)
            if (inspectBitmap != null) {
                Image(
                    bitmap = inspectBitmap,
                    contentDescription = if (inspectingKeyframe == 0) "Start Position" else "Peak Position",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else if (!hasVideo) {
            // Fallback keyframe animation
            val displayFrame = if (fallbackKeyframe == 0) (frame0 ?: frame1) else (frame1 ?: frame0)
            if (displayFrame != null) {
                Image(
                    bitmap = displayFrame,
                    contentDescription = if (fallbackKeyframe == 0) "Start Position" else "Peak Position",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF202330), Color(0xFF13141B))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFFF2D55)
                        )
                        Text(
                            "Loading video demonstration…",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8E8E93)
                        )
                    }
                }
            }
        }

        // Center Pause / Resume Icon Indicator
        if (!isPlaying) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Resume Video",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Left angle / position thumbnails (matching Fitbod start and peak contraction positions)
        if (showThumbnails && (effectiveThumb0 != null || effectiveThumb1 != null)) {
            val isStartSelected = (inspectingKeyframe == 0) || (inspectingKeyframe == null && !isPlaying && fallbackKeyframe == 0)
            val isFinishSelected = (inspectingKeyframe == 1) || (inspectingKeyframe == null && !isPlaying && fallbackKeyframe == 1)

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Thumbnail 0 (Start Position)
                Surface(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            inspectingKeyframe = 0
                            isPlaying = false
                            onFrameSelected?.invoke(0)
                        },
                    shape = RoundedCornerShape(8.dp),
                    border = if (isStartSelected) androidx.compose.foundation.BorderStroke(2.dp, Color.White)
                             else androidx.compose.foundation.BorderStroke(1.dp, Color(0x66FFFFFF)),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    if (effectiveThumb0 != null) {
                        Image(
                            bitmap = effectiveThumb0,
                            contentDescription = "Start Position",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Thumbnail 1 (Peak Contraction / Finish Position)
                Surface(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            inspectingKeyframe = 1
                            isPlaying = false
                            onFrameSelected?.invoke(1)
                        },
                    shape = RoundedCornerShape(8.dp),
                    border = if (isFinishSelected) androidx.compose.foundation.BorderStroke(2.dp, Color.White)
                             else androidx.compose.foundation.BorderStroke(1.dp, Color(0x66FFFFFF)),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    if (effectiveThumb1 != null) {
                        Image(
                            bitmap = effectiveThumb1,
                            contentDescription = "Finish Position",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Bottom right playback speed indicator pill (clickable to adjust speed)
        if (showSpeedBadge) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 12.dp)
                    .clickable {
                        // Cycle speed: 0.75x -> 1.0x -> 1.25x -> 1.5x -> 0.75x
                        currentSpeed = when {
                            currentSpeed < 0.9f -> 1.0f
                            currentSpeed < 1.1f -> 1.25f
                            currentSpeed < 1.4f -> 1.5f
                            else -> 0.75f
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.65f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x44FFFFFF))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "${currentSpeed}x",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
