package com.nightrec.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.nightrec.app.ui.components.PrimaryGradientButton
import com.nightrec.app.ui.components.WaveformCard
import com.nightrec.app.ui.theme.LocalSpacing

/**
 * First-run onboarding: brand + privacy disclosure (FR-043).
 * States continuous recording, local storage, third-party recognition traffic and legal responsibility.
 */
@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    val spacing = LocalSpacing.current
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("NightRec", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(spacing.xs))
            Text("整晚 DJ Set 连续留存", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(spacing.xl))
            WaveformCard(active = true)
            Spacer(Modifier.height(spacing.xl))

            Disclosure("持续录音", "点击“开始今晚”后，NightRec 会在后台持续录制现场声音，直到你主动结束今晚或暂离。")
            Disclosure("本地保存", "录音默认保存在本机应用私有目录，V1 不要求云端数据库；Original 原版不会被改写。")
            Disclosure("第三方识曲", "为识别歌曲，片段会发送给第三方识曲服务进行声纹匹配，仅用于识曲，不用于广告。")
            Disclosure("合法使用责任", "请遵守当地法律与场所规定，尊重他人隐私；请勿录制你无权录制的内容。")

            Spacer(Modifier.height(spacing.xl))
            PrimaryGradientButton(label = "我已了解，继续", onClick = onContinue)
            Spacer(Modifier.height(spacing.md))
            Text(
                "继续即表示你已阅读并同意上述说明。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Disclosure(title: String, body: String) {
    val spacing = LocalSpacing.current
    Column(Modifier.fillMaxWidth().padding(vertical = spacing.sm)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(spacing.xs))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}