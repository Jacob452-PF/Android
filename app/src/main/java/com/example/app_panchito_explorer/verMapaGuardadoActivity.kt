package com.example.app_panchito_explorer

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class verMapaGuardadoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_mapa_guardado)

        val db = DBHelper(this)
        val mapaId = intent.getIntExtra("mapa_id", -1)
        val mapa = db.obtenerMapa(mapaId)

        val titulo = findViewById<TextView>(R.id.tituloMapa)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnEliminar = findViewById<Button>(R.id.btnEliminar)

        if (mapa == null) {
            titulo.text = "Mapa no encontrado"
            Toast.makeText(this, "No se encontro el mapa", Toast.LENGTH_SHORT).show()
            btnBack.setOnClickListener { finish() }
            return
        }

        val rutas = db.obtenerRutas(mapa.id)

        titulo.text = mapa.nombre
        findViewById<TextView>(R.id.tvOesteGuardado).text = "Oeste\n${"%.1f".format(mapa.oeste)} M"
        findViewById<TextView>(R.id.tvNorteGuardado).text = "Norte\n${"%.1f".format(mapa.norte)} M"
        findViewById<TextView>(R.id.tvSurGuardado).text = "Sur\n${"%.1f".format(mapa.sur)} M"
        findViewById<TextView>(R.id.tvEsteGuardado).text = "Este\n${"%.1f".format(mapa.este)} M"
        findViewById<TextView>(R.id.tvTiempo).text = "Tiempo: ${mapa.tiempoSegundos} s\n"
        findViewById<TextView>(R.id.tvRuta).text = "Ruta manual: ${rutas.size} puntos\n"
        findViewById<TextView>(R.id.tvDistanciaGuardada).text = "Distancia: ${"%.1f".format(mapa.distanciaTotal)} m\n"
        findViewById<TextView>(R.id.tvPequenosGuardados).text = "Obstaculos pequenos: ${mapa.pequenos}\n"
        findViewById<TextView>(R.id.tvGrandesGuardados).text = "Obstaculos grandes: ${mapa.grandes}\n"
        findViewById<TextView>(R.id.tvPuertas).text = "Puertas posibles: ${mapa.puertas}\n"

        btnBack.setOnClickListener {
            finish()
        }

        btnEliminar.setOnClickListener {
            db.eliminarMapa(mapa.id)
            Toast.makeText(this, "Mapa eliminado", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
