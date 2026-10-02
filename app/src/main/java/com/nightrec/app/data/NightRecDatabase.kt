package com.nightrec.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter fun segmentToString(value: SegmentState): String = value.name
    @TypeConverter fun stringToSegment(value: String): SegmentState = SegmentState.valueOf(value)
    @TypeConverter fun occurrenceToString(value: OccurrenceState): String = value.name
    @TypeConverter fun stringToOccurrence(value: String): OccurrenceState = OccurrenceState.valueOf(value)
    @TypeConverter fun statusToString(value: ProcessingStatus): String = value.name
    @TypeConverter fun stringToStatus(value: String): ProcessingStatus = ProcessingStatus.valueOf(value)
}

@Database(
    entities = [
        NightSessionEntity::class,
        AudioSegmentEntity::class,
        SessionGapEntity::class,
        SongEntity::class,
        RecognitionObservationEntity::class,
        TrackOccurrenceEntity::class,
        UnrecognizedRangeEntity::class,
        ProcessingJobEntity::class,
        CleanRangeEntity::class,
        FavoriteRangeEntity::class,
        UserSettingsEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class NightRecDatabase : RoomDatabase() {
    abstract fun sessions(): SessionDao
    abstract fun segments(): SegmentDao
    abstract fun gaps(): GapDao
    abstract fun songs(): SongDao
    abstract fun observations(): ObservationDao
    abstract fun occurrences(): OccurrenceDao
    abstract fun unrecognized(): UnrecognizedRangeDao
    abstract fun jobs(): ProcessingJobDao
    abstract fun cleanRanges(): CleanRangeDao
    abstract fun favorites(): FavoriteRangeDao
    abstract fun settings(): SettingsDao

    companion object {
        fun build(context: Context): NightRecDatabase =
            Room.databaseBuilder(context, NightRecDatabase::class.java, "nightrec.db").build()
    }
}