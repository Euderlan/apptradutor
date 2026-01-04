package com.example.texttranslatorapp.presentation.utils

import android.content.Context
import android.graphics.Bitmap
import androidx.window.layout.WindowMetricsCalculator

class ImageDimensionCalculator(private val context: Context) {

    data class ImageDimensions(
        val displayWidth: Int,
        val displayHeight: Int,
        val originalWidth: Int,
        val originalHeight: Int
    )

    fun calculateOptimalDimensions(bitmap: Bitmap): ImageDimensions {
        val screenWidth = getScreenWidth()
        val screenHeight = getScreenHeight()

        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height
        val bitmapRatio = bitmapWidth.toFloat() / bitmapHeight

        val maxWidth = screenWidth - dpToPx(32)
        val maxHeight = (screenHeight - dpToPx(200)).coerceAtLeast(200)

        val displayWidth: Int
        val displayHeight: Int

        if (bitmapRatio > (maxWidth.toFloat() / maxHeight)) {
            displayWidth = maxWidth
            displayHeight = (displayWidth / bitmapRatio).toInt()
        } else {
            displayHeight = maxHeight
            displayWidth = (displayHeight * bitmapRatio).toInt()
        }

        return ImageDimensions(
            displayWidth = displayWidth.coerceAtLeast(200),
            displayHeight = displayHeight.coerceAtLeast(200),
            originalWidth = bitmapWidth,
            originalHeight = bitmapHeight
        )
    }

    private fun getScreenWidth(): Int {
        return try {
            val windowMetrics = WindowMetricsCalculator.getOrCreate()
                .computeCurrentWindowMetrics(context as? android.app.Activity ?: return 1080)
            windowMetrics.bounds.width()
        } catch (e: Exception) {
            1080
        }
    }

    private fun getScreenHeight(): Int {
        return try {
            val windowMetrics = WindowMetricsCalculator.getOrCreate()
                .computeCurrentWindowMetrics(context as? android.app.Activity ?: return 2340)
            windowMetrics.bounds.height()
        } catch (e: Exception) {
            2340
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}