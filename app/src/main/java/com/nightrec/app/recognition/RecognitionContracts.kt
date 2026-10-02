package com.nightrec.app.recognition

/**
 * 供应商无关的识曲结果（T051）。
 *
 * [audioTimestampMs] 是**逻辑时间**锚点，必须与采集分片的逻辑时间同一坐标系，
 * 保证识别结果能落回 [com.nightrec.app.TimelineMapper] 的时间轴。
 * [matched] 为 false 表示窗口内无可识别歌曲（应落入 unknown range），
 * 而不是传输失败；传输失败用 `null` 表示。
 */
data class RecognitionResult(
    val provider: String,
    val matched: Boolean,
    val rawSongId: String?,
    val title: String?,
    val artist: String?,
    val album: String?,
    val artworkUrl: String?,
    val audioTimestampMs: Long,
) {
    /** 稳定的歌曲身份：优先供应商 ID，否则用标题/艺术家组合。 */
    val identity: String
        get() = rawSongId?.takeIf { it.isNotBlank() }?.let { "$provider:$it" }
            ?: "$provider:${title.orEmpty()}|${artist.orEmpty()}"

    companion object {
        fun unmatched(provider: String, audioTimestampMs: Long) =
            RecognitionResult(provider, false, null, null, null, null, null, audioTimestampMs)
    }
}

/**
 * 供应商无关的识曲引擎（T051）。
 * 实现必须：只接受单声道 PCM16、不打印 token、失败返回 null 而不抛异常。
 */
interface RecognitionEngine {
    val provider: String

    /**
     * 识别一段单声道 PCM16。返回 null 表示网络/服务失败（可重试，不消耗“未命中”语义）。
     */
    suspend fun recognize(monoPcm16: ByteArray, sampleRate: Int, audioTimestampMs: Long): RecognitionResult?
}

/** AudD token 来源。仅 debug 构建有值；release 恒为空（T052）。 */
interface TokenProvider {
    fun token(): String
}