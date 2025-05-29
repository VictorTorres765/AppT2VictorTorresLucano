package com.torres.appt2.ui.mascota

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.torres.appt2.R
import com.torres.appt2.data.repository.AuthRepository
import com.torres.appt2.data.repository.MascotaRepository
import com.torres.appt2.databinding.ActivityMascotaListBinding
import com.torres.appt2.ui.auth.LoginActivity

class MascotaListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMascotaListBinding
    private lateinit var petListViewModel: MascotaListViewModel
    private lateinit var adapter: MascotaAdpter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMascotaListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializa Firebase
        val firebaseAuth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        // Inicializa repositorios
        val authRepository = AuthRepository(firebaseAuth, firestore)
        val petRepository = MascotaRepository(firestore, contentResolver)

        // Inicializa ViewModel
        petListViewModel = ViewModelProvider(this, MascotaListViewModelFactory(petRepository, authRepository))
            .get(MascotaListViewModel::class.java)

        // Configura RecyclerView
        adapter = MascotaAdpter(
            onEditClick = { pet ->
                val intent = Intent(this, MascotaAddEditActivity::class.java).apply {
                    putExtra("PET_ID", pet.id)
                    putExtra("PET_NAME", pet.name)
                    putExtra("PET_TYPE", pet.type)
                    putExtra("PET_AGE", pet.age)
                    putExtra("PET_IMAGE_BASE64", pet.imageBase64)
                }
                startActivity(intent)
            },
            onDeleteClick = { petId ->
                petListViewModel.deletePet(petId)
            }
        )
        binding.rvPets.layoutManager = LinearLayoutManager(this)
        binding.rvPets.adapter = adapter

        // Observa la lista de mascotas
        petListViewModel.pets.observe(this) { result ->
            result.onSuccess { pets ->
                adapter.submitList(pets)
                binding.tvMensaje.text = if (pets.isEmpty()) "No hay mascotas registradas" else "Mis Mascotas"
            }.onFailure { exception ->
                Toast.makeText(this, "Error al cargar mascotas: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Observa el resultado de eliminación
        petListViewModel.deleteResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "Mascota eliminada", Toast.LENGTH_SHORT).show()
            }.onFailure { exception ->
                Toast.makeText(this, "Error al eliminar: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Botón para añadir nueva mascota
        binding.btnAddPet.setOnClickListener {
            startActivity(Intent(this, MascotaAddEditActivity::class.java))
        }

        // Botón para cerrar sesión
        binding.btnCerrarSesion.setOnClickListener {
            authRepository.logoutUser()
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

class MascotaListViewModelFactory(
    private val petRepository: MascotaRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MascotaListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MascotaListViewModel(petRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}