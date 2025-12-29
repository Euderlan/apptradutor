package com.example.texttranslatorapp.data.repository

import com.example.texttranslatorapp.domain.models.DetectionResult
import com.example.texttranslatorapp.data.datasource.MLKitLanguageDetector

class LanguageDetectionRepository(
    private val detector: MLKitLanguageDetector
) {
    suspend fun detectLanguage(text: String): DetectionResult {
        return detector.detect(text)
    }
}