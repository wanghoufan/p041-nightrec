package com.nightrec.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.nightrec.app.capture.AndroidAudioCaptureEngine
import com.nightrec.app.capture.PcmFanout
import com.nightrec.app.capture.SealedSegment
import com.nightrec.app.data.AudioSegmentEntity
import com.nightrec.app.data.SegmentState
import com.nightrec.app.data.SessionGapEntity
import com.nightrec.app.recovery.CaptureFailure
import com.nightrec.app.recognition.RecognitionCoordinator
import com.nightrec.app.recognition.RecognitionRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * 采集前台服务（T034/T043/T044）。
 *
 * 只由可见 Activity 在用户动作 + 已授予 RECORD_AUDIO 后通过
 * [Context.startForegroundService] 启动，满足 while-in-use microphone 时序。
 * 持有 PARTIAL_WAKE_LOCK、常驻通知（状态 + 逻辑时长 + 结束/继续入口），并把状态写入
 * [RecordingStateStore] 供 REC/Away 界面只读展示。
 */
class RecordingForegroundService : Service() {

    companion object {
        const val ACTION_START = "com.nightrec.app.action.START"
        const val ACTION_AWAY = "com.nightrec.app.action.AWAY"
        const val ACTION_RESUME = "com.nightrec.app.action.RESUME"
        const val ACTION_END = "com.nightrec.app.action.END"
        const val EXTRA_NAME = "name"
        const val EXTRA_PLACE = "place"

        private const val NOTIFICATION_ID = 92
        private const val RECOVERY_FAILED_NOTIFICATION_ID = 93
        private const val CHANNEL_ID = "nightrec-recording"
        private const val MAX_RECOVERY_ATTEMPTS = 3

        /** 从可见 Activity 调用的唯一入口。 */
        fun startIntent(context: Context, name: String?, place: String?): Intent =
            Intent(context, RecordingForegroundService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_NAME, name)
                .putExtra(EXTRA_PLACE, place)

        fun actionIntent(context: Context, action: String): Intent =
            Intent(context, RecordingForegroundService::class.java).setAction(action)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val fanout = PcmFanout()
    private lateinit var app: NightRecApplication
    private var engine: AndroidAudioCaptureEngine? = null
    private var ticker: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var recognitionJob: Job? = null
    private var recoveryJob: Job? = null
    private var recoveryAttempts = 0
    /** 采集消费者线程读取；AWAY 时置位，让识别窗口在正确线程上丢弃 gap 前音频。 */
    @Volatile private var recognitionGapRequested = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        app = application as NightRecApplication
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "NightRec 录音", NotificationManager.IMPORTANCE_LOW),
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startCapture(intent.getStringExtra(EXTRA_NAME), intent.getStringExtra(EXTRA_PLACE))
            ACTION_AWAY -> pauseCapture()
            ACTION_RESUME -> resumeCapture()
            ACTION_END -> endCapture()
        }
        return START_NOT_STICKY
    }

    private fun startCapture(name: String?, place: String?) {
        if (engine != null) return
        startForegroundCompat(RecordingPhase.RECORDING)
        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NightRec:recording")
            .apply { acquire(8 * 60 * 60 * 1000L) }
        scope.launch {
            val sessionId: Long
            try {
                sessionId = app.sessionRepository.createActiveSession(name ?: "今晚", place)
            } catch (e: Exception) {
                app.recordingState.reset()
                stopSelf()
                return@launch
            }
            app.recordingState.update {
                it.copy(sessionId = sessionId, name = name, phase = RecordingPhase.RECORDING, logicalMs = 0)
            }
            app.sessionRepository.markRecording(sessionId)
            launchEngine(sessionId, startIndex = 0, logicalOffsetMs = 0)
            startRecognition(sessionId)
        }
    }

    private fun resumeCapture() {
        if (engine != null) return
        // 可能由恢复卡片从后台拉起：先确保进入前台服务，避免 FGS 超时崩溃（T076）。
        startForegroundCompat(RecordingPhase.RECORDING)
        recoveryAttempts = 0
        recoveryJob?.cancel()
        recoveryJob = null
        val state = app.recordingState.current
        val sessionId = state.sessionId ?: return
        app.recordingState.update { it.copy(phase = RecordingPhase.RECORDING, awayStartedAtMs = null) }
        launchEngine(sessionId, startIndex = state.segmentIndex + 1, logicalOffsetMs = state.logicalMs)
        if (recognitionJob == null) startRecognition(sessionId)
    }

    private fun launchEngine(sessionId: Long, startIndex: Int, logicalOffsetMs: Long) {
        val engine = AndroidAudioCaptureEngine(
            sessionId = sessionId,
            paths = app.storagePaths,
            fanout = fanout,
            onSegmentSealed = { segment -> persistSegment(sessionId, segment) },
            onFailure = { reason -> handleCaptureFailure(reason) },
            startIndex = startIndex,
            logicalOffsetMs = logicalOffsetMs,
        )
        this.engine = engine
        engine.start()
        startTicker()
    }

    /**
     * 启动识别消费者（T054）：采集消费者线程独占 [RecognitionCoordinator.accept]，
     * 识别 worker 协程独占 recognize/persist。正常分片轮转不重启识别；
     * 网络识别慢时通过有界 Channel 丢弃旧窗口，不阻塞录音。
     */
    private fun startRecognition(sessionId: Long) {
        recognitionJob?.cancel()
        val coordinator = RecognitionCoordinator(
            sessionId = sessionId,
            engine = app.recognitionEngine,
            budget = app.recognitionBudget,
            db = app.database,
            clock = app.clock,
            onCurrentTrack = { title, artist ->
                app.recordingState.update { s ->
                    s.copy(
                        previousTitle = s.currentTitle,
                        previousArtist = s.currentArtist,
                        currentTitle = title,
                        currentArtist = artist,
                        knownTrackCount = s.knownTrackCount + 1,
                    )
                }
            },
        )
        val requests = Channel<RecognitionRequest>(capacity = 2, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        recognitionGapRequested = false
        recognitionJob = scope.launch {
            launch {
                while (isActive) {
                    val chunk = fanout.poll(1_000) ?: continue
                    if (recognitionGapRequested) {
                        coordinator.onGap()
                        recognitionGapRequested = false
                    }
                    coordinator.accept(chunk.pcm, 0, chunk.pcm.size, chunk.logicalMs)
                        ?.let { requests.trySend(it) }
                }
            }
            for (request in requests) coordinator.recognize(request)
        }
    }

    private fun persistSegment(sessionId: Long, segment: SealedSegment) {
        // 在封口线程同步落盘，保证 STOP 后分片元数据必然已持久化（T042）。
        runBlocking(Dispatchers.IO) {
            app.database.segments().insert(
                AudioSegmentEntity(
                    sessionId = sessionId,
                    index = segment.index,
                    path = segment.file.absolutePath,
                    logicalStartMs = segment.logicalStartMs,
                    logicalDurationMs = segment.logicalDurationMs,
                    wallStartMs = segment.wallStartMs,
                    format = "m4a/aac-lc/48000/2",
                    state = SegmentState.SEALED,
                    checksum = segment.checksum,
                ),
            )
        }
        app.recordingState.update { it.copy(segmentIndex = segment.index) }
    }

    private fun pauseCapture() {
        val engine = engine ?: return
        ticker?.cancel()
        this.engine = null
        // 丢弃 gap 前的排队音频，并让识别窗口在消费者线程上重置（T054）。
        recognitionGapRequested = true
        fanout.clear()
        scope.launch {
            engine.stop()
            // 使用封口后的精确逻辑时间，保证恢复时新分片 logicalStart 与前一片 logicalEnd 完全相接（T049）。
            val logical = engine.logicalMs()
            val state = app.recordingState.current
            val sessionId = state.sessionId ?: return@launch
            val now = app.clock.wallMs()
            app.database.gaps().insert(
                SessionGapEntity(
                    sessionId = sessionId,
                    reason = "AWAY",
                    wallStartMs = now,
                    wallEndMs = now,
                    userConfirmed = true,
                ),
            )
            app.recordingState.update {
                it.copy(phase = RecordingPhase.AWAY, awayStartedAtMs = now, logicalMs = logical)
            }
            updateNotification(RecordingPhase.AWAY)
        }
    }

    private fun endCapture() {
        ticker?.cancel()
        recognitionJob?.cancel()
        recognitionJob = null
        recoveryJob?.cancel()
        recoveryJob = null
        fanout.clear()
        val engine = engine
        this.engine = null
        // 封口 + 落盘 + DB 更新放到后台协程，避免阻塞主线程（ANR）。
        scope.launch {
            engine?.stop()
            val state = app.recordingState.current
            val logical = engine?.logicalMs() ?: state.logicalMs
            state.sessionId?.let { sessionId ->
                app.sessionRepository.finishSession(sessionId, logical)
                // Original 立即可播放；补识别与保守 Clean 在后台独立推进（T064/T058/T082）。
                app.sessionProcessing.start(this@RecordingForegroundService, sessionId)
                app.sessionRepository.markReady(sessionId)
            }
            releaseWakeLock()
            app.recordingState.reset()
            ServiceCompat.stopForeground(this@RecordingForegroundService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    /** T071–T074：标准失败 -> 记录真实 Gap -> 短退避恢复；不可恢复或超限则通知用户。 */
    private fun handleCaptureFailure(reason: String) {
        val kind = CaptureFailure.of(reason)
        android.util.Log.w("NightRec", "Capture failure: $kind (${SafeLogger.sanitize(reason)})")
        val state = app.recordingState.current
        val sessionId = state.sessionId ?: return
        if (recoveryJob?.isActive == true) return

        if (!CaptureFailure.recoverable(kind) || recoveryAttempts >= MAX_RECOVERY_ATTEMPTS) {
            notifyRecoveryFailed()
            scope.launch {
                app.sessionRepository.markState(sessionId, SessionState.INTERRUPTED)
                app.recordingState.update { it.copy(phase = RecordingPhase.INTERRUPTED) }
            }
            return
        }

        recoveryAttempts++
        val backoffMs = 1_000L * recoveryAttempts
        val failed = engine
        this.engine = null
        ticker?.cancel()
        app.recordingState.update { it.copy(phase = RecordingPhase.INTERRUPTED) }
        recoveryJob = scope.launch {
            val logical = failed?.logicalMs() ?: app.recordingState.current.logicalMs
            failed?.stop()
            val now = app.clock.wallMs()
            // 记录真实 Gap：中断期间无音频、不计逻辑时长（T072）。
            app.database.gaps().insert(
                SessionGapEntity(
                    sessionId = sessionId,
                    reason = "INTERRUPTED",
                    wallStartMs = now,
                    wallEndMs = now,
                    userConfirmed = false,
                ),
            )
            app.sessionRepository.markState(sessionId, SessionState.INTERRUPTED)
            app.recordingState.update { it.copy(logicalMs = logical) }
            delay(backoffMs)
            app.sessionRepository.markState(sessionId, SessionState.RECOVERING)
            app.recordingState.update { it.copy(phase = RecordingPhase.RECORDING) }
            launchEngine(sessionId, startIndex = app.recordingState.current.segmentIndex + 1, logicalOffsetMs = logical)
        }
    }

    private fun notifyRecoveryFailed() {
        val manager = getSystemService(NotificationManager::class.java)
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val resume = PendingIntent.getService(
            this, 2, actionIntent(this, ACTION_RESUME),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val end = PendingIntent.getService(
            this, 1, actionIntent(this, ACTION_END),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_wave_mono)
            .setContentTitle("NightRec · 采集已中断")
            .setContentText("无法自动恢复。可恢复今晚或结束今晚（不会新建 Session）。")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(0, "恢复今晚", resume)
            .addAction(0, "结束今晚", end)
            .build()
        manager.notify(RECOVERY_FAILED_NOTIFICATION_ID, notification)
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                val engine = engine ?: break
                val ms = engine.logicalMs()
                app.recordingState.update { it.copy(logicalMs = ms) }
                updateNotification(RecordingPhase.RECORDING)
                delay(1_000)
            }
        }
    }

    private fun startForegroundCompat(phase: RecordingPhase) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(phase), type)
    }

    private fun updateNotification(phase: RecordingPhase) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(phase))
    }

    private fun buildNotification(phase: RecordingPhase): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val end = PendingIntent.getService(
            this, 1, actionIntent(this, ACTION_END),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val state = app.recordingState.current
        val text = if (phase == RecordingPhase.AWAY) {
            "已暂离 · 录音暂停"
        } else {
            "正在记录今晚 · " + formatDuration(state.logicalMs)
        }
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_wave_mono)
            .setContentTitle("NightRec")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(0, "结束今晚", end)
        if (phase == RecordingPhase.AWAY) {
            val resume = PendingIntent.getService(
                this, 2, actionIntent(this, ACTION_RESUME),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            builder.addAction(0, "继续记录", resume)
        }
        return builder.build()
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    override fun onDestroy() {
        ticker?.cancel()
        recognitionJob?.cancel()
        recognitionJob = null
        recoveryJob?.cancel()
        recoveryJob = null
        engine?.stop()
        engine = null
        releaseWakeLock()
        scope.cancel()
        super.onDestroy()
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = totalSeconds % 3600 / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}