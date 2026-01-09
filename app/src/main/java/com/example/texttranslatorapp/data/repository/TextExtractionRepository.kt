package com.example.texttranslatorapp.data.repository

import android.graphics.Bitmap
import com.example.texttranslatorapp.data.datasource.MLKitTextExtractorMultilingual

// Repositório responsável por centralizar a extração de texto (OCR)
class TextExtractionRepository(
    private val extractor: MLKitTextExtractorMultilingual
) {

    // Realiza OCR automaticamente, deixando o ML Kit identificar o idioma
    suspend fun extractText(bitmap: Bitmap): String {
        return extractor.extractTextAuto(bitmap)
    }

    // Realiza OCR forçando um idioma específico
    suspend fun extractTextByLanguage(bitmap: Bitmap, languageCode: String): String {
        return extractor.extractTextByLanguage(bitmap, languageCode)
    }
}
