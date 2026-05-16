package com.example.app_panchito_explorer

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object MapaFileManager {

    const val MIME_TYPE = "application/json"
    const val EXTENSION = ".panchitomap"

    fun exportar(context: Context, mapaId: Int): String? {
        val db = DBHelper(context)
        val mapa = db.obtenerMapa(mapaId) ?: return null
        val rutas = db.obtenerRutas(mapaId)
        val puertas = db.obtenerPuertas(mapaId)

        val json = JSONObject()
        json.put("format", "panchito-map")
        json.put("version", 1)
        json.put("nombre", mapa.nombre)
        json.put("descripcion", mapa.descripcion)
        json.put("distanciaTotal", mapa.distanciaTotal)
        json.put("tiempoSegundos", mapa.tiempoSegundos)
        json.put("oeste", mapa.oeste)
        json.put("norte", mapa.norte)
        json.put("sur", mapa.sur)
        json.put("este", mapa.este)
        json.put("pequenos", mapa.pequenos)
        json.put("grandes", mapa.grandes)
        json.put("puertas", mapa.puertas)

        val rutasJson = JSONArray()
        rutas.forEach { punto ->
            rutasJson.put(
                JSONObject()
                    .put("orden", punto.orden)
                    .put("x", punto.x)
                    .put("y", punto.y)
                    .put("modo", punto.modo)
            )
        }
        json.put("ruta", rutasJson)

        val puertasJson = JSONArray()
        puertas.forEach { puerta ->
            puertasJson.put(
                JSONObject()
                    .put("orden", puerta.orden)
                    .put("x", puerta.x)
                    .put("y", puerta.y)
                    .put("anchoEstimado", puerta.anchoEstimado)
                    .put("nota", puerta.nota)
            )
        }
        json.put("puertasDetalle", puertasJson)

        return json.toString(2)
    }

    fun importar(context: Context, contenido: String): Int {
        val json = JSONObject(contenido)

        if (json.optString("format") != "panchito-map") {
            throw IllegalArgumentException("Archivo de mapa no compatible")
        }

        val db = DBHelper(context)
        val mapaId = db.insertarMapa(
            nombre = json.optString("nombre", "Mapa importado"),
            descripcion = json.optString("descripcion", "Importado"),
            distancia = json.optDouble("distanciaTotal", 0.0),
            tiempoSegundos = json.optInt("tiempoSegundos", 0),
            oeste = json.optDouble("oeste", 0.0),
            norte = json.optDouble("norte", 0.0),
            sur = json.optDouble("sur", 0.0),
            este = json.optDouble("este", 0.0),
            pequenos = json.optInt("pequenos", 0),
            grandes = json.optInt("grandes", 0),
            puertas = json.optInt("puertas", 0)
        ).toInt()

        db.insertarMuro(mapaId, "Oeste", json.optDouble("oeste", 0.0))
        db.insertarMuro(mapaId, "Norte", json.optDouble("norte", 0.0))
        db.insertarMuro(mapaId, "Sur", json.optDouble("sur", 0.0))
        db.insertarMuro(mapaId, "Este", json.optDouble("este", 0.0))

        val rutas = json.optJSONArray("ruta") ?: JSONArray()
        for (i in 0 until rutas.length()) {
            val punto = rutas.getJSONObject(i)
            db.insertarRutaPunto(
                mapaId = mapaId,
                orden = punto.optInt("orden", i),
                x = punto.optDouble("x", 0.0),
                y = punto.optDouble("y", 0.0),
                modo = punto.optString("modo", "manual")
            )
        }

        val puertas = json.optJSONArray("puertasDetalle") ?: JSONArray()
        for (i in 0 until puertas.length()) {
            val puerta = puertas.getJSONObject(i)
            db.insertarPuerta(
                mapaId = mapaId,
                orden = puerta.optInt("orden", i),
                x = puerta.optDouble("x", 0.0),
                y = puerta.optDouble("y", 0.0),
                anchoEstimado = puerta.optDouble("anchoEstimado", 70.0),
                nota = puerta.optString("nota", "Puerta posible")
            )
        }

        return mapaId
    }
}
