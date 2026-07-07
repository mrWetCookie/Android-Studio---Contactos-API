package com.example.contactos_app1.data

import androidx.room.*

@Dao
interface SyncQueueDao {

    @Insert
    suspend fun insert(
        syncQueue: SyncQueue
    )

    @Query(
        "SELECT * FROM sync_queue"
    )
    suspend fun getAll():
            List<SyncQueue>

    @Delete
    suspend fun delete(
        syncQueue: SyncQueue
    )
}