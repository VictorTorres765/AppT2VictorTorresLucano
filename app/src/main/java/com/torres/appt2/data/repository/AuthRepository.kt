package com.torres.appt2.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await


/**
 * Repositorio para manejar todas las operaciones de autenticación de usuario con Firebase.
 * Centraliza la lógica de registro, inicio de sesión y cierre de sesión.
 *
 * @param firebaseAuth Instancia de FirebaseAuth para la autenticación.
 * @param firestore Instancia de FirebaseFirestore para guardar datos de usuario (opcional, pero útil para perfiles).
 */
class AuthRepository(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    /**
     * Registra un nuevo usuario con correo electrónico y contraseña.
     * @param email El correo electrónico del usuario.
     * @param password La contraseña del usuario.
     * @return Un objeto Result<Boolean> que indica éxito o fracaso.
     */
    suspend fun registerUser(email: String, password: String): Result<Boolean> {
        return try {
            // Crea el usuario con correo y contraseña
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user

            // Si el usuario se creó correctamente, puedes guardar información adicional en Firestore
            user?.let { firebaseUser ->
                val userData = hashMapOf(
                    "email" to firebaseUser.email,
                    "createdAt" to System.currentTimeMillis()
                    // Puedes añadir más campos de perfil aquí si es necesario
                )
                // Guarda el UID del usuario como ID del documento en la colección 'users'
                firestore.collection("users").document(firebaseUser.uid).set(userData).await()
            }
            Result.success(true) // Registro exitoso
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e) // Fallo en el registro
        }
    }

    /**
     * Inicia sesión de un usuario con correo electrónico y contraseña.
     * @param email El correo electrónico del usuario.
     * @param password La contraseña del usuario.
     * @return Un objeto Result<Boolean> que indica éxito o fracaso.
     */
    suspend fun loginUser(email: String, password: String): Result<Boolean> {
        return try {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            Result.success(true) // Inicio de sesión exitoso
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e) // Fallo en el inicio de sesión
        }
    }

    /**
     * Cierra la sesión del usuario actual.
     */
    fun logoutUser() {
        firebaseAuth.signOut()
    }

    /**
     * Obtiene el ID del usuario actualmente autenticado.
     * @return El UID del usuario si está autenticado, o null si no lo está.
     */
    fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    /**
     * Verifica si hay un usuario actualmente autenticado.
     * @return true si hay un usuario autenticado, false en caso contrario.
     */
    fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }
}