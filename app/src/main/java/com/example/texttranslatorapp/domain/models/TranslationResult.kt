package com.example.texttranslatorapp.domain.models

data class TranslationResult(
    val originalText: String,
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String
)