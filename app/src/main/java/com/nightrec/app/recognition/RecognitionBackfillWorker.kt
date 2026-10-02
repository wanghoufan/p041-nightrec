package com.nightrec.app.recognition

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.nightrec.app.NightRecApplication

/**
 * 后台补识别 Worker（T058）。
 *
 * 在断网/失败窗口恢复后（需要网络）对未解决的 unknown range 做幂等补识别。
 * 可指定单个 session，缺省则对所有已结束（PROCESSING/READY）session 运行。
 */
class RecognitionBackfillWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as NightRecApplication
        val requested = inputData.getLong(KEY_SESSION_ID, -1L)
        val ids = if (requested > 0) {
            listOf(requested)
        } else {
            app.database.sessions().all()
                .filter { it.state == "PROCESSING" || it.state == "READY" }
                .map { it.id }
        }
        val backfill = RecognitionBackfill(app.database, app.recognitionEngine, app.recognitionBudget, app.clock)
        for (id in ids) {
            val result = backfill.backfill(id)
            if (result.budgetExhausted) break
        }
        return Result.success()
    }

    companion object {
        const val KEY_SESSION_ID = "sessionId"

        /** 幂等入队：同一 session 只保留一个补识别任务（KEEP 策略）。 */
        fun enqueue(context: Context, sessionId: Long) {
            val request = OneTimeWorkRequestBuilder<RecognitionBackfillWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInputData(workDataOf(KEY_SESSION_ID to sessionId))
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("recognition-backfill-$sessionId", ExistingWorkPolicy.KEEP, request)
        }
    }
}