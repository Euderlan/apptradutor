package com.example.texttranslatorapp.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

// Gerencia verificação e fornecimento das permissões usadas no app
class PermissionManager(private val context: Context) {

    // Verifica se a permissão de câmera foi concedida
    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Verifica se a permissão de acesso à galeria foi concedida
    fun hasGalleryPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_IMAGES
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Retorna as permissões necessárias para uso da câmera
    fun cameraPermissions(): Array<String> {
        return arrayOf(Manifest.permission.CAMERA)
    }

    // Retorna as permissões necessárias para acesso à galeria
    fun galleryPermissions(): Array<String> {
        return arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    }
}
