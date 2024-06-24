package com.example.instafilterapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

// Clase que representa la pantalla de inicio (Splash Screen) de la aplicación
class Splash : AppCompatActivity() {

    // Método que se llama cuando la actividad se crea
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Establece el diseño de la actividad a partir del recurso XML correspondiente
        setContentView(R.layout.activity_splash)

        // Crea un Handler para retrasar la ejecución de un bloque de código
        Handler(Looper.getMainLooper()).postDelayed({
            // Inicia la actividad principal (MainActivity) después de un retraso de 3 segundos
            startActivity(Intent(this@Splash, MainActivity::class.java))
            // Finaliza la actividad actual para que no quede en la pila de actividades
            finish()
        }, 3000) // 3000 milisegundos = 3 segundos
    }
}
