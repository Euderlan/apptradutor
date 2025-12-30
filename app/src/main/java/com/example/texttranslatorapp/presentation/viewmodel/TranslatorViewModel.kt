package com.example.texttranslatorapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.texttranslatorapp.domain.usecases.DetectLanguageUseCase
import com.example.texttranslatorapp.domain.usecases.ExtractTextUseCase
import com.example.texttranslatorapp.domain.usecases.TranslateTextUseCase
import com.example.texttranslatorapp.domain.models.DetectionResult
import com.example.texttranslatorapp.domain.models.TranslationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.graphics.Bitmap

class TranslatorViewModel(
    private val extractTextUseCase: ExtractTextUseCase,
    private val detectLanguageUseCase: DetectLanguageUseCase,
    private val translateTextUseCase: TranslateTextUseCase
) : ViewModel() {

    private val _extractedText = MutableStateFlow("")
    val extractedText: StateFlow<String> = _extractedText

    private val _detectedLanguage = MutableStateFlow<DetectionResult?>(null)
    val detectedLanguage: StateFlow<DetectionResult?> = _detectedLanguage

    private val _translatedText = MutableStateFlow("")
    val translatedText: StateFlow<String> = _translatedText

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun processImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Extrair texto
                val text = extractTextUseCase(bitmap)
                _extractedText.value = text

                // Detectar idioma
                if (text.isNotEmpty()) {
                    val detection = detectLanguageUseCase(text)
                    _detectedLanguage.value = detection
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Erro desconhecido"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun translateText(targetLanguageCode: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val sourceLanguage = _detectedLanguage.value?.detectedLanguage ?: "English"
                val textToTranslate = _extractedText.value

                if (textToTranslate.isEmpty()) {
                    _error.value = "Nenhum texto para traduzir"
                    return@launch
                }

                val result = translateTextUseCase(
                    textToTranslate,
                    sourceLanguage,
                    targetLanguageCode
                )
                _translatedText.value = result.translatedText
            } catch (e: Exception) {
                _error.value = "Erro ao traduzir: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}