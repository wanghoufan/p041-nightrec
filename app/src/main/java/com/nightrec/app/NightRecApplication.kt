package com.nightrec.app

import android.app.Application
import com.nightrec.app.data.FavoriteRepository
import com.nightrec.app.data.NightRecDatabase
import com.nightrec.app.data.SessionRepository
import com.nightrec.app.data.SettingsRepository
import com.nightrec.app.data.TrackCorrectionRepository
import com.nightrec.app.playback.PlaybackSourceResolver
import com.nightrec.app.recognition.AuddRecognitionEngine
import com.nightrec.app.recognition.RecognitionEngine
import com.nightrec.app.recognition.SessionTimelineReader
import com.nightrec.app.recovery.SegmentRecoveryPlanner
import com.nightrec.app.recovery.StorageMonitor
import com.nightrec.app.session.SessionProcessing
import java.io.File

class NightRecApplication : Application() {
    val database: NightRecDatabase by lazy { NightRecDatabase.build(this) }
    val clock: ClockProvider = SystemClockProvider()
    val sessionRepository: SessionRepository by lazy { SessionRepository(database, clock) }
    val recordingState = RecordingStateStore()
    val storagePaths: StoragePaths by lazy { StoragePaths.of(this).also { it.ensure() } }
    val recognitionEngine: RecognitionEngine by lazy { AuddRecognitionEngine() }
    val timelineReader: SessionTimelineReader by lazy { SessionTimelineReader(database) }
    val playbackResolver: PlaybackSourceResolver by lazy { PlaybackSourceResolver(database, storagePaths) }
    val sessionProcessing: SessionProcessing by lazy { SessionProcessing(database, storagePaths) }
    val segmentRecovery: SegmentRecoveryPlanner by lazy { SegmentRecoveryPlanner(database) }
    val storageMonitor: StorageMonitor by lazy { StorageMonitor.of(storagePaths.filesDir) }
    val corrections: TrackCorrectionRepository by lazy { TrackCorrectionRepository(database, clock) }
    val favorites: FavoriteRepository by lazy { FavoriteRepository(database) }
    val settings: SettingsRepository by lazy { SettingsRepository(database) }

    /**
     * 共享识曲预算（T052）。不清零：若从 Spike probe 切换过来，用其已消耗计数做种子，
     * 保证 300 的共享上限不被突破；耗尽只停网络识曲，Original 采集继续。
     */
    val recognitionBudget: RequestBudget by lazy {
        val probeUsed = getSharedPreferences("audd-budget", MODE_PRIVATE).getInt("attempted", 0)
        RequestBudget(
            File(filesDir, "recognition/budget.txt"),
            limit = BuildConfig.AUDD_LIMIT,
            initialUsed = probeUsed,
        )
    }
}