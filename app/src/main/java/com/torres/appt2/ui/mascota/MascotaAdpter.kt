package com.torres.appt2.ui.mascota

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.torres.appt2.R
import com.torres.appt2.data.model.Mascota
import com.torres.appt2.databinding.ItemMascotaBinding

class MascotaAdpter(
    private val onEditClick: (Mascota) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : ListAdapter<Mascota, MascotaAdpter.MascotaViewHolder>(MascotaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MascotaViewHolder {
        val binding = ItemMascotaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MascotaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MascotaViewHolder, position: Int) {
        val pet = getItem(position)
        holder.bind(pet)
    }

    inner class MascotaViewHolder(private val binding: ItemMascotaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pet: Mascota) {
            binding.tvPetName.text = pet.name
            binding.tvPetType.text = pet.type
            binding.tvPetAge.text = pet.age.toString()

            if (pet.imageBase64.isNotEmpty()) {
                val bitmap = base64ToBitmap(pet.imageBase64)
                Glide.with(binding.ivPetImage.context)
                    .load(bitmap)
                    .placeholder(R.drawable.ic_placeholder) // Asegúrate de que este recurso exista
                    .into(binding.ivPetImage)
            } else {
                binding.ivPetImage.setImageResource(R.drawable.ic_placeholder) // Asegúrate de que este recurso exista
            }

            binding.btnEdit.setOnClickListener {
                onEditClick(pet)
            }

            binding.btnDelete.setOnClickListener {
                // Mostrar el AlertDialog de confirmación
                AlertDialog.Builder(binding.root.context)
                    .setTitle("Confirmar eliminación")
                    .setMessage("¿Estás seguro de que deseas eliminar a ${pet.name}?")
                    .setPositiveButton("Sí") { _, _ ->
                        // Si el usuario selecciona "Sí", proceder con la eliminación
                        onDeleteClick(pet.id)
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        // Si el usuario selecciona "No", cerrar el diálogo sin hacer nada
                        dialog.dismiss()
                    }
                    .setCancelable(true)
                    .show()
            }
        }

        private fun base64ToBitmap(base64String: String): Bitmap {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        }
    }
}

class MascotaDiffCallback : DiffUtil.ItemCallback<Mascota>() {
    override fun areItemsTheSame(oldItem: Mascota, newItem: Mascota): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Mascota, newItem: Mascota): Boolean {
        return oldItem == newItem
    }
}