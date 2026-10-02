package com.nightrec.app.recognition

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.time.Duration

/**
 * AudD 识曲引擎（T052/T053，替代原计划的 ShazamKit）。
 *
 * 契约：仅上传单声道 PCM16 WAV；token 来自 [TokenProvider] 且不落日志；任何网络/解析
 * 异常都返回 null（可重试），不抛到调用方。`status=success` 但 `result` 为 null 视为
 * “未命中”（matched=false），不是失败。
 */
class AuddRecognitionEngine(
    private val tokens: TokenProvider = DebugTokenProvider,
    private val client: OkHttpClient = defaultClient(),
    private val endpoint: String = "https://api.audd.io/",
) : RecognitionEngine {

    override val provider: String = PROVIDER

    override suspend fun recognize(
        monoPcm16: ByteArray,
        sampleRate: Int,
        audioTimestampMs: Long,
    ): RecognitionResult? = withContext(Dispatchers.IO) {
        val token = tokens.token()
        if (token.isBlank()) return@withContext null
        try {
            val wav = PcmWav.encode(monoPcm16, sampleRate)
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("api_token", token)
                .addFormDataPart("return", "apple_music,spotify")
                .addFormDataPart("file", "window.wav", wav.toRequestBody("audio/wav".toMediaType()))
                .build()
            client.newCall(Request.Builder().url(endpoint).post(body).build()).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val json = JSONObject(response.body?.string() ?: "{}")
                if (json.optString("status") != "success") return@withContext null
                val song = json.optJSONObject("result")
                if (song == null) return@withContext RecognitionResult.unmatched(provider, audioTimestampMs)
                RecognitionResult(
                    provider = provider,
                    matched = true,
                    rawSongId = song.optString("song_id").takeIf { it.isNotBlank() },
                    title = song.optString("title").takeIf { it.isNotBlank() },
                    artist = song.optString("artist").takeIf { it.isNotBlank() },
                    album = song.optString("album").takeIf { it.isNotBlank() },
                    artworkUrl = song.optString("song_link").takeIf { it.isNotBlank() },
                    audioTimestampMs = audioTimestampMs,
                )
            }
        } catch (e: Exception) {
            // 网络/解析失败：返回 null 让调用方决定是否补识别；绝不打印 token 或响应体。
            android.util.Log.w("NightRec", "recognition transport failure: ${e.javaClass.simpleName}")
            null
        }
    }

    companion object {
        const val PROVIDER = "audd"

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(20))
            .build()
    }
}