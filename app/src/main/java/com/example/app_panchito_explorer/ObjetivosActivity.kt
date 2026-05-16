
package com.example.app_panchito_explorer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ObjetivosActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_objetivos)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 🔹 REFERENCIAS
        val headerObjetivo = findViewById<LinearLayout>(R.id.headerObjetivo)
        val contentObjetivo = findViewById<TextView>(R.id.contentObjetivo)

        val headerMision = findViewById<LinearLayout>(R.id.headerMision)
        val contentMision = findViewById<TextView>(R.id.contentMision)

        val headerVision = findViewById<LinearLayout>(R.id.headerVision)
        val contentVision = findViewById<TextView>(R.id.contentVision)

        val cardTutorial = findViewById<LinearLayout>(R.id.cardTutorial)

        // 🔹 FUNCIÓN (tipo acordeón)
        fun expandirSolo(abrir: View, vararg otros: View) {
            abrir.visibility =
                if (abrir.visibility == View.GONE) View.VISIBLE else View.GONE

            otros.forEach { it.visibility = View.GONE }
        }

        // 🔹 EVENTOS
        headerObjetivo.setOnClickListener {
            expandirSolo(contentObjetivo, contentMision, contentVision)
        }

        headerMision.setOnClickListener {
            expandirSolo(contentMision, contentObjetivo, contentVision)
        }

        headerVision.setOnClickListener {
            expandirSolo(contentVision, contentObjetivo, contentMision)
        }

        // 🔹 BOTÓN TUTORIAL
        cardTutorial.setOnClickListener {
            startActivity(Intent(this, manualActivity::class.java))
        }
    }
}

