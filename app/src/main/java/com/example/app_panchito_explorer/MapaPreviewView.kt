package com.example.app_panchito_explorer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class MuroPunto(
    val x: Double,
    val y: Double,
    val grupo: Int = 0
)

class MapaPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val ruta = mutableListOf<RutaPunto>()
    private val muroPuntos = mutableListOf<MuroPunto>()
    private var mostrarMedidas = true

    private val areaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(236, 242, 245)
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(190, 203, 210)
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }

    private val muroPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(28, 42, 56)
        strokeWidth = 8f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val muroPuntoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(20, 32, 45)
        style = Paint.Style.FILL
    }

    private val robotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 184, 77)
        style = Paint.Style.FILL
    }

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

    private val textoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(38, 58, 75)
        textSize = 24f
        textAlign = Paint.Align.CENTER
        style = Paint.Style.FILL
    }

    private var puertas = 0

    fun setMedidasMuros(oeste: Double, norte: Double, sur: Double, este: Double) {
        // Se conserva para compatibilidad con pantallas existentes.
        // El mapa real ya no dibuja un rectangulo N/S/E/O como muro.
        invalidate()
    }

    fun setMostrarMedidas(mostrar: Boolean) {
        mostrarMedidas = mostrar
        invalidate()
    }

    fun setMuroPuntos(puntos: List<MuroPunto>) {
        muroPuntos.clear()
        muroPuntos.addAll(puntos)
        invalidate()
    }

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

        val padding = 28f
        val rutaMinX = ruta.minOfOrNull { it.x } ?: 0.0
        val rutaMaxX = ruta.maxOfOrNull { it.x } ?: 0.0
        val rutaMinY = ruta.minOfOrNull { it.y } ?: 0.0
        val rutaMaxY = ruta.maxOfOrNull { it.y } ?: 0.0
        val muroMinX = muroPuntos.minOfOrNull { it.x } ?: 0.0
        val muroMaxX = muroPuntos.maxOfOrNull { it.x } ?: 0.0
        val muroMinY = muroPuntos.minOfOrNull { it.y } ?: 0.0
        val muroMaxY = muroPuntos.maxOfOrNull { it.y } ?: 0.0
        val minX = min(rutaMinX, muroMinX).coerceAtMost(-0.25)
        val maxX = max(rutaMaxX, muroMaxX).coerceAtLeast(0.25)
        val minY = min(rutaMinY, muroMinY).coerceAtMost(-0.25)
        val maxY = max(rutaMaxY, muroMaxY).coerceAtLeast(0.25)
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

        canvas.drawRect(padding, padding, width - padding, height - padding, areaPaint)
        dibujarGrid(canvas, padding)
        dibujarMurosMedidos(canvas, ::sx, ::sy)

        if (ruta.isEmpty()) {
            return
        }

        for (i in 1 until ruta.size) {
            val a = ruta[i - 1]
            val b = ruta[i]
            canvas.drawLine(sx(a.x), sy(a.y), sx(b.x), sy(b.y), rutaPaint)
        }

        val inicio = ruta.first()
        val fin = ruta.last()
        canvas.drawCircle(sx(inicio.x), sy(inicio.y), 9f, puntoPaint)
        canvas.drawCircle(sx(inicio.x), sy(inicio.y), 14f, robotPaint)
        canvas.drawCircle(sx(fin.x), sy(fin.y), 11f, puertaPaint)

        val puertasADibujar = min(puertas, ruta.size)
        for (i in 0 until puertasADibujar) {
            val punto = ruta[(ruta.size - 1 - i).coerceAtLeast(0)]
            canvas.drawCircle(sx(punto.x), sy(punto.y), 7f, puertaPaint)
        }
    }

    private fun dibujarMurosMedidos(
        canvas: Canvas,
        sx: (Double) -> Float,
        sy: (Double) -> Float
    ) {
        if (muroPuntos.isEmpty()) {
            return
        }

        muroPuntos
            .groupBy { it.grupo }
            .values
            .forEach { grupo ->
                for (i in 1 until grupo.size) {
                    val a = grupo[i - 1]
                    val b = grupo[i]
                    val distancia =
                        sqrt(((b.x - a.x) * (b.x - a.x)) + ((b.y - a.y) * (b.y - a.y)))

                    if (distancia <= MAX_LINEA_MURO_M) {
                        canvas.drawLine(sx(a.x), sy(a.y), sx(b.x), sy(b.y), muroPaint)
                    }
                }
            }

        muroPuntos.forEach { punto ->
            canvas.drawCircle(sx(punto.x), sy(punto.y), 4.5f, muroPuntoPaint)
        }
    }

    private fun dibujarGrid(canvas: Canvas, padding: Float) {
        val columnas = 4
        val filas = 4
        val ancho = width - padding * 2
        val alto = height - padding * 2

        for (i in 1 until columnas) {
            val x = padding + ancho * i / columnas
            canvas.drawLine(x, padding, x, height - padding, gridPaint)
        }

        for (i in 1 until filas) {
            val y = padding + alto * i / filas
            canvas.drawLine(padding, y, width - padding, y, gridPaint)
        }
    }

    companion object {
        private const val MAX_LINEA_MURO_M = 0.75
    }
}
