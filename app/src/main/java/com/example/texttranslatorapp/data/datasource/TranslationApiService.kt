package com.example.texttranslatorapp.data.datasource

import com.example.texttranslatorapp.domain.models.TranslationResult
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import kotlinx.coroutines.tasks.await

class TranslationApiService {

    suspend fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): TranslationResult {
        return try {
            val sourceLangCode = languageNameToCode(sourceLanguage)
            val targetLangCode = languageNameToCode(targetLanguage)

            val translator = Translation.getClient(
                Translation.TranslatorOptions.Builder()
                    .setSourceLanguage(sourceLangCode)
                    .setTargetLanguage(targetLangCode)
                    .build()
            )

            val translatedText = translator.translate(text).await()

            TranslationResult(
                originalText = text,
                translatedText = translatedText,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage
            )
        } catch (e: Exception) {
            throw Exception("Erro ao traduzir: ${e.message}")
        }
    }

    private fun languageNameToCode(language: String): Int {
        return when (language.lowercase()) {
            "português", "pt" -> TranslateLanguage.PORTUGUESE
            "inglês", "en" -> TranslateLanguage.ENGLISH
            "espanhol", "es" -> TranslateLanguage.SPANISH
            "francês", "fr" -> TranslateLanguage.FRENCH
            "alemão", "de" -> TranslateLanguage.GERMAN
            "italiano", "it" -> TranslateLanguage.ITALIAN
            "japonês", "ja" -> TranslateLanguage.JAPANESE
            "chinês", "zh" -> TranslateLanguage.CHINESE
            "russo", "ru" -> TranslateLanguage.RUSSIAN
            else -> TranslateLanguage.ENGLISH
        }
    }
}