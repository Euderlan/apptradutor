package com.example.texttranslatorapp.presentation.utils

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.texttranslatorapp.util.PermissionManager
import android.widget.Toast

class PermissionsHandlerManager(
    private val activity: AppCompatActivity,
    // PermissionManager centraliza a checagem e definição das permissões necessárias
    private val permissionManager: PermissionManager
) {

    companion object {
        // Códigos usados para identificar o retorno das permissões solicitadas
        const val PERMISSION_CAMERA = 101
        const val PERMISSION_GALERIA = 102
    }

    fun pedirPermissaoCamera(onGranted: () -> Unit) {
        // Se a permissão já foi concedida, executa a ação imediatamente
        if (permissionManager.hasCameraPermission()) {
            onGranted()
        } else {
            // Caso contrário, solicita a permissão ao sistema
            ActivityCompat.requestPermissions(
                activity,
                permissionManager.cameraPermissions(),
                PERMISSION_CAMERA
            )
        }
    }

    fun pedirPermissaoGaleria(onGranted: () -> Unit) {
        // Fluxo equivalente ao da câmera, mas para acesso à galeria
        if (permissionManager.hasGalleryPermission()) {
            onGranted()
        } else {
            ActivityCompat.requestPermissions(
                activity,
                permissionManager.galleryPermissions(),
                PERMISSION_GALERIA
            )
        }
    }

    fun handleRequestPermissionsResult(
        requestCode: Int,
        grantResults: IntArray,
        onCameraGranted: () -> Unit,
        onGalleryGranted: () -> Unit
    ) {
        // Trata o retorno do Android após o pedido de permissões
        when (requestCode) {
            PERMISSION_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onCameraGranted()
                } else {
                    Toast.makeText(activity, "Permissão de câmera negada", Toast.LENGTH_SHORT).show()
                }
            }
            PERMISSION_GALERIA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onGalleryGranted()
                } else {
                    Toast.makeText(activity, "Permissão de galeria negada", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
