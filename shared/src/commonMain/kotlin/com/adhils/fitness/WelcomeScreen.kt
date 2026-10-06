package com.adhils.fitness

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Welcome / Splash Get-Started Screen.
 * Features an energetic athletic visual hero with cinematic gradient overlay,
 * bold "Train Smarter. Live Better." branding, and START / Log In action buttons.
 */
@Composable
fun WelcomeScreen(
    onStart: () -> Unit,
    onLogIn: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Cinematic Background Visual with Exercise Silhouette & Pulse Effect
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(2400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
        val barbellSweep by infiniteTransition.animateFloat(
            initialValue = -30f,
            targetValue = 30f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "barbellSweep"
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.68f)
        ) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h * 0.42f

            // Radial energetic backdrop glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFF2D55).copy(alpha = 0.22f * pulseAlpha),
                        Color(0xFF1F202B).copy(alpha = 0.4f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = w * 0.7f
                )
            )

            // Stylized Athletic Hero Lifter / Barbells Silhouette
            // Head
            drawCircle(Color(0xFF2E3140), radius = 24f, center = Offset(cx, cy - 80f))
            // Broad muscular torso & shoulders
            val torso = Path().apply {
                moveTo(cx - 65f, cy - 45f)
                cubicTo(cx - 75f, cy, cx - 45f, cy + 85f, cx, cy + 95f)
                cubicTo(cx + 45f, cy + 85f, cx + 75f, cy, cx + 65f, cy - 45f)
                close()
            }
            drawPath(torso, Color(0xFF252735), style = Fill)
            drawPath(torso, Color(0xFF14151C), style = Stroke(width = 2f))

            // Powerful chest & core division lines
            drawLine(Color(0xFF14151C), Offset(cx, cy - 40f), Offset(cx, cy + 50f), strokeWidth = 2.5f)
            drawLine(Color(0xFF14151C), Offset(cx - 50f, cy - 10f), Offset(cx + 50f, cy - 10f), strokeWidth = 2f)

            // Heavy Olympic Barbell Plate (held dynamically)
            val plateX = cx
            val plateY = cy + 20f
            // Left weight plate
            drawRoundRect(
                Color(0xFF1A1B22),
                topLeft = Offset(cx - 150f, cy - 65f),
                size = androidx.compose.ui.geometry.Size(32f, 130f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
            )
            drawRoundRect(
                Color(0xFFFF2D55).copy(alpha = 0.7f),
                topLeft = Offset(cx - 150f, cy - 65f),
                size = androidx.compose.ui.geometry.Size(6f, 130f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
            )
            // Right weight plate
            drawRoundRect(
                Color(0xFF1A1B22),
                topLeft = Offset(cx + 118f, cy - 65f),
                size = androidx.compose.ui.geometry.Size(32f, 130f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
            )
            drawRoundRect(
                Color(0xFFFF2D55).copy(alpha = 0.7f),
                topLeft = Offset(cx + 144f, cy - 65f),
                size = androidx.compose.ui.geometry.Size(6f, 130f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
            )
            // Barbell Steel Shaft
            drawLine(
                Color(0xFF484B5E),
                Offset(cx - 170f, cy),
                Offset(cx + 170f, cy),
                strokeWidth = 10f
            )

            // Powerful arm grips
            drawCircle(Color(0xFF383B4C), radius = 18f, center = Offset(cx - 85f, cy))
            drawCircle(Color(0xFF383B4C), radius = 18f, center = Offset(cx + 85f, cy))
        }

        // 2. Dark Cinematic Vignette Fade into Pitch-Black Bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x99000000),
                            Color(0xE6000000),
                            Color.Black,
                            Color.Black
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // 3. Foreground Content & Actions (Bottom aligned)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Brand Name Tag
            Text(
                "ADHILS FITNESS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                letterSpacing = 2.5.sp,
                color = Color.White.copy(alpha = 0.95f)
            )

            // Hero Tagline (Image 1 reference)
            Text(
                "Train Smarter.\nLive Better.",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = Color.White,
                lineHeight = 42.sp
            )

            Text(
                "Intelligent personalized workouts, anatomical recovery heat map, and live AI camera form coaching.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF8E8E93),
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(8.dp))

            // Primary Button: START (Crimson Pill)
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF2D55),
                    contentColor = Color.White
                )
            ) {
                Text(
                    "START",
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleMedium,
                    letterSpacing = 1.2.sp
                )
            }

            // Secondary Button: Log In (Dark Elevated Pill)
            Surface(
                onClick = onLogIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFF1C1C1E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2C2E))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "Log In",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Text(
                "Local AI Companion Mode · Privacy-First Offline Architecture",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF636366),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
        }
    }
}
