package com.example.app_panchito_explorer

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class guardadosActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_guardados)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        val btnAgregar = findViewById<ImageView>(R.id.btnAgregar)

        btnAgregar.setOnClickListener {
            val intent = Intent(this, importarMapaActivity::class.java)
            startActivity(intent)
        }

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        val ruta1 = findViewById<LinearLayout>(R.id.ruta1)
        val ruta2 = findViewById<LinearLayout>(R.id.ruta2)

        ruta1.setOnClickListener {
            val intent = Intent(this, verMapaGuardadoActivity::class.java)
            intent.putExtra("nombre", "Ruta #1")
            startActivity(intent)
        }

        ruta2.setOnClickListener {
            val intent = Intent(this, verMapaGuardadoActivity::class.java)
            intent.putExtra("nombre", "Ruta #2")
            startActivity(intent)
        }

        // marcar el icono activo
        bottomNav.selectedItemId = R.id.nav_files

        // botón regresar
        btnBack.setOnClickListener {
            finish()
        }

        // navegación inferior
        bottomNav.setOnItemSelectedListener {

            when (it.itemId) {

                R.id.nav_home -> {
                    startActivity(Intent(this, homeActivity::class.java))
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
                    true
                }

                else -> false
            }

        }
    }
}