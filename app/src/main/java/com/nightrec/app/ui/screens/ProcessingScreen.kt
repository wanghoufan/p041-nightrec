package com.nightrec.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.nightrec.app.session.SessionProcessingStatus
import com.nightrec.app.ui.components.PrimaryGradientButton
import com.nightrec.app.ui.components.StateBadge
import com.nightrec.app.ui.theme.LocalSpacing

/**
 * T064 ProcessingScreen：结束后分项展示处理状态。Original 立即可进入播放，
 * 补识别/保守 Clean/转场各自独立推进，互不阻塞。
 */
@Composable
fun ProcessingScreen(
    sessionName: String,
    status: SessionProcessingStatus,
    onEnterPlayer: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(spacing.lg),
        ) {
            StateBadge(text = "已结束", active = false)
            Spacer(Modifier.height(spacing.md))
            Text("正在整理今晚", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(spacing.xs))
            Text(sessionName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(spacing.lg))

            StatusRow("原版可用", "已封口分片 ${status.sealedCount}/${status.segmentCount}" + (if (status.damagedCount > 0) " · 隔离 ${status.damagedCount}" else ""))
            StatusRow("走带重建", if (status.ready) "完成" else "进行中")
            StatusRow("补识别", "${status.unknownRemaining} 段待识别")
            StatusRow("保守 Clean", if (status.cleanCount > 0) "${status.cleanCount} 段已派生" else "无需要处理的讲话段")
            StatusRow("存储占用", formatBytes(status.originalBytes))

            Spacer(Modifier.weight(1f))
            Text("原版已可立即播放；整理在后台继续，不影响现在回放。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(spacing.md))
            PrimaryGradientButton(label = "进入播放器", onClick = onEnterPlayer)
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    val spacing = LocalSpacing.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

internal fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) "%.2f GB".format(mb / 1024.0) else "%.1f MB".format(mb)
}