package com.example.firebaseapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.view.menu.MenuView.ItemView
import androidx.recyclerview.widget.RecyclerView
import com.example.firebaseapp.model.Inventario



class InventarioAdapter(private val inventarios:List<Inventario>) :
    RecyclerView.Adapter<InventarioAdapter.InventarioViewHolder>(){

        private var onItemClickListener: ((String, Inventario)->Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventarioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.inventario_item, parent, false)
        return InventarioViewHolder(view)
    }

    override fun onBindViewHolder(holder: InventarioViewHolder, position: Int) {
        val inventario = inventarios[position]
        holder.bind(inventario)

        holder.itemView.setOnClickListener{
            onItemClickListener?.invoke(inventario.id, inventario)
        }
    }

    override fun getItemCount() = inventarios.size

    fun setOnItemClickListener(Listener: (String, Inventario)-> Unit){
        onItemClickListener = Listener
    }


    class InventarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombre = itemView.findViewById<TextView>(R.id.tvNombre)
        private val tvDireccion = itemView.findViewById<TextView>(R.id.tvDireccion)
        private val tvCantidad = itemView.findViewById<TextView>(R.id.tvCantidad)

        fun bind(inventario: Inventario){//aca mostrara datos en pantalla
            tvNombre.text = inventario.nombre
            tvDireccion.text = inventario.direccion
            tvCantidad.text = inventario.cantidad.toString()
        }
    }


}
