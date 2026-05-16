package com.example.app_panchito_explorer

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.UUID

@SuppressLint("MissingPermission")
class bluetoothActivity : AppCompatActivity() {

    // =========================
    // BLUETOOTH
    // =========================

    private var bluetoothAdapter: BluetoothAdapter? = null

    private var bluetoothLeScanner: BluetoothLeScanner? = null

    // =========================
    // GATT GLOBAL
    // =========================

    private var bluetoothGatt: BluetoothGatt?
        get() = BluetoothManager.bluetoothGatt
        set(value) {
            BluetoothManager.bluetoothGatt = value
        }

    // =========================
    // CHARACTERISTIC GLOBAL
    // =========================

    private var characteristicTX: BluetoothGattCharacteristic?
        get() = BluetoothManager.characteristicTX
        set(value) {
            BluetoothManager.characteristicTX = value
        }

    // =========================
    // UUID HM-10 / HMSoft
    // =========================

    private val SERVICE_UUID: UUID =
        UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB")

    private val CHARACTERISTIC_UUID: UUID =
        UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB")

    // =========================
    // UI
    // =========================

    private lateinit var listBluetooth: ListView

    private lateinit var dispositivos: ArrayList<String>

    private lateinit var dispositivosBLE: ArrayList<BluetoothDevice>

    private var dispositivoPendiente: String? = null

    // =========================
    // ON CREATE
    // =========================

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(R.layout.activity_bluetooth)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->

            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.setPadding(
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

        val btnBuscar =
            findViewById<Button>(R.id.btnBuscar)

        val bottomNav =
            findViewById<BottomNavigationView>(R.id.bottomNavigation)

        listBluetooth =
            findViewById(R.id.listBluetooth)

        // =========================
        // LISTAS
        // =========================

        dispositivos = ArrayList()

        dispositivosBLE = ArrayList()

        // =========================
        // ADAPTADOR BLUETOOTH
        // =========================

        try {

            bluetoothAdapter =
                BluetoothAdapter.getDefaultAdapter()

            bluetoothLeScanner =
                bluetoothAdapter?.bluetoothLeScanner

        } catch (e: Exception) {

            e.printStackTrace()

            bluetoothAdapter = null
        }

        // =========================
        // NAVIGATION
        // =========================

        bottomNav.selectedItemId = R.id.nav_bluetooth

        bottomNav.setOnItemSelectedListener {

            when (it.itemId) {

                R.id.nav_home -> {

                    startActivity(
                        Intent(this, homeActivity::class.java)
                    )

                    true
                }

                R.id.nav_bluetooth -> true

                R.id.nav_map -> {

                    startActivity(
                        Intent(this, antesDeMapearActivity::class.java)
                    )

                    true
                }

                R.id.nav_files -> {

                    startActivity(
                        Intent(this, guardadosActivity::class.java)
                    )

                    true
                }

                else -> false
            }
        }

        // =========================
        // REGRESAR
        // =========================

        btnBack.setOnClickListener {

            finish()
        }

        // =========================
        // BUSCAR DISPOSITIVOS
        // =========================

        btnBuscar.setOnClickListener {

            // VERIFICAR PERMISOS

            if (!tienePermisos()) {

                pedirPermisos()

                Toast.makeText(
                    this,
                    "Concede permisos Bluetooth",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // VERIFICAR BLUETOOTH

            val adapter = bluetoothAdapter

            if (adapter == null) {

                Toast.makeText(
                    this,
                    "Bluetooth no soportado",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // ACTIVAR BLUETOOTH

            if (!adapter.isEnabled) {

                val enableBtIntent =
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

                startActivity(enableBtIntent)

                return@setOnClickListener
            }

            try {

                // DETENER ESCANEO ANTERIOR

                bluetoothLeScanner?.stopScan(leScanCallback)

                // LIMPIAR LISTA

                dispositivos.clear()

                dispositivosBLE.clear()

                listBluetooth.adapter = null

                // INICIAR ESCANEO

                bluetoothLeScanner?.startScan(leScanCallback)

                Toast.makeText(
                    this,
                    "Buscando dispositivos BLE...",
                    Toast.LENGTH_SHORT
                ).show()

                // DETENER ESCANEO AUTOMÁTICO

                Handler(Looper.getMainLooper()).postDelayed({

                    try {

                        bluetoothLeScanner?.stopScan(leScanCallback)

                        Toast.makeText(
                            this,
                            "Escaneo finalizado",
                            Toast.LENGTH_SHORT
                        ).show()

                    } catch (e: Exception) {

                        e.printStackTrace()
                    }

                }, 10000)

            } catch (e: Exception) {

                e.printStackTrace()

                Toast.makeText(
                    this,
                    "Error escaneando BLE",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // =========================
        // CLICK DISPOSITIVO
        // =========================

        listBluetooth.setOnItemClickListener { _, _, position, _ ->

            try {

                bluetoothLeScanner?.stopScan(leScanCallback)

                val device = dispositivosBLE[position]

                val nombre =
                    device.name ?: "Dispositivo BLE"

                val direccion =
                    device.address

                // =========================
                // GUARDAR NOMBRE
                // =========================

                BluetoothManager.nombreDispositivo =
                    nombre

                val info =
                    "$nombre\n$direccion"

                Toast.makeText(
                    this,
                    "Conectando a $nombre",
                    Toast.LENGTH_SHORT
                ).show()

                // =========================
                // CERRAR CONEXIÓN ANTERIOR
                // =========================

                BluetoothManager.cerrarConexion()
                dispositivoPendiente = info

                // =========================
                // NUEVA CONEXIÓN
                // =========================

                bluetoothGatt =
                    device.connectGatt(
                        this,
                        false,
                        gattCallback
                    )

            } catch (e: Exception) {

                e.printStackTrace()

                Toast.makeText(
                    this,
                    "Error conectando",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun abrirDispositivoConectado() {

        val info =
            dispositivoPendiente ?: return

        dispositivoPendiente = null

        val intent =
            Intent(
                this,
                dispositivoActivity::class.java
            )

        intent.putExtra(
            "device",
            info
        )

        startActivity(intent)
    }

    // =========================
    // CALLBACK ESCANEO BLE
    // =========================

    private val leScanCallback = object : ScanCallback() {

        override fun onScanResult(
            callbackType: Int,
            result: ScanResult
        ) {

            try {

                val device = result.device

                val nombre =
                    device.name ?: "Dispositivo BLE"

                val direccion =
                    device.address

                val info =
                    "$nombre\n$direccion"

                // EVITAR DUPLICADOS

                if (!dispositivos.contains(info)) {

                    dispositivos.add(info)

                    dispositivosBLE.add(device)

                    runOnUiThread {

                        val adapterList =
                            DispositivoAdapter(
                                this@bluetoothActivity,
                                dispositivos
                            )

                        listBluetooth.adapter =
                            adapterList
                    }
                }

            } catch (e: Exception) {

                e.printStackTrace()
            }
        }

        override fun onScanFailed(errorCode: Int) {

            runOnUiThread {

                Toast.makeText(
                    this@bluetoothActivity,
                    "Error escaneo BLE: $errorCode",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // =========================
    // GATT CALLBACK
    // =========================

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {

            runOnUiThread {

                if (newState ==
                    BluetoothProfile.STATE_CONNECTED
                ) {

                    // =========================
                    // CONECTADO
                    // =========================

                    BluetoothManager.conectado = true

                    Toast.makeText(
                        this@bluetoothActivity,
                        "Dispositivo conectado",
                        Toast.LENGTH_SHORT
                    ).show()

                    gatt.discoverServices()

                    abrirDispositivoConectado()

                } else if (
                    newState ==
                    BluetoothProfile.STATE_DISCONNECTED
                ) {

                    // =========================
                    // DESCONECTADO
                    // =========================

                    BluetoothManager.conectado = false
                    BluetoothManager.characteristicTX = null
                    dispositivoPendiente = null

                    Toast.makeText(
                        this@bluetoothActivity,
                        "Dispositivo desconectado",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        override fun onServicesDiscovered(
            gatt: BluetoothGatt,
            status: Int
        ) {

            try {

                val service =
                    gatt.getService(SERVICE_UUID)

                if (service == null) {

                    runOnUiThread {

                        Toast.makeText(
                            this@bluetoothActivity,
                            "Servicio BLE no encontrado",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    return
                }

                characteristicTX =
                    service.getCharacteristic(
                        CHARACTERISTIC_UUID
                    )

                runOnUiThread {

                    if (characteristicTX != null) {

                        Toast.makeText(
                            this@bluetoothActivity,
                            "BLE listo",
                            Toast.LENGTH_SHORT
                        ).show()

                        enviarDato("HOLA")

                    } else {

                        Toast.makeText(
                            this@bluetoothActivity,
                            "Característica BLE no encontrada",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            } catch (e: Exception) {

                e.printStackTrace()
            }
        }
    }

    // =========================
    // ENVIAR DATOS
    // =========================

    private fun enviarDato(mensaje: String) {

        try {

            if (characteristicTX == null) {

                Toast.makeText(
                    this,
                    "BLE no listo",
                    Toast.LENGTH_SHORT
                ).show()

                return
            }

            characteristicTX?.value =
                mensaje.toByteArray()

            bluetoothGatt?.writeCharacteristic(
                characteristicTX
            )

            Toast.makeText(
                this,
                "Dato enviado: $mensaje",
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {

            e.printStackTrace()
        }
    }

    // =========================
    // VERIFICAR PERMISOS
    // =========================

    private fun tienePermisos(): Boolean {

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {

            val bluetooth =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED

            val scan =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED

            val location =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

            bluetooth && scan && location

        } else {

            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    // =========================
    // PEDIR PERMISOS
    // =========================

    private fun pedirPermisos() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                1
            )

        } else {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                1
            )
        }
    }

    // =========================
    // RESULTADO PERMISOS
    // =========================

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == 1) {

            if (
                grantResults.isNotEmpty() &&
                grantResults.all {
                    it == PackageManager.PERMISSION_GRANTED
                }
            ) {

                Toast.makeText(
                    this,
                    "Permisos concedidos",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                Toast.makeText(
                    this,
                    "Permisos requeridos",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // =========================
    // CERRAR
    // =========================

    override fun onDestroy() {

        super.onDestroy()

        try {

            // SOLO DETENER ESCANEO
            // NO CERRAR GATT

            bluetoothLeScanner?.stopScan(
                leScanCallback
            )

        } catch (e: Exception) {

            e.printStackTrace()
        }
    }
}
