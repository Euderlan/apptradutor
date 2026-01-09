package com.example.texttranslatorapp.domain.models

// Modelo de domínio que representa o resultado da detecção de idioma
data class DetectionResult(
    val detectedLanguage: String, // Nome legível do idioma detectado
    val confidence: Float,        // Grau de confiança da detecção
    val languageCode: String      // Código do idioma (ex: "pt", "en", "es")
)
