package com.example.narukaminews.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// ⚡ DataStore tên "theme_prefs"
val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

object ThemePreferences {
    // 🔑 Keys
    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    private val ACCENT_COLOR_KEY = stringPreferencesKey("accent_color")
    private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
    private val FONT_SCALE_KEY = floatPreferencesKey("font_scale")

    // 🌙 Dark Mode
    fun isDarkMode(context: Context): Flow<Boolean> =
        context.themeDataStore.data.map { prefs ->
            prefs[DARK_MODE_KEY] ?: true
        }

    suspend fun setDarkMode(context: Context, enabled: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[DARK_MODE_KEY] = enabled
        }
    }

    // 🎨 Accent Color
    fun getAccentColor(context: Context): Flow<String> =
        context.themeDataStore.data.map { prefs ->
            prefs[ACCENT_COLOR_KEY] ?: "#7B1FA2" // 💜 Raiden tím mặc định
        }

    suspend fun setAccentColor(context: Context, colorHex: String) {
        context.themeDataStore.edit { prefs ->
            prefs[ACCENT_COLOR_KEY] = colorHex
        }
    }

    // 🌈 Dynamic Color (Android 12+)
    fun isDynamicColor(context: Context): Flow<Boolean> =
        context.themeDataStore.data.map { prefs ->
            prefs[DYNAMIC_COLOR_KEY] ?: true
        }

    suspend fun setDynamicColor(context: Context, enabled: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[DYNAMIC_COLOR_KEY] = enabled
        }
    }

    // 🔤 Font Scale (0.85f → 1.5f)
    fun getFontScale(context: Context): Flow<Float> =
        context.themeDataStore.data.map { prefs ->
            prefs[FONT_SCALE_KEY] ?: 1.0f
        }

    suspend fun setFontScale(context: Context, scale: Float) {
        context.themeDataStore.edit { prefs ->
            prefs[FONT_SCALE_KEY] = scale.coerceIn(0.85f, 1.5f)
        }
    }

    // ♻️ Reset tất cả về mặc định
    suspend fun resetToDefault(context: Context) {
        context.themeDataStore.edit { prefs ->
            prefs[DARK_MODE_KEY] = true
            prefs[ACCENT_COLOR_KEY] = "#7B1FA2"
            prefs[DYNAMIC_COLOR_KEY] = true
            prefs[FONT_SCALE_KEY] = 1.0f
        }
    }
}
