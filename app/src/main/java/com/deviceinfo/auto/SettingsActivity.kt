package com.deviceinfo.auto

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Spinner
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: AppPreferences
    private lateinit var spinner: Spinner
    private lateinit var themeGroup: RadioGroup

    private var suppressChanges = true
    private var savedLanguageIndex = 0
    private var savedThemeMode: AppPreferences.ThemeMode? = null

    private val languageTags = listOf("", "en", "vi")

    private val dirtyBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            showUnsavedDialog { finish() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithEdgeBarsAndUpToolbar(R.layout.activity_settings, R.string.settings_title)
        prefs = AppPreferences(this)
        onBackPressedDispatcher.addCallback(this, dirtyBackCallback)

        spinner = findViewById(R.id.spinner_language)
        themeGroup = findViewById(R.id.radio_theme)

        val languageOptions = listOf(
            getString(R.string.theme_system),
            "English",
            "Tiếng Việt",
        )
        val adapter = ArrayAdapter(this, R.layout.item_spinner_settings, languageOptions)
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown_settings)
        spinner.adapter = adapter

        bindUiFromPrefs()

        findViewById<Button>(R.id.button_open_app_settings).setOnClickListener { openAppDetailsSettings() }
        findViewById<Button>(R.id.button_reset_defaults).setOnClickListener { confirmResetDefaults() }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (suppressChanges) return
                updateDirtyUi()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        themeGroup.setOnCheckedChangeListener { _, _ ->
            if (suppressChanges) return@setOnCheckedChangeListener
            updateDirtyUi()
        }

        spinner.post {
            themeGroup.post {
                suppressChanges = false
                updateDirtyUi()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_settings, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.action_save)?.isVisible = isDirty()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_save) {
            persistAndRecreate()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        if (isDirty()) {
            showUnsavedDialog { defaultFinishOnNavigateUp() }
            return true
        }
        return defaultFinishOnNavigateUp()
    }

    private fun isDirty(): Boolean {
        val baselineTheme = savedThemeMode ?: return false
        val langDirty = spinner.selectedItemPosition != savedLanguageIndex
        val themeDirty = themeModeFromCheckedId(themeGroup.checkedRadioButtonId) != baselineTheme
        return langDirty || themeDirty
    }

    private fun updateDirtyUi() {
        dirtyBackCallback.isEnabled = isDirty()
        invalidateOptionsMenu()
    }

    private fun persistAndRecreate() {
        prefs.setLanguageTag(languageTags[spinner.selectedItemPosition])
        prefs.setThemeMode(themeModeFromCheckedId(themeGroup.checkedRadioButtonId))
        findViewById<View>(R.id.root).post {
            if (!isFinishing && !isDestroyed) recreate()
        }
    }

    private fun themeModeFromCheckedId(checkedId: Int): AppPreferences.ThemeMode = when (checkedId) {
        R.id.radio_theme_light -> AppPreferences.ThemeMode.LIGHT
        R.id.radio_theme_dark -> AppPreferences.ThemeMode.DARK
        else -> AppPreferences.ThemeMode.SYSTEM
    }

    private fun showUnsavedDialog(onDiscard: () -> Unit) {
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.settings_unsaved_title)
            .setMessage(R.string.settings_unsaved_message)
            .setPositiveButton(R.string.settings_unsaved_discard) { _, _ -> onDiscard() }
            .setNegativeButton(R.string.settings_unsaved_keep, null)
            .create()
        dialog.setOnShowListener {
            val ta = theme.obtainStyledAttributes(intArrayOf(R.attr.colorDestructive))
            try {
                val danger = ta.getColor(0, ContextCompat.getColor(this@SettingsActivity, R.color.color_danger))
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(danger)
            } finally {
                ta.recycle()
            }
        }
        dialog.show()
    }

    private fun openAppDetailsSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun confirmResetDefaults() {
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.settings_reset_confirm_title)
            .setMessage(R.string.settings_reset_confirm_message)
            .setPositiveButton(R.string.settings_reset_confirm_yes) { _, _ ->
                prefs.resetToDefaults()
                findViewById<View>(R.id.root).post {
                    if (!isFinishing && !isDestroyed) recreate()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dialog.setOnShowListener {
            val ta = theme.obtainStyledAttributes(intArrayOf(R.attr.colorDestructive))
            try {
                val danger = ta.getColor(0, ContextCompat.getColor(this@SettingsActivity, R.color.color_danger))
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(danger)
            } finally {
                ta.recycle()
            }
        }
        dialog.show()
    }

    private fun bindUiFromPrefs() {
        suppressChanges = true
        val tag = prefs.getLanguageTag()
        val langIndex = if (tag.isEmpty()) 0 else languageTags.indexOf(tag).takeIf { it >= 0 } ?: 0
        savedLanguageIndex = langIndex
        val mode = prefs.getThemeMode()
        savedThemeMode = mode
        spinner.setSelection(langIndex)
        when (mode) {
            AppPreferences.ThemeMode.LIGHT -> themeGroup.check(R.id.radio_theme_light)
            AppPreferences.ThemeMode.DARK -> themeGroup.check(R.id.radio_theme_dark)
            AppPreferences.ThemeMode.SYSTEM -> themeGroup.check(R.id.radio_theme_system)
        }
    }
}
