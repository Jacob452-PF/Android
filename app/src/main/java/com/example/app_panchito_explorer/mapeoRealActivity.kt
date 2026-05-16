package com.example.app_panchito_explorer

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
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
    private lateinit var mapaPreview: MapaPreviewView
    private val botonesActivos = arrayListOf<View>()

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
        val btnStopModo = findViewById<LinearLayout>(R.id.btnStopModo)
        val btnMedirModo = findViewById<LinearLayout>(R.id.btnMedirModo)
        val btnAutoModo = findViewById<LinearLayout>(R.id.btnAutoModo)

        tvOeste = findViewById(R.id.tvOeste)
        tvNorte = findViewById(R.id.tvNorte)
        tvSur = findViewById(R.id.tvSur)
        tvEste = findViewById(R.id.tvEste)
        tvDistancia = findViewById(R.id.tvDistancia)
        tvPequenos = findViewById(R.id.tvPequenos)
        tvGrandes = findViewById(R.id.tvGrandes)
        mapaPreview = findViewById(R.id.mapaPreview)
        botonesActivos.addAll(
            listOf(
                btnUp,
                btnDown,
                btnLeft,
                btnRight,
                btnStop,
                btnIniciarSesion,
                btnStopModo,
                btnMedirModo,
                btnAutoModo
            )
        )

        BluetoothManager.enviarDato("I")
        marcarBotonActivo(btnIniciarSesion)
        actualizarUI()

        btnIniciarSesion.setOnClickListener {
            marcarBotonActivo(btnIniciarSesion)
            modoManual = true
            reiniciarMapa()
            enviarComando("I")
        }

        btnMedirModo.setOnClickListener {
            marcarBotonActivo(btnMedirModo)
            modoManual = false
            enviarComando("M")
        }

        btnAutoModo.setOnClickListener {
            marcarBotonActivo(btnAutoModo)
            modoManual = false
            enviarComando("A")
        }

        btnUp.setOnClickListener {
            marcarBotonActivo(btnUp)
            modoManual = true
            enviarComando("F")
            registrarPasoManual(0.18)
        }

        btnDown.setOnClickListener {
            marcarBotonActivo(btnDown)
            modoManual = true
            enviarComando("B")
        }

        btnLeft.setOnClickListener {
            marcarBotonActivo(btnLeft)
            modoManual = true
            heading = (heading + 270) % 360
            enviarComando("L")
        }

        btnRight.setOnClickListener {
            marcarBotonActivo(btnRight)
            modoManual = true
            heading = (heading + 90) % 360
            enviarComando("R")
        }

        btnStop.setOnClickListener {
            marcarBotonActivo(btnStop)
            enviarComando("S")
        }

        btnStopModo.setOnClickListener {
            marcarBotonActivo(btnStopModo)
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
        mapaPreview.setRutaDesdeTexto(rutaManual, puertasDetectadas)
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
        mapaPreview.setRutaDesdeTexto(rutaManual, puertasDetectadas)
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

    private fun marcarBotonActivo(activo: View) {
        botonesActivos.forEach { boton ->
            val seleccionado = boton == activo
            val fondo = if (seleccionado) Color.rgb(0, 229, 255) else Color.rgb(14, 60, 110)
            val icono = if (seleccionado) Color.rgb(10, 42, 74) else Color.rgb(0, 229, 255)
            val texto = if (seleccionado) Color.rgb(10, 42, 74) else Color.WHITE

            boton.backgroundTintList = ColorStateList.valueOf(fondo)

            if (boton is ImageButton) {
                boton.imageTintList = ColorStateList.valueOf(icono)
            }

            if (boton is LinearLayout) {
                pintarHijosBoton(boton, icono, texto)
            }
        }
    }

    private fun pintarHijosBoton(contenedor: LinearLayout, icono: Int, texto: Int) {
        for (i in 0 until contenedor.childCount) {
            when (val hijo = contenedor.getChildAt(i)) {
                is ImageView -> hijo.imageTintList = ColorStateList.valueOf(icono)
                is TextView -> hijo.setTextColor(texto)
            }
        }
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
