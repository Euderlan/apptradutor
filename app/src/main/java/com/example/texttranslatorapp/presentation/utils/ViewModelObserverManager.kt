package com.example.texttranslatorapp.presentation.utils

import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.texttranslatorapp.presentation.viewmodel.TranslatorViewModel
import kotlinx.coroutines.launch

/**
 * Responsável por observar os estados expostos pelo ViewModel e refletir
 * automaticamente essas mudanças na interface do usuário.
 *
 * Essa classe conecta os Fluxos do ViewModel (texto extraído, idiomas,
 * tradução, loading e erros) aos componentes de UI, mantendo a Activity
 * livre de lógica de observação e sincronização.
 */
class ViewModelObserverManager(
    private val activity: AppCompatActivity,
    private val viewModel: TranslatorViewModel,
    private val textWatcherManager: TextWatcherDebounceManager
) {

    fun observeExtractedText(
        textoExtraido: EditText,
        textSourceLanguage: TextView
    ) {
        activity.lifecycleScope.launch {
            viewModel.extractedText.collect { text ->
                // Evita que o TextWatcher dispare quando o texto é atualizado programaticamente
                textWatcherManager.setUpdatingUI(true)
                textoExtraido.setText(text)
                textoExtraido.setSelection(text.length)
                textWatcherManager.setUpdatingUI(false)
            }
        }
    }

    fun observeDetectedLanguage(textDetectedLanguage: TextView) {
        activity.lifecycleScope.launch {
            viewModel.detectedLanguage.collect { detection ->
                // Atualiza a UI quando houver resultado de detecção
                detection?.let {
                    textDetectedLanguage.text = "Idioma detectado: ${it.detectedLanguage}"
                }
            }
        }
    }

    fun observeSourceLanguage(textSourceLanguage: TextView) {
        activity.lifecycleScope.launch {
            viewModel.sourceLanguage.collect { sourceLang ->
                // Evita setText redundante
                if (textSourceLanguage.text != sourceLang) {
                    textSourceLanguage.text = sourceLang
                }
            }
        }
    }

    fun observeTargetLanguage(textTargetLanguage: TextView) {
        activity.lifecycleScope.launch {
            viewModel.targetLanguage.collect { targetLang ->
                // Evita setText redundante
                if (textTargetLanguage.text != targetLang) {
                    textTargetLanguage.text = targetLang
                }
            }
        }
    }

    fun observeTranslatedText(textoTraduzido: EditText) {
        activity.lifecycleScope.launch {
            viewModel.translatedText.collect { text ->
                // Mostra o resultado da tradução
                textoTraduzido.setText(text)
            }
        }
    }

    fun observeLoadingState(
        btnCapturar: Button,
        btnGaleria: Button,
        btntraduzir: Button,
        btnSwapLanguages: Button,
        containerSourceLanguage: FrameLayout,
        containerTargetLanguage: FrameLayout
    ) {
        activity.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                // Bloqueia interações durante operações (OCR/detecção/tradução)
                btnCapturar.isEnabled = !isLoading
                btnGaleria.isEnabled = !isLoading
                btntraduzir.isEnabled = !isLoading
                btnSwapLanguages.isEnabled = !isLoading
                containerSourceLanguage.isEnabled = !isLoading
                containerTargetLanguage.isEnabled = !isLoading
            }
        }
    }

    fun observeErrors() {
        activity.lifecycleScope.launch {
            viewModel.error.collect { error ->
                // Exibe mensagens de erro emitidas pelo ViewModel
                error?.let {
                    Toast.makeText(activity, it, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
