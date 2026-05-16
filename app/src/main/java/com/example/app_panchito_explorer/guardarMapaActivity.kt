package com.example.app_panchito_explorer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class guardarMapaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_guardar_mapa)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 🔥 REFERENCIAS
        val tvOeste = findViewById<TextView>(R.id.tvOeste)
        val tvNorte = findViewById<TextView>(R.id.tvNorte)
        val tvSur = findViewById<TextView>(R.id.tvSur)
        val tvEste = findViewById<TextView>(R.id.tvEste)

        val tvDistancia = findViewById<TextView>(R.id.tvDistancia)
        val tvPequenos = findViewById<TextView>(R.id.tvPequenos)
        val tvGrandes = findViewById<TextView>(R.id.tvGrandes)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnGuardar = findViewById<Button>(R.id.btnGuardar)

        // 🔥 DATOS RECIBIDOS
        val oeste = intent.getDoubleExtra("oeste", 0.0)
        val norte = intent.getDoubleExtra("norte", 0.0)
        val sur = intent.getDoubleExtra("sur", 0.0)
        val este = intent.getDoubleExtra("este", 0.0)

        val distancia = intent.getDoubleExtra("distancia_total", 0.0)

        val pequenos = intent.getIntExtra("pequenos", 0)
        val grandes = intent.getIntExtra("grandes", 0)

        // 🔥 MOSTRAR DATOS
        tvOeste.text = "Oeste\n${oeste} M"
        tvNorte.text = "Norte\n${norte} M"
        tvSur.text = "Sur\n${sur} M"
        tvEste.text = "Este\n${este} M"

        tvDistancia.text = "Distancia: ${distancia} M"

        tvPequenos.text = "Obstáculos pequeños: $pequenos"
        tvGrandes.text = "Obstáculos grandes: $grandes"

        // 🔙 VOLVER
        btnBack.setOnClickListener {
            finish()
        }

        // 💾 GUARDAR MAPA
        btnGuardar.setOnClickListener {

            val intent = Intent(this, guardadosActivity::class.java)

            startActivity(intent)
            finish()
        }
    }
}