package com.torres.appt2.ui.mascota

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.torres.appt2.data.model.Mascota
import com.torres.appt2.data.repository.AuthRepository
import com.torres.appt2.data.repository.MascotaRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class MascotaListViewModel(
    private val petRepository: MascotaRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // LiveData para observar la lista de mascotas
    private val _pets = MutableLiveData<Result<List<Mascota>>>()
    val pets: LiveData<Result<List<Mascota>>> = _pets

    // LiveData para observar el resultado de una operación de eliminación
    private val _deleteResult = MutableLiveData<Result<Boolean>>()
    val deleteResult: LiveData<Result<Boolean>> = _deleteResult

    // LiveData para observar si el usuario no está autenticado
    private val _isUserNotAuthenticated = MutableLiveData<Boolean>()
    val isUserNotAuthenticated: LiveData<Boolean> = _isUserNotAuthenticated

    init {
        // Al inicializar el ViewModel, cargamos las mascotas
        loadPets()
    }

    /**
     * Carga la lista de mascotas para el usuario actualmente autenticado.
     * Observa los cambios en tiempo real a través del Flow del repositorio.
     */
    fun loadPets() {
        val currentUserId = authRepository.getCurrentUserId()

        // --- INICIO DEPURACIÓN ---
        Log.d("PetListViewModel", "Cargando mascotas para ownerId: $currentUserId")
        // --- FIN DEPURACIÓN ---

        if (currentUserId == null) {
            // Si no hay usuario autenticado, notificar a la UI
            _isUserNotAuthenticated.value = true
            _pets.value = Result.failure(Exception("User not authenticated"))
            Log.e("PetListViewModel", "Usuario no autenticado al cargar mascotas.")
            return
        }

        // Lanzamos una corrutina en el ámbito del ViewModel
        viewModelScope.launch {
            // collectLatest cancela la corrutina anterior si se emite un nuevo valor
            petRepository.getPetsForOwner(currentUserId).collectLatest { result ->
                result.onSuccess { pets ->
                    Log.d("PetListViewModel", "Flow emitió SUCCESS: ${pets.size} mascotas.")
                    // Asegúrate de que el ID del documento se mapee a la propiedad 'id' de Pet
                    // Esto ya se hace en PetRepository, pero es bueno recordarlo.
                    _pets.postValue(Result.success(pets)) // Publica la lista de mascotas en el LiveData
                }.onFailure { exception ->
                    Log.e(
                        "PetListViewModel",
                        "Flow emitió FAILURE: ${exception.message}",
                        exception
                    )
                    _pets.postValue(Result.failure(exception)) // Publica el error en el LiveData
                }
            }
        }
    }


    /**
     * Elimina una mascota de la base de datos.
     * @param petId El ID de la mascota a eliminar.
     */
    fun deletePet(petId: String) {
        viewModelScope.launch {
            val result = petRepository.deletePet(petId)

            result.onSuccess {
                Log.d("PetListViewModel", "Mascota eliminada exitosamente: $petId")
            }.onFailure { exception ->
                Log.e("PetListViewModel", "Error al eliminar mascota: ${exception.message}", exception)
            }

            _deleteResult.postValue(result) // Publica el resultado de la eliminación
            // Después de eliminar, recargamos la lista para reflejar el cambio
            loadPets()
        }
    }
}