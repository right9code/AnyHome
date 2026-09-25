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
    private lateinit var settingsEnableLockscreen: CheckBox
    private lateinit var pickKioskBtn: Button
    private lateinit var pickFontBtn: Button
    private lateinit var btnGesturePageTriple: Button
    private lateinit var btnGesturePrevLong: Button
    private lateinit var btnGestureNextLong: Button
    private lateinit var btnGestureTimeLong: Button
    private lateinit var btnGestureSettingsLong: Button

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
        settingsEnableLockscreen = findViewById(R.id.settings_enable_lockscreen)
        pickKioskBtn = findViewById(R.id.pick_kiosk_btn)
        pickFontBtn = findViewById(R.id.pick_font_btn)
        btnGesturePageTriple = findViewById(R.id.btn_gesture_page_triple)
        btnGesturePrevLong = findViewById(R.id.btn_gesture_prev_long)
        btnGestureNextLong = findViewById(R.id.btn_gesture_next_long)
        btnGestureTimeLong = findViewById(R.id.btn_gesture_time_long)
        btnGestureSettingsLong = findViewById(R.id.btn_gesture_settings_long)

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
        settingsEnableLockscreen.isChecked = PreferencesManager.enableLockscreen

        setupGestureButtons()

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
        settingsEnableLockscreen.setOnCheckedChangeListener { _, isChecked ->
            PreferencesManager.enableLockscreen = isChecked
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

    private fun getAppLabel(pkg: String?): String? {
        if (pkg.isNullOrEmpty()) return null
        return try {
            val appInfo = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            pkg
        }
    }

    private fun getActionName(actionId: Int, appPkg: String?): String {
        return when (actionId) {
            PreferencesManager.ACTION_TOGGLE_FRONTLIGHT -> "Toggle Frontlight (On/Off)"
            PreferencesManager.ACTION_NOTIFICATION_SHADE -> "Open Notification Shade"
            PreferencesManager.ACTION_CONTROL_CENTER -> "Open Control Center / Quick Settings"
            PreferencesManager.ACTION_REFRESH_SCREEN -> "Refresh E-Ink Display"
            PreferencesManager.ACTION_LAUNCHER_SETTINGS -> "Open Launcher Settings"
            PreferencesManager.ACTION_LOCK_SCREEN -> "Lock Screen"
            PreferencesManager.ACTION_SYSTEM_SETTINGS -> "Open Android System Settings"
            PreferencesManager.ACTION_LAUNCH_APP -> "Launch: ${getAppLabel(appPkg) ?: "App"}"
            else -> "None"
        }
    }

    private fun setupGestureButtons() {
        updateGestureLabels()

        val actionOptions = arrayOf(
            "Toggle Frontlight (On/Off)",
            "Open Notification Shade",
            "Open Control Center / Quick Settings",
            "Open Android System Settings",
            "Refresh E-Ink Display",
            "Open Launcher Settings",
            "Lock Screen",
            "Launch Installed App…",
            "None"
        )
        val actionIds = intArrayOf(
            PreferencesManager.ACTION_TOGGLE_FRONTLIGHT,
            PreferencesManager.ACTION_NOTIFICATION_SHADE,
            PreferencesManager.ACTION_CONTROL_CENTER,
            PreferencesManager.ACTION_SYSTEM_SETTINGS,
            PreferencesManager.ACTION_REFRESH_SCREEN,
            PreferencesManager.ACTION_LAUNCHER_SETTINGS,
            PreferencesManager.ACTION_LOCK_SCREEN,
            PreferencesManager.ACTION_LAUNCH_APP,
            PreferencesManager.ACTION_NONE
        )

        btnGesturePageTriple.setOnClickListener {
            showActionSelector("Page Number Triple-Tap", actionOptions, actionIds,
                onActionSelected = { chosenId ->
                    PreferencesManager.actionPageTriple = chosenId
                    PreferencesManager.actionPageTriplePkg = null
                    updateGestureLabels()
                },
                onAppPick = { pkg ->
                    PreferencesManager.actionPageTriple = PreferencesManager.ACTION_LAUNCH_APP
                    PreferencesManager.actionPageTriplePkg = pkg
                    updateGestureLabels()
                }
            )
        }

        btnGesturePrevLong.setOnClickListener {
            showActionSelector("PREV Button Long-Press", actionOptions, actionIds,
                onActionSelected = { chosenId ->
                    PreferencesManager.actionPrevLong = chosenId
                    PreferencesManager.actionPrevLongPkg = null
                    updateGestureLabels()
                },
                onAppPick = { pkg ->
                    PreferencesManager.actionPrevLong = PreferencesManager.ACTION_LAUNCH_APP
                    PreferencesManager.actionPrevLongPkg = pkg
                    updateGestureLabels()
                }
            )
        }

        btnGestureNextLong.setOnClickListener {
            showActionSelector("NEXT Button Long-Press", actionOptions, actionIds,
                onActionSelected = { chosenId ->
                    PreferencesManager.actionNextLong = chosenId
                    PreferencesManager.actionNextLongPkg = null
                    updateGestureLabels()
                },
                onAppPick = { pkg ->
                    PreferencesManager.actionNextLong = PreferencesManager.ACTION_LAUNCH_APP
                    PreferencesManager.actionNextLongPkg = pkg
                    updateGestureLabels()
                }
            )
        }

        btnGestureTimeLong.setOnClickListener {
            showActionSelector("Time Widget Long-Press", actionOptions, actionIds,
                onActionSelected = { chosenId ->
                    PreferencesManager.actionTimeLong = chosenId
                    PreferencesManager.actionTimeLongPkg = null
                    updateGestureLabels()
                },
                onAppPick = { pkg ->
                    PreferencesManager.actionTimeLong = PreferencesManager.ACTION_LAUNCH_APP
                    PreferencesManager.actionTimeLongPkg = pkg
                    updateGestureLabels()
                }
            )
        }

        btnGestureSettingsLong.setOnClickListener {
            showActionSelector("Settings Icon Long-Press", actionOptions, actionIds,
                onActionSelected = { chosenId ->
                    PreferencesManager.actionSettingsLong = chosenId
                    PreferencesManager.actionSettingsLongPkg = null
                    updateGestureLabels()
                },
                onAppPick = { pkg ->
                    PreferencesManager.actionSettingsLong = PreferencesManager.ACTION_LAUNCH_APP
                    PreferencesManager.actionSettingsLongPkg = pkg
                    updateGestureLabels()
                }
            )
        }
    }

    private fun updateGestureLabels() {
        btnGesturePageTriple.text = "Page 3-Tap: ${getActionName(PreferencesManager.actionPageTriple, PreferencesManager.actionPageTriplePkg)}"
        btnGesturePrevLong.text = "PREV Long-Press: ${getActionName(PreferencesManager.actionPrevLong, PreferencesManager.actionPrevLongPkg)}"
        btnGestureNextLong.text = "NEXT Long-Press: ${getActionName(PreferencesManager.actionNextLong, PreferencesManager.actionNextLongPkg)}"
        btnGestureTimeLong.text = "Time Long-Press: ${getActionName(PreferencesManager.actionTimeLong, PreferencesManager.actionTimeLongPkg)}"
        btnGestureSettingsLong.text = "Gear Long-Press: ${getActionName(PreferencesManager.actionSettingsLong, PreferencesManager.actionSettingsLongPkg)}"
    }

    private fun showActionSelector(
        title: String,
        options: Array<String>,
        ids: IntArray,
        onActionSelected: (Int) -> Unit,
        onAppPick: (String) -> Unit
    ) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(options) { dialog, which ->
                val chosenId = ids[which]
                if (chosenId == PreferencesManager.ACTION_LAUNCH_APP) {
                    dialog.dismiss()
                    showAppPickerForGesture(title, onAppPick)
                } else {
                    onActionSelected(chosenId)
                    dialog.dismiss()
                }
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showAppPickerForGesture(title: String, onAppSelected: (String) -> Unit) {
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

        var dialogRef: AlertDialog? = null

        for (app in apps) {
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(16, 16, 16, 16)
            }

            val iconView = ImageView(this).apply {
                val iconSize = (48 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
                val icon = IconProcessor.processIcon(this@SettingsActivity, app.activityInfo.applicationInfo, PreferencesManager.iconMode)
                if (icon != null) {
                    setImageDrawable(icon)
                } else {
                    visibility = View.GONE
                }
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
                onAppSelected(app.activityInfo.packageName)
                dialogRef?.dismiss()
            }

            scrollContainer.addView(item)
        }

        dialogRef = AlertDialog.Builder(this)
            .setTitle(title)
            .setView(container)
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .show()
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
