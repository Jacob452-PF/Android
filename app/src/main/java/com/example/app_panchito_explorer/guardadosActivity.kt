package com.example.app_panchito_explorer

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class guardadosActivity : AppCompatActivity() {

    // =========================
    // ON CREATE
    // =========================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configura pantalla completa y ajusta el contenido a las barras del sistema.
        enableEdgeToEdge()
        setContentView(R.layout.activity_guardados)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // Referencias principales de la pantalla.
        val btnAgregar = findViewById<ImageView>(R.id.btnAgregar)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        val listaRutas = findViewById<LinearLayout>(R.id.listaRutas)

        // Abre la pantalla para importar un mapa desde archivo.
        btnAgregar.setOnClickListener {
            startActivity(Intent(this, importarMapaActivity::class.java))
        }

        // Carga los mapas guardados al entrar a la pantalla.
        cargarMapas(listaRutas)

        bottomNav.selectedItemId = R.id.nav_files

        // Regresa a la pantalla anterior.
        btnBack.setOnClickListener {
            finish()
        }

        // Navegacion inferior entre las secciones principales.
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, homeActivity::class.java))
                    true
                }
                R.id.nav_bluetooth -> {
                    startActivity(Intent(this, bluetoothActivity::class.java))
                    true
                }
                R.id.nav_map -> {
                    startActivity(Intent(this, antesDeMapearActivity::class.java))
                    true
                }
                R.id.nav_files -> true
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val listaRutas = findViewById<LinearLayout>(R.id.listaRutas)
        // Actualiza la lista por si se importo o elimino un mapa.
        cargarMapas(listaRutas)
    }

    // =========================
    // CARGAR MAPAS
    // =========================
    private fun cargarMapas(listaRutas: LinearLayout) {
        listaRutas.removeAllViews()

        val mapas = DBHelper(this).obtenerMapasGuardados()

        // Muestra un mensaje cuando aun no hay mapas en la base de datos.
        if (mapas.isEmpty()) {
            val empty = TextView(this).apply {
                text = "No hay mapas guardados"
                setTextColor(android.graphics.Color.WHITE)
                textSize = 18f
            }
            listaRutas.addView(empty)
            return
        }

        // Crea una tarjeta visual por cada mapa guardado.
        mapas.forEach { mapa ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                setBackgroundResource(R.drawable.card_option)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(82)
                ).apply {
                    bottomMargin = dp(12)
                }
            }

            val icon = ImageView(this).apply {
                setImageResource(R.drawable.ic_map)
                layoutParams = LinearLayout.LayoutParams(dp(24), dp(24))
            }

            val textos = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply {
                    marginStart = dp(10)
                }
            }

            val titulo = TextView(this).apply {
                text = mapa.nombre
                setTextColor(android.graphics.Color.WHITE)
                textSize = 18f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }

            val detalle = TextView(this).apply {
                // El detalle resume distancia, puntos de ruta y puertas posibles.
                text = "${"%.1f".format(mapa.distanciaTotal)} m | Ruta ${DBHelper(this@guardadosActivity).obtenerRutas(mapa.id).size} pts | Puertas ${mapa.puertas}"
                setTextColor(android.graphics.Color.rgb(191, 223, 255))
                textSize = 14f
            }

            textos.addView(titulo)
            textos.addView(detalle)
            item.addView(icon)
            item.addView(textos)

            // Abre el visor del mapa seleccionado.
            item.setOnClickListener {
                val intent = Intent(this, verMapaGuardadoActivity::class.java)
                intent.putExtra("mapa_id", mapa.id)
                startActivity(intent)
            }

            listaRutas.addView(item)
        }
    }

    // Convierte dp a pixeles para mantener tamanos consistentes en distintos dispositivos.
    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
