package com.adhils.fitness

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adhils.fitness.core.MuscleStatus
import com.adhils.fitness.core.Recovery

/**
 * High-fidelity Anatomical Vector Model.
 * Renders realistic human muscular silhouettes (Front & Back) with dynamically tinted
 * muscle groups matching dark-mode athletic styling.
 */
object AnatomicalPaths {
    // Reference viewBox: 0..200 (width) x 0..400 (height), center x = 100

    // Common Athletic Silhouette (Front & Back)
    const val SILHOUETTE = """
        M 100 22 
        C 107 22 113 28 113 37 
        C 113 46 108 53 105 58 
        C 106 63 111 69 116 73 
        C 125 78 138 82 148 87 
        C 154 90 158 96 159 104 
        C 161 114 158 124 155 134 
        C 152 144 157 158 160 172 
        C 162 184 163 196 167 208 
        C 169 215 173 222 172 228 
        C 170 234 166 238 163 236 
        C 160 234 160 227 159 222 
        C 156 213 153 204 151 195 
        C 146 184 139 176 135 168 
        C 131 161 130 153 130 146 
        C 130 156 129 168 131 178 
        C 133 188 130 198 126 208 
        C 129 220 131 236 128 252 
        C 125 264 122 274 123 288 
        C 125 302 126 316 122 330 
        C 120 340 118 350 119 358 
        C 120 365 125 368 124 372 
        C 122 375 111 375 109 370 
        C 107 362 109 350 109 338 
        C 109 324 106 310 105 296 
        C 104 284 104 274 104 262 
        C 103 250 103 236 102 222 
        C 101 210 101 202 100 195 
        C 99 202 99 210 98 222 
        C 97 236 97 250 96 262 
        C 96 274 96 284 95 296 
        C 94 310 91 324 91 338 
        C 91 350 93 362 91 370 
        C 89 375 78 375 76 372 
        C 75 368 80 365 81 358 
        C 82 350 80 340 78 330 
        C 74 316 75 302 77 288 
        C 78 274 75 264 72 252 
        C 69 236 71 220 74 208 
        C 70 198 67 188 69 178 
        C 71 168 70 156 70 146 
        C 70 153 69 161 65 168 
        C 61 176 54 184 49 195 
        C 47 204 44 213 41 222 
        C 40 227 40 234 37 236 
        C 34 238 30 234 28 228 
        C 27 222 31 215 33 208 
        C 37 196 38 184 40 172 
        C 43 158 48 144 45 134 
        C 42 124 39 114 41 104 
        C 42 96 46 90 52 87 
        C 62 82 75 78 84 73 
        C 89 69 94 63 95 58 
        C 92 53 87 46 87 37 
        C 87 28 93 22 100 22 Z
    """

    // FRONT MUSCLE GROUPS
    const val CHEST_LEFT = """
        M 98 84 C 88 83 78 87 72 93 C 67 99 65 106 66 114 C 69 120 77 121 86 120 C 94 119 98 116 98 110 Z
    """
    const val CHEST_RIGHT = """
        M 102 84 C 112 83 122 87 128 93 C 133 99 135 106 134 114 C 131 120 123 121 114 120 C 106 119 102 116 102 110 Z
    """

    const val SHOULDERS_FRONT_LEFT = """
        M 70 88 C 60 84 50 90 46 99 C 42 107 43 116 47 124 C 52 121 58 114 63 107 C 67 99 69 92 70 88 Z
    """
    const val SHOULDERS_FRONT_RIGHT = """
        M 130 88 C 140 84 150 90 154 99 C 158 107 157 116 153 124 C 148 121 142 114 137 107 C 133 99 131 92 130 88 Z
    """

    const val BICEPS_LEFT = """
        M 47 125 C 43 133 43 143 48 150 C 54 153 58 147 60 139 C 61 131 58 125 54 122 C 51 122 48 123 47 125 Z
    """
    const val BICEPS_RIGHT = """
        M 153 125 C 157 133 157 143 152 150 C 146 153 142 147 140 139 C 139 131 142 125 146 122 C 149 122 152 123 153 125 Z
    """

    const val FOREARMS_LEFT = """
        M 47 153 C 41 163 39 174 36 187 C 34 195 36 204 34 208 C 38 207 43 202 46 193 C 49 181 53 169 52 157 Z
    """
    const val FOREARMS_RIGHT = """
        M 153 153 C 159 163 161 174 164 187 C 166 195 164 204 166 208 C 162 207 157 202 154 193 C 151 181 147 169 148 157 Z
    """

    // Abs 6-Pack + Obliques
    const val ABS_L1 = "M 88 125 H 98 V 136 H 88 Z"
    const val ABS_R1 = "M 102 125 H 112 V 136 H 102 Z"
    const val ABS_L2 = "M 88 139 H 98 V 150 H 88 Z"
    const val ABS_R2 = "M 102 139 H 112 V 150 H 102 Z"
    const val ABS_L3 = "M 88 153 H 98 V 166 C 94 166 89 163 88 160 Z"
    const val ABS_R3 = "M 102 153 H 112 V 160 C 111 163 106 166 102 166 Z"
    const val OBLIQUES_LEFT = "M 74 130 C 70 140 69 150 74 161 C 80 159 84 155 84 148 C 83 139 79 134 74 130 Z"
    const val OBLIQUES_RIGHT = "M 126 130 C 130 140 131 150 126 161 C 120 159 116 155 116 148 C 117 139 121 134 126 130 Z"

    // Quads
    const val QUADS_LEFT_OUTER = """
        M 76 196 C 69 209 67 224 70 239 C 72 248 74 254 75 259 C 78 254 81 243 82 232 C 83 219 83 206 80 196 Z
    """
    const val QUADS_LEFT_CENTER = """
        M 82 194 C 85 205 85 220 85 235 C 85 243 84 250 84 254 C 87 252 90 245 91 234 C 92 221 90 206 86 194 Z
    """
    const val QUADS_LEFT_INNER = """
        M 91 228 C 90 237 90 246 93 256 C 96 256 97 248 97 239 C 97 231 94 228 91 228 Z
    """
    const val QUADS_RIGHT_OUTER = """
        M 124 196 C 131 209 133 224 130 239 C 128 248 126 254 125 259 C 122 254 119 243 118 232 C 117 219 117 206 120 196 Z
    """
    const val QUADS_RIGHT_CENTER = """
        M 118 194 C 115 205 115 220 115 235 C 115 243 116 250 116 254 C 113 252 110 245 109 234 C 108 221 110 206 114 194 Z
    """
    const val QUADS_RIGHT_INNER = """
        M 109 228 C 110 237 110 246 107 256 C 104 256 103 248 103 239 C 103 231 106 228 109 228 Z
    """

    // Calves Front
    const val CALVES_FRONT_LEFT = """
        M 75 273 C 71 286 72 301 76 316 C 77 325 78 334 78 342 C 81 342 83 331 83 318 C 83 303 82 288 80 273 Z
    """
    const val CALVES_FRONT_LEFT_MEDIAL = """
        M 83 275 C 85 288 87 303 87 316 C 87 327 85 336 84 342 C 86 342 88 331 89 320 C 91 305 89 290 87 275 Z
    """
    const val CALVES_FRONT_RIGHT = """
        M 125 273 C 129 286 128 301 124 316 C 123 325 122 334 122 342 C 119 342 117 331 117 318 C 117 303 118 288 120 273 Z
    """
    const val CALVES_FRONT_RIGHT_MEDIAL = """
        M 117 275 C 115 288 113 303 113 316 C 113 327 115 336 116 342 C 114 342 112 331 111 320 C 109 305 111 290 113 275 Z
    """

    // BACK MUSCLE GROUPS
    const val TRAPS_LEFT = """
        M 99 60 C 94 66 86 72 74 79 C 78 88 85 99 98 112 L 99 60 Z
    """
    const val TRAPS_RIGHT = """
        M 101 60 C 106 66 114 72 126 79 C 122 88 115 99 102 112 L 101 60 Z
    """

    const val LATS_LEFT = """
        M 72 86 C 64 97 63 108 64 119 C 66 130 68 143 74 154 C 81 154 88 148 92 141 C 96 132 98 121 98 113 C 88 102 80 93 72 86 Z
    """
    const val LATS_RIGHT = """
        M 128 86 C 136 97 137 108 136 119 C 134 130 132 143 126 154 C 119 154 112 148 108 141 C 104 132 102 121 102 113 C 112 102 120 93 128 86 Z
    """

    const val LOWER_BACK_LEFT = "M 98 126 C 96 135 94 148 94 161 C 94 170 96 176 98 180 Z"
    const val LOWER_BACK_RIGHT = "M 102 126 C 104 135 106 148 106 161 C 106 170 104 176 102 180 Z"

    const val SHOULDERS_BACK_LEFT = """
        M 70 88 C 60 84 50 90 46 99 C 42 107 43 116 47 124 C 52 119 58 112 63 105 C 67 97 69 91 70 88 Z
    """
    const val SHOULDERS_BACK_RIGHT = """
        M 130 88 C 140 84 150 90 154 99 C 158 107 157 116 153 124 C 148 119 142 112 137 105 C 133 97 131 91 130 88 Z
    """

    const val TRICEPS_LEFT = """
        M 47 125 C 43 134 44 143 48 152 C 52 152 56 147 58 139 C 60 130 58 123 54 119 C 51 119 48 121 47 125 Z
    """
    const val TRICEPS_RIGHT = """
        M 153 125 C 157 134 156 143 152 152 C 148 152 144 147 142 139 C 140 130 142 123 146 119 C 149 119 152 121 153 125 Z
    """

    const val GLUTES_LEFT = """
        M 98 180 C 88 178 76 182 72 190 C 68 200 70 210 78 218 C 86 224 94 222 98 214 Z
    """
    const val GLUTES_RIGHT = """
        M 102 180 C 112 178 124 182 128 190 C 132 200 130 210 122 218 C 114 224 106 222 102 214 Z
    """

    const val HAMSTRINGS_LEFT = """
        M 76 222 C 72 234 72 246 74 256 C 80 256 85 254 87 246 C 89 236 90 226 89 218 C 83 220 78 221 76 222 Z
    """
    const val HAMSTRINGS_LEFT_MEDIAL = "M 88 220 C 91 230 93 240 94 252 C 97 252 98 244 98 234 C 98 226 95 222 88 220 Z"
    const val HAMSTRINGS_RIGHT = """
        M 124 222 C 128 234 128 246 126 256 C 120 256 115 254 113 246 C 111 236 110 226 111 218 C 117 220 122 221 124 222 Z
    """
    const val HAMSTRINGS_RIGHT_MEDIAL = "M 112 220 C 109 230 107 240 106 252 C 103 252 102 244 102 234 C 102 226 105 222 112 220 Z"

    const val CALVES_BACK_LEFT = """
        M 75 272 C 70 284 70 298 74 312 C 78 326 79 338 80 348 C 83 348 85 336 85 320 C 86 304 84 288 82 272 Z
    """
    const val CALVES_BACK_LEFT_MEDIAL = """
        M 84 272 C 86 286 88 300 89 314 C 89 326 87 338 86 348 C 88 348 91 336 92 320 C 94 304 92 288 90 272 Z
    """
    const val CALVES_BACK_RIGHT = """
        M 125 272 C 130 284 130 298 126 312 C 122 326 121 338 120 348 C 117 348 115 336 115 320 C 114 304 116 288 118 272 Z
    """
    const val CALVES_BACK_RIGHT_MEDIAL = """
        M 116 272 C 114 286 112 300 111 314 C 111 326 113 338 114 348 C 112 348 109 336 108 320 C 106 304 108 288 110 272 Z
    """
}

/**
 * Pre-parsed cached Compose Path instances for zero-allocation rendering.
 */
class ParsedAnatomy {
    private val parser = PathParser()
    private fun p(svg: String): Path {
        parser.clear()
        return parser.parsePathString(svg.trim()).toPath()
    }

    val silhouette = p(AnatomicalPaths.SILHOUETTE)

    // Front
    val chest = listOf(p(AnatomicalPaths.CHEST_LEFT), p(AnatomicalPaths.CHEST_RIGHT))
    val shouldersFront = listOf(p(AnatomicalPaths.SHOULDERS_FRONT_LEFT), p(AnatomicalPaths.SHOULDERS_FRONT_RIGHT))
    val biceps = listOf(p(AnatomicalPaths.BICEPS_LEFT), p(AnatomicalPaths.BICEPS_RIGHT))
    val forearmsFront = listOf(p(AnatomicalPaths.FOREARMS_LEFT), p(AnatomicalPaths.FOREARMS_RIGHT))
    val absCore = listOf(
        p(AnatomicalPaths.ABS_L1), p(AnatomicalPaths.ABS_R1),
        p(AnatomicalPaths.ABS_L2), p(AnatomicalPaths.ABS_R2),
        p(AnatomicalPaths.ABS_L3), p(AnatomicalPaths.ABS_R3),
        p(AnatomicalPaths.OBLIQUES_LEFT), p(AnatomicalPaths.OBLIQUES_RIGHT)
    )
    val quads = listOf(
        p(AnatomicalPaths.QUADS_LEFT_OUTER), p(AnatomicalPaths.QUADS_LEFT_CENTER), p(AnatomicalPaths.QUADS_LEFT_INNER),
        p(AnatomicalPaths.QUADS_RIGHT_OUTER), p(AnatomicalPaths.QUADS_RIGHT_CENTER), p(AnatomicalPaths.QUADS_RIGHT_INNER)
    )
    val calvesFront = listOf(
        p(AnatomicalPaths.CALVES_FRONT_LEFT), p(AnatomicalPaths.CALVES_FRONT_LEFT_MEDIAL),
        p(AnatomicalPaths.CALVES_FRONT_RIGHT), p(AnatomicalPaths.CALVES_FRONT_RIGHT_MEDIAL)
    )

    // Back
    val trapsBack = listOf(p(AnatomicalPaths.TRAPS_LEFT), p(AnatomicalPaths.TRAPS_RIGHT))
    val latsBack = listOf(p(AnatomicalPaths.LATS_LEFT), p(AnatomicalPaths.LATS_RIGHT))
    val lowerBack = listOf(p(AnatomicalPaths.LOWER_BACK_LEFT), p(AnatomicalPaths.LOWER_BACK_RIGHT))
    val shouldersBack = listOf(p(AnatomicalPaths.SHOULDERS_BACK_LEFT), p(AnatomicalPaths.SHOULDERS_BACK_RIGHT))
    val triceps = listOf(p(AnatomicalPaths.TRICEPS_LEFT), p(AnatomicalPaths.TRICEPS_RIGHT))
    val glutes = listOf(p(AnatomicalPaths.GLUTES_LEFT), p(AnatomicalPaths.GLUTES_RIGHT))
    val hamstrings = listOf(
        p(AnatomicalPaths.HAMSTRINGS_LEFT), p(AnatomicalPaths.HAMSTRINGS_LEFT_MEDIAL),
        p(AnatomicalPaths.HAMSTRINGS_RIGHT), p(AnatomicalPaths.HAMSTRINGS_RIGHT_MEDIAL)
    )
    val calvesBack = listOf(
        p(AnatomicalPaths.CALVES_BACK_LEFT), p(AnatomicalPaths.CALVES_BACK_LEFT_MEDIAL),
        p(AnatomicalPaths.CALVES_BACK_RIGHT), p(AnatomicalPaths.CALVES_BACK_RIGHT_MEDIAL)
    )

    companion object {
        val INSTANCE by lazy { ParsedAnatomy() }
    }
}

/**
 * Color Palette mapping based on muscle recovery status percentage.
 */
fun recoveryHeatColor(recoveryPct: Int): Color = when {
    recoveryPct < 50 -> Color(0xFFFF2D55) // Crimson (Fatigued)
    recoveryPct < 80 -> Color(0xFFFF6B8B) // Rose (Recovering)
    else -> Color(0xFF383B4A)             // Fresh slate (matches dark silhouette)
}

val BodyBaseSilhouette = Color(0xFF242633)
val BodyCreaseColor = Color(0xFF14151C)

/**
 * Draws the realistic anatomical human body view onto a Compose Canvas.
 */
fun DrawScope.drawAnatomicalBody(
    isFront: Boolean,
    recoveryMap: Map<String, Int>,
    highlightedMuscle: String? = null,
    paths: ParsedAnatomy = ParsedAnatomy.INSTANCE
) {
    val refW = 200f
    val refH = 400f
    val s = minOf(size.width / refW, size.height / refH)
    val dx = (size.width - refW * s) / 2f
    val dy = (size.height - refH * s) / 2f

    fun pct(muscle: String): Int = recoveryMap[muscle] ?: 100

    fun muscleColor(muscle: String): Color {
        if (highlightedMuscle != null && highlightedMuscle.equals(muscle, ignoreCase = true)) {
            return Color(0xFFFF375F)
        }
        return recoveryHeatColor(pct(muscle))
    }

    scale(s, s, Offset.Zero) {
        val tx = dx / s
        val ty = dy / s
        drawContext.canvas.save()
        drawContext.canvas.translate(tx, ty)

        // 1. Base Silhouette
        drawPath(paths.silhouette, color = BodyBaseSilhouette, style = Fill)
        drawPath(paths.silhouette, color = BodyCreaseColor, style = Stroke(width = 1.6f))

        if (isFront) {
            // FRONT VIEW MUSCLES
            // Chest
            val chestCol = muscleColor("Chest")
            paths.chest.forEach {
                drawPath(it, chestCol, style = Fill)
                drawPath(it, BodyCreaseColor, style = Stroke(width = 1.2f))
            }

            // Shoulders
            val shCol = muscleColor("Shoulders")
            paths.shouldersFront.forEach {
                drawPath(it, shCol, style = Fill)
                drawPath(it, BodyCreaseColor, style = Stroke(width = 1.2f))
            }

            // Biceps
            val biCol = muscleColor("Biceps")
            paths.biceps.forEach {
                drawPath(it, biCol, style = Fill)
                drawPath(it, BodyCreaseColor, style = Stroke(width = 1.2f))
            }

            // Forearms (linked to Biceps/grip recovery)
            val faCol = recoveryHeatColor(minOf(pct("Biceps"), pct("Back")))
            paths.forearmsFront.forEach {
                drawPath(it, faCol, style = Fill)
                drawPath(it, BodyCreaseColor, style = Stroke(width = 1.0f))
            }

            // Core / Abs
            val coreCol = muscleColor("Core")
            paths.absCore.forEach {
                drawPath(it, coreCol, style = Fill)
                drawPath(it, BodyCreaseColor, style = Stroke(width = 1.0f))
            }

            // Quadriceps
            val quadCol = muscleColor("Quads")
            paths.quads.forEach {
                drawPath(it, quadCol, style = Fill)
                drawPath(it, BodyCreaseColor, style = Stroke(width = 1.2f))
            }

            // Calves Front
            val calfCol = muscleColor("Calves")
            paths.calvesFront.forEach {
                drawPath(it, calfCol, style = Fill)
                drawPath(it, BodyCreaseColor, style = Stroke(width = 1.0f))
            }

            // Clavicle & Sternum crease lines
            drawLine(BodyCreaseColor, Offset(100f, 83f), Offset(100f, 120f), strokeWidth = 1.4f)
        } else {
            // BACK VIEW MUSCLES
            // Traps & Lats (Back)
            val backCol = muscleColor("Back")
            paths.trapsBack.forEach {
                drawPath(it, backCol, style = Fill)
                drawPath(it, BodyCreaseColor, style = Stroke(width = 1.2f))
            }
            paths.latsBack.forEach {
                drawPath(it, backCol, style = Fill)
                drawPath(it, BodyCreaseColor, style = Stroke(width = 1.2f))
            }
            paths.lowerBack.forEach {
                drawPath(it, backCol, style = Fill)
                drawPath(it, BodyCreaseColor, style = Stroke(width = 1.0f))
            }

            // Rear Deltoids
            val shCol = muscleColor("Shoulders")
            paths.shouldersBack.forEach {
                drawPath(it, shCol, style = Fill)
                drawPath(it, BodyCreaseColor, style = Stroke(width = 1.2f))
            }

            // Triceps
            val triCol = muscleColor("Triceps")
            paths.triceps.forEach {
                drawPath(it, triCol, style = Fill)
                drawPath(it, BodyCreaseColor, style = Stroke(width = 1.2f))
            }

            // Glutes
            val gluteCol = muscleColor("Glutes")
            paths.glutes.forEach {
                drawPath(it, gluteCol, style = Fill)
                drawPath(it, BodyCreaseColor, style = Stroke(width = 1.2f))
            }

            // Hamstrings
            val hamCol = muscleColor("Hamstrings")
            paths.hamstrings.forEach {
                drawPath(it, hamCol, style = Fill)
                drawPath(it, BodyCreaseColor, style = Stroke(width = 1.2f))
            }

            // Calves Back
            val calfCol = muscleColor("Calves")
            paths.calvesBack.forEach {
                drawPath(it, calfCol, style = Fill)
                drawPath(it, BodyCreaseColor, style = Stroke(width = 1.2f))
            }

            // Spine line
            drawLine(BodyCreaseColor, Offset(100f, 68f), Offset(100f, 182f), strokeWidth = 1.4f)
        }

        drawContext.canvas.restore()
    }
}

/**
 * Mini Anatomical Muscle Card matching User Image 3.
 * Renders a square-rounded cell with a cropped anatomical graphic highlighting that muscle,
 * the muscle display title, and colored recovery percentage.
 */
@Composable
fun AnatomicalMuscleCard(
    muscle: String,
    recoveryPct: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isBackMuscle = muscle in listOf("Back", "Triceps", "Glutes", "Hamstrings")
    val recoveryColor = recoveryHeatColor(recoveryPct)

    Surface(
        onClick = onClick,
        modifier = modifier
            .width(108.dp)
            .height(132.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1C1D24),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF2D55))
                 else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282A36))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Mini Canvas showing the highlighted muscle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF14151C)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                    drawAnatomicalBody(
                        isFront = !isBackMuscle,
                        recoveryMap = mapOf(muscle to recoveryPct),
                        highlightedMuscle = muscle
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Display Title & Percentage
            val displayTitle = when (muscle) {
                "Quads" -> "Quadriceps"
                "Core" -> "Abs"
                else -> muscle
            }

            Text(
                displayTitle,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                textAlign = TextAlign.Center
            )

            Text(
                "$recoveryPct%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (recoveryPct < 50) Color(0xFFFF2D55)
                        else if (recoveryPct < 80) Color(0xFFFF6B8B)
                        else Color(0xFF8E8E93),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Full Muscle Recovery Component.
 * Integrates the heroic anatomical body visualization (with Front/Back toggle or side-by-side),
 * top recovery metrics ("Days Since Last Workout", "Fresh Muscle Groups"), and the Image 3 muscle cards grid.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MuscleRecoveryView(
    statuses: List<MuscleStatus>,
    daysSinceLastWorkout: Int = 0,
    defaultExpanded: Boolean = false,
    onMuscleSelected: ((String) -> Unit)? = null
) {
    val byName = remember(statuses) { statuses.associateBy { it.muscle } }
    fun pct(m: String) = byName[m]?.recoveryPercent ?: 100
    val recoveryMap = remember(statuses) { statuses.associate { it.muscle to it.recoveryPercent } }

    val avg = remember(statuses) {
        if (statuses.isEmpty()) 100 else statuses.sumOf { it.recoveryPercent } / statuses.size
    }
    val freshCount = remember(statuses) { statuses.count { it.recoveryPercent >= 80 } }

    var isFrontView by remember { mutableStateOf(true) }
    var selectedMuscle by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(defaultExpanded) }

    PanelCard {
        // 1. Top Recovery Stats Header
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                SmallLabel("MUSCLE RECOVERY")
                Text(
                    "Body Recovery · $avg%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.heightIn(min=48.dp)
            ) {
                Text(if (expanded) "Collapse ▴" else "Details ▾")
            }
        }

        // Days Since Last Workout & Fresh Muscle Groups Counter (Image 2)
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF14151C), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    "$daysSinceLastWorkout",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                SmallLabel("DAYS SINCE LAST WORKOUT")
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$freshCount",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF30D158)
                )
                SmallLabel("FRESH MUSCLE GROUPS")
            }
        }

        // Heatmap Legend
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(10.dp).background(Color(0xFFFF2D55), CircleShape))
                SmallLabel("Fatigued (<50%)")
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(10.dp).background(Color(0xFFFF6B8B), CircleShape))
                SmallLabel("Recovering (50–79%)")
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(10.dp).background(Color(0xFF383B4A), CircleShape))
                SmallLabel("Fresh (80–100%)")
            }
        }

        // 2. Anatomical Body Graphic Area
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val isWide = maxWidth >= 500.dp

            if (isWide) {
                // Wide / Tablet Landscape: Display Front and Back Side-by-Side!
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color(0xFF121318), RoundedCornerShape(16.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SmallLabel("FRONT VIEW")
                        Canvas(Modifier.fillMaxSize()) {
                            drawAnatomicalBody(
                                isFront = true,
                                recoveryMap = recoveryMap,
                                highlightedMuscle = selectedMuscle
                            )
                        }
                    }
                    VerticalDivider(color = Color(0xFF282A36), modifier = Modifier.padding(vertical = 16.dp))
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SmallLabel("BACK VIEW")
                        Canvas(Modifier.fillMaxSize()) {
                            drawAnatomicalBody(
                                isFront = false,
                                recoveryMap = recoveryMap,
                                highlightedMuscle = selectedMuscle
                            )
                        }
                    }
                }
            } else {
                // Mobile Portrait: Single Detailed Body with Flip Toggle Button (matching Image 2)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .background(Color(0xFF121318), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    // Body Canvas
                    Canvas(Modifier.fillMaxSize()) {
                        drawAnatomicalBody(
                            isFront = isFrontView,
                            recoveryMap = recoveryMap,
                            highlightedMuscle = selectedMuscle
                        )
                    }

                    // View Indicator Pill (Top Center)
                    Surface(
                        modifier = Modifier.align(Alignment.TopCenter),
                        color = Color(0xFF1C1D24),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282A36))
                    ) {
                        Text(
                            if (isFrontView) "FRONT VIEW" else "BACK VIEW",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8E8E93)
                        )
                    }

                    // Flip View Button (Bottom Left, Image 2)
                    FilledIconButton(
                        onClick = { isFrontView = !isFrontView },
                        modifier = Modifier
                            .size(46.dp)
                            .align(Alignment.BottomStart),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFF2C2D3A),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = "Flip Front/Back", modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        // Active Selected Muscle Banner
        if (selectedMuscle != null) {
            val st = byName[selectedMuscle]
            if (st != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1F202B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF2D55))
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val title = when (st.muscle) {
                                "Quads" -> "Quadriceps"
                                "Core" -> "Abs & Obliques"
                                else -> st.muscle
                            }
                            Text(title, fontWeight = FontWeight.Bold, color = Color.White)
                            val info = if (st.lastTrainedHoursAgo != null)
                                "${st.recoveryPercent}% · ${st.workingSetsLast7Days} sets in 7d · Trained ${st.lastTrainedHoursAgo}h ago"
                            else "${st.recoveryPercent}% · Fully Recovered (0 sets in 7d)"
                            SmallLabel(info)
                        }
                        IconButton(onClick = { selectedMuscle = null }) {
                            Text("✕", color = Color(0xFF8E8E93), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Muscle Groups Grid matching Image 3
        SmallLabel("TARGET MUSCLE RECOVERY (TAP TO HIGHLIGHT)")
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            statuses.forEach { st ->
                AnatomicalMuscleCard(
                    muscle = st.muscle,
                    recoveryPct = st.recoveryPercent,
                    isSelected = selectedMuscle == st.muscle,
                    onClick = {
                        selectedMuscle = if (selectedMuscle == st.muscle) null else st.muscle
                        onMuscleSelected?.invoke(st.muscle)
                    }
                )
            }
        }

        // Granular Progress Bars (when expanded)
        if (expanded) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            SmallLabel("DETAILED RECOVERY TIMELINE")
            statuses.forEach { st ->
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(st.muscle, fontWeight = FontWeight.SemiBold)
                        val recText = if (st.lastTrainedHoursAgo != null)
                            "${st.recoveryPercent}% · ${st.workingSetsLast7Days} sets (7d) · ${st.lastTrainedHoursAgo}h ago"
                        else "${st.recoveryPercent}% · Fully recovered"
                        SmallLabel(recText)
                    }
                    LinearProgressIndicator(
                        progress = { st.recoveryPercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = recoveryHeatColor(st.recoveryPercent),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}
