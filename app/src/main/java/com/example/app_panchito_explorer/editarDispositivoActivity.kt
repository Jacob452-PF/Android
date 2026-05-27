package com.example.app_panchito_explorer

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class editarDispositivoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(R.layout.activity_editar_dispositivo)

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

        // BOTONES

        val btnBack =
            findViewById<ImageView>(R.id.btnBack)

        val btnGuardar =
            findViewById<Button>(R.id.btnGuardar)

        val btnConexion =
            findViewById<Button>(R.id.btnConexion)

        // EDITTEXT

        val editNombre =
            findViewById<EditText>(R.id.editNombre)

        val editDescripcion =
            findViewById<EditText>(R.id.editDescripcion)

        // TEXTVIEW

        val textMac =
            findViewById<TextView>(R.id.textMac)

        val textEstado =
            findViewById<TextView>(R.id.textEstado)

        // =========================
        // RECIBIR DATOS
        // =========================

        val nombre =
            intent.getStringExtra("device_name")
                ?: intent.getStringExtra("nombre")

        val descripcion =
            intent.getStringExtra("descripcion")

        val mac =
            intent.getStringExtra("device_address")
                ?: intent.getStringExtra("mac")

        // MOSTRAR DATOS

        editNombre.setText(
            nombre ?: "Panchito_map"
        )

        editDescripcion.setText(
            descripcion ?: "Auto de mapeo remoto"
        )

        textMac.text =
            mac ?: "00:11:22:33:44"

        textEstado.text =
            "Activo"

        // =========================
        // ESTADO CONEXION
        // =========================

        btnConexion.text =
            "Conectado ✔"

        // =========================
        // BOTON REGRESAR
        // =========================

        btnBack.setOnClickListener {

            finish()
        }

        // =========================
        // GUARDAR CAMBIOS
        // =========================

        btnGuardar.setOnClickListener {

            val nuevoNombre =
                editNombre.text.toString()

            val nuevaDescripcion =
                editDescripcion.text.toString()

            // VALIDAR CAMPOS

            if (nuevoNombre.isEmpty()) {

                Toast.makeText(
                    this,
                    "Ingrese un nombre",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            if (nuevaDescripcion.isEmpty()) {

                Toast.makeText(
                    this,
                    "Ingrese una descripción",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val direccion = mac ?: ""
            DBHelper(this).guardarDispositivoLocal(nuevoNombre, direccion)

            if (BluetoothManager.direccionDispositivo == direccion) {
                BluetoothManager.nombreDispositivo = nuevoNombre
            }

            Toast.makeText(
                this,
                "Cambios guardados",
                Toast.LENGTH_SHORT
            ).show()

            // CERRAR ACTIVITY

            finish()
        }
    }
}
