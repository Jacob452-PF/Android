package com.example.app_panchito_explorer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class guardarMapaActivity : AppCompatActivity() {

    // =========================
    // ON CREATE
    // =========================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configura pantalla completa y aplica padding segun las barras del sistema.
        enableEdgeToEdge()
        setContentView(R.layout.activity_guardar_mapa)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Referencias de resumen, vista previa y acciones.
        val tvOeste = findViewById<TextView>(R.id.tvOeste)
        val tvNorte = findViewById<TextView>(R.id.tvNorte)
        val tvSur = findViewById<TextView>(R.id.tvSur)
        val tvEste = findViewById<TextView>(R.id.tvEste)
        val tvDistancia = findViewById<TextView>(R.id.tvDistancia)
        val tvPequenos = findViewById<TextView>(R.id.tvPequenos)
        val tvGrandes = findViewById<TextView>(R.id.tvGrandes)
        val mapaPreview = findViewById<MapaPreviewView>(R.id.mapaPreview)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnGuardar = findViewById<Button>(R.id.btnGuardar)

        // Datos enviados desde la pantalla de mapeo.
        val oeste = intent.getDoubleExtra("oeste", 0.0)
        val norte = intent.getDoubleExtra("norte", 0.0)
        val sur = intent.getDoubleExtra("sur", 0.0)
        val este = intent.getDoubleExtra("este", 0.0)
        val distancia = intent.getDoubleExtra("distancia_total", 0.0)
        val tiempoSegundos = intent.getIntExtra("tiempo_segundos", 0)
        val pequenos = intent.getIntExtra("pequenos", 0)
        val grandes = intent.getIntExtra("grandes", 0)
        val puertas = intent.getIntExtra("puertas", 0)
        val rutaManual = intent.getStringArrayListExtra("ruta_manual") ?: arrayListOf()
        val muroPuntosTexto = intent.getStringArrayListExtra("muro_puntos") ?: arrayListOf()
        // Convierte los puntos de muro recibidos como texto a objetos del mapa.
        val muroPuntos = muroPuntosTexto.mapNotNull { punto ->
            val partes = punto.split(",")
            if (partes.size < 2) {
                null
            } else {
                MuroPunto(
                    x = partes[0].toDoubleOrNull() ?: 0.0,
                    y = partes[1].toDoubleOrNull() ?: 0.0,
                    grupo = partes.getOrNull(2)?.toIntOrNull() ?: 0
                )
            }
        }

        // Muestra el resumen de medidas y obstaculos antes de guardar.
        tvOeste.text = "Oeste\n${"%.1f".format(oeste)} M"
        tvNorte.text = "Norte\n${"%.1f".format(norte)} M"
        tvSur.text = "Sur\n${"%.1f".format(sur)} M"
        tvEste.text = "Este\n${"%.1f".format(este)} M"
        tvDistancia.text = "Distancia: ${"%.1f".format(distancia)} M"
        tvPequenos.text = "Obstaculos pequenos: $pequenos"
        tvGrandes.text = "Obstaculos grandes: $grandes | Puertas posibles: $puertas"
        mapaPreview.setMedidasMuros(oeste, norte, sur, este)
        mapaPreview.setMostrarMedidas(true)
        mapaPreview.setRutaDesdeTexto(rutaManual, puertas)
        mapaPreview.setMedidasMuros(oeste, norte, sur, este)
        mapaPreview.setMuroPuntos(muroPuntos)

        // Regresa sin guardar.
        btnBack.setOnClickListener {
            finish()
        }

        // Guarda el mapa completo en la base de datos local.
        btnGuardar.setOnClickListener {
            val db = DBHelper(this)
            val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            val nombre = "Mapa $fecha"
            val descripcion = "Ruta manual: ${rutaManual.size} puntos"

            val mapaId = db.insertarMapa(
                nombre = nombre,
                descripcion = descripcion,
                distancia = distancia,
                tiempoSegundos = tiempoSegundos,
                oeste = oeste,
                norte = norte,
                sur = sur,
                este = este,
                pequenos = pequenos,
                grandes = grandes,
                puertas = puertas
            ).toInt()

            if (mapaId <= 0) {
                Toast.makeText(this, "No se pudo guardar el mapa", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Guarda las medidas de los muros.
            db.insertarMuro(mapaId, "Oeste", oeste)
            db.insertarMuro(mapaId, "Norte", norte)
            db.insertarMuro(mapaId, "Sur", sur)
            db.insertarMuro(mapaId, "Este", este)

            // Registra el conteo de obstaculos detectados.
            repeat(pequenos) {
                db.insertarObstaculo(mapaId, "pequeno", 0.0, 18.0, 0.0, -1, "auto")
            }

            repeat(grandes) {
                db.insertarObstaculo(mapaId, "grande", 0.0, 18.0, 0.0, -1, "auto")
            }

            // Guarda cada punto de la ruta manual.
            rutaManual.forEach { punto ->
                val partes = punto.split(",")
                if (partes.size >= 4) {
                    db.insertarRutaPunto(
                        mapaId = mapaId,
                        orden = partes[0].toIntOrNull() ?: 0,
                        x = partes[1].toDoubleOrNull() ?: 0.0,
                        y = partes[2].toDoubleOrNull() ?: 0.0,
                        modo = partes[3]
                    )
                }
            }

            // Guarda los puntos reales de muros creados durante el escaneo.
            muroPuntos.forEachIndexed { index, punto ->
                db.insertarMuroPunto(
                    mapaId = mapaId,
                    orden = index,
                    x = punto.x,
                    y = punto.y,
                    grupo = punto.grupo
                )
            }

            // Registra las puertas posibles calculadas para este mapa.
            repeat(puertas) { index ->
                db.insertarPuerta(
                    mapaId = mapaId,
                    orden = index,
                    x = 0.0,
                    y = 0.0,
                    anchoEstimado = 70.0,
                    nota = "Puerta posible para robot de 16-18 cm de alto"
                )
            }

            // Lleva al usuario al listado de mapas despues de guardar.
            Toast.makeText(this, "Mapa guardado", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, guardadosActivity::class.java))
            finish()
        }
    }
}
