package com.example.shealthprobe

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object Scheduler {
    private const val UNIQUE_DAILY = "shealth_daily_sync"

    fun scheduleNext(context: Context) {
        if (!AppPrefs.autoEnabled(context)) {
            cancel(context)
            return
        }

        val now = ZonedDateTime.now()
        var next = now.withHour(AppPrefs.hour(context))
            .withMinute(AppPrefs.minute(context))
            .withSecond(0)
            .withNano(0)

        if (!next.isAfter(now)) next = next.plusDays(1)
        val delayMs = Duration.between(now, next).toMillis().coerceAtLeast(0)

        val req = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_DAILY,
            ExistingWorkPolicy.REPLACE,
            req
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_DAILY)
    }

    fun runNow(context: Context) {
        val req = OneTimeWorkRequestBuilder<SyncWorker>().build()
        WorkManager.getInstance(context).enqueue(req)
    }
}
