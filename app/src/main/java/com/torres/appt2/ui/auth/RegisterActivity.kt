package com.torres.appt2.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.torres.appt2.R
import com.torres.appt2.data.repository.AuthRepository
import com.torres.appt2.databinding.ActivityRegisterBinding
import com.torres.appt2.ui.mascota.MascotaListActivity


class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    private lateinit var registerViewModel: RegisterViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializa Firebase Auth y Firestore
        val firebaseAuth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        // Inicializa el repositorio de autenticación
        val authRepository = AuthRepository(firebaseAuth, firestore)

        // Inicializa el ViewModel con una Factory
        registerViewModel = ViewModelProvider(this, RegisterViewModelFactory(authRepository))
            .get(RegisterViewModel::class.java)

        // Observa el resultado del intento de registro
        registerViewModel.registerResult.observe(this) { result ->
            result.onSuccess {
                // Si el registro es exitoso, redirige a la PetListActivity
                Toast.makeText(this, "Registro exitoso. ¡Bienvenido!", Toast.LENGTH_SHORT).show()
                navigateToPetList()
            }.onFailure { exception ->
                // Si el registro falla, muestra un mensaje de error
                Toast.makeText(this, "Error al registrar: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        }


        // Configura el listener para el botón de registro
        binding.btnRegister.setOnClickListener {
            val email = binding.etEmailRegister.text.toString().trim()
            val password = binding.etPasswordRegister.text.toString().trim()
            val confirmPassword = binding.etConfirmPasswordRegister.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Puedes añadir más validaciones de contraseña aquí (ej. longitud mínima)
            if (password.length < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registerViewModel.register(email, password)
        }

        // Configura el listener para el texto de inicio de sesión
        binding.tvLogin.setOnClickListener {
            // Simplemente finaliza esta actividad para volver a LoginActivity
            finish()
        }
    }


    /**
     * Navega a la PetListActivity y finaliza la RegisterActivity para que el usuario no pueda volver atrás.
     */
    private fun navigateToPetList() {
        val intent = Intent(this, MascotaListActivity::class.java)
        // Limpia la pila de actividades para que el usuario no pueda volver a la pantalla de registro
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Finaliza esta actividad
    }

}


/**
 * Factory para crear instancias de RegisterViewModel.
 * Necesario para inyectar el AuthRepository en el ViewModel.
 */
class RegisterViewModelFactory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegisterViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}