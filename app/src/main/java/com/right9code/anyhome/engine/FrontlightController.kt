package com.right9code.anyhome.engine

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import java.util.concurrent.Executors

object FrontlightController {
    private const val TAG = "FrontlightController"
    private const val DEV_DIR = "/sys/bus/i2c/devices/2-0036"
    private const val COLD_PATH = "$DEV_DIR/lm3630a_cold_light"
    private const val WARM_PATH = "$DEV_DIR/lm3630a_warm_light"
    const val MAX_VALUE = 255

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    var currentCold: Int = 0
        private set

    @Volatile
    var currentWarm: Int = 0
        private set

    private var directIoAvailable = false
    private var syncScheduled = false

    fun init(context: Context) {
        executor.execute {
            try {
                directIoAvailable = testDirectIo()
                if (!directIoAvailable) {
                    // Try to grant permissions for zero-latency direct writes
                    Runtime.getRuntime().exec(arrayOf("su", "-c", "chmod 666 $COLD_PATH $WARM_PATH 2>/dev/null")).waitFor()
                    directIoAvailable = testDirectIo()
                }
                currentCold = readSysfs(COLD_PATH) ?: readSystemSetting(context, "ColdValue") ?: 0
                currentWarm = readSysfs(WARM_PATH) ?: readSystemSetting(context, "WarmValue") ?: 0
                Log.d(TAG, "Initialized cold=$currentCold warm=$currentWarm directIo=$directIoAvailable")
            } catch (e: Exception) {
                Log.e(TAG, "Init error", e)
            }
        }
    }

    private fun testDirectIo(): Boolean {
        return try {
            val fCold = File(COLD_PATH)
            val fWarm = File(WARM_PATH)
            fCold.exists() && fCold.canWrite() && fWarm.exists() && fWarm.canWrite()
        } catch (e: Exception) {
            false
        }
    }

    private fun readSysfs(path: String): Int? {
        return try {
            val file = File(path)
            if (file.exists() && file.canRead()) {
                FileInputStream(file).bufferedReader().use { reader ->
                    reader.readLine()?.trim()?.toIntOrNull()
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun readSystemSetting(context: Context, key: String): Int? {
        return try {
            Settings.System.getInt(context.contentResolver, key)
        } catch (e: Exception) {
            null
        }
    }

    fun adjustCold(context: Context, delta: Int): Int {
        val newVal = (currentCold + delta).coerceIn(0, MAX_VALUE)
        setCold(context, newVal)
        return newVal
    }

    fun adjustWarm(context: Context, delta: Int): Int {
        val newVal = (currentWarm + delta).coerceIn(0, MAX_VALUE)
        setWarm(context, newVal)
        return newVal
    }

    fun setCold(context: Context, value: Int) {
        currentCold = value.coerceIn(0, MAX_VALUE)
        applyHardware(COLD_PATH, currentCold)
        scheduleSystemSync(context)
    }

    fun setWarm(context: Context, value: Int) {
        currentWarm = value.coerceIn(0, MAX_VALUE)
        applyHardware(WARM_PATH, currentWarm)
        scheduleSystemSync(context)
    }

    fun setBoth(context: Context, cold: Int, warm: Int) {
        currentCold = cold.coerceIn(0, MAX_VALUE)
        currentWarm = warm.coerceIn(0, MAX_VALUE)
        applyHardware(COLD_PATH, currentCold)
        applyHardware(WARM_PATH, currentWarm)
        scheduleSystemSync(context)
    }

    fun toggleLight(context: Context): Boolean {
        val hwCold = readSysfs(COLD_PATH) ?: currentCold
        val hwWarm = readSysfs(WARM_PATH) ?: currentWarm
        return if (hwCold > 0 || hwWarm > 0) {
            setBoth(context, 0, 0)
            false // Light is now OFF
        } else {
            val sysCold = readSystemSetting(context, "ColdValue")
                ?: readSystemSetting(context, "LastColdLight")
                ?: 0
            val sysWarm = readSystemSetting(context, "WarmValue")
                ?: readSystemSetting(context, "LastWarmLight")
                ?: 0
            val c = if (sysCold > 0 || sysWarm > 0) sysCold else 0
            val w = if (sysCold > 0 || sysWarm > 0) sysWarm else 40
            setBoth(context, c, w)
            true // Light is now ON
        }
    }

    fun restoreLightIfNeeded(context: Context) {
        executor.execute {
            try {
                val hwCold = readSysfs(COLD_PATH) ?: 0
                val hwWarm = readSysfs(WARM_PATH) ?: 0
                val sysCold = readSystemSetting(context, "ColdValue") ?: 0
                val sysWarm = readSystemSetting(context, "WarmValue") ?: 0

                // If hardware is off but system has active levels, restore them
                if (hwCold == 0 && hwWarm == 0 && (sysCold > 0 || sysWarm > 0)) {
                    setBoth(context, sysCold, sysWarm)
                    Log.d(TAG, "Restored frontlight from system settings cold=$sysCold warm=$sysWarm")
                } else {
                    currentCold = hwCold
                    currentWarm = hwWarm
                }
            } catch (e: Exception) {
                Log.e(TAG, "restoreLightIfNeeded failed", e)
            }
        }
    }

    private fun applyHardware(path: String, value: Int) {
        if (directIoAvailable) {
            try {
                FileOutputStream(File(path)).use { os ->
                    os.write(value.toString().toByteArray())
                    os.flush()
                }
                return
            } catch (e: Exception) {
                directIoAvailable = false
            }
        }
        // Fallback to async root write
        executor.execute {
            try {
                val cmd = "echo $value > $path 2>/dev/null"
                Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            } catch (e: Exception) {
                Log.e(TAG, "Failed fallback write to $path", e)
            }
        }
    }

    private fun scheduleSystemSync(context: Context) {
        if (syncScheduled) return
        syncScheduled = true
        mainHandler.postDelayed({
            syncScheduled = false
            val c = currentCold
            val w = currentWarm
            executor.execute {
                try {
                    val syncCmd = "settings put system ColdValue $c 2>/dev/null; " +
                            "settings put system LastColdLight $c 2>/dev/null; " +
                            "settings put system screen_brightness_cold $c 2>/dev/null; " +
                            "settings put system WarmValue $w 2>/dev/null; " +
                            "settings put system LastWarmLight $w 2>/dev/null; " +
                            "settings put system screen_brightness_warm $w 2>/dev/null"
                    Runtime.getRuntime().exec(arrayOf("su", "-c", syncCmd))
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync system light settings", e)
                }
            }
        }, 200)
    }
}
