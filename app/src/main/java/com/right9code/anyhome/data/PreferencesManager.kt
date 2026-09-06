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
    private const val PREF_EDGE_SWIPE_ENABLED = "pref_edge_swipe_enabled"
    private const val PREF_COLD_LIGHT_ON_LEFT = "pref_cold_light_on_left"
    private const val PREF_WARM_LIGHT_ON_RIGHT = "pref_warm_light_on_right"
    private const val PREF_COLD_LIGHT_LEVEL = "pref_cold_light_level"
    private const val PREF_WARM_LIGHT_LEVEL = "pref_warm_light_level"

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

    var edgeSwipeEnabled: Boolean
        get() = prefs.getBoolean(PREF_EDGE_SWIPE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(PREF_EDGE_SWIPE_ENABLED, value).apply()

    var coldLightOnLeft: Boolean
        get() = prefs.getBoolean(PREF_COLD_LIGHT_ON_LEFT, true)
        set(value) = prefs.edit().putBoolean(PREF_COLD_LIGHT_ON_LEFT, value).apply()

    var warmLightOnRight: Boolean
        get() = prefs.getBoolean(PREF_WARM_LIGHT_ON_RIGHT, true)
        set(value) = prefs.edit().putBoolean(PREF_WARM_LIGHT_ON_RIGHT, value).apply()

    var coldLightLevel: Int
        get() = prefs.getInt(PREF_COLD_LIGHT_LEVEL, 5)
        set(value) = prefs.edit().putInt(PREF_COLD_LIGHT_LEVEL, value).apply()

    var warmLightLevel: Int
        get() = prefs.getInt(PREF_WARM_LIGHT_LEVEL, 7)
        set(value) = prefs.edit().putInt(PREF_WARM_LIGHT_LEVEL, value).apply()
}
