package com.example.texttranslatorapp.presentation.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import android.view.MotionEvent
import android.view.View

/**
 * View customizada que exibe um Bitmap ajustado à tela e permite
 * que o usuário selecione uma área retangular para recorte.
 *
 * A seleção é feita por toque e arrasto, desenhada visualmente
 * sobre a imagem, e pode ser convertida para coordenadas reais
 * do Bitmap original.
 */
class CropImageView(
    context: Context,
    private val originalBitmap: Bitmap
) : View(context) {

    // Indica se o modo de seleção está ativo
    private var selecaoAtiva = false

    // Retângulo de seleção em coordenadas da VIEW
    private var selectionRect = Rect()

    // Ponto inicial do toque (ACTION_DOWN)
    private var startX = 0f
    private var startY = 0f

    // Dimensões reais da View na tela
    private var displayWidth = 0
    private var displayHeight = 0

    // Offsets usados para centralizar o bitmap na View
    private var offsetX = 0f
    private var offsetY = 0f

    // Fator de escala aplicado ao bitmap para caber na View
    private var scale = 1f

    init {
        // Fundo escuro para destacar a imagem
        setBackgroundColor(0xFF121212.toInt())
    }

    /**
     * Chamado quando a View recebe ou altera suas dimensões.
     *
     * Aqui é calculado:
     * - o fator de escala do bitmap
     * - os offsets para centralização
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        displayWidth = w
        displayHeight = h

        // Proporção do bitmap e da View
        val bitmapRatio = originalBitmap.width.toFloat() / originalBitmap.height
        val viewRatio = w.toFloat() / h

        // Ajusta a escala mantendo o aspect ratio
        scale = if (bitmapRatio > viewRatio) {
            w.toFloat() / originalBitmap.width
        } else {
            h.toFloat() / originalBitmap.height
        }

        // Centraliza o bitmap na View
        offsetX = (w - originalBitmap.width * scale) / 2
        offsetY = (h - originalBitmap.height * scale) / 2

        Log.d(
            "CropImageView",
            "onSizeChanged: Display=${w}x${h}, Bitmap=${originalBitmap.width}x${originalBitmap.height}"
        )
        Log.d("CropImageView", "Scale=$scale, Offset=($offsetX, $offsetY)")
    }

    /**
     * Responsável por desenhar:
     * - o bitmap escalado e centralizado
     * - a área escurecida fora da seleção
     * - a borda do retângulo de seleção
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Desenha o bitmap aplicando escala e offset
        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)
        canvas.drawBitmap(originalBitmap, 0f, 0f, null)
        canvas.restore()

        // Desenha overlay e borda apenas se houver seleção ativa
        if (selecaoAtiva && selectionRect.width() > 0 && selectionRect.height() > 0) {

            // Paint para escurecer área fora da seleção
            val darkPaint = Paint().apply {
                color = 0x99000000.toInt()
            }

            // Área acima da seleção
            canvas.drawRect(
                0f, 0f,
                displayWidth.toFloat(),
                selectionRect.top.toFloat(),
                darkPaint
            )

            // Área abaixo da seleção
            canvas.drawRect(
                0f,
                selectionRect.bottom.toFloat(),
                displayWidth.toFloat(),
                displayHeight.toFloat(),
                darkPaint
            )

            // Área à esquerda da seleção
            canvas.drawRect(
                0f,
                selectionRect.top.toFloat(),
                selectionRect.left.toFloat(),
                selectionRect.bottom.toFloat(),
                darkPaint
            )

            // Área à direita da seleção
            canvas.drawRect(
                selectionRect.right.toFloat(),
                selectionRect.top.toFloat(),
                displayWidth.toFloat(),
                selectionRect.bottom.toFloat(),
                darkPaint
            )

            // Borda branca da seleção
            val borderPaint = Paint().apply {
                color = 0xFFFFFFFF.toInt()
                strokeWidth = 3f
                style = Paint.Style.STROKE
            }
            canvas.drawRect(selectionRect, borderPaint)
        }
    }

    /**
     * Trata os eventos de toque para criar o retângulo de seleção.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!selecaoAtiva) return false

        val x = event.x
        val y = event.y

        when (event.action) {

            // Início da seleção
            MotionEvent.ACTION_DOWN -> {
                startX = x
                startY = y
                selectionRect.set(x.toInt(), y.toInt(), x.toInt(), y.toInt())
                invalidate()
                return true
            }

            // Arrasto do dedo
            MotionEvent.ACTION_MOVE -> {
                val left = minOf(startX, x).toInt()
                val top = minOf(startY, y).toInt()
                val right = maxOf(startX, x).toInt()
                val bottom = maxOf(startY, y).toInt()

                selectionRect.set(left, top, right, bottom)
                invalidate()
                return true
            }

            // Fim da seleção
            MotionEvent.ACTION_UP -> return true
        }
        return false
    }

    /**
     * Ativa ou desativa o modo de seleção.
     */
    fun setSelecaoAtiva(ativa: Boolean) {
        selecaoAtiva = ativa
        if (!ativa) {
            selectionRect.setEmpty()
        }
        invalidate()
    }

    /**
     * Verifica se há uma seleção válida mínima na View.
     */
    fun temSelecao(): Boolean =
        selectionRect.width() > 50 && selectionRect.height() > 50

    /**
     * Converte o retângulo de seleção da View
     * para coordenadas reais do Bitmap original.
     *
     * @return Rect em coordenadas do Bitmap ou null se inválido.
     */
    fun getSelectionRect(): Rect? {

        if (!temSelecao()) {
            Log.d(
                "CropImageView",
                "Seleção muito pequena: ${selectionRect.width()}x${selectionRect.height()}"
            )
            return null
        }

        val minSize = 100

        // Converte coordenadas da View → Bitmap
        val left =
            ((selectionRect.left - offsetX) / scale)
                .toInt()
                .coerceAtLeast(0)

        val top =
            ((selectionRect.top - offsetY) / scale)
                .toInt()
                .coerceAtLeast(0)

        val right =
            ((selectionRect.right - offsetX) / scale)
                .toInt()
                .coerceAtMost(originalBitmap.width)

        val bottom =
            ((selectionRect.bottom - offsetY) / scale)
                .toInt()
                .coerceAtMost(originalBitmap.height)

        val finalWidth = right - left
        val finalHeight = bottom - top

        // Garante tamanho mínimo após conversão
        if (finalWidth < minSize || finalHeight < minSize) {
            Log.w(
                "CropImageView",
                "Após conversão, seleção inválida: ${finalWidth}x${finalHeight}"
            )
            return null
        }

        val mappedRect = Rect(left, top, right, bottom)

        Log.d("CropImageView", "Selection mapeado:")
        Log.d("CropImageView", "  View coords: $selectionRect")
        Log.d("CropImageView", "  Bitmap coords: $mappedRect")
        Log.d("CropImageView", "  Scale: $scale, Offset: ($offsetX, $offsetY)")

        return mappedRect
    }
}
