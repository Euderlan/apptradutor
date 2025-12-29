package com.example.texttranslatorapp.domain.usecases

import com.example.texttranslatorapp.data.repository.TextExtractionRepository
import android.graphics.Bitmap

class ExtractTextUseCase(
    private val repository: TextExtractionRepository
) {
    suspend operator fun invoke(bitmap: Bitmap): String {
        return repository.extractText(bitmap)
    }
}