package com.example.texttranslatorapp.util

import android.graphics.Bitmap

class ImageProcessor {

    fun compressBitmap(bitmap: Bitmap, maxWidth: Int = 4096, maxHeight: Int = 4096): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Se a imagem já está dentro do limite, retorna sem modificar
        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

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