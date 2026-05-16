
package com.example.app_panchito_explorer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class VideoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_video)

        val videoId = "LcApcwU06mM"

        // Intenta abrir en la app de YouTube
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))

        // Si no existe, abre en navegador
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://youtu.be/$videoId"))

        try {
            startActivity(appIntent)
        } catch (e: Exception) {
            startActivity(webIntent)
        }

        // Cierra esta Activity para no dejar pantalla vacía
        finish()
    }
}
