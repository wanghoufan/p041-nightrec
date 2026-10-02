package com.nightrec.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.nightrec.app.ui.components.PrimaryGradientButton
import com.nightrec.app.ui.components.StateBadge
import com.nightrec.app.ui.theme.LocalSpacing

/**
 * T076 未正常结束 Session 的恢复卡片：
 * 应用/进程在采集中被中断后，首页提供“继续今晚 / 结束并保存”，绝不新建 Session。
 */
@Composable
fun RecoveryScreen(
    sessionName: String,
    state: String,
    logicalMs: Long,
    onContinue: () -> Unit,
    onFinishAndSave: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(spacing.lg),
        ) {
            StateBadge(text = "未正常结束", active = false)
            Spacer(Modifier.height(spacing.md))
            Text("今晚还没结束", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(spacing.xs))
            Text("$sessionName · $state", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(spacing.sm))
            Text("已采集逻辑时长 ${formatClock(logicalMs)}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            PrimaryGradientButton(label = "继续今晚", onClick = onContinue)
            Spacer(Modifier.height(spacing.md))
            TextButton(onClick = onFinishAndSave) { Text("结束并保存") }
            Spacer(Modifier.height(spacing.xs))
            Text("继续会沿用同一次 Night Session，不会新建。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}