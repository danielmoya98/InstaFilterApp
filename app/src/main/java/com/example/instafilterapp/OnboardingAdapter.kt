package com.example.instafilterapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter

// Adaptador para el ViewPager del onboarding
class OnboardingAdapter(private val context: Context, private val layouts: IntArray) : PagerAdapter() {

    // Devuelve el número de vistas disponibles
    override fun getCount(): Int {
        return layouts.size
    }

    // Verifica si una vista corresponde a un objeto específico
    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    // Instancia una vista en una posición dada
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        // Obtiene el LayoutInflater del contexto
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        // Infla el layout correspondiente a la posición actual
        val view = inflater.inflate(layouts[position], container, false)
        // Añade la vista al contenedor
        container.addView(view)
        return view
    }

    // Destruye una vista en una posición dada
    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        // Convierte el objeto en una vista
        val view = `object` as View
        // Remueve la vista del contenedor
        container.removeView(view)
    }
}
