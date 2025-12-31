package com.example.texttranslatorapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.texttranslatorapp.domain.usecases.DetectLanguageUseCase
import com.example.texttranslatorapp.domain.usecases.ExtractTextUseCase
import com.example.texttranslatorapp.domain.usecases.TranslateTextUseCase
import com.example.texttranslatorapp.domain.models.DetectionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import android.util.Log

class TranslatorViewModel(
    private val extractTextUseCase: ExtractTextUseCase,
    private val detectLanguageUseCase: DetectLanguageUseCase,
    private val translateTextUseCase: TranslateTextUseCase
) : ViewModel() {

    private val _extractedText = MutableStateFlow("")
    val extractedText: StateFlow<String> = _extractedText

    private val _detectedLanguage = MutableStateFlow<DetectionResult?>(null)
    val detectedLanguage: StateFlow<DetectionResult?> = _detectedLanguage

    // ✅ Idioma de origem (selecionado/detectado)
    private val _sourceLanguage = MutableStateFlow("Inglês")
    val sourceLanguage: StateFlow<String> = _sourceLanguage

    // ✅ Idioma de destino
    private val _targetLanguage = MutableStateFlow("Português")
    val targetLanguage: StateFlow<String> = _targetLanguage

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
                Log.d("ViewModel", "Texto extraído da imagem: '$text'")

                // Detectar idioma
                if (text.isNotEmpty()) {
                    val detection = detectLanguageUseCase(text)
                    _detectedLanguage.value = detection
                    _sourceLanguage.value = detection.detectedLanguage
                    Log.d("ViewModel", "Idioma detectado: ${detection.detectedLanguage}")
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Erro desconhecido"
                Log.e("ViewModel", "Erro ao processar imagem: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setExtractedText(text: String, detectedLanguage: String? = null) {
        _extractedText.value = text
        Log.d("ViewModel", "Texto atualizado: '$text'")

        if (detectedLanguage != null) {
            _detectedLanguage.value = DetectionResult(
                detectedLanguage = detectedLanguage,
                confidence = 1.0f,
                languageCode = getLanguageCode(detectedLanguage)
            )
            _sourceLanguage.value = detectedLanguage
            Log.d("ViewModel", "Idioma definido: $detectedLanguage")
        }
    }

    fun updateExtractedText(text: String) {
        _extractedText.value = text
        Log.d("ViewModel", "Texto atualizado: '$text'")
    }

    // ✅ NOVO: Função para atualizar idioma de origem manualmente
    fun setSourceLanguage(language: String) {
        _sourceLanguage.value = language
        Log.d("ViewModel", "Idioma de origem atualizado: $language")
    }

    // ✅ NOVO: Função para atualizar idioma de destino manualmente
    fun setTargetLanguage(language: String) {
        _targetLanguage.value = language
        Log.d("ViewModel", "Idioma de destino atualizado: $language")
    }

    // ✅ NOVO: Inverter idiomas
    fun swapLanguages() {
        val temp = _sourceLanguage.value
        _sourceLanguage.value = _targetLanguage.value
        _targetLanguage.value = temp
        Log.d("ViewModel", "Idiomas invertidos: ${_sourceLanguage.value} <-> ${_targetLanguage.value}")
    }

    // ✅ Traduzir com idiomas atuais
    fun translateText() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val textToTranslate = _extractedText.value
                val sourceLang = _sourceLanguage.value
                val targetLang = _targetLanguage.value

                Log.d("ViewModel", "Traduzindo de '$sourceLang' para '$targetLang'")
                Log.d("ViewModel", "Texto: '$textToTranslate'")

                if (textToTranslate.isEmpty()) {
                    _error.value = "Nenhum texto para traduzir"
                    return@launch
                }

                val result = translateTextUseCase(textToTranslate, sourceLang, targetLang)
                _translatedText.value = result.translatedText

                Log.d("ViewModel", "Tradução concluída: '${result.translatedText}'")

            } catch (e: Exception) {
                _error.value = "Erro ao traduzir: ${e.message}"
                Log.e("ViewModel", "Erro: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun getLanguageCode(languageName: String): String {
        return when(languageName.lowercase()) {
            "português", "pt" -> "pt"
            "inglês", "english", "en" -> "en"
            "espanhol", "spanish", "es" -> "es"
            "francês", "french", "fr" -> "fr"
            "alemão", "german", "de" -> "de"
            "italiano", "italian", "it" -> "it"
            "japonês", "japanese", "ja" -> "ja"
            "chinês", "chinese", "zh" -> "zh"
            "russo", "russian", "ru" -> "ru"
            else -> "en"
        }
    }
}