package com.example.texttranslatorapp.data.datasource

import com.example.texttranslatorapp.domain.models.TranslationResult
import com.example.texttranslatorapp.presentation.utils.UnicodeDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

class TranslationApiService {

    suspend fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): TranslationResult {
        return withContext(Dispatchers.IO) {
            try {
                // Converter nomes para códigos de idioma
                val sourceLangCode = getLanguageCode(sourceLanguage)
                val targetLangCode = getLanguageCode(targetLanguage)

                // URL da API de tradução (MyMemory - Grátis)
                val encodedText = URLEncoder.encode(text, "UTF-8")
                val url = "https://api.mymemory.translated.net/get?q=$encodedText&langpair=$sourceLangCode|$targetLangCode"

                // Fazer requisição
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }

                    // Parse JSON simples
                    var translatedText = extractTranslatedText(response)

                    // Decodificar Unicode escapado se necessário
                    if (UnicodeDecoder.hasEscapedUnicode(translatedText)) {
                        translatedText = UnicodeDecoder.decode(translatedText)
                    }

                    TranslationResult(
                        originalText = text,
                        translatedText = translatedText,
                        sourceLanguage = sourceLanguage,
                        targetLanguage = targetLanguage
                    )
                } else {
                    throw Exception("Erro na tradução: código $responseCode")
                }
            } catch (e: Exception) {
                throw Exception("Erro ao traduzir: ${e.message}")
            }
        }
    }

    private fun extractTranslatedText(jsonResponse: String): String {
        return try {
            // Extrair o valor de "translatedText" do JSON
            val startIndex = jsonResponse.indexOf("\"translatedText\":\"") + 18
            val endIndex = jsonResponse.indexOf("\"", startIndex)

            if (startIndex > 18 && endIndex > startIndex) {
                jsonResponse.substring(startIndex, endIndex)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
            } else {
                "Erro ao processar tradução"
            }
        } catch (e: Exception) {
            "Erro: ${e.message}"
        }
    }

    private fun getLanguageCode(languageName: String): String {
        return when (languageName.lowercase()) {
            "português", "pt" -> "pt"
            "inglês", "en" -> "en"
            "espanhol", "es" -> "es"
            "francês", "fr" -> "fr"
            "alemão", "de" -> "de"
            "italiano", "it" -> "it"
            "japonês", "ja" -> "ja"
            "chinês", "zh" -> "zh"
            "russo", "ru" -> "ru"
            else -> "en"
        }
    }
}