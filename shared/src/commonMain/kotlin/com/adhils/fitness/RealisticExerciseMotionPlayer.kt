package com.adhils.fitness

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    var currentFrame by remember(exerciseId) { mutableIntStateOf(0) }
    var isPlaying by remember(exerciseId) { mutableStateOf(true) }
    var frame0 by remember(exerciseId) { mutableStateOf(ExerciseImageLoader.getCachedFrame(exerciseId, 0)) }
    var frame1 by remember(exerciseId) { mutableStateOf(ExerciseImageLoader.getCachedFrame(exerciseId, 1)) }

    LaunchedEffect(exerciseId) {
        if (frame0 == null) frame0 = ExerciseImageLoader.loadExerciseFrame(exerciseId, 0)
        if (frame1 == null) frame1 = ExerciseImageLoader.loadExerciseFrame(exerciseId, 1)
    }

    LaunchedEffect(exerciseId, isPlaying, speed) {
        if (isPlaying) {
            val delayMs = (1100 / speed).toLong().coerceIn(350, 2500)
            while (true) {
                delay(delayMs)
                currentFrame = if (currentFrame == 0) 1 else 0
            }
        }
    }

    val activeBitmap = if (currentFrame == 0) frame0 ?: frame1 else frame1 ?: frame0

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(290.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF14151C))
            .clickable { isPlaying = !isPlaying },
        contentAlignment = Alignment.Center
    ) {
        if (activeBitmap != null) {
            Image(
                bitmap = activeBitmap,
                contentDescription = "Realistic Exercise Movement Demonstration",
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
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFFFF2D55)
                    )
                    Text("Loading realistic human demonstration…", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8E8E93))
                }
            }
        }

        // Left angle / frame thumbnails (matching Image 1 & Image 3)
        if (showThumbnails) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Thumbnail 0 (Start Position)
                val thumb0 = frame0 ?: activeBitmap
                Surface(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            currentFrame = 0
                            isPlaying = false
                            onFrameSelected?.invoke(0)
                        },
                    shape = RoundedCornerShape(8.dp),
                    border = if (currentFrame == 0) androidx.compose.foundation.BorderStroke(2.dp, Color.White)
                             else androidx.compose.foundation.BorderStroke(1.dp, Color(0x66FFFFFF)),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    if (thumb0 != null) {
                        Image(
                            bitmap = thumb0,
                            contentDescription = "Position 1",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Thumbnail 1 (Peak Contraction)
                val thumb1 = frame1 ?: activeBitmap
                Surface(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            currentFrame = 1
                            isPlaying = false
                            onFrameSelected?.invoke(1)
                        },
                    shape = RoundedCornerShape(8.dp),
                    border = if (currentFrame == 1) androidx.compose.foundation.BorderStroke(2.dp, Color.White)
                             else androidx.compose.foundation.BorderStroke(1.dp, Color(0x66FFFFFF)),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    if (thumb1 != null) {
                        Image(
                            bitmap = thumb1,
                            contentDescription = "Position 2",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Bottom right playback speed indicator pill
        if (showSpeedBadge) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 12.dp),
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
                        "${speed} x",
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
