package com.example.texttranslatorapp.presentation.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel

/**
 * ViewModel compartilhado usado para manter uma imagem em memória
 * e permitir o acesso entre diferentes telas (Activities).
 *
 * Evita a necessidade de serializar o Bitmap via Intent,
 * o que poderia causar erros de limite de tamanho ou desempenho.
 */
object SharedImageViewModel : ViewModel() {

    /**
     * Bitmap temporário compartilhado entre telas.
     *
     * Deve ser limpo após o uso para evitar vazamento de memória.
     */
    var imagemCompartilhada: Bitmap? = null
}
