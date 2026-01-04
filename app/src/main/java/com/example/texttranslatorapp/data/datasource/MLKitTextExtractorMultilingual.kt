package com.example.texttranslatorapp.data.datasource

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/**
 * Extrator de texto com suporte a múltiplos idiomas (Latin, Japonês, Chinês, Coreano, Devanagari)
 *
 * Usa ML Kit com detecção automática do script para extrair texto de forma multilíngue
 */
class MLKitTextExtractorMultilingual {

    /**
     * Extração automática: tenta cada modelo até encontrar texto
     * Ordem de tentativa: Latin → Japonês → Chinês → Coreano → Devanagari
     */
    suspend fun extractTextAuto(bitmap: Bitmap): String {
        return try {
            // 1. Tenta Latin primeiro (mais rápido e comum)
            val latinText = extractTextLatin(bitmap)
            if (latinText.isNotEmpty()) {
                Log.d("TextExtractor", "Texto latino encontrado")
                return latinText
            }

            // 2. Tenta Japonês
            val japaneseText = extractTextJapanese(bitmap)
            if (japaneseText.isNotEmpty()) {
                Log.d("TextExtractor", "Texto japonês encontrado")
                return japaneseText
            }

            // 3. Tenta Chinês
            val chineseText = extractTextChinese(bitmap)
            if (chineseText.isNotEmpty()) {
                Log.d("TextExtractor", "Texto chinês encontrado")
                return chineseText
            }

            // 4. Tenta Coreano
            val koreanText = extractTextKorean(bitmap)
            if (koreanText.isNotEmpty()) {
                Log.d("TextExtractor", "Texto coreano encontrado")
                return koreanText
            }

            // 5. Tenta Devanagari (Hindi, Sânscrito)
            val devanagariText = extractTextDevanagari(bitmap)
            if (devanagariText.isNotEmpty()) {
                Log.d("TextExtractor", "Texto Devanagari encontrado")
                return devanagariText
            }

            // Nenhum modelo achou texto
            Log.w("TextExtractor", "Nenhum texto encontrado em nenhum idioma")
            ""

        } catch (e: Exception) {
            Log.e("TextExtractor", "Erro na extração automática: ${e.message}")
            throw Exception("Erro ao extrair texto: ${e.message}")
        }
    }

    /**
     * Extrai texto Latin (Português, Inglês, Espanhol, Francês, Alemão, Italiano, Russo)
     */
    suspend fun extractTextLatin(bitmap: Bitmap): String {
        return try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(image).await()
            recognizer.close()

            val text = result.text.trim()
            Log.d("TextExtractor-Latin", "Extraído: ${text.length} caracteres")
            text
        } catch (e: Exception) {
            Log.d("TextExtractor-Latin", "Falha na extração: ${e.message}")
            ""
        }
    }

    /**
     * Extrai texto Japonês (Hiragana, Katakana, Kanji)
     */
    suspend fun extractTextJapanese(bitmap: Bitmap): String {
        return try {
            val options = JapaneseTextRecognizerOptions.Builder().build()
            val recognizer = TextRecognition.getClient(options)
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(image).await()
            recognizer.close()

            val text = result.text.trim()
            Log.d("TextExtractor-Japanese", "Extraído: ${text.length} caracteres")
            text
        } catch (e: Exception) {
            Log.d("TextExtractor-Japanese", "Falha na extração: ${e.message}")
            ""
        }
    }

    /**
     * Extrai texto Chinês (Simplificado e Tradicional)
     */
    suspend fun extractTextChinese(bitmap: Bitmap): String {
        return try {
            val options = ChineseTextRecognizerOptions.Builder().build()
            val recognizer = TextRecognition.getClient(options)
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(image).await()
            recognizer.close()

            val text = result.text.trim()
            Log.d("TextExtractor-Chinese", "Extraído: ${text.length} caracteres")
            text
        } catch (e: Exception) {
            Log.d("TextExtractor-Chinese", "Falha na extração: ${e.message}")
            ""
        }
    }

    /**
     * Extrai texto Coreano (Hangul)
     */
    suspend fun extractTextKorean(bitmap: Bitmap): String {
        return try {
            val options = KoreanTextRecognizerOptions.Builder().build()
            val recognizer = TextRecognition.getClient(options)
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(image).await()
            recognizer.close()

            val text = result.text.trim()
            Log.d("TextExtractor-Korean", "Extraído: ${text.length} caracteres")
            text
        } catch (e: Exception) {
            Log.d("TextExtractor-Korean", "Falha na extração: ${e.message}")
            ""
        }
    }

    /**
     * Extrai texto Devanagari (Hindi, Sânscrito, Marathi, Nepali)
     */
    suspend fun extractTextDevanagari(bitmap: Bitmap): String {
        return try {
            val options = DevanagariTextRecognizerOptions.Builder().build()
            val recognizer = TextRecognition.getClient(options)
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(image).await()
            recognizer.close()

            val text = result.text.trim()
            Log.d("TextExtractor-Devanagari", "Extraído: ${text.length} caracteres")
            text
        } catch (e: Exception) {
            Log.d("TextExtractor-Devanagari", "Falha na extração: ${e.message}")
            ""
        }
    }

    /**
     * Extrai texto com modelo específico baseado no código do idioma
     * Útil quando já sabemos qual idioma é
     */
    suspend fun extractTextByLanguage(bitmap: Bitmap, languageCode: String): String {
        val code = languageCode.lowercase()
        return when {
            code.contains("ja") -> extractTextJapanese(bitmap)
            code.contains("zh") -> extractTextChinese(bitmap)
            code.contains("ko") -> extractTextKorean(bitmap)
            code.contains("hi") || code.contains("sa") || code.contains("mr") -> extractTextDevanagari(bitmap)
            else -> extractTextLatin(bitmap)
        }
    }
}