package com.example.app_panchito_explorer

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
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
import org.json.JSONObject
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

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
    private var velocidadNivel = 1

    private var mapeoActivo = false
    private var modoManual = true
    private var modoAuto = false
    private var localizacionActiva = false
    private var escaneoConstruyeMapa = false
    private var mostrarMedidasMuros = true
    private var headingGrados = 0.0
    private var posX = 0.0
    private var posY = 0.0
    private var ordenRuta = 0
    private var grupoMuroActual = 0
    private var iniciandoTramo = true 
    private val rutaManual = arrayListOf<String>()
    private val muroPuntos = arrayListOf<MuroPunto>()

    private lateinit var tvOeste: TextView
    private lateinit var tvNorte: TextView
    private lateinit var tvSur: TextView
    private lateinit var tvEste: TextView
    private lateinit var tvDistancia: TextView
    private lateinit var tvPequenos: TextView
    private lateinit var tvGrandes: TextView
    private lateinit var tvVelocidadNumero: TextView
    private lateinit var mapaPreview: MapaPreviewView
    private val botonesActivos = arrayListOf<View>()
    private var botonMovimientoActivo: View? = null
    private val movimientoHandler = Handler(Looper.getMainLooper())
    private val localizacionHandler = Handler(Looper.getMainLooper())
    private var comandoMovimientoActivo: String? = null
    private var ultimoTickMovimiento = 0L

    private val localizacionRunnable = object : Runnable {
        override fun run() {
            if (!localizacionActiva) return
            enviarComando("U")
            localizacionHandler.postDelayed(this, LOCALIZACION_INTERVALO_MS)
        }
    }

    private val movimientoRunnable = object : Runnable {
        override fun run() {
            val comando = comandoMovimientoActivo ?: return
            actualizarMovimientoManual(comando)
            movimientoHandler.postDelayed(this, MOVIMIENTO_TICK_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mapeo_real)

        tvOeste = findViewById(R.id.tvOeste)
        tvNorte = findViewById(R.id.tvNorte)
        tvSur = findViewById(R.id.tvSur)
        tvEste = findViewById(R.id.tvEste)
        tvDistancia = findViewById(R.id.tvDistancia)
        tvPequenos = findViewById(R.id.tvPequenos)
        tvGrandes = findViewById(R.id.tvGrandes)
        tvVelocidadNumero = findViewById(R.id.tvVelocidadNumero)
        mapaPreview = findViewById(R.id.mapaPreview)

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
        val btnUbicarModo = findViewById<LinearLayout>(R.id.btnUbicarModo)
        val btnVolverOrigen = findViewById<LinearLayout>(R.id.btnVolverOrigen)
        val panelVelocidad = findViewById<LinearLayout>(R.id.panelVelocidad)

        botonesActivos.addAll(listOf(btnUp, btnDown, btnLeft, btnRight, btnStop, btnIniciarSesion, btnMedirModo, btnAutoModo, btnUbicarModo, btnVolverOrigen))

        // --- ALERTA DE DESCONEXIÓN (MITAD SUPERIOR) ---
        BluetoothManager.onDesconectado = {
            if (mapeoActivo) {
                runOnUiThread {
                    if (!isFinishing) {
                        mapeoActivo = false
                        modoAuto = false
                        detenerLocalizacion()
                        detenerMovimientoManual(enviarStop = false)
                        findViewById<View>(R.id.layoutErrorConexion).visibility = View.VISIBLE
                        actualizarEstadosBotones()
                    }
                }
            }
        }

        findViewById<Button>(R.id.btnEntendidoError).setOnClickListener {
            finish()
        }

        btnIniciarSesion.setOnClickListener {
            salirModoAutoSiNecesario()
            mapeoActivo = !mapeoActivo
            if (mapeoActivo && rutaManual.isEmpty()) {
                reiniciarMapa()
                enviarComando("I")
                enviarComando(velocidadNivel.toString())
            }
            actualizarEstadosBotones()
        }

        btnMedirModo.setOnClickListener {
            salirModoAutoSiNecesario()
            iniciarEscaneoMuros(it)
            actualizarEstadosBotones()
        }

        btnAutoModo.setOnClickListener {
            detenerMovimientoManual()
            if (modoAuto) {
                modoAuto = false
                detenerLocalizacion()
                enviarComando("S")
            } else {
                modoAuto = true
                modoManual = false
                enviarComando("A")
                if (muroPuntos.size >= MIN_MUROS_PARA_LOCALIZAR) {
                    localizacionActiva = true
                    enviarComando("U")
                    localizacionHandler.postDelayed(localizacionRunnable, LOCALIZACION_INTERVALO_MS)
                }
            }
            botonMovimientoActivo = if (modoAuto) it else btnStop
            actualizarEstadosBotones()
        }

        btnUbicarModo.setOnClickListener { alternarLocalizacion(it) }
        panelVelocidad.setOnClickListener { cambiarVelocidad() }

        configurarBotonMovimiento(btnUp, "F")
        configurarBotonMovimiento(btnDown, "B")
        configurarBotonMovimiento(btnLeft, "L")
        configurarBotonMovimiento(btnRight, "R")
        
        btnStop.setOnClickListener {
            detenerMovimientoManual()
            pausarMapeo(it)
            enviarComando("S")
        }

        btnVolverOrigen.setOnClickListener { volverAlPuntoOrigen(it) }

        btnGuardar.setOnClickListener {
            val intent = Intent(this, guardarMapaActivity::class.java)
            intent.putExtra("distancia_total", distanciaTotal)
            intent.putExtra("tiempo_segundos", tiempoSegundos)
            intent.putExtra("bateria", bateria)
            intent.putExtra("oeste", oeste); intent.putExtra("norte", norte)
            intent.putExtra("sur", sur); intent.putExtra("este", este)
            intent.putExtra("pequenos", obstaculosPequenos)
            intent.putExtra("grandes", obstaculosGrandes)
            intent.putExtra("puertas", puertasDetectadas)
            intent.putStringArrayListExtra("ruta_manual", rutaManual)
            intent.putStringArrayListExtra("muro_puntos", ArrayList(muroPuntos.map { "${it.x},${it.y},${it.grupo}" }))
            startActivity(intent)
        }

        btnBack.setOnClickListener { mostrarAdvertenciaSalida() }
        onBackPressedDispatcher.addCallback(this) { mostrarAdvertenciaSalida() }

        BluetoothManager.onLineaRecibida = { linea ->
            runOnUiThread { procesarLineaBluetooth(linea) }
        }

        actualizarEstadosBotones()
        actualizarUI()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    private fun enviarComando(comando: String) {
        BluetoothManager.enviarDato(comando)
    }

    private fun cambiarVelocidad() {
        velocidadNivel = if (velocidadNivel >= 3) 1 else velocidadNivel + 1
        enviarComando(velocidadNivel.toString())
        actualizarUI()
    }

    private fun iniciarEscaneoMuros(boton: View) {
        detenerMovimientoManual(); detenerLocalizacion()
        modoManual = true; modoAuto = false
        botonMovimientoActivo = boton
        escaneoConstruyeMapa = true; grupoMuroActual++
        enviarComando("M")
    }

    private fun alternarLocalizacion(boton: View) {
        detenerMovimientoManual()
        if (localizacionActiva) {
            detenerLocalizacion(); botonMovimientoActivo = null
            enviarComando("S"); actualizarEstadosBotones(); return
        }
        if (muroPuntos.size < MIN_MUROS_PARA_LOCALIZAR) {
            Toast.makeText(this, "Mide algunas paredes primero", Toast.LENGTH_SHORT).show()
            return
        }
        localizacionActiva = true; modoManual = true; modoAuto = false
        botonMovimientoActivo = boton; enviarComando("U")
        localizacionHandler.postDelayed(localizacionRunnable, LOCALIZACION_INTERVALO_MS)
        actualizarEstadosBotones()
    }

    private fun detenerLocalizacion() {
        localizacionActiva = false
        localizacionHandler.removeCallbacks(localizacionRunnable)
    }

    private fun procesarLineaBluetooth(linea: String) {
        try {
            val json = JSONObject(linea)
            if (json.has("scan")) {
                val scan = json.getJSONObject("scan")
                procesarLecturaServo(scan.optInt("ang", 90), scan.optDouble("dist", 0.0), escaneoConstruyeMapa || !localizacionActiva)
                return
            }
            tiempoSegundos = json.optInt("t", tiempoSegundos)
            distanciaTotal = json.optDouble("d", distanciaTotal)
            bateria = json.optInt("b", bateria)
            obstaculosPequenos = json.optInt("sp", obstaculosPequenos)
            obstaculosGrandes = json.optInt("lg", obstaculosGrandes)
            actualizarUI()
        } catch (_: Exception) {}
    }

    private fun procesarLecturaServo(anguloServo: Int, distanciaCm: Double, construirMapa: Boolean) {
        if (distanciaCm < 3.0 || distanciaCm > 300.0) return
        if (!construirMapa && intentarCorregirUbicacion(anguloServo, distanciaCm)) {
            actualizarUI(); return
        }
        registrarMuroDesdeEscaneo(anguloServo, distanciaCm)
    }

    private fun registrarMuroDesdeEscaneo(anguloServo: Int, distanciaCm: Double) {
        val distM = (distanciaCm / 100.0) + 0.065
        val rad = Math.toRadians(headingGrados + (90 - anguloServo))
        val x = posX + sin(rad) * distM
        val y = posY + cos(rad) * distM
        muroPuntos.add(MuroPunto(x, y, grupoMuroActual))
        actualizarLimitesDesdePunto(x, y)
        mapaPreview.setMuroPuntos(muroPuntos); actualizarUI()
    }

    private fun intentarCorregirUbicacion(anguloServo: Int, distanciaCm: Double): Boolean {
        if (muroPuntos.size < MIN_MUROS_PARA_LOCALIZAR) return false
        val distM = (distanciaCm / 100.0) + 0.065
        val rad = Math.toRadians(headingGrados + (90 - anguloServo))
        val medX = posX + sin(rad) * distM
        val medY = posY + cos(rad) * distM
        val pared = muroPuntos.minByOrNull { sqrt((it.x - medX)*(it.x - medX) + (it.y - medY)*(it.y - medY)) } ?: return false
        val errorX = pared.x - medX; val errorY = pared.y - medY
        if (sqrt(errorX * errorX + errorY * errorY) > 0.45) return false
        posX += errorX * 0.35; posY += errorY * 0.35
        actualizarLimitesDesdePunto(posX, posY)
        
        if (modoAuto && mapeoActivo) {
            val ultimo = parsearRutaTexto(if(rutaManual.isNotEmpty()) rutaManual.last() else "")
            val d = if(ultimo != null) sqrt((posX-ultimo.x)*(posX-ultimo.x) + (posY-ultimo.y)*(posY-ultimo.y)) else 1.0
            if (d > 0.25) {
                rutaManual.add("${ordenRuta++},$posX,$posY,auto")
                mapaPreview.setRutaDesdeTexto(rutaManual, puertasDetectadas)
            }
        }
        return true
    }

    private fun parsearRutaTexto(txt: String): RutaPunto? {
        val p = txt.split(","); if(p.size < 4) return null
        return RutaPunto(p[0].toIntOrNull()?:0, p[1].toDoubleOrNull()?:0.0, p[2].toDoubleOrNull()?:0.0, p[3])
    }

    private fun configurarBotonMovimiento(boton: ImageButton, comando: String) {
        boton.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { v.isPressed = true; iniciandoTramo = true; iniciarMovimientoManual(v, comando); true }
                MotionEvent.ACTION_UP -> { v.isPressed = false; v.performClick(); detenerMovimientoManual(); true }
                MotionEvent.ACTION_CANCEL -> { v.isPressed = false; detenerMovimientoManual(); true }
                else -> true
            }
        }
    }

    private fun iniciarMovimientoManual(boton: View, comando: String) {
        if (comandoMovimientoActivo == comando) return
        detenerMovimientoManual(false); botonMovimientoActivo = boton; comandoMovimientoActivo = comando
        ultimoTickMovimiento = System.currentTimeMillis(); modoManual = true
        enviarComandoManual(comando); actualizarEstadosBotones(); movimientoHandler.post(movimientoRunnable)
    }

    private fun detenerMovimientoManual(enviarStop: Boolean = true) {
        if (comandoMovimientoActivo == null) return
        actualizarMovimientoManual(comandoMovimientoActivo!!)
        comandoMovimientoActivo = null; movimientoHandler.removeCallbacks(movimientoRunnable)
        botonMovimientoActivo = null; if (enviarStop) enviarComando("S")
        actualizarEstadosBotones()
    }

    private fun actualizarMovimientoManual(comando: String) {
        val dt = ((System.currentTimeMillis() - ultimoTickMovimiento).coerceAtLeast(0)) / 1000.0
        ultimoTickMovimiento = System.currentTimeMillis()
        if (dt <= 0.0 || !mapeoActivo || !modoManual) return
        when (comando) {
            "F" -> registrarPasoManual(distanciaPorSegundoActual() * dt)
            "B" -> registrarPasoManual(-distanciaPorSegundoActual() * dt)
            "L" -> { headingGrados = (headingGrados - 180 * dt).mod(360.0) }
            "R" -> { headingGrados = (headingGrados + 180 * dt).mod(360.0) }
        }
    }

    private fun registrarPasoManual(distancia: Double) {
        if (!mapeoActivo || !modoManual) return
        distanciaTotal += Math.abs(distancia)
        posX += sin(Math.toRadians(headingGrados)) * distancia
        posY += cos(Math.toRadians(headingGrados)) * distancia
        actualizarLimitesDesdePunto(posX, posY)
        if (iniciandoTramo || rutaManual.isEmpty()) {
            rutaManual.add("${ordenRuta++},$posX,$posY,manual"); iniciandoTramo = false
        } else {
            val last = rutaManual.size - 1
            val id = rutaManual[last].split(",")[0]
            rutaManual[last] = "$id,$posX,$posY,manual"
        }
        mapaPreview.setRutaDesdeTexto(rutaManual, puertasDetectadas); actualizarUI()
    }

    private fun actualizarLimitesDesdePunto(x: Double, y: Double) {
        norte = max(norte, y.coerceAtLeast(0.0)); sur = max(sur, (-y).coerceAtLeast(0.0))
        este = max(este, x.coerceAtLeast(0.0)); oeste = max(oeste, (-x).coerceAtLeast(0.0))
    }

    private fun reiniciarMapa() {
        oeste = 0.0; norte = 0.0; sur = 0.0; este = 0.0; distanciaTotal = 0.0
        headingGrados = 0.0; posX = 0.0; posY = 0.0; ordenRuta = 0
        rutaManual.clear(); rutaManual.add("${ordenRuta++},0.0,0.0,inicio")
        muroPuntos.clear(); mapaPreview.setRutaDesdeTexto(rutaManual, 0); mapaPreview.setMuroPuntos(muroPuntos); actualizarUI()
    }

    private fun volverAlPuntoOrigen(boton: View) {
        enviarComando("O"); modoAuto = false; mapeoActivo = false
        botonMovimientoActivo = boton; actualizarEstadosBotones()
    }

    private fun actualizarUI() {
        tvOeste.text = "Oeste\n${"%.1f".format(oeste)} M"; tvNorte.text = "Norte\n${"%.1f".format(norte)} M"
        tvSur.text = "Sur\n${"%.1f".format(sur)} M"; tvEste.text = "Este\n${"%.1f".format(este)} M"
        tvDistancia.text = "Distancia\n${"%.1f".format(distanciaTotal)} M"; tvVelocidadNumero.text = velocidadNivel.toString()
        mapaPreview.setMedidasMuros(oeste, norte, sur, este); mapaPreview.setMuroPuntos(muroPuntos)
    }

    private fun pausarMapeo(boton: View) {
        mapeoActivo = false; modoAuto = false; detenerLocalizacion()
        comandoMovimientoActivo = null; movimientoHandler.removeCallbacks(movimientoRunnable)
        botonMovimientoActivo = boton; actualizarEstadosBotones()
    }

    private fun enviarComandoManual(cmd: String) {
        if (modoAuto) { enviarComando("S"); modoAuto = false }
        enviarComando(cmd)
    }

    private fun salirModoAutoSiNecesario() { if (modoAuto) { enviarComando("S"); modoAuto = false; detenerLocalizacion() } }

    private fun actualizarEstadosBotones() {
        botonesActivos.forEach { pintarBoton(it, false) }
        if (mapeoActivo) pintarBoton(findViewById(R.id.btnIniciarSesion), true)
        botonMovimientoActivo?.let { pintarBoton(it, true) }
    }

    private fun pintarBoton(v: View, s: Boolean) {
        val f = if (s) Color.rgb(0, 229, 255) else Color.rgb(14, 60, 110)
        val i = if (s) Color.rgb(10, 42, 74) else Color.rgb(0, 229, 255)
        v.backgroundTintList = ColorStateList.valueOf(f)
        if (v is ImageButton) v.imageTintList = ColorStateList.valueOf(i)
        if (v is LinearLayout) {
            for (idx in 0 until v.childCount) {
                val h = v.getChildAt(idx)
                if (h is ImageView) h.imageTintList = ColorStateList.valueOf(i)
                if (h is TextView) h.setTextColor(if(s) Color.rgb(10, 42, 74) else Color.WHITE)
            }
        }
    }

    private fun mostrarAdvertenciaSalida() {
        AlertDialog.Builder(this).setTitle("Salir").setMessage("¿Deseas salir?").setPositiveButton("Sí") { _, _ -> finish() }.setNegativeButton("No", null).show()
    }

    private fun distanciaPorSegundoActual(): Double = when (velocidadNivel) { 1 -> 0.12; 2 -> 0.18; else -> 0.24 }

    override fun onPause() { detenerMovimientoManual(); detenerLocalizacion(); super.onPause() }

    override fun onDestroy() {
        BluetoothManager.onLineaRecibida = null
        BluetoothManager.onDesconectado = null
        super.onDestroy()
    }

    companion object {
        private const val MOVIMIENTO_TICK_MS = 100L
        private const val LOCALIZACION_INTERVALO_MS = 1200L
        private const val MIN_MUROS_PARA_LOCALIZAR = 8
    }
}
