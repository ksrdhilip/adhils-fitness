package com.adhils.fitness

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import com.adhils.fitness.core.Session
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * HealthConnectManager handles direct, native communication with Android Health Connect
 * and Samsung Health (which uses Health Connect as its data sharing layer).
 *
 * Reads user body stats (height, weight, body fat) and Galaxy Watch vitals (heartbeats,
 * active calories burned, steps, exercises), and writes completed strength workouts.
 */
object AndroidHealthConnectManager {
    private const val TAG = "HealthConnectManager"

    const val SAMSUNG_HEALTH_PACKAGE = "com.sec.android.app.shealth"
    const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"

    val READ_PERMISSIONS = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(HeightRecord::class),
        HealthPermission.getReadPermission(BodyFatRecord::class),
        HealthPermission.getReadPermission(LeanBodyMassRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    )

    val WRITE_PERMISSIONS = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class)
    )

    val ALL_PERMISSIONS = READ_PERMISSIONS + WRITE_PERMISSIONS

    data class BodyStats(
        val weightKg: Double? = null,
        val weightLb: Double? = null,
        val heightMeters: Double? = null,
        val heightFeet: Int? = null,
        val heightInches: Int? = null,
        val bodyFatPercentage: Double? = null,
        val lastUpdated: Instant? = null,
        val sourceApp: String = "Samsung Health"
    )

    data class WearableVitals(
        val liveHeartRateBpm: Int? = null,
        val minHeartRateBpm: Int? = null,
        val maxHeartRateBpm: Int? = null,
        val caloriesBurnedToday: Int = 0,
        val stepsToday: Long = 0,
        val activeMinutesToday: Long = 0,
        val lastSyncTime: Instant = Instant.now(),
        val isFromWearable: Boolean = true,
        val deviceName: String = "Galaxy Watch"
    )

    /**
     * Check if Health Connect is available on the device.
     */
    fun getAvailability(context: Context): Int {
        return try {
            HealthConnectClient.getSdkStatus(context)
        } catch (e: Exception) {
            Log.w(TAG, "Error checking Health Connect availability", e)
            HealthConnectClient.SDK_UNAVAILABLE
        }
    }

    fun isAvailable(context: Context): Boolean {
        return getAvailability(context) == HealthConnectClient.SDK_AVAILABLE
    }

    private fun getClient(context: Context): HealthConnectClient? {
        return try {
            if (isAvailable(context)) HealthConnectClient.getOrCreate(context) else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize HealthConnectClient", e)
            null
        }
    }

    /**
     * Check if Samsung Health application is installed on this device.
     */
    fun isSamsungHealthInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(SAMSUNG_HEALTH_PACKAGE, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Get the set of currently granted permissions.
     */
    suspend fun getGrantedPermissions(context: Context): Set<String> {
        val client = getClient(context) ?: return emptySet()
        return try {
            client.permissionController.getGrantedPermissions()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get granted permissions", e)
            emptySet()
        }
    }

    /**
     * Checks if minimum necessary permissions for reading body stats and wearable vitals are granted.
     */
    suspend fun hasRequiredPermissions(context: Context): Boolean {
        val granted = getGrantedPermissions(context)
        return granted.contains(HealthPermission.getReadPermission(HeartRateRecord::class)) ||
               granted.contains(HealthPermission.getReadPermission(WeightRecord::class)) ||
               granted.contains(HealthPermission.getReadPermission(StepsRecord::class))
    }

    /**
     * Read the latest Weight, Height, and Body Fat records written by Samsung Health / Health Connect.
     */
    suspend fun readLatestBodyStats(context: Context): BodyStats? {
        val client = getClient(context) ?: return null
        return try {
            val now = Instant.now()
            val thirtyDaysAgo = now.minus(180, ChronoUnit.DAYS)
            val timeRange = TimeRangeFilter.between(thirtyDaysAgo, now)

            // Read Weight
            var weightKg: Double? = null
            var weightLb: Double? = null
            var weightTime: Instant? = null
            try {
                val weightResponse = client.readRecords(
                    ReadRecordsRequest(
                        recordType = WeightRecord::class,
                        timeRangeFilter = timeRange,
                        ascendingOrder = false,
                        pageSize = 1
                    )
                )
                weightResponse.records.firstOrNull()?.let { record ->
                    weightKg = record.weight.inKilograms
                    weightLb = record.weight.inPounds
                    weightTime = record.time
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not read WeightRecord", e)
            }

            // Read Height
            var heightMeters: Double? = null
            var heightFeet: Int? = null
            var heightInches: Int? = null
            try {
                val heightResponse = client.readRecords(
                    ReadRecordsRequest(
                        recordType = HeightRecord::class,
                        timeRangeFilter = timeRange,
                        ascendingOrder = false,
                        pageSize = 1
                    )
                )
                heightResponse.records.firstOrNull()?.let { record ->
                    heightMeters = record.height.inMeters
                    val totalInches = record.height.inInches.roundToInt()
                    heightFeet = totalInches / 12
                    heightInches = totalInches % 12
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not read HeightRecord", e)
            }

            // Read Body Fat % (Galaxy Watch BIA sensor)
            var bodyFat: Double? = null
            try {
                val bodyFatResponse = client.readRecords(
                    ReadRecordsRequest(
                        recordType = BodyFatRecord::class,
                        timeRangeFilter = timeRange,
                        ascendingOrder = false,
                        pageSize = 1
                    )
                )
                bodyFatResponse.records.firstOrNull()?.let { record ->
                    bodyFat = record.percentage.value
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not read BodyFatRecord", e)
            }

            if (weightKg != null || heightMeters != null || bodyFat != null) {
                BodyStats(
                    weightKg = weightKg,
                    weightLb = weightLb,
                    heightMeters = heightMeters,
                    heightFeet = heightFeet,
                    heightInches = heightInches,
                    bodyFatPercentage = bodyFat,
                    lastUpdated = weightTime ?: now
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading body stats", e)
            null
        }
    }

    /**
     * Read today's wearable activity:
     * - Heart rate (bpm) and samples recorded by Galaxy Watch
     * - Calories burned today (Active + Basal)
     * - Daily step count
     * - Past exercise sessions
     */
    suspend fun readTodayWearableVitals(context: Context): WearableVitals {
        val client = getClient(context)
        if (client == null) {
            return WearableVitals(deviceName = if (isSamsungHealthInstalled(context)) "Samsung Galaxy Watch" else "Connected Wearable")
        }

        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val now = Instant.now()
        val todayFilter = TimeRangeFilter.between(startOfDay, now)

        var liveHeartRate: Int? = null
        var minHr: Int? = null
        var maxHr: Int? = null
        var totalCalories = 0
        var totalSteps = 0L

        // 1. Read Heart Rate (Galaxy Watch continuous or workout HR)
        try {
            val hrResponse = client.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(now.minus(4, ChronoUnit.HOURS), now),
                    ascendingOrder = false,
                    pageSize = 10
                )
            )
            val allSamples = hrResponse.records.flatMap { it.samples }
            if (allSamples.isNotEmpty()) {
                liveHeartRate = allSamples.first().beatsPerMinute.toInt()
                minHr = allSamples.minOf { it.beatsPerMinute }.toInt()
                maxHr = allSamples.maxOf { it.beatsPerMinute }.toInt()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not read HeartRateRecord", e)
        }

        // 2. Read Active Calories Burned
        try {
            val activeCalResponse = client.readRecords(
                ReadRecordsRequest(
                    recordType = ActiveCaloriesBurnedRecord::class,
                    timeRangeFilter = todayFilter
                )
            )
            val sumActive = activeCalResponse.records.sumOf { it.energy.inKilocalories }
            totalCalories += sumActive.roundToInt()
        } catch (e: Exception) {
            Log.w(TAG, "Could not read ActiveCaloriesBurnedRecord", e)
        }

        // 3. Read Total Calories Burned (if available and higher)
        try {
            val totalCalResponse = client.readRecords(
                ReadRecordsRequest(
                    recordType = TotalCaloriesBurnedRecord::class,
                    timeRangeFilter = todayFilter
                )
            )
            val sumTotal = totalCalResponse.records.sumOf { it.energy.inKilocalories }.roundToInt()
            if (sumTotal > totalCalories) {
                totalCalories = sumTotal
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not read TotalCaloriesBurnedRecord", e)
        }

        // 4. Read Steps
        try {
            val stepsResponse = client.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = todayFilter
                )
            )
            totalSteps = stepsResponse.records.sumOf { it.count }
        } catch (e: Exception) {
            Log.w(TAG, "Could not read StepsRecord", e)
        }

        return WearableVitals(
            liveHeartRateBpm = liveHeartRate,
            minHeartRateBpm = minHr,
            maxHeartRateBpm = maxHr,
            caloriesBurnedToday = totalCalories,
            stepsToday = totalSteps,
            lastSyncTime = now,
            deviceName = if (isSamsungHealthInstalled(context)) "Galaxy Watch6 (Samsung Health)" else "Android Wearable"
        )
    }

    /**
     * Write completed ADhils Fitness workout session to Health Connect.
     * This syncs into Samsung Health on Galaxy devices!
     */
    suspend fun writeWorkoutSession(
        context: Context,
        session: Session,
        caloriesBurned: Int
    ): Boolean {
        val client = getClient(context) ?: return false
        return try {
            val start = Instant.ofEpochMilli(session.startedAt)
            val end = session.finishedAt?.let { Instant.ofEpochMilli(it) } ?: Instant.now()

            val exerciseSession = ExerciseSessionRecord(
                startTime = start,
                startZoneOffset = ZoneId.systemDefault().rules.getOffset(start),
                endTime = end,
                endZoneOffset = ZoneId.systemDefault().rules.getOffset(end),
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
                title = "ADhils Fitness: Strength Workout",
                notes = "Completed ${session.results.size} sets. Logged with ADhils Fitness Coach.",
                metadata = Metadata()
            )

            val caloriesRecord = ActiveCaloriesBurnedRecord(
                startTime = start,
                startZoneOffset = ZoneId.systemDefault().rules.getOffset(start),
                endTime = end,
                endZoneOffset = ZoneId.systemDefault().rules.getOffset(end),
                energy = Energy.kilocalories(caloriesBurned.toDouble()),
                metadata = Metadata()
            )

            var success = false
            try {
                client.insertRecords(listOf<Record>(exerciseSession, caloriesRecord))
                Log.i(TAG, "Successfully inserted workout session & active calories to Health Connect / Samsung Health")
                success = true
            } catch (e: Exception) {
                Log.w(TAG, "Batch insert failed ($e). Attempting exercise session insert alone...", e)
                try {
                    client.insertRecords(listOf<Record>(exerciseSession))
                    Log.i(TAG, "Successfully inserted exercise session to Health Connect")
                    success = true
                } catch (e2: Exception) {
                    Log.e(TAG, "Failed to write exercise session to Health Connect", e2)
                }
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prepare workout session records for Health Connect", e)
            false
        }
    }

    /**
     * Intent to open the Health Connect settings screen.
     */
    fun createSettingsIntent(context: Context): Intent {
        val intent = Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
        return if (intent.resolveActivity(context.packageManager) != null) {
            intent
        } else {
            // Fallback to Samsung Health or Play Store
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$HEALTH_CONNECT_PACKAGE"))
        }
    }

    /**
     * Intent to open the Samsung Health application.
     */
    fun createSamsungHealthIntent(context: Context): Intent? {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(SAMSUNG_HEALTH_PACKAGE)
        return launchIntent
    }
}
