package com.example.texttranslatorapp.util

import android.graphics.Bitmap

// Classe utilitária para processamento básico de imagens
class ImageProcessor {

    // Redimensiona o bitmap mantendo a proporção dentro de um tamanho máximo
    fun compressBitmap(bitmap: Bitmap, maxWidth: Int = 4096, maxHeight: Int = 4096): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Retorna o bitmap original se já estiver dentro dos limites
        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        // Calcula a proporção da imagem e do limite máximo
        val bitmapRatio = width.toFloat() / height
        val maxRatio = maxWidth.toFloat() / maxHeight

        val finalWidth: Int
        val finalHeight: Int

        // Ajusta largura e altura mantendo o aspect ratio
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

    // Rotaciona o bitmap em um determinado ângulo
    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
