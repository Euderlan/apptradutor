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

// Activity responsável por exibir a imagem recebida e permitir extrair texto (OCR) da imagem inteira ou de uma seleção
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

    private lateinit var textExtractionHelper: TextExtractionHelper

    private var imagemBitmap: Bitmap? = null
    private var cropView: CropImageView? = null
    private var modoCropAtivo = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        // Inicializa UI, dependências e listeners
        initializeViews()
        initializeHelper()
        setupImage()
        setupListeners()
    }

    private fun initializeViews() {
        // Faz o bind dos componentes do layout
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

    private fun initializeHelper() {
        // Configura o helper de extração de texto com o extractor multilíngue
        val textExtractor = MLKitTextExtractorMultilingual()
        textExtractionHelper = TextExtractionHelper(textExtractor)
    }

    private fun setupImage() {
        // Recupera o Bitmap compartilhado por outra tela (via ViewModel estático/compartilhado)
        imagemBitmap = SharedImageViewModel.imagemCompartilhada

        imagemBitmap?.let {
            // Otimiza a imagem (ex.: redimensionamento) para melhorar desempenho do OCR
            val imagemOtimizada = ImageOptimizationUtils.otimizarImagemParaOCR(it)

            // Valida se a imagem tem tamanho aceitável antes do processamento
            val validacao = ImageOptimizationUtils.validarQualidadeImagem(imagemOtimizada)
            if (!validacao.isValid) {
                Toast.makeText(this, validacao.message, Toast.LENGTH_LONG).show()
                loadingText.text = validacao.message
                return@let
            }

            // Exibe a imagem no container usando uma View que permite seleção (crop)
            adicionarImagemAoContainer(imagemOtimizada)

            // Exibe uma dica aleatória para melhorar resultados do OCR
            val dicas = ImageOptimizationUtils.obterDicasOCR()
            loadingText.text = getString(R.string.texto_dica_ocr, dicas.random())

        } ?: run {
            // Finaliza a tela se não houver imagem recebida
            Toast.makeText(this, getString(R.string.erro_imagem_nao_recebida), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun adicionarImagemAoContainer(bitmap: Bitmap) {
        // Cria a CropImageView com o bitmap e adiciona ao container
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
            // Alterna entre modo seleção (crop) ativo/inativo e ajusta a UI conforme o estado
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

        // Extrai texto da imagem completa (sem seleção)
        btnExtrairTudo.setOnClickListener {
            extrairTextoDaImagemCompleta()
        }

        // Extrai texto apenas da área selecionada (se existir seleção válida)
        btnExtrairSelecao.setOnClickListener {
            if (cropView?.temSelecao() == true) {
                extrairTextoDaSelecao()
            } else {
                Toast.makeText(this, getString(R.string.selecione_area_primeiro), Toast.LENGTH_SHORT).show()
            }
        }

        btnContinuar.setOnClickListener {
            // Retorna o texto extraído/selecionado para a Activity anterior via setResult
            val textoSelecionado = editTextSelecionado.text.toString()
            if (textoSelecionado.isEmpty()) {
                Toast.makeText(this, getString(R.string.selecione_texto), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent().apply {
                putExtra("texto_selecionado", textoSelecionado)
                putExtra("idioma_detectado", "")
            }
            setResult(RESULT_OK, intent)
            finish()
        }

        btnCancelar.setOnClickListener {
            // Cancela a operação e fecha a tela
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun extrairTextoDaImagemCompleta() {
        imagemBitmap?.let { imagem ->
            lifecycleScope.launch {
                // Executa OCR em coroutine para não bloquear a UI
                loadingText.text = getString(R.string.extraindo_selecao)
                btnExtrairTudo.isEnabled = false

                try {
                    Log.d("ImageViewerActivity", "Extraindo texto da imagem completa")

                    // Reaproveita o mesmo fluxo de extração passando um rect que cobre a imagem inteira
                    val textoExtraido = textExtractionHelper.extrairTextoDaSelecao(
                        imagem,
                        android.graphics.Rect(0, 0, imagem.width, imagem.height)
                    )

                    if (textoExtraido.isEmpty()) {
                        Log.w("ImageViewerActivity", "Nenhum texto detectado")
                        Toast.makeText(
                            this@ImageViewerActivity,
                            getString(R.string.nenhum_texto_detectado_area),
                            Toast.LENGTH_LONG
                        ).show()
                        loadingText.text = getString(R.string.nenhum_texto_encontrado)
                    } else {
                        Log.d("ImageViewerActivity", "Texto extraído: ${textoExtraido.length} caracteres")
                        editTextSelecionado.setText(textoExtraido)
                        loadingText.text = getString(R.string.texto_extraido_caracteres, textoExtraido.length)

                        Toast.makeText(
                            this@ImageViewerActivity,
                            getString(R.string.texto_extraido_caracteres, textoExtraido.length),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    // Trata falhas do OCR e exibe mensagem amigável para o usuário
                    Log.e("ImageViewerActivity", "ERRO na extração", e)
                    val mensagemErro = e.message ?: getString(R.string.erro_desconhecido)
                    Toast.makeText(
                        this@ImageViewerActivity,
                        getString(R.string.erro_ao_extrair, mensagemErro),
                        Toast.LENGTH_LONG
                    ).show()
                    loadingText.text = getString(R.string.erro_na_extracao, mensagemErro)
                } finally {
                    btnExtrairTudo.isEnabled = true
                }
            }
        }
    }

    private fun extrairTextoDaSelecao() {
        imagemBitmap?.let { imagem ->
            cropView?.getSelectionRect()?.let { rect ->
                Log.d("ImageViewerActivity", "Iniciando extração de crop")
                Log.d("ImageViewerActivity", "Bitmap: ${imagem.width}x${imagem.height}")
                Log.d("ImageViewerActivity", "Rect: $rect")

                // Evita processar seleções inválidas
                if (rect.width() <= 0 || rect.height() <= 0) {
                    Toast.makeText(this, "Seleção inválida", Toast.LENGTH_LONG).show()
                    return@let
                }

                lifecycleScope.launch {
                    // Executa OCR da área selecionada em coroutine
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
                        // Trata falhas do OCR na seleção
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
                // Caso não exista rect de seleção disponível
                Log.w("ImageViewerActivity", "getSelectionRect() retornou null")
                Toast.makeText(
                    this,
                    getString(R.string.selecione_area_primeiro),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
