package com.nightrec.app.ui.screens

import android.content.ComponentName
import android.content.Context
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.nightrec.app.NightRecApplication
import com.nightrec.app.data.TrackOccurrenceEntity
import com.nightrec.app.playback.AudioVariant
import com.nightrec.app.playback.PlaybackService
import com.nightrec.app.playback.ResolvedSource
import com.nightrec.app.recognition.RecognitionBackfill
import com.nightrec.app.recognition.TimelineEntry
import com.nightrec.app.ui.components.PrimaryGradientButton
import com.nightrec.app.ui.components.StateBadge
import com.nightrec.app.ui.components.TrackMarkerRow
import com.nightrec.app.ui.theme.LocalSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed interface MarkerSelection {
    val entry: TimelineEntry
    data class Track(override val entry: TimelineEntry, val occurrence: TrackOccurrenceEntity?) : MarkerSelection
    data class Unknown(override val entry: TimelineEntry) : MarkerSelection
}

@Composable
private fun rememberPlaybackController(context: Context): MediaController? {
    var controller by remember { mutableStateOf<MediaController?>(null) }
    DisposableEffect(Unit) {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({ controller = runCatching { future.get() }.getOrNull() }, MoreExecutors.directExecutor())
        onDispose {
            controller?.release()
            controller = null
            MediaController.releaseFuture(future)
        }
    }
    return controller
}

/**
 * Session Player（T066–T069）：单一逻辑播放器（MediaController -> PlaybackService/ExoPlayer）。
 * 单进度条映射逻辑时间，Marker seek、Original/Clean 变体切换、收藏与纠错。
 */
@Composable
fun PlayerScreen(
    sessionId: Long,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as NightRecApplication
    val spacing = LocalSpacing.current
    val scope = rememberCoroutineScope()
    val controller = rememberPlaybackController(context)

    var source by remember { mutableStateOf<ResolvedSource?>(null) }
    var variant by remember { mutableStateOf(AudioVariant.ORIGINAL) }
    var timeline by remember { mutableStateOf<List<TimelineEntry>>(emptyList()) }
    var occurrences by remember { mutableStateOf<List<TrackOccurrenceEntity>>(emptyList()) }
    var favoritesCount by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf<MarkerSelection?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var retryMessage by remember { mutableStateOf<String?>(null) }

    suspend fun loadStatic() {
        timeline = app.timelineReader.timeline(sessionId)
        occurrences = app.database.occurrences().bySession(sessionId)
        favoritesCount = app.favorites.list(sessionId).size
    }

    // 关键：controller 异步连接完成后才能装载媒体项，因此 effect 必须以 controller 为键，
    // 否则首次 controller 为 null 时装载被跳过，播放按钮将无效。
    LaunchedEffect(sessionId, controller) {
        val resolved = app.playbackResolver.resolve(sessionId, AudioVariant.ORIGINAL)
        source = resolved
        val c = controller ?: return@LaunchedEffect
        if (c.mediaItemCount == 0) {
            val items = resolved?.items() ?: emptyList()
            if (items.isNotEmpty()) {
                c.setMediaItems(items)
                c.prepare()
            }
        }
    }

    LaunchedEffect(sessionId) {
        loadStatic()
    }

    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
        }
        controller?.addListener(listener)
        onDispose { controller?.removeListener(listener) }
    }

    // 位置轮询：把 ExoPlayer 的 (分片 index, 分片内偏移) 映射回全局逻辑时间。
    LaunchedEffect(controller, source) {
        while (true) {
            val c = controller
            val s = source
            if (c != null && s != null && !dragging) {
                val index = c.currentMediaItemIndex
                val base = s.segments.getOrNull(index)?.logicalStart ?: 0L
                val global = base + c.currentPosition.coerceAtLeast(0)
                sliderValue = global.coerceIn(0, s.durationMs.coerceAtLeast(0)).toFloat()
            }
            delay(500)
        }
    }

    fun seekToLogical(logicalMs: Long) {
        val c = controller ?: return
        val s = source ?: return
        val pos = s.seekTo(logicalMs.coerceIn(0, (s.durationMs - 1).coerceAtLeast(0)))
        c.seekTo(pos.index, pos.offset)
    }

    fun switchVariant(target: AudioVariant) {
        val c = controller ?: return
        val s = source ?: return
        val index = c.currentMediaItemIndex
        val base = s.segments.getOrNull(index)?.logicalStart ?: 0L
        val global = base + c.currentPosition.coerceAtLeast(0)
        val wasPlaying = c.isPlaying
        variant = target
        scope.launch {
            val resolved = app.playbackResolver.resolve(sessionId, target)
            source = resolved
            val items = resolved?.items() ?: return@launch
            val pos = resolved.seekTo(global.coerceIn(0, (resolved.durationMs - 1).coerceAtLeast(0)))
            c.setMediaItems(items, pos.index, pos.offset)
            c.prepare()
            if (wasPlaying) c.play()
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(spacing.lg),
        ) {
            TextButton(onClick = onBack) { Text("返回") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                StateBadge(text = if (variant == AudioVariant.CLEAN) "Clean" else "Original", active = false)
                Spacer(Modifier.padding(horizontal = spacing.xs))
                Text(
                    "收藏 ${favoritesCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(spacing.sm))
            Text(formatClock(sliderValue.toLong()), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(
                "总长 " + formatClock(source?.durationMs ?: 0),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Slider(
                value = sliderValue,
                onValueChange = { dragging = true; sliderValue = it },
                onValueChangeFinished = { dragging = false; seekToLogical(sliderValue.toLong()) },
                valueRange = 0f..(source?.durationMs ?: 1L).coerceAtLeast(1L).toFloat(),
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                PrimaryGradientButton(
                    label = if (isPlaying) "暂停" else "播放",
                    onClick = {
                        val c = controller
                        if (c != null) {
                            if (c.isPlaying) c.pause() else { c.prepare(); c.play() }
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
                OutlinedAction(
                    label = if (variant == AudioVariant.ORIGINAL) "切到 Clean" else "切到 Original",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        switchVariant(if (variant == AudioVariant.ORIGINAL) AudioVariant.CLEAN else AudioVariant.ORIGINAL)
                    },
                )
            }
            Spacer(Modifier.height(spacing.md))
            OutlinedAction(label = "重新识别未命中段", modifier = Modifier.fillMaxWidth()) {
                scope.launch {
                    retryMessage = "正在重新识别…"
                    val result = RecognitionBackfill(
                        db = app.database,
                        engine = app.recognitionEngine,
                        budget = app.recognitionBudget,
                        clock = app.clock,
                    ).backfill(sessionId)
                    loadStatic()
                    retryMessage = if (result.budgetExhausted) {
                        "识别预算已用尽，Original 继续保留"
                    } else if (result.resolved > 0) {
                        "重新识别到 ${result.resolved} 段，已更新"
                    } else {
                        "没有新增命中（尝试 ${result.attempted} 段）"
                    }
                }
            }
            retryMessage?.let {
                Spacer(Modifier.height(spacing.xs))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(spacing.md))
            Text("走带 / 歌曲", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(spacing.sm))
            LazyColumn(Modifier.fillMaxWidth()) {
                items(timeline, key = { "${it.kind}-${it.logicalStartMs}" }) { entry ->
                    val occurrence = occurrences.firstOrNull { it.firstTrustedLogicalMs == entry.logicalStartMs }
                    TrackMarkerRow(
                        timeLabel = formatClock(entry.logicalStartMs),
                        title = entry.title ?: "未知段",
                        subtitle = entry.artist ?: "未识别",
                        highlighted = entry.kind == TimelineEntry.Kind.TRACK,
                        onClick = {
                            seekToLogical(entry.logicalStartMs)
                            selected = if (entry.kind == TimelineEntry.Kind.TRACK) {
                                MarkerSelection.Track(entry, occurrence)
                            } else {
                                MarkerSelection.Unknown(entry)
                            }
                        },
                    )
                }
            }
        }
    }

    val current = selected
    if (current != null) {
        MarkerActionSheet(
            selection = current,
            onDismiss = { selected = null },
            onFavorite = {
                scope.launch {
                    app.favorites.add(
                        sessionId,
                        if (current.entry.kind == TimelineEntry.Kind.TRACK) "TRACK" else "CUSTOM",
                        current.entry.logicalStartMs,
                        (current.entry.logicalEndMs).coerceAtLeast(current.entry.logicalStartMs + 1000),
                    )
                    favoritesCount = app.favorites.list(sessionId).size
                    selected = null
                }
            },
            onMarkAway = {
                scope.launch {
                    val s = source
                    val wallStart = s?.wallTime(current.entry.logicalStartMs) ?: System.currentTimeMillis()
                    val wallEnd = s?.wallTime(current.entry.logicalEndMs) ?: wallStart
                    app.corrections.markAway(sessionId, wallStart, wallEnd)
                    loadStatic()
                    selected = null
                }
            },
            onDelete = if (current is MarkerSelection.Track && current.occurrence != null) {
                {
                    scope.launch {
                        app.corrections.deleteMarker(current.occurrence.id)
                        loadStatic()
                        selected = null
                    }
                }
            } else {
                null
            },
        )
    }
}

@Composable
private fun OutlinedAction(label: String, modifier: Modifier, onClick: () -> Unit) {
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
private fun MarkerActionSheet(
    selection: MarkerSelection,
    onDismiss: () -> Unit,
    onFavorite: () -> Unit,
    onMarkAway: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(selection.entry.title ?: "未知段") },
        text = { Text("时间 " + formatClock(selection.entry.logicalStartMs) + "。纠错只改标记，绝不修改音频。") },
        confirmButton = { TextButton(onClick = onFavorite) { Text("收藏此段") } },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("删除标记") }
                }
                TextButton(onClick = onMarkAway) { Text("标记为暂离") }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
    )
}