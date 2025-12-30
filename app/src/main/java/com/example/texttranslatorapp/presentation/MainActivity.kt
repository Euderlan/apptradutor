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
import android.widget.Spinner
import android.widget.EditText
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
import android.widget.ArrayAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var btnCapturar: Button
    private lateinit var btnGaleria: Button
    private lateinit var btntraduzir: Button
    private lateinit var textoExtraido: EditText
    private lateinit var textoTraduzido: EditText
    private lateinit var spinnerLanguage: Spinner
    private lateinit var textDetectedLanguage: TextView

    private val REQUEST_CAMERA = 1
    private val REQUEST_GALERIA = 2
    private val REQUEST_IMAGE_VIEWER = 3
    private val PERMISSION_CAMERA = 101
    private val PERMISSION_GALERIA = 102

    private lateinit var permissionManager: PermissionManager
    private lateinit var imageProcessor: ImageProcessor
    private lateinit var viewModel: TranslatorViewModel

    // ✅ Flag para evitar atualizar ViewModel quando é apenas renderização
    private var isUpdatingUI = false

    // ✅ Handler e Runnable para debounce do TextWatcher
    private val handler = Handler(Looper.getMainLooper())
    private var textWatcherRunnable: Runnable? = null
    private val DEBOUNCE_DELAY = 500L  // 500ms de delay

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        initializeManagers()
        initializeViewModel()
        setupListeners()
        observeViewModel()
        setupTextWatcher()  // ✅ Adicionar listener de edição
    }

    private fun initializeViews() {
        btnCapturar = findViewById(R.id.btnCapturar)
        btnGaleria = findViewById(R.id.btnGaleria)
        textoExtraido = findViewById(R.id.textoExtraido)
        textoTraduzido = findViewById(R.id.textoTraduzido)
        textDetectedLanguage = findViewById(R.id.textDetectedLanguage)
        btntraduzir = findViewById(R.id.btntraduzir)
        spinnerLanguage = findViewById(R.id.spinnerLanguage)
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

        val idiomas = arrayOf("Português", "Inglês", "Espanhol", "Francês", "Alemão", "Italiano", "Japonês", "Chinês", "Russo")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, idiomas)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLanguage.adapter = adapter
    }

    private fun setupListeners() {
        btnCapturar.setOnClickListener { pedirPermissaoCamera() }
        btnGaleria.setOnClickListener { pedirPermissaoGaleria() }
        btntraduzir.setOnClickListener {
            val selectedLanguage = spinnerLanguage.selectedItem.toString()
            val languageCode = getLanguageCode(selectedLanguage)
            val textToTranslate = textoExtraido.text.toString()

            android.util.Log.d("TRADUCAO_DEBUG", "Texto a traduzir: '$textToTranslate'")
            android.util.Log.d("TRADUCAO_DEBUG", "Idioma alvo: $selectedLanguage ($languageCode)")

            if (textToTranslate.isEmpty()) {
                Toast.makeText(this, "Por favor, digite ou selecione um texto para traduzir", Toast.LENGTH_SHORT).show()
            } else {
                // ✅ Atualizar texto no ViewModel antes de traduzir
                viewModel.updateExtractedText(textToTranslate)
                viewModel.translateText(languageCode)
            }
        }
    }

    // ✅ TextWatcher com debounce para sincronizar edições do usuário
    private fun setupTextWatcher() {
        textoExtraido.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // ✅ Quando o usuário termina de digitar, sincroniza com o ViewModel
                if (!isUpdatingUI && s != null) {
                    // Remover callback anterior se existir
                    textWatcherRunnable?.let { handler.removeCallbacks(it) }

                    // Criar novo callback com delay
                    textWatcherRunnable = Runnable {
                        val textoAtual = s.toString()
                        viewModel.updateExtractedText(textoAtual)
                        android.util.Log.d("TEXT_WATCHER", "Texto editado e sincronizado (com debounce): '$textoAtual'")
                    }

                    // Postar com delay de 500ms
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
                // ✅ Colocar cursor no final
                textoExtraido.setSelection(text.length)
                isUpdatingUI = false
                android.util.Log.d("TRADUCAO_DEBUG", "extractedText atualizado: '$text'")
            }
        }

        // Observar idioma detectado
        lifecycleScope.launch {
            viewModel.detectedLanguage.collect { detection ->
                detection?.let {
                    textDetectedLanguage.text = "Idioma detectado: ${it.detectedLanguage}"
                    android.util.Log.d("TRADUCAO_DEBUG", "Idioma detectado: ${it.detectedLanguage} (${it.languageCode})")
                }
            }
        }

        // Observar texto traduzido
        lifecycleScope.launch {
            viewModel.translatedText.collect { text ->
                textoTraduzido.setText(text)
                android.util.Log.d("TRADUCAO_DEBUG", "translatedText atualizado: '$text'")
            }
        }

        // Observar estado de loading
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                btnCapturar.isEnabled = !isLoading
                btnGaleria.isEnabled = !isLoading
                btntraduzir.isEnabled = !isLoading
                android.util.Log.d("TRADUCAO_DEBUG", "isLoading: $isLoading")
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
            // ========== RETORNO DA ImageViewerActivity ==========
            REQUEST_IMAGE_VIEWER -> {
                if (resultCode == RESULT_OK && data != null) {
                    // ✅ Receber dados da ImageViewerActivity
                    val textoSelecionado = data.getStringExtra("texto_selecionado") ?: ""
                    val idiomaDetectado = data.getStringExtra("idioma_detectado") ?: ""

                    android.util.Log.d("ACTIVITY_RESULT", "Texto recebido: '$textoSelecionado'")
                    android.util.Log.d("ACTIVITY_RESULT", "Idioma: $idiomaDetectado")

                    // ✅ IMPORTANTE: Atualizar ViewModel com os dados recebidos
                    viewModel.setExtractedText(textoSelecionado, idiomaDetectado)

                    // Limpar bitmap compartilhado
                    SharedImageViewModel.imagemCompartilhada = null
                } else {
                    // Usuário cancelou
                    SharedImageViewModel.imagemCompartilhada = null
                }
            }

            // ========== RETORNO DA CÂMERA ==========
            REQUEST_CAMERA -> {
                if (resultCode == RESULT_OK && data != null) {
                    val bitmap = data.extras?.get("data") as? Bitmap
                    bitmap?.let { processarImagem(it) }
                }
            }

            // ========== RETORNO DA GALERIA ==========
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

        // Salvar no ViewModel compartilhado
        SharedImageViewModel.imagemCompartilhada = compressedBitmap

        // Abrir ImageViewerActivity
        val intent = Intent(this, ImageViewerActivity::class.java)
        startActivityForResult(intent, REQUEST_IMAGE_VIEWER)
    }

    private fun getLanguageCode(languageName: String): String {
        return when(languageName) {
            "Português" -> "pt"
            "Inglês" -> "en"
            "Espanhol" -> "es"
            "Francês" -> "fr"
            "Alemão" -> "de"
            "Italiano" -> "it"
            "Japonês" -> "ja"
            "Chinês" -> "zh"
            "Russo" -> "ru"
            else -> "en"
        }
    }

    // ========== Cleanup ==========
    override fun onDestroy() {
        super.onDestroy()
        // Remover callbacks pendentes
        textWatcherRunnable?.let { handler.removeCallbacks(it) }
    }
}