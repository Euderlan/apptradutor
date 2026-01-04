package com.example.texttranslatorapp.presentation

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.texttranslatorapp.R
import com.example.texttranslatorapp.data.datasource.MLKitLanguageDetector
import com.example.texttranslatorapp.data.datasource.TranslationApiService
import com.example.texttranslatorapp.data.datasource.MLKitTextExtractorMultilingual
import com.example.texttranslatorapp.data.repository.LanguageDetectionRepository
import com.example.texttranslatorapp.data.repository.TextExtractionRepository
import com.example.texttranslatorapp.data.repository.TranslationRepository
import com.example.texttranslatorapp.domain.usecases.DetectLanguageUseCase
import com.example.texttranslatorapp.domain.usecases.ExtractTextUseCase
import com.example.texttranslatorapp.domain.usecases.TranslateTextUseCase
import com.example.texttranslatorapp.presentation.viewmodel.TranslatorViewModel
import com.example.texttranslatorapp.presentation.utils.ImageCaptureManager
import com.example.texttranslatorapp.presentation.utils.LanguageDialogManager
import com.example.texttranslatorapp.presentation.utils.LanguageDialogManagerImpl
import com.example.texttranslatorapp.presentation.utils.PermissionsHandlerManager
import com.example.texttranslatorapp.presentation.utils.TextWatcherDebounceManager
import com.example.texttranslatorapp.presentation.utils.UIListenersManager
import com.example.texttranslatorapp.presentation.utils.ViewModelObserverManager
import com.example.texttranslatorapp.util.ImageProcessor
import com.example.texttranslatorapp.util.PermissionManager

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

    private lateinit var viewModel: TranslatorViewModel
    private lateinit var permissionManager: PermissionManager
    private lateinit var permissionsHandler: PermissionsHandlerManager
    private lateinit var imageCaptureManager: ImageCaptureManager
    private lateinit var languageDialogManager: LanguageDialogManager
    private lateinit var uiListenersManager: UIListenersManager
    private lateinit var textWatcherManager: TextWatcherDebounceManager
    private lateinit var observerManager: ViewModelObserverManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        initializeViewModels()
        initializeManagers()
        setupUI()
        observeViewModel()
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

    private fun initializeViewModels() {
        val textExtractor = MLKitTextExtractorMultilingual()
        val languageDetector = MLKitLanguageDetector()
        val translationService = TranslationApiService()

        val textExtractionRepo = TextExtractionRepository(textExtractor)
        val languageDetectionRepo = LanguageDetectionRepository(languageDetector)
        val translationRepo = TranslationRepository(translationService)

        val extractTextUC = ExtractTextUseCase(textExtractionRepo)
        val detectLanguageUC = DetectLanguageUseCase(languageDetectionRepo)
        val translateTextUC = TranslateTextUseCase(translationRepo)

        viewModel = TranslatorViewModel(
            extractTextUC,
            detectLanguageUC,
            translateTextUC
        )

        textSourceLanguage.text = "Inglês"
        textTargetLanguage.text = "Português"
    }

    private fun initializeManagers() {
        permissionManager = PermissionManager(this)
        permissionsHandler = PermissionsHandlerManager(this, permissionManager)
        imageCaptureManager = ImageCaptureManager(this, ImageProcessor())

        languageDialogManager = LanguageDialogManagerImpl(
            this,
            viewModel,
            textSourceLanguage,
            textTargetLanguage
        )

        textWatcherManager = TextWatcherDebounceManager(viewModel)
        uiListenersManager = UIListenersManager(this, viewModel, languageDialogManager)
        observerManager = ViewModelObserverManager(this, viewModel, textWatcherManager)

        imageCaptureManager.setOnImageViewerResult { textoSelecionado, idiomaDetectado ->
            viewModel.setExtractedText(textoSelecionado, idiomaDetectado)
        }
    }

    private fun setupUI() {
        uiListenersManager.setupCaptureButtons(
            btnCapturar,
            btnGaleria,
            onCameraClick = {
                permissionsHandler.pedirPermissaoCamera {
                    imageCaptureManager.abrirCamera()
                }
            },
            onGalleryClick = {
                permissionsHandler.pedirPermissaoGaleria {
                    imageCaptureManager.abrirGaleria()
                }
            }
        )

        uiListenersManager.setupLanguageContainers(
            containerSourceLanguage,
            containerTargetLanguage
        )

        uiListenersManager.setupSwapButton(btnSwapLanguages)
        uiListenersManager.setupTranslateButton(btntraduzir, textoExtraido)

        textWatcherManager.setupTextWatcher(textoExtraido)
    }

    private fun observeViewModel() {
        observerManager.observeExtractedText(textoExtraido, textSourceLanguage)
        observerManager.observeDetectedLanguage(textDetectedLanguage)
        observerManager.observeSourceLanguage(textSourceLanguage)
        observerManager.observeTargetLanguage(textTargetLanguage)
        observerManager.observeTranslatedText(textoTraduzido)

        observerManager.observeLoadingState(
            btnCapturar,
            btnGaleria,
            btntraduzir,
            btnSwapLanguages,
            containerSourceLanguage,
            containerTargetLanguage
        )

        observerManager.observeErrors()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        permissionsHandler.handleRequestPermissionsResult(
            requestCode,
            grantResults,
            onCameraGranted = { imageCaptureManager.abrirCamera() },
            onGalleryGranted = { imageCaptureManager.abrirGaleria() }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        textWatcherManager.cleanup()
    }
}