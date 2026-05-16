package com.example.app_panchito_explorer

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2

class manualActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnNext: Button
    private lateinit var dotsLayout: LinearLayout
    private val dots = ArrayList<TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_manual)

        // Bordes
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Referencias
        viewPager = findViewById(R.id.viewPager)
        btnNext = findViewById(R.id.btnNext)
        dotsLayout = findViewById(R.id.dotsLayout)

        // Datos
        val lista = listOf(
            TutorialItem("Paso 1 de 5", "Presiona INICIAR para comenzar", "Asegúrate que el carrito esté encendido", R.drawable.paso1),
            TutorialItem("Paso 2 de 5", "Conecta tu carrito por Bluetooth", "Activa el Bluetooth antes de buscar", R.drawable.paso2),
            TutorialItem("Paso 3 de 5", "Controla el movimiento", "Usa velocidad baja al inicio", R.drawable.paso3),
            TutorialItem("Paso 4 de 5", "Mapea el entorno", "Evita obstáculos para mejor resultado", R.drawable.paso4),
            TutorialItem("Paso 5 de 5", "Guarda tus mapas", "Puedes exportarlos después", R.drawable.paso5)
        )

        // Adapter
        viewPager.adapter = TutorialAdapter(lista)

        // Crear dots iniciales
        crearDots(0, lista.size)

        // Listener cambio de página
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                crearDots(position, lista.size)
            }
        })

        // Botón siguiente
        btnNext.setOnClickListener {
            if (viewPager.currentItem < lista.size - 1) {
                viewPager.currentItem += 1
            } else {
                finish()
            }
        }
    }

    // 🔵 Crear indicadores
    private fun crearDots(posicion: Int, total: Int) {
        dotsLayout.removeAllViews()
        dots.clear()

        for (i in 0 until total) {
            val dot = TextView(this)
            dot.text = "●"
            dot.textSize = 20f
            dot.setPadding(8, 0, 8, 0)

            if (i == posicion) {
                dot.setTextColor(getColor(android.R.color.holo_blue_light))
            } else {
                dot.setTextColor(getColor(android.R.color.darker_gray))
            }

            dots.add(dot)
            dotsLayout.addView(dot)
        }
    }
}