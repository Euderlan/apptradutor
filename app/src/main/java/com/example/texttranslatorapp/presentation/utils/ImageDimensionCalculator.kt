package com.example.texttranslatorapp.presentation.utils

import android.content.Context
import android.graphics.Bitmap
import androidx.window.layout.WindowMetricsCalculator

/**
 * Responsável por calcular dimensões ideais para exibição de imagens
 * na tela, respeitando proporção original e limites do dispositivo.
 */
class ImageDimensionCalculator(private val context: Context) {

    /**
     * Representa as dimensões finais de exibição da imagem,
     * bem como suas dimensões originais.
     */
    data class ImageDimensions(
        val displayWidth: Int,
        val displayHeight: Int,
        val originalWidth: Int,
        val originalHeight: Int
    )

    /**
     * Calcula as dimensões ideais para exibir um Bitmap na tela.
     *
     * A imagem é redimensionada mantendo o aspect ratio e respeitando
     * margens e espaços reservados da interface.
     */
    fun calculateOptimalDimensions(bitmap: Bitmap): ImageDimensions {

        // Dimensões atuais da tela
        val screenWidth = getScreenWidth()
        val screenHeight = getScreenHeight()

        // Dimensões originais do bitmap
        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height

        // Proporção largura/altura da imagem
        val bitmapRatio = bitmapWidth.toFloat() / bitmapHeight

        // Espaços reservados para margens e outros componentes da UI
        val maxWidth = screenWidth - dpToPx(32)
        val maxHeight = (screenHeight - dpToPx(200)).coerceAtLeast(200)

        val displayWidth: Int
        val displayHeight: Int

        // Decide se a limitação principal será pela largura ou pela altura,
        // garantindo que a imagem não seja distorcida
        if (bitmapRatio > (maxWidth.toFloat() / maxHeight)) {
            displayWidth = maxWidth
            displayHeight = (displayWidth / bitmapRatio).toInt()
        } else {
            displayHeight = maxHeight
            displayWidth = (displayHeight * bitmapRatio).toInt()
        }

        // Retorna dimensões finais com limites mínimos para evitar imagens muito pequenas
        return ImageDimensions(
            displayWidth = displayWidth.coerceAtLeast(200),
            displayHeight = displayHeight.coerceAtLeast(200),
            originalWidth = bitmapWidth,
            originalHeight = bitmapHeight
        )
    }

    /**
     * Obtém a largura da janela atual do dispositivo.
     *
     * Usa WindowMetrics para compatibilidade com diferentes modos de tela
     * (split screen, multi-window, etc).
     */
    private fun getScreenWidth(): Int {
        return try {
            val windowMetrics = WindowMetricsCalculator.getOrCreate()
                .computeCurrentWindowMetrics(
                    context as? android.app.Activity ?: return 1080
                )
            windowMetrics.bounds.width()
        } catch (e: Exception) {
            // Fallback seguro para largura comum de dispositivos modernos
            1080
        }
    }

    /**
     * Obtém a altura da janela atual do dispositivo.
     *
     * Considera apenas a área útil retornada pelo sistema.
     */
    private fun getScreenHeight(): Int {
        return try {
            val windowMetrics = WindowMetricsCalculator.getOrCreate()
                .computeCurrentWindowMetrics(
                    context as? android.app.Activity ?: return 2340
                )
            windowMetrics.bounds.height()
        } catch (e: Exception) {
            // Fallback seguro para altura comum de dispositivos modernos
            2340
        }
    }

    /**
     * Converte valores em dp para pixels com base na densidade da tela.
     */
    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
