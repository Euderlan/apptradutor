package com.example.texttranslatorapp.data.repository

import com.example.texttranslatorapp.data.datasource.OfflineTranslationService
import com.example.texttranslatorapp.data.datasource.TranslationApiService
import com.example.texttranslatorapp.domain.models.TranslationResult
import com.example.texttranslatorapp.util.ConnectivityChecker

class TranslationRepository(
    private val onlineService: TranslationApiService,
    private val offlineService: OfflineTranslationService,
    private val connectivityChecker: ConnectivityChecker
) {

    suspend fun translateText(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): TranslationResult {

        val online = connectivityChecker.isOnline()

        // Se estiver online, já tenta garantir que o modelo offline fique baixado
        // (assim, se depois ficar sem internet, funciona).
        if (online) {
            try {
                offlineService.ensureModelsDownloaded(sourceLanguage, targetLanguage)
            } catch (_: Exception) {
                // Se falhar, não impede a tradução online.
            }
        }

        // Sem internet: tenta OFFLINE (sem tentar download)
        if (!online) {
            return offlineService.translate(
                text = text,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                allowDownload = false
            )
        }

        // Com internet: tenta ONLINE; se falhar, cai pro OFFLINE permitindo download
        return try {
            onlineService.translate(text, sourceLanguage, targetLanguage)
        } catch (_: Exception) {
            offlineService.translate(
                text = text,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                allowDownload = true
            )
        }
    }
}
