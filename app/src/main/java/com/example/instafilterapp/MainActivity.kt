package com.example.instafilterapp

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaPlayer
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
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.instafilterapp.databinding.ActivityMainBinding
import com.example.proyectopdi.OpenUtils
import com.google.android.material.button.MaterialButton
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
    // Vinculación con activity_main.xml
    private lateinit var viewBinding: ActivityMainBinding

    // Envoltorios opcionales para captura de imagen y video
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    // Servicio de ejecución para operaciones de cámara en un hilo de fondo
    private lateinit var cameraExecutor: ExecutorService

    // Envoltorio opcional para análisis de imagen
    private var imageAnalysis: ImageAnalysis? = null

    // Instancia de clase de utilidad
    private var openUtils = OpenUtils()

    // Clasificadores en cascada para detección de rostros y ojos
    private lateinit var cascadeClassifier: CascadeClassifier
    private lateinit var cascadeClassifier1: CascadeClassifier
    private lateinit var cascadeClassifier_eye: CascadeClassifier

    // Componentes de la interfaz de usuario
    private lateinit var mImageButton: ImageButton
    private var contadorActivo = false

    // Configuraciones de filtro y rotación
    private var filtroNum: Int = 0
    private var rotar = CameraSelector.DEFAULT_FRONT_CAMERA

    // TextView y botón de la interfaz de usuario
    private lateinit var contadorTextView: TextView
    private lateinit var clockButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Infla y establece el diseño de la actividad principal
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Verifica si todos los permisos han sido otorgados, de lo contrario, los solicita
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Configura el botón de captura
        val captureButton: MaterialButton = findViewById(R.id.btn_capture)
        mImageButton = findViewById(R.id.toggle_button)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Inicializa OpenCV localmente
        OpenCVLoader.initLocal()

        // Configura el botón de cuadrícula para alternar su visibilidad
        val btnGrid: ImageButton = findViewById(R.id.btnGrid)
        btnGrid.setOnClickListener {
            val gridOverlay: View = findViewById(R.id.gridOverlay)
            gridOverlay.visibility = if (gridOverlay.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        contadorTextView = findViewById(R.id.contador)
        clockButton = findViewById(R.id.clock)
        clockButton.setOnClickListener {
            clockButton.isSelected = !clockButton.isSelected
            contadorActivo = clockButton.isSelected
            Toast.makeText(this, if (contadorActivo) "El contador está activado" else "El contador está desactivado", Toast.LENGTH_SHORT).show()
        }

        // Botón para cambiar entre cámara frontal y trasera
        val rotateCameraButton: ImageButton = findViewById(R.id.rotar_camara)
        rotateCameraButton.setOnClickListener {
            rotar = if (rotar == CameraSelector.DEFAULT_FRONT_CAMERA) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
            startCamera()
        }

        captureButton.setOnClickListener {
            if (!contadorActivo) {
                takePhoto()
            } else {
                startCountdownAnimation()
                contarTresSegundos()
            }
        }

        // Botón para abrir la galería
        val galleryButton: ImageButton = findViewById(R.id.gallery_button)
        galleryButton.setOnClickListener { openGallery() }

        // Carga e inicializa archivos de cascada para detección de rostros
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
            Log.i(NativeCameraView.TAG, "Archivo de cascada no encontrado")
        }
    }


    // Anotación para indicar el uso de características experimentales de la API CameraX
    @OptIn(ExperimentalGetImage::class)
    private fun startCamera() {
        // Obtiene el proveedor de la cámara de forma asíncrona
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        // Agrega un listener que se ejecutará cuando el proveedor de cámara esté disponible
        cameraProviderFuture.addListener({
            // Accede al proveedor de la cámara una vez que esté disponible
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Configura y construye el caso de uso para captura de imágenes
            imageCapture = ImageCapture.Builder().build()

            // Configura y construye el caso de uso para análisis de imágenes
            imageAnalysis = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { image ->
                        // Convierte la imagen capturada a un objeto Bitmap
                        val bitmap: Bitmap? = BitmapUtils.getBitmap(image)

                        // Ejecuta operaciones en el hilo principal de la interfaz de usuario
                        runOnUiThread {
                            if (bitmap != null) {
                                // Aplica un filtro según el número de filtro seleccionado
                                var newBitmap = when (filtroNum) {
                                    1 -> openUtils.applyDogFilter(bitmap, cascadeClassifier, this)
                                    2 -> openUtils.variableThreshold(bitmap)
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
                                    17 -> openUtils.filterContours(bitmap)
                                    18 -> openUtils.filterBlur(bitmap, "")
                                    19 -> openUtils.filterSkin(bitmap)
                                    20 -> openUtils.filterEqualize(bitmap)
                                    21 -> openUtils.filterClahe(bitmap)
                                    22 -> openUtils.filterLab(bitmap)
                                    23 -> openUtils.filterSobelX(bitmap)
                                    24 -> openUtils.filterSobelY(bitmap)
                                    25 -> openUtils.filter3Bits(bitmap)
                                    26 -> openUtils.filterMaxRgb(bitmap)
                                    27 -> openUtils.filterChaoticRgb(bitmap)
                                    28 -> openUtils.anonymizeFacePixelate(bitmap)
                                    else -> bitmap
                                }

                                // Si la cámara está configurada para la cámara frontal, espejea la imagen
                                if(rotar == CameraSelector.DEFAULT_FRONT_CAMERA){
                                    newBitmap = Bitmap.createBitmap(newBitmap, 0, 0, newBitmap.width, newBitmap.height, Matrix().apply { postScale(-1f, 1f) }, true)
                                }

                                // Establece el bitmap en el ImageView del layout
                                viewBinding.viewImage.setImageBitmap(newBitmap)

                                // Guarda el bitmap filtrado
                                filteredBitmap = newBitmap
                            } else {
                                // Registra un error si el bitmap es nulo
                                Log.e(TAG, "El bitmap es nulo")
                            }
                        }
                        // Cierra la imagen para liberar recursos
                        image.close()
                    }
                }

            // Establece el selector de la cámara basado en la configuración actual
            val cameraSelector = rotar

            try {
                // Desvincula todos los casos de uso actuales y los vuelve a vincular
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, imageAnalysis, imageCapture)
            } catch (exc: Exception) {
                // Registra un error si hay problemas al vincular los casos de uso
                Log.e(TAG, "Error al vincular casos de uso", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // Variable para almacenar el bitmap filtrado
    private var filteredBitmap: Bitmap? = null

    private fun takePhoto() {
        // Obtiene el bitmap filtrado para guardar; si es nulo, termina la función
        val bitmapToSave = filteredBitmap ?: return

        // Formatea el nombre del archivo con la fecha y hora actual
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            // Añade la ruta relativa si la versión de Android es posterior a Pie
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Inserta la imagen en el almacenamiento externo y obtiene una URI
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            // Abre un flujo de salida para guardar la imagen
            val outputStream = contentResolver.openOutputStream(it)
            if (outputStream != null) {
                // Comprime y guarda la imagen en formato JPEG
                bitmapToSave.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream?.close()
            }

            // Reproduce el sonido del obturador de la cámara
            playShutterSound()

            // Resetea el número de filtro y inicia una actividad para mostrar la imagen
            filtroNum = 0
            startDisplayImageActivity(it)
        }
    }


    // Método para reproducir el sonido del obturador de la cámara
    private fun playShutterSound() {
        val mediaPlayer = MediaPlayer.create(this, R.raw.camera)
        mediaPlayer.start()  // Inicia el sonido
        mediaPlayer.setOnCompletionListener {
            it.release()  // Libera el recurso una vez que el sonido ha terminado
        }
    }

    // Método para iniciar una actividad que muestra una imagen a partir de su URI
    private fun startDisplayImageActivity(photoUri: Uri) {
        val intent = Intent(this, DisplayImageActivity::class.java)
        intent.putExtra("photoUri", photoUri.toString())  // Pasa la URI como extra
        startActivity(intent)  // Inicia la actividad
    }

    // Método para abrir la galería y seleccionar una imagen
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, 101)  // Inicia una actividad con resultado para seleccionar una imagen
    }

    // Método para verificar si todos los permisos necesarios han sido concedidos
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    // Se llama cuando la actividad es destruida, se utiliza para cerrar recursos
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()  // Cierra el executor de la cámara
    }

    // Objeto companion que contiene constantes utilizadas en la clase
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
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)  // Agrega permiso de escritura si es necesario
                }
            }.toTypedArray()
    }

    // Se llama cuando el usuario responde a la solicitud de permisos
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()  // Inicia la cámara si todos los permisos fueron concedidos
            } else {
                Toast.makeText(this,
                    "Permisos no concedidos por el usuario.",
                    Toast.LENGTH_SHORT).show()
                finish()  // Cierra la aplicación si los permisos no fueron concedidos
            }
        }
    }

    // Se llama cuando una actividad iniciada con startActivityForResult termina
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == RESULT_OK) {
            val selectedImageUri: Uri? = data?.data  // Obtiene la URI de la imagen seleccionada
            if (selectedImageUri != null) {
                startDisplayImageActivity(selectedImageUri)  // Inicia la actividad para mostrar la imagen
            } else {
                Toast.makeText(this, "No se ha seleccionado ninguna imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Gestiona los clics en las tarjetas que cambian el filtro de la imagen
    fun onCardClicked(view: View) {
        when (view.id) {
            R.id.card_view_1 -> filtroNum = 0
            R.id.card_view_2 -> filtroNum = 16
            R.id.card_view_3 -> filtroNum = 6
            R.id.card_view_4 -> filtroNum = 7
            R.id.card_view_5 -> filtroNum = 8
            R.id.card_view_6 -> filtroNum = 10
            R.id.card_view_7 -> filtroNum = 11
            R.id.card_view_8 -> filtroNum = 17
            R.id.card_view_9 -> filtroNum = 18


            R.id.card_view_10 -> filtroNum = 19
            R.id.card_view_11 -> filtroNum = 20
            R.id.card_view_11 -> filtroNum = 21
            R.id.card_view_12 -> filtroNum = 22
            R.id.card_view_13 -> filtroNum = 23
            R.id.card_view_14 -> filtroNum = 24
            R.id.card_view_15 -> filtroNum = 25
            R.id.card_view_16 -> filtroNum = 26
            R.id.card_view_17 -> filtroNum = 27
            R.id.card_view_18 -> filtroNum = 28
        }
    }

    // Inicia una animación de cuenta regresiva
    private fun startCountdownAnimation() {
        var count = 3
        contadorTextView.text = count.toString()
        contadorTextView.visibility = View.VISIBLE
        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        fadeInAnimation.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}
            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                countdown(count)  // Inicia la cuenta regresiva al finalizar la animación
            }
            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })
        contadorTextView.startAnimation(fadeInAnimation)
    }

    // Realiza la cuenta regresiva
    private fun countdown(count: Int) {
        var currentCount = count
        if (currentCount > 0) {
            contadorTextView.text = currentCount.toString()
            val fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out)
            fadeOutAnimation.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(animation: android.view.animation.Animation?) {}
                override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                    countdown(currentCount - 1)  // Continúa la cuenta regresiva
                }
                override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
            })
            contadorTextView.startAnimation(fadeOutAnimation)
        } else {
            contadorTextView.visibility = View.INVISIBLE  // Oculta el contador al finalizar
        }
    }

    // Contador de tres segundos antes de tomar una foto
    private fun contarTresSegundos() {
        object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                takePhoto()  // Toma una foto al finalizar el contador
            }
        }.start()
    }












}
