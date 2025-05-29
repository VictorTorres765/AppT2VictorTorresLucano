package com.torres.appt2.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.torres.appt2.data.repository.AuthRepository
import kotlinx.coroutines.launch


/**
 * ViewModel para la pantalla de inicio de sesión (Login).
 * Maneja la lógica de negocio relacionada con la autenticación del usuario.
 * Interactúa con AuthRepository para realizar operaciones de inicio de sesión.
 */
class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    // LiveData para observar el estado del inicio de sesión
    private val _loginResult = MutableLiveData<Result<Boolean>>()
    val loginResult: LiveData<Result<Boolean>> = _loginResult

    // LiveData para observar si el usuario ya está logueado
    private val _isUserLoggedIn = MutableLiveData<Boolean>()
    val isUserLoggedIn: LiveData<Boolean> = _isUserLoggedIn

    init {
        // Al inicializar el ViewModel, verificamos si el usuario ya está logueado
        checkUserLoggedIn()
    }

    /**
     * Intenta iniciar sesión con el correo electrónico y la contraseña proporcionados.
     * Los resultados se publican en _loginResult.
     * @param email El correo electrónico del usuario.
     * @param password La contraseña del usuario.
     */
    fun login(email: String, password: String) {
        // Lanzamos una corrutina en el ámbito del ViewModel
        // Esto asegura que la operación se cancele automáticamente si el ViewModel se destruye


        viewModelScope.launch {
            val result = authRepository.loginUser(email, password)
            _loginResult.postValue(result) // Publica el resultado en el LiveData
        }
    }

    /**
     * Verifica si hay un usuario actualmente autenticado y actualiza el LiveData _isUserLoggedIn.
     */
    private fun checkUserLoggedIn() {
        _isUserLoggedIn.value = authRepository.isUserLoggedIn()
    }
}
