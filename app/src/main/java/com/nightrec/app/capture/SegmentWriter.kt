package com.nightrec.app.capture

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest

/**
 * 单个 AAC-LC / M4A 分片写入器（T041）。
 *
 * 所有字节先写 [openTemp]（`.open`）。只有 [seal] 才 fsync 后原子重命名到 [target] 并写
 * 校验和 sidecar（T042）。进程崩溃或 [abandon] 只会留下 `.open` 临时片，半成品永远不会
 * 冒充已封口分片。Original 目录内不存在任何被覆盖写的句柄。
 */
class SegmentWriter(
    private val target: File,
    private val openTemp: File,
    private val checksumFile: File,
    sampleRate: Int,
    channels: Int,
) {
    // 必须最先执行：父目录存在后 MediaMuxer 才能创建 `.open` 文件（属性初始化顺序敏感）。
    init {
        openTemp.parentFile?.mkdirs()
        if (openTemp.exists()) openTemp.delete()
    }

    private val codec: MediaCodec = MediaCodec.createEncoderByType(MIME).apply {
        val format = MediaFormat.createAudioFormat(MIME, sampleRate, channels).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }
        configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        start()
    }
    private val muxer = MediaMuxer(openTemp.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    private val bufferInfo = MediaCodec.BufferInfo()
    private var track = -1
    private var muxerStarted = false
    private var closed = false
    private var frames = 0L
    private val hz = sampleRate
    private val channelsPerFrame = channels

    val frameCount: Long get() = frames

    fun write(pcm: ByteArray, offset: Int, size: Int) {
        check(!closed) { "SegmentWriter already closed" }
        var cursor = offset
        var remaining = size
        while (remaining > 0) {
            val index = codec.dequeueInputBuffer(TIMEOUT_US)
            if (index >= 0) {
                val buffer = codec.getInputBuffer(index)!!
                buffer.clear()
                val chunk = minOf(buffer.remaining(), remaining)
                buffer.put(pcm, cursor, chunk)
                codec.queueInputBuffer(index, 0, chunk, frames * 1_000_000 / hz, 0)
                frames += chunk / (2 * channelsPerFrame)
                cursor += chunk
                remaining -= chunk
            }
            drain(false)
        }
    }

    /** fsync + 原子重命名 + 写 SHA-256；返回校验和。 */
    fun seal(): String {
        check(!closed) { "SegmentWriter already closed" }
        closed = true
        val index = codec.dequeueInputBuffer(EOS_TIMEOUT_US)
        check(index >= 0) { "Encoder did not accept end-of-stream" }
        codec.queueInputBuffer(index, 0, 0, frames * 1_000_000 / hz, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        drain(true)
        codec.stop()
        codec.release()
        if (muxerStarted) muxer.stop()
        muxer.release()
        RandomAccessFile(openTemp, "rw").use { it.fd.sync() }
        check(openTemp.renameTo(target)) { "Cannot atomically seal segment" }
        val digest = MessageDigest.getInstance("SHA-256").digest(target.readBytes()).joinToString("") { "%02x".format(it) }
        checksumFile.writeText(digest)
        return digest
    }

    /** 丢弃未封口分片（例如空分片或采集失败）。 */
    fun abandon() {
        if (closed) return
        closed = true
        runCatching { codec.stop() }
        runCatching { codec.release() }
        runCatching { if (muxerStarted) muxer.stop() }
        runCatching { muxer.release() }
        openTemp.delete()
    }

    private fun drain(endOfStream: Boolean) {
        while (true) {
            val output = codec.dequeueOutputBuffer(bufferInfo, if (endOfStream) EOS_TIMEOUT_US else 0)
            if (output == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                track = muxer.addTrack(codec.outputFormat)
                muxer.start()
                muxerStarted = true
            } else if (output >= 0) {
                val buffer = codec.getOutputBuffer(output)!!
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) bufferInfo.size = 0
                if (bufferInfo.size > 0 && muxerStarted) {
                    buffer.position(bufferInfo.offset)
                    buffer.limit(bufferInfo.offset + bufferInfo.size)
                    muxer.writeSampleData(track, buffer, bufferInfo)
                }
                val finished = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                codec.releaseOutputBuffer(output, false)
                if (finished) return
            } else {
                if (!endOfStream) return
                return
            }
        }
    }

    companion object {
        private const val MIME = "audio/mp4a-latm"
        private const val BIT_RATE = 192_000
        private const val TIMEOUT_US = 10_000L
        private const val EOS_TIMEOUT_US = 10_000L
    }
}