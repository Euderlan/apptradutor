package com.example.texttranslatorapp.data.repository

import android.graphics.Bitmap
import com.example.texttranslatorapp.data.datasource.MLKitTextExtractorMultilingual

class TextExtractionRepository(
    private val extractor: MLKitTextExtractorMultilingual
) {
    suspend fun extractText(bitmap: Bitmap): String {
        return extractor.extractTextAuto(bitmap)
    }

    suspend fun extractTextByLanguage(bitmap: Bitmap, languageCode: String): String {
        return extractor.extractTextByLanguage(bitmap, languageCode)
    }
}