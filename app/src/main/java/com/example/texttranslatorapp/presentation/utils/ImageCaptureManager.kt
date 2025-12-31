package com.example.texttranslatorapp.presentation.utils

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.texttranslatorapp.presentation.ImageViewerActivity
import com.example.texttranslatorapp.presentation.viewmodel.SharedImageViewModel
import com.example.texttranslatorapp.util.ImageProcessor

class ImageCaptureManager(
    // Referência da Activity para iniciar intents (câmera, galeria e tela de visualização).
    private val activity: AppCompatActivity,
    // Processador de imagem para reduzir tamanho/qualidade antes de repassar a imagem.
    private val imageProcessor: ImageProcessor
) {

    companion object {
        // Códigos usados para identificar o retorno (onActivityResult) de cada ação.
        const val REQUEST_CAMERA = 1
        const val REQUEST_GALERIA = 2
        const val REQUEST_IMAGE_VIEWER = 3
    }

    fun abrirCamera() {
        try {
            // Intent padrão do Android para abrir a câmera e capturar uma imagem.
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

            // startActivityForResult: inicia a Activity externa e espera retorno no onActivityResult.
            activity.startActivityForResult(intent, REQUEST_CAMERA)
        } catch (e: Exception) {
            // Tratamento de erro para evitar crash caso não exista app de câmera ou falhe ao abrir.
            Toast.makeText(activity, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun abrirGaleria() {
        try {
            // Intent para selecionar uma imagem na galeria (conteúdo externo).
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

            // Inicia o seletor de imagens e espera retorno.
            activity.startActivityForResult(intent, REQUEST_GALERIA)
        } catch (e: Exception) {
            // Tratamento de erro para falhas ao abrir a galeria/seletor de imagens.
            Toast.makeText(activity, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun processarImagem(bitmap: Bitmap) {
        // Reduz/comprime o bitmap para evitar consumo alto de memória e facilitar transporte entre telas.
        val compressedBitmap = imageProcessor.compressBitmap(bitmap)

        // Armazena a imagem em um local compartilhado (estático) para que a próxima Activity possa acessar.
        // Isso evita passar Bitmap grande via Intent extras (o que pode estourar o limite do Binder).
        SharedImageViewModel.imagemCompartilhada = compressedBitmap

        // Abre a tela que exibirá a imagem e permitirá seleção de texto/resultado.
        val intent = Intent(activity, ImageViewerActivity::class.java)

        // Aguarda retorno da ImageViewerActivity com texto selecionado e idioma detectado.
        activity.startActivityForResult(intent, REQUEST_IMAGE_VIEWER)
    }

    fun handleActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        // Callback para devolver para a Activity chamadora o texto selecionado e o idioma detectado.
        onImageViewerResult: (textoSelecionado: String, idiomaDetectado: String) -> Unit
    ) {
        // Identifica qual fluxo está retornando (viewer, câmera ou galeria).
        when (requestCode) {
            REQUEST_IMAGE_VIEWER -> {
                // Retorno da tela ImageViewerActivity, que deve enviar texto e idioma via extras.
                if (resultCode == AppCompatActivity.RESULT_OK && data != null) {
                    // Recupera o texto selecionado (se não existir, usa string vazia).
                    val textoSelecionado = data.getStringExtra("texto_selecionado") ?: ""

                    // Recupera o idioma detectado (se não existir, usa string vazia).
                    val idiomaDetectado = data.getStringExtra("idioma_detectado") ?: ""

                    // Entrega os resultados ao chamador (normalmente MainActivity -> ViewModel).
                    onImageViewerResult(textoSelecionado, idiomaDetectado)

                    // Limpa a imagem compartilhada para evitar manter Bitmap em memória sem necessidade.
                    SharedImageViewModel.imagemCompartilhada = null
                } else {
                    // Mesmo se cancelar/der erro, limpa a referência para evitar vazamento de memória.
                    SharedImageViewModel.imagemCompartilhada = null
                }
            }

            REQUEST_CAMERA -> {
                // Retorno da câmera: normalmente vem um thumbnail no extra "data".
                if (resultCode == AppCompatActivity.RESULT_OK && data != null) {
                    // Extrai o bitmap do extra "data" (pode ser null dependendo do device/app de câmera).
                    val bitmap = data.extras?.get("data") as? Bitmap

                    // Se existe bitmap, processa (comprime, salva no compartilhado e abre ImageViewerActivity).
                    bitmap?.let { processarImagem(it) }
                }
            }

            REQUEST_GALERIA -> {
                // Retorno do seletor de galeria: vem um URI apontando para a imagem escolhida.
                if (resultCode == AppCompatActivity.RESULT_OK && data != null) {
                    val uri = data.data
                    if (uri != null) {
                        // Converte URI em Bitmap usando ContentResolver para então processar a imagem.
                        val bitmap = MediaStore.Images.Media.getBitmap(activity.contentResolver, uri)
                        processarImagem(bitmap)
                    }
                }
            }
        }
    }
}
