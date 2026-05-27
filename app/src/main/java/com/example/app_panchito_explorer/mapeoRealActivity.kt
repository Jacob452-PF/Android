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

    // Medidas acumuladas en CENTÍMETROS
    private var oeste = 0.0
    private var norte = 0.0
    private var sur = 0.0
    private var este = 0.0

    // Estadísticas generales
    private var distanciaTotal = 0.0
    private var tiempoSegundos = 0
    private var bateria = 0
    private var obstaculosPequenos = 0
    private var obstaculosGrandes = 0
    private var puertasDetectadas = 0
    private var velocidadNivel = 1

    // Estados de control
    private var mapeoActivo = false
    private var modoManual = true
    private var modoAuto = false
    private var modoDetectar = false
    private var escaneoConstruyeMapa = false
    private var headingGrados = 0.0
    private var posX = 0.0
    private var posY = 0.0
    private var ordenRuta = 0
    private var grupoMuroActual = 0
    private var iniciandoTramo = true 
    private val rutaManual = arrayListOf<String>()
    private val muroPuntos = arrayListOf<MuroPunto>()
    private val obstaculoPuntos = arrayListOf<ObstaculoPunto>()

    // Componentes de la interfaz
    private lateinit var tvOeste: TextView
    private lateinit var tvNorte: TextView
    private lateinit var tvSur: TextView
    private lateinit var tvEste: TextView
    private lateinit var tvTiempo: TextView
    private lateinit var tvDistancia: TextView
    private lateinit var tvBateria: TextView
    private lateinit var tvPequenos: TextView
    private lateinit var tvGrandes: TextView
    private lateinit var tvVelocidadNumero: TextView
    private lateinit var mapaPreview: MapaPreviewView
    private val botonesActivos = arrayListOf<View>()
    private var botonMovimientoActivo: View? = null
    private val movimientoHandler = Handler(Looper.getMainLooper())
    private val tiempoHandler = Handler(Looper.getMainLooper())
    private val autoMovimientoHandler = Handler(Looper.getMainLooper())
    private var comandoMovimientoActivo: String? = null
    private var comandoAutoActivo: String? = null
    private var ultimoTickMovimiento = 0L
    private var ultimoTickAuto = 0L
    private var tiempoInicioMs = 0L
    private var tiempoAcumuladoMs = 0L
    private var cronometroActivo = false
    private var movimientoBloqueadoPorMuro = false

    private val tiempoRunnable = object : Runnable {
        override fun run() {
            actualizarTiempoMapeo()
            tiempoHandler.postDelayed(this, TIEMPO_TICK_MS)
        }
    }

    private val movimientoRunnable = object : Runnable {
        override fun run() {
            val comando = comandoMovimientoActivo ?: return
            actualizarMovimientoManual(comando)
            movimientoHandler.postDelayed(this, MOVIMIENTO_TICK_MS)
        }
    }

    private val autoMovimientoRunnable = object : Runnable {
        override fun run() {
            val comando = comandoAutoActivo ?: return
            actualizarMovimientoAuto(comando)
            autoMovimientoHandler.postDelayed(this, MOVIMIENTO_TICK_MS)
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
        tvTiempo = findViewById(R.id.tvTiempo)
        tvDistancia = findViewById(R.id.tvDistancia)
        tvBateria = findViewById(R.id.tvBateria)
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
        val btnDetectarModo = findViewById<LinearLayout>(R.id.btnDetectarModo)
        val panelVelocidad = findViewById<LinearLayout>(R.id.panelVelocidad)

        botonesActivos.addAll(listOf(btnUp, btnDown, btnLeft, btnRight, btnStop, btnIniciarSesion, btnMedirModo, btnAutoModo, btnDetectarModo))

        BluetoothManager.onDesconectado = {
            if (mapeoActivo) {
                runOnUiThread {
                    if (!isFinishing) {
                        detenerMovimientoManual(enviarStop = false)
                        detenerMovimientoAuto()
                        mapeoActivo = false
                        modoAuto = false
                        modoDetectar = false
                        findViewById<View>(R.id.layoutErrorConexion).visibility = View.VISIBLE
                        actualizarEstadosBotones()
                    }
                }
            }
        }

        findViewById<Button>(R.id.btnEntendidoError).setOnClickListener { finish() }

        btnIniciarSesion.setOnClickListener {
            salirModosAutomaticos()
            mapeoActivo = !mapeoActivo
            if (mapeoActivo && rutaManual.isEmpty()) {
                reiniciarMapa()
                enviarComando("I")
                enviarComando(velocidadNivel.toString())
            }
            actualizarEstadosBotones()
        }

        btnMedirModo.setOnClickListener {
            salirModosAutomaticos()
            modoManual = true; escaneoConstruyeMapa = true; grupoMuroActual++
            botonMovimientoActivo = it
            enviarComando("M")
            actualizarEstadosBotones()
        }

        btnAutoModo.setOnClickListener {
            detenerMovimientoManual()
            if (modoAuto) {
                detenerMovimientoAuto()
                modoAuto = false; enviarComando("S"); botonMovimientoActivo = btnStop
            } else {
                modoAuto = true; modoManual = false; modoDetectar = false
                enviarComando("A"); botonMovimientoActivo = it
                iniciarMovimientoAuto("F")
            }
            actualizarEstadosBotones()
        }

        btnDetectarModo.setOnClickListener {
            detenerMovimientoManual()
            if (modoDetectar) {
                modoDetectar = false; enviarComando("S"); botonMovimientoActivo = btnStop
            } else {
                if (muroPuntos.isEmpty()) {
                    Toast.makeText(this, "Primero mide muros para definir el área", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                modoDetectar = true; modoManual = false; modoAuto = false; escaneoConstruyeMapa = false
                enviarComando("M"); botonMovimientoActivo = it
            }
            actualizarEstadosBotones()
        }

        panelVelocidad.setOnClickListener { cambiarVelocidad() }

        configurarBotonMovimiento(btnUp, "F")
        configurarBotonMovimiento(btnDown, "B")
        configurarBotonMovimiento(btnLeft, "L")
        configurarBotonMovimiento(btnRight, "R")
        
        btnStop.setOnClickListener { detenerMovimientoManual(); detenerMovimientoAuto(); pausarMapeo(it); enviarComando("S") }

        btnGuardar.setOnClickListener {
            actualizarTiempoMapeo()
            val intent = Intent(this, guardarMapaActivity::class.java)
            // Convertimos cm de vuelta a metros solo para el guardado si es necesario, 
            // o lo guardamos como cm. Asumiremos que el resto de la app espera metros.
            intent.putExtra("distancia_total", distanciaTotal / 100.0)
            intent.putExtra("tiempo_segundos", tiempoSegundos)
            intent.putExtra("bateria", bateria)
            intent.putExtra("oeste", oeste / 100.0); intent.putExtra("norte", norte / 100.0)
            intent.putExtra("sur", sur / 100.0); intent.putExtra("este", este / 100.0)
            intent.putExtra("pequenos", obstaculosPequenos); intent.putExtra("grandes", obstaculosGrandes)
            intent.putExtra("puertas", puertasDetectadas)
            intent.putStringArrayListExtra("ruta_manual", ArrayList(rutaManual.map { 
                val p = parsearRutaTexto(it)
                if (p != null) "${p.orden},${p.x/100.0},${p.y/100.0},${p.modo}" else it
            }))
            intent.putStringArrayListExtra("muro_puntos", ArrayList(muroPuntos.map { "${it.x / 100.0},${it.y / 100.0},${it.grupo}" }))
            startActivity(intent)
        }

        btnBack.setOnClickListener { mostrarAdvertenciaSalida() }
        onBackPressedDispatcher.addCallback(this) { mostrarAdvertenciaSalida() }

        BluetoothManager.onLineaRecibida = { linea -> runOnUiThread { procesarLineaBluetooth(linea) } }

        iniciarCronometro()
        actualizarEstadosBotones(); actualizarUI()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    private fun enviarComando(comando: String) { BluetoothManager.enviarDato(comando) }

    private fun cambiarVelocidad() {
        velocidadNivel = if (velocidadNivel >= 3) 1 else velocidadNivel + 1
        enviarComando("V$velocidadNivel")
        enviarComando(velocidadNivel.toString())
        actualizarUI()
    }

    private fun procesarLineaBluetooth(linea: String) {
        if (procesarEstadoSimple(linea)) return

        try {
            val json = JSONObject(linea)
            if (json.has("scan")) {
                val scan = json.getJSONObject("scan")
                procesarLecturaServo(scan.optInt("ang", 90), scan.optDouble("dist", 0.0))
                return
            }
            if (json.has("event")) {
                procesarEventoRobot(json.optString("event"))
            }
            if (json.has("st")) {
                procesarEventoRobot(json.optString("st"))
            }
            if (json.has("moving") && !json.optBoolean("moving", true)) {
                detenerMovimientoPorRobot()
            }
            if (json.has("move")) {
                procesarMovimientoRobot(json.optString("move"), json.optBoolean("moving", true))
            }
            if (json.has("t")) {
                tiempoSegundos = json.optInt("t", tiempoSegundos)
                tiempoAcumuladoMs = tiempoSegundos * 1000L
                if (cronometroActivo) tiempoInicioMs = System.currentTimeMillis()
            }
            if (json.has("d")) {
                distanciaTotal = json.optDouble("d", distanciaTotal / 100.0) * 100.0 // Si el robot manda metros, convertir a cm
            }
            bateria = json.optInt("b", bateria)
            BluetoothManager.bateria = bateria
            if (!modoDetectar) {
                obstaculosPequenos = json.optInt("sp", obstaculosPequenos)
                obstaculosGrandes = json.optInt("lg", obstaculosGrandes)
            }
            actualizarUI()
        } catch (_: Exception) {}
    }

    private fun procesarEstadoSimple(linea: String): Boolean {
        val texto = linea.trim()
        val upper = texto.uppercase()

        if (upper == "STOP" || upper == "S" || upper == "OBSTACLE" || upper == "WALL") {
            detenerMovimientoPorRobot()
            return true
        }

        val match = Regex("""^(BAT|BATERIA|B)[:=]\s*(\d{1,3})%?$""", RegexOption.IGNORE_CASE).find(texto)
        if (match != null) {
            bateria = match.groupValues[2].toInt().coerceIn(0, 100)
            BluetoothManager.bateria = bateria
            actualizarUI()
            return true
        }

        return false
    }

    private fun procesarEventoRobot(evento: String) {
        when (evento.trim().uppercase()) {
            "STOP", "STOPPED", "OBSTACLE", "WALL", "BLOCKED" -> detenerMovimientoPorRobot()
            "MOVING", "RUNNING" -> {
                movimientoBloqueadoPorMuro = false
                if (modoAuto) iniciarMovimientoAuto(comandoAutoActivo ?: "F")
            }
        }
    }

    private fun procesarMovimientoRobot(movimiento: String, moving: Boolean) {
        if (!modoAuto) return
        if (!moving) {
            detenerMovimientoAuto()
            return
        }

        when (movimiento.trim().uppercase()) {
            "F", "FRONT", "FORWARD" -> iniciarMovimientoAuto("F")
            "B", "BACK" -> iniciarMovimientoAuto("B")
            "L", "LEFT" -> iniciarMovimientoAuto("L")
            "R", "RIGHT" -> iniciarMovimientoAuto("R")
            "S", "STOP" -> detenerMovimientoAuto()
        }
    }

    private fun detenerMovimientoPorRobot() {
        if (comandoMovimientoActivo != null) {
            actualizarMovimientoManual(comandoMovimientoActivo!!)
        }
        comandoMovimientoActivo = null
        detenerMovimientoAuto()
        movimientoBloqueadoPorMuro = true
        movimientoHandler.removeCallbacks(movimientoRunnable)
        botonMovimientoActivo = null
        actualizarEstadosBotones()
        actualizarUI()
    }

    private fun procesarLecturaServo(anguloServo: Int, distanciaCm: Double) {
        if (distanciaCm < DISTANCIA_MIN_MURO_CM || distanciaCm > DISTANCIA_MAX_MURO_CM) return
        
        // distM se convierte en distCm
        val distCmTotal = distanciaCm + ROBOT_MEDIO_LARGO_CM
        val rad = Math.toRadians(headingGrados + (90 - anguloServo))
        val x = posX + sin(rad) * distCmTotal
        val y = posY + cos(rad) * distCmTotal

        if (escaneoConstruyeMapa) {
            muroPuntos.add(MuroPunto(x, y, grupoMuroActual))
            actualizarLimitesDesdePunto(x, y)
            mapaPreview.setMuroPuntos(muroPuntos)
        } else if (modoDetectar) {
            if (x > -oeste && x < este && y > -sur && y < norte) {
                if (distanciaCm < 18) obstaculosPequenos++ else obstaculosGrandes++
                obstaculoPuntos.add(ObstaculoPunto(x, y))
                mapaPreview.setObstaculos(obstaculoPuntos)
            }
        }
        actualizarUI()
    }

    private fun parsearRutaTexto(txt: String): RutaPunto? {
        val p = txt.split(","); if(p.size < 4) return null
        return RutaPunto(p[0].toIntOrNull()?:0, p[1].toDoubleOrNull()?:0.0, p[2].toDoubleOrNull()?:0.0, p[3])
    }

    private fun configurarBotonMovimiento(boton: ImageButton, comando: String) {
        boton.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { v.isPressed = true; iniciandoTramo = true; iniciarMovimientoManual(v, comando); true }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { 
                    v.isPressed = false
                    if (e.actionMasked == MotionEvent.ACTION_UP) v.performClick()
                    detenerMovimientoManual()
                    true 
                }
                else -> true
            }
        }
    }

    private fun iniciarMovimientoManual(boton: View, comando: String) {
        if (comandoMovimientoActivo == comando) return
        movimientoBloqueadoPorMuro = false
        detenerMovimientoManual(false); botonMovimientoActivo = boton; comandoMovimientoActivo = comando
        ultimoTickMovimiento = System.currentTimeMillis(); modoManual = true
        enviarComando(comando); actualizarEstadosBotones(); movimientoHandler.post(movimientoRunnable)
    }

    private fun detenerMovimientoManual(enviarStop: Boolean = true) {
        if (comandoMovimientoActivo == null) return
        actualizarMovimientoManual(comandoMovimientoActivo!!)
        comandoMovimientoActivo = null; movimientoHandler.removeCallbacks(movimientoRunnable)
        botonMovimientoActivo = null; if (enviarStop) enviarComando("S")
        actualizarEstadosBotones()
    }

    private fun iniciarMovimientoAuto(comando: String) {
        if (!mapeoActivo || !modoAuto || movimientoBloqueadoPorMuro) return
        if (comandoAutoActivo == comando) return
        val comandoAnterior = comandoAutoActivo
        detenerMovimientoAuto()
        if (comandoAnterior != null && comandoAnterior != comando) {
            iniciandoTramo = true
        }
        comandoAutoActivo = comando
        ultimoTickAuto = System.currentTimeMillis()
        autoMovimientoHandler.post(autoMovimientoRunnable)
    }

    private fun detenerMovimientoAuto() {
        val comando = comandoAutoActivo
        if (comando != null) {
            actualizarMovimientoAuto(comando)
        }
        comandoAutoActivo = null
        autoMovimientoHandler.removeCallbacks(autoMovimientoRunnable)
    }

    private fun actualizarMovimientoManual(comando: String) {
        val dt = ((System.currentTimeMillis() - ultimoTickMovimiento).coerceAtLeast(0)) / 1000.0
        ultimoTickMovimiento = System.currentTimeMillis()
        if (dt <= 0.0 || !mapeoActivo || !modoManual || movimientoBloqueadoPorMuro) return
        when (comando) {
            "F" -> registrarPasoManual(distanciaPorSegundoActual() * dt)
            "B" -> registrarPasoManual(-distanciaPorSegundoActual() * dt)
            "L" -> { headingGrados = (headingGrados - 180 * dt).mod(360.0) }
            "R" -> { headingGrados = (headingGrados + 180 * dt).mod(360.0) }
        }
    }

    private fun actualizarMovimientoAuto(comando: String) {
        val dt = ((System.currentTimeMillis() - ultimoTickAuto).coerceAtLeast(0)) / 1000.0
        ultimoTickAuto = System.currentTimeMillis()
        if (dt <= 0.0 || !mapeoActivo || !modoAuto || movimientoBloqueadoPorMuro) return

        when (comando) {
            "F" -> registrarPaso("auto", distanciaPorSegundoActual() * dt, actualizarMedidas = true)
            "B" -> registrarPaso("auto", -distanciaPorSegundoActual() * dt, actualizarMedidas = true)
            "L" -> headingGrados = (headingGrados - 180 * dt).mod(360.0)
            "R" -> headingGrados = (headingGrados + 180 * dt).mod(360.0)
        }
    }

    private fun registrarPasoManual(distancia: Double) {
        if (!mapeoActivo || !modoManual) return
        registrarPaso("manual", distancia, escaneoConstruyeMapa)
    }

    private fun registrarPaso(modo: String, distancia: Double, actualizarMedidas: Boolean) {
        distanciaTotal += Math.abs(distancia)
        posX += sin(Math.toRadians(headingGrados)) * distancia
        posY += cos(Math.toRadians(headingGrados)) * distancia
        if (actualizarMedidas) actualizarLimitesDesdePunto(posX, posY)
        if (iniciandoTramo || rutaManual.isEmpty()) {
            rutaManual.add("${ordenRuta++},$posX,$posY,$modo"); iniciandoTramo = false
        } else {
            val last = rutaManual.size - 1
            val id = rutaManual[last].split(",")[0]
            rutaManual[last] = "$id,$posX,$posY,$modo"
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
        iniciandoTramo = true
        rutaManual.clear(); rutaManual.add("${ordenRuta++},0.0,0.0,inicio")
        muroPuntos.clear(); obstaculoPuntos.clear()
        mapaPreview.setRutaDesdeTexto(rutaManual, 0); mapaPreview.setMuroPuntos(muroPuntos)
        mapaPreview.setObstaculos(obstaculoPuntos); actualizarUI()
    }

    private fun iniciarCronometro() {
        if (cronometroActivo) return
        cronometroActivo = true
        tiempoInicioMs = System.currentTimeMillis()
        tiempoHandler.post(tiempoRunnable)
    }

    private fun pausarCronometro() {
        if (!cronometroActivo) return
        actualizarTiempoMapeo()
        tiempoAcumuladoMs = tiempoSegundos * 1000L
        cronometroActivo = false
        tiempoHandler.removeCallbacks(tiempoRunnable)
    }

    private fun actualizarTiempoMapeo() {
        if (cronometroActivo) {
            val transcurrido = System.currentTimeMillis() - tiempoInicioMs
            tiempoSegundos = ((tiempoAcumuladoMs + transcurrido) / 1000L).toInt()
        }
        actualizarUI()
    }

    private fun actualizarUI() {
        // oeste, norte, sur, este están en cm
        val minutos = tiempoSegundos / 60
        val segundos = tiempoSegundos % 60
        tvTiempo.text = "Tiempo\n%02d:%02d".format(minutos, segundos)
        tvBateria.text = if (bateria > 0) "Bateria\n$bateria%" else "Bateria\n--"
        tvOeste.text = "Oeste\n${oeste.toInt()} cm"; tvNorte.text = "Norte\n${norte.toInt()} cm"
        tvSur.text = "Sur\n${sur.toInt()} cm"; tvEste.text = "Este\n${este.toInt()} cm"
        tvDistancia.text = "Distancia\n${distanciaTotal.toInt()} cm"; tvVelocidadNumero.text = velocidadNivel.toString()
        tvPequenos.text = "Pequeños\n$obstaculosPequenos"; tvGrandes.text = "Grandes\n$obstaculosGrandes"
        mapaPreview.setMedidasMuros(oeste, norte, sur, este)
    }

    private fun pausarMapeo(boton: View) {
        detenerMovimientoAuto()
        mapeoActivo = false; modoAuto = false; modoDetectar = false
        comandoMovimientoActivo = null; movimientoHandler.removeCallbacks(movimientoRunnable)
        botonMovimientoActivo = boton; actualizarEstadosBotones()
    }

    private fun salirModosAutomaticos() {
        if (modoAuto || modoDetectar) {
            enviarComando("S")
            detenerMovimientoAuto()
            modoAuto = false
            modoDetectar = false
        }
    }

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

    // Retorna CENTÍMETROS por segundo. Calibrado: 1.80m / 5s = 36cm/s en L3
    private fun distanciaPorSegundoActual(): Double = when (velocidadNivel) { 1 -> 12.0; 2 -> 24.0; else -> 36.0 }

    override fun onResume() {
        super.onResume()
        if (::tvTiempo.isInitialized) iniciarCronometro()
    }

    override fun onPause() {
        detenerMovimientoManual()
        detenerMovimientoAuto()
        pausarCronometro()
        super.onPause()
    }

    override fun onDestroy() {
        tiempoHandler.removeCallbacks(tiempoRunnable)
        autoMovimientoHandler.removeCallbacks(autoMovimientoRunnable)
        BluetoothManager.onLineaRecibida = null; BluetoothManager.onDesconectado = null
        super.onDestroy()
    }

    companion object {
        private const val MOVIMIENTO_TICK_MS = 100L
        private const val TIEMPO_TICK_MS = 1000L
        private const val MIN_MUROS_PARA_LOCALIZAR = 8
        private const val DISTANCIA_MIN_MURO_CM = 3.0
        private const val DISTANCIA_MAX_MURO_CM = 300.0
        private const val ROBOT_MEDIO_LARGO_CM = 8.5 // Calibrado: 17cm total
        private const val ERROR_LOCALIZACION_MAX_CM = 45.0
        private const val CORRECCION_LOCALIZACION = 0.35
    }
}
