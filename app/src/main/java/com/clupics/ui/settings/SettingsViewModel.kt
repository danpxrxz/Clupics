package com.clupics.ui.settings

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences =
        application.getSharedPreferences("galeria_settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(loadFromPrefs())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private fun loadFromPrefs() = SettingsState(
        darkModeOverride = when (prefs.getString("dark_mode_override", "system")) {
            "dark"  -> true
            "light" -> false
            else    -> null
        },
        amoledMode   = prefs.getBoolean("amoled_mode", false),
        dynamicColor = prefs.getBoolean("dynamic_color", Build.VERSION.SDK_INT >= Build.VERSION_CODES.S),
        gridColumns  = prefs.getInt("grid_columns", 3),
        showImages   = prefs.getBoolean("show_images", true),
        showVideos   = prefs.getBoolean("show_videos", true),
        language     = AppLanguage.fromCode(prefs.getString("language", "system") ?: "system"),
        appLockEnabled = prefs.getBoolean("app_lock_enabled", false)
    )

    fun setDarkModeOverride(override: Boolean?) {
        prefs.edit().putString("dark_mode_override", when (override) {
            true -> "dark"; false -> "light"; null -> "system"
        }).apply()
        _state.value = _state.value.copy(darkModeOverride = override)
    }

    fun setAmoledMode(enabled: Boolean) {
        prefs.edit().putBoolean("amoled_mode", enabled).apply()
        _state.value = _state.value.copy(amoledMode = enabled)
    }

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean("dynamic_color", enabled).apply()
        _state.value = _state.value.copy(dynamicColor = enabled)
    }

    fun setGridColumns(columns: Int) {
        prefs.edit().putInt("grid_columns", columns).apply()
        _state.value = _state.value.copy(gridColumns = columns)
    }

    fun setShowImages(show: Boolean) {
        prefs.edit().putBoolean("show_images", show).apply()
        _state.value = _state.value.copy(showImages = show)
    }

    fun setShowVideos(show: Boolean) {
        prefs.edit().putBoolean("show_videos", show).apply()
        _state.value = _state.value.copy(showVideos = show)
    }

    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString("language", language.code).apply()
        _state.value = _state.value.copy(language = language)
    }

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("app_lock_enabled", enabled).apply()
        _state.value = _state.value.copy(appLockEnabled = enabled)
    }
}
