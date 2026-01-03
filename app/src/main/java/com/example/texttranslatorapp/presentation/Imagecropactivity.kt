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

class ImageCropActivity : AppCompatActivity() {

    private lateinit var cropContainer: FrameLayout
    private lateinit var btnUsarTudo: Button
    private lateinit var btnSelecionarArea: Button
    private lateinit var btnConfirmar: Button
    private lateinit var btnCancelar: Button

    private var imagemOriginal: Bitmap? = null
    private var cropView: CropImageView? = null
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
        imagemOriginal = SharedImageViewModel.imagemCompartilhada

        imagemOriginal?.let { original ->
            // Criar CustomImageView
            cropView = CropImageView(this, original)

            // Adicionar ao container
            cropContainer.removeAllViews()
            cropContainer.addView(cropView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        } ?: run {
            Toast.makeText(this, "Erro: Imagem não recebida", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupListeners() {
        // Usar imagem inteira (sem crop)
        btnUsarTudo.setOnClickListener {
            if (modoSelecao) {
                // Se estava em modo seleção, sair dele
                modoSelecao = false
                cropView?.setSelecaoAtiva(false)
                updateUI()
            } else {
                // Usar imagem inteira e retornar
                SharedImageViewModel.imagemCompartilhada = imagemOriginal
                setResult(RESULT_OK)
                finish()
            }
        }

        // Entrar em modo seleção
        btnSelecionarArea.setOnClickListener {
            if (!modoSelecao) {
                modoSelecao = true
                cropView?.setSelecaoAtiva(true)
                updateUI()
                Toast.makeText(this, "Arraste para selecionar a área", Toast.LENGTH_SHORT).show()
            }
        }

        // Confirmar seleção
        btnConfirmar.setOnClickListener {
            if (modoSelecao && cropView?.temSelecao() == true) {
                cropImage()
            } else {
                Toast.makeText(this, "Selecione uma área primeiro", Toast.LENGTH_SHORT).show()
            }
        }

        // Cancelar
        btnCancelar.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

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

                    SharedImageViewModel.imagemCompartilhada = croppedBitmap
                    setResult(RESULT_OK)
                    finish()
                }
            }
        }
    }

    // CustomImageView para desenho preciso
    inner class CropImageView(
        context: android.content.Context,
        private val originalBitmap: Bitmap
    ) : android.view.View(context) {

        private var selecaoAtiva = false
        private var selectionRect = Rect()
        private var startX = 0f
        private var startY = 0f

        private var displayWidth = 0
        private var displayHeight = 0
        private var offsetX = 0f
        private var offsetY = 0f
        private var scale = 1f

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            displayWidth = w
            displayHeight = h

            // Calcular escala mantendo proporção
            val bitmapRatio = originalBitmap.width.toFloat() / originalBitmap.height
            val viewRatio = w.toFloat() / h

            if (bitmapRatio > viewRatio) {
                scale = w.toFloat() / originalBitmap.width
            } else {
                scale = h.toFloat() / originalBitmap.height
            }

            offsetX = (w - originalBitmap.width * scale) / 2
            offsetY = (h - originalBitmap.height * scale) / 2
        }

        override fun onDraw(canvas: android.graphics.Canvas) {
            super.onDraw(canvas)

            // Desenhar imagem
            canvas.save()
            canvas.translate(offsetX, offsetY)
            canvas.scale(scale, scale)
            canvas.drawBitmap(originalBitmap, 0f, 0f, null)
            canvas.restore()

            // Desenhar seleção se ativa
            if (selecaoAtiva && selectionRect.width() > 0 && selectionRect.height() > 0) {
                // Overlay escuro
                val paint = Paint().apply {
                    color = 0x99000000.toInt()
                }

                // Áreas fora da seleção
                canvas.drawRect(0f, 0f, displayWidth.toFloat(), selectionRect.top.toFloat(), paint)
                canvas.drawRect(0f, selectionRect.bottom.toFloat(), displayWidth.toFloat(), displayHeight.toFloat(), paint)
                canvas.drawRect(0f, selectionRect.top.toFloat(), selectionRect.left.toFloat(), selectionRect.bottom.toFloat(), paint)
                canvas.drawRect(selectionRect.right.toFloat(), selectionRect.top.toFloat(), displayWidth.toFloat(), selectionRect.bottom.toFloat(), paint)

                // Borda branca
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
                MotionEvent.ACTION_DOWN -> {
                    startX = x
                    startY = y
                    selectionRect.set(x.toInt(), y.toInt(), x.toInt(), y.toInt())
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val left = minOf(startX, x).toInt()
                    val top = minOf(startY, y).toInt()
                    val right = maxOf(startX, x).toInt()
                    val bottom = maxOf(startY, y).toInt()

                    selectionRect.set(left, top, right, bottom)
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    return true
                }
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

        fun temSelecao(): Boolean = selectionRect.width() > 50 && selectionRect.height() > 50

        fun getSelectionRect(): Rect? = if (temSelecao()) selectionRect else null
    }
}