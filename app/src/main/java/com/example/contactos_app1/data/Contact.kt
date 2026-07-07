package com.example.contactos_app1.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val phone: String,
    val email: String,
    val imageUri: String? = null,  // Para tu base de datos local
    val bannerUri: String? = null,
    val isFavorite: Boolean = false
)