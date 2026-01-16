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

/**
 * Serviço responsável por tradução offline utilizando ML Kit.
 *
 * A estratégia adotada separa claramente:
 * - download prévio de modelos (quando online)
 * - uso restrito a modelos locais (quando offline)
 */
class OfflineTranslationService {

    /**
     * Garante (quando online) que os modelos necessários para source -> target
     * estejam baixados no aparelho, para permitir tradução offline depois.
     *
     * Deve ser chamado enquanto o dispositivo tem conexão com a internet.
     */
    suspend fun ensureModelsDownloaded(
        sourceLanguage: String,
        targetLanguage: String
    ) {
        // Executa em thread de I/O por envolver acesso a disco e rede
        withContext(Dispatchers.IO) {
            val sourceCode = getLanguageCode(sourceLanguage)
            val targetCode = getLanguageCode(targetLanguage)

            // Configura o tradutor para o par de idiomas desejado
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceCode)
                .setTargetLanguage(targetCode)
                .build()

            val translator = Translation.getClient(options)
            try {
                // Condições padrão de download (Wi-Fi ou dados, conforme política do ML Kit)
                val conditions = DownloadConditions.Builder().build()
                translator.downloadModelIfNeeded(conditions).await()
            } finally {
                // Libera recursos nativos do tradutor
                translator.close()
            }
        }
    }

    /**
     * Realiza a tradução do texto.
     *
     * @param allowDownload
     *  - true: permite baixar modelos ausentes (modo online).
     *  - false: exige que os modelos já estejam instalados (modo offline).
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

                // Gerenciador central de modelos remotos do ML Kit
                val modelManager = RemoteModelManager.getInstance()

                // Representações dos modelos de origem e destino
                val sourceModel = TranslateRemoteModel.Builder(sourceCode).build()
                val targetModel = TranslateRemoteModel.Builder(targetCode).build()

                // Verifica se os modelos já estão disponíveis localmente
                val sourceInstalled = modelManager.isModelDownloaded(sourceModel).await()
                val targetInstalled = modelManager.isModelDownloaded(targetModel).await()

                // Garante comportamento previsível no modo offline
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
                    // No modo online, garante que os modelos estejam presentes
                    if (allowDownload) {
                        val conditions = DownloadConditions.Builder().build()
                        translator.downloadModelIfNeeded(conditions).await()
                    }

                    // Executa a tradução usando apenas recursos locais após o download
                    val translated = translator.translate(text).await()

                    TranslationResult(
                        originalText = text,
                        translatedText = translated,
                        sourceLanguage = sourceLanguage,
                        targetLanguage = targetLanguage
                    )
                } finally {
                    // Fecha o tradutor para evitar vazamento de recursos
                    translator.close()
                }

            } catch (e: Exception) {
                // Propaga erro com mensagem padronizada para a camada superior
                throw Exception("Erro ao traduzir offline: ${e.message}")
            }
        }
    }

    /**
     * Converte os nomes exibidos no app para códigos BCP-47
     * compatíveis com o ML Kit Translate.
     *
     * Centraliza o mapeamento para evitar espalhar lógica de conversão no código.
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
            else -> "en" // fallback seguro
        }
    }
}
