package com.nightrec.app.recovery

import com.nightrec.app.data.NightRecDatabase
import com.nightrec.app.data.SegmentState
import java.io.File

/**
 * 启动/恢复时扫描分片状态（T075）：
 * - 已 SEALED 且文件存在 -> 可播放；
 * - OPEN（进程被杀留下的未封口尾片）或文件缺失 -> 标记 DAMAGED 并隔离，不参与播放。
 * 已封口的片段永不被改动。
 */
class SegmentRecoveryPlanner(private val db: NightRecDatabase) {

    data class Plan(val sealedIndices: List<Int>, val damagedIndices: List<Int>)

    suspend fun plan(sessionId: Long): Plan {
        val sealed = ArrayList<Int>()
        val damaged = ArrayList<Int>()
        db.segments().bySession(sessionId).forEach { segment ->
            val file = File(segment.path)
            if (segment.state == SegmentState.SEALED && file.exists() && file.length() > 0) {
                sealed += segment.index
            } else {
                damaged += segment.index
                db.segments().update(segment.copy(state = SegmentState.DAMAGED))
            }
        }
        return Plan(sealed, damaged)
    }
}