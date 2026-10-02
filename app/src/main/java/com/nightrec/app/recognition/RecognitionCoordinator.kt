package com.nightrec.app.recognition

import com.nightrec.app.ClockProvider
import com.nightrec.app.RecognitionWindow
import com.nightrec.app.RequestBudget
import com.nightrec.app.StableTrack
import com.nightrec.app.TrackStabilizer
import com.nightrec.app.data.NightRecDatabase
import com.nightrec.app.data.OccurrenceState
import com.nightrec.app.data.RecognitionObservationEntity
import com.nightrec.app.data.SongEntity
import com.nightrec.app.data.TrackOccurrenceEntity
import com.nightrec.app.data.UnrecognizedRangeEntity

/** 一个已攒满、待识别的单声道窗口。 */
data class RecognitionRequest(val monoPcm16: ByteArray, val audioTimestampMs: Long)

/**
 * 采集 PCM -> 识别 -> 稳定 -> 持久化 的编排器（T054/T056/T057/T059）。
 *
 * 线程契约：[accept] 只在采集消费者线程调用（独占 [RecognitionWindow]）；[recognize]/[persist]
 * 只在识别 worker 协程调用（独占 [TrackStabilizer] 与 DB）。两者互不加锁。
 * 分片轮转不重启识别：窗口跨分片连续累积，仅 AWAY 时 [onGap] 丢弃 gap 前音频。
 */
class RecognitionCoordinator(
    private val sessionId: Long,
    private val engine: RecognitionEngine,
    private val budget: RequestBudget,
    private val db: NightRecDatabase,
    private val clock: ClockProvider,
    private val policy: RecognitionPolicy = RecognitionPolicy(),
    private val onCurrentTrack: (title: String, artist: String) -> Unit = { _, _ -> },
) {
    private val window = RecognitionWindow(
        policy.sampleRate,
        policy.channels,
        policy.windowMs,
        policy.intervalMs,
        silenceRmsThreshold = policy.silenceRmsThreshold,
    )
    private val stabilizer = TrackStabilizer()

    /** 命中冷却：命中一首歌后，其后的窗口在这段逻辑时间内不再发请求（仅识别 worker 协程访问）。 */
    private var suppressRequestsUntilMs = Long.MIN_VALUE

    /** 喂入采集 PCM；攒满一个窗口时返回识别请求，否则返回 null。 */
    fun accept(pcm: ByteArray, offset: Int, size: Int, logicalMs: Long): RecognitionRequest? {
        val slice = if (offset == 0 && size == pcm.size) pcm else pcm.copyOfRange(offset, offset + size)
        val ready = window.append(slice, logicalMs) ?: return null
        return RecognitionRequest(ready.first, ready.second)
    }

    /** AWAY/恢复边界：丢弃 gap 前音频，逻辑时间不跨越 gap（T054）。 */
    fun onGap() = window.reset()

    /** 识别一个窗口并持久化；预算耗尽或处于命中冷却期时只跳过网络识曲，不抛异常。 */
    suspend fun recognize(request: RecognitionRequest) {
        if (request.audioTimestampMs < suppressRequestsUntilMs) return
        if (!budget.reserve()) return
        val result = engine.recognize(request.monoPcm16, policy.sampleRate, request.audioTimestampMs) ?: return
        if (result.matched && policy.hitCooldownMs > 0) {
            suppressRequestsUntilMs = request.audioTimestampMs + policy.hitCooldownMs
        }
        persist(result)
    }

    /**
     * 幂等持久化（T057）：同一 (provider, session, 逻辑时间) 只落一次。
     * 返回 true 表示该窗口当前被认定为“已命中”，供补识别/手动重识别判断是否解除 unknown。
     * 若此前已落过 UNKNOWN、现在命中，则原地升级（补识别 T058）而不重复插入。
     */
    suspend fun persist(result: RecognitionResult): Boolean {
        val key = "${result.provider}:$sessionId:${result.audioTimestampMs}"
        val inserted = db.observations().insert(
            RecognitionObservationEntity(
                idempotencyKey = key,
                sessionId = sessionId,
                audioTimestampMs = result.audioTimestampMs,
                provider = result.provider,
                rawSongId = result.rawSongId,
                confidenceLikeState = if (result.matched) STATE_MATCHED else STATE_UNKNOWN,
                receivedAt = clock.wallMs(),
            ),
        )
        if (inserted == -1L) {
            val existing = db.observations().byKey(key)
            if (!result.matched || existing == null || existing.confidenceLikeState == STATE_MATCHED) {
                return existing?.confidenceLikeState == STATE_MATCHED
            }
            db.observations().update(
                existing.copy(
                    rawSongId = result.rawSongId,
                    confidenceLikeState = STATE_MATCHED,
                    receivedAt = clock.wallMs(),
                ),
            )
        } else if (!result.matched) {
            db.unrecognized().insert(
                UnrecognizedRangeEntity(
                    sessionId = sessionId,
                    logicalStartMs = result.audioTimestampMs,
                    logicalEndMs = result.audioTimestampMs + policy.windowMs,
                    resolved = false,
                ),
            )
            return false
        }
        val stable = stabilizer.observe(result.identity, result.audioTimestampMs) ?: return true
        persistOccurrence(stable, result)
        return true
    }

    private suspend fun persistOccurrence(stable: StableTrack, result: RecognitionResult) {
        val songId = db.songs().upsert(
            SongEntity(
                provider = result.provider,
                rawSongId = result.rawSongId ?: stable.song,
                title = result.title.orEmpty(),
                artist = result.artist.orEmpty(),
                album = result.album,
                artworkUrl = result.artworkUrl,
                externalIdsJson = null,
            ),
        )
        // 幂等去重：同一首歌在相邻窗口内已有 occurrence 时不重复插入（补识别可能重跑命中段）。
        val duplicate = db.occurrences().countSongInWindow(
            sessionId = sessionId,
            songId = songId,
            startMs = (stable.start - policy.windowMs).coerceAtLeast(0),
            endMs = stable.start + policy.windowMs,
        ) > 0
        if (!duplicate) {
            db.occurrences().insert(
                TrackOccurrenceEntity(
                    sessionId = sessionId,
                    songId = songId,
                    firstTrustedLogicalMs = stable.start,
                    confirmedLogicalMs = stable.confirmed,
                    wallTimeMs = clock.wallMs(),
                    state = OccurrenceState.CONFIRMED,
                ),
            )
        }
        onCurrentTrack(result.title.orEmpty(), result.artist.orEmpty())
    }

    companion object {
        const val STATE_MATCHED = "MATCHED"
        const val STATE_UNKNOWN = "UNKNOWN"
    }
}