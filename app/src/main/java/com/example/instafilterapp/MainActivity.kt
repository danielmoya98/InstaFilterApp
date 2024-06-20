package com.example.instafilterapp

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.cardview.widget.CardView
import com.example.instafilterapp.databinding.ActivityMainBinding
import com.example.proyectopdi.OpenUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import org.opencv.android.NativeCameraView
import org.opencv.android.OpenCVLoader
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private lateinit var cameraExecutor: ExecutorService
    private var imageAnalysis: ImageAnalysis? = null
    private var openUtils = OpenUtils()

    private lateinit var cascadeClassifier: CascadeClassifier
    private lateinit var cascadeClassifier1: CascadeClassifier
    private lateinit var cascadeClassifier_eye: CascadeClassifier

    private lateinit var mImageButton: ImageButton
    private var isScrollViewVisible = false
    private var contadorActivo = false
    private var isFlashOn = false

    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private lateinit var horizontalScrollView: HorizontalScrollView
    private var filtroNum: Int = 0
    private var rotar = CameraSelector.DEFAULT_FRONT_CAMERA

    private lateinit var contadorTextView: TextView
    private lateinit var clockButton: ImageButton
    private var photoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        val toggleButton = findViewById<ImageButton>(R.id.toggle_button)
        val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList[0]

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        val captureButton: MaterialButton = findViewById(R.id.btn_capture)



        mImageButton = findViewById(R.id.toggle_button)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (OpenCVLoader.initLocal()) {
            Toast.makeText(this, "load", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "fail", Toast.LENGTH_SHORT).show()
        }

        val btnGrid: ImageButton = findViewById(R.id.btnGrid)
        btnGrid.setOnClickListener {
            val gridOverlay: View = findViewById(R.id.gridOverlay)
            if (gridOverlay.visibility == View.VISIBLE) {
                gridOverlay.visibility = View.GONE
            } else {
                gridOverlay.visibility = View.VISIBLE
            }
        }

        contadorTextView = findViewById(R.id.contador)
        clockButton = findViewById(R.id.clock)

        clockButton.setOnClickListener {
            // Invierte el estado actual del ImageButton
            clockButton.isSelected = !clockButton.isSelected

            if (clockButton.isSelected) {
                // Cuando está activado
                contadorActivo = true
                Toast.makeText(this, "El contador está activado", Toast.LENGTH_SHORT).show()
            } else {
                // Cuando está desactivado
                contadorActivo = false
                Toast.makeText(this, "El contador está desactivado", Toast.LENGTH_SHORT).show()
            }
        }

        toggleButton.setOnClickListener {
            isFlashOn = !isFlashOn
            toggleFlash(cameraManager, cameraId, isFlashOn)
            toggleButton.isSelected = isFlashOn
        }

        val rotateCameraButton: ImageButton = findViewById(R.id.rotar_camara)

        rotateCameraButton.setOnClickListener {
            if (rotar == CameraSelector.DEFAULT_FRONT_CAMERA) {
                rotar = CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                rotar = CameraSelector.DEFAULT_FRONT_CAMERA
            }
            startCamera()
        }

        captureButton.setOnClickListener {
            if(contadorActivo == false){
                takePhoto()
            }
            else{
                startCountdownAnimation()
                contarTresSegundos()
            }
        }

        val galleryButton: ImageButton = findViewById(R.id.gallery_button)
        galleryButton.setOnClickListener { openGallery() }

        try {
            val inputStream: InputStream = resources.openRawResource(R.raw.haarcascade_frontalface_alt)
            val cascadeDir: File = getDir("cascade", Context.MODE_PRIVATE)
            val mCascadeFile: File = File(cascadeDir, "haarcascade_frontalface_alt.xml")
            val os: FileOutputStream = FileOutputStream(mCascadeFile)
            val buffer: ByteArray = ByteArray(4096)
            var byteRead: Int
            while (inputStream.read(buffer).also { byteRead = it } != -1) {
                os.write(buffer, 0, byteRead)
            }
            inputStream.close()
            os.close()

            cascadeClassifier = CascadeClassifier(mCascadeFile.absolutePath)
        } catch (e: IOException) {
            Log.i(NativeCameraView.TAG, "Cascade file not found")
        }

        try {
            val inputStream: InputStream = resources.openRawResource(R.raw.haarcascade_frontalface_default)
            val file: File = File(getDir("cascade", Context.MODE_PRIVATE), "haarcascade_frontalface_default.xml")
            val fileOutputStream: FileOutputStream = FileOutputStream(file)

            val data: ByteArray = ByteArray(4096)

            var read_bytes: Int

            while (inputStream.read(data).also { read_bytes = it } != -1) {
                fileOutputStream.write(data, 0, read_bytes)
            }
            cascadeClassifier1 = CascadeClassifier(file.absolutePath)

            inputStream.close()
            fileOutputStream.close()
            file.delete()

        } catch (e: IOException) {
            Log.i(NativeCameraView.TAG, "Cascade file not found")
        }

        try {
            val inputStream2: InputStream = resources.openRawResource(R.raw.haarcascade_eye)
            val cascadeDir: File = getDir("cascade", Context.MODE_PRIVATE)
            val mCascadeFile_eye: File = File(cascadeDir, "haarcascade_eye.xml")
            val os2: FileOutputStream = FileOutputStream(mCascadeFile_eye)
            val buffer1: ByteArray = ByteArray(4096)
            var byteRead1: Int
            while (inputStream2.read(buffer1).also { byteRead1 = it } != -1) {
                os2.write(buffer1, 0, byteRead1)
            }
            inputStream2.close()
            os2.close()

            cascadeClassifier_eye = CascadeClassifier(mCascadeFile_eye.absolutePath)
        } catch (e: IOException) {
            Log.i(NativeCameraView.TAG, "Cascade file not found")
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            imageCapture = ImageCapture.Builder().build()

            imageAnalysis = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { image ->
                        val bitmap: Bitmap? = BitmapUtils.getBitmap(image)

                        runOnUiThread {
                            if (bitmap != null) {
                                var newBitmap = when (filtroNum) {
                                    1 -> openUtils.applyDogFilter(bitmap, cascadeClassifier, this)
                                    2 -> openUtils.variableThreshold(bitmap!!)
                                    3 -> openUtils.detectEdges(bitmap)
                                    4 -> openUtils.detectFace(bitmap, cascadeClassifier)
                                    5 -> openUtils.detectFaceEye(bitmap, cascadeClassifier, cascadeClassifier_eye)
                                    6 -> openUtils.cannyFiltro(bitmap)
                                    7 -> openUtils.applyPixelize(bitmap)
                                    8 -> openUtils.applyPosterize(bitmap)
                                    9 -> openUtils.applyZoom(bitmap)
                                    10 -> openUtils.applySepia(bitmap)
                                    11 -> openUtils.applySobel(bitmap)
                                    12 -> openUtils.cerradura(bitmap)
                                    13 -> openUtils.processImage(bitmap)
                                    14 -> openUtils.blurBackground(bitmap, cascadeClassifier1)
                                    15 -> openUtils.cambiarColorIris(bitmap, cascadeClassifier_eye)
                                    16 -> openUtils.filterMotion(bitmap)
                                    else -> bitmap
                                }

                                viewBinding.viewImage.setImageBitmap(newBitmap)

                                // Guardamos el bitmap con el filtro aplicado en una variable global
                                filteredBitmap = newBitmap
                            } else {
                                Log.e(TAG, "El bitmap es nulo")
                            }
                        }
                        image.close()
                    }
                }

            val cameraSelector = rotar

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, imageAnalysis, imageCapture)
            } catch (exc: Exception) {
                Log.e(TAG, "Error al vincular casos de uso", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private var filteredBitmap: Bitmap? = null

//    private fun takePhoto() {
//        val bitmapToSave = filteredBitmap ?: return
//
//        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
//            .format(System.currentTimeMillis())
//        val contentValues = ContentValues().apply {
//            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
//            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
//            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
//                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
//            }
//        }
//
//        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
//        uri?.let {
//            val outputStream = contentResolver.openOutputStream(it)
//            if (outputStream != null) {
//                bitmapToSave.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
//            }
//            outputStream?.close()
//
//            val msg = "Captura de foto exitosa: $it"
//            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
//            Log.d(TAG, msg)
//        }
//    }


    private fun takePhoto() {
        val bitmapToSave = filteredBitmap ?: return

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            val outputStream = contentResolver.openOutputStream(it)
            if (outputStream != null) {
                bitmapToSave.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
            outputStream?.close()

            // Llamar a la función para iniciar la otra actividad con la URI de la foto
            filtroNum = 0
            startDisplayImageActivity(it)
        }
    }

    private fun startDisplayImageActivity(photoUri: Uri) {
        val intent = Intent(this, DisplayImageActivity::class.java)
        intent.putExtra("photoUri", photoUri.toString())
        startActivity(intent)
    }


    private fun getRearCameraId(cameraManager: CameraManager): String? {
        try {
            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val cameraFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (cameraFacing != null && cameraFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    return cameraId
                }
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        return null
    }

    private fun toggleFlash(cameraManager: CameraManager, cameraId: String?, turnOn: Boolean) {
        if (cameraId != null) {
            try {
                cameraManager.setTorchMode(cameraId, turnOn)
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
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
        startActivityForResult(intent, 101)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permisos no concedidos por el usuario.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == RESULT_OK) {
            val selectedImageUri: Uri? = data?.data
            if (selectedImageUri != null) {
                startDisplayImageActivity(selectedImageUri)
            } else {
                Toast.makeText(this, "No se ha seleccionado ninguna imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }


    fun onCardClicked(view: View) {
        when (view.id) {
            R.id.card_view_1 -> filtroNum = 0
            R.id.card_view_2 -> filtroNum = 16
            R.id.card_view_3 -> filtroNum = 6
            R.id.card_view_4 -> filtroNum = 7
            R.id.card_view_5 -> filtroNum = 8
            R.id.card_view_6 -> filtroNum = 10
            R.id.card_view_7 -> filtroNum = 11
            R.id.card_view_8 -> filtroNum = 4
            R.id.card_view_9 -> filtroNum = 1
        }
    }



    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    private fun startCountdownAnimation() {
        var count = 3
        contadorTextView.text = count.toString()
        contadorTextView.visibility = View.VISIBLE

        // Animación fade in
        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        fadeInAnimation.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {
                // No es necesario implementar este método
            }

            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                // Lógica para iniciar el conteo regresivo
                countdown(count)
            }

            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {
                // No es necesario implementar este método
            }
        })
        contadorTextView.startAnimation(fadeInAnimation)
    }

    private fun countdown(count: Int) {
        var currentCount = count

        if (currentCount > 0) {
            // Actualizar el texto del contador
            contadorTextView.text = currentCount.toString()

            // Animación fade out
            val fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out)
            fadeOutAnimation.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(animation: android.view.animation.Animation?) {
                    // No es necesario implementar este método
                }

                override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                    // Llamar recursivamente para el siguiente número
                    countdown(currentCount - 1)
                }

                override fun onAnimationRepeat(animation: android.view.animation.Animation?) {
                    // No es necesario implementar este método
                }
            })
            contadorTextView.startAnimation(fadeOutAnimation)
        } else {
            // Ocultar el TextView cuando el conteo regresivo termina
            contadorTextView.visibility = View.INVISIBLE
        }
    }


    private fun contarTresSegundos() {
        // Inicializar el contador con 3 segundos (3000 milisegundos)
        object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Actualiza el UI con el tiempo restante si lo necesitas
            }

            override fun onFinish() {
                // Cuando el contador termine, toma la foto
                takePhoto()
            }
        }.start()
    }
}
