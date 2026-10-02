package com.nightrec.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nightrec.app.ui.theme.BrandGradient
import com.nightrec.app.ui.theme.LocalSpacing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape

/** Key CTA — gradient fill. Brand gradient is reserved for emphasis, waveform and key CTAs. */
@Composable
fun PrimaryGradientButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val spacing = LocalSpacing.current
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = spacing.minTouchTarget)
            .clip(shape)
            .then(
                if (enabled) Modifier.background(Brush.linearGradient(BrandGradient))
                else Modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = spacing.md, horizontal = spacing.lg),
        )
    }
}

/** Status pill: REC / 暂离 / 已结束 etc. */
@Composable
fun StateBadge(
    text: String,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val bg = if (active) Brush.linearGradient(BrandGradient) else null
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .then(if (bg != null) Modifier.background(bg) else Modifier.background(MaterialTheme.colorScheme.surfaceVariant))
            .padding(horizontal = spacing.sm, vertical = spacing.xs),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 7-bar waveform card, mirroring the brand mark. Animates only while capture is live. */
@Composable
fun WaveformCard(
    active: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 7,
) {
    val spacing = LocalSpacing.current
    val transition = rememberInfiniteTransition(label = "waveform")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "phase",
    )
    Surface(
        modifier = modifier.fillMaxWidth().heightIn(min = 120.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(Modifier.padding(spacing.lg), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxWidth().height(72.dp)) {
                val n = barCount
                val gap = size.width / (n * 2f + (n - 1))
                val barW = gap
                val maxH = size.height
                val t = if (active) phase.value else 0.35f
                for (i in 0 until n) {
                    val center = (n - 1) / 2f
                    val dist = kotlin.math.abs(i - center) / center
                    val wave = kotlin.math.sin((dist + t) * Math.PI.toFloat())
                    val h = (maxH * (0.35f + 0.55f * kotlin.math.abs(wave))).coerceIn(maxH * 0.2f, maxH)
                    val x = i * (barW + gap) + gap / 2f
                    drawRoundRect(
                        brush = Brush.verticalGradient(BrandGradient),
                        topLeft = Offset(x, (maxH - h) / 2f),
                        size = Size(barW, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 2f, barW / 2f),
                    )
                }
            }
        }
    }
}

@Composable
fun SessionListItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    val spacing = LocalSpacing.current
    Surface(
        modifier = modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            Modifier.padding(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(spacing.xs))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (trailing != null) {
                Spacer(Modifier.width(spacing.sm))
                Text(trailing, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** A timeline marker row (song/transition) used in the player + timeline screens. */
@Composable
fun TrackMarkerRow(
    timeLabel: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    val spacing = LocalSpacing.current
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = spacing.minTouchTarget).clickable(onClick = onClick).padding(vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (highlighted) Brush.linearGradient(BrandGradient) else Brush.linearGradient(listOf(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.outline))),
        )
        Spacer(Modifier.width(spacing.md))
        Text(timeLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(spacing.md))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

enum class HomeTab(val label: String, val icon: ImageVector) {
    TONIGHT("今晚", Icons.Filled.Home),
    HISTORY("历史", Icons.AutoMirrored.Filled.List),
    SETTINGS("设置", Icons.Filled.Settings),
}

@Composable
fun NightRecBottomNav(
    selected: HomeTab,
    onSelect: (HomeTab) -> Unit,
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        HomeTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
            )
        }
    }
}