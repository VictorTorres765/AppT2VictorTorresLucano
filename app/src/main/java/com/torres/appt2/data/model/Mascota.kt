package com.torres.appt2.data.model

import com.google.firebase.firestore.DocumentId

data class Mascota (
    @DocumentId
    var id: String = "",
    var name: String = "",
    var type: String = "",
    var age: Int = 0,
    var ownerId: String = "",
    var imageBase64: String = "" // Cambia de imageUrl a imageBase64
)