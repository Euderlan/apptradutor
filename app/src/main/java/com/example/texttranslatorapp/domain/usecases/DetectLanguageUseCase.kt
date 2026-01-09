package com.example.texttranslatorapp.domain.usecases

import com.example.texttranslatorapp.data.repository.LanguageDetectionRepository
import com.example.texttranslatorapp.domain.models.DetectionResult

// Caso de uso responsável pela detecção de idioma
class DetectLanguageUseCase(
    private val repository: LanguageDetectionRepository
) {

    // Executa a detecção de idioma a partir de um texto
    suspend operator fun invoke(text: String): DetectionResult {
        return repository.detectLanguage(text)
    }
}
