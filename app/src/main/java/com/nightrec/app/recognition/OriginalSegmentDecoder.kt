package com.nightrec.app.recognition

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 把 Original 分片（AAC-LC / M4A）解码为**单声道 PCM16**（T058/T059）。
 *
 * 只读：绝不写入或替换 Original。解码结果与分片自身的采样率一致，由调用方按
 * `logicalStartMs + frame * 1000 / sampleRate` 映射回逻辑时间轴。
 */
object OriginalSegmentDecoder {

    data class Decoded(val monoPcm16: ByteArray, val sampleRate: Int)

    /** 原始声道交错 PCM16（用于 Clean 的声道不平衡分析）。 */
    data class Interleaved(val pcm16: ByteArray, val sampleRate: Int, val channels: Int)

    fun decodeMono(file: File): Decoded? {
        val raw = decodeInterleaved(file) ?: return null
        if (raw.channels == 1) return Decoded(raw.pcm16, raw.sampleRate)
        return Decoded(downmixToMono(raw.pcm16, raw.channels), raw.sampleRate)
    }

    fun decodeInterleaved(file: File): Interleaved? = decodeRaw(file)

    private fun decodeRaw(file: File): Interleaved? {
        if (!file.exists() || file.length() == 0L) return null
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(file.absolutePath)
            var trackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    format = f
                    trackIndex = i
                    break
                }
            }
            val fmt = format ?: return null
            if (trackIndex < 0) return null
            extractor.selectTrack(trackIndex)

            val sampleRate = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channels = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceIn(1, 2)
            val mime = fmt.getString(MediaFormat.KEY_MIME) ?: return null

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(fmt, null, null, 0)
            codec.start()

            val output = ByteArrayOutputStream()
            val info = MediaCodec.BufferInfo()
            var sawInputEos = false
            var sawOutputEos = false
            while (!sawOutputEos) {
                if (!sawInputEos) {
                    val inIndex = codec.dequeueInputBuffer(10_000)
                    if (inIndex >= 0) {
                        val input = codec.getInputBuffer(inIndex) ?: continue
                        val size = extractor.readSampleData(input, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            sawInputEos = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIndex = codec.dequeueOutputBuffer(info, 10_000)
                if (outIndex >= 0) {
                    val buffer = codec.getOutputBuffer(outIndex)
                    if (buffer != null && info.size > 0) {
                        buffer.position(info.offset)
                        buffer.limit(info.offset + info.size)
                        val chunk = ByteArray(info.size)
                        buffer.get(chunk)
                        output.write(chunk)
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) sawOutputEos = true
                }
            }
            return Interleaved(output.toByteArray(), sampleRate, channels)
        } catch (e: Exception) {
            // 解码失败：返回 null，让调用方保留 unknown 段，绝不抛给后台任务。
            android.util.Log.w("NightRec", "segment decode failure: ${e.javaClass.simpleName}")
            return null
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    private fun downmixToMono(interleaved: ByteArray, channels: Int): ByteArray {
        val buffer = ByteBuffer.wrap(interleaved).order(ByteOrder.LITTLE_ENDIAN)
        val shorts = buffer.asShortBuffer()
        val frames = shorts.remaining() / channels
        val mono = ByteArray(frames * 2)
        for (frame in 0 until frames) {
            var sum = 0
            for (channel in 0 until channels) sum += shorts.get(frame * channels + channel).toInt()
            val sample = sum / channels
            mono[frame * 2] = (sample and 0xFF).toByte()
            mono[frame * 2 + 1] = ((sample shr 8) and 0xFF).toByte()
        }
        return mono
    }
}