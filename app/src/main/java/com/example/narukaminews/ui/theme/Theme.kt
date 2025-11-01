package com.example.narukaminews.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.toColorInt

/**
 * ⚡ NarukamiNewsTheme — hệ thống theme toàn cục cho app NarukamiNews
 *
 * Hỗ trợ:
 *  - 🌙 Dark / Light mode
 *  - 🎨 Accent color người dùng chọn
 *  - 🌈 Dynamic Color (Android 12+)
 *  - 💫 Đồng bộ Material3 + Compose
 */
@Composable
fun NarukamiNewsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    accentHex: String = "#7B1FA2", // 💜 màu chủ đạo mặc định (Narukami)
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val accent = Color(accentHex.toColorInt())

    val colorScheme = when {
        // 🟢 Android 12+ dùng dynamic color (theo wallpaper)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val dynamic = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            dynamic.copy(
                primary = accent,
                secondary = Color(0xFFFFD54F),
                tertiary = accent.copy(alpha = 0.7f),
                background = if (darkTheme) DarkBackground else LightBackground,
                surface = if (darkTheme) DarkSurface else LightSurface,
                onPrimary = Color.White
            )
        }

        // 🌙 Dark Mode
        darkTheme -> darkColorScheme(
            primary = accent,
            secondary = Color(0xFFFFD54F),
            tertiary = accent.copy(alpha = 0.8f),
            background = DarkBackground,
            surface = DarkSurface,
            onPrimary = Color.White,
            onBackground = DarkOnSurface,
            onSurface = DarkOnSurface
        )

        // ☀️ Light Mode
        else -> lightColorScheme(
            primary = accent,
            secondary = Color(0xFFFFD54F),
            tertiary = accent.copy(alpha = 0.8f),
            background = LightBackground,
            surface = LightSurface,
            onPrimary = Color.White,
            onBackground = LightOnSurface,
            onSurface = LightOnSurface
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
