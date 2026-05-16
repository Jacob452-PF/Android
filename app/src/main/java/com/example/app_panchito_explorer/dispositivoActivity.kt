package com.example.app_panchito_explorer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class dispositivoActivity : AppCompatActivity() {

    // =========================
    // VARIABLES
    // =========================

    private lateinit var textDevice: TextView

    private lateinit var textInfo: TextView

    private lateinit var textEstado: TextView

    private lateinit var textConexion: TextView

    private var nombreDispositivo = "Desconocido"

    private var direccionDispositivo = "Sin dirección"

    // =========================
    // ON CREATE
    // =========================

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(R.layout.activity_dispositivo)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->

            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                bars.left,
                bars.top,
                bars.right,
                bars.bottom
            )

            insets
        }

        // =========================
        // COMPONENTES
        // =========================

        val btnEditar =
            findViewById<Button>(R.id.btnEditar)

        val btnBack =
            findViewById<ImageView>(R.id.btnBack)

        val btnDesconectar =
            findViewById<Button>(R.id.btnDesconectar)

        val btnMapear =
            findViewById<Button>(R.id.btnMapear)

        textDevice =
            findViewById(R.id.textDevice)

        textInfo =
            findViewById(R.id.textInfo)

        textEstado =
            findViewById(R.id.textEstado)

        textConexion =
            findViewById(R.id.textConexion)

        // =========================
        // RECIBIR DISPOSITIVO
        // =========================

        val deviceInfo =
            intent.getStringExtra("device")

        if (deviceInfo != null) {

            val partes = deviceInfo.split("\n")

            nombreDispositivo =
                partes.getOrNull(0)
                    ?: "Desconocido"

            direccionDispositivo =
                partes.getOrNull(1)
                    ?: "Sin dirección"

            textDevice.text =
                nombreDispositivo

            textInfo.text =
                "Dirección:\n$direccionDispositivo"

            textEstado.text =
                "Estado: Conectado"

            textConexion.text =
                "HMSoft BLE conectado correctamente"

        } else {

            textDevice.text =
                "Sin dispositivo"

            textInfo.text =
                "No se recibió información"

            textEstado.text =
                "Estado: Desconectado"

            textConexion.text =
                "Sin conexión BLE"
        }

        // =========================
        // BOTON EDITAR
        // =========================

        btnEditar.setOnClickListener {

            val intent =
                Intent(
                    this,
                    editarDispositivoActivity::class.java
                )

            intent.putExtra(
                "device_name",
                nombreDispositivo
            )

            intent.putExtra(
                "device_address",
                direccionDispositivo
            )

            startActivity(intent)
        }

        // =========================
        // BOTON REGRESAR
        // =========================

        btnBack.setOnClickListener {

            finish()
        }

        // =========================
        // BOTON DESCONECTAR
        // =========================

        btnDesconectar.setOnClickListener {

            BluetoothManager.cerrarConexion()

            textEstado.text =
                "Estado: Desconectado"

            textConexion.text =
                "Conexión BLE cerrada"

            Toast.makeText(
                this,
                "HMSoft desconectado",
                Toast.LENGTH_SHORT
            ).show()

            finish()
        }

        // =========================
        // BOTON MAPEAR
        // =========================

        btnMapear.setOnClickListener {

            if (!BluetoothManager.conectado) {

                Toast.makeText(
                    this,
                    "Conecta un dispositivo primero",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            Toast.makeText(
                this,
                "Abriendo sistema de mapeo...",
                Toast.LENGTH_SHORT
            ).show()

            val intent =
                Intent(
                    this,
                    antesDeMapearActivity::class.java
                )

            // enviar datos al mapeo

            intent.putExtra(
                "device_name",
                nombreDispositivo
            )

            intent.putExtra(
                "device_address",
                direccionDispositivo
            )

            startActivity(intent)
        }
    }
}
