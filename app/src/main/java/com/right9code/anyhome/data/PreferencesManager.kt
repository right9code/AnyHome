package com.right9code.anyhome.data

import android.content.Context
import androidx.preference.PreferenceManager

object PreferencesManager {

    private const val PREF_ROWS = "pref_rows"
    private const val PREF_COLS = "pref_cols"
    private const val PREF_ICON_MODE = "pref_icon_mode"
    private const val PREF_KIOSK_PKG = "pref_kiosk_pkg"
    private const val PREF_KIOSK_ENABLED = "pref_kiosk_enabled"
    private const val PREF_SHOW_PINNED = "pref_show_pinned"
    private const val PREF_SHOW_NAV = "pref_show_nav"
    private const val PREF_SHOW_SEARCH = "pref_show_search"
    private const val PREF_SHOW_BORDERS = "pref_show_borders"
    private const val PREF_CUSTOM_FONT = "pref_custom_font"
    private const val PREF_PINNED_APPS = "pref_pinned_apps"
    private const val PREF_VOLUME_NAV = "pref_volume_nav"
    private const val PREF_ACTION_PAGE_TRIPLE = "pref_action_page_triple"
    private const val PREF_ACTION_PREV_LONG = "pref_action_prev_long"
    private const val PREF_ACTION_NEXT_LONG = "pref_action_next_long"
    private const val PREF_ACTION_TIME_LONG = "pref_action_time_long"
    private const val PREF_ACTION_SETTINGS_LONG = "pref_action_settings_long"

    private const val PREF_APP_PAGE_TRIPLE = "pref_app_page_triple"
    private const val PREF_APP_PREV_LONG = "pref_app_prev_long"
    private const val PREF_APP_NEXT_LONG = "pref_app_next_long"
    private const val PREF_APP_TIME_LONG = "pref_app_time_long"
    private const val PREF_APP_SETTINGS_LONG = "pref_app_settings_long"

    // Action identifiers:
    // 0: None
    // 1: Toggle Frontlight (On/Off)
    // 2: Notification Shade
    // 3: Control Center / EinkCenter
    // 4: Refresh E-Ink Screen
    // 5: Launcher Settings
    // 6: Lock Screen
    // 7: System Settings (Android OS Settings)
    // 8: Launch App
    const val ACTION_NONE = 0
    const val ACTION_TOGGLE_FRONTLIGHT = 1
    const val ACTION_NOTIFICATION_SHADE = 2
    const val ACTION_CONTROL_CENTER = 3
    const val ACTION_REFRESH_SCREEN = 4
    const val ACTION_LAUNCHER_SETTINGS = 5
    const val ACTION_LOCK_SCREEN = 6
    const val ACTION_SYSTEM_SETTINGS = 7
    const val ACTION_LAUNCH_APP = 8

    private lateinit var prefs: android.content.SharedPreferences

    fun init(context: Context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    }

    var rows: Int
        get() = prefs.getInt(PREF_ROWS, 4)
        set(value) = prefs.edit().putInt(PREF_ROWS, value).apply()

    var cols: Int
        get() = prefs.getInt(PREF_COLS, 3)
        set(value) = prefs.edit().putInt(PREF_COLS, value).apply()

    var iconMode: Int
        get() = prefs.getInt(PREF_ICON_MODE, 2)
        set(value) = prefs.edit().putInt(PREF_ICON_MODE, value).apply()

    fun resetIconModeIfInvalid() {
        val current = prefs.getInt(PREF_ICON_MODE, -1)
        if (current < 0 || current > 2) {
            prefs.edit().putInt(PREF_ICON_MODE, 2).apply()
        }
    }

    var kioskPackage: String?
        get() = prefs.getString(PREF_KIOSK_PKG, null)
        set(value) = prefs.edit().putString(PREF_KIOSK_PKG, value).apply()

    var kioskEnabled: Boolean
        get() = prefs.getBoolean(PREF_KIOSK_ENABLED, false)
        set(value) = prefs.edit().putBoolean(PREF_KIOSK_ENABLED, value).apply()

    var showPinned: Boolean
        get() = prefs.getBoolean(PREF_SHOW_PINNED, false)
        set(value) = prefs.edit().putBoolean(PREF_SHOW_PINNED, value).apply()

    var showNav: Boolean
        get() = prefs.getBoolean(PREF_SHOW_NAV, true)
        set(value) = prefs.edit().putBoolean(PREF_SHOW_NAV, value).apply()

    var showSearch: Boolean
        get() = prefs.getBoolean(PREF_SHOW_SEARCH, true)
        set(value) = prefs.edit().putBoolean(PREF_SHOW_SEARCH, value).apply()

    var showBorders: Boolean
        get() = prefs.getBoolean(PREF_SHOW_BORDERS, false)
        set(value) = prefs.edit().putBoolean(PREF_SHOW_BORDERS, value).apply()

    var customFont: String?
        get() = prefs.getString(PREF_CUSTOM_FONT, null)
        set(value) = prefs.edit().putString(PREF_CUSTOM_FONT, value).apply()

    var pinnedApps: Set<String>
        get() = prefs.getStringSet(PREF_PINNED_APPS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(PREF_PINNED_APPS, value).apply()

    var volumeNav: Boolean
        get() = prefs.getBoolean(PREF_VOLUME_NAV, false)
        set(value) = prefs.edit().putBoolean(PREF_VOLUME_NAV, value).apply()

    var actionPageTriple: Int
        get() = prefs.getInt(PREF_ACTION_PAGE_TRIPLE, ACTION_TOGGLE_FRONTLIGHT)
        set(value) = prefs.edit().putInt(PREF_ACTION_PAGE_TRIPLE, value).apply()

    var actionPrevLong: Int
        get() = prefs.getInt(PREF_ACTION_PREV_LONG, ACTION_NOTIFICATION_SHADE)
        set(value) = prefs.edit().putInt(PREF_ACTION_PREV_LONG, value).apply()

    var actionNextLong: Int
        get() = prefs.getInt(PREF_ACTION_NEXT_LONG, ACTION_CONTROL_CENTER)
        set(value) = prefs.edit().putInt(PREF_ACTION_NEXT_LONG, value).apply()

    var actionTimeLong: Int
        get() = prefs.getInt(PREF_ACTION_TIME_LONG, ACTION_LOCK_SCREEN)
        set(value) = prefs.edit().putInt(PREF_ACTION_TIME_LONG, value).apply()

    var actionSettingsLong: Int
        get() = prefs.getInt(PREF_ACTION_SETTINGS_LONG, ACTION_SYSTEM_SETTINGS)
        set(value) = prefs.edit().putInt(PREF_ACTION_SETTINGS_LONG, value).apply()

    var actionPageTriplePkg: String?
        get() = prefs.getString(PREF_APP_PAGE_TRIPLE, null)
        set(value) = prefs.edit().putString(PREF_APP_PAGE_TRIPLE, value).apply()

    var actionPrevLongPkg: String?
        get() = prefs.getString(PREF_APP_PREV_LONG, null)
        set(value) = prefs.edit().putString(PREF_APP_PREV_LONG, value).apply()

    var actionNextLongPkg: String?
        get() = prefs.getString(PREF_APP_NEXT_LONG, null)
        set(value) = prefs.edit().putString(PREF_APP_NEXT_LONG, value).apply()

    var actionTimeLongPkg: String?
        get() = prefs.getString(PREF_APP_TIME_LONG, null)
        set(value) = prefs.edit().putString(PREF_APP_TIME_LONG, value).apply()

    var actionSettingsLongPkg: String?
        get() = prefs.getString(PREF_APP_SETTINGS_LONG, null)
        set(value) = prefs.edit().putString(PREF_APP_SETTINGS_LONG, value).apply()
}
