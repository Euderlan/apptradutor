package com.example.texttranslatorapp.presentation.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
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

    private var zoomScale = 1f
    private val scaleGestureDetector: ScaleGestureDetector

    private var translateX = 0f
    private var translateY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private val minZoom = 1f
    private val maxZoom = 4f

    init {
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        setBackgroundColor(0xFF121212.toInt())
    }

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

        Log.d("CropImageView", "onSizeChanged: Display=${w}x${h}, Bitmap=${originalBitmap.width}x${originalBitmap.height}")
        Log.d("CropImageView", "Scale=$scale, Offset=($offsetX, $offsetY)")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()

        val centerX = displayWidth / 2f
        val centerY = displayHeight / 2f

        canvas.translate(centerX, centerY)
        canvas.scale(zoomScale, zoomScale)
        canvas.translate(translateX / zoomScale, translateY / zoomScale)
        canvas.translate(-centerX / zoomScale, -centerY / zoomScale)

        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)
        canvas.drawBitmap(originalBitmap, 0f, 0f, null)
        canvas.restore()

        if (selecaoAtiva && selectionRect.width() > 0 && selectionRect.height() > 0) {
            val darkPaint = Paint().apply {
                color = 0x99000000.toInt()
            }

            canvas.drawRect(0f, 0f, displayWidth.toFloat(), selectionRect.top.toFloat(), darkPaint)
            canvas.drawRect(0f, selectionRect.bottom.toFloat(), displayWidth.toFloat(), displayHeight.toFloat(), darkPaint)
            canvas.drawRect(0f, selectionRect.top.toFloat(), selectionRect.left.toFloat(), selectionRect.bottom.toFloat(), darkPaint)
            canvas.drawRect(selectionRect.right.toFloat(), selectionRect.top.toFloat(), displayWidth.toFloat(), selectionRect.bottom.toFloat(), darkPaint)

            val borderPaint = Paint().apply {
                color = 0xFFFFFFFF.toInt()
                strokeWidth = 3f
                style = Paint.Style.STROKE
            }
            canvas.drawRect(selectionRect, borderPaint)

            val debugPaint = Paint().apply {
                color = 0xFFFFFFFF.toInt()
                textSize = 16f
            }
            canvas.drawText(
                "${selectionRect.width()}x${selectionRect.height()}",
                selectionRect.left + 10f,
                selectionRect.top + 30f,
                debugPaint
            )
        }

        if (zoomScale > 1.1f) {
            val zoomTextPaint = Paint().apply {
                color = 0xFFFFFFFF.toInt()
                textSize = 14f
            }
            canvas.drawText("Zoom: %.1fx".format(zoomScale), 10f, 30f, zoomTextPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)

        if (scaleGestureDetector.isInProgress) {
            return true
        }

        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = x
                lastTouchY = y

                if (selecaoAtiva) {
                    startX = x
                    startY = y
                    selectionRect.set(x.toInt(), y.toInt(), x.toInt(), y.toInt())
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (selecaoAtiva) {
                    val left = minOf(startX, x).toInt()
                    val top = minOf(startY, y).toInt()
                    val right = maxOf(startX, x).toInt()
                    val bottom = maxOf(startY, y).toInt()

                    selectionRect.set(left, top, right, bottom)
                } else if (zoomScale > 1.1f) {
                    val deltaX = x - lastTouchX
                    val deltaY = y - lastTouchY

                    translateX += deltaX
                    translateY += deltaY

                    limitTranslation()

                    lastTouchX = x
                    lastTouchY = y
                }

                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                Log.d("CropImageView", "Touch finalizado")
                return true
            }
        }
        return false
    }

    private fun limitTranslation() {
        val maxTranslateX = (displayWidth * (zoomScale - 1)) / 2
        val maxTranslateY = (displayHeight * (zoomScale - 1)) / 2

        translateX = translateX.coerceIn(-maxTranslateX, maxTranslateX)
        translateY = translateY.coerceIn(-maxTranslateY, maxTranslateY)
    }

    fun setSelecaoAtiva(ativa: Boolean) {
        selecaoAtiva = ativa
        if (!ativa) {
            selectionRect.setEmpty()
        }
        invalidate()
    }

    fun temSelecao(): Boolean = selectionRect.width() > 50 && selectionRect.height() > 50

    fun getSelectionRect(): Rect? {
        if (!temSelecao()) {
            Log.d("CropImageView", "Seleção muito pequena: ${selectionRect.width()}x${selectionRect.height()}")
            return null
        }

        val minSize = 100
        if (selectionRect.width() < minSize || selectionRect.height() < minSize) {
            Log.w("CropImageView", "Seleção abaixo do mínimo ($minSize px): ${selectionRect.width()}x${selectionRect.height()}")
            return null
        }

        val left = ((selectionRect.left - offsetX) / scale).toInt().coerceAtLeast(0)
        val top = ((selectionRect.top - offsetY) / scale).toInt().coerceAtLeast(0)
        val right = ((selectionRect.right - offsetX) / scale).toInt()
            .coerceAtMost(originalBitmap.width)
        val bottom = ((selectionRect.bottom - offsetY) / scale).toInt()
            .coerceAtMost(originalBitmap.height)

        val finalWidth = right - left
        val finalHeight = bottom - top

        if (finalWidth < minSize || finalHeight < minSize) {
            Log.w("CropImageView", "Após conversão, seleção inválida: ${finalWidth}x${finalHeight}")
            return null
        }

        val mappedRect = Rect(left, top, right, bottom)
        Log.d("CropImageView", "Selection mapeado:")
        Log.d("CropImageView", "  View coords: $selectionRect")
        Log.d("CropImageView", "  Bitmap coords: $mappedRect")
        Log.d("CropImageView", "  Scale: $scale, Offset: ($offsetX, $offsetY)")

        return mappedRect
    }

    fun getImagemCortada(): Bitmap? {
        return getSelectionRect()?.let { rect ->
            try {
                Bitmap.createBitmap(
                    originalBitmap,
                    rect.left.coerceAtLeast(0),
                    rect.top.coerceAtLeast(0),
                    rect.width().coerceAtMost(originalBitmap.width - rect.left),
                    rect.height().coerceAtMost(originalBitmap.height - rect.top)
                )
            } catch (e: Exception) {
                Log.e("CropImageView", "Erro ao criar bitmap cortado: ${e.message}")
                null
            }
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            zoomScale = (zoomScale * scaleFactor).coerceIn(minZoom, maxZoom)

            if (zoomScale <= minZoom) {
                zoomScale = minZoom
                translateX = 0f
                translateY = 0f
            } else {
                limitTranslation()
            }

            Log.d("CropImageView", "Zoom: $zoomScale")
            invalidate()
            return true
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            Log.d("CropImageView", "Zoom iniciado")
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            Log.d("CropImageView", "Zoom finalizado: $zoomScale")

            if (zoomScale < 1.1f) {
                zoomScale = 1f
                translateX = 0f
                translateY = 0f
                invalidate()
            }
        }
    }
}