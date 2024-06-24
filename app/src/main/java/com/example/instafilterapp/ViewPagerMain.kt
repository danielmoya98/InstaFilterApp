package com.example.instafilterapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager

// Clase que representa la actividad principal con un ViewPager para el onboarding
class ViewPagerMain : AppCompatActivity() {

    // Variables de la clase
    private lateinit var viewPagerMain: ViewPager
    private lateinit var onboardingAdapter: OnboardingAdapter
    private lateinit var dotsLayout: LinearLayout
    private lateinit var dots: Array<TextView?>
    private lateinit var layouts: IntArray
    private lateinit var startButton: Button

    // Método que se llama cuando la actividad se crea
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Establece el diseño de la actividad a partir del recurso XML correspondiente
        setContentView(R.layout.activity_view_pager)

        // Inicializa los elementos de la interfaz de usuario
        startButton = findViewById(R.id.start_button)
        viewPagerMain = findViewById(R.id.view_pager)
        dotsLayout = findViewById(R.id.dots_layout)

        // Array de layouts para las pantallas del onboarding
        layouts = intArrayOf(
            R.layout.screen1,
            R.layout.screen2,
            R.layout.screen3
        )

        // Configura el adaptador para el ViewPager
        onboardingAdapter = OnboardingAdapter(this, layouts)
        viewPagerMain.adapter = onboardingAdapter

        // Añade los indicadores de puntos para la primera pantalla
        addDotsIndicator(0)
        // Añade el listener para cambios de página en el ViewPager
        viewPagerMain.addOnPageChangeListener(viewPagerPageChangeListener)

        // Configura el listener para el botón de inicio
        startButton.setOnClickListener {
            openNextActivity()
        }
    }

    // Método para añadir los indicadores de puntos en la posición indicada
    private fun addDotsIndicator(position: Int) {
        dots = arrayOfNulls(layouts.size)
        dotsLayout.removeAllViews()

        // Crea y añade los puntos al layout de indicadores
        for (i in dots.indices) {
            dots[i] = TextView(this).apply {
                text = "•"
                textSize = 35f
                setTextColor(resources.getColor(R.color.black))
            }
            dotsLayout.addView(dots[i])
        }

        // Cambia el color del punto activo
        if (dots.isNotEmpty()) {
            dots[position]?.setTextColor(resources.getColor(R.color.green))
        }
    }

    // Listener para cambios de página en el ViewPager
    private val viewPagerPageChangeListener = object : ViewPager.OnPageChangeListener {
        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
        override fun onPageSelected(position: Int) {
            // Cambia el indicador de puntos cuando se selecciona una nueva página
            addDotsIndicator(position)
        }
        override fun onPageScrollStateChanged(state: Int) {}
    }

    // Método para abrir la siguiente actividad (MainActivity)
    private fun openNextActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}
