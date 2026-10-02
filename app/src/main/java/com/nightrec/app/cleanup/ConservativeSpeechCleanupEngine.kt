package com.nightrec.app.cleanup

import com.nightrec.app.ConservativeCleanup
import com.nightrec.app.recognition.OriginalSegmentDecoder
import com.nightrec.app.recognition.PcmWav
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 保守讲话清理引擎（T081）。
 *
 * 只读 Original 分片，做局部、温和的通道 ducking（[ConservativeCleanup]），输出派生 WAV 到
 * `clean/v1/`。没有可信讲话窗口时返回 `produced=false`，保留 Original（KEEP_ORIGINAL）。
 * **不是**声源分离，也不做整晚 Remove Vocals。Original 永不写入。
 */
class ConservativeSpeechCleanupEngine(
    private val detector: SpeechContaminationDetector = HeuristicSpeechContaminationDetector(),
) : SpeechCleanupEngine {

    override suspend fun process(request: CleanupRequest): CleanupOutcome = withContext(Dispatchers.Default) {
        require(request.originalFile.canonicalPath != request.outputFile.canonicalPath) {
            "Clean 不能写回 Original"
        }
        val decoded = OriginalSegmentDecoder.decodeInterleaved(request.originalFile)
            ?: return@withContext CleanupOutcome(false, 0.0, null)
        val windows = detector.detect(decoded.pcm16, decoded.sampleRate, decoded.channels)
        val trusted = windows.filter {
            it.speech >= 0.98f && it.singing <= 0.05f && it.music < 0.4f &&
                it.imbalance >= 4.0 && it.dominantChannel in 0..1
        }
        if (trusted.isEmpty()) return@withContext CleanupOutcome(false, 0.0, null)

        val floats = toFloats(decoded.pcm16)
        val cleaned = ConservativeCleanup.apply(floats, decoded.sampleRate, windows)
        if (cleaned.contentEquals(floats)) return@withContext CleanupOutcome(false, 0.0, null)

        val pcm16 = toPcm16(cleaned)
        writeAtomically(request.outputFile, PcmWav.encodeInterleaved(pcm16, decoded.sampleRate, decoded.channels))
        CleanupOutcome(
            produced = true,
            confidence = trusted.size.toDouble() / windows.size.toDouble(),
            assetPath = request.outputFile.absolutePath,
        )
    }

    private fun toFloats(pcm16: ByteArray): FloatArray {
        val shorts = ByteBuffer.wrap(pcm16).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val out = FloatArray(shorts.remaining())
        for (i in out.indices) out[i] = shorts.get(i).toFloat()
        return out
    }

    private fun toPcm16(floats: FloatArray): ByteArray {
        val out = ByteArray(floats.size * 2)
        for (i in floats.indices) {
            val s = floats[i].toInt().coerceIn(-32768, 32767)
            out[i * 2] = (s and 0xFF).toByte()
            out[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
        }
        return out
    }

    private fun writeAtomically(output: File, bytes: ByteArray) {
        output.parentFile?.mkdirs()
        val pending = File(output.path + ".pending")
        try {
            FileOutputStream(pending).use { it.write(bytes); it.fd.sync() }
            check(pending.renameTo(output)) { "Clean 派生 asset 落盘失败" }
        } finally {
            pending.delete()
        }
    }
}