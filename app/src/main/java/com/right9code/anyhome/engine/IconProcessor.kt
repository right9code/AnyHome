package com.right9code.anyhome.engine

import android.content.Context
import android.graphics.*
import android.text.TextPaint
import com.right9code.anyhome.data.PreferencesManager

object IconProcessor {

    enum class IconMode(val value: Int) {
        TEXT_ONLY(0),
        MONOGRAM(1),
        SYSTEM_DEFAULT(2)
    }

    fun processIcon(context: Context, appInfo: android.content.pm.ApplicationInfo, mode: Int): android.graphics.drawable.Drawable? {
        return when (mode) {
            IconMode.TEXT_ONLY.value -> null
            IconMode.MONOGRAM.value -> generateMonogram(context, appInfo)
            IconMode.SYSTEM_DEFAULT.value -> {
                val d = appInfo.loadIcon(context.packageManager)
                if (d == null) {
                    generateMonogram(context, appInfo)
                } else {
                    d
                }
            }
            else -> {
                val d = appInfo.loadIcon(context.packageManager)
                if (d == null) {
                    generateMonogram(context, appInfo)
                } else {
                    d
                }
            }
        }
    }

    private fun generateMonogram(context: Context, appInfo: android.content.pm.ApplicationInfo): android.graphics.drawable.Drawable {
        val label = appInfo.loadLabel(context.packageManager).toString().trim()
        val words = label.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        val display = when {
            words.size >= 2 -> {
                val first = words.first().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar() ?: "?"
                val second = words[1].firstOrNull { it.isLetterOrDigit() }?.lowercaseChar() ?: ""
                first.toString() + second
            }
            else -> {
                val letters = label.filter { it.isLetterOrDigit() }.take(2)
                letters.ifEmpty { "?" }
            }
        }
        val sizePx = (48 * context.resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = sizePx * 0.06f
        }
        val centerX = sizePx / 2f
        val centerY = sizePx / 2f
        val radius = sizePx * 0.4f
        canvas.drawCircle(centerX, centerY, radius, circlePaint)

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = sizePx * 0.35f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            style = Paint.Style.FILL
        }

        val x = sizePx / 2f
        val y = sizePx / 2f - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(display, x, y, textPaint)

        return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
    }
}
