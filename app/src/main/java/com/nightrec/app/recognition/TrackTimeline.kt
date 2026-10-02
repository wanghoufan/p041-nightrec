package com.nightrec.app.recognition

/** 时间轴上的一段。kind 区分已知曲目与未知段（T060）。 */
data class TimelineEntry(
    val kind: Kind,
    val logicalStartMs: Long,
    val logicalEndMs: Long,
    val title: String? = null,
    val artist: String? = null,
) {
    enum class Kind { TRACK, UNKNOWN }
}

/** 已确认曲目的一段（含歌曲元数据）。 */
data class TimelineTrack(
    val logicalStartMs: Long,
    val logicalEndMs: Long,
    val title: String,
    val artist: String,
)

/** 未识别段。 */
data class TimelineUnknown(val logicalStartMs: Long, val logicalEndMs: Long)

/** 把已确认曲目与未知段合成按逻辑时间排序的时间轴（T060，纯函数，可单测）。 */
object TrackTimeline {
    fun build(
        tracks: List<TimelineTrack>,
        unknowns: List<TimelineUnknown>,
        sessionDurationMs: Long,
    ): List<TimelineEntry> {
        val entries = ArrayList<TimelineEntry>(tracks.size + unknowns.size)
        tracks.forEach { t ->
            val end = t.logicalEndMs.coerceAtMost(sessionDurationMs).coerceAtLeast(t.logicalStartMs)
            entries += TimelineEntry(TimelineEntry.Kind.TRACK, t.logicalStartMs, end, t.title, t.artist)
        }
        unknowns.forEach { u ->
            val end = u.logicalEndMs.coerceAtMost(sessionDurationMs).coerceAtLeast(u.logicalStartMs)
            entries += TimelineEntry(TimelineEntry.Kind.UNKNOWN, u.logicalStartMs, end)
        }
        return entries.sortedBy { it.logicalStartMs }
    }

    /** 由按时间排序的曲目起点推导每段结束（= 下一段起点，末段= session 结束）。 */
    fun trackEnds(starts: List<Long>, sessionDurationMs: Long): List<Long> =
        starts.mapIndexed { index, _ -> starts.getOrNull(index + 1) ?: sessionDurationMs }
}