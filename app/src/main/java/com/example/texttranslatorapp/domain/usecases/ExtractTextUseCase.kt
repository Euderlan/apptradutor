package com.example.texttranslatorapp.domain.usecases

import com.example.texttranslatorapp.data.repository.TextExtractionRepository
import android.graphics.Bitmap

// Caso de uso responsável pela extração de texto a partir de imagem
class ExtractTextUseCase(
    private val repository: TextExtractionRepository
) {

    // Executa a extração de texto (OCR) a partir de um Bitmap
    suspend operator fun invoke(bitmap: Bitmap): String {
        return repository.extractText(bitmap)
    }
}
