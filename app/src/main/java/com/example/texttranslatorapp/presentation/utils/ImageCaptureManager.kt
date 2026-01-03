package com.example.texttranslatorapp.presentation.utils

import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.texttranslatorapp.presentation.ImageViewerActivity
import com.example.texttranslatorapp.presentation.viewmodel.SharedImageViewModel
import com.example.texttranslatorapp.util.ImageProcessor

class ImageCaptureManager(
    private val activity: AppCompatActivity,
    private val imageProcessor: ImageProcessor
) {

    companion object {
        const val REQUEST_CAMERA = 1
        const val REQUEST_GALERIA = 2
        const val REQUEST_IMAGE_VIEWER = 3
    }

    fun abrirCamera() {
        try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            activity.startActivityForResult(intent, REQUEST_CAMERA)
        } catch (e: Exception) {
            Toast.makeText(activity, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun abrirGaleria() {
        try {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activity.startActivityForResult(intent, REQUEST_GALERIA)
        } catch (e: Exception) {
            Toast.makeText(activity, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processarImagem(bitmap: Bitmap) {
        // Comprime a imagem
        val compressedBitmap = imageProcessor.compressBitmap(bitmap)

        // Armazena no local compartilhado
        SharedImageViewModel.imagemCompartilhada = compressedBitmap

        // Abre ImageViewerActivity diretamente (sem Activity de crop intermediária)
        val intent = Intent(activity, ImageViewerActivity::class.java)
        activity.startActivityForResult(intent, REQUEST_IMAGE_VIEWER)
    }

    fun handleActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        onImageViewerResult: (textoSelecionado: String, idiomaDetectado: String) -> Unit
    ) {
        when (requestCode) {
            REQUEST_IMAGE_VIEWER -> {
                if (resultCode == AppCompatActivity.RESULT_OK && data != null) {
                    val textoSelecionado = data.getStringExtra("texto_selecionado") ?: ""
                    val idiomaDetectado = data.getStringExtra("idioma_detectado") ?: ""
                    onImageViewerResult(textoSelecionado, idiomaDetectado)
                    SharedImageViewModel.imagemCompartilhada = null
                } else {
                    SharedImageViewModel.imagemCompartilhada = null
                }
            }

            REQUEST_CAMERA -> {
                if (resultCode == AppCompatActivity.RESULT_OK && data != null) {
                    val bitmap = data.extras?.get("data") as? Bitmap
                    bitmap?.let { processarImagem(it) }
                }
            }

            REQUEST_GALERIA -> {
                if (resultCode == AppCompatActivity.RESULT_OK && data != null) {
                    val uri = data.data
                    if (uri != null) {
                        val bitmap = MediaStore.Images.Media.getBitmap(activity.contentResolver, uri)
                        processarImagem(bitmap)
                    }
                }
            }
        }
    }
}