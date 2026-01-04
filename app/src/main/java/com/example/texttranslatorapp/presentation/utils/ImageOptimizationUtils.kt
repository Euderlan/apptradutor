package com.example.texttranslatorapp.presentation.utils

import android.graphics.Bitmap
import android.util.Log

object ImageOptimizationUtils {

    fun otimizarImagemParaOCR(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        Log.d("ImageOptimization", "Imagem original: ${width}x${height} pixels")

        val maxDimension = 2048

        if (width <= maxDimension && height <= maxDimension) {
            Log.d("ImageOptimization", "Imagem já está no tamanho ideal")
            return bitmap
        }

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

    fun validarQualidadeImagem(bitmap: Bitmap): ValidationResult {
        val width = bitmap.width
        val height = bitmap.height

        if (width < 300 || height < 300) {
            return ValidationResult(
                isValid = false,
                message = "Imagem muito pequena (${width}x${height}). Tire mais de perto!",
                severity = Severity.ERROR
            )
        }

        if (width > 5000 || height > 5000) {
            return ValidationResult(
                isValid = false,
                message = "Imagem muito grande (${width}x${height}). Tire de uma distância melhor.",
                severity = Severity.WARNING
            )
        }

        val ratio = maxOf(width, height).toFloat() / minOf(width, height)
        if (ratio > 15) {
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

    fun obterDicasOCR(): List<String> {
        return listOf(
            "Tire foto BEM PERTO do texto",
            "Certifique que está bem focado",
            "Boa iluminação é essencial",
            "Evite sombras e reflexos",
            "Texto deve estar FRONTAL (não de lado)",
            "Texto grande (ocupar 30%+ da imagem)",
            "Alto contraste é melhor (preto em branco)",
            "Mantenha a mão firme (evite blur)"
        )
    }

    fun diagnosticarProblemas(bitmap: Bitmap): List<String> {
        val problemas = mutableListOf<String>()

        val width = bitmap.width
        val height = bitmap.height

        if (width < 500 || height < 400) {
            problemas.add("Imagem muito pequena - tire mais de perto")
        }

        if (width > 3000 || height > 3000) {
            problemas.add("Imagem muito grande - pode ser lenta")
        }

        return problemas
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