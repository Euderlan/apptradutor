package com.example.texttranslatorapp.presentation

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.texttranslatorapp.R
import com.example.texttranslatorapp.data.datasource.MLKitLanguageDetector
import com.example.texttranslatorapp.data.datasource.MLKitTextExtractor
import com.example.texttranslatorapp.data.datasource.TranslationApiService
import com.example.texttranslatorapp.data.repository.LanguageDetectionRepository
import com.example.texttranslatorapp.data.repository.TextExtractionRepository
import com.example.texttranslatorapp.data.repository.TranslationRepository
import com.example.texttranslatorapp.domain.usecases.DetectLanguageUseCase
import com.example.texttranslatorapp.domain.usecases.ExtractTextUseCase
import com.example.texttranslatorapp.domain.usecases.TranslateTextUseCase
import com.example.texttranslatorapp.presentation.viewmodel.SharedImageViewModel
import com.example.texttranslatorapp.presentation.viewmodel.TranslatorViewModel
import kotlinx.coroutines.launch

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var imgCapturada: ImageView
    private lateinit var editTextSelecionado: EditText
    private lateinit var textDetectado: TextView
    private lateinit var btnContinuar: Button
    private lateinit var btnCancelar: Button
    private lateinit var btnExtrairTudo: Button
    private lateinit var loadingText: TextView

    private lateinit var viewModel: TranslatorViewModel
    private var imagemBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        initializeViews()
        initializeViewModel()
        setupListeners()
        observeViewModel()

        // Receber a imagem do SharedImageViewModel (sem passar pelo Intent)
        imagemBitmap = SharedImageViewModel.imagemCompartilhada
        imagemBitmap?.let {
            imgCapturada.setImageBitmap(it)
            // Iniciar extração de texto automaticamente
            viewModel.processImage(it)
        } ?: run {
            Toast.makeText(this, "Erro: Imagem não recebida", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initializeViews() {
        imgCapturada = findViewById(R.id.imgCapturada)
        editTextSelecionado = findViewById(R.id.editTextSelecionado)
        textDetectado = findViewById(R.id.textDetectado)
        btnContinuar = findViewById(R.id.btnContinuar)
        btnCancelar = findViewById(R.id.btnCancelar)
        btnExtrairTudo = findViewById(R.id.btnExtrairTudo)
        loadingText = findViewById(R.id.loadingText)
    }

    private fun initializeViewModel() {
        val textExtractor = MLKitTextExtractor()
        val languageDetector = MLKitLanguageDetector()
        val translationService = TranslationApiService()

        val textExtractionRepo = TextExtractionRepository(textExtractor)
        val languageDetectionRepo = LanguageDetectionRepository(languageDetector)
        val translationRepo = TranslationRepository(translationService)

        val extractTextUC = ExtractTextUseCase(textExtractionRepo)
        val detectLanguageUC = DetectLanguageUseCase(languageDetectionRepo)
        val translateTextUC = TranslateTextUseCase(translationRepo)

        viewModel = TranslatorViewModel(extractTextUC, detectLanguageUC, translateTextUC)
    }

    private fun setupListeners() {
        btnExtrairTudo.setOnClickListener {
            // Preenche o EditText com todo o texto extraído
            lifecycleScope.launch {
                viewModel.extractedText.collect { text ->
                    editTextSelecionado.setText(text)
                }
            }
        }

        btnContinuar.setOnClickListener {
            val textoSelecionado = editTextSelecionado.text.toString()
            if (textoSelecionado.isEmpty()) {
                Toast.makeText(this, "Por favor, selecione algum texto", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Passar apenas as strings de volta, não o Bitmap
            val intent = Intent().apply {
                putExtra("texto_selecionado", textoSelecionado)
                putExtra("idioma_detectado", viewModel.detectedLanguage.value?.detectedLanguage ?: "Inglês")
            }
            setResult(RESULT_OK, intent)
            finish()
        }

        btnCancelar.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.extractedText.collect { text ->
                if (text.isNotEmpty()) {
                    loadingText.text = "Texto extraído com sucesso"
                }
            }
        }

        lifecycleScope.launch {
            viewModel.detectedLanguage.collect { detection ->
                detection?.let {
                    textDetectado.text = "Idioma detectado: ${it.detectedLanguage}"
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                loadingText.text = if (isLoading) "Extraindo texto..." else "Pronto"
                btnContinuar.isEnabled = !isLoading
                btnExtrairTudo.isEnabled = !isLoading
            }
        }

        lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    Toast.makeText(this@ImageViewerActivity, "Erro: $it", Toast.LENGTH_LONG).show()
                    loadingText.text = "Erro ao extrair"
                }
            }
        }
    }
}