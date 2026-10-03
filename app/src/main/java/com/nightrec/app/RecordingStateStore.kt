package com.nightrec.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class RecordingPhase { IDLE, RECORDING, AWAY, INTERRUPTED }

/** 采集期跨 Service / UI 共享的只读状态（T043）。 */
data class RecordingUiState(
    val sessionId: Long? = null,
    val name: String? = null,
    val phase: RecordingPhase = RecordingPhase.IDLE,
    val logicalMs: Long = 0,
    val segmentIndex: Int = 0,
    val awayStartedAtMs: Long? = null,
    val knownTrackCount: Int = 0,
    val currentTitle: String? = null,
    val currentArtist: String? = null,
    val previousTitle: String? = null,
    val previousArtist: String? = null,
    /** 本场是否开启实时识曲（开始页勾选）；与全局设置共同门控识别（2026-10-03）。 */
    val liveRecognition: Boolean = false,
) {
    val active: Boolean get() = phase != RecordingPhase.IDLE
}

/** 单操作者应用，进程内单例状态容器；由 NightRecApplication 持有，Service 写、UI 读。 */
class RecordingStateStore {
    private val _state = MutableStateFlow(RecordingUiState())
    val state: StateFlow<RecordingUiState> = _state.asStateFlow()

    val current: RecordingUiState get() = _state.value

    fun update(transform: (RecordingUiState) -> RecordingUiState) = _state.update(transform)

    fun reset() { _state.value = RecordingUiState() }
}