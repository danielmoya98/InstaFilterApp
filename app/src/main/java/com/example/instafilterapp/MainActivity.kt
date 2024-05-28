package com.example.instafilterapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.cardview.widget.CardView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private val CAMERA_PERMISSION_CODE = 100
    private val PICK_IMAGE_REQUEST = 101

    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var mImageButton: ImageButton
    private var isScrollViewVisible = false
    private var isFlashOn = false
    private var lensFacing = CameraSelector.LENS_FACING_BACK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val previewView: PreviewView = findViewById(R.id.camera_preview)
        val captureButton: MaterialButton = findViewById(R.id.btn_capture)
        mImageButton = findViewById(R.id.toggle_button)

        cameraExecutor = Executors.newSingleThreadExecutor()

        mImageButton.setOnClickListener {
            toggleFlash()
        }

        val rotateCameraButton: ImageButton = findViewById(R.id.rotar_camara)
        rotateCameraButton.setOnClickListener {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
            startCamera(previewView)
        }

        captureButton.setOnClickListener {
            takePhoto()
        }

        val horizontalScrollView: HorizontalScrollView = findViewById(R.id.horizontal_scroll_view)


        val galleryButton: ImageButton = findViewById(R.id.gallery_button)
        galleryButton.setOnClickListener { openGallery() }

        val cardView1: MaterialCardView = findViewById(R.id.card_view_1)
        val cardView2: MaterialCardView = findViewById(R.id.card_view_2)
        val cardView3: MaterialCardView = findViewById(R.id.card_view_3)
        val cardView4: MaterialCardView = findViewById(R.id.card_view_4)


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        } else {
            startCamera(previewView)
        }
    }

    private fun startCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().apply {
                setFlashMode(if (isFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
            }.build()

            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e("CameraX", "Error al iniciar la cámara: ${exc.message}")
            }
        }, ContextCompat.getMainExecutor(this))
        updateFlashIcon()
    }

    private fun takePhoto() {
        val photoFile = File(externalMediaDirs.firstOrNull(), "${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraX", "Error al tomar la foto: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val intent = Intent(this@MainActivity, DisplayImageActivity::class.java)
                    intent.putExtra("imageUri", savedUri)
                    startActivity(intent)
                }
            })
    }

    private fun toggleFlash() {
        if (this::imageCapture.isInitialized) {
            isFlashOn = !isFlashOn
            Log.d("CameraX", "Flash toggled, isFlashOn: $isFlashOn")
            imageCapture.flashMode = if (isFlashOn) {
                ImageCapture.FLASH_MODE_ON
            } else {
                ImageCapture.FLASH_MODE_OFF
            }
            updateFlashIcon()
        }
    }

    private fun updateFlashIcon() {
        val flashIcon = if (isFlashOn) {
            R.drawable.fon
        } else {
            R.drawable.foff
        }
        mImageButton.setImageResource(flashIcon)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            val selectedImage = data.data
            val intent = Intent(this@MainActivity, DisplayImageActivity::class.java)
            intent.putExtra("imageUri", selectedImage)
            startActivity(intent)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val previewView: PreviewView = findViewById(R.id.camera_preview)
                startCamera(previewView)
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
