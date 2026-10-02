package com.nightrec.app.session

import android.content.Context
import com.nightrec.app.StoragePaths
import com.nightrec.app.cleanup.SpeechCleanupWorker
import com.nightrec.app.data.NightRecDatabase
import com.nightrec.app.data.SegmentState
import com.nightrec.app.data.SessionGapEntity
import com.nightrec.app.recognition.RecognitionBackfillWorker
import java.io.File

/** Processing 界面所需的分项状态（T064）。Original 可立即进入播放；派生处理独立推进。 */
data class SessionProcessingStatus(
    val segmentCount: Int,
    val sealedCount: Int,
    val damagedCount: Int,
    val unknownRemaining: Int,
    val cleanCount: Int,
    val originalBytes: Long,
    val ready: Boolean,
)

/** 结束采集后的派生处理编排（T063/T064/T082/T058）。 */
class SessionProcessing(
    private val db: NightRecDatabase,
    private val storage: StoragePaths,
) {

    /** 结束今晚后：入队补识别与保守 Clean；Original 立即可播放（无需等待派生）。 */
    fun start(context: Context, sessionId: Long) {
        RecognitionBackfillWorker.enqueue(context, sessionId)
        SpeechCleanupWorker.enqueue(context, sessionId)
    }

    suspend fun status(sessionId: Long): SessionProcessingStatus {
        val segments = db.segments().bySession(sessionId)
        val sealed = segments.count { it.state == SegmentState.SEALED }
        val damaged = segments.count { it.state == SegmentState.DAMAGED }
        val unknown = db.unrecognized().unresolved(sessionId).size
        val clean = db.cleanRanges().bySession(sessionId).count { it.assetPath != null }
        val bytes = segments.filter { it.state == SegmentState.SEALED }.sumOf { File(it.path).length().coerceAtLeast(0) }
        val open = segments.any { it.state == SegmentState.OPEN }
        return SessionProcessingStatus(
            segmentCount = segments.size,
            sealedCount = sealed,
            damagedCount = damaged,
            unknownRemaining = unknown,
            cleanCount = clean,
            originalBytes = bytes,
            ready = segments.isNotEmpty() && !open,
        )
    }

    suspend fun sessionName(sessionId: Long): String = db.sessions().byId(sessionId)?.name.orEmpty()

    suspend fun gaps(sessionId: Long): List<SessionGapEntity> = db.gaps().bySession(sessionId)
}