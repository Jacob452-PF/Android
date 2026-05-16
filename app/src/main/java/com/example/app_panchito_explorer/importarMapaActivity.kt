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

    private val abrirArchivo = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            importarDesdeUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_importar_mapa)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnCancelar = findViewById<Button>(R.id.btnCancelar)
        val btnIniciar = findViewById<Button>(R.id.btnIniciar)

        if (intent?.action == Intent.ACTION_VIEW && intent?.data != null) {
            importarDesdeUri(intent.data!!)
        }

        btnIniciar.setOnClickListener {
            abrirArchivo.launch(
                arrayOf(
                    MapaFileManager.MIME_TYPE,
                    "application/octet-stream",
                    "text/plain"
                )
            )
        }

        btnBack.setOnClickListener {
            finish()
        }

        btnCancelar.setOnClickListener {
            finish()
        }
    }

    private fun importarDesdeUri(uri: Uri) {
        try {
            val contenido = contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader().readText()
            }

            if (contenido.isNullOrBlank()) {
                Toast.makeText(this, "Archivo vacio", Toast.LENGTH_SHORT).show()
                return
            }

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
