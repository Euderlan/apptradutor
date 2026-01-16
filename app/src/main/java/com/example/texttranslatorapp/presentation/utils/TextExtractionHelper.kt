package com.example.texttranslatorapp.presentation.utils

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.example.texttranslatorapp.data.datasource.MLKitTextExtractorMultilingual
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
/**
 * Responsável por extrair texto de uma região específica da imagem
 * a partir de um retângulo de seleção definido pelo usuário.
 */
class TextExtractionHelper(
    private val textExtractor: MLKitTextExtractorMultilingual
) {

    /**
     * Extrai texto de uma área específica da imagem
     * @param imagemOriginal Bitmap da imagem completa
     * @param selectionRect Retângulo da área selecionada (em coordenadas do BITMAP)
     * @return Texto extraído da área selecionada
     */
    suspend fun extrairTextoDaSelecao(
        imagemOriginal: Bitmap,
        selectionRect: Rect
    ): String {
        return withContext(Dispatchers.Default) {
            try {
                Log.d("TextExtractionHelper", "==================================================")
                Log.d("TextExtractionHelper", "INICIANDO EXTRAÇÃO DE CROP")
                Log.d("TextExtractionHelper", "==================================================")

                Log.d("TextExtractionHelper", "Bitmap original: ${imagemOriginal.width}x${imagemOriginal.height} pixels")
                Log.d("TextExtractionHelper", "Retângulo solicitado: $selectionRect")
                Log.d("TextExtractionHelper", "  left=${selectionRect.left}, top=${selectionRect.top}")
                Log.d("TextExtractionHelper", "  right=${selectionRect.right}, bottom=${selectionRect.bottom}")
                Log.d("TextExtractionHelper", "  width=${selectionRect.width()}, height=${selectionRect.height()}")

                if (selectionRect.width() <= 0 || selectionRect.height() <= 0) {
                    throw Exception(
                        "Dimensões inválidas do retângulo: " +
                                "width=${selectionRect.width()}, height=${selectionRect.height()}"
                    )
                }

                if (selectionRect.left < 0 || selectionRect.top < 0) {
                    throw Exception(
                        "Posição negativa do retângulo: " +
                                "left=${selectionRect.left}, top=${selectionRect.top}"
                    )
                }

                if (selectionRect.right > imagemOriginal.width ||
                    selectionRect.bottom > imagemOriginal.height) {
                    throw Exception(
                        "Retângulo ultrapassa limites do bitmap: " +
                                "Bitmap=${imagemOriginal.width}x${imagemOriginal.height}, " +
                                "Rect right=${selectionRect.right}, bottom=${selectionRect.bottom}"
                    )
                }

                Log.d("TextExtractionHelper", "Criando bitmap cortado...")
                val imagemCortada = try {
                    Bitmap.createBitmap(
                        imagemOriginal,
                        selectionRect.left.coerceAtLeast(0),
                        selectionRect.top.coerceAtLeast(0),
                        selectionRect.width().coerceAtMost(imagemOriginal.width - selectionRect.left),
                        selectionRect.height().coerceAtMost(imagemOriginal.height - selectionRect.top)
                    )
                } catch (e: Exception) {
                    Log.e("TextExtractionHelper", "Erro ao criar bitmap cortado", e)
                    throw Exception("Falha ao criar bitmap cortado: ${e.message}")
                }

                Log.d("TextExtractionHelper", "Bitmap cortado criado: ${imagemCortada.width}x${imagemCortada.height} pixels")
                Log.d("TextExtractionHelper", "Iniciando extração de texto...")
                val inicioExtracao = System.currentTimeMillis()

                val texto = textExtractor.extractTextAuto(imagemCortada)

                val tempoExtracao = System.currentTimeMillis() - inicioExtracao
                Log.d("TextExtractionHelper", "Extração concluída em ${tempoExtracao}ms")

                if (texto.isEmpty()) {
                    Log.w("TextExtractionHelper", "Nenhum texto detectado na área selecionada")
                } else {
                    Log.d("TextExtractionHelper", "Texto extraído: '${texto.take(100)}'")
                    Log.d("TextExtractionHelper", "Total: ${texto.length} caracteres")
                    Log.d("TextExtractionHelper", "Linhas: ${texto.split("\n").size}")
                }

                Log.d("TextExtractionHelper", "==================================================")

                texto

            } catch (e: Exception) {
                Log.e("TextExtractionHelper", "ERRO CRÍTICO NA EXTRAÇÃO", e)
                e.printStackTrace()
                throw Exception("Erro ao extrair texto da seleção: ${e.message}")
            }
        }
    }
}