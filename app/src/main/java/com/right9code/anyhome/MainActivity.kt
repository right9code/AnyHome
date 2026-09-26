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
import com.right9code.anyhome.data.QuotesManager
import com.right9code.anyhome.engine.FrontlightController
import com.right9code.anyhome.engine.IconProcessor
import com.right9code.anyhome.engine.Page
import com.right9code.anyhome.engine.PagedAppViewManager
import com.right9code.anyhome.ui.SettingsActivity
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

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

    // Lock Screen Views
    private lateinit var lockOverlay: FrameLayout
    private lateinit var lockContent: LinearLayout
    private lateinit var lockTime: TextView
    private lateinit var lockDate: TextView
    private lateinit var lockBattery: TextView
    private lateinit var lockQuoteContainer: LinearLayout
    private lateinit var lockQuoteText: TextView
    private lateinit var lockOwnerContainer: LinearLayout
    private lateinit var lockOwnerName: TextView
    private lateinit var lockOwnerContacts: TextView
    private lateinit var lockOwnerAddress: TextView
    private lateinit var lockHint: TextView

    // PIN Keypad Views
    private lateinit var pinKeypadContainer: LinearLayout
    private lateinit var pinTitle: TextView
    private lateinit var pinDots: TextView
    private lateinit var pinError: TextView
    private lateinit var btnPinBack: Button
    private val enteredPin = StringBuilder()
    private var currentLockQuote: String = ""

    private var isLocked: Boolean = false
    private lateinit var lockGestureDetector: GestureDetector

    private var allApps: List<ResolveInfo> = emptyList()
    private var pages: List<Page> = emptyList()
    private var currentPage = 1
    private var lastKioskLaunch = 0L
    private var consecutiveQuickReturns = 0
    private var lastHomeTapTime = 0L
    private var homeTapCount = 0
    private val handler = Handler(Looper.getMainLooper())
    private val pinnedApps = mutableSetOf<String>()
    private var hudToast: Toast? = null

    private lateinit var gestureDetector: GestureDetector
    private lateinit var scaleDetector: ScaleGestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferencesManager.init(this)
        PreferencesManager.resetIconModeIfInvalid()
        FrontlightController.init(this)
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

        // Bind Lock Screen & Keypad
        lockOverlay = findViewById(R.id.lock_overlay)
        lockContent = findViewById(R.id.lock_content)
        lockTime = findViewById(R.id.lock_time)
        lockDate = findViewById(R.id.lock_date)
        lockBattery = findViewById(R.id.lock_battery)
        lockQuoteContainer = findViewById(R.id.lock_quote_container)
        lockQuoteText = findViewById(R.id.lock_quote_text)
        lockOwnerContainer = findViewById(R.id.lock_owner_container)
        lockOwnerName = findViewById(R.id.lock_owner_name)
        lockOwnerContacts = findViewById(R.id.lock_owner_contacts)
        lockOwnerAddress = findViewById(R.id.lock_owner_address)
        lockHint = findViewById(R.id.lock_hint)

        pinKeypadContainer = findViewById(R.id.pin_keypad_container)
        pinTitle = findViewById(R.id.pin_title)
        pinDots = findViewById(R.id.pin_dots)
        pinError = findViewById(R.id.pin_error)
        btnPinBack = findViewById(R.id.btn_pin_back)

        setupFullScreen()

        loadApps()
        setupHeader()
        setupGestures()
        setupControls()
        setupLockKeypad()
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
            addAction(android.content.Intent.ACTION_SCREEN_OFF)
            addAction(android.content.Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(headerReceiver, filter)

        settingsBtn.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        settingsBtn.setOnLongClickListener {
            executeAction(PreferencesManager.actionSettingsLong, PreferencesManager.actionSettingsLongPkg)
            true
        }

        time.setOnClickListener { triggerEInkRefresh() }
        time.setOnLongClickListener {
            executeAction(PreferencesManager.actionTimeLong, PreferencesManager.actionTimeLongPkg)
            true
        }
        date.setOnClickListener { triggerEInkRefresh() }
        date.setOnLongClickListener {
            executeAction(PreferencesManager.actionTimeLong, PreferencesManager.actionTimeLongPkg)
            true
        }
    }

    private fun updateTimeAndDate() {
        val now = java.util.Calendar.getInstance()
        val timeFormat = android.text.format.DateFormat.getTimeFormat(this)
        val dateFormat = android.text.format.DateFormat.getMediumDateFormat(this)
        time.text = timeFormat.format(now.time)
        date.text = dateFormat.format(now.time)
        if (isLocked) {
            updateLockScreenWidgets()
        }
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
        if (isLocked) {
            updateLockScreenWidgets()
        }
    }

    private fun updateLockScreenWidgets() {
        val now = java.util.Calendar.getInstance()
        val timeFormat = android.text.format.DateFormat.getTimeFormat(this)
        val dayFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        lockTime.text = timeFormat.format(now.time)
        lockDate.text = dayFormat.format(now.time)

        val intent = registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 0
        val charging = plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS
        lockBattery.text = if (charging) "[ $batteryPct% • CHARGING ]" else "[ $batteryPct% • BATTERY ]"

        // Quote: only select a new quote if one hasn't been chosen for this lock session
        if (currentLockQuote.isEmpty()) {
            currentLockQuote = QuotesManager.getNextQuote(this)
        }
        lockQuoteText.text = currentLockQuote

        // Owner details
        val name = PreferencesManager.ownerName ?: ""
        val phone = PreferencesManager.ownerPhone ?: ""
        val email = PreferencesManager.ownerEmail ?: ""
        val addr = PreferencesManager.ownerAddress ?: ""

        if (name.isNotEmpty()) {
            lockOwnerName.visibility = View.VISIBLE
            lockOwnerName.text = "👤 Owner: $name"
        } else {
            lockOwnerName.visibility = View.GONE
        }

        val contacts = mutableListOf<String>()
        if (phone.isNotEmpty()) contacts.add("📞 $phone")
        if (email.isNotEmpty()) contacts.add("✉️ $email")
        if (contacts.isNotEmpty()) {
            lockOwnerContacts.visibility = View.VISIBLE
            lockOwnerContacts.text = contacts.joinToString("  •  ")
        } else {
            lockOwnerContacts.visibility = View.GONE
        }

        if (addr.isNotEmpty()) {
            lockOwnerAddress.visibility = View.VISIBLE
            lockOwnerAddress.text = "📍 $addr"
        } else {
            lockOwnerAddress.visibility = View.GONE
        }

        // Display Mode: 0=Full, 1=Quote Only, 2=Owner Only, 3=Clock Only
        val mode = PreferencesManager.lockDisplayMode
        val hasOwner = name.isNotEmpty() || phone.isNotEmpty() || email.isNotEmpty() || addr.isNotEmpty()
        lockQuoteContainer.visibility = if (mode == 0 || mode == 1) View.VISIBLE else View.GONE
        lockOwnerContainer.visibility = if ((mode == 0 || mode == 2) && hasOwner) View.VISIBLE else View.GONE

        val hasPin = !PreferencesManager.lockPin.isNullOrEmpty()
        lockHint.text = if (hasPin) "▲ Swipe up or tap to enter PIN" else "▲ Swipe up or tap to unlock"
    }

    private fun setupLockKeypad() {
        val pinBtns = intArrayOf(
            R.id.btn_pin_0, R.id.btn_pin_1, R.id.btn_pin_2, R.id.btn_pin_3,
            R.id.btn_pin_4, R.id.btn_pin_5, R.id.btn_pin_6, R.id.btn_pin_7,
            R.id.btn_pin_8, R.id.btn_pin_9
        )
        for (i in 0..9) {
            findViewById<Button>(pinBtns[i]).setOnClickListener {
                appendPinDigit(i.toString())
            }
        }
        findViewById<Button>(R.id.btn_pin_delete).setOnClickListener {
            if (enteredPin.isNotEmpty()) {
                enteredPin.deleteCharAt(enteredPin.length - 1)
                updatePinDots()
            }
        }
        findViewById<Button>(R.id.btn_pin_enter).setOnClickListener {
            checkPinAndUnlock()
        }
        btnPinBack.setOnClickListener {
            pinKeypadContainer.visibility = View.GONE
            lockContent.visibility = View.VISIBLE
            enteredPin.clear()
            updatePinDots()
        }
    }

    private fun appendPinDigit(d: String) {
        val targetPin = PreferencesManager.lockPin ?: ""
        val maxLen = if (targetPin.isNotEmpty()) targetPin.length else 4
        if (enteredPin.length < maxLen) {
            enteredPin.append(d)
            updatePinDots()
            if (enteredPin.length == maxLen) {
                handler.postDelayed({ checkPinAndUnlock() }, 100)
            }
        }
    }

    private fun updatePinDots() {
        val targetPin = PreferencesManager.lockPin ?: ""
        val maxLen = if (targetPin.isNotEmpty()) targetPin.length else 4
        val sb = StringBuilder()
        for (i in 0 until maxLen) {
            if (i > 0) sb.append("  ")
            sb.append(if (i < enteredPin.length) "●" else "○")
        }
        pinDots.text = sb.toString()
        pinError.visibility = View.INVISIBLE
    }

    private fun checkPinAndUnlock() {
        val targetPin = PreferencesManager.lockPin
        if (targetPin.isNullOrEmpty() || enteredPin.toString() == targetPin) {
            unlockScreen()
        } else {
            pinError.visibility = View.VISIBLE
            val targetLen = if (!targetPin.isNullOrEmpty()) targetPin.length else 4
            val sb = StringBuilder()
            for (i in 0 until targetLen) {
                if (i > 0) sb.append("  ")
                sb.append("●")
            }
            pinDots.text = sb.toString()
            handler.postDelayed({
                enteredPin.clear()
                updatePinDots()
            }, 600)
        }
    }

    private fun requestUnlock() {
        val targetPin = PreferencesManager.lockPin
        if (!targetPin.isNullOrEmpty()) {
            lockContent.visibility = View.GONE
            pinKeypadContainer.visibility = View.VISIBLE
            enteredPin.clear()
            updatePinDots()
        } else {
            unlockScreen()
        }
    }

    fun lockScreen(forceNewQuote: Boolean = false) {
        if (!PreferencesManager.enableLockscreen) return
        isLocked = true
        if (forceNewQuote || currentLockQuote.isEmpty()) {
            currentLockQuote = QuotesManager.getNextQuote(this)
        }
        pinKeypadContainer.visibility = View.GONE
        lockContent.visibility = View.VISIBLE
        enteredPin.clear()
        updateLockScreenWidgets()
        lockOverlay.visibility = View.VISIBLE
    }

    fun unlockScreen() {
        isLocked = false
        currentLockQuote = ""
        enteredPin.clear()
        pinKeypadContainer.visibility = View.GONE
        lockContent.visibility = View.VISIBLE
        lockOverlay.visibility = View.GONE
        triggerEInkRefresh()
    }

    private val headerReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            when (intent?.action) {
                android.content.Intent.ACTION_TIME_TICK,
                android.content.Intent.ACTION_TIME_CHANGED,
                android.content.Intent.ACTION_DATE_CHANGED -> updateTimeAndDate()
                android.content.Intent.ACTION_BATTERY_CHANGED -> updateBattery()
                android.content.Intent.ACTION_SCREEN_OFF -> {
                    if (PreferencesManager.enableLockscreen) {
                        lockScreen(forceNewQuote = true)
                    }
                }
                android.content.Intent.ACTION_SCREEN_ON -> {
                    if (PreferencesManager.enableLockscreen) {
                        if (!isLocked) {
                            lockScreen(forceNewQuote = true)
                        } else {
                            updateLockScreenWidgets()
                        }
                    }
                }
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
                val startX = e1?.x ?: 0f
                val startY = e1?.y ?: 0f
                val dx = e2.x - startX
                val dy = e2.y - startY
                val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                if (dist < dpToPx(35)) return false

                val screenWidth = resources.displayMetrics.widthPixels.toFloat()
                val screenHeight = resources.displayMetrics.heightPixels.toFloat()
                val edgeMargin = dpToPx(50)
                val topSafeZone = dpToPx(65)

                if (abs(dy) > abs(dx) * 1.1f) {
                    // If swipe originated from the very top bezel (Y < 40dp) going downwards, let the system show transient status bar peek
                    if (startY < dpToPx(40) && dy > 0) {
                        return false
                    }

                    // Left edge swipe (Cold Light ❄️)
                    if (startX <= edgeMargin && startY >= topSafeZone) {
                        val ratio = (abs(dy) / screenHeight).coerceIn(0.04f, 1.0f)
                        val step = (255 * (ratio.toDouble().pow(1.3))).roundToInt().coerceIn(2, 255)
                        val delta = if (dy < 0) step else -step
                        val newVal = FrontlightController.adjustCold(this@MainActivity, delta)
                        val pct = Math.round((newVal / 255.0f) * 100)
                        val deltaStr = if (delta > 0) "+$delta" else "$delta"
                        showLightHud("❄️ Cool: $newVal/255 ($pct%) [Δ$deltaStr]")
                        return true
                    }

                    // Right edge swipe (Warm Light 🔥)
                    if (startX >= screenWidth - edgeMargin && startY >= topSafeZone) {
                        val ratio = (abs(dy) / screenHeight).coerceIn(0.04f, 1.0f)
                        val step = (255 * (ratio.toDouble().pow(1.3))).roundToInt().coerceIn(2, 255)
                        val delta = if (dy < 0) step else -step
                        val newVal = FrontlightController.adjustWarm(this@MainActivity, delta)
                        val pct = Math.round((newVal / 255.0f) * 100)
                        val deltaStr = if (delta > 0) "+$delta" else "$delta"
                        showLightHud("🔥 Warm: $newVal/255 ($pct%) [Δ$deltaStr]")
                        return true
                    }

                    // Center swipes or swipes starting from middle
                    if (dy < 0) swipeUp() else swipeDown()
                    return true
                }

                if (abs(dx) > abs(dy) * 1.4f) {
                    if (dx < 0) nextPage() else prevPage()
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

        lockGestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (pinKeypadContainer.visibility != View.VISIBLE) {
                    requestUnlock()
                    return true
                }
                return false
            }
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (pinKeypadContainer.visibility != View.VISIBLE) {
                    requestUnlock()
                    return true
                }
                return false
            }
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                val dy = e2.y - (e1?.y ?: 0f)
                if (dy < -dpToPx(30)) {
                    if (pinKeypadContainer.visibility != View.VISIBLE) {
                        requestUnlock()
                        return true
                    }
                }
                return false
            }
        })

    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (isLocked) {
            if (pinKeypadContainer.visibility == View.VISIBLE) {
                return super.dispatchTouchEvent(event)
            }
            lockGestureDetector.onTouchEvent(event)
            return true
        }

        // Handle triple-tap detection directly in navBar for page label
        if (navBar.visibility == View.VISIBLE && event.action == MotionEvent.ACTION_UP) {
            val pageLoc = IntArray(2)
            pageLabel.getLocationOnScreen(pageLoc)
            if (event.rawX >= pageLoc[0] && event.rawX <= pageLoc[0] + pageLabel.width &&
                event.rawY >= pageLoc[1] && event.rawY <= pageLoc[1] + pageLabel.height) {
                val now = SystemClock.elapsedRealtime()
                if (now - lastPageTapTime < 650) {
                    pageTapCount++
                } else {
                    pageTapCount = 1
                }
                lastPageTapTime = now
                if (pageTapCount >= 3) {
                    pageTapCount = 0
                    executeAction(PreferencesManager.actionPageTriple, PreferencesManager.actionPageTriplePkg)
                    return true
                }
            }
        }

        // Exclude header and navBar from GestureDetector interception so buttons get direct click/long-click events
        val navLoc = IntArray(2)
        navBar.getLocationOnScreen(navLoc)
        val inNavBar = (navBar.visibility == View.VISIBLE && event.rawY >= navLoc[1])

        val headerLoc = IntArray(2)
        header.getLocationOnScreen(headerLoc)
        val inHeader = (header.visibility == View.VISIBLE && event.rawY <= headerLoc[1] + header.height)

        if (inNavBar || inHeader) {
            return super.dispatchTouchEvent(event)
        }

        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return super.dispatchTouchEvent(event)
    }

    private fun setupFullScreen() {
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

    private fun executeAction(actionId: Int, appPkg: String? = null) {
        when (actionId) {
            PreferencesManager.ACTION_TOGGLE_FRONTLIGHT -> {
                val isOn = FrontlightController.toggleLight(this)
                if (isOn) {
                    showLightHud("💡 Frontlight: ON (${FrontlightController.currentCold}/${FrontlightController.currentWarm})")
                } else {
                    showLightHud("🌑 Frontlight: OFF")
                }
            }
            PreferencesManager.ACTION_NOTIFICATION_SHADE -> {
                openNotifications()
            }
            PreferencesManager.ACTION_CONTROL_CENTER -> {
                openQuickSettings()
            }
            PreferencesManager.ACTION_SYSTEM_SETTINGS -> {
                try {
                    startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                } catch (e: Exception) {
                    Log.e("AnyHome", "Failed to open system settings", e)
                }
            }
            PreferencesManager.ACTION_LAUNCH_APP -> {
                if (!appPkg.isNullOrEmpty()) {
                    try {
                        val launchIntent = packageManager.getLaunchIntentForPackage(appPkg)
                        if (launchIntent != null) {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                            startActivity(launchIntent)
                        } else {
                            Toast.makeText(this, "App not found", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("AnyHome", "Failed to launch $appPkg", e)
                    }
                }
            }
            PreferencesManager.ACTION_REFRESH_SCREEN -> {
                triggerEInkRefresh()
            }
            PreferencesManager.ACTION_LAUNCHER_SETTINGS -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            PreferencesManager.ACTION_LOCK_SCREEN -> {
                performLock()
            }
        }
    }

    private var pageTapCount = 0
    private var lastPageTapTime = 0L

    private fun setupControls() {
        prevBtn.setOnClickListener { prevPage() }
        prevBtn.setOnLongClickListener {
            executeAction(PreferencesManager.actionPrevLong, PreferencesManager.actionPrevLongPkg)
            true
        }

        nextBtn.setOnClickListener { nextPage() }
        nextBtn.setOnLongClickListener {
            executeAction(PreferencesManager.actionNextLong, PreferencesManager.actionNextLongPkg)
            true
        }

        pageLabel.setOnClickListener {
            val now = SystemClock.elapsedRealtime()
            if (now - lastPageTapTime < 650) {
                pageTapCount++
            } else {
                pageTapCount = 1
            }
            lastPageTapTime = now
            Log.d("AnyHome", "pageLabel tapped count=$pageTapCount")

            if (pageTapCount >= 3) {
                pageTapCount = 0
                executeAction(PreferencesManager.actionPageTriple, PreferencesManager.actionPageTriplePkg)
            }
        }

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
        openNotifications()
    }

    private fun openNotifications() {
        Log.d("AnyHome", "openNotifications invoked")
        try {
            val statusBarService = getSystemService("statusbar") ?: getSystemService(STATUS_BAR_SERVICE)
            val method = statusBarService.javaClass.getMethod("expandNotificationsPanel")
            method.invoke(statusBarService)
        } catch (e: Exception) {
            Log.e("AnyHome", "expandNotificationsPanel failed, fallback to root", e)
            Executors.newSingleThreadExecutor().execute {
                try {
                    Runtime.getRuntime().exec(arrayOf("su", "-c", "cmd statusbar expand-notifications 2>/dev/null || input swipe 250 5 250 700 200 2>/dev/null")).waitFor()
                } catch (e2: Exception) {
                    Log.e("AnyHome", "openNotifications root shell failed", e2)
                }
            }
        }
    }

    private fun openQuickSettings() {
        Log.d("AnyHome", "openQuickSettings invoked")
        try {
            val res = contentResolver.call(
                Uri.parse("content://com.xrz.sys.control.provider"),
                "showMenuControl",
                null,
                null
            )
            if (res != null) return
        } catch (e: Exception) {
            Log.d("AnyHome", "content call showMenuControl failed", e)
        }

        try {
            val res = contentResolver.call(
                Uri.parse("content://com.xrz.SettingProvider"),
                "setting_einkcenter",
                null,
                null
            )
            if (res != null) return
        } catch (e: Exception) {
            Log.d("AnyHome", "content call setting_einkcenter failed", e)
        }

        Executors.newSingleThreadExecutor().execute {
            try {
                Runtime.getRuntime().exec(arrayOf("su", "-c", "content call --uri content://com.xrz.sys.control.provider --method showMenuControl 2>/dev/null || cmd statusbar expand-settings 2>/dev/null")).waitFor()
            } catch (e2: Exception) {
                Log.e("AnyHome", "openQuickSettings root shell failed", e2)
            }
        }
    }

    private fun showLightHud(msg: String) {
        hudToast?.cancel()
        hudToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT).apply {
            show()
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
        if (PreferencesManager.enableLockscreen) {
            lockScreen(forceNewQuote = true)
            handler.postDelayed({
                Executors.newSingleThreadExecutor().execute {
                    try {
                        Runtime.getRuntime().exec(arrayOf("su", "-c", "input keyevent 26 2>/dev/null")).waitFor()
                    } catch (e: Exception) {
                        Log.e("AnyHome", "performLock failed", e)
                    }
                }
            }, 150)
        } else {
            Executors.newSingleThreadExecutor().execute {
                try {
                    Runtime.getRuntime().exec(arrayOf("su", "-c", "input keyevent 26 2>/dev/null")).waitFor()
                } catch (e: Exception) {
                    Log.e("AnyHome", "performLock failed", e)
                }
            }
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
        FrontlightController.restoreLightIfNeeded(this)
        if (PreferencesManager.enableLockscreen && isLocked) {
            lockScreen(forceNewQuote = false)
        }
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
