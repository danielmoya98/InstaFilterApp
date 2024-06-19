package com.example.instafilterapp

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

class DisplayImageActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var cardViewCrop: CardView
    private lateinit var cardViewFilters: CardView
    private lateinit var cardViewRotate: CardView
    private lateinit var cardViewBrightness: CardView
    private lateinit var cardViewRaw: CardView
    private lateinit var cardViewBalance: CardView

    private lateinit var seekBarBrightness: SeekBar
    private lateinit var seekBarFilters: SeekBar
    private lateinit var seekBarRotate: SeekBar
    private lateinit var seekBarCrop: SeekBar
    private lateinit var seekBarRaw: SeekBar
    private lateinit var seekBarBalance: SeekBar

    private lateinit var originalMat: Mat
    private lateinit var processedMat: Mat
    private lateinit var bitmap: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_image)

        // Inicializar OpenCV
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }

        // Obtener referencias de vistas
        imageView = findViewById(R.id.image_view)
        seekBarBrightness = findViewById(R.id.seekBar_brightness)
        seekBarFilters = findViewById(R.id.seekBar_filters)
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

        // Inicializar LinearLayouts
        val llCrop: LinearLayout = findViewById(R.id.ll_crop)
        val llFilters: LinearLayout = findViewById(R.id.ll_filters)
        val llRotate: LinearLayout = findViewById(R.id.ll_rotate)
        val llBrightness: LinearLayout = findViewById(R.id.ll_brightness)
        val llRaw: LinearLayout = findViewById(R.id.ll_raw)
        val llBalance: LinearLayout = findViewById(R.id.ll_balance)

        // Botones para controlar la visibilidad del ScrollView horizontal
        val arrowButton: ImageButton = findViewById(R.id.arrow_button)
        val upButton: ImageButton = findViewById(R.id.up)
        val horizontalScrollView: HorizontalScrollView = findViewById(R.id.horizontal_scroll_view)
        val downButton: ImageButton = findViewById(R.id.down)

        // Configurar onClickListeners para toggle de CardViews
        llCrop.setOnClickListener {
            toggleCardView(cardViewCrop)
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

        llBalance.setOnClickListener {
            toggleCardView(cardViewBalance)
        }

        // Configurar onClickListeners para controlar la visibilidad del ScrollView horizontal
        upButton.setOnClickListener {
            upButton.visibility = View.GONE
            downButton.visibility = View.VISIBLE
            horizontalScrollView.visibility = View.VISIBLE
        }

        downButton.setOnClickListener {
            downButton.visibility = View.GONE
            upButton.visibility = View.VISIBLE
            horizontalScrollView.visibility = View.GONE
        }

        // Configurar onClickListener para el botón de regreso a MainActivity
        arrowButton.setOnClickListener {
            val intent = Intent(this@DisplayImageActivity, MainActivity::class.java)
            startActivity(intent)
        }

        // Obtener la Uri de la imagen seleccionada desde MainActivity
        val photoUriString = intent.getStringExtra("photoUri")
        val photoUri = Uri.parse(photoUriString)

        // Mostrar la imagen en ImageView
        imageView.setImageURI(photoUri)

        // Obtener la Uri del Bitmap seleccionado (si lo hay)
        val imageUri: String? = intent.getStringExtra("imageUri")

        // Decodificar y mostrar el Bitmap en ImageView
        if (imageUri != null) {
            val uri = Uri.parse(imageUri)
            bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
            imageView.setImageBitmap(bitmap)

            // Convertir el Bitmap a Matriz original
            originalMat = Mat()
            Utils.bitmapToMat(bitmap, originalMat)

            // Clonar la Matriz original a la Matriz procesada
            processedMat = originalMat.clone()
        } else {
            Toast.makeText(this, "No image to display", Toast.LENGTH_SHORT).show()
        }

        // Configurar SeekBarChangeListener para el brillo
        seekBarBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                applyBrightnessChange(progress - 50)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Configurar SeekBarChangeListener para los filtros
        seekBarFilters.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                applyFilterChange(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Configurar SeekBarChangeListener para la rotación
        seekBarRotate.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                applyRotationChange(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Configurar onClickListener para el botón de descarga
        findViewById<View>(R.id.download_button).setOnClickListener {
            val success = ImageUtils.saveImageToGallery(this@DisplayImageActivity, imageView, "image_filename.png")
            if (success) {
                Toast.makeText(this@DisplayImageActivity, "Imagen guardada correctamente", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@DisplayImageActivity, "Error al guardar la imagen", Toast.LENGTH_SHORT).show()
            }
        }

        // Configurar onClickListener para el botón de compartir
        findViewById<View>(R.id.share_button).setOnClickListener {
            val imagePath = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)}/InstaFilter/image_filename.png"
            ImageUtils.shareImageFromGallery(this@DisplayImageActivity, imagePath)
        }
    }

    private fun applyBrightnessChange(brightness: Int) {
        originalMat.copyTo(processedMat)
        processedMat.convertTo(processedMat, CvType.CV_8UC1, 1.0, brightness.toDouble())
        updateImageViewFromMat(processedMat)
    }

    private fun applyFilterChange(filterValue: Int) {
        val alpha = 1.0 + (filterValue / 100.0)
        originalMat.copyTo(processedMat)
        processedMat.convertTo(processedMat, -1, alpha, 0.0)
        updateImageViewFromMat(processedMat)
    }

    private fun applyRotationChange(rotationValue: Int) {
        originalMat.copyTo(processedMat)
        val center = org.opencv.core.Point((processedMat.cols() / 2).toDouble(), (processedMat.rows() / 2).toDouble())
        val rotationMatrix = Imgproc.getRotationMatrix2D(center, rotationValue.toDouble(), 1.0)
        Imgproc.warpAffine(processedMat, processedMat, rotationMatrix, processedMat.size())
        updateImageViewFromMat(processedMat)
    }

    private fun updateImageViewFromMat(mat: Mat) {
        // Convertir la matriz procesada a un bitmap
        val resultBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, resultBitmap)

        // Establecer el bitmap en el imageView
        imageView.setImageBitmap(resultBitmap)
    }

    private fun toggleCardView(cardView: CardView) {
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
}
