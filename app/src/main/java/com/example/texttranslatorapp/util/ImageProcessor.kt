package com.example.texttranslatorapp.util

import android.graphics.Bitmap

class ImageProcessor {

    fun compressBitmap(bitmap: Bitmap, maxWidth: Int = 1200, maxHeight: Int = 1200): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val bitmapRatio = width.toFloat() / height
        val maxRatio = maxWidth.toFloat() / maxHeight

        val finalWidth: Int
        val finalHeight: Int

        when {
            bitmapRatio > maxRatio -> {
                finalWidth = maxWidth
                finalHeight = (maxWidth / bitmapRatio).toInt()
            }
            else -> {
                finalHeight = maxHeight
                finalWidth = (maxHeight * bitmapRatio).toInt()
            }
        }

        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
    }

    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}