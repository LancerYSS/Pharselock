package com.denie.phraselock

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Включает/выключает ежечасное обновление «фразы часа» в уведомлении. */
object PhraseScheduler {
    private const val WORK_NAME = "phrase_of_hour"

    fun enable(context: Context) {
        NotificationHelper.showCurrentPhrase(context)
        val request = PeriodicWorkRequestBuilder<PhraseWorker>(1, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun disable(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
        NotificationHelper.cancel(context)
    }

    fun isEnabled(context: Context): Boolean {
        val prefs = context.applicationContext
            .getSharedPreferences("settings", Context.MODE_PRIVATE)
        return prefs.getBoolean("notifications_enabled", false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.applicationContext
            .getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("notifications_enabled", enabled)
            .apply()
    }
}
