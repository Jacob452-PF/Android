package com.example.app_panchito_explorer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.max

class mapeoRealActivity : AppCompatActivity() {

    private var oeste = 0.0
    private var norte = 0.0
    private var sur = 0.0
    private var este = 0.0

    private var distanciaTotal = 0.0
    private var tiempoSegundos = 0
    private var bateria = 0
    private var obstaculosPequenos = 0
    private var obstaculosGrandes = 0
    private var puertasDetectadas = 0

    private var modoManual = true
    private var heading = 0
    private var posX = 0.0
    private var posY = 0.0
    private var ordenRuta = 0
    private val rutaManual = arrayListOf<String>()

    private lateinit var tvOeste: TextView
    private lateinit var tvNorte: TextView
    private lateinit var tvSur: TextView
    private lateinit var tvEste: TextView
    private lateinit var tvDistancia: TextView
    private lateinit var tvPequenos: TextView
    private lateinit var tvGrandes: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mapeo_real)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarMapa)
        val btnUp = findViewById<ImageButton>(R.id.btnUp)
        val btnDown = findViewById<ImageButton>(R.id.btnDown)
        val btnLeft = findViewById<ImageButton>(R.id.btnLeft)
        val btnRight = findViewById<ImageButton>(R.id.btnRight)
        val btnStop = findViewById<ImageButton>(R.id.btnStop)
        val btnIniciarSesion = findViewById<LinearLayout>(R.id.btnIniciarSesion)
        val btnMedirModo = findViewById<LinearLayout>(R.id.btnMedirModo)
        val btnAutoModo = findViewById<LinearLayout>(R.id.btnAutoModo)

        tvOeste = findViewById(R.id.tvOeste)
        tvNorte = findViewById(R.id.tvNorte)
        tvSur = findViewById(R.id.tvSur)
        tvEste = findViewById(R.id.tvEste)
        tvDistancia = findViewById(R.id.tvDistancia)
        tvPequenos = findViewById(R.id.tvPequenos)
        tvGrandes = findViewById(R.id.tvGrandes)

        BluetoothManager.enviarDato("I")
        actualizarUI()

        btnIniciarSesion.setOnClickListener {
            modoManual = true
            reiniciarMapa()
            enviarComando("I")
        }

        btnMedirModo.setOnClickListener {
            modoManual = false
            enviarComando("M")
        }

        btnAutoModo.setOnClickListener {
            modoManual = false
            enviarComando("A")
        }

        btnUp.setOnClickListener {
            modoManual = true
            enviarComando("F")
            registrarPasoManual(0.18)
        }

        btnDown.setOnClickListener {
            modoManual = true
            enviarComando("B")
        }

        btnLeft.setOnClickListener {
            modoManual = true
            heading = (heading + 270) % 360
            enviarComando("L")
        }

        btnRight.setOnClickListener {
            modoManual = true
            heading = (heading + 90) % 360
            enviarComando("R")
        }

        btnStop.setOnClickListener {
            enviarComando("S")
        }

        btnGuardar.setOnClickListener {
            val intent = Intent(this, guardarMapaActivity::class.java)
            intent.putExtra("distancia_total", distanciaTotal)
            intent.putExtra("tiempo_segundos", tiempoSegundos)
            intent.putExtra("bateria", bateria)
            intent.putExtra("oeste", oeste)
            intent.putExtra("norte", norte)
            intent.putExtra("sur", sur)
            intent.putExtra("este", este)
            intent.putExtra("pequenos", obstaculosPequenos)
            intent.putExtra("grandes", obstaculosGrandes)
            intent.putExtra("puertas", puertasDetectadas)
            intent.putStringArrayListExtra("ruta_manual", rutaManual)
            startActivity(intent)
        }

        btnBack.setOnClickListener {
            mostrarAdvertenciaSalida()
        }

        onBackPressedDispatcher.addCallback(this) {
            mostrarAdvertenciaSalida()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun enviarComando(comando: String) {
        if (!BluetoothManager.enviarDato(comando)) {
            Toast.makeText(this, "No se pudo enviar comando Bluetooth", Toast.LENGTH_SHORT).show()
        }
    }

    private fun registrarPasoManual(distancia: Double) {
        if (!modoManual) {
            return
        }

        distanciaTotal += distancia

        when (heading) {
            0 -> {
                posY += distancia
                norte += distancia
            }
            90 -> {
                posX += distancia
                este += distancia
            }
            180 -> {
                posY -= distancia
                sur += distancia
            }
            270 -> {
                posX -= distancia
                oeste += distancia
            }
        }

        rutaManual.add("${ordenRuta++},$posX,$posY,manual")
        detectarPuertaPorAbertura()
        actualizarUI()
    }

    private fun reiniciarMapa() {
        oeste = 0.0
        norte = 0.0
        sur = 0.0
        este = 0.0
        distanciaTotal = 0.0
        tiempoSegundos = 0
        obstaculosPequenos = 0
        obstaculosGrandes = 0
        puertasDetectadas = 0
        heading = 0
        posX = 0.0
        posY = 0.0
        ordenRuta = 0
        rutaManual.clear()
        actualizarUI()
    }

    private fun detectarPuertaPorAbertura() {
        val anchoEstimadoCm = max(norte + sur, este + oeste) * 100

        if (anchoEstimadoCm >= 70 && puertasDetectadas == 0) {
            puertasDetectadas = 1
        }
    }

    private fun actualizarUI() {
        tvOeste.text = "Oeste\n${"%.1f".format(oeste)} M"
        tvNorte.text = "Norte\n${"%.1f".format(norte)} M"
        tvSur.text = "Sur\n${"%.1f".format(sur)} M"
        tvEste.text = "Este\n${"%.1f".format(este)} M"
        tvDistancia.text = "Distancia\n${"%.1f".format(distanciaTotal)} M"
        tvPequenos.text = "Pequenos\n$obstaculosPequenos"
        tvGrandes.text = "Grandes\n$obstaculosGrandes"
    }

    private fun mostrarAdvertenciaSalida() {
        AlertDialog.Builder(this)
            .setTitle("Cancelar mapeo")
            .setMessage(
                "El proceso de mapeo aun esta en ejecucion.\n\n" +
                    "Si sales ahora se perderan los datos recolectados.\n\n" +
                    "Deseas continuar?"
            )
            .setPositiveButton("Salir") { _, _ ->
                BluetoothManager.enviarDato("X")
                finish()
            }
            .setNegativeButton("Continuar mapeando", null)
            .show()
    }
}
