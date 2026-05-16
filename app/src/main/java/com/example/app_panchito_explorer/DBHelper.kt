package com.example.app_panchito_explorer

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context) :
    SQLiteOpenHelper(context, "panchito.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {

        db.execSQL("""
            CREATE TABLE dispositivos(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT,
                direccion TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE mapas(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT,
                descripcion TEXT,
                distancia_total REAL,
                entradas TEXT,
                salidas TEXT,
                dispositivo_id INTEGER
            )
        """)

        db.execSQL("""
            CREATE TABLE muros(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                mapa_id INTEGER,
                distancia REAL
            )
        """)

        db.execSQL("""
            CREATE TABLE obstaculos(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                mapa_id INTEGER,
                tamaño TEXT,
                ancho REAL,
                alto REAL
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS obstaculos")
        db.execSQL("DROP TABLE IF EXISTS muros")
        db.execSQL("DROP TABLE IF EXISTS mapas")
        db.execSQL("DROP TABLE IF EXISTS dispositivos")
        onCreate(db)
    }

    // ===============================
    // 📡 INSERTAR DISPOSITIVO
    // ===============================
    fun insertarDispositivo(nombre: String, direccion: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("nombre", nombre)
            put("direccion", direccion)
        }
        return db.insert("dispositivos", null, values)
    }

    // ===============================
    // 🗺️ INSERTAR MAPA
    // ===============================
    fun insertarMapa(
        nombre: String,
        descripcion: String,
        distancia: Double,
        entradas: String,
        salidas: String,
        dispositivoId: Int
    ): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("nombre", nombre)
            put("descripcion", descripcion)
            put("distancia_total", distancia)
            put("entradas", entradas)
            put("salidas", salidas)
            put("dispositivo_id", dispositivoId)
        }
        return db.insert("mapas", null, values)
    }

    // ===============================
    // 🧱 INSERTAR MURO
    // ===============================
    fun insertarMuro(mapaId: Int, distancia: Double): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("mapa_id", mapaId)
            put("distancia", distancia)
        }
        return db.insert("muros", null, values)
    }

    // ===============================
    // ⚠️ INSERTAR OBSTÁCULO
    // ===============================
    fun insertarObstaculo(
        mapaId: Int,
        tamaño: String,
        ancho: Double,
        alto: Double
    ): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("mapa_id", mapaId)
            put("tamaño", tamaño)
            put("ancho", ancho)
            put("alto", alto)
        }
        return db.insert("obstaculos", null, values)
    }

    // ===============================
    // 📖 OBTENER MAPAS
    // ===============================
    fun obtenerMapas(): List<String> {
        val lista = mutableListOf<String>()
        val db = readableDatabase

        val cursor = db.rawQuery("SELECT * FROM mapas", null)

        if (cursor.moveToFirst()) {
            do {
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre"))
                val descripcion = cursor.getString(cursor.getColumnIndexOrThrow("descripcion"))
                lista.add("$nombre - $descripcion")
            } while (cursor.moveToNext())
        }

        cursor.close()
        return lista
    }
}