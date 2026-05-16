package com.example.app_panchito_explorer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TutorialAdapter(private val lista: List<TutorialItem>) :
    RecyclerView.Adapter<TutorialAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titulo: TextView = view.findViewById(R.id.titulo)
        val descripcion: TextView = view.findViewById(R.id.descripcion)
        val consejo: TextView = view.findViewById(R.id.consejo) // 👈 NUEVO
        val imagen: ImageView = view.findViewById(R.id.imagen)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tutorial, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = lista.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]

        holder.titulo.text = item.titulo
        holder.descripcion.text = item.descripcion
        holder.consejo.text = "Tip: ${item.consejo}" // 👈 NUEVO
        holder.imagen.setImageResource(item.imagen)
    }
}