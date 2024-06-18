package com.example.rulerview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.instafilterapp.R

class RulerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint()
    private var tickSpacing = 10f
    private var longTickHeight = 40f
    private var shortTickHeight = 20f
    private var backgroundColor = Color.BLACK
    private var pointerColor = Color.RED // Color del puntero

    init {
        val typedArray = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.RulerView,
            0, 0
        )

        try {
            tickSpacing = typedArray.getDimension(R.styleable.RulerView_tickSpacing, 10f)
            longTickHeight = typedArray.getDimension(R.styleable.RulerView_longTickHeight, 40f)
            shortTickHeight = typedArray.getDimension(R.styleable.RulerView_shortTickHeight, 20f)
            paint.color = typedArray.getColor(R.styleable.RulerView_tickColor, Color.WHITE)
            backgroundColor = typedArray.getColor(R.styleable.RulerView_backgroundColor, Color.BLACK)
            pointerColor = typedArray.getColor(R.styleable.RulerView_pointerColor, Color.RED)
        } finally {
            typedArray.recycle()
        }

        paint.strokeWidth = 4f
        paint.isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(backgroundColor)
        drawRuler(canvas)
        drawPointer(canvas)
    }

    private fun drawRuler(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()

        // Dibuja la línea horizontal principal
        canvas.drawLine(0f, height / 2, width, height / 2, paint)

        // Dibuja las marcas
        for (i in 0 until (width / tickSpacing).toInt()) {
            val x = i * tickSpacing
            val tickHeight = if (i % 10 == 0) longTickHeight else shortTickHeight
            canvas.drawLine(x, height / 2 - tickHeight / 2, x, height / 2 + tickHeight / 2, paint)
        }
    }

    private fun drawPointer(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()
        val pointerX = width / 2
        val pointerHeight = longTickHeight + 20 // Altura del puntero

        paint.color = pointerColor
        canvas.drawLine(pointerX, height / 2 - pointerHeight / 2, pointerX, height / 2 + pointerHeight / 2, paint)
        paint.color = Color.WHITE // Restablecer el color de la pintura a blanco
    }

    override fun setBackgroundColor(color: Int) {
        backgroundColor = color
        invalidate() // Solicita un redibujado para aplicar el nuevo color de fondo
    }

    fun setPointerColor(color: Int) {
        pointerColor = color
        invalidate() // Solicita un redibujado para aplicar el nuevo color del puntero
    }
}
