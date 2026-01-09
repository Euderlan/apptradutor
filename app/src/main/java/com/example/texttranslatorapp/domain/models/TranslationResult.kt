package com.example.texttranslatorapp.domain.models

// Modelo de domínio que representa o resultado de uma tradução
data class TranslationResult(
    val originalText: String,    // Texto antes da tradução
    val translatedText: String,  // Texto traduzido
    val sourceLanguage: String,  // Idioma de origem
    val targetLanguage: String   // Idioma de destino
)
