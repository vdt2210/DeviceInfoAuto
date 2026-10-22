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

import android.widget.TextView

import androidx.activity.OnBackPressedCallback

import androidx.activity.result.contract.ActivityResultContracts

import androidx.appcompat.app.AlertDialog

import androidx.appcompat.app.AppCompatActivity

import com.google.android.material.color.MaterialColors



class SettingsActivity : AppCompatActivity() {



    private lateinit var prefs: AppPreferences

    private lateinit var spinner: Spinner

    private lateinit var themeGroup: RadioGroup

    private lateinit var unitsGroup: RadioGroup

    private lateinit var textPermissionsStatus: TextView

    private lateinit var buttonRequestPermissions: Button



    private val sensitivePermissionLauncher = registerForActivityResult(

        ActivityResultContracts.RequestMultiplePermissions(),

    ) {
        bindPermissionsUi()
        if (SensitivePermissions.denied(this).isEmpty()) {
            setResult(RESULT_OK)
        }
    }



    private var suppressChanges = true

    private var savedLanguageIndex = 0

    private var savedThemeMode: AppPreferences.ThemeMode? = null

    private var savedUnitMode: AppPreferences.UnitMode? = null



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

        unitsGroup = findViewById(R.id.radio_units)



        val languageOptions = listOf(

            getString(R.string.language_system),

            "English",

            "Tiếng Việt",

        )

        val adapter = ArrayAdapter(this, R.layout.item_spinner_settings, languageOptions)

        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown_settings)

        spinner.adapter = adapter



        bindUiFromPrefs()



        textPermissionsStatus = findViewById(R.id.text_permissions_status)

        buttonRequestPermissions = findViewById(R.id.button_request_permissions)

        buttonRequestPermissions.setOnClickListener { requestSensitivePermissions() }

        bindPermissionsUi()



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



        unitsGroup.setOnCheckedChangeListener { _, _ ->

            if (suppressChanges) return@setOnCheckedChangeListener

            updateDirtyUi()

        }



        spinner.post {

            themeGroup.post {

                unitsGroup.post {

                    suppressChanges = false

                    updateDirtyUi()

                }

            }

        }

    }



    override fun onResume() {

        super.onResume()

        bindPermissionsUi()

    }



    private fun bindPermissionsUi() {

        val allGranted = SensitivePermissions.allGranted(this)

        textPermissionsStatus.text = if (allGranted) {

            getString(R.string.settings_permissions_status_granted)

        } else {

            getString(

                R.string.settings_permissions_status_missing,

                SensitivePermissions.missingLabels(this).joinToString(", "),

            )

        }

        buttonRequestPermissions.post {
            buttonRequestPermissions.isEnabled = !SensitivePermissions.allGranted(this)
        }

    }



    private fun requestSensitivePermissions() {

        val denied = SensitivePermissions.activityRequestDenied(this)

        if (denied.isEmpty()) return

        sensitivePermissionLauncher.launch(denied.toTypedArray())

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

        val unitsDirty = unitModeFromCheckedId(unitsGroup.checkedRadioButtonId) !=

            (savedUnitMode ?: return false)

        return langDirty || themeDirty || unitsDirty

    }



    private fun updateDirtyUi() {

        dirtyBackCallback.isEnabled = isDirty()

        invalidateOptionsMenu()

    }



    private fun persistAndRecreate() {

        prefs.setLanguageTag(languageTags[spinner.selectedItemPosition])

        prefs.setThemeMode(themeModeFromCheckedId(themeGroup.checkedRadioButtonId))

        prefs.setUnitMode(unitModeFromCheckedId(unitsGroup.checkedRadioButtonId))

        setResult(RESULT_OK)
        findViewById<View>(R.id.root).post {

            if (!isFinishing && !isDestroyed) recreate()

        }

    }



    private fun themeModeFromCheckedId(checkedId: Int): AppPreferences.ThemeMode = when (checkedId) {

        R.id.radio_theme_light -> AppPreferences.ThemeMode.LIGHT

        R.id.radio_theme_dark -> AppPreferences.ThemeMode.DARK

        else -> AppPreferences.ThemeMode.SYSTEM

    }



    private fun unitModeFromCheckedId(checkedId: Int): AppPreferences.UnitMode = when (checkedId) {

        R.id.radio_units_metric -> AppPreferences.UnitMode.METRIC

        R.id.radio_units_imperial -> AppPreferences.UnitMode.IMPERIAL

        else -> AppPreferences.UnitMode.AUTO

    }



    private fun showUnsavedDialog(onDiscard: () -> Unit) {

        val dialog = AlertDialog.Builder(this)

            .setTitle(R.string.settings_unsaved_title)

            .setMessage(R.string.settings_unsaved_message)

            .setPositiveButton(R.string.settings_unsaved_discard) { _, _ -> onDiscard() }

            .setNegativeButton(R.string.settings_unsaved_keep, null)

            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(errorThemeColor())
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
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(errorThemeColor())
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

        val unitMode = prefs.getUnitMode()

        savedUnitMode = unitMode

        spinner.setSelection(langIndex)

        when (mode) {

            AppPreferences.ThemeMode.LIGHT -> themeGroup.check(R.id.radio_theme_light)

            AppPreferences.ThemeMode.DARK -> themeGroup.check(R.id.radio_theme_dark)

            AppPreferences.ThemeMode.SYSTEM -> themeGroup.check(R.id.radio_theme_system)

        }

        when (unitMode) {

            AppPreferences.UnitMode.METRIC -> unitsGroup.check(R.id.radio_units_metric)

            AppPreferences.UnitMode.IMPERIAL -> unitsGroup.check(R.id.radio_units_imperial)

            AppPreferences.UnitMode.AUTO -> unitsGroup.check(R.id.radio_units_auto)

        }

    }

    private fun errorThemeColor(): Int =
        MaterialColors.getColor(
            this,
            com.google.android.material.R.attr.colorError,
            javaClass.simpleName,
        )

}

