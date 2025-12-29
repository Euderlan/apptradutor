package com.example.texttranslatorapp.domain.models

data class DetectionResult(
    val detectedLanguage: String,
    val confidence: Float,
    val languageCode: String // ex: "pt", "en", "es"
)