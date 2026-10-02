package com.nightrec.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nightrec.app.RecordingUiState
import com.nightrec.app.ui.components.PrimaryGradientButton
import com.nightrec.app.ui.components.StateBadge
import com.nightrec.app.ui.theme.BrandGradient
import com.nightrec.app.ui.theme.LocalSpacing
import kotlinx.coroutines.delay

/**
 * T047 AwayScreen：对齐原型 06 —— 已暂离、暂离时长、回来继续、结束今晚。
 * 暂离期间不写音频、不做识别，但仍是同一次 Night Session。
 */
@Composable
fun AwayScreen(
    state: RecordingUiState,
    onResume: () -> Unit,
    onEndTonight: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val awayMs by awayElapsed(state.awayStartedAtMs)

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.6f))
            Box(
                Modifier
                    .heightIn(min = 96.dp)
                    .fillMaxWidth(0.4f)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(BrandGradient))
                    .padding(spacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                Text("Ⅱ", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.height(spacing.lg))
            StateBadge(text = "已暂离", active = false)
            Spacer(Modifier.height(spacing.sm))
            Text(
                formatClock(awayMs),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(spacing.sm))
            Text(
                "录音已暂停，识别已暂停",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1f))

            PrimaryGradientButton(label = "回来了，继续记录", onClick = onResume)
            Spacer(Modifier.height(spacing.md))
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = spacing.minTouchTarget)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onEndTonight),
                contentAlignment = Alignment.Center,
            ) {
                Text("结束今晚", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(spacing.md))
            Text(
                "暂离期间不会记录任何音频，但属于本次 Night Session。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun awayElapsed(startedAtMs: Long?): State<Long> = produceState(
    initialValue = startedAtMs?.let { (System.currentTimeMillis() - it).coerceAtLeast(0) } ?: 0L,
    key1 = startedAtMs,
) {
    if (startedAtMs == null) {
        value = 0L
        return@produceState
    }
    while (true) {
        value = (System.currentTimeMillis() - startedAtMs).coerceAtLeast(0)
        delay(1_000)
    }
}