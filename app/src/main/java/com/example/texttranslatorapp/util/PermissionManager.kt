package com.example.texttranslatorapp.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class PermissionManager(private val context: Context) {

    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasGalleryPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_IMAGES
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun cameraPermissions(): Array<String> {
        return arrayOf(Manifest.permission.CAMERA)
    }

    fun galleryPermissions(): Array<String> {
        return arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    }
}