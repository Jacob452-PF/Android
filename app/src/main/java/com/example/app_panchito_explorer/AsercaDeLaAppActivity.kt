
package com.example.app_panchito_explorer

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class AsercaDeLaAppActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_aserca_de_la_app)

        // 🔹 REFERENCIAS NUEVAS (YA NO EXISTEN headerObjetivo, etc.)
        val cardManual = findViewById<LinearLayout>(R.id.cardManual)
        val cardVideo = findViewById<LinearLayout>(R.id.cardVideo)
        val cardObjetivos = findViewById<LinearLayout>(R.id.cardObjetivos)

        // 🔹 EVENTOS

        // Abrir manual (ViewPager)
        cardManual.setOnClickListener {
            startActivity(Intent(this, manualActivity::class.java))
        }

        cardVideo.setOnClickListener {
            startActivity(Intent(this, VideoActivity::class.java))
        }

        // Abrir pantalla de Objetivo, Misión y Visión
        cardObjetivos.setOnClickListener {
            startActivity(Intent(this, ObjetivosActivity::class.java))
        }
    }
}

