package com.nightrec.app.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import com.nightrec.app.SegmentPosition
import com.nightrec.app.StoragePaths
import com.nightrec.app.TimedSegment
import com.nightrec.app.TimelineMapper
import com.nightrec.app.data.NightRecDatabase
import com.nightrec.app.data.SegmentState
import java.io.File

enum class AudioVariant { ORIGINAL, CLEAN }

/**
 * 已解析的播放源（T065）：有序 MediaItems + 逻辑时间映射。
 *
 * 分片按逻辑时间背靠背拼接，因此 ExoPlayer 的全局位置就等于逻辑时间（gap 不含时长），
 * Marker/全局 seek 通过 [TimelineMapper] 映射到 (分片 index, 偏移)。
 */
data class ResolvedSource(
    val variant: AudioVariant,
    val segments: List<TimedSegment>,
    val mapper: TimelineMapper,
) {
    fun items(): List<MediaItem> = segments.map { seg ->
        MediaItem.Builder()
            .setUri(Uri.fromFile(File(seg.path)))
            .setMediaId(seg.logicalStart.toString())
            .build()
    }

    val durationMs: Long get() = mapper.duration
    fun seekTo(logicalMs: Long): SegmentPosition = mapper.seek(logicalMs)
    fun wallTime(logicalMs: Long): Long = mapper.wallTime(logicalMs)
}

/** PlaybackSourceResolver（T065）：SEALED 分片 -> 有序 MediaItems + logical ranges。 */
class PlaybackSourceResolver(
    private val db: NightRecDatabase,
    private val storage: StoragePaths,
) {

    suspend fun resolve(
        sessionId: Long,
        variant: AudioVariant = AudioVariant.ORIGINAL,
    ): ResolvedSource? {
        val sealed = db.segments().bySession(sessionId)
            .filter { it.state == SegmentState.SEALED && File(it.path).exists() }
            .sortedBy { it.index }
        if (sealed.isEmpty()) return null

        val cleanByStart = if (variant == AudioVariant.CLEAN) {
            db.cleanRanges().bySession(sessionId).associateBy { it.logicalStartMs }
        } else {
            emptyMap()
        }

        val timed = sealed.map { segment ->
            val cleanPath = cleanByStart[segment.logicalStartMs]?.assetPath
            val path = if (variant == AudioVariant.CLEAN && cleanPath != null && File(cleanPath).exists()) {
                cleanPath
            } else {
                segment.path
            }
            TimedSegment(path, segment.logicalStartMs, segment.logicalDurationMs, segment.wallStartMs)
        }
        val mapper = runCatching { TimelineMapper(timed) }.getOrNull() ?: return null
        return ResolvedSource(variant, timed, mapper)
    }
}