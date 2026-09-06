package com.right9code.anyhome.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.right9code.anyhome.R
import com.right9code.anyhome.data.PreferencesManager
import com.right9code.anyhome.engine.FontManager
import com.right9code.anyhome.engine.IconProcessor

class SettingsActivity : Activity() {

    private lateinit var settingsVersion: TextView
    private lateinit var settingsRows: SeekBar
    private lateinit var settingsRowsLabel: TextView
    private lateinit var settingsCols: SeekBar
    private lateinit var settingsColsLabel: TextView
    private lateinit var settingsIconMode: RadioGroup
    private lateinit var iconText: RadioButton
    private lateinit var iconMonogram: RadioButton
    private lateinit var iconSystem: RadioButton
    private lateinit var settingsShowPinned: CheckBox
    private lateinit var settingsShowNav: CheckBox
    private lateinit var settingsShowSearch: CheckBox
    private lateinit var settingsShowBorders: CheckBox
    private lateinit var settingsVolumeNav: CheckBox
    private lateinit var pickKioskBtn: Button
    private lateinit var pickFontBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferencesManager.init(this)
        setContentView(R.layout.activity_settings)

        settingsVersion = findViewById(R.id.settings_version)
        settingsRows = findViewById(R.id.settings_rows)
        settingsRowsLabel = findViewById(R.id.settings_rows_label)
        settingsCols = findViewById(R.id.settings_cols)
        settingsColsLabel = findViewById(R.id.settings_cols_label)
        settingsIconMode = findViewById(R.id.settings_icon_mode)
        iconText = findViewById(R.id.icon_text)
        iconMonogram = findViewById(R.id.icon_monogram)
        iconSystem = findViewById(R.id.icon_system)
        settingsShowPinned = findViewById(R.id.settings_show_pinned)
        settingsShowNav = findViewById(R.id.settings_show_nav)
        settingsShowSearch = findViewById(R.id.settings_show_search)
        settingsShowBorders = findViewById(R.id.settings_show_borders)
        settingsVolumeNav = findViewById(R.id.settings_volume_nav)
        pickKioskBtn = findViewById(R.id.pick_kiosk_btn)
        pickFontBtn = findViewById(R.id.pick_font_btn)

        settingsVersion.text = getString(R.string.version, getVersionName())

        val githubLink = findViewById<TextView>(R.id.settings_github_link)
        githubLink.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/right9code/AnyHome/releases"))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "https://github.com/right9code/AnyHome", Toast.LENGTH_SHORT).show()
            }
        }

        settingsRows.progress = PreferencesManager.rows - 1
        settingsRowsLabel.text = getString(R.string.pref_rows, PreferencesManager.rows)

        settingsCols.progress = PreferencesManager.cols - 1
        settingsColsLabel.text = getString(R.string.pref_cols, PreferencesManager.cols)

        when (PreferencesManager.iconMode) {
            0 -> iconText.isChecked = true
            1 -> iconMonogram.isChecked = true
            2 -> iconSystem.isChecked = true
        }

        settingsShowPinned.isChecked = PreferencesManager.showPinned
        settingsShowNav.isChecked = PreferencesManager.showNav
        settingsShowSearch.isChecked = PreferencesManager.showSearch
        settingsShowBorders.isChecked = PreferencesManager.showBorders
        settingsVolumeNav.isChecked = PreferencesManager.volumeNav

        settingsRows.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress + 1
                PreferencesManager.rows = value
                settingsRowsLabel.text = getString(R.string.pref_rows, value)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        settingsCols.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress + 1
                PreferencesManager.cols = value
                settingsColsLabel.text = getString(R.string.pref_cols, value)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        settingsIconMode.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.icon_text -> PreferencesManager.iconMode = 0
                R.id.icon_monogram -> PreferencesManager.iconMode = 1
                R.id.icon_system -> PreferencesManager.iconMode = 2
            }
        }

        settingsShowPinned.setOnCheckedChangeListener { _, isChecked ->
            PreferencesManager.showPinned = isChecked
        }
        settingsShowNav.setOnCheckedChangeListener { _, isChecked ->
            PreferencesManager.showNav = isChecked
        }
        settingsShowSearch.setOnCheckedChangeListener { _, isChecked ->
            PreferencesManager.showSearch = isChecked
        }
        settingsShowBorders.setOnCheckedChangeListener { _, isChecked ->
            PreferencesManager.showBorders = isChecked
        }
        settingsVolumeNav.setOnCheckedChangeListener { _, isChecked ->
            PreferencesManager.volumeNav = isChecked
        }

        pickKioskBtn.setOnClickListener {
            showKioskPicker()
        }

        pickFontBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("font/*", "application/x-font-ttf", "application/x-font-otf"))
            }
            startActivityForResult(intent, REQUEST_FONT_PICK)
        }

        if (intent.getBooleanExtra("EXTRA_PICK_KIOSK", false)) {
            showKioskPicker()
        }
    }

    private fun showKioskPicker() {
        val pm = packageManager
        val apps = mutableListOf<ResolveInfo>()
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }

        try {
            val launcherApps = getSystemService("launcher") as LauncherApps?
            launcherApps?.getActivityList(null, android.os.Process.myUserHandle())?.forEach { info ->
                val resolve = ResolveInfo()
                resolve.activityInfo = pm.getActivityInfo(info.componentName, PackageManager.MATCH_ALL)
                resolve.resolvePackageName = info.componentName.packageName
                apps.add(resolve)
            }
        } catch (e: Exception) {
            Log.w("AnyHome", "LauncherApps discovery failed", e)
        }

        if (apps.isEmpty()) {
            apps.addAll(pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL))
        }

        apps.sortBy { it.loadLabel(pm).toString().lowercase() }
        Log.d("AnyHome", "kioskPicker apps=${apps.size}")

        if (apps.isEmpty()) {
            Toast.makeText(this, "No apps found", Toast.LENGTH_SHORT).show()
            return
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val scrollContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            addView(scrollContainer)
        }

        container.addView(scrollView)

        for (app in apps) {
            val item = LinearLayout(this)
            item.orientation = LinearLayout.HORIZONTAL
            item.setPadding(16, 16, 16, 16)

            val iconView = ImageView(this)
            val iconSize = (48 * resources.displayMetrics.density).toInt()
            iconView.layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
            val icon = IconProcessor.processIcon(this, app.activityInfo.applicationInfo, PreferencesManager.iconMode)
            if (icon != null) {
                iconView.setImageDrawable(icon)
            } else {
                iconView.visibility = View.GONE
            }

            val label = TextView(this).apply {
                text = app.loadLabel(pm)
                setPadding(16, 0, 0, 0)
                textSize = 16f
                setTextColor(resources.getColor(android.R.color.black, null))
            }

            item.addView(iconView)
            item.addView(label)

            item.setOnClickListener {
                PreferencesManager.kioskPackage = app.activityInfo.packageName
                PreferencesManager.kioskEnabled = true
                finish()
            }

            scrollContainer.addView(item)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.pick_kiosk_app)
            .setView(container)
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || data == null) return
        when (requestCode) {
            REQUEST_FONT_PICK -> {
                val uri = data.data ?: return
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (FontManager.streamFontToCache(this, uri)) {
                    // success
                }
            }
        }
    }

    companion object {
        const val REQUEST_KIOSK_PICK = 1001
        const val REQUEST_FONT_PICK = 1002
    }

    private fun getVersionName(): String {
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
}
