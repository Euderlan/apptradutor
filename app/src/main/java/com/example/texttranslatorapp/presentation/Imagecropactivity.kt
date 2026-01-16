package com.example.texttranslatorapp.presentation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.texttranslatorapp.R
import com.example.texttranslatorapp.presentation.viewmodel.SharedImageViewModel

/**
 * Tela responsável por permitir que o usuário:
 * - use a imagem inteira, ou
 * - selecione uma área (crop) e confirme o recorte
 *
 * A imagem entra e sai pela SharedImageViewModel, evitando passar Bitmap por Intent.
 */
class ImageCropActivity : AppCompatActivity() {

    private lateinit var cropContainer: FrameLayout
    private lateinit var btnUsarTudo: Button
    private lateinit var btnSelecionarArea: Button
    private lateinit var btnConfirmar: Button
    private lateinit var btnCancelar: Button

    // Bitmap recebido de outra tela (via SharedImageViewModel)
    private var imagemOriginal: Bitmap? = null

    // View customizada usada para exibir a imagem e desenhar o retângulo de seleção
    private var cropView: CropImageView? = null

    // Controla se a tela está no modo de seleção/crop
    private var modoSelecao = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_crop)

        initializeViews()
        setupImage()
        setupListeners()
    }

    private fun initializeViews() {
        cropContainer = findViewById(R.id.cropContainer)
        btnUsarTudo = findViewById(R.id.btnUsarTudo)
        btnSelecionarArea = findViewById(R.id.btnSelecionarArea)
        btnConfirmar = findViewById(R.id.btnConfirmar)
        btnCancelar = findViewById(R.id.btnCancelar)
    }

    private fun setupImage() {
        // Recupera a imagem compartilhada pela tela anterior
        imagemOriginal = SharedImageViewModel.imagemCompartilhada

        imagemOriginal?.let { original ->
            // Cria a View que desenha o bitmap e permite seleção por toque
            cropView = CropImageView(this, original)

            // Insere a CropImageView no container da tela
            cropContainer.removeAllViews()
            cropContainer.addView(
                cropView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        } ?: run {
            // Se não houver imagem, encerra para evitar tela quebrada
            Toast.makeText(this, "Erro: Imagem não recebida", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupListeners() {

        // Botão "Usar Imagem Inteira" também funciona como "Cancelar Seleção"
        btnUsarTudo.setOnClickListener {
            if (modoSelecao) {
                // Sai do modo seleção e limpa o retângulo
                modoSelecao = false
                cropView?.setSelecaoAtiva(false)
                updateUI()
            } else {
                // Retorna a imagem original sem recorte
                SharedImageViewModel.imagemCompartilhada = imagemOriginal
                setResult(RESULT_OK)
                finish()
            }
        }

        // Habilita modo seleção: usuário pode arrastar para definir o retângulo
        btnSelecionarArea.setOnClickListener {
            if (!modoSelecao) {
                modoSelecao = true
                cropView?.setSelecaoAtiva(true)
                updateUI()
                Toast.makeText(this, "Arraste para selecionar a área", Toast.LENGTH_SHORT).show()
            }
        }

        // Confirma o recorte somente se existir uma seleção válida
        btnConfirmar.setOnClickListener {
            if (modoSelecao && cropView?.temSelecao() == true) {
                cropImage()
            } else {
                Toast.makeText(this, "Selecione uma área primeiro", Toast.LENGTH_SHORT).show()
            }
        }

        // Cancela e volta sem alterar a imagem compartilhada
        btnCancelar.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    /**
     * Ajusta textos e estados de botões conforme o modo atual.
     */
    private fun updateUI() {
        if (modoSelecao) {
            btnUsarTudo.text = "Cancelar Seleção"
            btnSelecionarArea.isEnabled = false
            btnConfirmar.isEnabled = true
        } else {
            btnUsarTudo.text = "Usar Imagem Inteira"
            btnSelecionarArea.isEnabled = true
            btnConfirmar.isEnabled = false
        }
    }

    /**
     * Executa o crop no bitmap original usando o retângulo selecionado
     * (coordenadas retornadas pela CropImageView) e devolve o resultado.
     */
    private fun cropImage() {
        cropView?.getSelectionRect()?.let { rect ->
            imagemOriginal?.let { original ->
                if (rect.width() > 0 && rect.height() > 0) {
                    val croppedBitmap = Bitmap.createBitmap(
                        original,
                        rect.left.coerceAtLeast(0),
                        rect.top.coerceAtLeast(0),
                        rect.width().coerceAtMost(original.width - rect.left),
                        rect.height().coerceAtMost(original.height - rect.top)
                    )

                    // Devolve o bitmap recortado para as próximas telas
                    SharedImageViewModel.imagemCompartilhada = croppedBitmap
                    setResult(RESULT_OK)
                    finish()
                }
            }
        }
    }

    /**
     * View interna responsável por:
     * - desenhar o bitmap ajustado na tela (escala + centralização)
     * - permitir o usuário arrastar e desenhar um retângulo de seleção
     * - exibir overlay escuro fora da seleção
     *
     * Observação: nesta implementação, o selectionRect fica em coordenadas da View.
     * O método getSelectionRect() retorna diretamente esse Rect, sem mapear para o Bitmap.
     */
    inner class CropImageView(
        context: android.content.Context,
        private val originalBitmap: Bitmap
    ) : android.view.View(context) {

        private var selecaoAtiva = false
        private var selectionRect = Rect()
        private var startX = 0f
        private var startY = 0f

        // Dimensões da View e parâmetros para exibir a imagem centralizada
        private var displayWidth = 0
        private var displayHeight = 0
        private var offsetX = 0f
        private var offsetY = 0f
        private var scale = 1f

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            displayWidth = w
            displayHeight = h

            // Calcula escala mantendo proporção da imagem, para caber na View
            val bitmapRatio = originalBitmap.width.toFloat() / originalBitmap.height
            val viewRatio = w.toFloat() / h

            scale = if (bitmapRatio > viewRatio) {
                w.toFloat() / originalBitmap.width
            } else {
                h.toFloat() / originalBitmap.height
            }

            // Centraliza a imagem dentro da View
            offsetX = (w - originalBitmap.width * scale) / 2
            offsetY = (h - originalBitmap.height * scale) / 2
        }

        override fun onDraw(canvas: android.graphics.Canvas) {
            super.onDraw(canvas)

            // Desenha o bitmap com translate/scale para encaixar corretamente
            canvas.save()
            canvas.translate(offsetX, offsetY)
            canvas.scale(scale, scale)
            canvas.drawBitmap(originalBitmap, 0f, 0f, null)
            canvas.restore()

            // Desenha a seleção (overlay + borda) somente quando ativa e com tamanho válido
            if (selecaoAtiva && selectionRect.width() > 0 && selectionRect.height() > 0) {
                val paint = Paint().apply { color = 0x99000000.toInt() }

                // Escurece áreas fora do retângulo selecionado
                canvas.drawRect(0f, 0f, displayWidth.toFloat(), selectionRect.top.toFloat(), paint)
                canvas.drawRect(0f, selectionRect.bottom.toFloat(), displayWidth.toFloat(), displayHeight.toFloat(), paint)
                canvas.drawRect(0f, selectionRect.top.toFloat(), selectionRect.left.toFloat(), selectionRect.bottom.toFloat(), paint)
                canvas.drawRect(selectionRect.right.toFloat(), selectionRect.top.toFloat(), displayWidth.toFloat(), selectionRect.bottom.toFloat(), paint)

                // Borda branca para destacar o retângulo selecionado
                val borderPaint = Paint().apply {
                    color = 0xFFFFFFFF.toInt()
                    strokeWidth = 3f
                    style = Paint.Style.STROKE
                }
                canvas.drawRect(selectionRect, borderPaint)
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (!selecaoAtiva) return false

            val x = event.x
            val y = event.y

            when (event.action) {
                // Inicia seleção
                MotionEvent.ACTION_DOWN -> {
                    startX = x
                    startY = y
                    selectionRect.set(x.toInt(), y.toInt(), x.toInt(), y.toInt())
                    invalidate()
                    return true
                }
                // Atualiza retângulo conforme arrasto
                MotionEvent.ACTION_MOVE -> {
                    val left = minOf(startX, x).toInt()
                    val top = minOf(startY, y).toInt()
                    val right = maxOf(startX, x).toInt()
                    val bottom = maxOf(startY, y).toInt()

                    selectionRect.set(left, top, right, bottom)
                    invalidate()
                    return true
                }
                // Finaliza seleção
                MotionEvent.ACTION_UP -> return true
            }
            return false
        }

        fun setSelecaoAtiva(ativa: Boolean) {
            selecaoAtiva = ativa
            if (!ativa) {
                selectionRect.setEmpty()
            }
            invalidate()
        }

        // Critério mínimo para considerar que houve seleção
        fun temSelecao(): Boolean =
            selectionRect.width() > 50 && selectionRect.height() > 50

        // Retorna o retângulo atual se houver seleção válida
        fun getSelectionRect(): Rect? =
            if (temSelecao()) selectionRect else null
    }
}
