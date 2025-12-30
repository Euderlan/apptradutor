package com.example.texttranslatorapp.presentation

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
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
import com.example.texttranslatorapp.presentation.viewmodel.TranslatorViewModel
import com.example.texttranslatorapp.util.ImageProcessor
import com.example.texttranslatorapp.util.PermissionManager
import kotlinx.coroutines.launch
import android.widget.ArrayAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var btnCapturar: Button
    private lateinit var btnGaleria: Button
    private lateinit var btntraduzir: Button
    private lateinit var textoExtraido: EditText  // ← MUDADO PARA EditText
    private lateinit var textoTraduzido: EditText  // ← MUDADO PARA EditText
    private lateinit var spinnerLanguage: Spinner
    private lateinit var textDetectedLanguage: TextView

    private val REQUEST_CAMERA = 1
    private val REQUEST_GALERIA = 2
    private val PERMISSION_CAMERA = 101
    private val PERMISSION_GALERIA = 102

    private lateinit var permissionManager: PermissionManager
    private lateinit var imageProcessor: ImageProcessor
    private lateinit var viewModel: TranslatorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        initializeManagers()
        initializeViewModel()
        setupListeners()
        observeViewModel()
    }

    private fun initializeViews() {
        btnCapturar = findViewById(R.id.btnCapturar)
        btnGaleria = findViewById(R.id.btnGaleria)
        textoExtraido = findViewById(R.id.textoExtraido)  // ← EditText
        textoTraduzido = findViewById(R.id.textoTraduzido)  // ← EditText
        textDetectedLanguage = findViewById(R.id.textDetectedLanguage)
        btntraduzir = findViewById(R.id.btntraduzir)
        spinnerLanguage = findViewById(R.id.spinnerLanguage)
    }

    private fun initializeManagers() {
        permissionManager = PermissionManager(this)
        imageProcessor = ImageProcessor()
    }

    private fun initializeViewModel() {
        // Inicializar repositórios e use cases
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

        // Configurar Spinner
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
            val languageCode = when(selectedLanguage) {
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
            // ← AGORA USA O TEXTO DO EditText
            val textToTranslate = textoExtraido.text.toString()
            if (textToTranslate.isEmpty()) {
                Toast.makeText(this, "Por favor, extraia um texto primeiro", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.translateText(languageCode)
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.extractedText.collect { text ->
                textoExtraido.setText(text)  // ← EditText.setText()
            }
        }

        lifecycleScope.launch {
            viewModel.detectedLanguage.collect { detection ->
                detection?.let {
                    textDetectedLanguage.text = "Idioma detectado: ${it.detectedLanguage}"
                }
            }
        }

        lifecycleScope.launch {
            viewModel.translatedText.collect { text ->
                textoTraduzido.setText(text)  // ← EditText.setText()
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                btnCapturar.isEnabled = !isLoading
                btnGaleria.isEnabled = !isLoading
                btntraduzir.isEnabled = !isLoading
            }
        }

        lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show()
                    android.util.Log.e("TRADUCAO", "Erro: $it")
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

        if (resultCode == RESULT_OK && data != null) {
            val bitmap = when (requestCode) {
                REQUEST_CAMERA -> data.extras?.get("data") as? Bitmap
                REQUEST_GALERIA -> {
                    val uri = data.data
                    MediaStore.Images.Media.getBitmap(contentResolver, uri)
                }
                else -> null
            }

            bitmap?.let {
                val compressedBitmap = imageProcessor.compressBitmap(it)
                viewModel.processImage(compressedBitmap)
            }
        }
    }
}