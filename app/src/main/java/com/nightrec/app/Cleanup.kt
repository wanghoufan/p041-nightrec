package com.nightrec.app

data class SpeechWindow(
    val startMs: Long, val endMs: Long, val speech: Float, val singing: Float,
    val music: Float, val imbalance: Double, val dominantChannel: Int
)

/** Local gentle ducking only. It is not source separation or lossless voice removal. */
object ConservativeCleanup {
    fun apply(stereo: FloatArray, rate: Int, windows: List<SpeechWindow>): FloatArray {
        require(rate > 0 && stereo.size % 2 == 0)
        val output = stereo.copyOf()
        val trusted = windows.sortedBy { it.startMs }.filter {
            it.startMs >= 0 && it.endMs > it.startMs && it.speech >= .98f &&
            it.singing <= .05f && it.music < .4f && it.imbalance >= 4 && it.dominantChannel in 0..1
        }
        val groups = mutableListOf<MutableList<SpeechWindow>>()
        for (window in trusted) {
            val last = groups.lastOrNull()?.lastOrNull()
            if (last != null && last.endMs == window.startMs && last.dominantChannel == window.dominantChannel)
                groups.last().add(window)
            else groups.add(mutableListOf(window))
        }
        for (group in groups.filter { it.size >= 2 }) {
            val start = (group.first().startMs * rate / 1000).coerceAtMost(stereo.size / 2L).toInt()
            val end = (group.last().endMs * rate / 1000).coerceAtMost(stereo.size / 2L).toInt()
            val fade = maxOf(1, rate / 10)
            val channel = group.first().dominantChannel
            for (frame in start until end) {
                val envelope = minOf(1f, (frame - start).toFloat() / fade, (end - 1 - frame).toFloat() / fade).coerceAtLeast(0f)
                val offset = frame * 2 + channel
                output[offset] = stereo[offset] * (1f - .15f * envelope)
            }
        }
        return output
    }
}
