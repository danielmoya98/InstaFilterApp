package com.example.instafilterapp

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageButton
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
    private var isFlashOn = false
    private var lensFacing = CameraSelector.LENS_FACING_BACK

    private var filtroNum: Int = 0
    private var rotar = CameraSelector.DEFAULT_FRONT_CAMERA

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)


        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        val captureButton: MaterialButton = findViewById(R.id.btn_capture)

        mImageButton = findViewById(R.id.toggle_button)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if(OpenCVLoader.initLocal()) {
            Toast.makeText(this, "load", Toast.LENGTH_SHORT).show()
        }
        else {
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

        mImageButton.setOnClickListener {
            toggleFlash()
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
            takePhoto()
        }

        val horizontalScrollView: HorizontalScrollView = findViewById(R.id.horizontal_scroll_view)


        val galleryButton: ImageButton = findViewById(R.id.gallery_button)
        galleryButton.setOnClickListener { openGallery() }

        val cardView1: MaterialCardView = findViewById(R.id.card_view_1)





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
        }
        catch (e: IOException){
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


        }
        catch (e: IOException){
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
        }
        catch (e: IOException){
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
                                    1 -> openUtils.applyDogFilter(bitmap, cascadeClassifier,this)
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
                                    16 -> openUtils.cannyFiltroBlanco(bitmap)
                                    else -> bitmap
                                }

                                viewBinding.viewImage.setImageBitmap(newBitmap)

                                // Aquí guardamos el bitmap con el filtro aplicado en una variable global
                                filteredBitmap = newBitmap
                            } else {
                                Log.e(TAG, "Grayscale bitmap is null")
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
                Log.e(TAG, "Fallo al vincular casos de uso", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private var filteredBitmap: Bitmap? = null

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

            val msg = "Photo capture succeeded: $it"
            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
            Log.d(TAG, msg)
        }
    }

    private fun toggleFlash() {
        imageCapture?.let {
            isFlashOn = !isFlashOn
            Log.d("CameraX", "Flash toggled, isFlashOn: $isFlashOn")
            it.flashMode = if (isFlashOn) {
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
            mutableListOf (
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
                    "Permissions not granted by the user.",
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
                val intent = Intent(this, DisplayImageActivity::class.java)
                intent.putExtra("imageUri", selectedImageUri.toString())
                startActivity(intent)
            } else {
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun onCardClicked(view: View) {
        when (view.id) {
            R.id.card_view_1 -> filtroNum = 7
            R.id.card_view_2 -> filtroNum = 1

        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


}
