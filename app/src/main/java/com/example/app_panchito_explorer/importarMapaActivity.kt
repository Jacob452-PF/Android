package com.example.app_panchito_explorer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class importarMapaActivity : AppCompatActivity() {

    private var contenidoSeleccionado: String? = null
    private lateinit var tvTituloArchivo: TextView
    private lateinit var tvSubtituloArchivo: TextView
    private lateinit var tvNombreMapa: TextView
    private lateinit var tvDistanciaMapa: TextView
    private lateinit var tvTiempoMapa: TextView
    private lateinit var tvRutaMapa: TextView
    private lateinit var tvPuertasMapa: TextView
    private lateinit var btnConfirmar: Button

    // Selector de archivos del sistema para elegir un mapa exportado.
    private val abrirArchivo = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            cargarVistaPrevia(uri)
        }
    }

    // =========================
    // ON CREATE
    // =========================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_importar_mapa)

        // Referencias de botones de la pantalla.
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnCancelar = findViewById<Button>(R.id.btnCancelar)
        btnConfirmar = findViewById(R.id.btnIniciar)
        val subirMapa = findViewById<LinearLayout>(R.id.subirMapa)
        tvTituloArchivo = findViewById(R.id.tvTituloArchivo)
        tvSubtituloArchivo = findViewById(R.id.tvSubtituloArchivo)
        tvNombreMapa = findViewById(R.id.tvNombreMapa)
        tvDistanciaMapa = findViewById(R.id.tvDistanciaMapa)
        tvTiempoMapa = findViewById(R.id.tvTiempoMapa)
        tvRutaMapa = findViewById(R.id.tvRutaMapa)
        tvPuertasMapa = findViewById(R.id.tvPuertasMapa)

        btnConfirmar.text = "Confirmar"
        btnConfirmar.isEnabled = false

        // Permite importar directamente si Android abre la app desde un archivo.
        if (intent?.action == Intent.ACTION_VIEW && intent?.data != null) {
            cargarVistaPrevia(intent.data!!)
        }

        // Abre el selector limitado a los tipos de archivo compatibles.
        subirMapa.setOnClickListener {
            abrirArchivo.launch(
                arrayOf(
                    MapaFileManager.MIME_TYPE,
                    "application/octet-stream",
                    "text/plain"
                )
            )
        }

        btnConfirmar.setOnClickListener {
            confirmarImportacion()
        }

        // Cierra la pantalla sin importar.
        btnBack.setOnClickListener {
            finish()
        }

        btnCancelar.setOnClickListener {
            finish()
        }
    }

    // Lee el contenido del archivo y muestra sus datos antes de guardar.
    private fun cargarVistaPrevia(uri: Uri) {
        try {
            val contenido = contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader().readText()
            }

            if (contenido.isNullOrBlank()) {
                Toast.makeText(this, "Archivo vacio", Toast.LENGTH_SHORT).show()
                return
            }

            val json = JSONObject(contenido)
            if (json.optString("format") != "panchito-map") {
                Toast.makeText(this, "Archivo de mapa no compatible", Toast.LENGTH_LONG).show()
                return
            }

            contenidoSeleccionado = contenido
            mostrarDatosArchivo(json)
            btnConfirmar.isEnabled = true

        } catch (e: Exception) {
            e.printStackTrace()
            contenidoSeleccionado = null
            btnConfirmar.isEnabled = false
            Toast.makeText(this, "No se pudo leer el mapa", Toast.LENGTH_LONG).show()
        }
    }

    private fun mostrarDatosArchivo(json: JSONObject) {
        val nombre = json.optString("nombre", "Mapa sin nombre")
        val distancia = json.optDouble("distanciaTotal", 0.0)
        val tiempo = json.optInt("tiempoSegundos", 0)
        val rutas = json.optJSONArray("ruta")?.length() ?: 0
        val puntosAuto = contarPuntosAuto(json)
        val puertas = json.optInt("puertas", 0)
        val muros = json.optJSONArray("muroPuntos")?.length() ?: 0

        tvTituloArchivo.text = "Archivo seleccionado"
        tvSubtituloArchivo.text = nombre
        tvNombreMapa.text = "Nombre: $nombre"
        tvDistanciaMapa.text = "Distancia: ${"%.1f".format(distancia)} m"
        tvTiempoMapa.text = "Tiempo: ${formatearTiempo(tiempo)}"
        tvRutaMapa.text = "Recorrido: $rutas puntos | Auto: $puntosAuto"
        tvPuertasMapa.text = "Puertas: $puertas | Muros: $muros puntos"
    }

    private fun contarPuntosAuto(json: JSONObject): Int {
        val rutas = json.optJSONArray("ruta") ?: return 0
        var total = 0
        for (i in 0 until rutas.length()) {
            val punto = rutas.optJSONObject(i)
            if (punto?.optString("modo")?.equals("auto", ignoreCase = true) == true) {
                total++
            }
        }
        return total
    }

    private fun confirmarImportacion() {
        val contenido = contenidoSeleccionado
        if (contenido.isNullOrBlank()) {
            Toast.makeText(this, "Selecciona un archivo primero", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val mapaId = MapaFileManager.importar(this, contenido)
            Toast.makeText(this, "Mapa importado", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, verMapaGuardadoActivity::class.java)
            intent.putExtra("mapa_id", mapaId)
            startActivity(intent)
            finish()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "No se pudo importar el mapa", Toast.LENGTH_LONG).show()
        }
    }

    private fun formatearTiempo(totalSegundos: Int): String {
        val minutos = totalSegundos / 60
        val segundos = totalSegundos % 60
        return "%02d:%02d".format(minutos, segundos)
    }
}
