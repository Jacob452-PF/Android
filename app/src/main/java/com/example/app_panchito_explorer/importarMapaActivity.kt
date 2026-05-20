package com.example.app_panchito_explorer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class importarMapaActivity : AppCompatActivity() {

    // Selector de archivos del sistema para elegir un mapa exportado.
    private val abrirArchivo = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            importarDesdeUri(uri)
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
        val btnIniciar = findViewById<Button>(R.id.btnIniciar)

        // Permite importar directamente si Android abre la app desde un archivo.
        if (intent?.action == Intent.ACTION_VIEW && intent?.data != null) {
            importarDesdeUri(intent.data!!)
        }

        // Abre el selector limitado a los tipos de archivo compatibles.
        btnIniciar.setOnClickListener {
            abrirArchivo.launch(
                arrayOf(
                    MapaFileManager.MIME_TYPE,
                    "application/octet-stream",
                    "text/plain"
                )
            )
        }

        // Cierra la pantalla sin importar.
        btnBack.setOnClickListener {
            finish()
        }

        btnCancelar.setOnClickListener {
            finish()
        }
    }

    // Lee el contenido del archivo, lo importa a la base de datos y abre el visor.
    private fun importarDesdeUri(uri: Uri) {
        try {
            val contenido = contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader().readText()
            }

            if (contenido.isNullOrBlank()) {
                Toast.makeText(this, "Archivo vacio", Toast.LENGTH_SHORT).show()
                return
            }

            // MapaFileManager valida y guarda el archivo importado.
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
}
