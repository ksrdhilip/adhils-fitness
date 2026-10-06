package com.adhils.fitness

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.adhils.fitness.core.Profile

enum class GoalIconType { DUMBBELL, CHEVRON_UP, TARGET, CHEVRON_DOWN }

data class QuestionOption(
    val id: String,
    val title: String,
    val description: String? = null,
    val iconType: GoalIconType? = null
)

data class EquipmentItem(
    val id: String,
    val category: String, // "Small weights", "Bars & plates", "Benches & accessories"
    val name: String,
    val canEditWeights: Boolean = false,
    val thumbnailType: String = id
)

@Composable
fun OnboardingQuestionnaire(
    initialProfile: Profile = Profile(),
    initialStep: Int = 0,
    onFinish: (Profile) -> Unit,
    onBackToWelcome: () -> Unit
) {
    var step by remember { mutableStateOf(initialStep) }
    var selectedGoal by remember { mutableStateOf("Build muscle") }
    var selectedHabit by remember { mutableStateOf("I strength train consistently") }
    var selectedExperience by remember { mutableStateOf("Less than 1 year") }
    var selectedGymPreset by remember { mutableStateOf("At Home") }
    
    // Equipment set initialized based on location choice
    var selectedEquipment by remember { mutableStateOf(setOf("Dumbbells", "Bodyweight", "Resistance Bands")) }
    
    // Dumbbell weights list
    val defaultLbWeights = listOf(5.0, 10.0, 15.0, 20.0, 25.0, 30.0, 35.0, 40.0, 45.0, 50.0)
    var dumbbellWeights by remember { mutableStateOf(defaultLbWeights) }
    var showDumbbellEditor by remember { mutableStateOf(false) }

    // Schedule (Step 5)
    var scheduleType by remember { mutableStateOf("days_per_week") } // "days_per_week" or "specific_days"
    var selectedDaysCount by remember { mutableStateOf(3) }
    var selectedSpecificDays by remember { mutableStateOf(setOf("Monday", "Wednesday", "Friday")) }

    // Workout Previews (Step 6)
    var workoutPreviewEnabled by remember { mutableStateOf(true) }
    var workoutPreviewTime by remember { mutableStateOf("9:00 AM") }
    var showTimePicker by remember { mutableStateOf(false) }
    var showNotificationSettingsDialog by remember { mutableStateOf(false) }
    var notificationPermissionGranted by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var healthSyncEnabled by remember { mutableStateOf(false) }
    var isReadingHealthData by remember { mutableStateOf(false) }
    var selectedGender by remember { mutableStateOf("Male") }
    var birthDate by remember { mutableStateOf("1995-06-15") }
    var userHeightText by remember { mutableStateOf("5' 10\"") }
    var userWeightText by remember { mutableStateOf("165") }

    var athleteName by remember { mutableStateOf("") }

    val totalSteps = 10 // 0: Goal, 1: Habit, 2: Experience, 3: Location, 4: Equipment Review, 5: Schedule, 6: Previews, 7: Body Stats, 8: Name, 9: Your Program

    // Update equipment selection whenever location changes
    LaunchedEffect(selectedGymPreset) {
        selectedEquipment = when (selectedGymPreset) {
            "Large Gym" -> setOf("Dumbbells", "Barbells", "Weight Plates", "EZ Bar", "Flat / Incline Bench", "Squat Rack", "Cable Machine", "Pull-Up Bar", "Bodyweight")
            "Small Gym" -> setOf("Dumbbells", "Cable Machine", "Flat / Incline Bench", "Bodyweight")
            "Garage Gym" -> setOf("Dumbbells", "Barbells", "Weight Plates", "Flat / Incline Bench", "Squat Rack", "Bodyweight")
            "At Home" -> setOf("Dumbbells", "Resistance Bands", "Bodyweight")
            "Without Equipment" -> setOf("Bodyweight")
            else -> setOf("Dumbbells", "Bodyweight")
        }
    }

    fun createProfile(): Profile {
        val mappedGoal = when (selectedGoal) {
            "Lift heavier" -> "Get stronger"
            "Build muscle" -> "Build muscle"
            "Get lean and defined" -> "General fitness"
            "Lose weight" -> "Support weight management"
            else -> "Build muscle"
        }
        val mappedExp = when (selectedExperience) {
            "I am brand new to strength training" -> "Beginner"
            "Less than 1 year" -> "Beginner"
            "1-2 years" -> "Intermediate"
            "2-4 years" -> "Intermediate"
            "4+ years" -> "Advanced"
            else -> "Beginner"
        }
        val mappedPreset = when (selectedGymPreset) {
            "Large Gym", "Small Gym" -> "Commercial Gym"
            "Garage Gym", "At Home" -> "Home Gym"
            "Without Equipment" -> "Bodyweight / Travel"
            else -> "Custom"
        }
        val effectiveDays = if (scheduleType == "days_per_week") selectedDaysCount else selectedSpecificDays.size.coerceAtLeast(1)

        val heightInCm: Double = try {
            if (userHeightText.contains("'")) {
                val parts = userHeightText.replace("\"", "").split("'")
                val ft = parts[0].trim().toIntOrNull() ?: 5
                val inches = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 10
                ((ft * 12 + inches) * 2.54)
            } else {
                userHeightText.toDoubleOrNull() ?: 178.0
            }
        } catch (_: Exception) { 178.0 }

        val weightVal = userWeightText.toDoubleOrNull() ?: 165.0

        return initialProfile.copy(
            name = athleteName.trim().ifEmpty { "Athlete" },
            goal = mappedGoal,
            fitnessHabit = selectedHabit,
            rawExperience = selectedExperience,
            experience = mappedExp,
            gymPreset = mappedPreset,
            equipment = selectedEquipment,
            availableDumbbellWeights = dumbbellWeights.sorted(),
            days = effectiveDays.coerceIn(1, 7),
            scheduleType = scheduleType,
            specificDays = selectedSpecificDays,
            workoutPreviewEnabled = workoutPreviewEnabled,
            workoutPreviewTime = workoutPreviewTime,
            healthSyncEnabled = healthSyncEnabled,
            gender = selectedGender,
            birthDate = birthDate,
            heightCm = heightInCm,
            bodyWeight = weightVal,
            connectedWearable = if (healthSyncEnabled) PlatformHealth.defaultWearableModel else null,
            onboardingComplete = true
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F14))
            .systemBarsPadding()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            // TOP NAVIGATION BAR
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1F29))
                        .clickable {
                            if (step > 0) step-- else onBackToWelcome()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFFFF2D55),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Category Title
                val stepTitle = when (step) {
                    0 -> "Fitness Goal"
                    1 -> "Fitness Habit"
                    2 -> "Fitness Experience"
                    3, 4 -> "Available Equipment"
                    5 -> "Weekly Workout Goal"
                    6 -> "Workout Previews"
                    7 -> "Your Body Stats"
                    8 -> "Your Profile"
                    9 -> "Your Program"
                    else -> "Your Profile"
                }
                Text(
                    text = stepTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                // Skip Button (Hidden on step 9 "Your Program")
                if (step < 9) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF1E1F29))
                            .clickable {
                                if (step < 8) step++ else step = 9
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Skip",
                            color = Color(0xFFFF2D55),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                } else {
                    Spacer(Modifier.size(42.dp))
                }
            }

            Spacer(Modifier.height(24.dp))

            // STEP ROUTER
            when (step) {
                0 -> {
                    QuestionStepContent(
                        headline = "What is your top\nfitness goal?",
                        subtitle = null,
                        options = listOf(
                            QuestionOption("Lift heavier", "Lift heavier", iconType = GoalIconType.DUMBBELL),
                            QuestionOption("Build muscle", "Build muscle", iconType = GoalIconType.CHEVRON_UP),
                            QuestionOption("Get lean and defined", "Get lean and defined", iconType = GoalIconType.TARGET),
                            QuestionOption("Lose weight", "Lose weight", iconType = GoalIconType.CHEVRON_DOWN)
                        ),
                        selectedId = selectedGoal,
                        onSelect = { selectedGoal = it },
                        onNext = { step = 1 }
                    )
                }
                1 -> {
                    QuestionStepContent(
                        headline = "How consistent are you\nwith strength training?",
                        subtitle = null,
                        options = listOf(
                            QuestionOption("never", "I've never had a consistent routine"),
                            QuestionOption("break", "I'm returning from a break in consistency"),
                            QuestionOption("struggle", "I struggle with consistency"),
                            QuestionOption("consistent", "I strength train consistently")
                        ),
                        selectedId = selectedHabit,
                        onSelect = { selectedHabit = it },
                        onNext = { step = 2 }
                    )
                }
                2 -> {
                    QuestionStepContent(
                        headline = "How much strength\ntraining experience do you\nhave?",
                        subtitle = null,
                        options = listOf(
                            QuestionOption("brand_new", "I am brand new to strength training"),
                            QuestionOption("less_1", "Less than 1 year"),
                            QuestionOption("1_2", "1-2 years"),
                            QuestionOption("2_4", "2-4 years"),
                            QuestionOption("4_plus", "4+ years")
                        ),
                        selectedId = selectedExperience,
                        onSelect = { selectedExperience = it },
                        onNext = { step = 3 }
                    )
                }
                3 -> {
                    QuestionStepContent(
                        headline = "Where do you usually\nwork out?",
                        subtitle = "ADhils will compile a starter equipment list based on the location you pick.",
                        options = listOf(
                            QuestionOption(
                                "Large Gym",
                                "Large Gym",
                                description = "Full fitness clubs such as Anytime, Planet Fitness, Golds, 24-Hour, Equinox."
                            ),
                            QuestionOption(
                                "Small Gym",
                                "Small Gym",
                                description = "Compact public gyms with limited equipment."
                            ),
                            QuestionOption(
                                "Garage Gym",
                                "Garage Gym",
                                description = "Barbells, squat rack, dumbbells, etc."
                            ),
                            QuestionOption(
                                "At Home",
                                "At Home",
                                description = "Limited equipment such as bands and dumbbells."
                            ),
                            QuestionOption(
                                "Without Equipment",
                                "Without Equipment",
                                description = "Workout anywhere with bodyweight only exercises."
                            ),
                            QuestionOption(
                                "Custom",
                                "Custom",
                                description = "Start from scratch and build your own equipment list."
                            )
                        ),
                        selectedId = selectedGymPreset,
                        onSelect = { selectedGymPreset = it },
                        onNext = { step = 4 }
                    )
                }
                4 -> {
                    ReviewEquipmentStepContent(
                        selectedEquipment = selectedEquipment,
                        onToggleEquipment = { id ->
                            selectedEquipment = if (id in selectedEquipment) selectedEquipment - id else selectedEquipment + id
                        },
                        dumbbellWeights = dumbbellWeights,
                        onEditDumbbells = { showDumbbellEditor = true },
                        onNext = { step = 5 }
                    )
                }
                5 -> {
                    WeeklyWorkoutGoalStepContent(
                        scheduleType = scheduleType,
                        onScheduleTypeChange = { scheduleType = it },
                        selectedDaysCount = selectedDaysCount,
                        onSelectDaysCount = { selectedDaysCount = it },
                        selectedSpecificDays = selectedSpecificDays,
                        onToggleSpecificDay = { day ->
                            selectedSpecificDays = if (day in selectedSpecificDays) selectedSpecificDays - day else selectedSpecificDays + day
                        },
                        onNext = { step = 6 }
                    )
                }
                6 -> {
                    LaunchedEffect(Unit) {
                        PlatformNotification.checkPermission { granted ->
                            notificationPermissionGranted = granted
                        }
                    }

                    WorkoutPreviewsStepContent(
                        previewTime = workoutPreviewTime,
                        isPermissionGranted = notificationPermissionGranted,
                        onOpenTimePicker = { showTimePicker = true },
                        onSelectPresetTime = { workoutPreviewTime = it },
                        onTestNotification = {
                            PlatformNotification.requestPermission { allowed ->
                                notificationPermissionGranted = allowed
                                PlatformNotification.sendTestNotification(
                                    title = "TODAY'S WORKOUT IS READY 🏋️",
                                    message = "Push Day: Barbell Bench Press, Dumbbell Shoulder Press, Lateral Raise & 3 more."
                                )
                            }
                        },
                        onEnableNotifications = {
                            PlatformNotification.requestPermission { allowed ->
                                notificationPermissionGranted = allowed
                                workoutPreviewEnabled = true
                                val (h, m) = parseTimeString(workoutPreviewTime)
                                PlatformNotification.scheduleReminder(h, m, setOf(1, 2, 3, 4, 5, 6, 7))
                                step = 7
                            }
                        },
                        onNotNow = {
                            workoutPreviewEnabled = false
                            step = 7
                        }
                    )
                }
                7 -> {
                    BodyStatsStepContent(
                        isHealthSyncEnabled = healthSyncEnabled,
                        isSyncing = isReadingHealthData,
                        onSyncHealthClicked = {
                            isReadingHealthData = true
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(500)
                                PlatformHealth.requestAuthorization { granted ->
                                    isReadingHealthData = false
                                    if (granted) {
                                        healthSyncEnabled = true
                                    }
                                }
                            }
                        },
                        onLearnMoreClicked = {
                            // Information dialog
                        },
                        gender = selectedGender,
                        onGenderChange = { selectedGender = it },
                        birthDate = birthDate,
                        onBirthDateChange = { birthDate = it },
                        heightText = userHeightText,
                        onHeightChange = { userHeightText = it },
                        weightText = userWeightText,
                        onWeightChange = { userWeightText = it },
                        unit = initialProfile.unit,
                        onNext = { step = 8 }
                    )
                }
                8 -> {
                    NameStepContent(
                        name = athleteName,
                        onNameChange = { athleteName = it },
                        onFinish = { step = 9 }
                    )
                }
                9 -> {
                    val effectiveDays = if (scheduleType == "days_per_week") selectedDaysCount else selectedSpecificDays.size.coerceAtLeast(1)
                    val calculatedSplit = when (effectiveDays) {
                        1 -> "Full Body"
                        2 -> "Full Body Split"
                        3 -> "Full Body"
                        4 -> "Upper/Lower"
                        5 -> "Push/Pull/Legs"
                        6 -> "Push/Pull/Legs (6-Day)"
                        else -> "Target Muscle Split"
                    }
                    val mappedPreset = when (selectedGymPreset) {
                        "Large Gym", "Small Gym" -> "Commercial Gym"
                        "Garage Gym", "At Home" -> "Home Gym"
                        "Without Equipment" -> "Bodyweight / Travel"
                        else -> "Custom"
                    }
                    val mappedExp = when (selectedExperience) {
                        "I am brand new to strength training", "Less than 1 year" -> "Beginner"
                        "1-2 years", "2-4 years" -> "Intermediate"
                        "4+ years" -> "Advanced"
                        else -> "Beginner"
                    }

                    YourProgramStepContent(
                        goal = selectedGoal,
                        trainingStyle = "Strength Training",
                        muscleSplit = calculatedSplit,
                        equipmentProfile = mappedPreset,
                        difficulty = mappedExp,
                        onGetProgram = { onFinish(createProfile()) }
                    )
                }
            }
        }
    }

    // Dumbbell Weights Edit Dialog
    if (showDumbbellEditor) {
        DumbbellWeightEditorDialog(
            unit = initialProfile.unit,
            currentWeights = dumbbellWeights,
            onSave = { updated ->
                dumbbellWeights = updated
                showDumbbellEditor = false
            },
            onDismiss = { showDumbbellEditor = false }
        )
    }

    // Workout Preview Time Picker Dialog
    if (showTimePicker) {
        WorkoutTimePickerDialog(
            initialTime = workoutPreviewTime,
            onSaveTime = {
                workoutPreviewTime = it
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    // System Notification Settings Dialog (when notifications are disabled/denied)
    if (showNotificationSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationSettingsDialog = false },
            title = {
                Text(
                    text = "Enable System Notifications",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "To receive workout previews at $workoutPreviewTime, system notifications must be enabled for ADhils Fitness in device Settings.\n\nWould you like to open Settings now?",
                    color = Color(0xFFC7C7CC),
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNotificationSettingsDialog = false
                        WorkoutNotificationManager.openNotificationSettings(null)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2D55))
                ) {
                    Text("Open Settings", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNotificationSettingsDialog = false
                        workoutPreviewEnabled = false
                        step = 7
                    }
                ) {
                    Text("Continue Without Notifications", color = Color(0xFF8E8E93))
                }
            },
            containerColor = Color(0xFF1E1F29)
        )
    }
}

@Composable
private fun ReviewEquipmentStepContent(
    selectedEquipment: Set<String>,
    onToggleEquipment: (String) -> Unit,
    dumbbellWeights: List<Double>,
    onEditDumbbells: () -> Unit,
    onNext: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val allEquipmentCatalog = remember {
        listOf(
            // Small weights
            EquipmentItem("Dumbbells", "Small weights", "Dumbbells", canEditWeights = true, "Dumbbells"),
            EquipmentItem("Kettlebells", "Small weights", "Kettlebells", canEditWeights = true, "Kettlebells"),
            
            // Bars & plates
            EquipmentItem("Barbells", "Bars & plates", "Barbells", canEditWeights = true, "Barbells"),
            EquipmentItem("Weight Plates", "Bars & plates", "Weight Plates", canEditWeights = true, "Plates"),
            EquipmentItem("EZ Bar", "Bars & plates", "EZ Bar", canEditWeights = false, "EZ Bar"),

            // Benches & accessories
            EquipmentItem("Flat / Incline Bench", "Benches & accessories", "Flat / Incline Bench", false, "Bench"),
            EquipmentItem("Squat Rack", "Benches & accessories", "Squat Rack", false, "Squat Rack"),
            EquipmentItem("Pull-Up Bar", "Benches & accessories", "Pull-Up Bar", false, "Pull-Up Bar"),
            EquipmentItem("Resistance Bands", "Benches & accessories", "Resistance Bands", false, "Bands"),
            EquipmentItem("Cable Machine", "Benches & accessories", "Cable Machine", false, "Cable"),
            EquipmentItem("Bodyweight", "Benches & accessories", "Bodyweight", false, "Bodyweight")
        )
    }

    val filteredList = remember(searchQuery) {
        if (searchQuery.isBlank()) allEquipmentCatalog
        else allEquipmentCatalog.filter { it.name.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true) }
    }

    val grouped = remember(filteredList) {
        filteredList.groupBy { it.category }
    }

    val formattedDumbbellSummary = remember(dumbbellWeights) {
        if (dumbbellWeights.isEmpty()) "None selected"
        else dumbbellWeights.sorted().take(4).map { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }.joinToString(", ") + if (dumbbellWeights.size > 4) "..." else ""
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 140.dp) // Space for floating search and next button
        ) {
            // Heading matching media_1790808814622.png
            Text(
                text = "Review your\nselected equipment.",
                style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 38.sp,
                    color = Color.White
                )
            )

            Spacer(Modifier.height(10.dp))
            Text(
                text = "We selected these based on where you train. You can edit this now or adjust later.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF8E8E93),
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(24.dp))

            // Render each category
            grouped.forEach { (categoryName, items) ->
                Text(
                    text = categoryName,
                    style = TextStyle(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        color = Color.White
                    ),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                items.forEach { eq ->
                    val isChecked = eq.id in selectedEquipment

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (eq.canEditWeights && eq.id == "Dumbbells") {
                                    onEditDumbbells()
                                } else {
                                    onToggleEquipment(eq.id)
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Equipment Graphic Thumbnail
                        EquipmentThumbnail(eq.thumbnailType)

                        Spacer(Modifier.width(16.dp))

                        // Title & Subtitle / Weights
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = eq.name,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )

                            if (eq.canEditWeights) {
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val weightsPreview = if (eq.id == "Dumbbells") formattedDumbbellSummary
                                        else if (eq.id == "Kettlebells") "9.0, 13.0, 18.0, 26.0..."
                                        else if (eq.id == "Barbells") "45.0..."
                                        else "2.5, 5.0, 10.0, 25.0..."

                                    Text(
                                        text = weightsPreview,
                                        fontSize = 14.sp,
                                        color = Color(0xFF8E8E93)
                                    )

                                    // Interactive Edit text
                                    Text(
                                        text = "Edit",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontStyle = FontStyle.Italic,
                                        color = Color.White,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable {
                                                if (eq.id == "Dumbbells") onEditDumbbells()
                                            }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.width(16.dp))

                        // Custom Checkbox
                        AthleticCheckbox(
                            checked = isChecked,
                            onCheckedChange = { onToggleEquipment(eq.id) }
                        )
                    }

                    HorizontalDivider(
                        color = Color(0xFF1E2028),
                        thickness = 1.dp
                    )
                }

                Spacer(Modifier.height(12.dp))
            }
        }

        // FLOATING BOTTOM CONTROLS (Search + Next Button)
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF0F0F14).copy(alpha = 0.95f), Color(0xFF0F0F14))
                    )
                )
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search Input Pill
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF242633)
            ) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                "Search For Equipment",
                                color = Color(0xFF8E8E93),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = Color(0xFF8E8E93),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }
            }

            // Next Button
            Button(
                onClick = onNext,
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
                    text = "Next",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DumbbellWeightEditorDialog(
    unit: String,
    currentWeights: List<Double>,
    onSave: (List<Double>) -> Unit,
    onDismiss: () -> Unit
) {
    var workingWeights by remember { mutableStateOf(currentWeights.toSet()) }
    var customWeightInput by remember { mutableStateOf("") }

    // Predefined standard dumbbells in lb
    val standardWeights = remember {
        listOf(
            5.0, 7.5, 10.0, 12.5, 15.0, 17.5, 20.0, 22.5, 25.0, 27.5,
            30.0, 32.5, 35.0, 40.0, 45.0, 50.0, 55.0, 60.0, 65.0, 70.0,
            75.0, 80.0, 85.0, 90.0, 95.0, 100.0
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF161720)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header with title and close button
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Available Dumbbells",
                        style = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                            color = Color.White
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Text(
                    text = "Select all dumbbell pairs you have access to. ADhils will only recommend workout weights from this list.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E8E93),
                    lineHeight = 16.sp
                )

                Spacer(Modifier.height(14.dp))

                // Quick presets: Select all, clear, standard 5-50
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = false,
                        onClick = { workingWeights = standardWeights.filter { it <= 50.0 }.toSet() },
                        label = { Text("5–50 lb", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF242633),
                            labelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = false,
                        onClick = { workingWeights = standardWeights.toSet() },
                        label = { Text("Select All", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF242633),
                            labelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = false,
                        onClick = { workingWeights = emptySet() },
                        label = { Text("Clear All", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF242633),
                            labelColor = Color.White
                        )
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Weights Grid
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        standardWeights.forEach { wt ->
                            val isSelected = wt in workingWeights
                            val label = if (wt % 1.0 == 0.0) "${wt.toInt()} $unit" else "$wt $unit"

                            Surface(
                                onClick = {
                                    workingWeights = if (isSelected) workingWeights - wt else workingWeights + wt
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFFFF2D55) else Color(0xFF242633),
                                border = if (!isSelected) BorderStroke(1.dp, Color(0xFF353746)) else null,
                                modifier = Modifier
                                    .height(44.dp)
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                    Text(
                                        text = label,
                                        color = Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Custom weight input row
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customWeightInput,
                            onValueChange = { customWeightInput = it },
                            placeholder = { Text("Add custom (e.g. 17.5)", fontSize = 13.sp, color = Color(0xFF636366)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFF2D55),
                                unfocusedBorderColor = Color(0xFF2C2C36),
                                focusedContainerColor = Color(0xFF1E1F29),
                                unfocusedContainerColor = Color(0xFF1E1F29)
                            )
                        )
                        Button(
                            onClick = {
                                val parsed = customWeightInput.toDoubleOrNull()
                                if (parsed != null && parsed > 0) {
                                    workingWeights = workingWeights + parsed
                                    customWeightInput = ""
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C36))
                        ) {
                            Text("Add", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Save Button
                Button(
                    onClick = { onSave(workingWeights.toList().sorted()) },
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
                        text = "Save Dumbbell Weights (${workingWeights.size} selected)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun EquipmentThumbnail(thumbnailType: String) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1C1D26)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(36.dp)) {
            val w = size.width; val h = size.height
            val primaryColor = Color(0xFF9EA0AA)

            when (thumbnailType) {
                "Dumbbells" -> {
                    // Pair of dumbbells
                    drawLine(primaryColor, Offset(w * 0.15f, h * 0.45f), Offset(w * 0.85f, h * 0.45f), strokeWidth = 3f)
                    drawRoundRect(primaryColor, Offset(w * 0.10f, h * 0.20f), androidx.compose.ui.geometry.Size(w * 0.12f, h * 0.50f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f))
                    drawRoundRect(primaryColor, Offset(w * 0.78f, h * 0.20f), androidx.compose.ui.geometry.Size(w * 0.12f, h * 0.50f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f))
                }
                "Kettlebells" -> {
                    // Kettlebell bell
                    drawCircle(primaryColor, radius = w * 0.32f, center = Offset(w * 0.5f, h * 0.62f))
                    // Kettlebell handle
                    val handle = Path().apply {
                        moveTo(w * 0.30f, h * 0.42f)
                        cubicTo(w * 0.30f, h * 0.15f, w * 0.70f, h * 0.15f, w * 0.70f, h * 0.42f)
                    }
                    drawPath(handle, primaryColor, style = Stroke(width = 3.5f, cap = StrokeCap.Round))
                }
                "Barbells" -> {
                    // Long barbell diagonal
                    drawLine(primaryColor, Offset(w * 0.15f, h * 0.85f), Offset(w * 0.85f, h * 0.15f), strokeWidth = 3.5f)
                    drawCircle(primaryColor, radius = w * 0.14f, center = Offset(w * 0.28f, h * 0.72f))
                    drawCircle(primaryColor, radius = w * 0.14f, center = Offset(w * 0.72f, h * 0.28f))
                }
                "Plates" -> {
                    drawCircle(primaryColor, radius = w * 0.42f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = 4f))
                    drawCircle(primaryColor, radius = w * 0.12f, center = Offset(w * 0.5f, h * 0.5f))
                }
                "EZ Bar" -> {
                    val ez = Path().apply {
                        moveTo(w * 0.10f, h * 0.50f)
                        lineTo(w * 0.30f, h * 0.50f)
                        lineTo(w * 0.40f, h * 0.35f)
                        lineTo(w * 0.60f, h * 0.65f)
                        lineTo(w * 0.70f, h * 0.50f)
                        lineTo(w * 0.90f, h * 0.50f)
                    }
                    drawPath(ez, primaryColor, style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                "Bench" -> {
                    // Bench top
                    drawLine(primaryColor, Offset(w * 0.15f, h * 0.45f), Offset(w * 0.85f, h * 0.45f), strokeWidth = 5f, cap = StrokeCap.Round)
                    // Legs
                    drawLine(primaryColor, Offset(w * 0.25f, h * 0.45f), Offset(w * 0.25f, h * 0.80f), strokeWidth = 3f)
                    drawLine(primaryColor, Offset(w * 0.75f, h * 0.45f), Offset(w * 0.75f, h * 0.80f), strokeWidth = 3f)
                    drawLine(primaryColor, Offset(w * 0.15f, h * 0.80f), Offset(w * 0.85f, h * 0.80f), strokeWidth = 3f)
                }
                "Bands" -> {
                    drawOval(primaryColor, topLeft = Offset(w * 0.15f, h * 0.30f), size = androidx.compose.ui.geometry.Size(w * 0.70f, h * 0.40f), style = Stroke(width = 3f))
                }
                else -> {
                    // Bodyweight icon / figure
                    drawCircle(primaryColor, radius = w * 0.12f, center = Offset(w * 0.5f, h * 0.25f))
                    drawLine(primaryColor, Offset(w * 0.5f, h * 0.37f), Offset(w * 0.5f, h * 0.70f), strokeWidth = 3.5f)
                    drawLine(primaryColor, Offset(w * 0.25f, h * 0.50f), Offset(w * 0.75f, h * 0.50f), strokeWidth = 3f)
                    drawLine(primaryColor, Offset(w * 0.5f, h * 0.70f), Offset(w * 0.30f, h * 0.92f), strokeWidth = 3f)
                    drawLine(primaryColor, Offset(w * 0.5f, h * 0.70f), Offset(w * 0.70f, h * 0.92f), strokeWidth = 3f)
                }
            }
        }
    }
}

@Composable
fun AthleticCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (checked) Color.White else Color.Transparent)
            .border(
                width = 2.dp,
                color = if (checked) Color.White else Color(0xFF484B5E),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Checked",
                tint = Color(0xFF0F0F14),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun QuestionStepContent(
    headline: String,
    subtitle: String?,
    options: List<QuestionOption>,
    selectedId: String,
    onSelect: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Large Italic Headline
        Text(
            text = headline,
            style = TextStyle(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                lineHeight = 38.sp,
                color = Color.White
            )
        )

        if (subtitle != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF8E8E93),
                lineHeight = 20.sp
            )
        }

        Spacer(Modifier.height(28.dp))

        // Options List
        options.forEachIndexed { index, option ->
            val isSelected = (option.id == selectedId || option.title == selectedId)

            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(option.id.ifEmpty { option.title }) }
                    .padding(vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Optional Left Icon
                if (option.iconType != null) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .padding(end = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        RenderGoalIcon(option.iconType, isSelected)
                    }
                    Spacer(Modifier.width(16.dp))
                }

                // Text Content
                Column(Modifier.weight(1f)) {
                    Text(
                        text = option.title,
                        fontSize = 17.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = Color.White
                    )
                    if (option.description != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = option.description,
                            fontSize = 14.sp,
                            color = Color(0xFF8E8E93),
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                // Custom Radio Circle
                AthleticRadioButton(selected = isSelected)
            }

            HorizontalDivider(
                color = Color(0xFF1E2028),
                thickness = 1.dp
            )
        }

        Spacer(Modifier.height(36.dp))

        // Next Button
        Button(
            onClick = onNext,
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
                text = "Next",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun NameStepContent(
    name: String,
    onNameChange: (String) -> Unit,
    onFinish: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "What is your\nname?",
            style = TextStyle(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                lineHeight = 38.sp,
                color = Color.White
            )
        )

        Spacer(Modifier.height(12.dp))
        Text(
            text = "We'll personalize your daily workouts, progress charts, and AI coaching.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF8E8E93),
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(36.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            placeholder = { Text("Enter your name (e.g. Alex, Jordan)", color = Color(0xFF636366)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFFF2D55),
                unfocusedBorderColor = Color(0xFF2C2C36),
                focusedContainerColor = Color(0xFF17181F),
                unfocusedContainerColor = Color(0xFF17181F)
            )
        )

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = onFinish,
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
                text = "Next",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun AthleticRadioButton(selected: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier.size(24.dp)) {
        val strokeColor = if (selected) Color(0xFFFF2D55) else Color(0xFF484B5E)
        drawCircle(
            color = strokeColor,
            radius = size.minDimension / 2f - 1.dp.toPx(),
            style = Stroke(width = 2.dp.toPx())
        )
        if (selected) {
            drawCircle(
                color = Color(0xFFFF2D55),
                radius = (size.minDimension / 2f) * 0.52f
            )
        }
    }
}

@Composable
fun RenderGoalIcon(type: GoalIconType, selected: Boolean) {
    val iconColor = if (selected) Color(0xFFFF2D55) else Color(0xFF8E8E93)
    when (type) {
        GoalIconType.DUMBBELL -> {
            Canvas(Modifier.size(24.dp)) {
                val w = size.width; val h = size.height
                drawLine(
                    iconColor,
                    Offset(w * 0.15f, h * 0.5f),
                    Offset(w * 0.85f, h * 0.5f),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawRoundRect(
                    iconColor,
                    Offset(w * 0.10f, h * 0.20f),
                    androidx.compose.ui.geometry.Size(w * 0.10f, h * 0.60f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                )
                drawRoundRect(
                    iconColor,
                    Offset(w * 0.23f, h * 0.28f),
                    androidx.compose.ui.geometry.Size(w * 0.08f, h * 0.44f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                )
                drawRoundRect(
                    iconColor,
                    Offset(w * 0.69f, h * 0.28f),
                    androidx.compose.ui.geometry.Size(w * 0.08f, h * 0.44f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                )
                drawRoundRect(
                    iconColor,
                    Offset(w * 0.80f, h * 0.20f),
                    androidx.compose.ui.geometry.Size(w * 0.10f, h * 0.60f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                )
            }
        }
        GoalIconType.CHEVRON_UP -> {
            Canvas(Modifier.size(24.dp)) {
                val w = size.width; val h = size.height
                val p1 = Path().apply {
                    moveTo(w * 0.22f, h * 0.46f)
                    lineTo(w * 0.50f, h * 0.22f)
                    lineTo(w * 0.78f, h * 0.46f)
                }
                val p2 = Path().apply {
                    moveTo(w * 0.22f, h * 0.72f)
                    lineTo(w * 0.50f, h * 0.48f)
                    lineTo(w * 0.78f, h * 0.72f)
                }
                drawPath(p1, iconColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(p2, iconColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        GoalIconType.TARGET -> {
            Canvas(Modifier.size(24.dp)) {
                val c = Offset(size.width / 2f, size.height / 2f)
                drawCircle(iconColor, radius = size.width * 0.40f, style = Stroke(width = 2.dp.toPx()))
                drawCircle(iconColor, radius = size.width * 0.15f)
                drawLine(iconColor, Offset(c.x, c.y - size.width * 0.46f), Offset(c.x, c.y - size.width * 0.24f), strokeWidth = 2.dp.toPx())
                drawLine(iconColor, Offset(c.x, c.y + size.width * 0.24f), Offset(c.x, c.y + size.width * 0.46f), strokeWidth = 2.dp.toPx())
                drawLine(iconColor, Offset(c.x - size.width * 0.46f, c.y), Offset(c.x - size.width * 0.24f, c.y), strokeWidth = 2.dp.toPx())
                drawLine(iconColor, Offset(c.x + size.width * 0.24f, c.y), Offset(c.x + size.width * 0.46f, c.y), strokeWidth = 2.dp.toPx())
            }
        }
        GoalIconType.CHEVRON_DOWN -> {
            Canvas(Modifier.size(24.dp)) {
                val w = size.width; val h = size.height
                val p1 = Path().apply {
                    moveTo(w * 0.22f, h * 0.28f)
                    lineTo(w * 0.50f, h * 0.52f)
                    lineTo(w * 0.78f, h * 0.28f)
                }
                val p2 = Path().apply {
                    moveTo(w * 0.22f, h * 0.54f)
                    lineTo(w * 0.50f, h * 0.78f)
                    lineTo(w * 0.78f, h * 0.54f)
                }
                drawPath(p1, iconColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(p2, iconColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

@Composable
private fun WeeklyWorkoutGoalStepContent(
    scheduleType: String,
    onScheduleTypeChange: (String) -> Unit,
    selectedDaysCount: Int,
    onSelectDaysCount: (Int) -> Unit,
    selectedSpecificDays: Set<String>,
    onToggleSpecificDay: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Set your workout schedule",
            style = TextStyle(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                lineHeight = 38.sp,
                color = Color.White
            )
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Choose a frequency you can commit to.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF8E8E93)
        )

        Spacer(Modifier.height(24.dp))

        // Segmented Control
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(23.dp))
                .background(Color(0xFF1E1F29))
                .padding(4.dp)
        ) {
            Row(Modifier.fillMaxSize()) {
                val isDaysPerWeek = scheduleType == "days_per_week"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isDaysPerWeek) Color(0xFF383A4A) else Color.Transparent)
                        .clickable { onScheduleTypeChange("days_per_week") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Days Per Week",
                        color = if (isDaysPerWeek) Color.White else Color(0xFF8E8F9E),
                        fontWeight = if (isDaysPerWeek) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (!isDaysPerWeek) Color(0xFF383A4A) else Color.Transparent)
                        .clickable { onScheduleTypeChange("specific_days") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Specific Days",
                        color = if (!isDaysPerWeek) Color.White else Color(0xFF8E8F9E),
                        fontWeight = if (!isDaysPerWeek) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (scheduleType == "days_per_week") {
            (1..6).forEach { days ->
                val label = "$days day${if (days > 1) "s" else ""} a week"
                val isSelected = selectedDaysCount == days

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectDaysCount(days) }
                        .padding(vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = TextStyle(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                    AthleticRadioButton(selected = isSelected)
                }
                HorizontalDivider(color = Color(0xFF1E2028), thickness = 1.dp)
            }
        } else {
            val allDays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
            Text(
                text = "${selectedSpecificDays.size} day${if (selectedSpecificDays.size != 1) "s" else ""} selected",
                color = Color(0xFFFF2D55),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            allDays.forEach { day ->
                val isSelected = day in selectedSpecificDays
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleSpecificDay(day) }
                        .padding(vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = day,
                        style = TextStyle(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                    AthleticCheckbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSpecificDay(day) }
                    )
                }
                HorizontalDivider(color = Color(0xFF1E2028), thickness = 1.dp)
            }
        }

        Spacer(Modifier.height(36.dp))

        Button(
            onClick = onNext,
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
                text = "Next",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun WorkoutPreviewsStepContent(
    previewTime: String,
    isPermissionGranted: Boolean,
    onOpenTimePicker: () -> Unit,
    onSelectPresetTime: (String) -> Unit,
    onTestNotification: () -> Unit,
    onEnableNotifications: () -> Unit,
    onNotNow: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Get workout previews on\ntraining days",
            style = TextStyle(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                lineHeight = 38.sp,
                color = Color.White
            )
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "When it's almost time to train, we'll remind you so you can get ready.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF8E8E93),
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(16.dp))

        // System Notification Status Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (isPermissionGranted) Color(0xFF142B1D) else Color(0xFF282015))
                .border(
                    width = 1.dp,
                    color = if (isPermissionGranted) Color(0xFF34C759).copy(alpha = 0.6f) else Color(0xFFFF9500).copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isPermissionGranted) Icons.Default.CheckCircle else Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = if (isPermissionGranted) Color(0xFF34C759) else Color(0xFFFF9500),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isPermissionGranted) "System Notifications Allowed" else "System Permission Required",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPermissionGranted) Color(0xFF34C759) else Color(0xFFFF9500)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (isPermissionGranted)
                            "ADhils Fitness has permission to post workout previews."
                        else
                            "Tapping 'Enable Notifications' will prompt system notification permission.",
                        fontSize = 12.sp,
                        color = Color(0xFF8E8E93),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Example:",
            color = Color(0xFF8E8E93),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))

        // Example Notification Card matching media_1790808875971.png
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF242533))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                // Red Hexagon App Icon
                Canvas(modifier = Modifier.size(38.dp)) {
                    val w = size.width; val h = size.height
                    val p = Path().apply {
                        moveTo(w * 0.5f, 0f)
                        lineTo(w, h * 0.25f)
                        lineTo(w, h * 0.75f)
                        lineTo(w * 0.5f, h)
                        lineTo(0f, h * 0.75f)
                        lineTo(0f, h * 0.25f)
                        close()
                    }
                    drawPath(p, Color(0xFFFF2D55))
                    drawCircle(Color.White, radius = w * 0.16f, center = Offset(w * 0.5f, h * 0.5f))
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TODAY'S WORKOUT IS READY 🏋️",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Text(
                            text = "now",
                            fontSize = 12.sp,
                            color = Color(0xFF8E8E93)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Push Day: Barbell Bench Press, Dumbbell Shoulder Press, Lateral Raise, and 3 more.",
                        fontSize = 13.sp,
                        color = Color(0xFFC7C7CC),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = "Choose a time to receive your preview:",
            fontSize = 15.sp,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(12.dp))

        // Preset Time Chips
        val presetTimes = listOf("7:30 AM", "9:00 AM", "12:00 PM", "5:30 PM")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presetTimes.forEach { preset ->
                val isSelected = previewTime.equals(preset, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color(0xFFFF2D55) else Color(0xFF222330))
                        .clickable { onSelectPresetTime(preset) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = preset,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color(0xFFB0B1BD)
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Custom Time",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF222330))
                    .clickable { onOpenTimePicker() }
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(
                    text = previewTime,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Test Notification Button
        OutlinedButton(
            onClick = onTestNotification,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF2D55)),
            border = BorderStroke(1.dp, Color(0xFFFF2D55).copy(alpha = 0.5f))
        ) {
            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Send Test Preview Notification", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = Color(0xFF1E2028), thickness = 1.dp)
        Spacer(Modifier.height(8.dp))

        Text(
            text = "You can easily change reminder times and notification settings later in Gym Settings",
            fontSize = 13.sp,
            color = Color(0xFF7E8092)
        )

        Spacer(Modifier.height(36.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onNotNow,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E1F29),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Not Now",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = onEnableNotifications,
                modifier = Modifier
                    .weight(1.6f)
                    .height(54.dp),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF2D55),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Enable Notifications",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun WorkoutTimePickerDialog(
    initialTime: String,
    onSaveTime: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHour by remember { mutableStateOf(if (initialTime.contains(":")) initialTime.split(":")[0].trim().toIntOrNull() ?: 9 else 9) }
    var selectedMinute by remember { mutableStateOf(if (initialTime.contains(":")) initialTime.split(":")[1].take(2).trim() else "00") }
    var isAm by remember { mutableStateOf(!initialTime.contains("PM", ignoreCase = true)) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1E1F29))
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Select Preview Time",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(Modifier.height(20.dp))

                // Time Display
                Text(
                    text = "$selectedHour:$selectedMinute ${if (isAm) "AM" else "PM"}",
                    style = TextStyle(
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF2D55)
                    )
                )

                Spacer(Modifier.height(20.dp))

                // Hour Selection Chips
                Text("Hour", color = Color(0xFF8E8E93), fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..12).forEach { hr ->
                        val isSel = hr == selectedHour
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isSel) Color(0xFFFF2D55) else Color(0xFF282937))
                                .clickable { selectedHour = hr },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$hr",
                                color = if (isSel) Color.White else Color(0xFFB0B1BD),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Minute Selection Chips
                Text("Minute", color = Color(0xFF8E8E93), fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("00", "15", "30", "45").forEach { min ->
                        val isSel = min == selectedMinute
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) Color(0xFFFF2D55) else Color(0xFF282937))
                                .clickable { selectedMinute = min }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = min,
                                color = if (isSel) Color.White else Color(0xFFB0B1BD),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // AM/PM Toggle
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("AM" to true, "PM" to false).forEach { (label, amState) ->
                        val isSel = isAm == amState
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) Color(0xFFFF2D55) else Color(0xFF282937))
                                .clickable { isAm = amState }
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSel) Color.White else Color(0xFFB0B1BD),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Cancel", color = Color.White)
                    }

                    Button(
                        onClick = {
                            val formatted = "$selectedHour:$selectedMinute ${if (isAm) "AM" else "PM"}"
                            onSaveTime(formatted)
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2D55))
                    ) {
                        Text("Save Time", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun BodyStatsStepContent(
    isHealthSyncEnabled: Boolean,
    isSyncing: Boolean = false,
    onSyncHealthClicked: () -> Unit,
    onLearnMoreClicked: () -> Unit,
    gender: String,
    onGenderChange: (String) -> Unit,
    birthDate: String,
    onBirthDateChange: (String) -> Unit,
    heightText: String,
    onHeightChange: (String) -> Unit,
    weightText: String,
    onWeightChange: (String) -> Unit,
    unit: String,
    onNext: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Sync with ${PlatformHealth.platformName}",
            style = TextStyle(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                lineHeight = 38.sp,
                color = Color.White
            )
        )

        Spacer(Modifier.height(20.dp))

        // Bullet 1: Exercises, reps and weight
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color(0xFFFF2D55),
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = "Exercises, reps and weight that match your profile",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(16.dp))

        // Bullet 2: Calories burned
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = Color(0xFFFF5252),
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = "Calculate calories burned & ${PlatformHealth.wearableName} heart rate",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(16.dp))

        // Bullet 3: Progress tracking
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = null,
                tint = Color(0xFFFF4081),
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = "Track your fitness progress & recovery",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(16.dp))

        // Bullet 4: Secure and private
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color(0xFFFF2D55),
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = "Secure and private",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Secure and private. We don't sell your data to third parties. It stays on your device to help customize your workouts and sync your sessions.",
                    color = Color(0xFF8E8E93),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // Sync with Platform Health Button
        Button(
            onClick = onSyncHealthClicked,
            enabled = !isSyncing,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFFFF2D55)
            )
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color(0xFFFF2D55),
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Connecting to ${PlatformHealth.platformName}...",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFFFF2D55)
                )
            } else {
                Text(
                    text = if (isHealthSyncEnabled) "✓ Synced with ${PlatformHealth.platformName}" else "Sync with ${PlatformHealth.platformName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    fontStyle = FontStyle.Italic
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLearnMoreClicked() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Learn More",
                color = Color(0xFFFF2D55),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }

        Spacer(Modifier.height(24.dp))

        // OR Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF2C2D3A), thickness = 1.dp)
            Text(
                text = "  OR  ",
                color = Color(0xFF8E8E93),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF2C2D3A), thickness = 1.dp)
        }

        Spacer(Modifier.height(24.dp))

        // Enter Manually Section
        Text(
            text = "Enter manually",
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                color = Color.White
            )
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Optional and can be added later",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF8E8E93)
        )

        Spacer(Modifier.height(18.dp))

        // Gender Selector
        var genderExpanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { genderExpanded = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF2C2C36)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF17181F))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Gender: $gender", color = Color.White, fontSize = 15.sp)
                    Text("▾", color = Color(0xFF8E8E93), fontSize = 16.sp)
                }
            }
            DropdownMenu(
                expanded = genderExpanded,
                onDismissRequest = { genderExpanded = false }
            ) {
                listOf("Male", "Female", "Non-binary", "Prefer not to say").forEach { g ->
                    DropdownMenuItem(
                        text = { Text(g) },
                        onClick = {
                            onGenderChange(g)
                            genderExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Date of Birth
        OutlinedTextField(
            value = birthDate,
            onValueChange = onBirthDateChange,
            label = { Text("Date of Birth (YYYY-MM-DD)", color = Color(0xFF8E8E93)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFFF2D55),
                unfocusedBorderColor = Color(0xFF2C2C36),
                focusedContainerColor = Color(0xFF17181F),
                unfocusedContainerColor = Color(0xFF17181F)
            )
        )

        Spacer(Modifier.height(12.dp))

        // Height & Weight Fields Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = heightText,
                onValueChange = onHeightChange,
                label = { Text("Height", color = Color(0xFF8E8E93)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFFF2D55),
                    unfocusedBorderColor = Color(0xFF2C2C36),
                    focusedContainerColor = Color(0xFF17181F),
                    unfocusedContainerColor = Color(0xFF17181F)
                )
            )

            OutlinedTextField(
                value = weightText,
                onValueChange = onWeightChange,
                label = { Text("Weight ($unit)", color = Color(0xFF8E8E93)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFFF2D55),
                    unfocusedBorderColor = Color(0xFF2C2C36),
                    focusedContainerColor = Color(0xFF17181F),
                    unfocusedContainerColor = Color(0xFF17181F)
                )
            )
        }

        Spacer(Modifier.height(36.dp))

        Button(
            onClick = onNext,
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
                text = "Next",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun YourProgramStepContent(
    goal: String,
    trainingStyle: String = "Strength Training",
    muscleSplit: String,
    equipmentProfile: String,
    difficulty: String,
    onGetProgram: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(8.dp))

        // Large Italic Gold Headline
        Text(
            text = goal,
            style = TextStyle(
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                color = Color(0xFFFFD56B)
            ),
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "Review your program details. You can always edit these later in the app.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF9E9FA9),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(32.dp))

        // Card 1: Training Style
        ProgramSummaryCard(
            icon = Icons.Default.FitnessCenter,
            title = "Training Style",
            value = trainingStyle
        )

        Spacer(Modifier.height(14.dp))

        // Card 2: Muscle Split
        ProgramSummaryCard(
            icon = Icons.Default.DateRange,
            title = "Muscle Split",
            value = muscleSplit
        )

        Spacer(Modifier.height(14.dp))

        // Card 3: Equipment Profile
        ProgramSummaryCard(
            icon = Icons.Default.FitnessCenter,
            title = "Equipment Profile",
            value = equipmentProfile
        )

        Spacer(Modifier.height(14.dp))

        // Card 4: Exercise Difficulty
        ProgramSummaryCard(
            icon = Icons.Default.Tune,
            title = "Exercise Difficulty",
            value = difficulty
        )

        Spacer(Modifier.height(48.dp))

        // Get Your Program Button
        Button(
            onClick = onGetProgram,
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
                text = "Get Your Program",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ProgramSummaryCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F29))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )

            Spacer(Modifier.width(18.dp))

            Column {
                Text(
                    text = title,
                    style = TextStyle(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = value,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF9E9FA9)
                    )
                )
            }
        }
    }
}

