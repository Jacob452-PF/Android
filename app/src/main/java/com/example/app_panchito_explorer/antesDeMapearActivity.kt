package com.example.app_panchito_explorer

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONObject

class antesDeMapearActivity : AppCompatActivity() {

    private lateinit var bateriaTexto: TextView
    private lateinit var bateriaBarra: ProgressBar
    private lateinit var textDispositivo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(R.layout.activity_antes_de_mapear)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->

            val bars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                bars.left,
                bars.top,
                bars.right,
                bars.bottom
            )

            insets
        }

        // =========================
        // VISTAS
        // =========================

        val btnBack =
            findViewById<ImageView>(R.id.btnBack)

        val btnBluetooth =
            findViewById<ImageView>(R.id.btnBluetooth)

        val btnIniciar =
            findViewById<Button>(R.id.btnIniciarMapa)

        bateriaTexto =
            findViewById(R.id.textBateria)

        bateriaBarra =
            findViewById(R.id.progressBateria)

        textDispositivo =
            findViewById(R.id.textDispositivo)

        actualizarEstadoBluetooth(mostrarAviso = true)

        // =========================
        // BOTON REGRESAR
        // =========================

        btnBack.setOnClickListener {

            finish()
        }

        // =========================
        // BOTON BLUETOOTH
        // =========================

        btnBluetooth.setOnClickListener {

            val intent =
                Intent(
                    this,
                    bluetoothActivity::class.java
                )

            startActivity(intent)
        }

        // =========================
        // INICIAR MAPEO
        // =========================

        btnIniciar.setOnClickListener {

            actualizarEstadoBluetooth(mostrarAviso = false)

            if (!BluetoothManager.conectado) {

                Toast.makeText(
                    this,
                    "Conecta un dispositivo primero",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val intent =
                Intent(
                    this,
                    mapeoRealActivity::class.java
                )

            startActivity(intent)
        }
    }

    override fun onResume() {

        super.onResume()

        BluetoothManager.onLineaRecibida = { linea ->
            runOnUiThread {
                actualizarBateriaDesdeLinea(linea)
                actualizarEstadoBluetooth(mostrarAviso = false)
            }
        }

        if (::bateriaTexto.isInitialized) {

            actualizarEstadoBluetooth(mostrarAviso = false)
        }
    }

    override fun onPause() {
        if (BluetoothManager.onLineaRecibida != null) {
            BluetoothManager.onLineaRecibida = null
        }
        super.onPause()
    }

    private fun actualizarBateriaDesdeLinea(linea: String) {
        val texto = linea.trim()
        val match = Regex("""^(BAT|BATERIA|B)[:=]\s*(\d{1,3})%?$""", RegexOption.IGNORE_CASE).find(texto)

        if (match != null) {
            BluetoothManager.bateria = match.groupValues[2].toInt().coerceIn(0, 100)
            return
        }

        try {
            val json = JSONObject(texto)
            if (json.has("b")) {
                BluetoothManager.bateria = json.optInt("b", BluetoothManager.bateria).coerceIn(0, 100)
            }
        } catch (_: Exception) {
        }
    }

    private fun actualizarEstadoBluetooth(mostrarAviso: Boolean) {

        val conectado =
            BluetoothManager.conectado

        if (conectado) {

            textDispositivo.text =
                BluetoothManager.nombreDispositivo
                    ?: "Dispositivo conectado"

            val bateria =
                BluetoothManager.bateria

            bateriaTexto.text =
                if (bateria > 0) "$bateria%" else "Sin lectura"

            bateriaBarra.progress =
                bateria

            when {

                bateria >= 80 -> {

                    bateriaBarra.progressTintList =
                        ColorStateList.valueOf(Color.CYAN)
                }

                bateria >= 40 -> {

                    bateriaBarra.progressTintList =
                        ColorStateList.valueOf(Color.YELLOW)
                }

                else -> {

                    bateriaBarra.progressTintList =
                        ColorStateList.valueOf(Color.RED)
                }
            }

        } else {

            textDispositivo.text =
                "Sin dispositivo"

            bateriaTexto.text =
                "Sin conexion"

            bateriaBarra.progress =
                0

            bateriaBarra.progressTintList =
                ColorStateList.valueOf(Color.RED)

            if (mostrarAviso) {

                Toast.makeText(
                    this,
                    "No hay dispositivo conectado",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
