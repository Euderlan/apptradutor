package com.example.texttranslatorapp.data.datasource

import com.example.texttranslatorapp.domain.models.DetectionResult
import com.google.mlkit.nl.languageid.LanguageIdentification
import kotlinx.coroutines.tasks.await
import java.util.Locale

/**
 * Classe responsável por detectar o idioma de um texto utilizando o ML Kit.
 */
class MLKitLanguageDetector {

    /**
     * Instância do identificador de idiomas do ML Kit.
     * É inicializada de forma lazy para evitar custo de criação desnecessário.
     * O threshold de confiança pode ser configurado se necessário.
     */
    private val languageIdentifier by lazy {
        LanguageIdentification.getClient()
    }

    /**
     * Detecta o idioma de um texto de forma assíncrona.
     *
     * @param text Texto de entrada a ser analisado.
     * @return DetectionResult contendo idioma detectado, código do idioma e confiança.
     * @throws Exception em caso de falha na detecção.
     */
    suspend fun detect(text: String): DetectionResult {
        return try {
            // Remove espaços em branco no início e no fim do texto
            val cleanText = text.trim()

            // Obtém uma lista de possíveis idiomas com suas respectivas confidências
            // Esse método é mais robusto do que identifyLanguage(), pois retorna múltiplas opções
            val possibilities = languageIdentifier
                .identifyPossibleLanguages(cleanText)
                .await()

            // Seleciona o idioma com maior nível de confiança
            val best = possibilities.maxByOrNull { it.confidence }

            // Código bruto do idioma retornado pelo ML Kit (ex: "pt-BR", "es-419")
            val rawCode = best?.languageTag ?: "und"

            // Valor de confiança associado ao idioma detectado
            val confidence = best?.confidence ?: 0.0f

            // Normaliza o código do idioma para o formato base (ex: "pt-BR" -> "pt")
            val normalizedCode = normalizeLanguageCode(rawCode)

            // Converte o código do idioma para um nome legível em português
            val displayName = codeToPortugueseName(normalizedCode)

            // Retorna o resultado da detecção encapsulado no modelo de domínio
            DetectionResult(
                detectedLanguage = displayName,
                confidence = confidence,
                languageCode = normalizedCode
            )
        } catch (e: Exception) {
            // Encapsula qualquer erro ocorrido durante o processo de detecção
            throw Exception("Erro ao detectar idioma: ${e.message}")
        }
    }

    /**
     * Normaliza o código do idioma retornado pelo ML Kit.
     *
     * Exemplos:
     * - "pt-BR" -> "pt"
     * - "es-419" -> "es"
     * - "und" ou vazio -> "und"
     */
    private fun normalizeLanguageCode(code: String): String {
        // Retorna "und" caso o código seja inválido ou não identificado
        if (code.isBlank() || code == "und") return "und"

        // Converte para minúsculas e extrai apenas o idioma base antes do hífen
        return code
            .lowercase(Locale.ROOT)
            .split("-")
            .firstOrNull()
            .orEmpty()
            .ifBlank { "und" }
    }

    /**
     * Converte um código de idioma para o nome correspondente em português.
     *
     * @param languageCode Código ISO do idioma (ex: "pt", "en").
     * @return Nome do idioma em português ou "Desconhecido".
     */
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
            // Código padrão para idioma não identificado
            "und" -> "Desconhecido"
            else -> languageNames[languageCode] ?: "Desconhecido"
        }
    }
}
