package com.nightrec.app.cleanup

import com.nightrec.app.SpeechWindow
import java.io.File

/** 讲话污染检测契约（T079）。输入为 Original 分片的声道交错 PCM16。 */
interface SpeechContaminationDetector {
    fun detect(stereoPcm16: ByteArray, sampleRate: Int, channels: Int): List<SpeechWindow>
}

/**
 * 保守 Clean 引擎契约（T080）。
 * 只读 [CleanupRequest.originalFile]；任何派生输出写入 [CleanupRequest.outputFile]，
 * 二者绝不能是同一文件。低置信返回 `produced=false`（KEEP_ORIGINAL），禁止全量 Remove Vocals。
 */
interface SpeechCleanupEngine {
    suspend fun process(request: CleanupRequest): CleanupOutcome
}

data class CleanupRequest(
    val originalFile: File,
    val outputFile: File,
    val logicalStartMs: Long,
)

data class CleanupOutcome(
    val produced: Boolean,
    val confidence: Double,
    val assetPath: String?,
)

/**
 * 启发式检测器：以左右声道能量不平衡 + 单侧近距离能量作为“讲话”代理信号。
 * 只有当音乐/歌声能量（弱声道能量）足够低且声道极不平衡时才判定为讲话窗口，
 * 因此对正常音乐（两声道均衡）保持 KEEP_ORIGINAL。并非声源分离。
 */
class HeuristicSpeechContaminationDetector(private val windowMs: Long = 1_000) : SpeechContaminationDetector {

    override fun detect(stereoPcm16: ByteArray, sampleRate: Int, channels: Int): List<SpeechWindow> {
        if (channels < 2 || sampleRate <= 0) return emptyList()
        val framesPerWindow = (sampleRate * windowMs / 1000).toInt().coerceAtLeast(1)
        val totalFrames = stereoPcm16.size / (channels * 2)
        val out = ArrayList<SpeechWindow>()
        var frame = 0
        while (frame < totalFrames) {
            val end = minOf(frame + framesPerWindow, totalFrames)
            var sum0 = 0.0
            var sum1 = 0.0
            for (f in frame until end) {
                val s0 = sampleAt(stereoPcm16, f * channels)
                val s1 = sampleAt(stereoPcm16, f * channels + 1)
                sum0 += s0.toDouble() * s0
                sum1 += s1.toDouble() * s1
            }
            val count = (end - frame).coerceAtLeast(1)
            val rms0 = kotlin.math.sqrt(sum0 / count)
            val rms1 = kotlin.math.sqrt(sum1 / count)
            val maxE = maxOf(rms0, rms1)
            val minE = minOf(rms0, rms1)
            val imbalance = if (minE < 1.0) 8.0 else maxE / minE
            val quietLevel = (minE / 32768.0).toFloat()
            val speech = if (imbalance >= 4.0) 0.99f else (imbalance / 4.0 * 0.9).toFloat()
            out += SpeechWindow(
                startMs = frame * 1000L / sampleRate,
                endMs = end * 1000L / sampleRate,
                speech = speech,
                singing = quietLevel.coerceAtMost(0.5f),
                music = quietLevel.coerceAtMost(0.9f),
                imbalance = imbalance,
                dominantChannel = if (rms0 >= rms1) 0 else 1,
            )
            frame = end
        }
        return out
    }

    private fun sampleAt(bytes: ByteArray, frameIndex: Int): Short {
        val offset = frameIndex * 2
        return ((bytes[offset].toInt() and 0xFF) or (bytes[offset + 1].toInt() shl 8)).toShort()
    }
}