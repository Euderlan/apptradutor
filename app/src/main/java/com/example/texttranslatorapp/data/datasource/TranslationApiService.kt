package com.example.texttranslatorapp.data.datasource

import com.example.texttranslatorapp.domain.models.TranslationResult
import com.example.texttranslatorapp.presentation.utils.UnicodeDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

/**
 * Serviço de tradução online utilizando a API pública MyMemory.
 *
 * Indicado como fallback ou complemento da tradução offline (ML Kit),
 * especialmente para idiomas não suportados localmente.
 */
class TranslationApiService {

    /**
     * Realiza a tradução do texto via requisição HTTP.
     *
     * Executa em Dispatcher.IO por envolver rede e I/O bloqueante.
     */
    suspend fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): TranslationResult {
        return withContext(Dispatchers.IO) {
            try {
                // Converte os nomes exibidos no app para códigos de idioma da API
                val sourceLangCode = getLanguageCode(sourceLanguage)
                val targetLangCode = getLanguageCode(targetLanguage)

                // Codifica o texto para evitar problemas com espaços e caracteres especiais
                val encodedText = URLEncoder.encode(text, "UTF-8")

                // Endpoint da API MyMemory no formato esperado
                val url =
                    "https://api.mymemory.translated.net/get?q=$encodedText&langpair=$sourceLangCode|$targetLangCode"

                // Cria e configura a conexão HTTP manualmente
                val connection =
                    java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    // Lê a resposta completa da API
                    val response = connection.inputStream
                        .bufferedReader()
                        .use { it.readText() }

                    // Extrai o campo translatedText do JSON retornado
                    var translatedText = extractTranslatedText(response)

                    // Algumas respostas vêm com Unicode escapado (ex: \u00e7)
                    // Esse passo garante texto legível ao usuário
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
                    // Erro HTTP explícito (timeout, limite de requisições, etc.)
                    throw Exception("Erro na tradução: código $responseCode")
                }
            } catch (e: Exception) {
                // Encapsula erros de rede, parsing ou conversão
                throw Exception("Erro ao traduzir: ${e.message}")
            }
        }
    }

    /**
     * Extrai manualmente o valor do campo "translatedText" do JSON retornado.
     *
     * Implementação simples para evitar dependência de bibliotecas de parsing.
     * Depende da estrutura atual da resposta da API.
     */
    private fun extractTranslatedText(jsonResponse: String): String {
        return try {
            val startIndex =
                jsonResponse.indexOf("\"translatedText\":\"") + 18
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

    /**
     * Mapeia os nomes de idiomas exibidos no app
     * para os códigos aceitos pela API MyMemory.
     */
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
            else -> "en" // fallback padrão da API
        }
    }
}
