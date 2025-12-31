package com.example.texttranslatorapp.presentation.utils

import android.content.Context
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.texttranslatorapp.presentation.viewmodel.TranslatorViewModel

class LanguageDialogManagerImpl(
    private val context: Context,
    // ViewModel usado para atualizar o estado global de idiomas
    private val viewModel: TranslatorViewModel,
    // TextViews da UI que exibem os idiomas selecionados
    private val textSourceLanguage: TextView,
    private val textTargetLanguage: TextView
) : LanguageDialogManager {

    // Lista de idiomas disponíveis para seleção no diálogo
    private val IDIOMAS = arrayOf(
        "Português", "Inglês", "Espanhol", "Francês",
        "Alemão", "Italiano", "Japonês", "Chinês", "Russo"
    )

    // Mapeamento nome do idioma -> código do idioma
    private val LANGUAGE_CODES = mapOf(
        "Português" to "pt",
        "Inglês" to "en",
        "Espanhol" to "es",
        "Francês" to "fr",
        "Alemão" to "de",
        "Italiano" to "it",
        "Japonês" to "ja",
        "Chinês" to "zh",
        "Russo" to "ru"
    )

    override fun showLanguageSelector(isSource: Boolean) {
        // Define título e idioma atual com base se é origem ou destino
        val titulo = if (isSource) "Selecionar idioma de origem" else "Selecionar idioma de destino"
        val idiomaAtual =
            if (isSource) textSourceLanguage.text.toString()
            else textTargetLanguage.text.toString()

        // Marca no diálogo o idioma atualmente selecionado
        val posicaoAtual = IDIOMAS.indexOf(idiomaAtual)

        AlertDialog.Builder(context)
            .setTitle(titulo)
            .setSingleChoiceItems(IDIOMAS, posicaoAtual) { dialog, which ->
                val idiomaSelecionado = IDIOMAS[which]
                val codigoIdioma = LANGUAGE_CODES[idiomaSelecionado] ?: "pt"

                // Atualiza ViewModel e UI conforme o tipo de idioma selecionado
                if (isSource) {
                    viewModel.setSourceLanguage(idiomaSelecionado)
                    textSourceLanguage.text = idiomaSelecionado
                } else {
                    viewModel.setTargetLanguage(idiomaSelecionado)
                    textTargetLanguage.text = idiomaSelecionado
                }

                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
