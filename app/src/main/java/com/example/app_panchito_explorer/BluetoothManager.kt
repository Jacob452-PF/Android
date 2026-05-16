package com.example.app_panchito_explorer

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic

object BluetoothManager {

    // Estado de conexion
    var conectado = false

    // Nombre dispositivo
    var nombreDispositivo: String? = null

    // GATT global
    var bluetoothGatt: BluetoothGatt? = null

    // Caracteristica TX
    var characteristicTX: BluetoothGattCharacteristic? = null

    // Bateria actual
    var bateria: Int = 0

    fun cerrarConexion() {

        try {

            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()

        } catch (e: Exception) {

            e.printStackTrace()
        }

        conectado = false
        bluetoothGatt = null
        characteristicTX = null
        bateria = 0
    }
}
