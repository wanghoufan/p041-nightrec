package com.nightrec.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.nightrec.app.ui.components.HomeTab
import com.nightrec.app.ui.components.NightRecBottomNav
import com.nightrec.app.ui.components.PrimaryGradientButton
import com.nightrec.app.ui.components.SessionListItem
import com.nightrec.app.ui.components.StateBadge
import com.nightrec.app.ui.theme.LocalSpacing

data class RecentSessionRow(
    val id: Long,
    val title: String,
    val subtitle: String,
)

@Composable
fun HomeScreen(
    tab: HomeTab,
    onSelectTab: (HomeTab) -> Unit,
    onStartTonight: () -> Unit,
    recentSessions: List<RecentSessionRow>,
    onOpenSession: (Long) -> Unit,
    onThemeChange: (com.nightrec.app.data.ThemeMode) -> Unit,
) {
    val spacing = LocalSpacing.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { NightRecBottomNav(selected = tab, onSelect = onSelectTab) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .safeDrawingPadding()
                .padding(spacing.lg),
        ) {
            when (tab) {
                HomeTab.TONIGHT -> TonightContent(spacing, onStartTonight, recentSessions, onOpenSession)
                HomeTab.HISTORY -> HistoryContent(onOpenSession)
                HomeTab.SETTINGS -> SettingsContent(onThemeChange)
            }
        }
    }
}

@Composable
private fun TonightContent(
    spacing: com.nightrec.app.ui.theme.Spacing,
    onStartTonight: () -> Unit,
    recentSessions: List<RecentSessionRow>,
    onOpenSession: (Long) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Text("今晚", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(spacing.xs))
        Text("一键开始，整晚不间断记录现场 DJ Set。", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(spacing.lg))
        PrimaryGradientButton(label = "开始今晚", onClick = onStartTonight)
        Spacer(Modifier.height(spacing.xl))
        Text("最近记录", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(spacing.sm))
        if (recentSessions.isEmpty()) {
            Text("还没有记录。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(Modifier.fillMaxWidth()) {
                items(recentSessions, key = { it.id }) { row ->
                    SessionListItem(
                        title = row.title,
                        subtitle = row.subtitle,
                        onClick = { onOpenSession(row.id) },
                    )
                    Spacer(Modifier.height(spacing.sm))
                }
            }
        }
    }
}

@Composable
internal fun EmptyState(title: String, body: String) {
    val spacing = LocalSpacing.current
    Column(Modifier.fillMaxSize()) {
        Text(title, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(spacing.sm))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(spacing.md))
        StateBadge(text = "V1", active = false)
    }
}