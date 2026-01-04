package com.example.texttranslatorapp.presentation

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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
import com.example.texttranslatorapp.presentation.utils.ImageDimensionCalculator
import com.example.texttranslatorapp.presentation.utils.TextExtractionHelper
import com.example.texttranslatorapp.presentation.viewmodel.SharedImageViewModel
import com.example.texttranslatorapp.presentation.viewmodel.TranslatorViewModel
import com.example.texttranslatorapp.presentation.viewmodel.CropImageView
import kotlinx.coroutines.launch
import com.example.texttranslatorapp.presentation.utils.ImageOptimizationUtils

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var imgContainer: FrameLayout
    private lateinit var editTextSelecionado: EditText
    private lateinit var textDetectado: TextView
    private lateinit var btnContinuar: Button
    private lateinit var btnCancelar: Button
    private lateinit var btnExtrairTudo: Button
    private lateinit var btnExtrairSelecao: Button
    private lateinit var loadingText: TextView
    private lateinit var btnToggleCrop: Button

    private lateinit var viewModel: TranslatorViewModel
    private lateinit var textExtractionHelper: TextExtractionHelper
    private lateinit var imageDimensionCalculator: ImageDimensionCalculator

    private var imagemBitmap: Bitmap? = null
    private var cropView: CropImageView? = null
    private var modoCropAtivo = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        imageDimensionCalculator = ImageDimensionCalculator(this)

        initializeViews()
        initializeViewModel()
        setupImage()
        setupListeners()
        observeViewModel()
    }

    private fun initializeViews() {
        imgContainer = findViewById(R.id.imgContainer)
        editTextSelecionado = findViewById(R.id.editTextSelecionado)
        textDetectado = findViewById(R.id.textDetectado)
        btnContinuar = findViewById(R.id.btnContinuar)
        btnCancelar = findViewById(R.id.btnCancelar)
        btnExtrairTudo = findViewById(R.id.btnExtrairTudo)
        btnExtrairSelecao = findViewById(R.id.btnExtrairSelecao)
        loadingText = findViewById(R.id.loadingText)
        btnToggleCrop = findViewById(R.id.btnToggleCrop)
    }

    private fun initializeViewModel() {
        val textExtractor = MLKitTextExtractorMultilingual()
        val languageDetector = MLKitLanguageDetector()
        val translationService = TranslationApiService()

        val textExtractionRepo = TextExtractionRepository(textExtractor)
        val languageDetectionRepo = LanguageDetectionRepository(languageDetector)
        val translationRepo = TranslationRepository(translationService)

        val extractTextUC = ExtractTextUseCase(textExtractionRepo)
        val detectLanguageUC = DetectLanguageUseCase(languageDetectionRepo)
        val translateTextUC = TranslateTextUseCase(translationRepo)

        viewModel = TranslatorViewModel(extractTextUC, detectLanguageUC, translateTextUC)
        textExtractionHelper = TextExtractionHelper(textExtractor)
    }

    private fun setupImage() {
        imagemBitmap = SharedImageViewModel.imagemCompartilhada

        imagemBitmap?.let {
            val imagemOtimizada = ImageOptimizationUtils.otimizarImagemParaOCR(it)

            val validacao = ImageOptimizationUtils.validarQualidadeImagem(imagemOtimizada)
            if (!validacao.isValid) {
                Toast.makeText(this, validacao.message, Toast.LENGTH_LONG).show()
                loadingText.text = validacao.message
                return@let
            }

            adicionarImagemAoContainer(imagemOtimizada)

            val dicas = ImageOptimizationUtils.obterDicasOCR()
            loadingText.text = getString(R.string.texto_dica_ocr, dicas.random())

            viewModel.processImage(imagemOtimizada)
        } ?: run {
            Toast.makeText(this, getString(R.string.erro_imagem_nao_recebida), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun adicionarImagemAoContainer(bitmap: Bitmap) {
        cropView = CropImageView(this, bitmap)
        imgContainer.removeAllViews()

        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )

        imgContainer.addView(cropView, layoutParams)

        Log.d("ImageViewerActivity", "Imagem adicionada ao container em tamanho completo")
        Log.d("ImageViewerActivity", "Tamanho original: ${bitmap.width}x${bitmap.height}")
    }

    private fun setupListeners() {
        btnToggleCrop.setOnClickListener {
            if (!modoCropAtivo) {
                modoCropAtivo = true
                cropView?.setSelecaoAtiva(true)
                btnToggleCrop.text = getString(R.string.cancelar_selecao)
                btnExtrairTudo.isEnabled = false
                btnExtrairSelecao.isEnabled = true
                loadingText.text = getString(R.string.modo_crop_ativo)
                Toast.makeText(this, getString(R.string.arraste_para_selecionar), Toast.LENGTH_SHORT).show()
            } else {
                modoCropAtivo = false
                cropView?.setSelecaoAtiva(false)
                btnToggleCrop.text = getString(R.string.ativar_selecao)
                btnExtrairTudo.isEnabled = true
                btnExtrairSelecao.isEnabled = false
                loadingText.text = getString(R.string.pronto)
            }
        }

        btnExtrairTudo.setOnClickListener {
            lifecycleScope.launch {
                viewModel.extractedText.collect { text ->
                    editTextSelecionado.setText(text)
                }
            }
        }

        btnExtrairSelecao.setOnClickListener {
            if (cropView?.temSelecao() == true) {
                extrairTextoDaSelecao()
            } else {
                Toast.makeText(this, getString(R.string.selecione_area_primeiro), Toast.LENGTH_SHORT).show()
            }
        }

        btnContinuar.setOnClickListener {
            val textoSelecionado = editTextSelecionado.text.toString()
            if (textoSelecionado.isEmpty()) {
                Toast.makeText(this, getString(R.string.selecione_texto), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent().apply {
                putExtra("texto_selecionado", textoSelecionado)
                putExtra("idioma_detectado", viewModel.detectedLanguage.value?.detectedLanguage ?: getString(R.string.idioma_padrao))
            }
            setResult(RESULT_OK, intent)
            finish()
        }

        btnCancelar.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun extrairTextoDaSelecao() {
        imagemBitmap?.let { imagem ->
            cropView?.getSelectionRect()?.let { rect ->
                Log.d("ImageViewerActivity", "Iniciando extração de crop")
                Log.d("ImageViewerActivity", "Bitmap: ${imagem.width}x${imagem.height}")
                Log.d("ImageViewerActivity", "Rect: $rect")

                val minSize = 100
                if (rect.width() < minSize || rect.height() < minSize) {
                    val msg = getString(R.string.area_muito_pequena, rect.width(), rect.height(), minSize)
                    Log.w("ImageViewerActivity", msg)
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                    return@let
                }

                lifecycleScope.launch {
                    loadingText.text = getString(R.string.extraindo_selecao)
                    btnExtrairSelecao.isEnabled = false

                    try {
                        Log.d("ImageViewerActivity", "Chamando textExtractionHelper")
                        val textoExtraido = textExtractionHelper.extrairTextoDaSelecao(imagem, rect)

                        if (textoExtraido.isEmpty()) {
                            Log.w("ImageViewerActivity", "Nenhum texto detectado na seleção")
                            Toast.makeText(
                                this@ImageViewerActivity,
                                getString(R.string.nenhum_texto_detectado_area),
                                Toast.LENGTH_LONG
                            ).show()
                            loadingText.text = getString(R.string.nenhum_texto_encontrado)
                        } else {
                            Log.d("ImageViewerActivity", "Texto extraído com sucesso")
                            Log.d("ImageViewerActivity", "Caracteres: ${textoExtraido.length}")

                            editTextSelecionado.setText(textoExtraido)
                            loadingText.text = getString(R.string.texto_extraido_caracteres, textoExtraido.length)

                            Toast.makeText(
                                this@ImageViewerActivity,
                                getString(R.string.texto_extraido_caracteres, textoExtraido.length),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Log.e("ImageViewerActivity", "ERRO na extração de crop", e)
                        val mensagemErro = e.message ?: getString(R.string.erro_desconhecido)
                        Toast.makeText(
                            this@ImageViewerActivity,
                            getString(R.string.erro_ao_extrair, mensagemErro),
                            Toast.LENGTH_LONG
                        ).show()
                        loadingText.text = getString(R.string.erro_na_extracao, mensagemErro)
                    } finally {
                        btnExtrairSelecao.isEnabled = true
                    }
                }
            } ?: run {
                Log.w("ImageViewerActivity", "getSelectionRect() retornou null")
                Toast.makeText(
                    this,
                    getString(R.string.selecao_invalida),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.extractedText.collect { text ->
                if (text.isNotEmpty() && editTextSelecionado.text.isEmpty()) {
                    editTextSelecionado.setText(text)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.detectedLanguage.collect { detection ->
                detection?.let {
                    loadingText.text = getString(R.string.idioma_detectado_com_valor, it.detectedLanguage)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                if (!isLoading) {
                    loadingText.text = getString(R.string.pronto)
                }
                btnContinuar.isEnabled = !isLoading
                btnExtrairTudo.isEnabled = !isLoading && !modoCropAtivo
                btnToggleCrop.isEnabled = !isLoading
            }
        }

        lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    Log.e("ImageViewerActivity", "Erro do ViewModel: $it")
                    Toast.makeText(this@ImageViewerActivity, getString(R.string.erro_com_mensagem, it), Toast.LENGTH_LONG).show()
                    loadingText.text = getString(R.string.erro_extrair)
                }
            }
        }
    }
}