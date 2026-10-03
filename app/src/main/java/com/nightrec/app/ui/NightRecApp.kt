package com.nightrec.app.ui

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.nightrec.app.NightRecApplication
import com.nightrec.app.RecordingForegroundService
import com.nightrec.app.RecordingPhase
import com.nightrec.app.SessionState
import com.nightrec.app.data.NightSessionEntity
import com.nightrec.app.data.SegmentState
import com.nightrec.app.data.ThemeMode
import com.nightrec.app.permission.PermissionCoordinator
import com.nightrec.app.session.SessionProcessingStatus
import com.nightrec.app.ui.components.HomeTab
import com.nightrec.app.ui.screens.AwayScreen
import com.nightrec.app.ui.screens.HomeScreen
import com.nightrec.app.ui.screens.OnboardingScreen
import com.nightrec.app.ui.screens.PlayerScreen
import com.nightrec.app.ui.screens.ProcessingScreen
import com.nightrec.app.ui.screens.RecentSessionRow
import com.nightrec.app.ui.screens.RecoveryScreen
import com.nightrec.app.ui.screens.RecordingScreen
import com.nightrec.app.ui.screens.StartSessionScreen
import com.nightrec.app.ui.theme.NightRecTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val PREFS = "nightrec.prefs"
private const val KEY_ONBOARDED = "onboarded"

private val ACTIVE_STATES = setOf(
    SessionState.STARTING.name, SessionState.RECORDING.name, SessionState.AWAY.name,
    SessionState.INTERRUPTED.name, SessionState.RECOVERING.name, SessionState.STOPPING.name,
)

private sealed interface Screen {
    data object Onboarding : Screen
    data object Home : Screen
    data object StartSession : Screen
    data class SessionDetail(val sessionId: Long) : Screen
}

private data class StartRequest(val name: String, val place: String?, val liveRecognition: Boolean)

@Composable
fun NightRecApp() {
    val context = LocalContext.current
    val app = context.applicationContext as NightRecApplication
    val systemDark = isSystemInDarkTheme()
    var darkOverride by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        darkOverride = when (ThemeMode.from(app.settings.current().themeMode)) {
            ThemeMode.SYSTEM -> null
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    }

    NightRecTheme(darkTheme = darkOverride ?: systemDark) {
        AppContent(onThemeMode = { mode ->
            darkOverride = when (mode) {
                ThemeMode.SYSTEM -> null
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
        })
    }
}

@Composable
private fun AppContent(onThemeMode: (ThemeMode) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as NightRecApplication
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    var screen by remember { mutableStateOf<Screen>(if (prefs.getBoolean(KEY_ONBOARDED, false)) Screen.Home else Screen.Onboarding) }
    var tab by remember { mutableStateOf(HomeTab.TONIGHT) }
    var pendingStart by remember { mutableStateOf<StartRequest?>(null) }

    val recording by app.recordingState.state.collectAsState()
    val sessions by app.sessionRepository.observeAll().collectAsState(initial = emptyList())

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val request = pendingStart
        pendingStart = null
        val granted = result[Manifest.permission.RECORD_AUDIO] ?: PermissionCoordinator.canCapture(context)
        if (granted && request != null) {
            startRecording(context, request.name, request.place, request.liveRecognition)
        } else {
            Toast.makeText(context, "需要麦克风权限才能开始今晚", Toast.LENGTH_LONG).show()
        }
    }

    // 采集中：录制 / 暂离 / 恢复 界面优先。
    if (recording.active) {
        val onAway = { sendAction(context, RecordingForegroundService.ACTION_AWAY) }
        val onResume = { sendAction(context, RecordingForegroundService.ACTION_RESUME) }
        val onEnd = { sendAction(context, RecordingForegroundService.ACTION_END) }
        when (recording.phase) {
            RecordingPhase.AWAY -> AwayScreen(state = recording, onResume = onResume, onEndTonight = onEnd)
            else -> RecordingScreen(state = recording, onAway = onAway, onEndTonight = onEnd)
        }
        return
    }

    // 未正常结束的 Session 恢复卡片（T076），不新建 Session。
    val recovery = sessions.firstOrNull { it.state in ACTIVE_STATES }
    if (recovery != null && screen != Screen.Onboarding) {
        RecoveryScreen(
            sessionName = recovery.name,
            state = recovery.state,
            logicalMs = recovery.logicalDurationMs,
            onContinue = {
                scope.launch {
                    val segments = app.database.segments().bySession(recovery.id)
                    val logical = segments.filter { it.state == SegmentState.SEALED }.sumOf { it.logicalDurationMs }
                    val lastIndex = segments.maxOfOrNull { it.index } ?: -1
                    app.recordingState.update {
                        it.copy(
                            sessionId = recovery.id,
                            name = recovery.name,
                            phase = RecordingPhase.RECORDING,
                            logicalMs = logical,
                            segmentIndex = lastIndex,
                        )
                    }
                    app.sessionRepository.markState(recovery.id, SessionState.RECOVERING)
                    sendAction(context, RecordingForegroundService.ACTION_RESUME)
                }
            },
            onFinishAndSave = {
                scope.launch {
                    val segments = app.database.segments().bySession(recovery.id)
                    val logical = segments.filter { it.state == SegmentState.SEALED }.sumOf { it.logicalDurationMs }
                    app.sessionRepository.finishSession(recovery.id, logical)
                    app.sessionProcessing.start(context, recovery.id)
                    app.sessionRepository.markReady(recovery.id)
                    screen = Screen.SessionDetail(recovery.id)
                }
            },
        )
        return
    }

    when (val s = screen) {
        Screen.Onboarding -> OnboardingScreen(onContinue = {
            prefs.edit().putBoolean(KEY_ONBOARDED, true).apply()
            screen = Screen.Home
        })

        Screen.Home -> HomeScreen(
            tab = tab,
            onSelectTab = { tab = it },
            onStartTonight = { screen = Screen.StartSession },
            recentSessions = sessions.map { it.toRow() },
            onOpenSession = { screen = Screen.SessionDetail(it) },
            onThemeChange = onThemeMode,
        )

        Screen.StartSession -> StartSessionScreen(
            defaultName = defaultSessionName(),
            onBack = { screen = Screen.Home },
            onConfirm = { name, place, _clean, liveRecognition ->
                screen = Screen.Home
                if (PermissionCoordinator.canCapture(context)) {
                    startRecording(context, name, place, liveRecognition)
                } else {
                    pendingStart = StartRequest(name, place, liveRecognition)
                    permissionLauncher.launch(PermissionCoordinator.required())
                }
            },
        )

        is Screen.SessionDetail -> SessionDetailScreen(
            sessionId = s.sessionId,
            onBack = { screen = Screen.Home },
        )
    }
}

@Composable
private fun SessionDetailScreen(sessionId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as NightRecApplication
    var state by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<SessionProcessingStatus?>(null) }
    var name by remember { mutableStateOf("") }
    var enteredPlayer by remember { mutableStateOf(false) }

    LaunchedEffect(sessionId) {
        val session = app.database.sessions().byId(sessionId)
        state = session?.state
        name = session?.name.orEmpty()
        status = app.sessionProcessing.status(sessionId)
    }

    val current = state
    if (current == null) {
        ProcessingScreen(sessionName = "...", status = SessionProcessingStatus(0, 0, 0, 0, 0, 0, false), onEnterPlayer = onBack)
        return
    }
    if (current == SessionState.PROCESSING.name && !enteredPlayer) {
        ProcessingScreen(
            sessionName = name,
            status = status ?: SessionProcessingStatus(0, 0, 0, 0, 0, 0, false),
            onEnterPlayer = { enteredPlayer = true },
        )
    } else {
        PlayerScreen(sessionId = sessionId, onBack = onBack)
    }
}

private fun startRecording(context: Context, name: String, place: String?, liveRecognition: Boolean) {
    ContextCompat.startForegroundService(
        context,
        RecordingForegroundService.startIntent(context, name, place, liveRecognition),
    )
}

private fun sendAction(context: Context, action: String) {
    ContextCompat.startForegroundService(
        context,
        RecordingForegroundService.actionIntent(context, action),
    )
}

private fun defaultSessionName(): String =
    "今晚 · " + SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date())

private fun NightSessionEntity.toRow(): RecentSessionRow {
    val place = place?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""
    return RecentSessionRow(id = id, title = name, subtitle = state + place)
}