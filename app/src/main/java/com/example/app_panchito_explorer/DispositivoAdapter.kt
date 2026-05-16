package com.example.app_panchito_explorer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class DispositivoAdapter(
    private val context: Context,
    private val lista: ArrayList<String>
) : BaseAdapter() {

    override fun getCount(): Int {
        return lista.size
    }

    override fun getItem(position: Int): Any {
        return lista[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup?
    ): View {

        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_dispositivo, parent, false)

        val textNombre =
            view.findViewById<TextView>(R.id.textNombre)

        val textDireccion =
            view.findViewById<TextView>(R.id.textDireccion)

        val datos = lista[position].split("\n")

        val nombre = datos.getOrNull(0) ?: "Dispositivo"
        val direccion = datos.getOrNull(1) ?: ""

        textNombre.text = nombre
        textDireccion.text = direccion

        return view
    }
}