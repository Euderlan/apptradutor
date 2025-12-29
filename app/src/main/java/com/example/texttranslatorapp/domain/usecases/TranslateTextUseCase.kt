package com.example.texttranslatorapp.domain.usecases

import com.example.texttranslatorapp.data.repository.TranslationRepository
import com.example.texttranslatorapp.domain.models.TranslationResult

class TranslateTextUseCase(
    private val repository: TranslationRepository
) {
    suspend operator fun invoke(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): TranslationResult {
        return repository.translateText(text, sourceLanguage, targetLanguage)
    }
}