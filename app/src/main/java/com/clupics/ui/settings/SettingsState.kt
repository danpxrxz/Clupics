package com.clupics.ui.settings

enum class AppLanguage(val code: String, val displayNameKey: String) {
    SYSTEM("system", "lang_system"),
    SPANISH("es", "lang_es"),
    ENGLISH("en", "lang_en"),
    FRENCH("fr", "lang_fr"),
    PORTUGUESE("pt", "lang_pt"),
    GERMAN("de", "lang_de");

    companion object {
        fun fromCode(code: String) = values().firstOrNull { it.code == code } ?: SYSTEM
    }
}

data class SettingsState(
    val darkModeOverride : Boolean? = null,
    val amoledMode       : Boolean  = false,
    val dynamicColor     : Boolean  = true,
    val gridColumns      : Int      = 3,
    val showImages       : Boolean  = true,
    val showVideos       : Boolean  = true,
    val language         : AppLanguage = AppLanguage.SYSTEM,
    val appLockEnabled   : Boolean  = false
)
