package com.deviceinfo.auto

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

class AppPreferences(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    enum class ThemeMode {
        SYSTEM,
        LIGHT,
        DARK
    }

    fun getThemeMode(): ThemeMode = when (prefs.getString(KEY_THEME, "system")) {
        "light" -> ThemeMode.LIGHT
        "dark" -> ThemeMode.DARK
        else -> ThemeMode.SYSTEM
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(
            KEY_THEME,
            when (mode) {
                ThemeMode.LIGHT -> "light"
                ThemeMode.DARK -> "dark"
                ThemeMode.SYSTEM -> "system"
            }
        ).apply()
        applyNightMode()
    }

    fun getLanguageTag(): String = (prefs.getString(KEY_LANGUAGE, "") ?: "").trim()

    fun setLanguageTag(tag: String) {
        prefs.edit().apply {
            if (tag.isEmpty()) remove(KEY_LANGUAGE)
            else putString(KEY_LANGUAGE, tag)
        }.apply()
        applyLocales()
    }

    fun applyToDelegate() {
        applyNightMode()
        applyLocales()
    }

    /** Clears saved language and theme; [applyToDelegate] runs immediately. */
    fun resetToDefaults() {
        prefs.edit()
            .remove(KEY_LANGUAGE)
            .putString(KEY_THEME, "system")
            .apply()
        applyToDelegate()
    }

    private fun applyNightMode() {
        AppCompatDelegate.setDefaultNightMode(
            when (getThemeMode()) {
                ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    private fun applyLocales() {
        AppCompatDelegate.setApplicationLocales(resolveApplicationLocales())
    }

    private fun resolveApplicationLocales(): LocaleListCompat {
        val saved = getLanguageTag()
        if (saved.isNotEmpty()) return LocaleListCompat.forLanguageTags(saved)
        return LocaleListCompat.getEmptyLocaleList()
    }

    companion object {
        private const val PREFS_NAME = "device_info_prefs"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_LANGUAGE = "language_tag"

        /** Car [androidx.car.app.CarContext] may ignore [AppCompatDelegate] app locales. */
        fun localizedContext(base: Context): Context {
            val tag = AppPreferences(base.applicationContext).getLanguageTag()
            if (tag.isEmpty()) return base
            val config = Configuration(base.resources.configuration)
            config.setLocale(Locale.forLanguageTag(tag))
            return base.createConfigurationContext(config)
        }
    }
}
