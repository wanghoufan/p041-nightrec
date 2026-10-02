package com.nightrec.app.recovery

import java.io.File

/**
 * 存储监控（T077）。
 * WARNING：停止 Clean/Backfill 等派生处理，继续采集；
 * HARD：安全停止新的采集（先封口当前分片），不结束 Session。
 */
class StorageMonitor(private val usableSpace: () -> Long) {

    enum class Level { OK, WARNING, HARD }

    fun level(): Level {
        val free = usableSpace()
        return when {
            free <= HARD_BYTES -> Level.HARD
            free <= WARNING_BYTES -> Level.WARNING
            else -> Level.OK
        }
    }

    fun allowsDerivedProcessing(): Boolean = level() != Level.HARD

    companion object {
        const val WARNING_BYTES = 500L * 1024 * 1024
        const val HARD_BYTES = 100L * 1024 * 1024

        fun of(dir: File): StorageMonitor = StorageMonitor { dir.usableSpace }
    }
}