package com.nightrec.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.nightrec.app.ui.components.PrimaryGradientButton
import com.nightrec.app.ui.theme.LocalSpacing

/**
 * T032 StartSessionScreen. Defaults allow starting immediately without input.
 * Actual capture start (permission + FGS) is wired in the capture phase.
 */
@Composable
fun StartSessionScreen(
    defaultName: String,
    onBack: () -> Unit,
    onConfirm: (name: String, place: String?, clean: Boolean, liveRecognition: Boolean) -> Unit,
) {
    val spacing = LocalSpacing.current
    var name by remember { mutableStateOf(defaultName) }
    var place by remember { mutableStateOf("") }
    var clean by remember { mutableStateOf(true) }
    // 默认关闭：实时识曲消耗 AudD 额度，仅在用户明确需要时开启（默认关避免空跑烧额度）。
    var liveRecognition by remember { mutableStateOf(false) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(spacing.lg),
        ) {
            TextButton(onClick = onBack) { Text("返回") }
            Text("开始今晚", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(spacing.lg))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("今晚名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(spacing.md))
            OutlinedTextField(
                value = place,
                onValueChange = { place = it },
                label = { Text("地点（可选）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(spacing.lg))

            ToggleRow("实时识曲", "边录边识别歌曲", liveRecognition) { liveRecognition = it }
            ToggleRow("AI 净化", "降低附近讲话，保留歌曲主唱与转场", clean) { clean = it }

            Spacer(Modifier.height(spacing.xl))
            PrimaryGradientButton(
                label = "开始今晚",
                onClick = { onConfirm(name.ifBlank { defaultName }, place.ifBlank { null }, clean, liveRecognition) },
            )
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val spacing = LocalSpacing.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}