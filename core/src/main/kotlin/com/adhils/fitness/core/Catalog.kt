package com.adhils.fitness.core

object Catalog {
    private fun ex(id: String, name: String, pattern: String, equipment: String, muscles: String,
        setup: String, movement: String, camera: String? = null, view: String = "Side",
        timed: Boolean = false, perHand: Boolean = false) =
        Exercise(id, name, pattern, equipment, muscles, listOf(setup, movement,
            "Choose a comfortable range. Stop the set if you feel pain."),
            "Breathe steadily; exhale through the effort.", camera, view, timed, perHand)
    val exercises = listOf(
        ex("goblet-squat", "Goblet squat", "Squat", "Dumbbells", "Quads · Glutes",
            "Hold one dumbbell close to your chest. Stand with feet comfortably apart.",
            "Bend your knees and hips, then return to standing without bouncing.", "squat"),
        ex("bodyweight-squat", "Bodyweight squat", "Squat", "Bodyweight", "Quads · Glutes",
            "Stand with feet comfortably apart and arms forward for balance.",
            "Lower in control and return to standing.", "squat"),
        ex("rdl", "Dumbbell Romanian deadlift", "Hinge", "Dumbbells", "Hamstrings · Glutes",
            "Stand with a dumbbell in each hand and a slight bend in your knees.",
            "Move your hips back, keeping the weights close. Return to standing in control.", "rdl", perHand = true),
        ex("press", "Dumbbell shoulder press", "Push", "Dumbbells", "Shoulders · Triceps",
            "Stand with dumbbells near shoulder height and feet planted.",
            "Press overhead through a comfortable range, then lower in control.", "press", "Front", perHand = true),
        ex("pushup", "Push-up", "Push", "Bodyweight", "Chest · Triceps · Core",
            "Place hands on the floor near shoulder width with legs extended.",
            "Lower and press up with shoulders, hips, and ankles moving together.", "pushup"),
        ex("plank", "Forearm plank", "Core", "Bodyweight", "Core",
            "Place forearms on the floor with elbows under shoulders.",
            "Hold a steady shoulder-to-ankle line while breathing.", "plank", timed = true),
        ex("row", "One-arm dumbbell row", "Pull", "Dumbbells", "Back · Biceps",
            "Support one hand on a stable surface and hold a dumbbell in the other.",
            "Pull the elbow toward your hip, lower in control, then switch sides.", perHand = true),
        ex("floor-press", "Dumbbell floor press", "Push", "Dumbbells", "Chest · Triceps",
            "Lie on the floor with knees bent and dumbbells near your chest.",
            "Press up and lower until your upper arms gently meet the floor.", perHand = true),
        ex("curl", "Dumbbell curl", "Pull", "Dumbbells", "Biceps",
            "Stand with arms by your sides and a dumbbell in each hand.",
            "Bend your elbows without swinging, then lower slowly.", perHand = true),
        ex("reverse-lunge", "Reverse lunge", "Squat", "Bodyweight", "Quads · Glutes",
            "Stand tall near a stable support if needed.",
            "Step back, lower through a comfortable range, return, and alternate sides."),
        ex("bridge", "Glute bridge", "Hinge", "Bodyweight", "Glutes · Hamstrings",
            "Lie on your back with knees bent and feet flat.",
            "Lift your hips, pause comfortably, then lower."),
        ex("bird-dog", "Bird dog", "Core", "Bodyweight", "Core · Back",
            "Start on hands and knees.",
            "Reach an opposite arm and leg, return, and alternate sides."),
        ex("dead-bug", "Dead bug", "Core", "Bodyweight", "Core",
            "Lie on your back with arms raised and knees bent.",
            "Slowly extend an opposite arm and leg, return, and switch sides."),
        ex("wall-pushup", "Wall push-up", "Push", "Bodyweight", "Chest · Triceps",
            "Stand facing a wall with palms at shoulder height.",
            "Bend elbows toward the wall and press back."),
        ex("calf-raise", "Calf raise", "Accessory", "Bodyweight", "Calves",
            "Stand near a stable support.",
            "Raise your heels, pause, and lower in control."),
        ex("lateral-raise", "Dumbbell lateral raise", "Accessory", "Dumbbells", "Shoulders",
            "Stand with light dumbbells by your sides.",
            "Raise arms outward through a comfortable range without swinging.", perHand = true),
        ex("march", "Easy marching", "Warm-up", "Bodyweight", "Whole body",
            "Stand with space around you.",
            "March at an easy pace while moving your arms.", timed = true),
        ex("cat-cow", "Cat–cow mobility", "Mobility", "Bodyweight", "Back",
            "Start on hands and knees.",
            "Gently move through a comfortable back range without forcing end positions.", timed = true),
        ex("band-row", "Resistance band row", "Pull", "Bands", "Back · Biceps",
            "Secure a band to a suitable anchor according to its instructions.",
            "Pull your elbows back and return with control."),
        ex("band-pull-apart", "Band pull-apart", "Pull", "Bands", "Upper back",
            "Hold a light band in front of you.",
            "Move hands apart through a comfortable range and return slowly.")
    )
    val byId = exercises.associateBy { it.id }
    fun get(id: String): Exercise = requireNotNull(byId[id]) { "Unknown exercise: $id" }
    fun alternatives(id: String, profile: Profile) = exercises.filter {
        it.id != id && it.pattern == get(id).pattern && it.id !in profile.excluded &&
        (it.equipment == "Bodyweight" || it.equipment in profile.equipment)
    }
}
