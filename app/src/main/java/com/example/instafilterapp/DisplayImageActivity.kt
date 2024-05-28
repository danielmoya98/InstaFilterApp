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
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class DisplayImageActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private var isScrollViewVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_image)

        imageView = findViewById(R.id.image_view)

        val arrowButton: ImageButton = findViewById(R.id.arrow_button)

        // Establecer OnClickListener para el botón
        arrowButton.setOnClickListener {
            // Crear un Intent para iniciar la MainActivity
            val intent = Intent(this@DisplayImageActivity, MainActivity::class.java)
            startActivity(intent)
        }

        val detailedCardView: CardView = findViewById(R.id.detailed_card_view)
        val btnGlass: MaterialButton = findViewById(R.id.btn_glass)
        val horizontalScrollView: HorizontalScrollView = findViewById(R.id.horizontal_scroll_view)

        btnGlass.setOnClickListener {
            isScrollViewVisible = !isScrollViewVisible
            horizontalScrollView.visibility = if (isScrollViewVisible) View.VISIBLE else View.GONE
            detailedCardView.visibility = if (isScrollViewVisible) View.GONE else View.VISIBLE
        }

        // Handling MaterialCardView clicks
        val cardView1: MaterialCardView = findViewById(R.id.card_view_1)
        val cardView2: MaterialCardView = findViewById(R.id.card_view_2)
        val cardView3: MaterialCardView = findViewById(R.id.card_view_3)
        val cardView4: MaterialCardView = findViewById(R.id.card_view_4)

        val onClickListener = View.OnClickListener {
            horizontalScrollView.visibility = View.GONE
            detailedCardView.visibility = View.VISIBLE
        }

        cardView1.setOnClickListener(onClickListener)
        cardView2.setOnClickListener(onClickListener)
        cardView3.setOnClickListener(onClickListener)
        cardView4.setOnClickListener(onClickListener)

        // Obtener la URI de la imagen desde el Intent
        val imageUri: Uri? = intent.getParcelableExtra("imageUri")
        imageUri?.let { imageView.setImageURI(it) }

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
}
