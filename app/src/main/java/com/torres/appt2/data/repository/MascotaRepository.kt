package com.torres.appt2.data.repository

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.auth.FirebaseAuth
import com.torres.appt2.data.model.Mascota
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

class MascotaRepository(
    private val firestore: FirebaseFirestore,
    private val contentResolver: ContentResolver
) {

    private val petsCollection = firestore.collection("pets")

    suspend fun addPet(pet: Mascota, imageUri: android.net.Uri?): Result<Boolean> {
        return try {
            var petToSave = pet
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                ?: throw Exception("Usuario no autenticado")
            petToSave = pet.copy(ownerId = currentUserId)

            if (imageUri != null) {
                Log.d("MascotaRepository", "Convirtiendo imagen a Base64: $imageUri")
                val inputStream = contentResolver.openInputStream(imageUri)
                if (inputStream != null) {
                    // Decodificar la imagen a Bitmap
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    // Comprimir la imagen
                    val compressedBitmap = compressBitmap(bitmap, 80) // Calidad ajustable (0-100)
                    // Convertir a Base64
                    val base64Image = bitmapToBase64(compressedBitmap)
                    Log.d("MascotaRepository", "Imagen convertida a Base64, tamaño: ${base64Image.length} bytes")
                    petToSave = petToSave.copy(imageBase64 = base64Image)
                    inputStream.close()
                } else {
                    Log.e("MascotaRepository", "No se pudo abrir el flujo de la imagen")
                    throw Exception("No se pudo abrir el flujo de la imagen")
                }
            }
            petsCollection.add(petToSave).await()
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun getPetsForOwner(ownerId: String): Flow<Result<List<Mascota>>> = callbackFlow {
        val subscription = petsCollection
            .whereEqualTo("ownerId", ownerId)
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    trySend(Result.failure(e))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val pets = snapshot.documents.mapNotNull { document ->
                        document.toObject(Mascota::class.java)?.apply {
                            this.id = document.id
                        }
                    }
                    trySend(Result.success(pets))
                } else {
                    trySend(Result.success(emptyList()))
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun updatePet(pet: Mascota, imageUri: android.net.Uri?): Result<Boolean> {
        return try {
            var petToSave = pet
            if (imageUri != null) {
                Log.d("MascotaRepository", "Convirtiendo imagen a Base64: $imageUri")
                val inputStream = contentResolver.openInputStream(imageUri)
                if (inputStream != null) {
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    val compressedBitmap = compressBitmap(bitmap, 80)
                    val base64Image = bitmapToBase64(compressedBitmap)
                    Log.d("MascotaRepository", "Imagen convertida a Base64, tamaño: ${base64Image.length} bytes")
                    petToSave = pet.copy(imageBase64 = base64Image)
                    inputStream.close()
                } else {
                    Log.e("MascotaRepository", "No se pudo abrir el flujo de la imagen")
                    throw Exception("No se pudo abrir el flujo de la imagen")
                }
            }
            petsCollection.document(pet.id).set(petToSave).await()
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun deletePet(petId: String): Result<Boolean> {
        return try {
            petsCollection.document(petId).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun compressBitmap(bitmap: Bitmap, quality: Int): Bitmap {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val byteArray = outputStream.toByteArray()
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}