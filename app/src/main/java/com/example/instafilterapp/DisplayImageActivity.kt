package com.example.instafilterapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.github.furkankaplan.fkblurview.FKBlurView


class DisplayImageActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private var isScrollViewVisible = false
    private lateinit var cardViewCrop: CardView
    private lateinit var cardViewFilters: CardView
    private lateinit var cardViewRotate: CardView
    private lateinit var cardViewBrightness: CardView
    private lateinit var cardViewRaw: CardView
    private lateinit var cardViewBalance: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_image)

        imageView = findViewById(R.id.image_view)
        // Inicializar CardViews
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

        val arrowButton: ImageButton = findViewById(R.id.arrow_button)
        val upButton: ImageButton = findViewById(R.id.up)
        val horizontalScrollView: HorizontalScrollView = findViewById(R.id.horizontal_scroll_view)
        val downButton: ImageButton = findViewById(R.id.down)

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
//        val blurView = findViewById<FKBlurView>(R.id.glass)
//        blurView.setBlur(this,blurView)

        // Establecer OnClickListener para el botón
        arrowButton.setOnClickListener {
            // Crear un Intent para iniciar la MainActivity
            val intent = Intent(this@DisplayImageActivity, MainActivity::class.java)
            startActivity(intent)
        }

        val detailedCardView: CardView = findViewById(R.id.detailed_card_view)
//        val btnGlass: MaterialButton = findViewById(R.id.btn_glass)


//        btnGlass.setOnClickListener {
//            isScrollViewVisible = !isScrollViewVisible
//            horizontalScrollView.visibility = if (isScrollViewVisible) View.VISIBLE else View.GONE
//            detailedCardView.visibility = if (isScrollViewVisible) View.GONE else View.VISIBLE
//        }

        // Handling MaterialCardView clicks
//        val cardView1: MaterialCardView = findViewById(R.id.card_view_1)
//        val cardView2: MaterialCardView = findViewById(R.id.card_view_2)
//        val cardView3: MaterialCardView = findViewById(R.id.card_view_3)
//        val cardView4: MaterialCardView = findViewById(R.id.card_view_4)

//        val onClickListener = View.OnClickListener {
//            horizontalScrollView.visibility = View.GONE
//            detailedCardView.visibility = View.VISIBLE
//        }

//        cardView1.setOnClickListener(onClickListener)
//        cardView2.setOnClickListener(onClickListener)
//        cardView3.setOnClickListener(onClickListener)
//        cardView4.setOnClickListener(onClickListener)

        val imageUri: String? = intent.getStringExtra("imageUri")

        if (imageUri != null) {
            val uri = Uri.parse(imageUri)
            imageView.setImageURI(uri)
        } else {
            Toast.makeText(this, "No image to display", Toast.LENGTH_SHORT).show()
        }

        // Agregar funcionalidad al botón de descarga
        val context: Context = this
        findViewById<View>(R.id.download_button).setOnClickListener {
            val success = ImageUtils.saveImageToGallery(context, imageView, "image_filename.png")
            if (success) {
                Toast.makeText(context, "Imagen guardada correctamente", Toast.LENGTH_SHORT).show()

                // Obtener la ruta de la imagen guardada
                val imagePath = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)}/InstaFilter/image_filename.png"

            } else {
                Toast.makeText(context, "Error al guardar la imagen", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<View>(R.id.share_button).setOnClickListener {
            // Obtener la ruta de la imagen guardada
            val imagePath = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)}/InstaFilter/image_filename.png"
            // Compartir la imagen
            ImageUtils.shareImageFromGallery(this@DisplayImageActivity, imagePath)
        }
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
