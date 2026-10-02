package com.nightrec.app.cleanup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.nightrec.app.NightRecApplication
import com.nightrec.app.data.CleanRangeEntity
import com.nightrec.app.data.ProcessingStatus
import com.nightrec.app.data.SegmentState
import com.nightrec.app.recovery.StorageMonitor
import java.io.File

/**
 * 保守 Clean Worker（T082）。
 * 对每个 SEALED 分片做局部讲话 ducking，派生到 `clean/v1/<sessionId>/<index>.wav`，幂等。
 * 存储告警/危险时停止派生处理；没有可信讲话窗口则保留 Original。
 */
class SpeechCleanupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as NightRecApplication
        val sessionId = inputData.getLong(KEY_SESSION_ID, -1L)
        if (sessionId <= 0) return Result.failure()

        val storage = app.storagePaths
        val monitor = StorageMonitor.of(storage.filesDir)
        if (!monitor.allowsDerivedProcessing()) return Result.success()

        val engine = ConservativeSpeechCleanupEngine()
        val segments = app.database.segments().bySession(sessionId)
            .filter { it.state == SegmentState.SEALED }

        for (segment in segments) {
            if (!monitor.allowsDerivedProcessing()) break
            if (app.database.cleanRanges().byStart(sessionId, segment.logicalStartMs) != null) continue
            val output = File(storage.sessionClean(sessionId), "%05d.wav".format(segment.index))
            val outcome = engine.process(CleanupRequest(File(segment.path), output, segment.logicalStartMs))
            if (outcome.produced) {
                app.database.cleanRanges().insert(
                    CleanRangeEntity(
                        sessionId = sessionId,
                        logicalStartMs = segment.logicalStartMs,
                        logicalEndMs = segment.logicalStartMs + segment.logicalDurationMs,
                        status = ProcessingStatus.DONE,
                        assetPath = outcome.assetPath,
                        confidence = outcome.confidence,
                    ),
                )
            }
        }
        return Result.success()
    }

    companion object {
        const val KEY_SESSION_ID = "sessionId"

        fun enqueue(context: Context, sessionId: Long) {
            val request = OneTimeWorkRequestBuilder<SpeechCleanupWorker>()
                .setInputData(workDataOf(KEY_SESSION_ID to sessionId))
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("speech-cleanup-$sessionId", ExistingWorkPolicy.KEEP, request)
        }
    }
}