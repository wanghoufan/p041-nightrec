package com.nightrec.app.capture

/**
 * Negotiated capture format. T004 spike固定 48kHz / stereo PCM16，单声道为回退。
 * 任何写入持久化格式字符串的地方都必须反映实际协商结果，不能硬编码假设 stereo。
 */
data class CaptureFormat(val sampleRate: Int, val channels: Int) {
    val channelMaskDescription: String get() = if (channels == 2) "stereo" else "mono"
}