package com.example.texttranslatorapp.presentation.utils

import android.graphics.Bitmap
import android.graphics.Rect
import com.example.texttranslatorapp.data.datasource.MLKitTextExtractorMultilingual
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TextExtractionHelper(
    private val textExtractor: MLKitTextExtractorMultilingual
) {

    /**
     * Extrai texto de uma área específica da imagem
     * @param imagemOriginal Bitmap da imagem completa
     * @param selectionRect Retângulo da área selecionada
     * @return Texto extraído da área selecionada
     */
    suspend fun extrairTextoDaSelecao(
        imagemOriginal: Bitmap,
        selectionRect: Rect
    ): String {
        return withContext(Dispatchers.Default) {
            try {
                // Criar bitmap cortado
                val imagemCortada = Bitmap.createBitmap(
                    imagemOriginal,
                    selectionRect.left.coerceAtLeast(0),
                    selectionRect.top.coerceAtLeast(0),
                    selectionRect.width().coerceAtMost(imagemOriginal.width - selectionRect.left),
                    selectionRect.height().coerceAtMost(imagemOriginal.height - selectionRect.top)
                )

                // Extrair texto da área cortada usando novo extrator
                textExtractor.extractTextAuto(imagemCortada)
            } catch (e: Exception) {
                throw Exception("Erro ao extrair texto da seleção: ${e.message}")
            }
        }
    }

    /**
     * Extrai texto da imagem completa
     */
    suspend fun extrairTextoCompleto(imagemOriginal: Bitmap): String {
        return textExtractor.extractTextAuto(imagemOriginal)
    }
}