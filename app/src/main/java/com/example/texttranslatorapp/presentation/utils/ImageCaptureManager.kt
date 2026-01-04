package com.example.texttranslatorapp.presentation.utils

import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.texttranslatorapp.presentation.ImageViewerActivity
import com.example.texttranslatorapp.presentation.viewmodel.SharedImageViewModel
import com.example.texttranslatorapp.util.ImageProcessor

class ImageCaptureManager(
    private val activity: AppCompatActivity,
    private val imageProcessor: ImageProcessor
) {

    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var imageViewerLauncher: ActivityResultLauncher<Intent>
    private var onImageViewerResult: ((textoSelecionado: String, idiomaDetectado: String) -> Unit)? = null

    init {
        setupLaunchers()
    }

    private fun setupLaunchers() {
        cameraLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                val bitmap = result.data?.extras?.get("data") as? Bitmap
                bitmap?.let { processarImagem(it) }
            }
        }

        galleryLauncher = activity.registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(activity.contentResolver, uri)
                    processarImagem(bitmap)
                } catch (e: Exception) {
                    Toast.makeText(activity, "Erro ao carregar imagem: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        imageViewerLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK && result.data != null) {
                val textoSelecionado = result.data?.getStringExtra("texto_selecionado") ?: ""
                val idiomaDetectado = result.data?.getStringExtra("idioma_detectado") ?: ""
                onImageViewerResult?.invoke(textoSelecionado, idiomaDetectado)
                SharedImageViewModel.imagemCompartilhada = null
            } else {
                SharedImageViewModel.imagemCompartilhada = null
            }
        }
    }

    fun abrirCamera() {
        try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(activity, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun abrirGaleria() {
        try {
            galleryLauncher.launch("image/*")
        } catch (e: Exception) {
            Toast.makeText(activity, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processarImagem(bitmap: Bitmap) {
        val compressedBitmap = imageProcessor.compressBitmap(bitmap)
        SharedImageViewModel.imagemCompartilhada = compressedBitmap

        val intent = Intent(activity, ImageViewerActivity::class.java)
        imageViewerLauncher.launch(intent)
    }

    fun setOnImageViewerResult(callback: (textoSelecionado: String, idiomaDetectado: String) -> Unit) {
        onImageViewerResult = callback
    }
}