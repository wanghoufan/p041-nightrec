package com.nightrec.app.ui.screens

/** HH:MM:SS 逻辑时钟，用于 REC / Away / 播放器展示。 */
internal fun formatClock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d:%02d".format(total / 3600, total % 3600 / 60, total % 60)
}