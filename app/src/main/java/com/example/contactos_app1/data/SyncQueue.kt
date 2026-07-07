package com.example.contactos_app1.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueue(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val contactId: Int,

    val action: String // CREATE, UPDATE, DELETE
)