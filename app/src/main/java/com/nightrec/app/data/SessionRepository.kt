package com.nightrec.app.data

import com.nightrec.app.ClockProvider
import com.nightrec.app.SessionState
import kotlinx.coroutines.flow.Flow

/** Session metadata repository. Enforces the "at most one active session" invariant (T033). */
class SessionRepository(
    private val db: NightRecDatabase,
    private val clock: ClockProvider,
) {
    suspend fun createActiveSession(name: String, place: String?): Long {
        require(db.sessions().activeCount() == 0) { "An active Night Session already exists" }
        val now = clock.wallMs()
        return db.sessions().insert(
            NightSessionEntity(
                name = name,
                place = place,
                state = SessionState.STARTING.name,
                wallStartMs = now,
                wallEndMs = null,
                logicalDurationMs = 0,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun active(): NightSessionEntity? = db.sessions().activeSession()

    fun observeAll(): Flow<List<NightSessionEntity>> = db.sessions().observeAll()

    /** STARTING -> RECORDING once capture is actually live (T034). */
    suspend fun markRecording(sessionId: Long) {
        val session = db.sessions().byId(sessionId) ?: return
        db.sessions().update(
            session.copy(state = SessionState.RECORDING.name, updatedAt = clock.wallMs()),
        )
    }

    /** Terminates capture: seals the logical duration and moves to PROCESSING (T063). */
    suspend fun finishSession(sessionId: Long, logicalDurationMs: Long) {
        val session = db.sessions().byId(sessionId) ?: return
        val now = clock.wallMs()
        db.sessions().update(
            session.copy(
                state = SessionState.PROCESSING.name,
                wallEndMs = now,
                logicalDurationMs = logicalDurationMs,
                updatedAt = now,
            ),
        )
    }

    /** PROCESSING -> READY：Original 已可安全播放（派生处理在后台独立推进）。 */
    suspend fun markReady(sessionId: Long) {
        val session = db.sessions().byId(sessionId) ?: return
        if (session.state == SessionState.READY.name) return
        db.sessions().update(session.copy(state = SessionState.READY.name, updatedAt = clock.wallMs()))
    }

    /** 采集中断/恢复状态标记（T072/T073）。 */
    suspend fun markState(sessionId: Long, state: SessionState) {
        val session = db.sessions().byId(sessionId) ?: return
        db.sessions().update(session.copy(state = state.name, updatedAt = clock.wallMs()))
    }
}