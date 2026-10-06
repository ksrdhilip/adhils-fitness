package com.adhils.fitness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class HealthRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HealthRationaleScreen(onClose = { finish() })
        }
    }
}

@Composable
fun HealthRationaleScreen(onClose: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF101016)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Samsung Health & Health Connect",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "How ADhils Fitness uses your health and wearable data:",
                fontSize = 15.sp,
                color = Color(0xFF8E8E93)
            )

            RationaleCard(
                icon = Icons.Default.Watch,
                title = "Galaxy Watch & Wearable Vitals",
                description = "Reads live and resting heart rate, active calorie burning, and daily steps to accurately measure exertion and recovery during strength workouts."
            )

            RationaleCard(
                icon = Icons.Default.Favorite,
                title = "Body Stats & Personalization",
                description = "Pulls your latest height, weight, and body composition from Samsung Health to automatically calibrate exercise loads, target volume, and 1RM progressions."
            )

            RationaleCard(
                icon = Icons.Default.Lock,
                title = "Private & Local to Your Device",
                description = "Your health metrics stay on your phone and watch. Completed workouts are exported back to Samsung Health so your daily log stays unified."
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF2D55),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Close & Return",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun RationaleCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C24))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFFF2D55),
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = description,
                    color = Color(0xFFB0B0BA),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
