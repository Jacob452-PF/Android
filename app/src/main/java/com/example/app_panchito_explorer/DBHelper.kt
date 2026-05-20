package com.example.app_panchito_explorer

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class MapaGuardado(
    val id: Int,
    val nombre: String,
    val descripcion: String,
    val distanciaTotal: Double,
    val tiempoSegundos: Int,
    val oeste: Double,
    val norte: Double,
    val sur: Double,
    val este: Double,
    val pequenos: Int,
    val grandes: Int,
    val puertas: Int
)

data class RutaPunto(
    val orden: Int,
    val x: Double,
    val y: Double,
    val modo: String
)

data class PuertaGuardada(
    val orden: Int,
    val x: Double,
    val y: Double,
    val anchoEstimado: Double,
    val nota: String
)

class DBHelper(context: Context) :
    SQLiteOpenHelper(context, "panchito.db", null, 4) {

    override fun onCreate(db: SQLiteDatabase) {
        crearTablas(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            crearTablaMuroPuntos(db)
            return
        } else if (oldVersion < 4) {
            db.execSQL("ALTER TABLE muro_puntos ADD COLUMN grupo INTEGER DEFAULT 0")
            return
        }

        db.execSQL("DROP TABLE IF EXISTS puertas")
        db.execSQL("DROP TABLE IF EXISTS rutas")
        db.execSQL("DROP TABLE IF EXISTS muro_puntos")
        db.execSQL("DROP TABLE IF EXISTS obstaculos")
        db.execSQL("DROP TABLE IF EXISTS muros")
        db.execSQL("DROP TABLE IF EXISTS mapas")
        db.execSQL("DROP TABLE IF EXISTS dispositivos")
        crearTablas(db)
    }

    private fun crearTablas(db: SQLiteDatabase) {
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
                tiempo_segundos INTEGER DEFAULT 0,
                oeste REAL DEFAULT 0,
                norte REAL DEFAULT 0,
                sur REAL DEFAULT 0,
                este REAL DEFAULT 0,
                obstaculos_pequenos INTEGER DEFAULT 0,
                obstaculos_grandes INTEGER DEFAULT 0,
                puertas INTEGER DEFAULT 0,
                entradas TEXT,
                salidas TEXT,
                dispositivo_id INTEGER
            )
        """)

        db.execSQL("""
            CREATE TABLE muros(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                mapa_id INTEGER,
                direccion TEXT,
                distancia REAL
            )
        """)

        crearTablaMuroPuntos(db)

        db.execSQL("""
            CREATE TABLE obstaculos(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                mapa_id INTEGER,
                tamano TEXT,
                ancho REAL,
                alto REAL,
                distancia REAL DEFAULT 0,
                angulo INTEGER DEFAULT -1,
                modo TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE rutas(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                mapa_id INTEGER,
                orden INTEGER,
                x REAL,
                y REAL,
                modo TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE puertas(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                mapa_id INTEGER,
                orden INTEGER,
                x REAL,
                y REAL,
                ancho_estimado REAL,
                nota TEXT
            )
        """)
    }

    private fun crearTablaMuroPuntos(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS muro_puntos(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                mapa_id INTEGER,
                orden INTEGER,
                x REAL,
                y REAL,
                grupo INTEGER DEFAULT 0
            )
        """)
    }

    fun insertarDispositivo(nombre: String, direccion: String): Long {
        val values = ContentValues().apply {
            put("nombre", nombre)
            put("direccion", direccion)
        }
        return writableDatabase.insert("dispositivos", null, values)
    }

    fun insertarMapa(
        nombre: String,
        descripcion: String,
        distancia: Double,
        tiempoSegundos: Int,
        oeste: Double,
        norte: Double,
        sur: Double,
        este: Double,
        pequenos: Int,
        grandes: Int,
        puertas: Int,
        dispositivoId: Int = 0
    ): Long {
        val values = ContentValues().apply {
            put("nombre", nombre)
            put("descripcion", descripcion)
            put("distancia_total", distancia)
            put("tiempo_segundos", tiempoSegundos)
            put("oeste", oeste)
            put("norte", norte)
            put("sur", sur)
            put("este", este)
            put("obstaculos_pequenos", pequenos)
            put("obstaculos_grandes", grandes)
            put("puertas", puertas)
            put("entradas", "")
            put("salidas", "")
            put("dispositivo_id", dispositivoId)
        }
        return writableDatabase.insert("mapas", null, values)
    }

    fun insertarMuro(mapaId: Int, direccion: String, distancia: Double): Long {
        val values = ContentValues().apply {
            put("mapa_id", mapaId)
            put("direccion", direccion)
            put("distancia", distancia)
        }
        return writableDatabase.insert("muros", null, values)
    }

    fun insertarObstaculo(
        mapaId: Int,
        tamano: String,
        ancho: Double,
        alto: Double,
        distancia: Double,
        angulo: Int,
        modo: String
    ): Long {
        val values = ContentValues().apply {
            put("mapa_id", mapaId)
            put("tamano", tamano)
            put("ancho", ancho)
            put("alto", alto)
            put("distancia", distancia)
            put("angulo", angulo)
            put("modo", modo)
        }
        return writableDatabase.insert("obstaculos", null, values)
    }

    fun insertarRutaPunto(mapaId: Int, orden: Int, x: Double, y: Double, modo: String): Long {
        val values = ContentValues().apply {
            put("mapa_id", mapaId)
            put("orden", orden)
            put("x", x)
            put("y", y)
            put("modo", modo)
        }
        return writableDatabase.insert("rutas", null, values)
    }

    fun insertarMuroPunto(mapaId: Int, orden: Int, x: Double, y: Double, grupo: Int): Long {
        val values = ContentValues().apply {
            put("mapa_id", mapaId)
            put("orden", orden)
            put("x", x)
            put("y", y)
            put("grupo", grupo)
        }
        return writableDatabase.insert("muro_puntos", null, values)
    }

    fun insertarPuerta(
        mapaId: Int,
        orden: Int,
        x: Double,
        y: Double,
        anchoEstimado: Double,
        nota: String
    ): Long {
        val values = ContentValues().apply {
            put("mapa_id", mapaId)
            put("orden", orden)
            put("x", x)
            put("y", y)
            put("ancho_estimado", anchoEstimado)
            put("nota", nota)
        }
        return writableDatabase.insert("puertas", null, values)
    }

    fun obtenerMapasGuardados(): List<MapaGuardado> {
        val lista = mutableListOf<MapaGuardado>()
        val cursor = readableDatabase.rawQuery(
            """
            SELECT id,nombre,descripcion,distancia_total,tiempo_segundos,
                   oeste,norte,sur,este,obstaculos_pequenos,
                   obstaculos_grandes,puertas
            FROM mapas
            ORDER BY id DESC
            """,
            null
        )

        cursor.use {
            while (it.moveToNext()) {
                lista.add(
                    MapaGuardado(
                        id = it.getInt(0),
                        nombre = it.getString(1),
                        descripcion = it.getString(2),
                        distanciaTotal = it.getDouble(3),
                        tiempoSegundos = it.getInt(4),
                        oeste = it.getDouble(5),
                        norte = it.getDouble(6),
                        sur = it.getDouble(7),
                        este = it.getDouble(8),
                        pequenos = it.getInt(9),
                        grandes = it.getInt(10),
                        puertas = it.getInt(11)
                    )
                )
            }
        }

        return lista
    }

    fun obtenerMapa(id: Int): MapaGuardado? {
        return obtenerMapasGuardados().firstOrNull { it.id == id }
    }

    fun obtenerRutas(mapaId: Int): List<RutaPunto> {
        val lista = mutableListOf<RutaPunto>()
        val cursor = readableDatabase.rawQuery(
            "SELECT orden,x,y,modo FROM rutas WHERE mapa_id=? ORDER BY orden",
            arrayOf(mapaId.toString())
        )

        cursor.use {
            while (it.moveToNext()) {
                lista.add(
                    RutaPunto(
                        orden = it.getInt(0),
                        x = it.getDouble(1),
                        y = it.getDouble(2),
                        modo = it.getString(3)
                    )
                )
            }
        }

        return lista
    }

    fun obtenerPuertas(mapaId: Int): List<PuertaGuardada> {
        val lista = mutableListOf<PuertaGuardada>()
        val cursor = readableDatabase.rawQuery(
            "SELECT orden,x,y,ancho_estimado,nota FROM puertas WHERE mapa_id=? ORDER BY orden",
            arrayOf(mapaId.toString())
        )

        cursor.use {
            while (it.moveToNext()) {
                lista.add(
                    PuertaGuardada(
                        orden = it.getInt(0),
                        x = it.getDouble(1),
                        y = it.getDouble(2),
                        anchoEstimado = it.getDouble(3),
                        nota = it.getString(4)
                    )
                )
            }
        }

        return lista
    }

    fun obtenerMuroPuntos(mapaId: Int): List<MuroPunto> {
        val lista = mutableListOf<MuroPunto>()
        val cursor = readableDatabase.rawQuery(
            "SELECT x,y,grupo FROM muro_puntos WHERE mapa_id=? ORDER BY orden",
            arrayOf(mapaId.toString())
        )

        cursor.use {
            while (it.moveToNext()) {
                lista.add(
                    MuroPunto(
                        x = it.getDouble(0),
                        y = it.getDouble(1),
                        grupo = it.getInt(2)
                    )
                )
            }
        }

        return lista
    }

    fun obtenerMapas(): List<String> {
        return obtenerMapasGuardados().map { "${it.nombre} - ${it.descripcion}" }
    }

    fun eliminarMapa(id: Int) {
        val db = writableDatabase
        val args = arrayOf(id.toString())
        db.delete("puertas", "mapa_id=?", args)
        db.delete("rutas", "mapa_id=?", args)
        db.delete("muro_puntos", "mapa_id=?", args)
        db.delete("obstaculos", "mapa_id=?", args)
        db.delete("muros", "mapa_id=?", args)
        db.delete("mapas", "id=?", args)
    }
}
