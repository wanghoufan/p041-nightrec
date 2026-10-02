package com.nightrec.app.recognition

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** 把单声道 PCM16 打包成 WAV 供识别接口上传。 */
object PcmWav {
    fun encode(monoPcm16: ByteArray, sampleRate: Int): ByteArray =
        encodeInterleaved(monoPcm16, sampleRate, 1)

    /** 通用交错 PCM16 -> WAV（channels=1/2），用于识曲上传与 Clean 派生 asset。 */
    fun encodeInterleaved(pcm16: ByteArray, sampleRate: Int, channels: Int): ByteArray {
        val buffer = ByteBuffer.allocate(44 + pcm16.size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray())
        buffer.putInt(36 + pcm16.size)
        buffer.put("WAVEfmt ".toByteArray())
        buffer.putInt(16)
        buffer.putShort(1) // PCM
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(sampleRate * channels * 2) // byte rate
        buffer.putShort((channels * 2).toShort()) // block align
        buffer.putShort(16) // bits per sample
        buffer.put("data".toByteArray())
        buffer.putInt(pcm16.size)
        buffer.put(pcm16)
        return buffer.array()
    }
}