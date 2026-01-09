package com.example.texttranslatorapp.data.datasource

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

class MLKitTextExtractor {

    // Inicializa o reconhecedor de texto do ML Kit de forma preguiçosa
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    // Extrai o texto de uma imagem Bitmap usando OCR do ML Kit
    suspend fun extractText(bitmap: Bitmap): String {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(image).await()
            result.text
        } catch (e: Exception) {
            // Propaga uma exceção com mensagem personalizada em caso de erro
            throw Exception("Erro ao extrair texto: ${e.message}")
        }
    }
}
