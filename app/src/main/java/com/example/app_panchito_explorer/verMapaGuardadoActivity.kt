package com.example.app_panchito_explorer

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class verMapaGuardadoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_mapa_guardado)

        val titulo = findViewById<TextView>(R.id.tituloMapa)
        val nombre = intent.getStringExtra("nombre")

        titulo.text = nombre

        val btnBack = findViewById<ImageView>(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }
    }
}