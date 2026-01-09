package com.example.texttranslatorapp.data.repository

import com.example.texttranslatorapp.data.datasource.TranslationApiService
import com.example.texttranslatorapp.domain.models.TranslationResult

// Repositório responsável por intermediar o acesso ao serviço de tradução
class TranslationRepository(
    private val apiService: TranslationApiService
) {

    // Executa a tradução delegando a chamada para a API externa
    suspend fun translateText(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): TranslationResult {
        return apiService.translate(text, sourceLanguage, targetLanguage)
    }
}
