package com.example.texttranslatorapp.data.repository

import com.example.texttranslatorapp.domain.models.DetectionResult
import com.example.texttranslatorapp.data.datasource.MLKitLanguageDetector

// Repositório responsável pela detecção de idioma
class LanguageDetectionRepository(
    private val detector: MLKitLanguageDetector
) {

    // Executa a detecção de idioma delegando ao ML Kit
    suspend fun detectLanguage(text: String): DetectionResult {
        return detector.detect(text)
    }
}
