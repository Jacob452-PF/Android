package com.example.app_panchito_explorer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class importarMapaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_importar_mapa)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnCancelar = findViewById<Button>(R.id.btnCancelar)
        val btniniciar = findViewById<Button>(R.id.btnIniciar)

        btniniciar.setOnClickListener {

            val intent = Intent(this, mapeoRealActivity::class.java)
            startActivity(intent)


        }

        btnBack.setOnClickListener {
            finish()
        }

        btnCancelar.setOnClickListener {
            finish()
        }
    }
}