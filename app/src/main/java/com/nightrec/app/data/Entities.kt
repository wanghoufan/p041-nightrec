package com.nightrec.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SegmentState { OPEN, SEALED, DAMAGED }
enum class OccurrenceState { SUGGESTED, CONFIRMED, CORRECTED }
enum class ProcessingStatus { PENDING, RUNNING, DONE, FAILED }

@Entity(tableName = "night_session")
data class NightSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val place: String?,
    val state: String,
    val wallStartMs: Long,
    val wallEndMs: Long?,
    val logicalDurationMs: Long,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "audio_segment",
    foreignKeys = [ForeignKey(NightSessionEntity::class, ["id"], ["sessionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("sessionId")],
)
data class AudioSegmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val index: Int,
    val path: String,
    val logicalStartMs: Long,
    val logicalDurationMs: Long,
    val wallStartMs: Long,
    val format: String,
    val state: SegmentState,
    val checksum: String?,
)

@Entity(
    tableName = "session_gap",
    foreignKeys = [ForeignKey(NightSessionEntity::class, ["id"], ["sessionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("sessionId")],
)
data class SessionGapEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val reason: String,
    val wallStartMs: Long,
    val wallEndMs: Long,
    val userConfirmed: Boolean,
)

@Entity(tableName = "song", indices = [Index(value = ["provider", "rawSongId"], unique = true)])
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val provider: String,
    val rawSongId: String,
    val title: String,
    val artist: String,
    val album: String?,
    val artworkUrl: String?,
    val externalIdsJson: String?,
)

@Entity(
    tableName = "recognition_observation",
    indices = [Index(value = ["idempotencyKey"], unique = true), Index("sessionId")],
)
data class RecognitionObservationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val idempotencyKey: String,
    val sessionId: Long,
    val audioTimestampMs: Long,
    val provider: String,
    val rawSongId: String?,
    val confidenceLikeState: String,
    val receivedAt: Long,
)

@Entity(
    tableName = "track_occurrence",
    foreignKeys = [
        ForeignKey(NightSessionEntity::class, ["id"], ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(SongEntity::class, ["id"], ["songId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("sessionId"), Index("songId")],
)
data class TrackOccurrenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val songId: Long,
    val firstTrustedLogicalMs: Long,
    val confirmedLogicalMs: Long,
    val wallTimeMs: Long,
    val state: OccurrenceState,
)

@Entity(
    tableName = "unrecognized_range",
    foreignKeys = [ForeignKey(NightSessionEntity::class, ["id"], ["sessionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("sessionId")],
)
data class UnrecognizedRangeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val logicalStartMs: Long,
    val logicalEndMs: Long,
    val resolved: Boolean,
)

@Entity(
    tableName = "processing_job",
    foreignKeys = [ForeignKey(NightSessionEntity::class, ["id"], ["sessionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["idempotencyKey"], unique = true), Index("sessionId")],
)
data class ProcessingJobEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val type: String,
    val rangeStartMs: Long,
    val rangeEndMs: Long,
    val status: ProcessingStatus,
    val attempt: Int,
    val idempotencyKey: String,
)

@Entity(
    tableName = "clean_range",
    foreignKeys = [ForeignKey(NightSessionEntity::class, ["id"], ["sessionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("sessionId")],
)
data class CleanRangeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val logicalStartMs: Long,
    val logicalEndMs: Long,
    val status: ProcessingStatus,
    val assetPath: String?,
    val confidence: Double,
)

@Entity(
    tableName = "favorite_range",
    foreignKeys = [ForeignKey(NightSessionEntity::class, ["id"], ["sessionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("sessionId")],
)
data class FavoriteRangeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val type: String,
    val logicalStartMs: Long,
    val logicalEndMs: Long,
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val themeMode: String,
    val cleanEnabled: Boolean,
    val audioQuality: String,
    val recognitionEnabled: Boolean,
    val originalAutoDeleteEnabled: Boolean = false,
)