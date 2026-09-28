package com.example.shealthprobe

import android.content.Context
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalDateFilter
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class HealthReader(context: Context) {
    val store: HealthDataStore = HealthDataService.getStore(context.applicationContext)

    val requiredPermissions: Set<Permission> = setOf(
        Permission.of(DataTypes.STEPS, AccessType.READ),
        Permission.of(DataTypes.ACTIVITY_SUMMARY, AccessType.READ),
        Permission.of(DataTypes.SLEEP, AccessType.READ),
        Permission.of(DataTypes.FLOORS_CLIMBED, AccessType.READ),
        Permission.of(DataTypes.WATER_INTAKE, AccessType.READ),
        Permission.of(DataTypes.NUTRITION, AccessType.READ),
        Permission.of(DataTypes.HEART_RATE, AccessType.READ)
    )

    suspend fun hasAllPermissions(): Boolean =
        store.getGrantedPermissions(requiredPermissions).containsAll(requiredPermissions)

    suspend fun readTodayPayload(): MutableMap<String, Any> {
        val today = LocalDate.now()
        val start = today.atStartOfDay()
        val end = LocalDateTime.now()
        val localFilter = LocalTimeFilter.of(start, end)

        val payload = linkedMapOf<String, Any>(
            "date" to today.toString(),
            "source" to "samsung"
        )

        runCatching { readSteps(localFilter) }.getOrNull()?.let { payload["steps"] = it }
        runCatching { readDistanceMeters(localFilter) }.getOrNull()?.let { payload["distance_m"] = it }
        runCatching { readActiveMinutes(localFilter) }.getOrNull()?.let { payload["active_minutes"] = it }
        runCatching { readActiveCalories(localFilter) }.getOrNull()?.let { payload["active_energy"] = it }
        runCatching { readSleepHours(today) }.getOrNull()?.let { payload["sleep_hours"] = it }
        runCatching { readFloors(localFilter) }.getOrNull()?.let { payload["floors"] = it }
        runCatching { readWaterMl(localFilter) }.getOrNull()?.let { payload["water_ml"] = it }
        runCatching { readSugarGrams(localFilter) }.getOrNull()?.let { payload["sugar_g"] = it }

        // Samsung Health Data SDK 1.1.0 does not expose a dedicated resting-HR value,
        // and it does not expose the old standalone caffeine-intake type. We deliberately
        // omit resting_hr and caffeine_mg instead of sending misleading derived values.
        return payload
    }

    private suspend fun readSteps(filter: LocalTimeFilter): Long {
        val request = DataType.StepsType.TOTAL.requestBuilder
            .setLocalTimeFilter(filter)
            .build()
        return store.aggregateData(request).dataList.mapNotNull { it.value }.sum()
    }

    private suspend fun readDistanceMeters(filter: LocalTimeFilter): Float {
        val request = DataType.ActivitySummaryType.TOTAL_DISTANCE.requestBuilder
            .setLocalTimeFilter(filter)
            .build()
        return store.aggregateData(request).dataList.mapNotNull { it.value }.sum()
    }

    private suspend fun readActiveCalories(filter: LocalTimeFilter): Float {
        val request = DataType.ActivitySummaryType.TOTAL_ACTIVE_CALORIES_BURNED.requestBuilder
            .setLocalTimeFilter(filter)
            .build()
        return store.aggregateData(request).dataList.mapNotNull { it.value }.sum()
    }

    private suspend fun readActiveMinutes(filter: LocalTimeFilter): Long {
        val request = DataType.ActivitySummaryType.TOTAL_ACTIVE_TIME.requestBuilder
            .setLocalTimeFilter(filter)
            .build()
        val durations = store.aggregateData(request).dataList.mapNotNull { it.value }
        return durations.sumOf { durationToMinutes(it) }
    }

    private suspend fun readSleepHours(today: LocalDate): Double {
        val request = DataType.SleepType.TOTAL_DURATION.requestBuilder
            .setLocalDateFilter(LocalDateFilter.of(today, today))
            .build()
        val durations = store.aggregateData(request).dataList.mapNotNull { it.value }
        val minutes = durations.sumOf { durationToMinutes(it) }
        return minutes / 60.0
    }

    private suspend fun readFloors(filter: LocalTimeFilter): Float {
        val request = DataType.FloorsClimbedType.TOTAL.requestBuilder
            .setLocalTimeFilter(filter)
            .build()
        return store.aggregateData(request).dataList.mapNotNull { it.value }.sum()
    }

    private suspend fun readWaterMl(filter: LocalTimeFilter): Float {
        val request = DataType.WaterIntakeType.TOTAL.requestBuilder
            .setLocalTimeFilter(filter)
            .build()
        return store.aggregateData(request).dataList.mapNotNull { it.value }.sum()
    }

    private suspend fun readSugarGrams(filter: LocalTimeFilter): Float {
        val request = DataTypes.NUTRITION.readDataRequestBuilder
            .setLocalTimeFilter(filter)
            .build()
        return store.readData(request).dataList
            .mapNotNull { it.getValue(DataType.NutritionType.SUGAR) }
            .sum()
    }

    private fun durationToMinutes(value: Any): Long {
        return when (value) {
            is Duration -> value.toMinutes()
            else -> {
                // Defensive fallback in case Samsung changes the concrete Duration wrapper.
                val method = value.javaClass.methods.firstOrNull {
                    it.name == "toMinutes" && it.parameterCount == 0
                }
                (method?.invoke(value) as? Number)?.toLong() ?: 0L
            }
        }
    }
}
