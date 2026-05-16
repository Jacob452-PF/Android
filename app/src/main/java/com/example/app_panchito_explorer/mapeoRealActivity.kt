package com.example.app_panchito_explorer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class mapeoRealActivity : AppCompatActivity() {

    // 🔥 VARIABLES DEL MAPEO
    private var oeste = 0.0
    private var norte = 0.0
    private var sur = 0.0
    private var este = 0.0

    private var distanciaTotal = 0.0
    private var obstaculosPequenos = 0
    private var obstaculosGrandes = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mapeo_real)

        // 🔥 BOTONES
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarMapa)

        // 📍 REFERENCIAS UI
        val tvOeste = findViewById<TextView>(R.id.tvOeste)
        val tvNorte = findViewById<TextView>(R.id.tvNorte)
        val tvSur = findViewById<TextView>(R.id.tvSur)
        val tvEste = findViewById<TextView>(R.id.tvEste)

        val tvDistancia = findViewById<TextView>(R.id.tvDistancia)
        val tvPequenos = findViewById<TextView>(R.id.tvPequenos)
        val tvGrandes = findViewById<TextView>(R.id.tvGrandes)

        // 🔥 ACTUALIZAR UI
        fun actualizarUI() {

            tvOeste.text = "Oeste\n${oeste} M"
            tvNorte.text = "Norte\n${norte} M"
            tvSur.text = "Sur\n${sur} M"
            tvEste.text = "Este\n${este} M"

            tvDistancia.text = "Distancia\n${distanciaTotal} M"

            tvPequenos.text = "Pequeños\n$obstaculosPequenos"
            tvGrandes.text = "Grandes\n$obstaculosGrandes"
        }

        // 🧪 DATOS DE PRUEBA
        // Luego vendrán del Bluetooth

        oeste = 5.2
        norte = 3.1
        sur = 4.0
        este = 2.8

        distanciaTotal = 15.1

        obstaculosPequenos = 3
        obstaculosGrandes = 1

        actualizarUI()

        // 💾 GUARDAR MAPA
        btnGuardar.setOnClickListener {

            val intent = Intent(this, guardarMapaActivity::class.java)

            intent.putExtra("distancia_total", distanciaTotal)

            intent.putExtra("oeste", oeste)
            intent.putExtra("norte", norte)
            intent.putExtra("sur", sur)
            intent.putExtra("este", este)

            intent.putExtra("pequenos", obstaculosPequenos)
            intent.putExtra("grandes", obstaculosGrandes)

            startActivity(intent)
        }

        // 🔙 BOTÓN DE REGRESO DE LA APP
        btnBack.setOnClickListener {

            mostrarAdvertenciaSalida()
        }

        // 🔥 BOTÓN FÍSICO / GESTO ATRÁS
        onBackPressedDispatcher.addCallback(this) {

            mostrarAdvertenciaSalida()
        }

        // 🔥 AJUSTE DE BORDES
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->

            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }
    }

    // 🔥 MENSAJE DE ADVERTENCIA
    private fun mostrarAdvertenciaSalida() {

        AlertDialog.Builder(this)
            .setTitle("Cancelar mapeo")

            .setMessage(
                "El proceso de mapeo aún está en ejecución.\n\n" +
                        "Si sales ahora se perderán los datos recolectados.\n\n" +
                        "¿Deseas continuar?"
            )

            .setPositiveButton("Salir") { _, _ ->

                finish()
            }

            .setNegativeButton("Continuar mapeando", null)

            .show()
    }
}