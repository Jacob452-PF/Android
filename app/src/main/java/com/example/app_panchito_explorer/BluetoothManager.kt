package com.example.app_panchito_explorer

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic

object BluetoothManager {

    // Estado de conexion
    var conectado = false

    // Nombre dispositivo
    var nombreDispositivo: String? = null

    // Direccion dispositivo
    var direccionDispositivo: String? = null

    // GATT global
    var bluetoothGatt: BluetoothGatt? = null

    // Caracteristica TX
    var characteristicTX: BluetoothGattCharacteristic? = null

    // Bateria actual
    var bateria: Int = 0

    private var serialBuffer = StringBuilder()
    var onLineaRecibida: ((String) -> Unit)? = null

    fun enviarDato(mensaje: String): Boolean {

        return try {

            val tx =
                characteristicTX ?: return false

            tx.writeType =
                if (
                    tx.properties and
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0
                ) {
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                } else {
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                }

            tx.value =
                mensaje.toByteArray()

            bluetoothGatt?.writeCharacteristic(tx) == true

        } catch (e: Exception) {

            e.printStackTrace()
            false
        }
    }

    fun cerrarConexion() {

        try {

            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()

        } catch (e: Exception) {

            e.printStackTrace()
        }

        conectado = false
        nombreDispositivo = null
        direccionDispositivo = null
        bluetoothGatt = null
        characteristicTX = null
        bateria = 0
        serialBuffer.clear()
        onLineaRecibida = null
    }

    fun procesarDatoRecibido(bytes: ByteArray) {
        val texto = bytes.toString(Charsets.UTF_8)

        for (char in texto) {
            if (char == '\n' || char == '\r') {
                val linea = serialBuffer.toString().trim()
                serialBuffer.clear()

                if (linea.isNotEmpty()) {
                    onLineaRecibida?.invoke(linea)
                }
            } else {
                serialBuffer.append(char)
            }
        }
    }
}
