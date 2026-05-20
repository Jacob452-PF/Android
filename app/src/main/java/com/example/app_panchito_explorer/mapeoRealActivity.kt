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

    // Medidas acumuladas del area detectada por el robot.
    private var oeste = 0.0
    private var norte = 0.0
    private var sur = 0.0
    private var este = 0.0

    // Estadisticas generales del recorrido actual.
    private var distanciaTotal = 0.0
    private var tiempoSegundos = 0
    private var bateria = 0
    private var obstaculosPequenos = 0
    private var obstaculosGrandes = 0
    private var puertasDetectadas = 0
    private var velocidadNivel = 1

    // Estados de control del mapeo, modos de manejo y posicion estimada.
    private var mapeoActivo = false
    private var modoManual = true
    private var modoAuto = false
    private var localizacionActiva = false
    private var escaneoConstruyeMapa = false
    private var mostrarMedidasMuros = true
    private var heading = 0
    private var headingGrados = 0.0
    private var posX = 0.0
    private var posY = 0.0
    private var ordenRuta = 0
    private var grupoMuroActual = 0
    private val rutaManual = arrayListOf<String>()
    private val muroPuntos = arrayListOf<MuroPunto>()

    // Componentes de la interfaz que se actualizan constantemente.
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

    // Solicita ubicacion al robot de forma periodica mientras el modo esta activo.
    private val localizacionRunnable = object : Runnable {
        override fun run() {
            if (!localizacionActiva) {
                return
            }

            enviarComando("U")
            localizacionHandler.postDelayed(this, LOCALIZACION_INTERVALO_MS)
        }
    }

    // Calcula movimiento manual continuo mientras el usuario mantiene presionado un boton.
    private val movimientoRunnable = object : Runnable {
        override fun run() {
            val comando = comandoMovimientoActivo ?: return
            actualizarMovimientoManual(comando)
            movimientoHandler.postDelayed(this, MOVIMIENTO_TICK_MS)
        }
    }

    // =========================
    // ON CREATE
    // =========================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mapeo_real)

        // Botones y paneles de control del robot.
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarMapa)
        val btnUp = findViewById<ImageButton>(R.id.btnUp)
        val btnDown = findViewById<ImageButton>(R.id.btnDown)
        val btnLeft = findViewById<ImageButton>(R.id.btnLeft)
        val btnRight = findViewById<ImageButton>(R.id.btnRight)
        val btnStop = findViewById<ImageButton>(R.id.btnStop)
        val btnServoLeft = findViewById<ImageButton>(R.id.btnServoLeft)
        val btnServoCenter = findViewById<ImageButton>(R.id.btnServoCenter)
        val btnServoRight = findViewById<ImageButton>(R.id.btnServoRight)
        val btnIniciarSesion = findViewById<LinearLayout>(R.id.btnIniciarSesion)
        val btnMedirModo = findViewById<LinearLayout>(R.id.btnMedirModo)
        val btnAutoModo = findViewById<LinearLayout>(R.id.btnAutoModo)
        val btnUbicarModo = findViewById<LinearLayout>(R.id.btnUbicarModo)
        val btnVolverOrigen = findViewById<LinearLayout>(R.id.btnVolverOrigen)
        val panelVelocidad = findViewById<LinearLayout>(R.id.panelVelocidad)

        // Textos de medicion y vista previa del mapa.
        tvOeste = findViewById(R.id.tvOeste)
        tvNorte = findViewById(R.id.tvNorte)
        tvSur = findViewById(R.id.tvSur)
        tvEste = findViewById(R.id.tvEste)
        tvDistancia = findViewById(R.id.tvDistancia)
        tvPequenos = findViewById(R.id.tvPequenos)
        tvGrandes = findViewById(R.id.tvGrandes)
        tvVelocidadNumero = findViewById(R.id.tvVelocidadNumero)
        mapaPreview = findViewById(R.id.mapaPreview)
        botonesActivos.addAll(
            listOf(
                btnUp,
                btnDown,
                btnLeft,
                btnRight,
                btnStop,
                btnServoLeft,
                btnServoCenter,
                btnServoRight,
                btnIniciarSesion,
                btnMedirModo,
                btnAutoModo,
                btnUbicarModo,
                btnVolverOrigen
            )
        )

        // Estado inicial de botones y textos.
        actualizarEstadosBotones()
        actualizarUI()

        // Inicia o pausa la sesion de mapeo manual.
        btnIniciarSesion.setOnClickListener {
            salirModoAutoSiNecesario()
            mapeoActivo = !mapeoActivo
            marcarBotonActivo(btnIniciarSesion)
            modoManual = true
            modoAuto = false

            if (mapeoActivo && rutaManual.isEmpty()) {
                reiniciarMapa()
                enviarComando("I")
                enviarComando(velocidadNivel.toString())
            }

            actualizarEstadosBotones()
        }

        // Solicita al robot un escaneo de muros.
        btnMedirModo.setOnClickListener {
            salirModoAutoSiNecesario()
            iniciarEscaneoMuros(btnMedirModo)
            actualizarEstadosBotones()
        }

        // Alterna el modo automatico del robot.
        btnAutoModo.setOnClickListener {
            detenerMovimientoManual()

            if (modoAuto) {
                modoAuto = false
                botonMovimientoActivo = btnStop
                enviarComando("S")
                actualizarEstadosBotones()
                return@setOnClickListener
            }

            modoAuto = true
            modoManual = false
            enviarComando("A")
            botonMovimientoActivo = btnAutoModo
            actualizarEstadosBotones()
        }

        // Alterna el modo de localizacion sobre el mapa ya medido.
        btnUbicarModo.setOnClickListener {
            alternarLocalizacion(btnUbicarModo)
        }

        // Cambia el nivel de velocidad enviado al robot.
        panelVelocidad.setOnClickListener {
            cambiarVelocidad()
        }

        // Configura controles de movimiento y servo.
        configurarBotonMovimiento(btnUp, "F")
        configurarBotonMovimiento(btnDown, "B")
        configurarBotonMovimiento(btnLeft, "L")
        configurarBotonMovimiento(btnRight, "R")
        configurarBotonServo(btnServoLeft, "Q")
        configurarBotonServo(btnServoCenter, "W")
        configurarBotonServo(btnServoRight, "E")

        // Detiene el movimiento actual y pausa el mapeo.
        btnStop.setOnClickListener {
            detenerMovimientoManual()
            pausarMapeo(btnStop)
            enviarComando("S")
        }

        // Ordena volver al punto registrado mas cercano al origen.
        btnVolverOrigen.setOnClickListener {
            volverAlPuntoOrigen(btnVolverOrigen)
        }

        // Envia los datos recolectados a la pantalla de guardado.
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
            intent.putStringArrayListExtra(
                "muro_puntos",
                ArrayList(muroPuntos.map { punto -> "${punto.x},${punto.y},${punto.grupo}" })
            )
            startActivity(intent)
        }

        // Protege la salida para evitar perder datos por accidente.
        btnBack.setOnClickListener {
            mostrarAdvertenciaSalida()
        }

        onBackPressedDispatcher.addCallback(this) {
            mostrarAdvertenciaSalida()
        }

        // Recibe datos Bluetooth y los procesa en el hilo principal.
        BluetoothManager.onLineaRecibida = { linea ->
            runOnUiThread {
                procesarLineaBluetooth(linea)
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // Envia un comando al modulo Bluetooth y avisa si no se pudo mandar.
    private fun enviarComando(comando: String) {
        if (!BluetoothManager.enviarDato(comando)) {
            Toast.makeText(this, "No se pudo enviar comando Bluetooth", Toast.LENGTH_SHORT).show()
        }
    }

    // Cambia la velocidad entre 1 y 3.
    private fun cambiarVelocidad() {
        velocidadNivel = if (velocidadNivel >= 3) 1 else velocidadNivel + 1
        enviarComando(velocidadNivel.toString())
        actualizarUI()
    }

    // Prepara un escaneo que agrega puntos de muro al mapa.
    private fun iniciarEscaneoMuros(boton: View) {
        detenerMovimientoManual()
        detenerLocalizacion()
        modoManual = true
        modoAuto = false
        botonMovimientoActivo = boton
        escaneoConstruyeMapa = true
        grupoMuroActual++
        enviarComando("M")
    }

    // Activa o detiene la localizacion usando puntos de muro ya conocidos.
    private fun alternarLocalizacion(boton: View) {
        detenerMovimientoManual()

        if (localizacionActiva) {
            detenerLocalizacion()
            botonMovimientoActivo = null
            enviarComando("S")
            actualizarEstadosBotones()
            return
        }

        if (muroPuntos.size < MIN_MUROS_PARA_LOCALIZAR) {
            Toast.makeText(
                this,
                "Primero mide algunas paredes para poder ubicarse",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        escaneoConstruyeMapa = false
        localizacionActiva = true
        modoManual = true
        modoAuto = false
        botonMovimientoActivo = boton
        enviarComando("U")
        localizacionHandler.postDelayed(localizacionRunnable, LOCALIZACION_INTERVALO_MS)
        actualizarEstadosBotones()
    }

    // Detiene las solicitudes periodicas de localizacion.
    private fun detenerLocalizacion() {
        localizacionActiva = false
        localizacionHandler.removeCallbacks(localizacionRunnable)
    }

    // Interpreta las lineas JSON recibidas desde el robot.
    private fun procesarLineaBluetooth(linea: String) {
        try {
            val json = JSONObject(linea)

            if (json.has("scan")) {
                val scan = json.getJSONObject("scan")
                procesarLecturaServo(
                    anguloServo = scan.optInt("ang", 90),
                    distanciaCm = scan.optDouble("dist", 0.0),
                    construirMapa = escaneoConstruyeMapa || !localizacionActiva
                )
                return
            }

            tiempoSegundos = json.optInt("t", tiempoSegundos)
            distanciaTotal = json.optDouble("d", distanciaTotal)
            bateria = json.optInt("b", bateria)
            obstaculosPequenos = json.optInt("sp", obstaculosPequenos)
            obstaculosGrandes = json.optInt("lg", obstaculosGrandes)
            actualizarUI()
        } catch (_: Exception) {
        }
    }

    // Decide si una lectura del servo corrige ubicacion o agrega un muro nuevo.
    private fun procesarLecturaServo(
        anguloServo: Int,
        distanciaCm: Double,
        construirMapa: Boolean
    ) {
        if (distanciaCm < DISTANCIA_MIN_MURO_CM || distanciaCm > DISTANCIA_MAX_MURO_CM) {
            return
        }

        if (!construirMapa && intentarCorregirUbicacion(anguloServo, distanciaCm)) {
            actualizarUI()
            return
        }

        registrarMuroDesdeEscaneo(anguloServo, distanciaCm)
    }

    // Convierte una medicion de distancia y angulo en un punto de muro del mapa.
    private fun registrarMuroDesdeEscaneo(anguloServo: Int, distanciaCm: Double) {
        val distanciaM = (distanciaCm / 100.0) + ROBOT_MEDIO_LARGO_M
        val anguloGlobal = headingGrados + (90 - anguloServo)
        val radianes = Math.toRadians(anguloGlobal)
        val x = posX + sin(radianes) * distanciaM
        val y = posY + cos(radianes) * distanciaM

        muroPuntos.add(MuroPunto(x, y, grupoMuroActual))
        oeste = max(oeste, (-x).coerceAtLeast(0.0))
        norte = max(norte, y.coerceAtLeast(0.0))
        sur = max(sur, (-y).coerceAtLeast(0.0))
        este = max(este, x.coerceAtLeast(0.0))

        mapaPreview.setMuroPuntos(muroPuntos)
        actualizarUI()
    }

    // Ajusta la posicion estimada cuando una lectura coincide con una pared conocida.
    private fun intentarCorregirUbicacion(anguloServo: Int, distanciaCm: Double): Boolean {
        if (muroPuntos.size < MIN_MUROS_PARA_LOCALIZAR) {
            return false
        }

        val distanciaM = (distanciaCm / 100.0) + ROBOT_MEDIO_LARGO_M
        val anguloGlobal = headingGrados + (90 - anguloServo)
        val radianes = Math.toRadians(anguloGlobal)
        val medidoX = posX + sin(radianes) * distanciaM
        val medidoY = posY + cos(radianes) * distanciaM

        val paredCercana = muroPuntos.minByOrNull { punto ->
            val dx = punto.x - medidoX
            val dy = punto.y - medidoY
            (dx * dx) + (dy * dy)
        } ?: return false

        val errorX = paredCercana.x - medidoX
        val errorY = paredCercana.y - medidoY
        val errorM = sqrt((errorX * errorX) + (errorY * errorY))

        if (errorM > ERROR_LOCALIZACION_MAX_M) {
            return false
        }

        posX += errorX * CORRECCION_LOCALIZACION
        posY += errorY * CORRECCION_LOCALIZACION
        actualizarLimitesMapa()
        return true
    }

    // Configura un boton que envia movimiento continuo mientras se mantiene presionado.
    private fun configurarBotonMovimiento(boton: ImageButton, comando: String) {
        boton.setOnTouchListener { vista, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    vista.isPressed = true
                    iniciarMovimientoManual(vista, comando)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    vista.isPressed = false
                    vista.performClick()
                    detenerMovimientoManual()
                    true
                }
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE -> {
                    vista.isPressed = false
                    detenerMovimientoManual()
                    true
                }
                else -> true
            }
        }
    }

    // Configura los botones que mueven el servo a izquierda, centro o derecha.
    private fun configurarBotonServo(boton: ImageButton, comando: String) {
        boton.setOnClickListener {
            detenerMovimientoManual()
            enviarComando(comando)
            botonMovimientoActivo = boton
            actualizarEstadosBotones()
        }
    }

    // Inicia el movimiento manual y programa calculos periodicos de posicion.
    private fun iniciarMovimientoManual(boton: View, comando: String) {
        if (comandoMovimientoActivo == comando) {
            return
        }

        detenerMovimientoManual(enviarStop = false)
        botonMovimientoActivo = boton
        comandoMovimientoActivo = comando
        ultimoTickMovimiento = System.currentTimeMillis()
        modoManual = true
        enviarComandoManual(comando)
        actualizarEstadosBotones()
        movimientoHandler.post(movimientoRunnable)
    }

    // Detiene el movimiento manual y opcionalmente envia el comando de alto.
    private fun detenerMovimientoManual(enviarStop: Boolean = true) {
        if (comandoMovimientoActivo == null) {
            return
        }

        actualizarMovimientoManual(comandoMovimientoActivo!!)
        comandoMovimientoActivo = null
        movimientoHandler.removeCallbacks(movimientoRunnable)
        botonMovimientoActivo = null

        if (enviarStop) {
            enviarComando("S")
        }

        actualizarEstadosBotones()
    }

    // Actualiza distancia, giro o posicion segun el comando manual activo.
    private fun actualizarMovimientoManual(comando: String) {
        val ahora = System.currentTimeMillis()
        val dt = ((ahora - ultimoTickMovimiento).coerceAtLeast(0)) / 1000.0
        ultimoTickMovimiento = ahora

        if (dt <= 0.0 || !mapeoActivo || !modoManual) {
            return
        }

        when (comando) {
            "F" -> registrarPasoManual(distanciaPorSegundoActual() * dt)
            "B" -> registrarPasoManual(-distanciaPorSegundoActual() * dt)
            "L" -> actualizarGiroManual(-gradosGiroPorSegundoActual() * dt)
            "R" -> actualizarGiroManual(gradosGiroPorSegundoActual() * dt)
        }
    }

    // Actualiza el rumbo del robot en grados y en direccion cardinal aproximada.
    private fun actualizarGiroManual(deltaGrados: Double) {
        headingGrados = (headingGrados + deltaGrados).mod(360.0)
        heading = normalizarHeadingCardinal(headingGrados)
    }

    // Registra un avance o retroceso manual dentro de la ruta.
    private fun registrarPasoManual(distancia: Double) {
        if (!mapeoActivo || !modoManual) {
            return
        }

        val distanciaAbs = kotlin.math.abs(distancia)
        distanciaTotal += distanciaAbs

        val radianes =
            Math.toRadians(headingGrados)

        posX += sin(radianes) * distancia
        posY += cos(radianes) * distancia

        actualizarLimitesMapa()

        rutaManual.add("${ordenRuta++},$posX,$posY,manual")
        detectarPuertaPorAbertura()
        mapaPreview.setRutaDesdeTexto(rutaManual, puertasDetectadas)
        actualizarUI()
    }

    // Expande los limites del mapa segun la posicion actual.
    private fun actualizarLimitesMapa() {
        norte = max(norte, posY.coerceAtLeast(0.0))
        sur = max(sur, (-posY).coerceAtLeast(0.0))
        este = max(este, posX.coerceAtLeast(0.0))
        oeste = max(oeste, (-posX).coerceAtLeast(0.0))
    }

    // Limpia todos los datos para iniciar un mapa desde cero.
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
        velocidadNivel = 1
        heading = 0
        headingGrados = 0.0
        posX = 0.0
        posY = 0.0
        ordenRuta = 0
        grupoMuroActual = 0
        escaneoConstruyeMapa = false
        detenerLocalizacion()
        rutaManual.clear()
        muroPuntos.clear()
        mapaPreview.setRutaDesdeTexto(rutaManual, puertasDetectadas)
        mapaPreview.setMuroPuntos(muroPuntos)
        mapaPreview.setMedidasMuros(oeste, norte, sur, este)
        actualizarUI()
    }

    // Pide al robot que vuelva al punto guardado mas cercano al origen.
    private fun volverAlPuntoOrigen(botonOrigen: View) {
        val puntoCercano =
            obtenerPuntoMasCercanoAlOrigen()

        if (puntoCercano == null) {
            Toast.makeText(
                this,
                "Aun no hay ruta guardada para volver",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        mapeoActivo = false
        botonMovimientoActivo = botonOrigen
        actualizarEstadosBotones()
        enviarComandoConSalidaAuto("O")
        modoAuto = false

        Toast.makeText(
            this,
            "Volviendo al punto recordado mas cercano al inicio",
            Toast.LENGTH_SHORT
        ).show()
    }

    // Obtiene el punto de ruta mas cercano a las coordenadas 0,0.
    private fun obtenerPuntoMasCercanoAlOrigen(): RutaPunto? {
        return rutaManual
            .mapNotNull { punto ->
                val partes = punto.split(",")
                if (partes.size < 4) {
                    null
                } else {
                    RutaPunto(
                        orden = partes[0].toIntOrNull() ?: 0,
                        x = partes[1].toDoubleOrNull() ?: 0.0,
                        y = partes[2].toDoubleOrNull() ?: 0.0,
                        modo = partes[3]
                    )
                }
            }
            .minByOrNull { punto ->
                sqrt((punto.x * punto.x) + (punto.y * punto.y))
            }
    }

    // Marca una puerta posible si el ancho estimado es suficiente para el robot.
    private fun detectarPuertaPorAbertura() {
        val anchoEstimadoCm = max(norte + sur, este + oeste) * 100

        if (anchoEstimadoCm >= 70 && puertasDetectadas == 0) {
            puertasDetectadas = 1
        }
    }

    // Refresca textos, velocidad y vista previa del mapa.
    private fun actualizarUI() {
        if (mostrarMedidasMuros) {
            tvOeste.text = "Oeste\n${"%.1f".format(oeste)} M"
            tvNorte.text = "Norte\n${"%.1f".format(norte)} M"
            tvSur.text = "Sur\n${"%.1f".format(sur)} M"
            tvEste.text = "Este\n${"%.1f".format(este)} M"
        } else {
            tvOeste.text = "Oeste"
            tvNorte.text = "Norte"
            tvSur.text = "Sur"
            tvEste.text = "Este"
        }

        tvDistancia.text = "Distancia\n${"%.1f".format(distanciaTotal)} M"
        tvPequenos.text = "Pequenos\n$obstaculosPequenos"
        tvGrandes.text = "Grandes\n$obstaculosGrandes"
        tvVelocidadNumero.text = velocidadNivel.toString()
        mapaPreview.setMedidasMuros(oeste, norte, sur, este)
        mapaPreview.setMostrarMedidas(mostrarMedidasMuros)
        mapaPreview.setMuroPuntos(muroPuntos)
    }

    // Pausa el mapeo y deja resaltado el boton de alto.
    private fun pausarMapeo(botonStop: View) {
        mapeoActivo = false
        modoAuto = false
        detenerLocalizacion()
        comandoMovimientoActivo = null
        movimientoHandler.removeCallbacks(movimientoRunnable)
        botonMovimientoActivo = botonStop
        actualizarEstadosBotones()
    }

    // Envia un comando manual saliendo antes del modo automatico si estaba activo.
    private fun enviarComandoManual(comando: String) {
        enviarComandoConSalidaAuto(comando)
        modoAuto = false
    }

    // Detiene el modo automatico antes de mandar otro comando.
    private fun enviarComandoConSalidaAuto(comando: String) {
        if (modoAuto) {
            enviarComando("S")
            Handler(Looper.getMainLooper()).postDelayed({
                enviarComando(comando)
            }, SALIDA_AUTO_DELAY_MS)
        } else {
            enviarComando(comando)
        }
    }

    // Cancela el modo automatico cuando otra accion toma control.
    private fun salirModoAutoSiNecesario() {
        if (modoAuto) {
            enviarComando("S")
            modoAuto = false
        }
    }

    // Repinta todos los botones segun el modo actual.
    private fun actualizarEstadosBotones() {
        botonesActivos.forEach { pintarBoton(it, seleccionado = false) }

        if (mapeoActivo) {
            pintarBoton(findViewById(R.id.btnIniciarSesion), seleccionado = true)
        }

        if (mostrarMedidasMuros) {
            pintarBoton(findViewById(R.id.btnMedirModo), seleccionado = true)
        }

        botonMovimientoActivo?.let {
            pintarBoton(it, seleccionado = true)
        }
    }

    // Resalta un solo boton y apaga el resto.
    private fun marcarBotonActivo(activo: View) {
        botonesActivos.forEach { boton ->
            pintarBoton(boton, boton == activo)
        }
    }

    // Aplica colores de seleccionado o normal a un boton.
    private fun pintarBoton(boton: View, seleccionado: Boolean) {
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

    // Ajusta iconos y textos dentro de botones compuestos por LinearLayout.
    private fun pintarHijosBoton(contenedor: LinearLayout, icono: Int, texto: Int) {
        for (i in 0 until contenedor.childCount) {
            when (val hijo = contenedor.getChildAt(i)) {
                is ImageView -> hijo.imageTintList = ColorStateList.valueOf(icono)
                is TextView -> hijo.setTextColor(texto)
            }
        }
    }

    // Muestra confirmacion antes de salir y perder el mapeo actual.
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

    // Redondea el angulo a una direccion cardinal de 0, 90, 180 o 270.
    private fun normalizarHeadingCardinal(valor: Double): Int {
        val cardinal = (((valor + 45.0) / 90.0).toInt() * 90) % 360
        return if (cardinal < 0) cardinal + 360 else cardinal
    }

    // Distancia estimada por segundo para cada nivel de velocidad.
    private fun distanciaPorSegundoActual(): Double {
        return when (velocidadNivel) {
            1 -> 0.12
            2 -> 0.18
            else -> 0.24
        }
    }

    // Grados de giro estimados por segundo para cada nivel de velocidad.
    private fun gradosGiroPorSegundoActual(): Double {
        return when (velocidadNivel) {
            1 -> 120.0
            2 -> 180.0
            else -> 240.0
        }
    }

    // Detiene tareas periodicas cuando la Activity pasa a segundo plano.
    override fun onPause() {
        detenerMovimientoManual()
        detenerLocalizacion()
        super.onPause()
    }

    // Libera el callback Bluetooth para no recibir datos despues de cerrar.
    override fun onDestroy() {
        if (BluetoothManager.onLineaRecibida != null) {
            BluetoothManager.onLineaRecibida = null
        }
        super.onDestroy()
    }

    companion object {
        // Constantes de tiempos, dimensiones y tolerancias usadas durante el mapeo.
        private const val MOVIMIENTO_TICK_MS = 100L
        private const val SALIDA_AUTO_DELAY_MS = 80L
        private const val LOCALIZACION_INTERVALO_MS = 1200L
        private const val DISTANCIA_MIN_MURO_CM = 3.0
        private const val DISTANCIA_MAX_MURO_CM = 300.0
        private const val ROBOT_MEDIO_LARGO_M = 0.065
        private const val ROBOT_MEDIO_ANCHO_M = 0.09
        private const val MIN_MUROS_PARA_LOCALIZAR = 8
        private const val ERROR_LOCALIZACION_MAX_M = 0.45
        private const val CORRECCION_LOCALIZACION = 0.35
    }
}
