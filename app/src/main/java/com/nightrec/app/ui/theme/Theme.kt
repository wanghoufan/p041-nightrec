package com.nightrec.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * NightRec theme. Brand color is fixed from the V2.0 prototype tokens; dynamic
 * color is intentionally NOT applied so the brand gradient can't be overridden.
 */
@Composable
fun NightRecTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    CompositionLocalProvider(LocalSpacing provides Spacing()) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NightRecTypography,
            shapes = NightRecShapes,
            content = content,
        )
    }
}