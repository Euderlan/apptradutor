package com.example.texttranslatorapp.presentation

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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
import com.example.texttranslatorapp.util.ImageProcessor
import com.example.texttranslatorapp.util.PermissionManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var btnCapturar: Button
    private lateinit var btnGaleria: Button
    private lateinit var btntraduzir: Button
    private lateinit var textoExtraido: EditText
    private lateinit var textoTraduzido: EditText
    private lateinit var textSourceLanguage: TextView
    private lateinit var textTargetLanguage: TextView
    private lateinit var btnSwapLanguages: Button
    private lateinit var containerSourceLanguage: FrameLayout
    private lateinit var containerTargetLanguage: FrameLayout
    private lateinit var textDetectedLanguage: TextView

    private val REQUEST_CAMERA = 1
    private val REQUEST_GALERIA = 2
    private val REQUEST_IMAGE_VIEWER = 3
    private val PERMISSION_CAMERA = 101
    private val PERMISSION_GALERIA = 102

    private val IDIOMAS = arrayOf(
        "Português", "Inglês", "Espanhol", "Francês",
        "Alemão", "Italiano", "Japonês", "Chinês", "Russo"
    )

    private lateinit var permissionManager: PermissionManager
    private lateinit var imageProcessor: ImageProcessor
    private lateinit var viewModel: TranslatorViewModel

    private var isUpdatingUI = false

    private val handler = Handler(Looper.getMainLooper())
    private var textWatcherRunnable: Runnable? = null
    private val DEBOUNCE_DELAY = 500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        initializeManagers()
        initializeViewModel()
        setupListeners()
        observeViewModel()
        setupTextWatcher()
    }

    private fun initializeViews() {
        btnCapturar = findViewById(R.id.btnCapturar)
        btnGaleria = findViewById(R.id.btnGaleria)
        textoExtraido = findViewById(R.id.textoExtraido)
        textoTraduzido = findViewById(R.id.textoTraduzido)
        textDetectedLanguage = findViewById(R.id.textDetectedLanguage)
        btntraduzir = findViewById(R.id.btntraduzir)
        textSourceLanguage = findViewById(R.id.textSourceLanguage)
        textTargetLanguage = findViewById(R.id.textTargetLanguage)
        btnSwapLanguages = findViewById(R.id.btnSwapLanguages)
        containerSourceLanguage = findViewById(R.id.containerSourceLanguage)
        containerTargetLanguage = findViewById(R.id.containerTargetLanguage)
    }

    private fun initializeManagers() {
        permissionManager = PermissionManager(this)
        imageProcessor = ImageProcessor()
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

        // Definir padrões iniciais
        textSourceLanguage.text = "Inglês"
        textTargetLanguage.text = "Português"
    }

    private fun setupListeners() {
        btnCapturar.setOnClickListener { pedirPermissaoCamera() }
        btnGaleria.setOnClickListener { pedirPermissaoGaleria() }

        // ✅ Listener para selecionar idioma de origem
        containerSourceLanguage.setOnClickListener {
            mostrarSeletorIdioma(isSource = true)
        }

        // ✅ Listener para selecionar idioma de destino
        containerTargetLanguage.setOnClickListener {
            mostrarSeletorIdioma(isSource = false)
        }

        // ✅ Botão para inverter idiomas
        btnSwapLanguages.setOnClickListener {
            viewModel.swapLanguages()
            Toast.makeText(this, "Idiomas invertidos", Toast.LENGTH_SHORT).show()
            android.util.Log.d("SWAP_LANGUAGE", "Idiomas invertidos pelo usuário")
        }

        // ✅ Botão traduzir
        btntraduzir.setOnClickListener {
            val textToTranslate = textoExtraido.text.toString()

            if (textToTranslate.isEmpty()) {
                Toast.makeText(this, "Por favor, digite ou selecione um texto para traduzir", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.updateExtractedText(textToTranslate)
                viewModel.translateText()
                android.util.Log.d("TRADUCAO_DEBUG", "Traduzindo de ${textSourceLanguage.text} para ${textTargetLanguage.text}")
            }
        }
    }

    // ✅ NOVO: Função para mostrar seletor de idiomas
    private fun mostrarSeletorIdioma(isSource: Boolean) {
        val titulo = if (isSource) "Selecionar idioma de origem" else "Selecionar idioma de destino"
        val idiomaAtual = if (isSource) textSourceLanguage.text.toString() else textTargetLanguage.text.toString()

        val posicaoAtual = IDIOMAS.indexOf(idiomaAtual)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(titulo)
            .setSingleChoiceItems(IDIOMAS, posicaoAtual) { dialog, which ->
                val idiomaSelecionado = IDIOMAS[which]

                if (isSource) {
                    viewModel.setSourceLanguage(idiomaSelecionado)
                    textSourceLanguage.text = idiomaSelecionado
                    android.util.Log.d("IDIOMA_DEBUG", "Idioma de origem: $idiomaSelecionado")
                } else {
                    viewModel.setTargetLanguage(idiomaSelecionado)
                    textTargetLanguage.text = idiomaSelecionado
                    android.util.Log.d("IDIOMA_DEBUG", "Idioma de destino: $idiomaSelecionado")
                }

                dialog.dismiss()
            }
            .show()
    }

    private fun setupTextWatcher() {
        textoExtraido.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (!isUpdatingUI && s != null) {
                    textWatcherRunnable?.let { handler.removeCallbacks(it) }

                    textWatcherRunnable = Runnable {
                        val textoAtual = s.toString()
                        viewModel.updateExtractedText(textoAtual)
                        android.util.Log.d("TEXT_WATCHER", "Texto editado: '$textoAtual'")
                    }

                    handler.postDelayed(textWatcherRunnable!!, DEBOUNCE_DELAY)
                }
            }
        })
    }

    private fun observeViewModel() {
        // Observar texto extraído
        lifecycleScope.launch {
            viewModel.extractedText.collect { text ->
                isUpdatingUI = true
                textoExtraido.setText(text)
                textoExtraido.setSelection(text.length)
                isUpdatingUI = false
            }
        }

        // Observar idioma detectado
        lifecycleScope.launch {
            viewModel.detectedLanguage.collect { detection ->
                detection?.let {
                    textDetectedLanguage.text = "Idioma detectado: ${it.detectedLanguage}"
                    android.util.Log.d("TRADUCAO_DEBUG", "Idioma detectado: ${it.detectedLanguage}")
                }
            }
        }

        // ✅ Observar idioma de origem
        lifecycleScope.launch {
            viewModel.sourceLanguage.collect { sourceLang ->
                if (textSourceLanguage.text != sourceLang) {
                    textSourceLanguage.text = sourceLang
                    android.util.Log.d("TRADUCAO_DEBUG", "SourceLanguage: $sourceLang")
                }
            }
        }

        // ✅ Observar idioma de destino
        lifecycleScope.launch {
            viewModel.targetLanguage.collect { targetLang ->
                if (textTargetLanguage.text != targetLang) {
                    textTargetLanguage.text = targetLang
                    android.util.Log.d("TRADUCAO_DEBUG", "TargetLanguage: $targetLang")
                }
            }
        }

        // Observar texto traduzido
        lifecycleScope.launch {
            viewModel.translatedText.collect { text ->
                textoTraduzido.setText(text)
            }
        }

        // Observar estado de loading
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                btnCapturar.isEnabled = !isLoading
                btnGaleria.isEnabled = !isLoading
                btntraduzir.isEnabled = !isLoading
                btnSwapLanguages.isEnabled = !isLoading
                containerSourceLanguage.isEnabled = !isLoading
                containerTargetLanguage.isEnabled = !isLoading
            }
        }

        // Observar erros
        lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show()
                    android.util.Log.e("TRADUCAO_ERROR", "Erro: $it")
                }
            }
        }
    }

    // ========== PERMISSÕES ==========
    private fun pedirPermissaoCamera() {
        if (permissionManager.hasCameraPermission()) {
            abrirCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                permissionManager.cameraPermissions(),
                PERMISSION_CAMERA
            )
        }
    }

    private fun pedirPermissaoGaleria() {
        if (permissionManager.hasGalleryPermission()) {
            abrirGaleria()
        } else {
            ActivityCompat.requestPermissions(
                this,
                permissionManager.galleryPermissions(),
                PERMISSION_GALERIA
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSION_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    abrirCamera()
                } else {
                    Toast.makeText(this, "Permissão de câmera negada", Toast.LENGTH_SHORT).show()
                }
            }
            PERMISSION_GALERIA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    abrirGaleria()
                } else {
                    Toast.makeText(this, "Permissão de galeria negada", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ========== CÂMERA ==========
    private fun abrirCamera() {
        try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, REQUEST_CAMERA)
        } catch (e: Exception) {
            Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ========== GALERIA ==========
    private fun abrirGaleria() {
        try {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_GALERIA)
        } catch (e: Exception) {
            Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ========== PROCESSAR RESULTADO ==========
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_IMAGE_VIEWER -> {
                if (resultCode == RESULT_OK && data != null) {
                    val textoSelecionado = data.getStringExtra("texto_selecionado") ?: ""
                    val idiomaDetectado = data.getStringExtra("idioma_detectado") ?: ""

                    android.util.Log.d("ACTIVITY_RESULT", "Texto recebido: '$textoSelecionado'")
                    android.util.Log.d("ACTIVITY_RESULT", "Idioma: $idiomaDetectado")

                    viewModel.setExtractedText(textoSelecionado, idiomaDetectado)
                    SharedImageViewModel.imagemCompartilhada = null
                } else {
                    SharedImageViewModel.imagemCompartilhada = null
                }
            }

            REQUEST_CAMERA -> {
                if (resultCode == RESULT_OK && data != null) {
                    val bitmap = data.extras?.get("data") as? Bitmap
                    bitmap?.let { processarImagem(it) }
                }
            }

            REQUEST_GALERIA -> {
                if (resultCode == RESULT_OK && data != null) {
                    val uri = data.data
                    if (uri != null) {
                        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                        processarImagem(bitmap)
                    }
                }
            }
        }
    }

    // ========== HELPER: Processar imagem capturada ==========
    private fun processarImagem(bitmap: Bitmap) {
        val compressedBitmap = imageProcessor.compressBitmap(bitmap)
        SharedImageViewModel.imagemCompartilhada = compressedBitmap
        val intent = Intent(this, ImageViewerActivity::class.java)
        startActivityForResult(intent, REQUEST_IMAGE_VIEWER)
    }

    // ========== Cleanup ==========
    override fun onDestroy() {
        super.onDestroy()
        textWatcherRunnable?.let { handler.removeCallbacks(it) }
    }
}