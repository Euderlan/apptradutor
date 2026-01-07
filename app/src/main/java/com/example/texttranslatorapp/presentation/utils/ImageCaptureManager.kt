package com.example.texttranslatorapp.presentation.utils

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.example.texttranslatorapp.presentation.ImageViewerActivity
import com.example.texttranslatorapp.presentation.viewmodel.SharedImageViewModel
import com.example.texttranslatorapp.util.ImageProcessor
import java.io.File

class ImageCaptureManager(
    private val activity: AppCompatActivity,
    private val imageProcessor: ImageProcessor
) {

    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var imageViewerLauncher: ActivityResultLauncher<Intent>
    private var onImageViewerResult: ((textoSelecionado: String, idiomaDetectado: String) -> Unit)? = null

    private var cameraImageUri: Uri? = null

    init {
        setupLaunchers()
    }

    private fun setupLaunchers() {
        cameraLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                cameraImageUri?.let { uri ->
                    try {
                        var bitmap = MediaStore.Images.Media.getBitmap(activity.contentResolver, uri)
                        bitmap = corrigirRotacao(bitmap, uri)

                        // IMPORTANTE: Comprime fotos da câmera (geralmente muito grandes)
                        // Reduz para máximo 2048px mantendo proporção
                        bitmap = comprimirSeFotoDCamera(bitmap)

                        abrirImageViewer(bitmap)
                    } catch (e: Exception) {
                        Toast.makeText(activity, "Erro ao processar foto: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        galleryLauncher = activity.registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                try {
                    var bitmap = MediaStore.Images.Media.getBitmap(activity.contentResolver, uri)
                    bitmap = corrigirRotacao(bitmap, uri)
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
            intent.putExtra("android.intent.extras.CAMERA_FACING", 0)

            val photoFile = createImageFile()
            cameraImageUri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                photoFile
            )

            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
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
        val imagemFinal = imageProcessor.compressBitmap(bitmap, maxWidth = 4096, maxHeight = 4096)
        abrirImageViewer(imagemFinal)
    }

    private fun comprimirSeFotoDCamera(bitmap: Bitmap): Bitmap {
        // Fotos da câmera geralmente são muito grandes (3000x4000+)
        // Reduz para 2048px máximo para evitar problemas de renderização
        val maxSize = 2048
        return if (bitmap.width > maxSize || bitmap.height > maxSize) {
            imageProcessor.compressBitmap(bitmap, maxWidth = maxSize, maxHeight = maxSize)
        } else {
            bitmap
        }
    }

    private fun abrirImageViewer(bitmap: Bitmap) {
        SharedImageViewModel.imagemCompartilhada = bitmap
        val intent = Intent(activity, ImageViewerActivity::class.java)
        imageViewerLauncher.launch(intent)
    }

    private fun createImageFile(): File {
        val storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("IMG_", ".jpg", storageDir)
    }

    private fun corrigirRotacao(bitmap: Bitmap, uri: Uri): Bitmap {
        return try {
            val inputStream = activity.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }

            inputStream.close()

            if (rotationDegrees == 0) {
                bitmap
            } else {
                imageProcessor.rotateBitmap(bitmap, rotationDegrees.toFloat())
            }
        } catch (e: Exception) {
            bitmap
        }
    }

    fun setOnImageViewerResult(callback: (textoSelecionado: String, idiomaDetectado: String) -> Unit) {
        onImageViewerResult = callback
    }
}