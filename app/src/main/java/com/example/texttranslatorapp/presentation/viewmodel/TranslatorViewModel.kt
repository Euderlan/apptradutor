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
    // Dependências do domínio (casos de uso). A UI aciona o ViewModel, e o ViewModel delega para os UseCases.
    private val extractTextUseCase: ExtractTextUseCase,
    private val detectLanguageUseCase: DetectLanguageUseCase,
    private val translateTextUseCase: TranslateTextUseCase
) : ViewModel() {

    // Estado interno mutável (privado) e exposição imutável (pública) via StateFlow.
    // A UI observa "extractedText" para atualizar o campo de texto extraído.
    private val _extractedText = MutableStateFlow("")
    val extractedText: StateFlow<String> = _extractedText

    // Estado do resultado de detecção (pode ser null enquanto não detectou ou quando não há texto).
    private val _detectedLanguage = MutableStateFlow<DetectionResult?>(null)
    val detectedLanguage: StateFlow<DetectionResult?> = _detectedLanguage

    // Idioma de origem (selecionado ou definido pela detecção).
    private val _sourceLanguage = MutableStateFlow("Inglês")
    val sourceLanguage: StateFlow<String> = _sourceLanguage

    // Idioma de destino definido pelo usuário.
    private val _targetLanguage = MutableStateFlow("Português")
    val targetLanguage: StateFlow<String> = _targetLanguage

    // Estado do texto traduzido; a UI observa para preencher a saída.
    private val _translatedText = MutableStateFlow("")
    val translatedText: StateFlow<String> = _translatedText

    // Flag para controlar loading na UI (desabilitar botões, mostrar progresso, etc.).
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Estado de erro; pode ser exibido como toast/snackbar.
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun processImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Otimizar imagem ANTES de processar
                val imagemOtimizada = otimizarImagemParaOCR(bitmap)

                val text = extractTextUseCase(imagemOtimizada)
                _extractedText.value = text
                Log.d("ViewModel", "Texto extraído: '$text'")

                if (text.isNotEmpty()) {
                    val detection = detectLanguageUseCase(text)
                    _detectedLanguage.value = detection
                    _sourceLanguage.value = detection.detectedLanguage
                    Log.d("ViewModel", "Idioma: ${detection.detectedLanguage}")
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Erro desconhecido"
                Log.e("ViewModel", "Erro: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun otimizarImagemParaOCR(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Se imagem já está pequena, NÃO reduz
        if (width < 1200 || height < 1200) {
            Log.d("OPTIMIZE", "Imagem pequena, não otimizando")
            return bitmap  // ← Retorna como está
        }

        // Só reduz se MUITO grande
        val maxDimension = 2000
        val ratio = if (width > height) {
            maxDimension.toFloat() / width
        } else {
            maxDimension.toFloat() / height
        }

        val novaLargura = (width * ratio).toInt()
        val novaAltura = (height * ratio).toInt()

        Log.d("OPTIMIZE", "Original: ${width}x${height} → Otimizado: ${novaLargura}x${novaAltura}")

        return Bitmap.createScaledBitmap(bitmap, novaLargura, novaAltura, true)
    }

    fun setExtractedText(text: String, detectedLanguage: String? = null) {
        // Atualiza texto extraído diretamente (ex.: vindo de seleção/recorte em outra tela).
        _extractedText.value = text
        Log.d("ViewModel", "Texto atualizado: '$text'")

        // Se o idioma veio pronto (por exemplo, de outro componente), atualiza o estado de detecção manualmente.
        if (detectedLanguage != null) {
            _detectedLanguage.value = DetectionResult(
                // Nome do idioma (como está sendo exibido/guardado no app).
                detectedLanguage = detectedLanguage,
                // Confiança fixa como 1.0f pois está sendo “forçado” pelo parâmetro recebido.
                confidence = 1.0f,
                // Converte o nome para código (pt/en/es...) para uso em integrações.
                languageCode = getLanguageCode(detectedLanguage)
            )
            // Ajusta idioma de origem para manter consistência com o que foi informado.
            _sourceLanguage.value = detectedLanguage
            Log.d("ViewModel", "Idioma definido: $detectedLanguage")
        }
    }

    fun updateExtractedText(text: String) {
        // Atualiza apenas o texto extraído (sem interferir em idioma/estado de detecção).
        _extractedText.value = text
        Log.d("ViewModel", "Texto atualizado: '$text'")
    }

    // Atualiza idioma de origem manualmente (seleção do usuário).
    fun setSourceLanguage(language: String) {
        _sourceLanguage.value = language
        Log.d("ViewModel", "Idioma de origem atualizado: $language")
    }

    // Atualiza idioma de destino manualmente (seleção do usuário).
    fun setTargetLanguage(language: String) {
        _targetLanguage.value = language
        Log.d("ViewModel", "Idioma de destino atualizado: $language")
    }

    // Inverte origem e destino (útil para o botão de swap).
    fun swapLanguages() {
        val temp = _sourceLanguage.value
        _sourceLanguage.value = _targetLanguage.value
        _targetLanguage.value = temp
        Log.d("ViewModel", "Idiomas invertidos: ${_sourceLanguage.value} <-> ${_targetLanguage.value}")
    }

    // Traduz o texto atual usando os idiomas atuais do estado.
    fun translateText() {
        viewModelScope.launch {
            // Estado de carregamento e limpeza de erro anterior.
            _isLoading.value = true
            _error.value = null

            try {
                // Captura estado atual para traduzir.
                val textToTranslate = _extractedText.value
                val sourceLang = _sourceLanguage.value
                val targetLang = _targetLanguage.value

                Log.d("ViewModel", "Traduzindo de '$sourceLang' para '$targetLang'")
                Log.d("ViewModel", "Texto: '$textToTranslate'")

                // Validação básica: não traduz texto vazio.
                if (textToTranslate.isEmpty()) {
                    _error.value = "Nenhum texto para traduzir"
                    return@launch
                }

                // Chama o UseCase de tradução (provavelmente faz requisição à API).
                val result = translateTextUseCase(textToTranslate, sourceLang, targetLang)

                // Atualiza estado de saída com o texto traduzido.
                _translatedText.value = result.translatedText
                Log.d("ViewModel", "Tradução concluída: '${result.translatedText}'")

            } catch (e: Exception) {
                // Propaga erro para UI e registra stacktrace no log.
                _error.value = "Erro ao traduzir: ${e.message}"
                Log.e("ViewModel", "Erro: ${e.message}", e)
            } finally {
                // Finaliza estado de carregamento.
                _isLoading.value = false
            }
        }
    }

    private fun getLanguageCode(languageName: String): String {
        // Mapeia nomes comuns (em PT e EN) e abreviações para códigos de idioma.
        // Esse código pode ser usado por API/serviço de tradução/detecção.
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
            // Padrão: inglês quando não reconhece o nome informado.
            else -> "en"
        }
    }
}
