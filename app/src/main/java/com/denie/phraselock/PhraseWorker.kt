package com.denie.phraselock

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/** Раз в час обновляет «фразу часа» в постоянном уведомлении. */
class PhraseWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        NotificationHelper.showCurrentPhrase(applicationContext)
        return Result.success()
    }
}
