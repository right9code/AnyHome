package com.right9code.anyhome

import android.app.Activity
import android.app.AlertDialog
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Process
import android.graphics.Color
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.right9code.anyhome.data.PreferencesManager
import com.right9code.anyhome.engine.FontManager
import com.right9code.anyhome.engine.IconProcessor
import com.right9code.anyhome.engine.Page
import com.right9code.anyhome.engine.PagedAppViewManager
import com.right9code.anyhome.ui.SettingsActivity
import kotlin.math.abs

class MainActivity : Activity() {

    private lateinit var root: LinearLayout
    private lateinit var header: LinearLayout
    private lateinit var time: TextView
    private lateinit var date: TextView
    private lateinit var battery: TextView
    private lateinit var settingsBtn: ImageButton
    private lateinit var pinnedContainer: LinearLayout
    private lateinit var searchContainer: LinearLayout
    private lateinit var searchEdit: EditText
    private lateinit var gridContainer: LinearLayout
    private lateinit var navBar: LinearLayout
    private lateinit var prevBtn: Button
    private lateinit var nextBtn: Button
    private lateinit var pageLabel: TextView
    private var allApps: List<ResolveInfo> = emptyList()
    private var pages: List<Page> = emptyList()
    private var currentPage = 1
    private var lastKioskLaunch = 0L
    private var consecutiveQuickReturns = 0
    private var lastHomeTapTime = 0L
    private var homeTapCount = 0
    private val handler = Handler(Looper.getMainLooper())
    private val pinnedApps = mutableSetOf<String>()

    private lateinit var gestureDetector: GestureDetector
    private lateinit var scaleDetector: ScaleGestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferencesManager.init(this)
        PreferencesManager.resetIconModeIfInvalid()
        setContentView(R.layout.activity_main)

        root = findViewById(R.id.root)
        header = findViewById(R.id.header)
        time = findViewById(R.id.time)
        date = findViewById(R.id.date)
        battery = findViewById(R.id.battery)
        settingsBtn = findViewById(R.id.settings_btn)
        pinnedContainer = findViewById(R.id.pinned_container)
        searchContainer = findViewById(R.id.search_container)
        searchEdit = findViewById(R.id.search_edit)
        gridContainer = findViewById(R.id.grid_container)
        navBar = findViewById(R.id.nav_bar)
        prevBtn = findViewById(R.id.prev_btn)
        nextBtn = findViewById(R.id.next_btn)
        pageLabel = findViewById(R.id.page_label)

        setupFullScreen()

        loadApps()
        setupHeader()
        setupGestures()
        setupControls()
        applySettings()
        recalcPages()
        renderPage()
        Log.d("AnyHome", "onCreate complete apps=${allApps.size} pages=${pages.size}")
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(headerReceiver)
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun loadApps() {
        val pm = packageManager
        val apps = linkedMapOf<String, ResolveInfo>()

        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        try {
            val launcherApps = getSystemService(LauncherApps::class.java)
            launcherApps?.getActivityList(null, Process.myUserHandle())?.forEach { info ->
                addResolveInfo(apps, resolveInfoFor(info.componentName))
            }
        } catch (e: Exception) {
            Log.w("AnyHome", "LauncherApps discovery failed", e)
        }

        addResolveInfos(apps, safeQuery(pm, launcherIntent))
        pm.getInstalledApplications(PackageManager.MATCH_ALL).forEach { appInfo ->
            val packageIntent = Intent(launcherIntent).setPackage(appInfo.packageName)
            addResolveInfos(apps, safeQuery(pm, packageIntent))

            if (apps.keys.none { it.startsWith("${appInfo.packageName}/") }) {
                try {
                    addResolveInfo(apps, pm.resolveActivity(packageIntent, PackageManager.MATCH_ALL))
                } catch (_: Exception) {
                    // The package may be visible but not launchable for this user.
                }
            }
        }

        allApps = apps.values.sortedBy { it.loadLabel(pm).toString().lowercase() }
        Log.d("AnyHome", "loadApps final=${allApps.size}")
        allApps.take(30).forEach { Log.d("AnyHome", "app=${it.loadLabel(pm)} pkg=${it.activityInfo.packageName}") }
    }

    private fun safeQuery(pm: PackageManager, intent: Intent): List<ResolveInfo> {
        return try {
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        } catch (e: Exception) {
            Log.w("AnyHome", "Launcher query failed for ${intent.`package`}", e)
            emptyList()
        }
    }

    private fun addResolveInfos(apps: MutableMap<String, ResolveInfo>, resolved: List<ResolveInfo>) {
        resolved.forEach { addResolveInfo(apps, it) }
    }

    private fun addResolveInfo(apps: MutableMap<String, ResolveInfo>, resolveInfo: ResolveInfo?) {
        val activityInfo = resolveInfo?.activityInfo ?: return
        apps["${activityInfo.packageName}/${activityInfo.name}"] = resolveInfo
    }

    private fun resolveInfoFor(component: ComponentName): ResolveInfo? {
        return try {
            ResolveInfo().apply {
                activityInfo = packageManager.getActivityInfo(component, PackageManager.MATCH_ALL)
                resolvePackageName = component.packageName
            }
        } catch (e: Exception) {
            Log.w("AnyHome", "Unable to read launcher activity $component", e)
            null
        }
    }

    private fun setupHeader() {
        updateTimeAndDate()
        updateBattery()

        val filter = android.content.IntentFilter().apply {
            addAction(android.content.Intent.ACTION_TIME_TICK)
            addAction(android.content.Intent.ACTION_TIME_CHANGED)
            addAction(android.content.Intent.ACTION_DATE_CHANGED)
            addAction(android.content.Intent.ACTION_BATTERY_CHANGED)
        }
        registerReceiver(headerReceiver, filter)

        settingsBtn.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        time.setOnClickListener { triggerEInkRefresh() }
        date.setOnClickListener { triggerEInkRefresh() }
    }

    private fun updateTimeAndDate() {
        val now = java.util.Calendar.getInstance()
        val timeFormat = android.text.format.DateFormat.getTimeFormat(this)
        val dateFormat = android.text.format.DateFormat.getMediumDateFormat(this)
        time.text = timeFormat.format(now.time)
        date.text = dateFormat.format(now.time)
    }

    private fun updateBattery() {
        val intent = registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 0
        val charging = plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS
        battery.text = if (charging) {
            "${batteryPct}% AC"
        } else {
            "$batteryPct%"
        }
    }

    private val headerReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            when (intent?.action) {
                android.content.Intent.ACTION_TIME_TICK,
                android.content.Intent.ACTION_TIME_CHANGED,
                android.content.Intent.ACTION_DATE_CHANGED -> updateTimeAndDate()
                android.content.Intent.ACTION_BATTERY_CHANGED -> updateBattery()
            }
        }
    }

    private fun setupGestures() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                // Accept the stream so GestureDetector can recognize a later fling.
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (isTapOnClock(e)) {
                    performLock()
                    return true
                }
                return false
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (isTapOnClock(e)) {
                    triggerEInkRefresh()
                    return true
                }
                return false
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                val dx = e2.x - (e1?.x ?: 0f)
                val dy = e2.y - (e1?.y ?: 0f)
                val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                if (dist < dpToPx(40)) return false

                if (abs(dx) > abs(dy) * 1.5f) {
                    if (dx < 0) nextPage() else prevPage()
                    return true
                }
                if (abs(dy) > abs(dx) * 1.5f) {
                    if (dy < 0) swipeUp() else swipeDown()
                    return true
                }
                if (dist > dpToPx(80)) {
                    triggerEInkRefresh()
                    return true
                }
                return false
            }
        })

        scaleDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (detector.scaleFactor < 0.7f) {
                    startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                    return true
                }
                return false
            }
        })

    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // Observe touches before child app tiles get a chance to consume them.
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return super.dispatchTouchEvent(event)
    }

    private fun setupFullScreen() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
    }

    private fun setupControls() {
        prevBtn.setOnClickListener { prevPage() }
        nextBtn.setOnClickListener { nextPage() }

        searchEdit.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable) {
                val q = s.toString()
                if (q.isNotEmpty()) {
                    val filtered = PagedAppViewManager.filterApps(q, allApps, packageManager)
                    pages = PagedAppViewManager.chunkIntoPages(filtered, PreferencesManager.rows, PreferencesManager.cols)
                    currentPage = 1
                } else {
                    recalcPages()
                }
                renderPage()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun applySettings() {
        searchContainer.visibility = if (PreferencesManager.showSearch) View.VISIBLE else View.GONE
        navBar.visibility = if (PreferencesManager.showNav) View.VISIBLE else View.GONE
        pinnedContainer.visibility = if (PreferencesManager.showPinned) View.VISIBLE else View.GONE
        pinnedApps.clear()
        pinnedApps.addAll(PreferencesManager.pinnedApps)
        renderPinnedApps()
    }

    private fun recalcPages() {
        pages = PagedAppViewManager.chunkIntoPages(allApps, PreferencesManager.rows, PreferencesManager.cols)
    }

    private fun renderPage() {
        gridContainer.removeAllViews()
        if (pages.isEmpty()) {
            pageLabel.text = "Page 0 / 0"
            return
        }
        val pageIndex = currentPage - 1
        if (pageIndex !in pages.indices) {
            currentPage = 1
            renderPage()
            return
        }
        val page = pages[pageIndex]
        val pageTotal = pages.size
        pageLabel.text = getString(R.string.page, currentPage, pageTotal)

        val pm = packageManager
        val colCount = PreferencesManager.cols

        for (rowIndex in page.apps.indices step colCount) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )

            for (colIndex in 0 until colCount) {
                val appIndex = rowIndex + colIndex
                if (appIndex >= page.apps.size) break

                val app = page.apps[appIndex]
                val item = layoutInflater.inflate(R.layout.item_app, row, false)
                val icon = item.findViewById<android.widget.ImageView>(R.id.icon)
                val label = item.findViewById<TextView>(R.id.label)

                label.text = app.loadLabel(pm)

                val mode = PreferencesManager.iconMode
                val drawable = IconProcessor.processIcon(this, app.activityInfo.applicationInfo, mode)
                if (drawable != null) {
                    icon.setImageDrawable(drawable)
                    icon.visibility = View.VISIBLE
                } else {
                    icon.visibility = View.GONE
                }

                if (PreferencesManager.showBorders) {
                    item.setBackgroundResource(R.drawable.bg_border_rect)
                } else {
                    item.setBackgroundResource(0)
                }

                FontManager.applyToTextView(this, label)

                val itemParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                itemParams.setMargins(2, 2, 2, 2)
                item.layoutParams = itemParams

                item.setOnClickListener {
                    val launchIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        component = ComponentName(app.activityInfo.packageName, app.activityInfo.name)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    }
                    startActivity(launchIntent)
                }

                item.setOnLongClickListener {
                    showAppOptions(app)
                    true
                }

                row.addView(item)
            }

            gridContainer.addView(row)
        }
    }

    private fun showAppOptions(app: ResolveInfo) {
        val pm = packageManager
        val label = app.loadLabel(pm).toString()
        val pkgName = app.activityInfo.packageName
        val isPinned = pinnedApps.contains(pkgName)
        val appInfo = app.activityInfo.applicationInfo
        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val isUpdatedSystemApp = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        val canUninstall = !isSystemApp || isUpdatedSystemApp

        val options = mutableListOf<String>()
        options.add("Set as Home App")
        options.add(if (isPinned) "Unpin from Top" else "Pin to Top")
        if (canUninstall) {
            options.add("Uninstall")
        }
        options.add("App Info")

        AlertDialog.Builder(this)
            .setTitle(label)
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "Set as Home App" -> {
                        PreferencesManager.kioskPackage = pkgName
                        PreferencesManager.kioskEnabled = true
                        Toast.makeText(this, "Home App set", Toast.LENGTH_SHORT).show()
                    }
                    "Pin to Top" -> {
                        pinnedApps.add(pkgName)
                        PreferencesManager.pinnedApps = pinnedApps
                        renderPinnedApps()
                        Toast.makeText(this, "Pinned to top", Toast.LENGTH_SHORT).show()
                    }
                    "Unpin from Top" -> {
                        pinnedApps.remove(pkgName)
                        PreferencesManager.pinnedApps = pinnedApps
                        renderPinnedApps()
                        Toast.makeText(this, "Unpinned", Toast.LENGTH_SHORT).show()
                    }
                    "Uninstall" -> {
                        val uninstallIntent = Intent(Intent.ACTION_DELETE).apply {
                            data = Uri.parse("package:$pkgName")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(uninstallIntent)
                    }
                    "App Info" -> {
                        val infoIntent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:$pkgName")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(infoIntent)
                    }
                }
            }
            .show()
    }

    private fun renderPinnedApps() {
        pinnedContainer.removeAllViews()
        if (!PreferencesManager.showPinned || pinnedApps.isEmpty()) {
            pinnedContainer.visibility = View.GONE
            return
        }
        pinnedContainer.visibility = View.VISIBLE
        val pm = packageManager
        for (pkg in pinnedApps) {
            val app = allApps.find { it.activityInfo.packageName == pkg } ?: continue
            val item = layoutInflater.inflate(R.layout.item_app, pinnedContainer, false)
            val icon = item.findViewById<android.widget.ImageView>(R.id.icon)
            val label = item.findViewById<TextView>(R.id.label)

            label.text = app.loadLabel(pm)
            val mode = PreferencesManager.iconMode
            val drawable = IconProcessor.processIcon(this, app.activityInfo.applicationInfo, mode)
            if (drawable != null) {
                icon.setImageDrawable(drawable)
                icon.visibility = View.VISIBLE
            } else {
                icon.visibility = View.GONE
            }

            if (PreferencesManager.showBorders) {
                item.setBackgroundResource(R.drawable.bg_border_rect)
            } else {
                item.setBackgroundResource(0)
            }

            FontManager.applyToTextView(this, label)

            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            params.setMargins(2, 2, 2, 2)
            item.layoutParams = params

            item.setOnClickListener {
                val launchIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    component = ComponentName(app.activityInfo.packageName, app.activityInfo.name)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                startActivity(launchIntent)
            }

            item.setOnLongClickListener {
                showAppOptions(app)
                true
            }

            pinnedContainer.addView(item)
        }
    }

    private fun prevPage() {
        if (currentPage > 1) {
            currentPage--
            renderPage()
        }
    }

    private fun nextPage() {
        if (currentPage < pages.size) {
            currentPage++
            renderPage()
        }
    }

    private fun swipeDown() {
        try {
            val statusBarService = getSystemService(STATUS_BAR_SERVICE)
            val method = statusBarService.javaClass.getMethod("expandNotificationsPanel")
            method.invoke(statusBarService)
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun swipeUp() {
        searchEdit.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(searchEdit, 0)
    }

    private fun triggerEInkRefresh() {
        val overlay = View(this)
        overlay.setBackgroundColor(Color.BLACK)
        root.addView(overlay, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        handler.postDelayed({
            overlay.setBackgroundColor(Color.WHITE)
            handler.postDelayed({
                root.removeView(overlay)
            }, 80)
        }, 80)
    }

    private fun performLock() {
        val km = getSystemService(KEYGUARD_SERVICE) as android.app.KeyguardManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            km.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            km.newKeyguardLock("AnyHome").disableKeyguard()
        }
    }

    private fun isTapOnClock(e: MotionEvent): Boolean {
        val loc = IntArray(2)
        time.getLocationOnScreen(loc)
        val x = loc[0]
        val y = loc[1]
        return e.rawX >= x && e.rawX <= x + time.width && e.rawY >= y && e.rawY <= y + time.height
    }

    override fun onResume() {
        super.onResume()
        if (PreferencesManager.kioskEnabled) {
            val now = SystemClock.elapsedRealtime()
            val timeSinceLastLaunch = now - lastKioskLaunch
            if (lastKioskLaunch > 0L && timeSinceLastLaunch < 2500L) {
                consecutiveQuickReturns++
                if (consecutiveQuickReturns >= 4) {
                    PreferencesManager.kioskEnabled = false
                    consecutiveQuickReturns = 0
                    Toast.makeText(this, "Home App loop detected: Mode OFF", Toast.LENGTH_LONG).show()
                }
            } else {
                consecutiveQuickReturns = 0
            }
        }

        if (PreferencesManager.kioskEnabled) {
            val pkg = PreferencesManager.kioskPackage
            if (!pkg.isNullOrEmpty()) {
                val pm = packageManager
                val intent = pm.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    lastKioskLaunch = SystemClock.elapsedRealtime()
                    startActivity(intent)
                    return
                }
            }
        }
        loadApps()
        recalcPages()
        applySettings()
        renderPage()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val now = SystemClock.elapsedRealtime()
        if (now - lastHomeTapTime < 1500L) {
            homeTapCount++
        } else {
            homeTapCount = 1
        }
        lastHomeTapTime = now

        if (PreferencesManager.kioskEnabled) {
            if (homeTapCount >= 5) {
                homeTapCount = 0
                PreferencesManager.kioskEnabled = false
                Toast.makeText(this, "Home App mode OFF", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Launcher mode: pressing Home returns to first page and clears search
            if (currentPage != 1) {
                currentPage = 1
                renderPage()
            }
            if (searchEdit.text.isNotEmpty()) {
                searchEdit.setText("")
            }
        }
    }

    private fun toggleKioskMode() {
        if (PreferencesManager.kioskEnabled) {
            PreferencesManager.kioskEnabled = false
            Toast.makeText(this, "Home App mode OFF", Toast.LENGTH_SHORT).show()
        } else {
            val pkg = PreferencesManager.kioskPackage
            if (pkg != null) {
                PreferencesManager.kioskEnabled = true
                val pm = packageManager
                val intent = pm.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    startActivity(intent)
                }
                Toast.makeText(this, "Home App mode ON", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, SettingsActivity::class.java).apply {
                    putExtra("EXTRA_PICK_KIOSK", true)
                })
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (PreferencesManager.volumeNav) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_PAGE_UP -> {
                        prevPage()
                        return true
                    }
                    KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_PAGE_DOWN -> {
                        nextPage()
                        return true
                    }
                }
            } else if (event.action == KeyEvent.ACTION_UP) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_PAGE_UP,
                    KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_PAGE_DOWN -> {
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun dpToPx(dp: Int): Float {
        return dp * resources.displayMetrics.density
    }
}
