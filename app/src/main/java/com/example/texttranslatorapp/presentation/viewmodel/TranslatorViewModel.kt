package com.example.texttranslatorapp.presentation.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.texttranslatorapp.domain.models.DetectionResult
import com.example.texttranslatorapp.domain.usecases.DetectLanguageUseCase
import com.example.texttranslatorapp.domain.usecases.ExtractTextUseCase
import com.example.texttranslatorapp.domain.usecases.TranslateTextUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ViewModel que centraliza o estado e as ações do fluxo: OCR -> detecção de idioma -> tradução
class TranslatorViewModel(
    private val extractTextUseCase: ExtractTextUseCase,
    private val detectLanguageUseCase: DetectLanguageUseCase,
    private val translateTextUseCase: TranslateTextUseCase
) : ViewModel() {

    // Estado do texto extraído (OCR) ou digitado pelo usuário
    private val _extractedText = MutableStateFlow("")
    val extractedText: StateFlow<String> = _extractedText

    // Estado do resultado da detecção de idioma
    private val _detectedLanguage = MutableStateFlow<DetectionResult?>(null)
    val detectedLanguage: StateFlow<DetectionResult?> = _detectedLanguage

    // Idiomas selecionados para tradução (origem e destino)
    private val _sourceLanguage = MutableStateFlow("Inglês")
    val sourceLanguage: StateFlow<String> = _sourceLanguage

    private val _targetLanguage = MutableStateFlow("Português")
    val targetLanguage: StateFlow<String> = _targetLanguage

    // Estado do texto traduzido
    private val _translatedText = MutableStateFlow("")
    val translatedText: StateFlow<String> = _translatedText

    // Estado de carregamento para controlar UI (botões, progress etc.)
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Estado de erro para feedback ao usuário
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Job usado para cancelar detecção automática quando o texto muda rapidamente
    private var detectJob: Job? = null

    // Processa uma imagem: otimiza, extrai texto via OCR e detecta o idioma automaticamente
    fun processImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val imagemOtimizada = otimizarImagemParaOCR(bitmap)
                val text = extractTextUseCase(imagemOtimizada)
                _extractedText.value = text
                Log.d("ViewModel", "Texto extraído: '$text'")

                autoDetectLanguageAndAdjustLanguages(text)

            } catch (e: Exception) {
                _error.value = e.message ?: "Erro desconhecido"
                Log.e("ViewModel", "Erro: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Redimensiona imagens grandes para melhorar desempenho do OCR
    private fun otimizarImagemParaOCR(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width < 1200 || height < 1200) {
            Log.d("OPTIMIZE", "Imagem pequena, não otimizando")
            return bitmap
        }

        val maxDimension = 2000
        val ratio = if (width > height) maxDimension.toFloat() / width else maxDimension.toFloat() / height
        val novaLargura = (width * ratio).toInt()
        val novaAltura = (height * ratio).toInt()

        Log.d("OPTIMIZE", "Original: ${width}x${height} → Otimizado: ${novaLargura}x${novaAltura}")
        return Bitmap.createScaledBitmap(bitmap, novaLargura, novaAltura, true)
    }

    // Atualiza o texto extraído e, se não houver idioma confiável, dispara auto-detecção
    fun setExtractedText(text: String, detectedLanguage: String? = null) {
        _extractedText.value = text
        Log.d("ViewModel", "Texto atualizado: '$text'")

        val safeDetected = detectedLanguage?.trim()?.takeIf { it.isNotEmpty() }

        // Se o idioma veio explícito (ex.: outra tela), atualiza estado e ajusta destino se necessário
        if (safeDetected != null) {
            _detectedLanguage.value = DetectionResult(
                detectedLanguage = safeDetected,
                confidence = 1.0f,
                languageCode = getLanguageCode(safeDetected)
            )
            _sourceLanguage.value = safeDetected
            Log.d("ViewModel", "Idioma definido: $safeDetected")
            adjustTargetIfSameAsSource(safeDetected)
        } else {
            autoDetectLanguageAndAdjustLanguages(text)
        }
    }

    // Atualiza o texto digitado/colado e dispara detecção automática
    fun updateExtractedText(text: String) {
        _extractedText.value = text
        Log.d("ViewModel", "Texto atualizado: '$text'")
        autoDetectLanguageAndAdjustLanguages(text)
    }

    // Atualiza idioma de origem selecionado manualmente
    fun setSourceLanguage(language: String) {
        _sourceLanguage.value = language
        Log.d("ViewModel", "Idioma de origem atualizado: $language")
    }

    // Atualiza idioma de destino selecionado manualmente
    fun setTargetLanguage(language: String) {
        _targetLanguage.value = language
        Log.d("ViewModel", "Idioma de destino atualizado: $language")
    }

    // Inverte idiomas de origem e destino
    fun swapLanguages() {
        val temp = _sourceLanguage.value
        _sourceLanguage.value = _targetLanguage.value
        _targetLanguage.value = temp
        Log.d("ViewModel", "Idiomas invertidos: ${_sourceLanguage.value} <-> ${_targetLanguage.value}")
    }

    // Traduz o texto atual; se o idioma de origem estiver vazio/desconhecido, detecta antes de traduzir
    fun translateText() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val textToTranslate = _extractedText.value.trim()
                if (textToTranslate.isEmpty()) {
                    _error.value = "Nenhum texto para traduzir"
                    return@launch
                }

                // Evita que uma detecção automática em andamento altere o estado durante a tradução
                detectJob?.cancel()

                var sourceLang = _sourceLanguage.value.trim()
                val targetLang = _targetLanguage.value.trim()

                // Se source estiver inválido, detecta imediatamente e usa o resultado
                if (sourceLang.isEmpty() || sourceLang.equals("Desconhecido", ignoreCase = true)) {
                    try {
                        val detection = detectLanguageUseCase(textToTranslate)
                        _detectedLanguage.value = detection
                        _sourceLanguage.value = detection.detectedLanguage
                        sourceLang = detection.detectedLanguage
                        Log.d("ViewModel", "Idioma detectado (antes de traduzir): $sourceLang")
                        adjustTargetIfSameAsSource(sourceLang)
                    } catch (e: Exception) {
                        // Fallback para evitar quebra do fluxo de tradução
                        sourceLang = "Inglês"
                        _sourceLanguage.value = sourceLang
                        Log.e("ViewModel", "Falha ao detectar idioma antes de traduzir: ${e.message}", e)
                    }
                }

                Log.d("ViewModel", "Traduzindo de '$sourceLang' para '$targetLang'")
                Log.d("ViewModel", "Texto: '$textToTranslate'")

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

    // Detecta idioma de forma assíncrona e ajusta automaticamente o destino se ficar igual ao origem
    private fun autoDetectLanguageAndAdjustLanguages(text: String) {
        val normalized = text.trim()
        if (normalized.length < 3) {
            _detectedLanguage.value = null
            return
        }

        // Cancela a detecção anterior para evitar múltiplas chamadas concorrentes
        detectJob?.cancel()
        detectJob = viewModelScope.launch {
            try {
                val detection = detectLanguageUseCase(normalized)
                _detectedLanguage.value = detection
                _sourceLanguage.value = detection.detectedLanguage
                Log.d("ViewModel", "Idioma detectado automaticamente: ${detection.detectedLanguage}")
                adjustTargetIfSameAsSource(detection.detectedLanguage)
            } catch (e: Exception) {
                Log.e("ViewModel", "Falha ao detectar idioma automaticamente: ${e.message}", e)
            }
        }
    }

    // Garante que origem e destino não fiquem iguais após detecção/seleção
    private fun adjustTargetIfSameAsSource(sourceDetected: String) {
        if (_targetLanguage.value == sourceDetected) {
            _targetLanguage.value = if (sourceDetected.equals("Português", ignoreCase = true)) {
                "Inglês"
            } else {
                "Português"
            }
            Log.d("ViewModel", "Destino ajustado automaticamente para: ${_targetLanguage.value}")
        }
    }

    // Converte nome do idioma para um código usado internamente (fallback "en")
    private fun getLanguageCode(languageName: String): String {
        return when (languageName.lowercase()) {
            "português", "portugues", "pt" -> "pt"
            "inglês", "ingles", "english", "en" -> "en"
            "espanhol", "spanish", "es" -> "es"
            "francês", "frances", "french", "fr" -> "fr"
            "alemão", "alemao", "german", "de" -> "de"
            "italiano", "italian", "it" -> "it"
            "japonês", "japones", "japanese", "ja" -> "ja"
            "chinês", "chines", "chinese", "zh" -> "zh"
            "russo", "russian", "ru" -> "ru"
            else -> "en"
        }
    }
}
