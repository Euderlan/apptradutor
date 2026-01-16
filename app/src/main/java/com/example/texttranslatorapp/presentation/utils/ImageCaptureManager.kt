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

/**
 * Gerencia todo o fluxo de captura e seleção de imagens:
 * - câmera
 * - galeria
 * - visualização e retorno de texto selecionado
 *
 * Centraliza a lógica de intents, tratamento de Bitmap e callbacks.
 */
class ImageCaptureManager(
    private val activity: AppCompatActivity,
    private val imageProcessor: ImageProcessor
) {

    // Launchers responsáveis por iniciar câmera, galeria e visualizador
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var imageViewerLauncher: ActivityResultLauncher<Intent>

    // Callback usado para devolver o texto selecionado ao chamador
    private var onImageViewerResult:
            ((textoSelecionado: String, idiomaDetectado: String) -> Unit)? = null

    // URI temporária da imagem capturada pela câmera
    private var cameraImageUri: Uri? = null

    init {
        // Inicializa todos os launchers assim que o manager é criado
        setupLaunchers()
    }

    /**
     * Registra os ActivityResultLaunchers e define o comportamento
     * ao receber o resultado de cada fluxo.
     */
    private fun setupLaunchers() {

        // Launcher da câmera
        cameraLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                cameraImageUri?.let { uri ->
                    try {
                        // Converte a imagem capturada em Bitmap
                        var bitmap =
                            MediaStore.Images.Media.getBitmap(
                                activity.contentResolver,
                                uri
                            )

                        // Corrige rotação baseada nos metadados EXIF
                        bitmap = corrigirRotacao(bitmap, uri)

                        // Compressão extra para fotos da câmera (normalmente muito grandes)
                        bitmap = comprimirSeFotoDCamera(bitmap)

                        abrirImageViewer(bitmap)
                    } catch (e: Exception) {
                        Toast.makeText(
                            activity,
                            "Erro ao processar foto: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        // Launcher da galeria
        galleryLauncher = activity.registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                try {
                    var bitmap =
                        MediaStore.Images.Media.getBitmap(
                            activity.contentResolver,
                            uri
                        )

                    // Ajusta rotação para imagens da galeria também
                    bitmap = corrigirRotacao(bitmap, uri)

                    processarImagem(bitmap)
                } catch (e: Exception) {
                    Toast.makeText(
                        activity,
                        "Erro ao carregar imagem: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // Launcher do ImageViewerActivity
        imageViewerLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK && result.data != null) {
                val textoSelecionado =
                    result.data?.getStringExtra("texto_selecionado") ?: ""
                val idiomaDetectado =
                    result.data?.getStringExtra("idioma_detectado") ?: ""

                // Retorna o resultado para quem chamou
                onImageViewerResult?.invoke(
                    textoSelecionado,
                    idiomaDetectado
                )

                // Limpa a imagem compartilhada para evitar vazamento de memória
                SharedImageViewModel.imagemCompartilhada = null
            } else {
                SharedImageViewModel.imagemCompartilhada = null
            }
        }
    }

    /**
     * Inicia o fluxo de captura de imagem pela câmera.
     */
    fun abrirCamera() {
        try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra("android.intent.extras.CAMERA_FACING", 0)

            // Cria arquivo temporário para a foto
            val photoFile = createImageFile()

            // Gera URI segura via FileProvider
            cameraImageUri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                photoFile
            )

            // Força a câmera a salvar a imagem no arquivo criado
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)

            cameraLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(activity, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Inicia o fluxo de seleção de imagem pela galeria.
     */
    fun abrirGaleria() {
        try {
            galleryLauncher.launch("image/*")
        } catch (e: Exception) {
            Toast.makeText(activity, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Processamento padrão para imagens da galeria.
     * Aplica compressão leve antes de exibir.
     */
    private fun processarImagem(bitmap: Bitmap) {
        val imagemFinal =
            imageProcessor.compressBitmap(
                bitmap,
                maxWidth = 4096,
                maxHeight = 4096
            )

        abrirImageViewer(imagemFinal)
    }

    /**
     * Aplica compressão mais agressiva apenas para fotos da câmera.
     */
    private fun comprimirSeFotoDCamera(bitmap: Bitmap): Bitmap {
        val maxSize = 2048
        return if (bitmap.width > maxSize || bitmap.height > maxSize) {
            imageProcessor.compressBitmap(
                bitmap,
                maxWidth = maxSize,
                maxHeight = maxSize
            )
        } else {
            bitmap
        }
    }

    /**
     * Abre a tela de visualização da imagem.
     * Usa ViewModel compartilhado para evitar serialização de Bitmap.
     */
    private fun abrirImageViewer(bitmap: Bitmap) {
        SharedImageViewModel.imagemCompartilhada = bitmap
        val intent = Intent(activity, ImageViewerActivity::class.java)
        imageViewerLauncher.launch(intent)
    }

    /**
     * Cria um arquivo temporário no diretório privado do app.
     */
    private fun createImageFile(): File {
        val storageDir =
            activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        return File.createTempFile(
            "IMG_",
            ".jpg",
            storageDir
        )
    }

    /**
     * Corrige a rotação da imagem com base nos metadados EXIF.
     *
     * Essencial para fotos da câmera, que geralmente vêm rotacionadas.
     */
    private fun corrigirRotacao(bitmap: Bitmap, uri: Uri): Bitmap {
        return try {
            val inputStream =
                activity.contentResolver.openInputStream(uri)
                    ?: return bitmap

            val exif = ExifInterface(inputStream)
            val orientation =
                exif.getAttributeInt(
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
                imageProcessor.rotateBitmap(
                    bitmap,
                    rotationDegrees.toFloat()
                )
            }
        } catch (e: Exception) {
            bitmap
        }
    }

    /**
     * Define o callback que receberá o texto selecionado
     * após o usuário interagir com o ImageViewer.
     */
    fun setOnImageViewerResult(
        callback: (textoSelecionado: String, idiomaDetectado: String) -> Unit
    ) {
        onImageViewerResult = callback
    }
}
