package com.example.texttranslatorapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var btnCapturar: Button
    private lateinit var btnGaleria: Button
    private lateinit var textoExtraido: TextView

    private val REQUEST_CAMERA = 1
    private val REQUEST_GALERIA = 2
    private val PERMISSION_CAMERA = 101
    private val PERMISSION_GALERIA = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnCapturar = findViewById(R.id.btnCapturar)
        btnGaleria = findViewById(R.id.btnGaleria)
        textoExtraido = findViewById(R.id.textoExtraido)

        btnCapturar.setOnClickListener { pedirPermissaoCamera() }
        btnGaleria.setOnClickListener { pedirPermissaoGaleria() }
    }

    // ========== PERMISSÕES ==========
    private fun pedirPermissaoCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            abrirCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                PERMISSION_CAMERA
            )
        }
    }

    private fun pedirPermissaoGaleria() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
            == PackageManager.PERMISSION_GRANTED
        ) {
            abrirGaleria()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                PERMISSION_GALERIA
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSION_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    abrirCamera()
                } else {
                    Toast.makeText(this, "Permissão de câmera negada", Toast.LENGTH_SHORT).show()
                }
            }
            PERMISSION_GALERIA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    abrirGaleria()
                } else {
                    Toast.makeText(this, "Permissão de galeria negada", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ========== CÂMERA ==========
    private fun abrirCamera() {
        try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, REQUEST_CAMERA)
        } catch (e: Exception) {
            Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ========== GALERIA ==========
    private fun abrirGaleria() {
        try {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_GALERIA)
        } catch (e: Exception) {
            Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ========== PROCESSAR RESULTADO ==========
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && data != null) {
            val bitmap = when (requestCode) {
                REQUEST_CAMERA -> data.extras?.get("data") as? Bitmap
                REQUEST_GALERIA -> {
                    val uri = data.data
                    MediaStore.Images.Media.getBitmap(contentResolver, uri)
                }
                else -> null
            }

            if (bitmap != null) {
                textoExtraido.text = "Imagem carregada com sucesso!"
                Toast.makeText(this, "Imagem carregada!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}