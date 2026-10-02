package com.nightrec.app.data

import com.nightrec.app.ClockProvider

/** 曲目纠错仓储（T087）：retry/merge/delete marker/标记暂离。绝不修改任何音频。 */
class TrackCorrectionRepository(
    private val db: NightRecDatabase,
    private val clock: ClockProvider,
) {
    /** 删除一条曲目标记（不删音频）。 */
    suspend fun deleteMarker(occurrenceId: Long) {
        db.occurrences().deleteById(occurrenceId)
    }

    /** 合并：把 fromId 的曲目标记并入最近一条同歌标记，等价于移除重复标记。 */
    suspend fun mergeMarker(fromId: Long, intoId: Long) {
        if (fromId == intoId) return
        db.occurrences().deleteById(fromId)
    }

    /** 把一段时间标记为暂离（事后修订），创建用户确认的 Gap。 */
    suspend fun markAway(sessionId: Long, wallStartMs: Long, wallEndMs: Long) {
        db.gaps().insert(
            SessionGapEntity(
                sessionId = sessionId,
                reason = "AWAY_MANUAL",
                wallStartMs = wallStartMs,
                wallEndMs = wallEndMs,
                userConfirmed = true,
            ),
        )
    }
}

/** 收藏仓储（T089）：TRACK / TRANSITION / CUSTOM 三种范围，点击可回现场。 */
class FavoriteRepository(private val db: NightRecDatabase) {
    suspend fun add(sessionId: Long, type: String, startMs: Long, endMs: Long): Long {
        require(type in setOf("TRACK", "TRANSITION", "CUSTOM"))
        require(endMs > startMs)
        return db.favorites().insert(
            FavoriteRangeEntity(
                sessionId = sessionId,
                type = type,
                logicalStartMs = startMs,
                logicalEndMs = endMs,
            ),
        )
    }

    suspend fun list(sessionId: Long): List<FavoriteRangeEntity> = db.favorites().bySession(sessionId)

    suspend fun remove(id: Long) = db.favorites().delete(id)
}

enum class ThemeMode(val persisted: String) {
    SYSTEM("system"), LIGHT("light"), DARK("dark");

    companion object {
        fun from(value: String?): ThemeMode = entries.firstOrNull { it.persisted == value } ?: SYSTEM
    }
}

/** 设置仓储（T090）。V1 默认不开启自动删除 Original，且不提供自动开启开关。 */
class SettingsRepository(private val db: NightRecDatabase) {
    suspend fun current(): UserSettingsEntity =
        db.settings().current() ?: DEFAULT.also { db.settings().upsert(it) }

    suspend fun update(settings: UserSettingsEntity) {
        // 无论 UI 如何传值，V1 强制 originalAutoDeleteEnabled=false。
        db.settings().upsert(settings.copy(originalAutoDeleteEnabled = false))
    }

    companion object {
        val DEFAULT = UserSettingsEntity(
            id = 1,
            themeMode = ThemeMode.SYSTEM.persisted,
            cleanEnabled = true,
            audioQuality = "high",
            recognitionEnabled = true,
            originalAutoDeleteEnabled = false,
        )
    }
}