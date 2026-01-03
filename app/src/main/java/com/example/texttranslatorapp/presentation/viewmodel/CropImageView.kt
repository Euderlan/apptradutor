package com.example.texttranslatorapp.presentation.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View

class CropImageView(
    context: Context,
    private val originalBitmap: Bitmap
) : View(context) {

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

    override fun onDraw(canvas: Canvas) {
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

    fun getImagemCortada(): Bitmap? {
        return getSelectionRect()?.let { rect ->
            Bitmap.createBitmap(
                originalBitmap,
                rect.left.coerceAtLeast(0),
                rect.top.coerceAtLeast(0),
                rect.width().coerceAtMost(originalBitmap.width - rect.left),
                rect.height().coerceAtMost(originalBitmap.height - rect.top)
            )
        }
    }
}