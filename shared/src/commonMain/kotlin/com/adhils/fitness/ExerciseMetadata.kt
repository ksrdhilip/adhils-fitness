package com.adhils.fitness

import com.adhils.fitness.core.Catalog
import com.adhils.fitness.core.Exercise
import com.adhils.fitness.core.Recovery

data class ExerciseMetadata(
    val aliases: List<String>,
    val primaryMuscles: List<String>,
    val secondaryMuscles: List<String>,
    val equipmentNeeded: String,
    val instructionParagraphs: List<String>
)

object ExerciseCatalogMetadata {
    private val METADATA_MAP = mapOf(
        "single-leg-bridge" to ExerciseMetadata(
            aliases = listOf("Cable Kickbacks", "Glute Kickback", "Leg Kickback", "One-Leg Kickback"),
            primaryMuscles = listOf("Glutes"),
            secondaryMuscles = listOf("Hamstrings"),
            equipmentNeeded = "No equipment needed",
            instructionParagraphs = listOf(
                "Get into a kneeling push-up position with your hands underneath your shoulders and your knees underneath your hips bent to 90 degrees.",
                "Brace your core to keep a neutral spine.",
                "Keeping your ankles bent to 90 degrees, extend your right hip by flexing your right glute to elevate your right foot off the ground."
            )
        ),
        "bridge" to ExerciseMetadata(
            aliases = listOf("Glute Bridge", "Hip Bridge", "Floor Bridge", "Butt Lift"),
            primaryMuscles = listOf("Glutes"),
            secondaryMuscles = listOf("Hamstrings", "Core"),
            equipmentNeeded = "No equipment needed",
            instructionParagraphs = listOf(
                "Lie flat on your back with your knees bent and feet planted flat on the floor, about hip-width apart.",
                "Brace your core and press through your heels to raise your hips until your thighs and torso align in a straight diagonal.",
                "Squeeze your glutes firmly at the top for a two-second pause before slowly lowering under control."
            )
        ),
        "goblet-squat" to ExerciseMetadata(
            aliases = listOf("Front Squat", "Dumbbell Goblet Squat", "Kettlebell Goblet Squat", "DB Squat"),
            primaryMuscles = listOf("Quads"),
            secondaryMuscles = listOf("Glutes", "Core", "Calves"),
            equipmentNeeded = "1 Dumbbell or Kettlebell",
            instructionParagraphs = listOf(
                "Stand tall with your feet slightly wider than shoulder-width apart, holding a dumbbell vertically against your chest with both hands under the top bell.",
                "Brace your core, draw your shoulders back, and keep your elbows pointing down.",
                "Initiate the movement by sending your hips back and bending your knees, lowering until your thighs are parallel to the floor or slightly below.",
                "Drive firmly through the center of your feet and heels to stand back up, squeezing your glutes at the top."
            )
        ),
        "march" to ExerciseMetadata(
            aliases = listOf("Marching in Place", "Standing High Knees March", "Cardio Warmup March"),
            primaryMuscles = listOf("Quads", "Cardio"),
            secondaryMuscles = listOf("Calves", "Hip Flexors", "Core"),
            equipmentNeeded = "No equipment needed",
            instructionParagraphs = listOf(
                "Stand tall with your feet hip-width apart, arms relaxed at your sides, and brace your core.",
                "Lift your right knee smoothly toward your chest until your thigh is parallel to the floor, pumping your left arm forward in a natural running rhythm.",
                "Lower your right foot with control and immediately drive your left knee up while pumping your right arm forward.",
                "Maintain an upright posture, engage your abdominals, and keep a steady, rhythmic marching pace to elevate heart rate and warm up joints."
            )
        ),
        "bodyweight-squat" to ExerciseMetadata(
            aliases = listOf("Air Squat", "Bodyweight Deep Squat", "Free Squat"),
            primaryMuscles = listOf("Quads"),
            secondaryMuscles = listOf("Glutes", "Hamstrings", "Calves"),
            equipmentNeeded = "No equipment needed",
            instructionParagraphs = listOf(
                "Stand with your feet shoulder-width apart and your toes angled slightly outward.",
                "Extend your arms straight in front of you at chest height for balance and brace your core.",
                "Hinge at your hips and bend your knees to lower your body until thighs are parallel to the ground.",
                "Push through the full foot to return smoothly to the upright starting position."
            )
        ),
        "reverse-lunge" to ExerciseMetadata(
            aliases = listOf("Step-Back Lunge", "Backward Lunge", "Alternating Reverse Lunge"),
            primaryMuscles = listOf("Quads"),
            secondaryMuscles = listOf("Glutes", "Hamstrings"),
            equipmentNeeded = "No equipment needed",
            instructionParagraphs = listOf(
                "Stand tall with your feet hip-width apart and hands on your hips or by your sides.",
                "Take a controlled step backward with your right leg and lower your hips until both knees are bent at approximately 90 degrees.",
                "Keep your torso upright and your front knee aligned directly above your ankle.",
                "Drive through your front heel to step back to the starting stance, then alternate legs."
            )
        ),
        "dumbbell-split-squat" to ExerciseMetadata(
            aliases = listOf("Static Lunge", "Dumbbell Split Squat", "DB Stationary Lunge"),
            primaryMuscles = listOf("Quads"),
            secondaryMuscles = listOf("Glutes", "Hamstrings"),
            equipmentNeeded = "Dumbbells",
            instructionParagraphs = listOf(
                "Set up in a staggered stance holding dumbbells at your sides, with one foot forward and the other back on the ball of the foot.",
                "Keep your chest upright and core braced.",
                "Lower your rear knee straight down toward the floor until your front thigh is parallel to the ground.",
                "Press through the heel of your front foot to rise back up to the top of the stance."
            )
        ),
        "bulgarian-split-squat" to ExerciseMetadata(
            aliases = listOf("Rear-Foot Elevated Split Squat", "Bulgarian Lunge", "Bench Split Squat"),
            primaryMuscles = listOf("Quads"),
            secondaryMuscles = listOf("Glutes", "Hamstrings"),
            equipmentNeeded = "Flat Bench, Dumbbells",
            instructionParagraphs = listOf(
                "Stand about two feet in front of a sturdy bench and place the top of your back foot on the bench surface.",
                "Hold dumbbells by your sides with shoulders relaxed and core tight.",
                "Lower your hips down and slightly back until your front thigh is nearly parallel to the floor.",
                "Drive powerfully through your front heel to return to the starting position."
            )
        ),
        "rdl" to ExerciseMetadata(
            aliases = listOf("Romanian Deadlift", "Stiff-Leg Deadlift", "DB RDL", "Dumbbell Hinge"),
            primaryMuscles = listOf("Hamstrings"),
            secondaryMuscles = listOf("Glutes", "Lower Back"),
            equipmentNeeded = "Dumbbells",
            instructionParagraphs = listOf(
                "Stand tall holding a dumbbell in each hand in front of your thighs, feet hip-width apart, with a soft bend in your knees.",
                "Hinge backward at your hips, pushing your glutes toward the wall behind you while keeping your back flat.",
                "Keep the dumbbells close to your legs as they descend to mid-shin height until you feel a deep hamstring stretch.",
                "Squeeze your glutes and hamstrings to pull your hips forward back into a standing lockout."
            )
        ),
        "pushup" to ExerciseMetadata(
            aliases = listOf("Press-up", "Floor Pushup", "Standard Pushup", "Military Pushup"),
            primaryMuscles = listOf("Chest"),
            secondaryMuscles = listOf("Triceps", "Shoulders", "Core"),
            equipmentNeeded = "No equipment needed",
            instructionParagraphs = listOf(
                "Get into a high plank position with your hands slightly wider than shoulder-width and fingers pointing forward.",
                "Lock your core, glutes, and thighs to keep your spine in a straight, unbroken line.",
                "Lower your chest until it hovers an inch above the floor, keeping your elbows tucked at roughly 45 degrees.",
                "Press firmly through the palms of your hands to push the ground away and lock out at the top."
            )
        ),
        "bench-press" to ExerciseMetadata(
            aliases = listOf("Flat Dumbbell Press", "DB Bench Press", "Dumbbell Chest Press"),
            primaryMuscles = listOf("Chest"),
            secondaryMuscles = listOf("Triceps", "Shoulders"),
            equipmentNeeded = "Flat Bench, Dumbbells",
            instructionParagraphs = listOf(
                "Lie back on a flat bench with feet firmly planted on the floor, holding dumbbells at chest height.",
                "Retract your shoulder blades and brace your abdominal wall.",
                "Press the dumbbells straight up over the center of your chest until your arms are extended.",
                "Lower the weights slowly until your elbows are just below bench level, feeling a full chest stretch."
            )
        ),
        "incline-press" to ExerciseMetadata(
            aliases = listOf("Incline DB Press", "Incline Chest Press", "Upper Chest Dumbbell Press"),
            primaryMuscles = listOf("Chest"),
            secondaryMuscles = listOf("Shoulders", "Triceps"),
            equipmentNeeded = "Incline Bench, Dumbbells",
            instructionParagraphs = listOf(
                "Set an adjustable bench to an incline angle between 30 and 45 degrees and sit back with dumbbells at your chest.",
                "Plant your feet firmly on the floor and keep your shoulder blades pinched together.",
                "Press the weights upward and slightly inward in a smooth arc until your elbows are nearly straight.",
                "Lower with control until the weights gently touch the outside of your upper chest."
            )
        ),
        "press" to ExerciseMetadata(
            aliases = listOf("Overhead Press", "Dumbbell Shoulder Press", "DB Military Press", "Seated DB Press"),
            primaryMuscles = listOf("Shoulders"),
            secondaryMuscles = listOf("Triceps", "Upper Traps"),
            equipmentNeeded = "Dumbbells",
            instructionParagraphs = listOf(
                "Hold dumbbells at shoulder height with your palms facing forward and elbows angled slightly forward.",
                "Brace your core and squeeze your glutes to maintain a stable, neutral spine without arching your lower back.",
                "Press both dumbbells straight overhead until arms are fully extended.",
                "Lower the weights with control back to shoulder level."
            )
        ),
        "row" to ExerciseMetadata(
            aliases = listOf("Single-Arm Dumbbell Row", "One-Arm DB Row", "Lats Dumbbell Row", "Kroc Row"),
            primaryMuscles = listOf("Back"),
            secondaryMuscles = listOf("Biceps", "Shoulders"),
            equipmentNeeded = "Dumbbells, Bench (optional)",
            instructionParagraphs = listOf(
                "Place your left knee and left hand on a flat bench for support (or hinge forward supporting on a stable surface).",
                "Hold a dumbbell in your right hand hanging straight down, keeping your back flat and neck neutral.",
                "Pull the dumbbell up toward your hip pocket, driving your elbow toward the ceiling while keeping it tight to your ribs.",
                "Squeeze your lat and mid-back at the top, then lower smoothly to a full stretch."
            )
        ),
        "bent-over-row" to ExerciseMetadata(
            aliases = listOf("Two-Arm DB Row", "Bent Over Dumbbell Row", "Barbell Row Alternative"),
            primaryMuscles = listOf("Back"),
            secondaryMuscles = listOf("Biceps", "Core"),
            equipmentNeeded = "Dumbbells",
            instructionParagraphs = listOf(
                "Hinge forward at the hips with knees softly bent and your back flat at about 45 degrees.",
                "Hold dumbbells below your shoulders with palms facing each other or facing back.",
                "Row the weights upward toward your lower ribs, pulling with your elbows and squeezing your shoulder blades together.",
                "Lower under control back to the starting hang position."
            )
        ),
        "curl" to ExerciseMetadata(
            aliases = listOf("Dumbbell Bicep Curl", "Alternating Bicep Curl", "Standing DB Curl"),
            primaryMuscles = listOf("Biceps"),
            secondaryMuscles = listOf("Forearms"),
            equipmentNeeded = "Dumbbells",
            instructionParagraphs = listOf(
                "Stand tall with feet shoulder-width apart, holding a pair of dumbbells at your sides with palms facing inward.",
                "Pin your upper arms firmly against your ribcage.",
                "Curl the weights up while rotating your wrists outward (supinating) so that your palms face your shoulders at the top.",
                "Squeeze your biceps hard at the peak of contraction, then lower slowly through the full range of motion."
            )
        ),
        "hammer-curl" to ExerciseMetadata(
            aliases = listOf("Neutral Grip Curl", "DB Hammer Curl", "Brachialis Curl"),
            primaryMuscles = listOf("Biceps"),
            secondaryMuscles = listOf("Forearms"),
            equipmentNeeded = "Dumbbells",
            instructionParagraphs = listOf(
                "Hold dumbbells at your sides with palms facing directly inward toward each other in a neutral grip.",
                "Keep your upper arms stationary and elbows tucked.",
                "Curl the weights upward until your thumbs are near shoulder height.",
                "Lower slowly under control to full arm extension."
            )
        ),
        "lateral-raise" to ExerciseMetadata(
            aliases = listOf("Side Lateral Raise", "Dumbbell Side Raise", "Delt Fly"),
            primaryMuscles = listOf("Shoulders"),
            secondaryMuscles = listOf("Upper Traps"),
            equipmentNeeded = "Dumbbells",
            instructionParagraphs = listOf(
                "Stand with a slight bend in your knees and elbows, holding light dumbbells at your sides with palms facing inward.",
                "Engage your core and lean forward very slightly.",
                "Raise your arms out to the sides in a wide arc until your elbows reach shoulder height.",
                "Pause momentarily at the top, then lower with control without swinging."
            )
        ),
        "plank" to ExerciseMetadata(
            aliases = listOf("Forearm Plank", "Elbow Plank", "Core Plank Hold"),
            primaryMuscles = listOf("Core"),
            secondaryMuscles = listOf("Shoulders", "Glutes"),
            equipmentNeeded = "No equipment needed",
            instructionParagraphs = listOf(
                "Place your forearms on the floor with elbows directly beneath your shoulders.",
                "Extend your legs behind you with your toes tucked into the floor.",
                "Brace your abdominals firmly, squeezing your glutes and quads to keep your body in a rigid straight line.",
                "Hold the position while breathing steadily, avoiding any sagging in the lower back or hiking of the hips."
            )
        ),
        "tricep-extension" to ExerciseMetadata(
            aliases = listOf("Overhead Tricep Extension", "DB French Press", "Triceps Skull Crusher"),
            primaryMuscles = listOf("Triceps"),
            secondaryMuscles = listOf("Shoulders"),
            equipmentNeeded = "Dumbbells",
            instructionParagraphs = listOf(
                "Hold a dumbbell overhead with both hands cup-gripping the inner plate, standing or seated tall.",
                "Keep your elbows pointed forward and close to your head.",
                "Slowly bend your elbows to lower the dumbbell behind your neck until your triceps are fully stretched.",
                "Extend your elbows to press the weight back to the overhead lockout."
            )
        ),
        "calf-raise" to ExerciseMetadata(
            aliases = listOf("Standing Calf Raise", "Heel Raise", "Toe Raise"),
            primaryMuscles = listOf("Calves"),
            secondaryMuscles = listOf("Ankles"),
            equipmentNeeded = "No equipment needed",
            instructionParagraphs = listOf(
                "Stand tall with feet hip-width apart near a wall or railing for balance.",
                "Press down through the balls of both feet to lift your heels as high as possible.",
                "Squeeze your calves at the peak contraction for one full second.",
                "Slowly lower your heels back to the floor in control."
            )
        )
    )

    fun getDetails(e: Exercise): ExerciseMetadata {
        METADATA_MAP[e.id]?.let { return it }

        val extracted = Recovery.extractMuscles(e)
        val primary = extracted.take(1).ifEmpty { listOf(e.muscles.split("·").firstOrNull()?.trim() ?: "Muscles") }
        val secondary = extracted.drop(1).ifEmpty { 
            val splits = e.muscles.split("·").drop(1).map { it.trim() }
            if (splits.isNotEmpty()) splits else listOf("Core", "Stabilizers")
        }
        val equip = when (e.equipment) {
            "Bodyweight" -> "No equipment needed"
            "Dumbbells" -> "Dumbbells"
            "Bench" -> "Flat or Incline Bench, Dumbbells"
            "Bands" -> "Resistance Bands"
            else -> e.equipment
        }
        val cleanAliases = listOf(
            "${e.name} Variation",
            "${e.pattern} ${e.name}",
            if (e.equipment == "Dumbbells") "DB ${e.name}" else "Free-weight ${e.name}"
        )
        val steps = e.instructions.ifEmpty {
            listOf(
                "Set up in position with a braced core and neutral spine.",
                "Perform the movement through a comfortable range of motion under steady control.",
                "Breathe steadily and exhale during the effort phase."
            )
        }

        return ExerciseMetadata(
            aliases = cleanAliases,
            primaryMuscles = primary,
            secondaryMuscles = secondary,
            equipmentNeeded = equip,
            instructionParagraphs = steps
        )
    }
}
