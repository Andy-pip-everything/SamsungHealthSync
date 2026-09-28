package com.example.shealthprobe

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val url = AppPrefs.url(applicationContext)
        val token = AppPrefs.token(applicationContext)

        if (url.isBlank()) {
            AppPrefs.setLastStatus(applicationContext, "${LocalDateTime.now()}\nFailed: URL is empty")
            scheduleNextIfNeeded()
            return Result.failure()
        }

        return try {
            val reader = HealthReader(applicationContext)
            if (!reader.hasAllPermissions()) {
                AppPrefs.setLastStatus(
                    applicationContext,
                    "${LocalDateTime.now()}\nFailed: Samsung Health permissions missing. Open the app and grant permissions."
                )
                scheduleNextIfNeeded()
                return Result.failure()
            }

            val payload = reader.readTodayPayload()
            val json = Gson().toJson(payload)
            val response = postJson(url, token, json)

            AppPrefs.setLastStatus(
                applicationContext,
                "${LocalDateTime.now()}\nHTTP ${response.first}\nSent: $json\nResponse: ${response.second.take(800)}"
            )

            scheduleNextIfNeeded()
            if (response.first in 200..299) Result.success() else Result.retry()
        } catch (e: Throwable) {
            AppPrefs.setLastStatus(
                applicationContext,
                "${LocalDateTime.now()}\nException: ${e::class.java.simpleName}: ${e.message}"
            )
            scheduleNextIfNeeded()
            Result.retry()
        }
    }

    private fun postJson(endpoint: String, token: String, body: String): Pair<Int, String> {
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 15_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (token.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer $token")
            }
        }

        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val stream = if (code in 200..399) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
        conn.disconnect()
        return code to text
    }

    private fun scheduleNextIfNeeded() {
        if (AppPrefs.autoEnabled(applicationContext)) {
            Scheduler.scheduleNext(applicationContext)
        }
    }
}
