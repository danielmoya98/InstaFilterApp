package com.example.instafilterapp

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.proyectopdi.OpenUtils
import org.opencv.android.NativeCameraView
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

class DisplayImageActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var cardViewCrop: FrameLayout
    private lateinit var cardViewFilters: FrameLayout
    private lateinit var cardViewRotate: FrameLayout
    private lateinit var cardViewBrightness: FrameLayout
    private lateinit var cardViewRaw: FrameLayout
    private lateinit var cardViewBalance: FrameLayout
    private val REQUEST_WRITE_EXTERNAL_STORAGE = 1
    private var saveImagePending = false
    private lateinit var seekBarBrightness: SeekBar
    private lateinit var seekBarRotate: SeekBar
    private lateinit var seekBarCrop: SeekBar
    private lateinit var seekBarRaw: SeekBar
    private lateinit var seekBarBalance: SeekBar
    private var originalMat: Mat? = null
    private lateinit var processedMat: Mat
    private lateinit var bitmap: Bitmap
    private var openUtils = OpenUtils()
    private lateinit var cascadeClassifier: CascadeClassifier
    lateinit var saveButton: ImageButton
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_image)

        // Inicializa OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "OpenCV initialization failed")
            // Maneja el error de inicialización si es necesario
        }

        // Obtén las referencias de las vistas
        imageView = findViewById(R.id.image_view)
        seekBarBrightness = findViewById(R.id.seekBar_brightness)
        seekBarRotate = findViewById(R.id.seekBar_rotate)
        seekBarCrop = findViewById(R.id.seekBar_crop)
        seekBarRaw = findViewById(R.id.seekBar_raw)
        seekBarBalance = findViewById(R.id.seekBar_balance)

        cardViewCrop = findViewById(R.id.card_view_crop)
        cardViewFilters = findViewById(R.id.card_view_filters)
        cardViewRotate = findViewById(R.id.card_view_rotate)
        cardViewBrightness = findViewById(R.id.card_view_brightness)
        cardViewRaw = findViewById(R.id.card_view_raw)
        cardViewBalance = findViewById(R.id.card_view_balance)

        saveButton = findViewById(R.id.save_button)

        // Inicializa los LinearLayouts
        val llCrop: LinearLayout = findViewById(R.id.ll_crop)
        val llarrow: LinearLayout = findViewById(R.id.arrow_button)
        val llFilters: LinearLayout = findViewById(R.id.ll_filters)
        val llRotate: LinearLayout = findViewById(R.id.ll_rotate)
        val llBrightness: LinearLayout = findViewById(R.id.ll_brightness)
        val llRaw: LinearLayout = findViewById(R.id.ll_raw)

        // Botones para controlar la visibilidad del ScrollView horizontal

        // Establece onClickListeners para alternar los CardViews
        llCrop.setOnClickListener {
            toggleCardView(cardViewCrop)
        }

        llarrow.setOnClickListener {
            val intent = Intent(this@DisplayImageActivity, MainActivity::class.java)
            startActivity(intent)
        }

        llFilters.setOnClickListener {
            toggleCardView(cardViewFilters)
        }

        llRotate.setOnClickListener {
            toggleCardView(cardViewRotate)
        }

        llBrightness.setOnClickListener {
            toggleCardView(cardViewBrightness)
        }

        llRaw.setOnClickListener {
            toggleCardView(cardViewRaw)
        }

        saveButton.setOnClickListener {
            guardarImage()
        }

        // Obtén el Uri de la imagen seleccionada desde MainActivity
        val photoUriString = intent.getStringExtra("photoUri")
        val photoUri = Uri.parse(photoUriString)

        // Muestra la imagen en ImageView
        imageView.setImageURI(photoUri)

        // Obtén el Uri del Bitmap seleccionado (si existe)
        val imageUri: String? = intent.getStringExtra("imageUri")

        // Decodifica y muestra el Bitmap en ImageView
        if (imageUri != null) {
            val uri = Uri.parse(imageUri)
            bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
            imageView.setImageBitmap(bitmap)

            // Convierte el Bitmap a la matriz original
            originalMat = Mat()
            Utils.bitmapToMat(bitmap, originalMat)

            // Clona la matriz original a la matriz procesada
            originalMat?.let {
                processedMat = it.clone()
            } ?: run {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
        } else if (photoUriString != null) {
            // Hay un photoUri proporcionado pero no imageUri (Bitmap)
            bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(photoUri))
            imageView.setImageBitmap(bitmap)

            // Convierte el Bitmap a la matriz original
            originalMat = Mat()
            Utils.bitmapToMat(bitmap, originalMat)

            // Clona la matriz original a la matriz procesada
            originalMat?.let {
                processedMat = it.clone()
            } ?: run {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
        } else {
            // No hay imagen válida para mostrar
            Toast.makeText(this, "No image to display", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Establece SeekBarChangeListener para el brillo
        seekBarBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (originalMat != null) {
                    applyBrightnessChange(progress - 50)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Establece SeekBarChangeListener para los filtros
        seekBarCrop.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (originalMat != null) {
                    applyFilterChange(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Establece SeekBarChangeListener para la rotación
        seekBarRotate.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (originalMat != null) {
                    applyRotationChange(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Establece SeekBarChangeListener para el efecto de viñeta
        seekBarRaw.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (originalMat != null) {
                    val intensity = progress / 100.0 // Convierte el progreso a un valor entre 0 y 1
                    val vignetteMat = applyVignetteEffect(originalMat!!, intensity)
                    // Muestra la imagen con el efecto de viñeta en la vista de imagen
                    val bitmap = Bitmap.createBitmap(
                        vignetteMat.cols(),
                        vignetteMat.rows(),
                        Bitmap.Config.ARGB_8888
                    )
                    Utils.matToBitmap(vignetteMat, bitmap)
                    imageView.setImageBitmap(bitmap)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Establece onClickListener para el botón de compartir
        findViewById<View>(R.id.share_button).setOnClickListener {
            // Primero, guarda la imagen en almacenamiento si no se ha guardado aún
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Si no tenemos permiso, solicítalo
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_WRITE_EXTERNAL_STORAGE
                )
                saveImagePending = true
            } else {
                // Guarda la imagen y luego compártela
                saveAndShareImage()
            }
        }

        // Intenta cargar el archivo de cascada para detección de rostros
        try {
            val inputStream: InputStream =
                resources.openRawResource(R.raw.haarcascade_frontalface_alt)
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
    }
    private fun applyBrightnessChange(brightness: Int) {
        originalMat?.let {
            it.copyTo(processedMat)
            processedMat.convertTo(processedMat, CvType.CV_8UC1, 1.0, brightness.toDouble())
            updateImageViewFromMat(processedMat)
        }
    }

    private fun applyFilterChange(filterValue: Int) {
        originalMat?.let {
            val alpha = 1.0 + (filterValue / 100.0)
            it.copyTo(processedMat)
            processedMat.convertTo(processedMat, -1, alpha, 0.0)
            updateImageViewFromMat(processedMat)
        }
    }

    private fun applyRotationChange(rotationValue: Int) {
        originalMat?.let {
            it.copyTo(processedMat)
            val center = org.opencv.core.Point(
                (processedMat.cols() / 2).toDouble(),
                (processedMat.rows() / 2).toDouble()
            )
            val rotationMatrix = Imgproc.getRotationMatrix2D(center, rotationValue.toDouble(), 1.0)
            Imgproc.warpAffine(processedMat, processedMat, rotationMatrix, processedMat.size())
            updateImageViewFromMat(processedMat)
        }
    }

    private fun updateImageViewFromMat(mat: Mat) {
        // Convertir la matriz procesada a un bitmap
        val resultBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, resultBitmap)

        // Establecer el bitmap en el imageView
        imageView.setImageBitmap(resultBitmap)
    }

    private fun toggleCardView(cardView: FrameLayout) {
        if (cardView.visibility == View.VISIBLE) {
            cardView.visibility = View.GONE
        } else {
            hideAllCardViews()
            cardView.visibility = View.VISIBLE
        }
    }

    private fun hideAllCardViews() {
        cardViewCrop.visibility = View.GONE
        cardViewFilters.visibility = View.GONE
        cardViewRotate.visibility = View.GONE
        cardViewBrightness.visibility = View.GONE
        cardViewRaw.visibility = View.GONE
        cardViewBalance.visibility = View.GONE
    }

    fun onCardClick(view: View) {
        when (view.id) {
            R.id.cv1 -> {
                var bitmapMoment = bitmap.copy(bitmap.config, true)
                imageView.setImageBitmap(bitmapMoment)
            }

            R.id.cv2 -> {
                var bitmapMoment = bitmap.copy(bitmap.config, true)
                var newBitmap = openUtils.filterMotion(bitmapMoment)
                imageView.setImageBitmap(newBitmap)
            }

            R.id.cv3 -> {
                var bitmapMoment = bitmap.copy(bitmap.config, true)
                var newBitmap = openUtils.cannyFiltro(bitmapMoment)
                imageView.setImageBitmap(newBitmap)
            }

            R.id.cv4 -> {
                var bitmapMoment = bitmap.copy(bitmap.config, true)
                var newBitmap = openUtils.applyPixelize(bitmapMoment)
                imageView.setImageBitmap(newBitmap)
            }

            R.id.cv5 -> {
                var bitmapMoment = bitmap.copy(bitmap.config, true)
                var newBitmap = openUtils.applyPosterize(bitmapMoment)
                imageView.setImageBitmap(newBitmap)
            }

            R.id.cv6 -> {
                var bitmapMoment = bitmap.copy(bitmap.config, true)
                var newBitmap = openUtils.applySepia(bitmapMoment)
                imageView.setImageBitmap(newBitmap)
            }

            R.id.cv7 -> {
                var bitmapMoment = bitmap.copy(bitmap.config, true)
                var newBitmap = openUtils.applySobel(bitmapMoment)
                imageView.setImageBitmap(newBitmap)
            }

            R.id.cv8 -> {
                var bitmapMoment = bitmap.copy(bitmap.config, true)
                var newBitmap = openUtils.detectFace(bitmapMoment, cascadeClassifier)
                imageView.setImageBitmap(newBitmap)
            }

            R.id.cv9 -> {
                var bitmapMoment = bitmap.copy(bitmap.config, true)
                var newBitmap = openUtils.applyDogFilter(bitmapMoment, cascadeClassifier, this)
                imageView.setImageBitmap(newBitmap)
            }

            R.id.cv10 -> {
                var bitmapMoment = bitmap.copy(bitmap.config, true)
                var newBitmap = openUtils.filterContours(bitmapMoment)
                imageView.setImageBitmap(newBitmap)
            }

            R.id.cv11 -> {
                var bitmapMoment = bitmap.copy(bitmap.config, true)
                var newBitmap = openUtils.filterBlur(bitmapMoment, "")
                imageView.setImageBitmap(newBitmap)
            }

            R.id.cv12 -> {
                var bitmapMoment = bitmap.copy(bitmap.config, true)
                var newBitmap = openUtils.filterSkin(bitmapMoment)
                imageView.setImageBitmap(newBitmap)
            }

            R.id.cv13 -> {
                var bitmapMoment = bitmap.copy(bitmap.config, true)
                var newBitmap = openUtils.filterEqualize(bitmapMoment)
                imageView.setImageBitmap(newBitmap)
            }

            R.id.cv14 -> {
                var bitmapMoment = bitmap.copy(bitmap.config, true)
                var newBitmap = openUtils.filterClahe(bitmapMoment)
                imageView.setImageBitmap(newBitmap)
            }

            R.id.cv15 -> {
                var bitmapMoment = bitmap.copy(bitmap.config, true)
                var newBitmap = openUtils.filterLab(bitmapMoment)
                imageView.setImageBitmap(newBitmap)
            }

            R.id.cv16 -> {
                var bitmapMoment = bitmap.copy(bitmap.config, true)
                var newBitmap = openUtils.filterSobelX(bitmapMoment)
                imageView.setImageBitmap(newBitmap)
            }

            R.id.cv17 -> {
                var bitmapMoment = bitmap.copy(bitmap.config, true)
                var newBitmap = openUtils.filterSobelY(bitmapMoment)
                imageView.setImageBitmap(newBitmap)
            }

            R.id.cv18 -> {
                var bitmapMoment = bitmap.copy(bitmap.config, true)
                var newBitmap = openUtils.filter3Bits(bitmapMoment)
                imageView.setImageBitmap(newBitmap)
            }

            R.id.cv19 -> {
                var bitmapMoment = bitmap.copy(bitmap.config, true)
                var newBitmap = openUtils.filterMaxRgb(bitmapMoment)
                imageView.setImageBitmap(newBitmap)
            }

            R.id.cv20 -> {
                var bitmapMoment = bitmap.copy(bitmap.config, true)
                var newBitmap = openUtils.filterChaoticRgb(bitmapMoment)
                imageView.setImageBitmap(newBitmap)
            }

            R.id.cv21 -> {
                var bitmapMoment = bitmap.copy(bitmap.config, true)
                var newBitmap = openUtils.anonymizeFacePixelate(bitmapMoment)
                imageView.setImageBitmap(newBitmap)
            }
        }
    }

    private fun applyVignetteEffect(originalMat: Mat, intensity: Double): Mat {
        // Paso 1: Convertir la imagen original a escala de grises
        val grayMat = Mat(originalMat.size(), CvType.CV_8UC1)
        Imgproc.cvtColor(originalMat, grayMat, Imgproc.COLOR_RGB2GRAY)

        // Paso 2: Calcular el centro de la imagen
        val center = Point(originalMat.cols() / 2.0, originalMat.rows() / 2.0)

        // Paso 3: Calcular los parámetros de la viñeta
        val maxDistance = Math.sqrt(Math.pow(center.x, 2.0) + Math.pow(center.y, 2.0)).toFloat()
        val result = Mat(originalMat.size(), CvType.CV_8UC1)

        // Paso 4: Aplicar el efecto de viñeta
        for (y in 0 until originalMat.rows()) {
            for (x in 0 until originalMat.cols()) {
                val distance = Math.sqrt(
                    Math.pow(
                        center.x - x.toDouble(),
                        2.0
                    ) + Math.pow(center.y - y.toDouble(), 2.0)
                )
                val vignette = 1.0 - Math.pow(distance / maxDistance, intensity)
                val pixelValue = grayMat.get(y, x)[0] * vignette
                result.put(y, x, pixelValue)
            }
        }

        return result
    }

    private fun guardarImage() {
        // Verificar si tenemos permiso de escritura
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Si no tenemos permiso, solicitarlo
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_WRITE_EXTERNAL_STORAGE
            )
        } else {
            // Guardar la imagen
            saveImageToStorage()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido, guardar la imagen
                saveImageToStorage()
            } else {
                // Permiso denegado, mostrar un mensaje al usuario
                Toast.makeText(
                    this,
                    "Se requiere permiso de escritura para guardar imágenes",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun saveImageToStorage() {
        val bitmapToSave = (imageView.drawable as BitmapDrawable).bitmap

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/InstaFilterApp")
            }
        }

        val uri =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            val outputStream = contentResolver.openOutputStream(it)
            if (outputStream != null) {
                bitmapToSave.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
            outputStream?.close()

            // Notificar a la galería sobre la nueva imagen
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            intent.data = uri
            sendBroadcast(intent)

            Toast.makeText(this, "Imagen guardada", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(this, "Error al guardar la imagen", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    private fun saveAndShareImage() {
        val bitmapToSave = (imageView.drawable as BitmapDrawable).bitmap

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/InstaFilterApp")
            }
        }

        val uri =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            val outputStream = contentResolver.openOutputStream(it)
            if (outputStream != null) {
                bitmapToSave.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
            outputStream?.close()

            // Notificar a la galería sobre la nueva imagen
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            intent.data = uri
            sendBroadcast(intent)

            // Compartir la imagen
            shareImage(uri)
        } ?: run {
            Toast.makeText(this, "Error al guardar la imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareImage(imageUri: Uri) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, imageUri)
            type = "image/jpeg"
        }
        startActivity(Intent.createChooser(shareIntent, "Compartir Imagen"))
    }

}

