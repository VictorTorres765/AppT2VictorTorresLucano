package com.torres.appt2.ui.mascota

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.torres.appt2.data.model.Mascota
import com.torres.appt2.data.repository.AuthRepository
import com.torres.appt2.data.repository.MascotaRepository
import kotlinx.coroutines.launch

class MascotaAddEditViewModel(
    private val petRepository: MascotaRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _saveResult = MutableLiveData<Result<Boolean>>()
    val saveResult: LiveData<Result<Boolean>> = _saveResult

    fun savePet(pet: Mascota, imageUri: android.net.Uri?) {
        val currentUserId = authRepository.getCurrentUserId()
        if (currentUserId == null) {
            _saveResult.postValue(Result.failure(Exception("User not authenticated. Cannot save pet.")))
            return
        }

        val petToSave = pet.copy(ownerId = currentUserId)
        viewModelScope.launch {
            val result = if (petToSave.id.isEmpty()) {
                petRepository.addPet(petToSave, imageUri)
            } else {
                petRepository.updatePet(petToSave, imageUri)
            }
            _saveResult.postValue(result)
        }
    }
}