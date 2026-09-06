package com.right9code.anyhome.engine

import android.content.Context
import android.graphics.Typeface
import java.io.File

object FontManager {

    private var cachedTypeface: Typeface? = null
    private const val CACHE_NAME = "custom_font.ttf"

    fun loadCustomFont(context: Context): Typeface? {
        if (cachedTypeface != null) return cachedTypeface
        val file = File(context.filesDir, CACHE_NAME)
        if (!file.exists()) return null
        return try {
            cachedTypeface = Typeface.createFromFile(file)
            cachedTypeface
        } catch (e: Exception) {
            null
        }
    }

    fun applyToTextView(context: Context, textView: android.widget.TextView) {
        val tf = loadCustomFont(context)
        if (tf != null) {
            textView.typeface = tf
        }
    }

    fun streamFontToCache(context: Context, uri: android.net.Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                File(context.filesDir, CACHE_NAME).outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            cachedTypeface = null
            true
        } catch (e: Exception) {
            false
        }
    }

    fun clearCache(context: Context) {
        try {
            File(context.filesDir, CACHE_NAME).delete()
        } catch (e: Exception) {
            // ignore
        }
        cachedTypeface = null
    }
}
