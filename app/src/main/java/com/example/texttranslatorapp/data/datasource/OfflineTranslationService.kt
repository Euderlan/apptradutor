package com.example.texttranslatorapp.data.datasource

import com.example.texttranslatorapp.domain.models.TranslationResult
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class OfflineTranslationService {

    /**
     * Garante (quando online) que os modelos necessários para source -> target
     * estejam baixados no aparelho, para permitir tradução offline depois.
     */
    suspend fun ensureModelsDownloaded(
        sourceLanguage: String,
        targetLanguage: String
    ) {
        withContext(Dispatchers.IO) {
            val sourceCode = getLanguageCode(sourceLanguage)
            val targetCode = getLanguageCode(targetLanguage)

            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceCode)
                .setTargetLanguage(targetCode)
                .build()

            val translator = Translation.getClient(options)
            try {
                val conditions = DownloadConditions.Builder().build()
                translator.downloadModelIfNeeded(conditions).await()
            } finally {
                translator.close()
            }
        }
    }

    /**
     * @param allowDownload
     *  - true: pode baixar modelos se necessário (use quando estiver online).
     *  - false: NÃO baixa; só traduz se os modelos já estiverem instalados (use quando offline).
     */
    suspend fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String,
        allowDownload: Boolean
    ): TranslationResult {
        return withContext(Dispatchers.IO) {
            try {
                val sourceCode = getLanguageCode(sourceLanguage)
                val targetCode = getLanguageCode(targetLanguage)

                // Verifica se modelos já estão instalados
                val modelManager = RemoteModelManager.getInstance()
                val sourceModel = TranslateRemoteModel.Builder(sourceCode).build()
                val targetModel = TranslateRemoteModel.Builder(targetCode).build()

                val sourceInstalled = modelManager.isModelDownloaded(sourceModel).await()
                val targetInstalled = modelManager.isModelDownloaded(targetModel).await()

                // Se estiver offline (allowDownload = false) e faltar algum modelo, falha com mensagem clara
                if (!allowDownload && (!sourceInstalled || !targetInstalled)) {
                    throw Exception(
                        "Modo offline: modelo de tradução não instalado para $sourceLanguage → $targetLanguage. " +
                                "Conecte-se à internet uma vez para baixar os modelos."
                    )
                }

                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(sourceCode)
                    .setTargetLanguage(targetCode)
                    .build()

                val translator = Translation.getClient(options)

                try {
                    // Se puder baixar, garante modelos
                    if (allowDownload) {
                        val conditions = DownloadConditions.Builder().build()
                        translator.downloadModelIfNeeded(conditions).await()
                    }

                    val translated = translator.translate(text).await()

                    TranslationResult(
                        originalText = text,
                        translatedText = translated,
                        sourceLanguage = sourceLanguage,
                        targetLanguage = targetLanguage
                    )
                } finally {
                    translator.close()
                }

            } catch (e: Exception) {
                throw Exception("Erro ao traduzir offline: ${e.message}")
            }
        }
    }

    /**
     * Mapeia os nomes exibidos no app para códigos BCP-47 suportados pelo ML Kit Translate.
     * Ajuste conforme sua lista de idiomas no app.
     */
    private fun getLanguageCode(languageName: String): String {
        return when (languageName.lowercase()) {
            "português", "portugues", "pt" -> "pt"
            "inglês", "ingles", "english", "en" -> "en"
            "espanhol", "spanish", "es" -> "es"
            "francês", "frances", "french", "fr" -> "fr"
            "alemão", "alemao", "german", "de" -> "de"
            "italiano", "italian", "it" -> "it"
            "japonês", "japones", "japanese", "ja" -> "ja"
            "chinês", "chines", "chinese", "zh" -> "zh"
            "russo", "russian", "ru" -> "ru"
            else -> "en"
        }
    }
}
