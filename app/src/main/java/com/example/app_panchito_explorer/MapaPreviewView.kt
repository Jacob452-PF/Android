package com.example.app_panchito_explorer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.min

class MapaPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val ruta = mutableListOf<RutaPunto>()

    private val rutaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 150, 220)
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val puntoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 70, 140)
        style = Paint.Style.FILL
    }

    private val puertaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(70, 190, 90)
        style = Paint.Style.FILL
    }

    private var puertas = 0

    fun setRuta(puntos: List<RutaPunto>, puertasPosibles: Int = puertas) {
        ruta.clear()
        ruta.addAll(puntos)
        puertas = puertasPosibles
        invalidate()
    }

    fun setRutaDesdeTexto(puntos: List<String>, puertasPosibles: Int = puertas) {
        val convertidos = puntos.mapNotNull { punto ->
            val partes = punto.split(",")
            if (partes.size < 4) {
                null
            } else {
                RutaPunto(
                    orden = partes[0].toIntOrNull() ?: 0,
                    x = partes[1].toDoubleOrNull() ?: 0.0,
                    y = partes[2].toDoubleOrNull() ?: 0.0,
                    modo = partes[3]
                )
            }
        }
        setRuta(convertidos, puertasPosibles)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(Color.rgb(229, 229, 229))

        if (ruta.isEmpty()) {
            return
        }

        val padding = 28f
        val minX = ruta.minOf { it.x }
        val maxX = ruta.maxOf { it.x }
        val minY = ruta.minOf { it.y }
        val maxY = ruta.maxOf { it.y }
        val rangoX = max(0.5, maxX - minX)
        val rangoY = max(0.5, maxY - minY)
        val dibujoW = max(1f, width - padding * 2)
        val dibujoH = max(1f, height - padding * 2)

        fun sx(x: Double): Float {
            return (padding + ((x - minX) / rangoX * dibujoW)).toFloat()
        }

        fun sy(y: Double): Float {
            return (height - padding - ((y - minY) / rangoY * dibujoH)).toFloat()
        }

        for (i in 1 until ruta.size) {
            val a = ruta[i - 1]
            val b = ruta[i]
            canvas.drawLine(sx(a.x), sy(a.y), sx(b.x), sy(b.y), rutaPaint)
        }

        val inicio = ruta.first()
        val fin = ruta.last()
        canvas.drawCircle(sx(inicio.x), sy(inicio.y), 9f, puntoPaint)
        canvas.drawCircle(sx(fin.x), sy(fin.y), 11f, puertaPaint)

        val puertasADibujar = min(puertas, ruta.size)
        for (i in 0 until puertasADibujar) {
            val punto = ruta[(ruta.size - 1 - i).coerceAtLeast(0)]
            canvas.drawCircle(sx(punto.x), sy(punto.y), 7f, puertaPaint)
        }
    }
}
