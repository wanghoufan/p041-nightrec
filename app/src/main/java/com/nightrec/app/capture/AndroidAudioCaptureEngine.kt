package com.nightrec.app.capture

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import com.nightrec.app.StoragePaths
import java.io.File

/** 已原子封口的一个分片（T042 元数据来源）。 */
data class SealedSegment(
    val index: Int,
    val file: File,
    val logicalStartMs: Long,
    val logicalDurationMs: Long,
    val wallStartMs: Long,
    val checksum: String,
)

/**
 * 正式采集引擎（T039/T040/T041）。
 *
 * 专用高优先级 audio thread 按 T004 协商格式（48kHz/stereo PCM16，mono 回退）读取
 * [AudioRecord]，同步写 Original 分片，并把 PCM 非阻塞 fanout 给识别消费者。默认
 * 每 5 分钟安全分片并原子封口。`logicalOffsetMs` / `startIndex` 让 AWAY 恢复后继续
 * 同一 Session 的逻辑时间与分片序号（T048），逻辑时间不包含 gap。
 */
class AndroidAudioCaptureEngine(
    private val sessionId: Long,
    private val paths: StoragePaths,
    private val fanout: PcmFanout,
    private val onSegmentSealed: (SealedSegment) -> Unit,
    private val onFailure: (String) -> Unit,
    private val startIndex: Int = 0,
    private val logicalOffsetMs: Long = 0,
) {
    @Volatile private var frames = 0L
    @Volatile private var running = false
    @Volatile private var format = CaptureFormat(48_000, 2)
    private var record: AudioRecord? = null
    private var thread: Thread? = null
    private var segmentIndex = startIndex
    private var segmentStartFrames = 0L
    private var segmentWallStart = 0L

    val indexOfLastSegment: Int get() = segmentIndex

    fun start() {
        if (running) return
        running = true
        thread = Thread({ captureLoop() }, "nightrec-capture").also { it.start() }
    }

    fun logicalMs(): Long = logicalOffsetMs + frames * 1000L / format.sampleRate

    fun stop() {
        running = false
        thread?.join(3_000)
        thread = null
    }

    private fun captureLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        try {
            val audioRecord = openRecord() ?: run { onFailure(FAILURE_MIC_BUSY); running = false; return }
            record = audioRecord
            audioRecord.startRecording()
            var writer = newWriter(segmentIndex, 0L)
            val pcm = ByteArray(4096)
            val segmentFrames = format.sampleRate.toLong() * (SEGMENT_MS / 1000)
            while (running) {
                val read = audioRecord.read(pcm, 0, pcm.size, AudioRecord.READ_BLOCKING)
                if (read <= 0) { onFailure(FAILURE_DEVICE); break }
                writer.write(pcm, 0, read)
                fanout.offer(pcm, 0, read, logicalMs())
                frames += read / (2 * format.channels)
                if (frames - segmentStartFrames >= segmentFrames) {
                    sealSegment(writer)
                    writer = newWriter(segmentIndex + 1, frames)
                }
            }
            audioRecord.stop()
            sealSegment(writer)
        } catch (e: Exception) {
            onFailure(e.javaClass.simpleName)
            runCatching { record?.stop() }
        } finally {
            runCatching { record?.release() }
            record = null
            running = false
        }
    }

    // 采集只由 RecordingForegroundService 在 RECORD_AUDIO 已授予后启动（PermissionCoordinator 校验）。
    @SuppressLint("MissingPermission")
    private fun openRecord(): AudioRecord? {
        val rate = 48_000
        for (channels in intArrayOf(2, 1)) {
            val mask = if (channels == 2) AudioFormat.CHANNEL_IN_STEREO else AudioFormat.CHANNEL_IN_MONO
            val min = AudioRecord.getMinBufferSize(rate, mask, AudioFormat.ENCODING_PCM_16BIT)
            if (min <= 0) continue
            val candidate = AudioRecord(
                MediaRecorder.AudioSource.UNPROCESSED, rate, mask, AudioFormat.ENCODING_PCM_16BIT, min * 4,
            )
            if (candidate.state == AudioRecord.STATE_INITIALIZED) {
                format = CaptureFormat(rate, channels)
                return candidate
            }
            candidate.release()
        }
        return null
    }

    private fun newWriter(index: Int, startFrames: Long): SegmentWriter {
        segmentIndex = index
        segmentStartFrames = startFrames
        segmentWallStart = System.currentTimeMillis()
        return SegmentWriter(
            target = paths.segmentFile(sessionId, index),
            openTemp = paths.segmentOpenTemp(sessionId, index),
            checksumFile = paths.segmentChecksum(sessionId, index),
            sampleRate = format.sampleRate,
            channels = format.channels,
        )
    }

    private fun sealSegment(writer: SegmentWriter) {
        if (writer.frameCount <= 0) { writer.abandon(); return }
        val index = segmentIndex
        val startMs = logicalOffsetMs + segmentStartFrames * 1000L / format.sampleRate
        val durationMs = writer.frameCount * 1000L / format.sampleRate
        val wallStart = segmentWallStart
        val checksum = writer.seal()
        onSegmentSealed(
            SealedSegment(index, paths.segmentFile(sessionId, index), startMs, durationMs, wallStart, checksum),
        )
    }

    companion object {
        private const val SEGMENT_MS = 5 * 60 * 1000L
        const val FAILURE_MIC_BUSY = "MIC_BUSY"
        const val FAILURE_DEVICE = "DEVICE"
    }
}