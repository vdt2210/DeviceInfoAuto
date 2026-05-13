package com.deviceinfo.auto

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RadioGroup
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: AppPreferences
    private var suppressChanges = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithEdgeBarsAndUpToolbar(R.layout.activity_settings, R.string.settings_title)
        prefs = AppPreferences(this)

        val spinner = findViewById<Spinner>(R.id.spinner_language)
        val languageOptions = listOf(
            getString(R.string.language_system),
            "English",
            "Tiếng Việt"
        )
        val adapter = ArrayAdapter(this, R.layout.item_spinner_settings, languageOptions)
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown_settings)
        spinner.adapter = adapter

        val languageTags = listOf("", "en", "vi")
        val themeGroup = findViewById<RadioGroup>(R.id.radio_theme)

        bindUiFromPrefs(spinner, themeGroup, languageTags)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (suppressChanges) return
                val tag = languageTags[position]
                if (tag == prefs.getLanguageTag()) return
                prefs.setLanguageTag(tag)
                recreate()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        themeGroup.setOnCheckedChangeListener { _, checkedId ->
            if (suppressChanges) return@setOnCheckedChangeListener
            val mode = when (checkedId) {
                R.id.radio_theme_light -> AppPreferences.ThemeMode.LIGHT
                R.id.radio_theme_dark -> AppPreferences.ThemeMode.DARK
                else -> AppPreferences.ThemeMode.SYSTEM
            }
            if (mode == prefs.getThemeMode()) return@setOnCheckedChangeListener
            prefs.setThemeMode(mode)
            recreate()
        }

        spinner.post {
            themeGroup.post {
                suppressChanges = false
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean = defaultFinishOnNavigateUp()

    private fun bindUiFromPrefs(spinner: Spinner, themeGroup: RadioGroup, languageTags: List<String>) {
        suppressChanges = true
        val tag = prefs.getLanguageTag()
        val langIndex = languageTags.indexOf(tag).coerceAtLeast(0)
        spinner.setSelection(langIndex)
        when (prefs.getThemeMode()) {
            AppPreferences.ThemeMode.LIGHT -> themeGroup.check(R.id.radio_theme_light)
            AppPreferences.ThemeMode.DARK -> themeGroup.check(R.id.radio_theme_dark)
            AppPreferences.ThemeMode.SYSTEM -> themeGroup.check(R.id.radio_theme_system)
        }
    }
}
