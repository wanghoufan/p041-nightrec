package com.nightrec.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.nightrec.app.NightRecApplication
import com.nightrec.app.data.ThemeMode
import com.nightrec.app.data.UserSettingsEntity
import com.nightrec.app.ui.components.SessionListItem
import com.nightrec.app.ui.components.StateBadge
import com.nightrec.app.ui.theme.LocalSpacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

data class HistoryRow(
    val id: Long,
    val name: String,
    val subtitle: String,
)

/** HistoryScreen（T086）：日期、wall range、逻辑时长、曲目数、处理状态。 */
@Composable
fun HistoryContent(onOpenSession: (Long) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as NightRecApplication
    val spacing = LocalSpacing.current
    var rows by remember { mutableStateOf<List<HistoryRow>>(emptyList()) }
    val formatter = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        rows = app.database.sessions().all().map { session ->
            val trackCount = app.database.occurrences().countBySession(session.id)
            val date = formatter.format(Date(session.wallStartMs))
            HistoryRow(
                id = session.id,
                name = session.name,
                subtitle = "$date · 逻辑时长 ${formatClock(session.logicalDurationMs)} · $trackCount 首 · ${session.state}",
            )
        }
    }

    Column(Modifier.fillMaxSize()) {
        Text("历史", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(spacing.sm))
        if (rows.isEmpty()) {
            Text("还没有今晚记录。", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(Modifier.fillMaxWidth()) {
                items(rows, key = { it.id }) { row ->
                    SessionListItem(title = row.name, subtitle = row.subtitle, onClick = { onOpenSession(row.id) })
                    Spacer(Modifier.height(spacing.sm))
                }
            }
        }
    }
}

/** SettingsScreen（T090）：主题、音质、Clean、识曲状态、存储占用。Original 自动删除 V1 只读关闭。 */
@Composable
fun SettingsContent(onThemeChange: (ThemeMode) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as NightRecApplication
    val spacing = LocalSpacing.current
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(UserSettingsEntity(id = 1, themeMode = "system", cleanEnabled = true, audioQuality = "high", recognitionEnabled = true)) }
    var storageLabel by remember { mutableStateOf("—") }
    var recognitionLabel by remember { mutableStateOf("—") }

    LaunchedEffect(Unit) {
        settings = app.settings.current()
        onThemeChange(ThemeMode.from(settings.themeMode))
        val used = app.recognitionBudget.used
        recognitionLabel = "$used / 300 已用"
        storageLabel = formatBytes(app.storagePaths.filesDir.usableSpace)
    }

    fun persist(next: UserSettingsEntity) {
        settings = next
        // 通过仓储落盘（V1 强制 originalAutoDeleteEnabled=false）。
        scope.launch { app.settings.update(next) }
        onThemeChange(ThemeMode.from(next.themeMode))
    }

    Column(Modifier.fillMaxSize()) {
        Text("设置", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(spacing.md))

        Text("主题", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            ThemeMode.entries.forEach { mode ->
                val selected = ThemeMode.from(settings.themeMode) == mode
                TextButton(onClick = { persist(settings.copy(themeMode = mode.persisted)) }) {
                    Text(
                        text = when (mode) { ThemeMode.SYSTEM -> "跟随系统"; ThemeMode.LIGHT -> "浅色"; ThemeMode.DARK -> "深色" },
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }

        SettingToggle("AI 净化", "结束后保守降低附近讲话", settings.cleanEnabled) {
            persist(settings.copy(cleanEnabled = it))
        }
        SettingToggle("实时识曲", "边录边识别歌曲", settings.recognitionEnabled) {
            persist(settings.copy(recognitionEnabled = it))
        }
        SettingToggle("音质：高", "48kHz / stereo Original", true) { }

        Spacer(Modifier.height(spacing.md))
        Text("识曲状态：$recognitionLabel", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("可用存储：$storageLabel", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(spacing.md))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StateBadge(text = "Original 自动删除：关闭", active = false)
        }
        Spacer(Modifier.height(spacing.xs))
        Text("V1 不会自动删除原版，也不提供自动开启。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingToggle(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val spacing = LocalSpacing.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}