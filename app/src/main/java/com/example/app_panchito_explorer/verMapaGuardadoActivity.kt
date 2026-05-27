package com.example.app_panchito_explorer

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class verMapaGuardadoActivity : AppCompatActivity() {

    // Id del mapa abierto; se usa al exportar.
    private var mapaActualId = -1

    // Crea un archivo con el formato propio de la app y luego lo comparte.
    private val crearArchivo = registerForActivityResult(
        ActivityResultContracts.CreateDocument(MapaFileManager.MIME_TYPE)
    ) { uri: Uri? ->
        val target = uri ?: return@registerForActivityResult
        val contenido = MapaFileManager.exportar(this, mapaActualId)

        if (contenido == null) {
            Toast.makeText(this, "No se pudo exportar", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }

        contentResolver.openOutputStream(target)?.use { output ->
            output.write(contenido.toByteArray())
        }

        compartirArchivo(target)
    }

    // =========================
    // ON CREATE
    // =========================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_mapa_guardado)

        // Busca el mapa solicitado en la base de datos.
        val db = DBHelper(this)
        val mapaId = intent.getIntExtra("mapa_id", -1)
        val mapa = db.obtenerMapa(mapaId)

        // Referencias principales de la pantalla.
        val titulo = findViewById<TextView>(R.id.tituloMapa)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnEliminar = findViewById<Button>(R.id.btnEliminar)
        val btnExportar = findViewById<Button>(R.id.btnExportar)
        val mapaPreview = findViewById<MapaPreviewView>(R.id.mapaPreview)

        // Si no existe el mapa, muestra aviso y permite volver.
        if (mapa == null) {
            titulo.text = "Mapa no encontrado"
            Toast.makeText(this, "No se encontro el mapa", Toast.LENGTH_SHORT).show()
            btnBack.setOnClickListener { finish() }
            return
        }

        // Carga ruta y muros para dibujar la vista previa.
        val rutas = db.obtenerRutas(mapa.id)
        val muroPuntos = db.obtenerMuroPuntos(mapa.id)
        val rutasPreview = rutas.map { punto ->
            RutaPunto(
                orden = punto.orden,
                x = punto.x * 100.0,
                y = punto.y * 100.0,
                modo = punto.modo,
                angulo = punto.angulo
            )
        }
        val muroPuntosPreview = muroPuntos.map { punto ->
            MuroPunto(
                x = punto.x * 100.0,
                y = punto.y * 100.0,
                grupo = punto.grupo
            )
        }
        val puntosAuto = rutas.count { it.modo.equals("auto", ignoreCase = true) }
        mapaActualId = mapa.id
        mapaPreview.setMedidasMuros(mapa.oeste * 100.0, mapa.norte * 100.0, mapa.sur * 100.0, mapa.este * 100.0)
        mapaPreview.setMostrarMedidas(true)
        mapaPreview.setRuta(rutasPreview, mapa.puertas)
        mapaPreview.setMuroPuntos(muroPuntosPreview)

        // Muestra los datos generales del mapa guardado.
        titulo.text = mapa.nombre
        findViewById<TextView>(R.id.tvOesteGuardado).text = "Oeste\n${"%.1f".format(mapa.oeste)} M"
        findViewById<TextView>(R.id.tvNorteGuardado).text = "Norte\n${"%.1f".format(mapa.norte)} M"
        findViewById<TextView>(R.id.tvSurGuardado).text = "Sur\n${"%.1f".format(mapa.sur)} M"
        findViewById<TextView>(R.id.tvEsteGuardado).text = "Este\n${"%.1f".format(mapa.este)} M"
        findViewById<TextView>(R.id.tvTiempo).text = "Tiempo: ${formatearTiempo(mapa.tiempoSegundos)}\n"
        findViewById<TextView>(R.id.tvRuta).text = "Recorrido: ${rutas.size} puntos | Auto: $puntosAuto\n"
        findViewById<TextView>(R.id.tvDistanciaGuardada).text = "Distancia: ${"%.1f".format(mapa.distanciaTotal)} m\n"
        findViewById<TextView>(R.id.tvPequenosGuardados).text = "Obstaculos pequenos: ${mapa.pequenos}\n"
        findViewById<TextView>(R.id.tvGrandesGuardados).text = "Obstaculos grandes: ${mapa.grandes}\n"
        findViewById<TextView>(R.id.tvPuertas).text = "Puertas posibles: ${mapa.puertas}\n"

        // Cierra el visor.
        btnBack.setOnClickListener {
            finish()
        }

        // Elimina el mapa actual de la base de datos.
        btnEliminar.setOnClickListener {
            db.eliminarMapa(mapa.id)
            Toast.makeText(this, "Mapa eliminado", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Exporta el mapa con un nombre seguro para archivo.
        btnExportar.setOnClickListener {
            val nombreSeguro = mapa.nombre
                .replace(Regex("[^A-Za-z0-9_-]"), "_")
                .take(40)
            crearArchivo.launch("$nombreSeguro${MapaFileManager.EXTENSION}")
        }
    }

    // Lanza el dialogo de Android para compartir el archivo exportado.
    private fun compartirArchivo(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = MapaFileManager.MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(intent, "Compartir mapa"))
    }

    private fun formatearTiempo(totalSegundos: Int): String {
        val minutos = totalSegundos / 60
        val segundos = totalSegundos % 60
        return "%02d:%02d".format(minutos, segundos)
    }
}
