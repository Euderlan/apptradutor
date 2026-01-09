package com.example.texttranslatorapp.data.datasource

import com.example.texttranslatorapp.domain.models.DetectionResult
import com.google.mlkit.nl.languageid.LanguageIdentification
import kotlinx.coroutines.tasks.await
import java.util.Locale

class MLKitLanguageDetector {

    private val languageIdentifier by lazy {
        // dá pra ajustar o threshold se quiser (ex: 0.4f)
        LanguageIdentification.getClient()
    }

    suspend fun detect(text: String): DetectionResult {
        return try {
            val cleanText = text.trim()

            // 1) Pega lista com confidências (mais robusto que identifyLanguage)
            val possibilities = languageIdentifier.identifyPossibleLanguages(cleanText).await()

            // Escolhe o melhor resultado com maior confiança
            val best = possibilities.maxByOrNull { it.confidence }
            val rawCode = best?.languageTag ?: "und"
            val confidence = best?.confidence ?: 0.0f

            // Normaliza "es-419" -> "es", "pt-BR" -> "pt"
            val normalizedCode = normalizeLanguageCode(rawCode)

            val displayName = codeToPortugueseName(normalizedCode)

            DetectionResult(
                detectedLanguage = displayName,
                confidence = confidence,
                languageCode = normalizedCode
            )
        } catch (e: Exception) {
            throw Exception("Erro ao detectar idioma: ${e.message}")
        }
    }

    private fun normalizeLanguageCode(code: String): String {
        if (code.isBlank() || code == "und") return "und"
        // pega só o idioma base antes do hífen (pt-BR -> pt)
        return code.lowercase(Locale.ROOT).split("-").firstOrNull().orEmpty().ifBlank { "und" }
    }

    private fun codeToPortugueseName(languageCode: String): String {
        val languageNames = mapOf(
            "pt" to "Português",
            "en" to "Inglês",
            "es" to "Espanhol",
            "fr" to "Francês",
            "de" to "Alemão",
            "it" to "Italiano",
            "ja" to "Japonês",
            "zh" to "Chinês",
            "ru" to "Russo",
        )
        return when (languageCode) {
            "und" -> "Desconhecido"
            else -> languageNames[languageCode] ?: "Desconhecido"
        }
    }
}
