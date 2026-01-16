package com.example.texttranslatorapp.presentation.utils

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.texttranslatorapp.util.PermissionManager
import android.widget.Toast

/**
 * Classe responsável por gerenciar o fluxo de permissões em tempo de execução.
 *
 * Centraliza:
 * - pedido de permissões
 * - verificação de concessão
 * - tratamento do retorno do sistema
 */
class PermissionsHandlerManager(
    private val activity: AppCompatActivity,
    // PermissionManager abstrai quais permissões são necessárias
    private val permissionManager: PermissionManager
) {

    companion object {
        // Request codes usados para identificar o resultado das permissões
        const val PERMISSION_CAMERA = 101
        const val PERMISSION_GALERIA = 102
    }

    /**
     * Solicita permissão para uso da câmera.
     *
     * Executa imediatamente o callback caso a permissão já tenha sido concedida.
     */
    fun pedirPermissaoCamera(onGranted: () -> Unit) {
        if (permissionManager.hasCameraPermission()) {
            onGranted()
        } else {
            ActivityCompat.requestPermissions(
                activity,
                permissionManager.cameraPermissions(),
                PERMISSION_CAMERA
            )
        }
    }

    /**
     * Solicita permissão para acesso à galeria.
     */
    fun pedirPermissaoGaleria(onGranted: () -> Unit) {
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

    /**
     * Trata o retorno do sistema após o usuário responder ao pedido de permissão.
     *
     * Deve ser chamado a partir do onRequestPermissionsResult da Activity.
     */
    fun handleRequestPermissionsResult(
        requestCode: Int,
        grantResults: IntArray,
        onCameraGranted: () -> Unit,
        onGalleryGranted: () -> Unit
    ) {
        when (requestCode) {

            // Resultado da permissão de câmera
            PERMISSION_CAMERA -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    onCameraGranted()
                } else {
                    Toast.makeText(
                        activity,
                        "Permissão de câmera negada",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            // Resultado da permissão de galeria
            PERMISSION_GALERIA -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    onGalleryGranted()
                } else {
                    Toast.makeText(
                        activity,
                        "Permissão de galeria negada",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
