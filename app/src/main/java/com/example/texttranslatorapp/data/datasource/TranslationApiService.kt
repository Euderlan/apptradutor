package com.example.texttranslatorapp.data.datasource

import com.example.texttranslatorapp.domain.models.TranslationResult

class TranslationApiService {

    suspend fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): TranslationResult {
        // Dicionário simples de tradução
        val dictionary = mapOf(
            "olá" to "hello",
            "oi" to "hi",
            "obrigado" to "thank you",
            "por favor" to "please",
            "sim" to "yes",
            "não" to "no",
            "água" to "water",
            "casa" to "house",
            "carro" to "car",
            "comida" to "food"
        )

        val words = text.lowercase().split(" ")
        val translated = words.map { word ->
            dictionary[word] ?: word
        }.joinToString(" ")

        return TranslationResult(
            originalText = text,
            translatedText = translated,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage
        )
    }
}