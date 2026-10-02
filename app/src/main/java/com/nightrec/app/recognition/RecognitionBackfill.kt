package com.nightrec.app.recognition

import com.nightrec.app.ClockProvider
import com.nightrec.app.RecognitionWindow
import com.nightrec.app.RequestBudget
import com.nightrec.app.data.NightRecDatabase
import java.io.File

/**
 * 断网/失败 range 的幂等补识别（T058），也是手动“重新识别”入口（T059）。
 *
 * 对每个未解决的 unknown range：解码覆盖它的 Original 分片为单声道 PCM16，
 * 按 [RecognitionPolicy.windowMs] 步长切窗重新识别；命中即升级已有 observation、
 * 解除该 range 并落曲目 occurrence（幂等：重复运行不产生重复行）。
 * 预算耗尽只停止网络识曲，不抛异常；Original 始终只读。
 */
class RecognitionBackfill(
    private val db: NightRecDatabase,
    private val engine: RecognitionEngine,
    private val budget: RequestBudget,
    private val clock: ClockProvider,
    private val policy: RecognitionPolicy = RecognitionPolicy(),
    private val decode: (File) -> OriginalSegmentDecoder.Decoded? = { OriginalSegmentDecoder.decodeMono(it) },
) {

    data class Result(
        val attempted: Int,
        val matched: Int,
        val resolved: Int,
        val budgetExhausted: Boolean,
        val decodeUnavailable: Boolean,
    )

    suspend fun backfill(sessionId: Long): Result {
        val segments = db.segments().bySession(sessionId).sortedBy { it.index }
        val ranges = db.unrecognized().unresolved(sessionId)
        if (segments.isEmpty() || ranges.isEmpty()) return Result(0, 0, 0, false, false)

        val coordinator = RecognitionCoordinator(sessionId, engine, budget, db, clock, policy)
        val cache = HashMap<Int, OriginalSegmentDecoder.Decoded?>()
        var attempted = 0
        var matched = 0
        var resolved = 0
        var decodeUnavailable = false

        for (range in ranges) {
            var cursor = range.logicalStartMs
            while (cursor < range.logicalEndMs) {
                val segment = segments.lastOrNull { it.logicalStartMs <= cursor } ?: break
                val segmentEnd = segment.logicalStartMs + segment.logicalDurationMs
                if (cursor >= segmentEnd) break

                val decoded = cache.getOrPut(segment.index) { decode(File(segment.path)) }
                if (decoded == null) {
                    decodeUnavailable = true
                    break
                }
                val windowMs = minOf(policy.windowMs, segmentEnd - cursor)
                val startFrame = ((cursor - segment.logicalStartMs) * decoded.sampleRate / 1000).toInt()
                val frameCount = (windowMs * decoded.sampleRate / 1000).toInt()
                val pcm = slice(decoded.monoPcm16, startFrame, frameCount)
                if (pcm == null) break
                // 静音窗口直接跳过，不消耗额度（与实时链一致）。
                if (policy.silenceRmsThreshold > 0 && RecognitionWindow.rms(pcm) < policy.silenceRmsThreshold) {
                    cursor += windowMs
                    continue
                }

                if (!budget.reserve()) return Result(attempted, matched, resolved, true, decodeUnavailable)
                attempted++
                val result = engine.recognize(pcm, decoded.sampleRate, cursor) ?: break
                if (coordinator.persist(result)) {
                    matched++
                    db.unrecognized().markResolved(range.id)
                    resolved++
                    break
                }
                cursor += windowMs
            }
        }
        return Result(attempted, matched, resolved, false, decodeUnavailable)
    }

    private fun slice(source: ByteArray, startFrame: Int, frameCount: Int): ByteArray? {
        val startByte = startFrame * 2
        val endByte = (startFrame + frameCount) * 2
        if (startByte < 0 || startByte >= source.size) return null
        val end = endByte.coerceAtMost(source.size)
        if (end <= startByte) return null
        return source.copyOfRange(startByte, end)
    }
}