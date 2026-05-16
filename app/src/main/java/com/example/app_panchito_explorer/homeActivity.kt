package com.example.app_panchito_explorer

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.widget.ImageButton

class homeActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }


        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        // CARDS (opciones del menú principal)
        val cardConectar = findViewById<LinearLayout>(R.id.cardConectar)
        val cardMapear = findViewById<LinearLayout>(R.id.cardMapear)
        val cardGuardados = findViewById<LinearLayout>(R.id.cardGuardados)
        val cardAuto = findViewById<LinearLayout>(R.id.cardAcercaApp)

// Navegación de cards (igual que bottom nav)

        cardConectar.setOnClickListener {
            startActivity(Intent(this, bluetoothActivity::class.java))
        }

        cardMapear.setOnClickListener {
            startActivity(Intent(this, antesDeMapearActivity::class.java))
        }

        cardGuardados.setOnClickListener {
            startActivity(Intent(this, guardadosActivity::class.java))
        }

        cardAuto.setOnClickListener {
            startActivity(Intent(this, AsercaDeLaAppActivity::class.java))
        }

        // marcar el icono activo
        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener {

            when (it.itemId) {

                R.id.nav_home -> {
                    true
                }

                R.id.nav_bluetooth -> {
                    startActivity(Intent(this, bluetoothActivity::class.java))
                    true
                }

                R.id.nav_map -> {
                    startActivity(Intent(this, antesDeMapearActivity::class.java))
                    true
                }

                R.id.nav_files -> {
                    startActivity(Intent(this, guardadosActivity::class.java))
                    true
                }

                else -> false
            }

        }
    }
}