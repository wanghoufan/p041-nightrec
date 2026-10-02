package com.nightrec.app.recognition

/**
 * 识曲策略阈值（T056）。取 Spike 后固定值：
 * - 窗口 12s、步长 12s（背靠背连续窗口，避免漏识别与重复上传）；
 * - 未知段长度 = 窗口长度，用于 unknown range。
 *
 * 额度节流（2026-10-02 增补，防止 AudD 请求被空跑耗尽）：
 * - [silenceRmsThreshold]：窗口 RMS 低于该值视为静音，直接跳过、不发请求（派对空档/间隙不烧额度）；
 * - [hitCooldownMs]：命中一首歌后，接下来这段时间不再发请求（同曲重复窗口不必反复上传）；
 *   歌曲切换通常远长于该窗口，因此保守取 60s（约把“已识别歌曲段”的请求降到 1 次/分钟）。
 */
data class RecognitionPolicy(
    val sampleRate: Int = 48_000,
    val channels: Int = 2,
    val windowMs: Long = 12_000,
    val intervalMs: Long = 12_000,
    val silenceRmsThreshold: Int = 300,
    val hitCooldownMs: Long = 60_000,
) {
    init {
        require(sampleRate > 0 && channels in 1..2)
        require(windowMs > 0 && intervalMs >= windowMs)
        require(silenceRmsThreshold >= 0)
        require(hitCooldownMs >= 0)
    }
}