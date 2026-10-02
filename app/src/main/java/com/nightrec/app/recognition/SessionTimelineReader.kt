package com.nightrec.app.recognition

import com.nightrec.app.data.NightRecDatabase

/**
 * 从数据库装载某 Session 的时间轴（T060）。
 * 曲目段结束时间 = 下一段起点（末段 = session 逻辑时长）；未知段来自 unrecognized_range。
 */
class SessionTimelineReader(private val db: NightRecDatabase) {

    suspend fun timeline(sessionId: Long): List<TimelineEntry> {
        val session = db.sessions().byId(sessionId) ?: return emptyList()
        val occurrences = db.occurrences().bySession(sessionId)
        val starts = occurrences.map { it.confirmedLogicalMs }
        val ends = TrackTimeline.trackEnds(starts, session.logicalDurationMs)
        val tracks = ArrayList<TimelineTrack>(occurrences.size)
        occurrences.forEachIndexed { index, occurrence ->
            val song = db.songs().byId(occurrence.songId) ?: return@forEachIndexed
            tracks += TimelineTrack(
                logicalStartMs = occurrence.firstTrustedLogicalMs,
                logicalEndMs = ends[index],
                title = song.title,
                artist = song.artist,
            )
        }
        val unknowns = db.unrecognized().bySession(sessionId).map {
            TimelineUnknown(it.logicalStartMs, it.logicalEndMs)
        }
        return TrackTimeline.build(tracks, unknowns, session.logicalDurationMs)
    }
}