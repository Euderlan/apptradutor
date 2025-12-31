package com.example.texttranslatorapp.presentation.utils

// Interface que define o contrato para exibição do seletor de idiomas.
// Permite abstrair a implementação concreta do diálogo.
interface LanguageDialogManager {

    // Exibe o seletor de idiomas.
    // isSource indica se o idioma selecionado será o de origem ou o de destino.
    fun showLanguageSelector(isSource: Boolean)
}
