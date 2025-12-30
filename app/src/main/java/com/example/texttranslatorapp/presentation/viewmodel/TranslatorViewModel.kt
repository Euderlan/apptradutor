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

    // ✅ Função para atualizar texto extraído manualmente (vindo da ImageViewerActivity)
    fun setExtractedText(text: String, detectedLanguage: String? = null) {
        _extractedText.value = text
        Log.d("ViewModel", "Texto atualizado (setExtractedText): '$text'")

        if (detectedLanguage != null) {
            // Criar um DetectionResult com o idioma fornecido
            val languageCode = getLanguageCode(detectedLanguage)
            _detectedLanguage.value = DetectionResult(
                detectedLanguage = detectedLanguage,
                confidence = 1.0f,
                languageCode = languageCode
            )
            Log.d("ViewModel", "Idioma definido: $detectedLanguage ($languageCode)")
        }
    }

    // ✅ NOVO: Função para atualizar texto enquanto o usuário edita
    // Não detecta idioma de novo, apenas atualiza o texto
    fun updateExtractedText(text: String) {
        _extractedText.value = text
        Log.d("ViewModel", "Texto atualizado (updateExtractedText): '$text'")
        // Não faz detecção de novo, mantém o idioma anterior
    }

    // ✅ Função para detectar idioma de texto manualmente inserido
    fun detectLanguageForText(text: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                if (text.isNotEmpty()) {
                    val detection = detectLanguageUseCase(text)
                    _detectedLanguage.value = detection
                    Log.d("ViewModel", "Idioma detectado para texto: ${detection.detectedLanguage}")
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Erro ao detectar idioma"
                Log.e("ViewModel", "Erro ao detectar idioma: ${e.message}")
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
                // ✅ IMPORTANTE: Pegar o idioma de origem do ViewModel
                val sourceLanguageName = _detectedLanguage.value?.detectedLanguage
                val sourceLanguageCode = _detectedLanguage.value?.languageCode

                Log.d("ViewModel", "Idioma de origem: $sourceLanguageName ($sourceLanguageCode)")
                Log.d("ViewModel", "Idioma alvo: $targetLanguageCode")

                val textToTranslate = _extractedText.value

                Log.d("ViewModel", "Texto a traduzir: '$textToTranslate'")

                if (textToTranslate.isEmpty()) {
                    _error.value = "Nenhum texto para traduzir"
                    Log.e("ViewModel", "Erro: Nenhum texto para traduzir")
                    return@launch
                }

                // ✅ Usar o nome do idioma (ex: "Português") como sourceLanguage
                val sourceLangName = sourceLanguageName ?: "English"

                Log.d("ViewModel", "Chamando translateTextUseCase com:")
                Log.d("ViewModel", "  - Texto: '$textToTranslate'")
                Log.d("ViewModel", "  - Idioma origem: '$sourceLangName'")
                Log.d("ViewModel", "  - Idioma alvo: '$targetLanguageCode'")

                val result = translateTextUseCase(
                    textToTranslate,
                    sourceLangName,
                    targetLanguageCode
                )

                _translatedText.value = result.translatedText
                Log.d("ViewModel", "Tradução concluída: '${result.translatedText}'")

            } catch (e: Exception) {
                _error.value = "Erro ao traduzir: ${e.message}"
                Log.e("ViewModel", "Erro ao traduzir: ${e.message}", e)
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