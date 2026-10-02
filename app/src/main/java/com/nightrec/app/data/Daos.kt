package com.nightrec.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: NightSessionEntity): Long

    @Update
    suspend fun update(session: NightSessionEntity)

    @Query("SELECT * FROM night_session WHERE state IN ('STARTING','RECORDING','AWAY','INTERRUPTED','RECOVERING','STOPPING') LIMIT 1")
    suspend fun activeSession(): NightSessionEntity?

    @Query("SELECT COUNT(*) FROM night_session WHERE state IN ('STARTING','RECORDING','AWAY','INTERRUPTED','RECOVERING','STOPPING')")
    suspend fun activeCount(): Int

    @Query("SELECT * FROM night_session WHERE id = :id")
    suspend fun byId(id: Long): NightSessionEntity?

    @Query("SELECT * FROM night_session ORDER BY wallStartMs DESC")
    fun observeAll(): Flow<List<NightSessionEntity>>

    @Query("SELECT * FROM night_session ORDER BY wallStartMs DESC")
    suspend fun all(): List<NightSessionEntity>

    @Query("SELECT * FROM night_session WHERE state = :state")
    suspend fun byState(state: String): List<NightSessionEntity>
}

@Dao
interface SegmentDao {
    @Insert
    suspend fun insert(segment: AudioSegmentEntity): Long

    @Update
    suspend fun update(segment: AudioSegmentEntity)

    @Query("SELECT * FROM audio_segment WHERE sessionId = :sessionId ORDER BY `index`")
    suspend fun bySession(sessionId: Long): List<AudioSegmentEntity>

    @Query("SELECT * FROM audio_segment WHERE state != 'SEALED'")
    suspend fun unsealed(): List<AudioSegmentEntity>
}

@Dao
interface GapDao {
    @Insert
    suspend fun insert(gap: SessionGapEntity): Long

    @Query("SELECT * FROM session_gap WHERE sessionId = :sessionId ORDER BY wallStartMs")
    suspend fun bySession(sessionId: Long): List<SessionGapEntity>
}

@Dao
interface SongDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(song: SongEntity): Long

    @Query("SELECT * FROM song WHERE provider = :provider AND rawSongId = :rawSongId")
    suspend fun byProviderId(provider: String, rawSongId: String): SongEntity?

    @Query("SELECT * FROM song WHERE id = :id")
    suspend fun byId(id: Long): SongEntity?

    /** Upsert that is safe to call repeatedly; returns the row id. */
    suspend fun upsert(song: SongEntity): Long {
        val existing = byProviderId(song.provider, song.rawSongId)
        return existing?.id ?: insert(song)
    }
}

@Dao
interface ObservationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(observation: RecognitionObservationEntity): Long

    @Update
    suspend fun update(observation: RecognitionObservationEntity)

    @Query("SELECT * FROM recognition_observation WHERE idempotencyKey = :key")
    suspend fun byKey(key: String): RecognitionObservationEntity?

    @Query("SELECT * FROM recognition_observation WHERE sessionId = :sessionId ORDER BY audioTimestampMs")
    suspend fun bySession(sessionId: Long): List<RecognitionObservationEntity>
}

@Dao
interface OccurrenceDao {
    @Insert
    suspend fun insert(occurrence: TrackOccurrenceEntity): Long

    @Update
    suspend fun update(occurrence: TrackOccurrenceEntity)

    @Query("SELECT * FROM track_occurrence WHERE sessionId = :sessionId ORDER BY confirmedLogicalMs")
    fun observeBySession(sessionId: Long): Flow<List<TrackOccurrenceEntity>>

    @Query("SELECT * FROM track_occurrence WHERE sessionId = :sessionId ORDER BY confirmedLogicalMs")
    suspend fun bySession(sessionId: Long): List<TrackOccurrenceEntity>

    @Query("SELECT COUNT(*) FROM track_occurrence WHERE sessionId = :sessionId")
    suspend fun countBySession(sessionId: Long): Int

    @Query("SELECT * FROM track_occurrence WHERE id = :id")
    suspend fun byId(id: Long): TrackOccurrenceEntity?

    @Query("SELECT COUNT(*) FROM track_occurrence WHERE sessionId = :sessionId AND songId = :songId AND confirmedLogicalMs BETWEEN :startMs AND :endMs")
    suspend fun countSongInWindow(sessionId: Long, songId: Long, startMs: Long, endMs: Long): Int

    @Query("DELETE FROM track_occurrence WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface UnrecognizedRangeDao {
    @Insert
    suspend fun insert(range: UnrecognizedRangeEntity): Long

    @Query("SELECT * FROM unrecognized_range WHERE sessionId = :sessionId AND resolved = 0")
    suspend fun unresolved(sessionId: Long): List<UnrecognizedRangeEntity>

    @Query("SELECT * FROM unrecognized_range WHERE sessionId = :sessionId ORDER BY logicalStartMs")
    suspend fun bySession(sessionId: Long): List<UnrecognizedRangeEntity>

    @Query("UPDATE unrecognized_range SET resolved = 1 WHERE id = :id")
    suspend fun markResolved(id: Long)
}

@Dao
interface ProcessingJobDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(job: ProcessingJobEntity): Long

    @Update
    suspend fun update(job: ProcessingJobEntity)

    @Query("SELECT * FROM processing_job WHERE status IN ('PENDING','RUNNING') ORDER BY id")
    suspend fun pending(): List<ProcessingJobEntity>
}

@Dao
interface CleanRangeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(range: CleanRangeEntity): Long

    @Update
    suspend fun update(range: CleanRangeEntity)

    @Query("SELECT * FROM clean_range WHERE sessionId = :sessionId ORDER BY logicalStartMs")
    suspend fun bySession(sessionId: Long): List<CleanRangeEntity>

    @Query("SELECT * FROM clean_range WHERE sessionId = :sessionId AND logicalStartMs = :startMs LIMIT 1")
    suspend fun byStart(sessionId: Long, startMs: Long): CleanRangeEntity?
}

@Dao
interface FavoriteRangeDao {
    @Insert
    suspend fun insert(favorite: FavoriteRangeEntity): Long

    @Query("SELECT * FROM favorite_range WHERE sessionId = :sessionId")
    suspend fun bySession(sessionId: Long): List<FavoriteRangeEntity>

    @Query("DELETE FROM favorite_range WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface SettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: UserSettingsEntity)

    @Query("SELECT * FROM user_settings WHERE id = 1")
    suspend fun current(): UserSettingsEntity?
}