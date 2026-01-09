package com.example.texttranslatorapp.domain.usecases

import com.example.texttranslatorapp.data.repository.TranslationRepository
import com.example.texttranslatorapp.domain.models.TranslationResult

// Caso de uso responsável pela tradução de texto
class TranslateTextUseCase(
    private val repository: TranslationRepository
) {

    // Executa a tradução de um texto entre dois idiomas
    suspend operator fun invoke(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): TranslationResult {
        return repository.translateText(text, sourceLanguage, targetLanguage)
    }
}
