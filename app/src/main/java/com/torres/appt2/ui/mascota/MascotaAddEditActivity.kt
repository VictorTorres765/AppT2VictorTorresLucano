package com.torres.appt2.ui.mascota

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.torres.appt2.R
import com.torres.appt2.data.model.Mascota
import com.torres.appt2.data.repository.AuthRepository
import com.torres.appt2.data.repository.MascotaRepository
import com.torres.appt2.databinding.ActivityMascotaAddEditBinding

class MascotaAddEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMascotaAddEditBinding
    private lateinit var addEditPetViewModel: MascotaAddEditViewModel
    private var petId: String? = null
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                Glide.with(this).load(uri).into(binding.ivPetImage)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            pickImageLauncher.launch(intent)
        } else {
            Toast.makeText(this, "Permiso denegado. Habilítalo en Ajustes para seleccionar imágenes.", Toast.LENGTH_LONG).show()
            goToAppSettings()
        }
    }

    private fun goToAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun checkGalleryPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                Log.d("MascotaAddEdit", "Permiso ya concedido")
                val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
                pickImageLauncher.launch(intent)
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                Log.d("MascotaAddEdit", "Mostrando justificación de permiso")
                Toast.makeText(this, "Se necesita permiso para seleccionar imágenes. Por favor, habilítalo.", Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            else -> {
                Log.d("MascotaAddEdit", "Solicitando permiso")
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun base64ToBitmap(base64String: String): Bitmap {
        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMascotaAddEditBinding.inflate(layoutInflater)
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
        addEditPetViewModel = ViewModelProvider(this, AddEditPetViewModelFactory(petRepository, authRepository))
            .get(MascotaAddEditViewModel::class.java)

        // Verifica si es modo edición
        petId = intent.getStringExtra("PET_ID")
        if (petId != null) {
            binding.tvTitle.text = "Editar Mascota"
            binding.etPetName.setText(intent.getStringExtra("PET_NAME"))
            binding.etPetType.setText(intent.getStringExtra("PET_TYPE"))
            binding.etPetAge.setText(intent.getIntExtra("PET_AGE", 0).toString())
            val imageBase64 = intent.getStringExtra("PET_IMAGE_BASE64")
            if (!imageBase64.isNullOrEmpty()) {
                val bitmap = base64ToBitmap(imageBase64)
                Glide.with(this).load(bitmap).into(binding.ivPetImage)
            }
        } else {
            binding.tvTitle.text = "Añadir Nueva Mascota"
        }

        // Configura el listener para seleccionar imagen
        binding.btnSelectImage.setOnClickListener {
            checkGalleryPermission()
        }

        // Configura el listener para guardar
        binding.btnSavePet.setOnClickListener {
            savePet()
        }

        // Configura el listener para el botón Volver
        binding.btnBack.setOnClickListener {
            val intent = Intent(this, MascotaListActivity::class.java)
            startActivity(intent)
            finish() // Cierra la actividad actual
        }

        // Observa el resultado de guardar
        addEditPetViewModel.saveResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "Mascota guardada exitosamente", Toast.LENGTH_SHORT).show()
                finish()
            }.onFailure { exception ->
                Toast.makeText(this, "Error al guardar: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun savePet() {
        val name = binding.etPetName.text.toString().trim()
        val type = binding.etPetType.text.toString().trim()
        val ageString = binding.etPetAge.text.toString().trim()

        if (name.isEmpty() || type.isEmpty() || ageString.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val age = ageString.toIntOrNull()
        if (age == null || age <= 0) {
            Toast.makeText(this, "La edad debe ser un número válido y positivo", Toast.LENGTH_SHORT).show()
            return
        }

        val pet = Mascota(
            id = petId ?: "",
            name = name,
            type = type,
            age = age,
            ownerId = "",
            imageBase64 = intent.getStringExtra("PET_IMAGE_BASE64") ?: ""
        )

        addEditPetViewModel.savePet(pet, selectedImageUri)
    }
}

class AddEditPetViewModelFactory(
    private val petRepository: MascotaRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MascotaAddEditViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MascotaAddEditViewModel(petRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}