package com.example.texttranslatorapp.data.datasource

import com.example.texttranslatorapp.domain.models.TranslationResult
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// ========== RETROFIT INTERFACE ==========
interface LibreTranslateAPI {
    @GET("translate")
    fun translate(
        @Query("q") text: String,
        @Query("source") source: String,
        @Query("target") target: String
    ): Call<TranslationResponse>
}

// ========== MODELOS GSON ==========
data class TranslationResponse(
    @SerializedName("translatedText")
    val translatedText: String
)

// ========== TRANSLATION API SERVICE ==========
class TranslationApiService {

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://libretranslate.de/")
        .addConverterFactory(GsonConverterFactory.create(com.google.gson.Gson()))
        .client(okHttpClient())
        .build()

    private fun okHttpClient(): okhttp3.OkHttpClient {
        return okhttp3.OkHttpClient.Builder()
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    private val api = retrofit.create(LibreTranslateAPI::class.java)

    suspend fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): TranslationResult {
        return try {
            val sourceLang = convertLanguageCode(sourceLanguage)
            val targetLang = convertLanguageCode(targetLanguage)

            // Chamar API de forma suspensa usando Coroutine
            val translatedText = callAPI(text, sourceLang, targetLang)

            TranslationResult(
                originalText = text,
                translatedText = translatedText,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage
            )
        } catch (e: Exception) {
            throw Exception("Erro ao traduzir: ${e.message}")
        }
    }

    // Converter Call do Retrofit para Coroutine suspend
    private suspend fun callAPI(text: String, source: String, target: String): String {
        return suspendCancellableCoroutine { continuation ->
            api.translate(text, source, target).enqueue(object : Callback<TranslationResponse> {
                override fun onResponse(call: Call<TranslationResponse>, response: Response<TranslationResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        continuation.resume(response.body()!!.translatedText)
                    } else {
                        continuation.resumeWithException(
                            Exception("Erro na API: ${response.code()}")
                        )
                    }
                }

                override fun onFailure(call: Call<TranslationResponse>, t: Throwable) {
                    continuation.resumeWithException(t)
                }
            })
        }
    }

    private fun convertLanguageCode(language: String): String {
        return when (language.lowercase().trim()) {
            "português", "pt" -> "pt"
            "inglês", "en", "english" -> "en"
            "espanhol", "es", "spanish" -> "es"
            "francês", "fr", "french" -> "fr"
            "alemão", "de", "german" -> "de"
            "italiano", "it", "italian" -> "it"
            "japonês", "ja", "japanese" -> "ja"
            "chinês", "zh", "chinese" -> "zh"
            "russo", "ru", "russian" -> "ru"
            else -> "en"
        }
    }
}