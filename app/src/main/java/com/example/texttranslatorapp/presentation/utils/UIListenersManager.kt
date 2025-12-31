package com.example.texttranslatorapp.presentation.utils

import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.texttranslatorapp.presentation.viewmodel.TranslatorViewModel

class UIListenersManager(
    private val activity: AppCompatActivity,
    // ViewModel para acionar ações de negócio a partir da UI
    private val viewModel: TranslatorViewModel,
    // Manager responsável pela seleção de idiomas
    private val languageDialogManager: LanguageDialogManager
) {

    fun setupCaptureButtons(
        btnCapturar: Button,
        btnGaleria: Button,
        onCameraClick: () -> Unit,
        onGalleryClick: () -> Unit
    ) {
        // Delegação direta dos cliques para callbacks externos
        btnCapturar.setOnClickListener { onCameraClick() }
        btnGaleria.setOnClickListener { onGalleryClick() }
    }

    fun setupLanguageContainers(
        containerSourceLanguage: FrameLayout,
        containerTargetLanguage: FrameLayout
    ) {
        // Abre seletor de idioma de origem
        containerSourceLanguage.setOnClickListener {
            languageDialogManager.showLanguageSelector(isSource = true)
        }

        // Abre seletor de idioma de destino
        containerTargetLanguage.setOnClickListener {
            languageDialogManager.showLanguageSelector(isSource = false)
        }
    }

    fun setupSwapButton(btnSwapLanguages: Button) {
        // Inverte idiomas de origem e destino
        btnSwapLanguages.setOnClickListener {
            viewModel.swapLanguages()
            Toast.makeText(activity, "Idiomas invertidos", Toast.LENGTH_SHORT).show()
        }
    }

    fun setupTranslateButton(
        btntraduzir: Button,
        textoExtraido: EditText
    ) {
        btntraduzir.setOnClickListener {
            val textToTranslate = textoExtraido.text.toString()

            // Validação básica antes de solicitar tradução
            if (textToTranslate.isEmpty()) {
                Toast.makeText(
                    activity,
                    "Por favor, digite ou selecione um texto para traduzir",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                viewModel.updateExtractedText(textToTranslate)
                viewModel.translateText()
            }
        }
    }
}
