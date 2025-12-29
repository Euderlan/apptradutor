package com.example.texttranslatorapp.data.repository

import com.example.texttranslatorapp.data.datasource.TranslationApiService
import com.example.texttranslatorapp.domain.models.TranslationResult

class TranslationRepository(
    private val apiService: TranslationApiService
) {
    suspend fun translateText(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): TranslationResult {
        return apiService.translate(text, sourceLanguage, targetLanguage)
    }
}