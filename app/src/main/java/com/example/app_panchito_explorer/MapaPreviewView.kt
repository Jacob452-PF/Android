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

data class ObstaculoPunto(
    val x: Double,
    val y: Double
)

class MapaPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val ruta = mutableListOf<RutaPunto>()
    private val muroPuntos = mutableListOf<MuroPunto>()
    private val obstaculoPuntos = mutableListOf<ObstaculoPunto>()
    private var mostrarMedidas = true
    private var puertas = 0

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

    private val obstaculoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 80, 80)
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

    private val medidaTextoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 80, 160)
        textSize = 22f
        textAlign = Paint.Align.CENTER
        style = Paint.Style.FILL
        isFakeBoldText = true
    }

    private val medidaBordePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 22f
        textAlign = Paint.Align.CENTER
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isFakeBoldText = true
    }

    fun setMedidasMuros(oeste: Double, norte: Double, sur: Double, este: Double) {
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

    fun setObstaculos(puntos: List<ObstaculoPunto>) {
        obstaculoPuntos.clear()
        obstaculoPuntos.addAll(puntos)
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
            if (partes.size < 4) null
            else RutaPunto(partes[0].toIntOrNull() ?: 0, partes[1].toDoubleOrNull() ?: 0.0, partes[2].toDoubleOrNull() ?: 0.0, partes[3])
        }
        setRuta(convertidos, puertasPosibles)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(229, 229, 229))

        val padding = 40f
        val allX = ruta.map { it.x } + muroPuntos.map { it.x } + obstaculoPuntos.map { it.x }
        val allY = ruta.map { it.y } + muroPuntos.map { it.y } + obstaculoPuntos.map { it.y }
        
        val minX = min(allX.minOfOrNull { it } ?: 0.0, -50.0)
        val maxX = max(allX.maxOfOrNull { it } ?: 0.0, 50.0)
        val minY = min(allY.minOfOrNull { it } ?: 0.0, -50.0)
        val maxY = max(allY.maxOfOrNull { it } ?: 0.0, 50.0)
        
        val rangoX = maxX - minX
        val rangoY = maxY - minY
        val dibujoW = width - padding * 2
        val dibujoH = height - padding * 2
        
        val escala = min(dibujoW / rangoX, dibujoH / rangoY).toFloat()
        val centroX = padding + (dibujoW - rangoX * escala) / 2
        val centroY = padding + (dibujoH - rangoY * escala) / 2

        fun sx(x: Double): Float = (centroX + (x - minX) * escala).toFloat()
        fun sy(y: Double): Float = (height - centroY - (y - minY) * escala).toFloat()

        canvas.drawRect(padding, padding, width - padding, height - padding, areaPaint)
        dibujarGrid(canvas, padding)
        dibujarMurosMedidos(canvas, ::sx, ::sy)
        
        obstaculoPuntos.forEach { canvas.drawCircle(sx(it.x), sy(it.y), 6f, obstaculoPaint) }

        if (ruta.isNotEmpty()) {
            for (i in 1 until ruta.size) {
                val a = ruta[i - 1]; val b = ruta[i]
                val x1 = sx(a.x); val y1 = sy(a.y); val x2 = sx(b.x); val y2 = sy(b.y)
                canvas.drawLine(x1, y1, x2, y2, rutaPaint)

                if (mostrarMedidas) {
                    val distCm = sqrt((b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y))
                    if (distCm > 5.0) {
                        val midX = (x1 + x2) / 2; val midY = (y1 + y2) / 2
                        val angle = Math.toDegrees(Math.atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())).toFloat()
                        canvas.save()
                        canvas.translate(midX, midY)
                        canvas.rotate(if (angle > 90 || angle < -90) angle + 180 else angle)
                        canvas.drawText("${distCm.toInt()}cm", 0f, -10f, medidaBordePaint)
                        canvas.drawText("${distCm.toInt()}cm", 0f, -10f, medidaTextoPaint)
                        canvas.restore()
                    }
                }
            }

            val inicio = ruta.first(); val fin = ruta.last()
            val robotRadioPx = (8.5 * escala).toFloat().coerceAtLeast(15f) // Radio real 8.5cm
            canvas.drawCircle(sx(fin.x), sy(fin.y), robotRadioPx, robotPaint)
            canvas.drawCircle(sx(inicio.x), sy(inicio.y), 9f, puntoPaint)
            canvas.drawCircle(sx(fin.x), sy(fin.y), 11f, puertaPaint)

            val puertasADibujar = min(puertas, ruta.size)
            for (i in 0 until puertasADibujar) {
                val punto = ruta[(ruta.size - 1 - i).coerceAtLeast(0)]
                canvas.drawCircle(sx(punto.x), sy(punto.y), 7f, puertaPaint)
            }
        }
    }

    private fun dibujarMurosMedidos(canvas: Canvas, sx: (Double) -> Float, sy: (Double) -> Float) {
        if (muroPuntos.isEmpty()) return
        muroPuntos.groupBy { it.grupo }.values.forEach { grupo ->
            for (i in 1 until grupo.size) {
                val a = grupo[i - 1]; val b = grupo[i]
                if (sqrt((b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y)) <= 75.0)
                    canvas.drawLine(sx(a.x), sy(a.y), sx(b.x), sy(b.y), muroPaint)
            }
        }
        muroPuntos.forEach { canvas.drawCircle(sx(it.x), sy(it.y), 4.5f, muroPuntoPaint) }
    }

    private fun dibujarGrid(canvas: Canvas, padding: Float) {
        val div = 4; val w = width - padding * 2; val h = height - padding * 2
        for (i in 1 until div) {
            canvas.drawLine(padding + w * i / div, padding, padding + w * i / div, height - padding, gridPaint)
            canvas.drawLine(padding, padding + h * i / div, width - padding, padding + h * i / div, gridPaint)
        }
    }
}
