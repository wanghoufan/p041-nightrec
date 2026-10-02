package com.nightrec.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nightrec.app.RecordingUiState
import com.nightrec.app.ui.components.PrimaryGradientButton
import com.nightrec.app.ui.components.StateBadge
import com.nightrec.app.ui.components.WaveformCard
import com.nightrec.app.ui.theme.BrandGradient
import com.nightrec.app.ui.theme.LocalSpacing

/**
 * T045 RecordingScreen：对齐深色原型 05 —— REC 徽标、逻辑计时、波形、当前/上一首、
 * 暂离与结束今晚。结束今晚有防误触确认（T062）。
 */
@Composable
fun RecordingScreen(
    state: RecordingUiState,
    onAway: () -> Unit,
    onEndTonight: () -> Unit,
) {
    val spacing = LocalSpacing.current
    var confirming by remember { mutableStateOf(false) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(spacing.lg),
        ) {
            Text("正在记录", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(spacing.md))

            Row(verticalAlignment = Alignment.CenterVertically) {
                StateBadge(text = "● REC", active = true)
                Spacer(Modifier.padding(horizontal = spacing.xs))
                Text("正在记录今晚", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(spacing.sm))
            Text(
                formatClock(state.logicalMs),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(Modifier.height(spacing.lg))
            WaveformCard(active = state.phase == com.nightrec.app.RecordingPhase.RECORDING)

            Spacer(Modifier.height(spacing.lg))
            TrackBlock(
                label = "已知歌曲 · ${state.knownTrackCount} 首",
                title = state.currentTitle ?: "识别中…",
                artist = state.currentArtist ?: "等待下一段确认",
                emphasized = true,
            )
            Spacer(Modifier.height(spacing.md))
            TrackBlock(
                label = "上一首",
                title = state.previousTitle ?: "—",
                artist = state.previousArtist ?: "尚无确认歌曲",
                emphasized = false,
            )

            Spacer(Modifier.weight(1f))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                OutlinedPill(
                    label = "Ⅱ 暂离",
                    modifier = Modifier.weight(1f),
                    onClick = onAway,
                )
                DangerPill(
                    label = "结束今晚",
                    modifier = Modifier.weight(1f),
                    onClick = { confirming = true },
                )
            }
        }
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("结束今晚？") },
            text = {
                Text("已记录 ${formatClock(state.logicalMs)}。结束后将关闭原版歌曲文件并进入后台整理，仍可回放。")
            },
            confirmButton = { TextButton(onClick = { confirming = false; onEndTonight() }) { Text("结束并保存") } },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text("继续记录") } },
        )
    }
}

@Composable
private fun TrackBlock(label: String, title: String, artist: String, emphasized: Boolean) {
    val spacing = LocalSpacing.current
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(spacing.xs))
        Text(
            title,
            style = if (emphasized) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OutlinedPill(label: String, modifier: Modifier, onClick: () -> Unit) {
    val spacing = LocalSpacing.current
    Box(
        modifier = modifier
            .heightIn(min = spacing.minTouchTarget)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun DangerPill(label: String, modifier: Modifier, onClick: () -> Unit) {
    val spacing = LocalSpacing.current
    Box(
        modifier = modifier
            .heightIn(min = spacing.minTouchTarget)
            .clip(RoundedCornerShape(50))
            .background(Brush.linearGradient(listOf(Color(0xFFFF3B5C), Color(0xFFFF6B3D))))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}