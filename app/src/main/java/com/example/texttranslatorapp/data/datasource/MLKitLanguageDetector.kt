package com.example.texttranslatorapp.data.datasource

import com.example.texttranslatorapp.domain.models.DetectionResult
import com.google.mlkit.nl.languageid.LanguageIdentification
import kotlinx.coroutines.tasks.await

class MLKitLanguageDetector {
    private val languageIdentifier by lazy {
        LanguageIdentification.getClient()
    }

    suspend fun detect(text: String): DetectionResult {
        return try {
            val languageCode = languageIdentifier.identifyLanguage(text).await()

            val languageNames = mapOf(
                "pt" to "Português",
                "en" to "Inglês",
                "es" to "Espanhol",
                "fr" to "Francês",
                "de" to "Alemão",
                "it" to "Italiano",
                "ja" to "Japonês",
                "zh" to "Chinês",
                "ru" to "Russo"
            )

            val displayName = languageNames[languageCode] ?: "Desconhecido"

            DetectionResult(
                detectedLanguage = displayName,
                confidence = 0.95f, // ML Kit não retorna confidence diretamente
                languageCode = languageCode
            )
        } catch (e: Exception) {
            throw Exception("Erro ao detectar idioma: ${e.message}")
        }
    }
}