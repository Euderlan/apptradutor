package com.example.texttranslatorapp.presentation.utils

/**
 * Utilitário para decodificar Unicode escapado em respostas de tradução
 *
 * Exemplo:
 * Input:  "\u4eba\u751f\u306f\u5947\u8de1\u3067\u3059"
 * Output: "人生は奇跡です"
 */
object UnicodeDecoder {

    /**
     * Decodifica Unicode escapado em string
     * @param encoded String com Unicode escapado (ex: \u1234)
     * @return String decodificada
     */
    fun decode(encoded: String): String {
        return try {
            // Primeiro, substitui \u por seu valor Unicode real
            var result = encoded

            // Pattern para encontrar \uXXXX
            val pattern = Regex("\\\\u([0-9a-fA-F]{4})")

            result = pattern.replace(result) { matchResult ->
                val hex = matchResult.groupValues[1]
                val codePoint = hex.toInt(16)
                codePoint.toChar().toString()
            }

            result
        } catch (e: Exception) {
            // Se falhar, retorna original
            encoded
        }
    }

    /**
     * Verifica se a string contém Unicode escapado
     */
    fun hasEscapedUnicode(text: String): Boolean {
        return text.contains(Regex("\\\\u[0-9a-fA-F]{4}"))
    }
}