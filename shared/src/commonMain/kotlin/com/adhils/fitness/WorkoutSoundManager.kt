package com.adhils.fitness

/**
 * Shared workout sound cue manager bridging to platform audio capabilities.
 */
object WorkoutSoundManager {
    fun playCountdownBeep() {
        PlatformSound.playCountdownBeep()
    }

    fun playTimerFinishedChime() {
        PlatformSound.playTimerFinishedChime()
    }
}
