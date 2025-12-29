package com.example.texttranslatorapp.domain.usecases

import com.example.texttranslatorapp.data.repository.LanguageDetectionRepository
import com.example.texttranslatorapp.domain.models.DetectionResult

class DetectLanguageUseCase(
    private val repository: LanguageDetectionRepository
) {
    suspend operator fun invoke(text: String): DetectionResult {
        return repository.detectLanguage(text)
    }
}