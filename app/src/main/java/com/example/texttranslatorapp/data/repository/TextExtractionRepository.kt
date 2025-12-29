package com.example.texttranslatorapp.data.repository

import com.example.texttranslatorapp.data.datasource.MLKitTextExtractor
import android.graphics.Bitmap

class TextExtractionRepository(
    private val extractor: MLKitTextExtractor
) {
    suspend fun extractText(bitmap: Bitmap): String {
        return extractor.extractText(bitmap)
    }
}