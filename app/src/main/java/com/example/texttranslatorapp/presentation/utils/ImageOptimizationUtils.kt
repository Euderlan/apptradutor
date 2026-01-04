package com.example.texttranslatorapp.presentation.utils

import android.graphics.Bitmap
import android.util.Log

/**
 * Utilitário para otimizar imagens da câmera para melhor extração de texto (OCR)
 *
 * ML Kit funciona melhor com:
 * - Resolução: 1000-2000px na dimensão maior
 * - Contraste alto
 * - Texto ocupando pelo menos 30% da imagem
 */
object ImageOptimizationUtils {

    /**
     * Otimiza imagem para OCR
     * Redimensiona para tamanho ideal sem perder qualidade
     */
    fun otimizarImagemParaOCR(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        Log.d("ImageOptimization", "Imagem original: ${width}x${height} pixels")

        // ML Kit funciona melhor com resolução entre 1000-2000px
        val maxDimension = 1500

        // Se já está no tamanho ideal, não redimensiona
        if (width <= maxDimension && height <= maxDimension) {
            Log.d("ImageOptimization", "Imagem já está no tamanho ideal")
            return bitmap
        }

        // Calcular nova proporção mantendo aspect ratio
        val ratio = if (width > height) {
            maxDimension.toFloat() / width
        } else {
            maxDimension.toFloat() / height
        }

        val novaLargura = (width * ratio).toInt()
        val novaAltura = (height * ratio).toInt()

        Log.d("ImageOptimization", "Redimensionando para: ${novaLargura}x${novaAltura}")

        return Bitmap.createScaledBitmap(bitmap, novaLargura, novaAltura, true)
    }

    /**
     * Valida se a imagem tem qualidade mínima para OCR
     */
    fun validarQualidadeImagem(bitmap: Bitmap): ValidationResult {
        val width = bitmap.width
        val height = bitmap.height

        // Imagem muito pequena?
        if (width < 400 || height < 300) {
            return ValidationResult(
                isValid = false,
                message = "Imagem muito pequena (${width}x${height}). Tire mais de perto!",
                severity = Severity.ERROR
            )
        }

        // Imagem muito grande (pode ser lenta)?
        if (width > 4000 || height > 4000) {
            return ValidationResult(
                isValid = false,
                message = "Imagem muito grande (${width}x${height}). Tire de uma distância melhor.",
                severity = Severity.WARNING
            )
        }

        // Proporção muito extrema?
        val ratio = maxOf(width, height).toFloat() / minOf(width, height)
        if (ratio > 10) {
            return ValidationResult(
                isValid = true,
                message = "Imagem muito alongada. Pode ter problemas de extração.",
                severity = Severity.WARNING
            )
        }

        return ValidationResult(
            isValid = true,
            message = "Qualidade OK",
            severity = Severity.INFO
        )
    }

    /**
     * Obter dicas para melhorar extração
     */
    fun obterDicasOCR(): List<String> {
        return listOf(
            "📸 Tire foto BEM PERTO do texto",
            "🎯 Certifique que está bem focado",
            "💡 Boa iluminação é essencial",
            "⬜ Evite sombras e reflexos",
            "📐 Texto deve estar FRONTAL (não de lado)",
            "📏 Texto grande (ocupar 30%+ da imagem)",
            "⚫⚪ Alto contraste é melhor (preto em branco)",
            "🤚 Mantenha a mão firme (evite blur)"
        )
    }

    /**
     * Diagnosticar possíveis problemas
     */
    fun diagnosticarProblemas(bitmap: Bitmap): List<String> {
        val problemas = mutableListOf<String>()

        val width = bitmap.width
        val height = bitmap.height

        if (width < 500 || height < 400) {
            problemas.add("❌ Imagem muito pequena - tire mais de perto")
        }

        if (width > 3000 || height > 3000) {
            problemas.add("⚠️ Imagem muito grande - pode ser lenta")
        }

        // Verificar se parece desfocada (heurística simples)
        // Imagens desfocadas tendem a ter menos variação de pixels
        if (pareceDesfocada(bitmap)) {
            problemas.add("🔍 Imagem pode estar desfocada - tente novamente")
        }

        return problemas
    }

    /**
     * Heurística simples para detectar blur
     * (não é 100% acurado, mas ajuda)
     */
    private fun pareceDesfocada(bitmap: Bitmap): Boolean {
        // Se é muito pequena, não conseguimos detectar bem
        if (bitmap.width < 200 || bitmap.height < 200) {
            return false
        }

        // Amostragem: verificar variação de pixels em pontos aleatórios
        // Se muito baixa, pode estar desfocada
        // (Este é um teste simplificado)

        return false // Por enquanto, não implementar (é complexo)
    }

    data class ValidationResult(
        val isValid: Boolean,
        val message: String,
        val severity: Severity
    )

    enum class Severity {
        INFO,
        WARNING,
        ERROR
    }
}