package com.adhils.fitness

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adhils.fitness.core.Catalog
import com.adhils.fitness.core.Exercise

@Composable
fun HowToScreen(
    exerciseId: String,
    onBack: () -> Unit,
    youtubeUrl: String? = null,
    initialTab: Int = 0
) {
    val e = Catalog.get(exerciseId)
    val details = remember(e.id) { ExerciseCatalogMetadata.getDetails(e) }
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) } // 0: Instructions, 1: Target, 2: Equipment
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Back Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = Color(0xFF1E202B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2D3A))
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Realistic Human Video / Motion Frame
        RealisticExerciseMotionPlayer(
            exerciseId = e.id,
            modifier = Modifier.fillMaxWidth(),
            showThumbnails = true,
            showSpeedBadge = true,
            speed = 1.0f
        )

        // Exercise Title & Aliases (Bold Italic matching screenshot)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = e.name,
                style = MaterialTheme.typography.headlineMedium,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Black,
                color = Color.White,
                fontSize = 28.sp
            )
            if (details.aliases.isNotEmpty()) {
                Text(
                    text = "Also called: " + details.aliases.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF8E8E93)
                )
            }
        }

        // Segmented Pill Tab Bar [ Instructions | Target | Equipment ]
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1C1D26)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("Instructions", "Target", "Equipment").forEachIndexed { index, label ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color(0xFF383B4A) else Color.Transparent)
                            .clickable { selectedTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF8E8E93)
                        )
                    }
                }
            }
        }

        // Tab Content
        when (selectedTab) {
            0 -> {
                // INSTRUCTIONS TAB
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    details.instructionParagraphs.forEach { paragraph ->
                        Text(
                            text = paragraph,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFFE0E0E6),
                            lineHeight = 24.sp
                        )
                    }

                    if (e.breathing.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF181A22),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282B38)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                SmallLabel("BREATHING PATTERN")
                                Text(
                                    text = e.breathing,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFCCD0DF)
                                )
                            }
                        }
                    }

                    // YouTube full tutorial button
                    val targetUrl = youtubeUrl ?: youtubePostureUrl(e, null)
                    OutlinedButton(
                        onClick = {
                            runCatching { uriHandler.openUri(targetUrl) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OndemandVideo,
                            contentDescription = "Watch tutorial on YouTube",
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFFFF3B30)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Watch full tutorial with audio on YouTube",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            1 -> {
                // TARGET TAB
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Primary Muscles
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Primary",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF8E8E93)
                        )
                        details.primaryMuscles.forEach { muscle ->
                            MuscleTargetItem(muscle = muscle)
                        }
                    }

                    // Secondary Muscles
                    if (details.secondaryMuscles.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Secondary",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF8E8E93)
                            )
                            details.secondaryMuscles.forEach { muscle ->
                                MuscleTargetItem(muscle = muscle)
                            }
                        }
                    }
                }
            }
            2 -> {
                // EQUIPMENT TAB
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = details.equipmentNeeded,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF181A22),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282B38)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = if (e.equipment == "Bodyweight") "Bodyweight Resistance" else "${e.equipment} Setup",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (e.equipment == "Bodyweight") "Can be performed anywhere without any equipment."
                                    else "Ensure proper grip and secure collars if using adjustable weights.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF8E8E93)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MuscleTargetItem(muscle: String) {
    val isBack = muscle in listOf("Glutes", "Hamstrings", "Back", "Calves", "Triceps")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E202B),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3142))
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                drawAnatomicalBody(
                    isFront = !isBack,
                    recoveryMap = emptyMap(),
                    highlightedMuscle = muscle
                )
            }
        }

        Text(
            text = when (muscle) {
                "Quads" -> "Quadriceps"
                "Core" -> "Abs & Core"
                else -> muscle
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
