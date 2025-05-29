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
import com.torres.appt2.databinding.ActivityLoginBinding
import com.torres.appt2.ui.mascota.MascotaListActivity


class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var loginViewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
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

        // Inicializa el ViewModel con una Factory (necesario para pasar parámetros al constructor del ViewModel)
        loginViewModel = ViewModelProvider(this, LoginViewModelFactory(authRepository))
            .get(LoginViewModel::class.java)

        // Observa si el usuario ya está logueado
        loginViewModel.isUserLoggedIn.observe(this) { isLoggedIn ->
            if (isLoggedIn) {
                // Si el usuario ya está logueado, redirige a la PetListActivity
                navigateToPetList()
            }
        }

        // Observa el resultado del intento de inicio de sesión
        loginViewModel.loginResult.observe(this) { result ->
            result.onSuccess {
                // Si el inicio de sesión es exitoso, redirige a la PetListActivity
                Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                navigateToPetList()
            }.onFailure { exception ->
                // Si el inicio de sesión falla, muestra un mensaje de error
                Toast.makeText(this, "Error al iniciar sesión: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Configura el listener para el botón de inicio de sesión
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, ingresa correo y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginViewModel.login(email, password)
        }

        // Configura el listener para el texto de registro
        binding.tvRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

    }


    /**
     * Navega a la PetListActivity y finaliza la LoginActivity para que el usuario no pueda volver atrás.
     */
    private fun navigateToPetList() {
        val intent = Intent(this, MascotaListActivity::class.java)
        // Limpia la pila de actividades para que el usuario no pueda volver a la pantalla de login
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Finaliza esta actividad
    }

}


/**
 * Factory para crear instancias de LoginViewModel.
 * Necesario para inyectar el AuthRepository en el ViewModel.
 */
class LoginViewModelFactory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
