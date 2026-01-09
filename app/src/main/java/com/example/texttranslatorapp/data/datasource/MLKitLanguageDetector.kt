package com.example.texttranslatorapp.data.datasource

// Importa o modelo de resultado da detecção de idioma do domínio
import com.example.texttranslatorapp.domain.models.DetectionResult

// Importa a API de identificação de idioma do ML Kit
import com.google.mlkit.nl.languageid.LanguageIdentification

// Importa extensão para aguardar Tasks do Google usando corrotinas
import kotlinx.coroutines.tasks.await

// Classe responsável por detectar o idioma de um texto usando o ML Kit
class MLKitLanguageDetector {

    // Inicializa o identificador de idioma de forma preguiçosa (lazy)
    // Ele só será criado quando for utilizado pela primeira vez
    private val languageIdentifier by lazy {
        LanguageIdentification.getClient()
    }

    // Função suspensa que detecta o idioma de um texto
    // Retorna um objeto DetectionResult com os dados da detecção
    suspend fun detect(text: String): DetectionResult {
        return try {
            // Executa a detecção do idioma e aguarda o resultado de forma assíncrona
            val languageCode = languageIdentifier.identifyLanguage(text).await()

            // Mapa que associa códigos de idioma aos seus nomes em português
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

            // Obtém o nome do idioma a partir do código retornado
            // Caso não exista no mapa, assume como "Desconhecido"
            val displayName = languageNames[languageCode] ?: "Desconhecido"

            // Cria e retorna o objeto de resultado da detecção
            DetectionResult(
                detectedLanguage = displayName,
                confidence = 0.95f, // Valor fixo, pois o ML Kit não fornece confiança diretamente
                languageCode = languageCode
            )
        } catch (e: Exception) {
            // Lança uma nova exceção com uma mensagem mais descritiva
            throw Exception("Erro ao detectar idioma: ${e.message}")
        }
    }
}
