package com.example.contactos_app1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.contactos_app1.data.*
import com.example.contactos_app1.data.api.RetrofitClient
import com.example.contactos_app1.data.api.UserResponse
import com.example.contactos_app1.data.api.ImageResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class ContactViewModel(application: Application) :
    AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).contactDao()
    private val syncDao = AppDatabase.getDatabase(application).syncQueueDao()

    // ✅ Mapa para guardar la página actual de cada contacto
    private val _currentPages = mutableMapOf<Int, Int>()
    val currentPages: Map<Int, Int> = _currentPages

    init {
        syncFromLaravel()
        startAutoSync()
    }

    val contacts = dao.getAllContacts()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun getContactById(id: Int): Flow<Contact?> {
        return dao.getContactById(id)
    }

    // ✅ Función para guardar la página actual de un contacto
    fun saveCurrentPage(contactId: Int, page: Int) {
        _currentPages[contactId] = page
        println("Página guardada para contacto $contactId: $page")
    }

    // ✅ Función para obtener la página guardada de un contacto
    fun getCurrentPage(contactId: Int): Int {
        return _currentPages[contactId] ?: 0
    }

    // ===================== CREATE =====================
    fun insert(contact: Contact) {
        viewModelScope.launch {
            val newId = dao.insert(contact).toInt()
            syncDao.insert(
                SyncQueue(
                    contactId = newId,
                    action = "CREATE"
                )
            )
        }
    }

    // ===================== UPDATE =====================
    fun update(contact: Contact) {
        viewModelScope.launch {
            dao.update(contact)
            syncDao.insert(
                SyncQueue(
                    contactId = contact.id,
                    action = "UPDATE"
                )
            )
        }
    }

    // ===================== DELETE =====================
    fun delete(contact: Contact) {
        viewModelScope.launch {
            syncDao.insert(
                SyncQueue(
                    contactId = contact.id,
                    action = "DELETE"
                )
            )
            dao.delete(contact)

            // ✅ Limpiar la página guardada cuando se elimina el contacto
            _currentPages.remove(contact.id)
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            dao.deleteAll()
            // ✅ Limpiar todas las páginas guardadas
            _currentPages.clear()
        }
    }

    // ===================== SYNC FROM LARAVEL =====================
    private fun syncFromLaravel() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.getUsers()

                if (response.isSuccessful) {
                    val userResponses = response.body() ?: emptyList()

                    userResponses.forEach { userResponse ->
                        val existingContact = dao.getContactByIdSync(userResponse.id)

                        val contact = Contact(
                            id = userResponse.id,
                            name = userResponse.name,
                            phone = userResponse.phone ?: "",
                            email = userResponse.email,
                            imageUri = userResponse.images.firstOrNull()?.url,
                            bannerUri = existingContact?.bannerUri,
                            isFavorite = existingContact?.isFavorite ?: false
                        )

                        if (existingContact == null) {
                            dao.insert(contact)
                        } else {
                            dao.update(contact)
                        }
                    }

                    println("Contactos sincronizados: ${userResponses.size}")
                } else {
                    println("Error HTTP: ${response.code()}")
                }

            } catch (e: Exception) {
                println("ERROR API: ${e.message}")
            }
        }
    }

    // ===================== AUTO SYNC =====================
    private fun startAutoSync() {
        viewModelScope.launch {
            while (true) {
                try {
                    syncPendingOperations()
                    syncFromLaravel()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(30000)
            }
        }
    }

    // ===================== SYNC QUEUE =====================
    private suspend fun syncPendingOperations() {
        val pending = syncDao.getAll()

        for (op in pending) {
            try {
                val contact = dao.getContactByIdSync(op.contactId)

                when (op.action) {
                    "CREATE" -> {
                        contact?.let {
                            val safeContact = if (it.email.isBlank()) {
                                it.copy(email = "${it.name.lowercase().replace(" ", ".")}@example.com")
                            } else {
                                it
                            }

                            val response = if (safeContact.imageUri != null && safeContact.imageUri.isNotEmpty()) {
                                try {
                                    val file = File(safeContact.imageUri)
                                    if (file.exists()) {
                                        val requestFile = file.asRequestBody("image/*".toMediaType())
                                        val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)

                                        val name = safeContact.name.toRequestBody("text/plain".toMediaType())
                                        val email = safeContact.email.toRequestBody("text/plain".toMediaType())
                                        val phone = safeContact.phone.toRequestBody("text/plain".toMediaType())

                                        RetrofitClient.api.createUserWithImage(name, email, phone, imagePart)
                                    } else {
                                        RetrofitClient.api.createUser(safeContact)
                                    }
                                } catch (e: Exception) {
                                    println("Error al procesar imagen: ${e.message}")
                                    RetrofitClient.api.createUser(safeContact)
                                }
                            } else {
                                RetrofitClient.api.createUser(safeContact)
                            }

                            if (response.isSuccessful) {
                                val userResponse: UserResponse? = response.body()
                                userResponse?.let { user ->
                                    val updatedContact = Contact(
                                        id = user.id,
                                        name = user.name,
                                        phone = user.phone ?: "",
                                        email = user.email,
                                        imageUri = user.images.firstOrNull()?.url ?: safeContact.imageUri,
                                        bannerUri = safeContact.bannerUri,
                                        isFavorite = safeContact.isFavorite
                                    )
                                    dao.update(updatedContact)
                                    syncDao.delete(op)
                                    println("Sincronizado CREATE: ${user.id}")
                                }
                            } else {
                                println("Error CREATE: ${response.code()}")
                            }
                        }
                    }

                    "UPDATE" -> {
                        contact?.let {
                            val safeContact = if (it.email.isBlank()) {
                                it.copy(email = "${it.name.lowercase().replace(" ", ".")}@example.com")
                            } else {
                                it
                            }

                            val response = if (safeContact.imageUri != null && safeContact.imageUri.isNotEmpty()) {
                                try {
                                    val file = File(safeContact.imageUri)
                                    if (file.exists()) {
                                        val requestFile = file.asRequestBody("image/*".toMediaType())
                                        val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)

                                        val name = safeContact.name.toRequestBody("text/plain".toMediaType())
                                        val email = safeContact.email.toRequestBody("text/plain".toMediaType())
                                        val phone = safeContact.phone.toRequestBody("text/plain".toMediaType())

                                        RetrofitClient.api.updateUserWithImage(safeContact.id, name, email, phone, imagePart)
                                    } else {
                                        RetrofitClient.api.updateUser(safeContact.id, safeContact)
                                    }
                                } catch (e: Exception) {
                                    println("Error al procesar imagen: ${e.message}")
                                    RetrofitClient.api.updateUser(safeContact.id, safeContact)
                                }
                            } else {
                                RetrofitClient.api.updateUser(safeContact.id, safeContact)
                            }

                            if (response.isSuccessful) {
                                val userResponse: UserResponse? = response.body()
                                userResponse?.let { user ->
                                    val updatedContact = Contact(
                                        id = user.id,
                                        name = user.name,
                                        phone = user.phone ?: "",
                                        email = user.email,
                                        imageUri = user.images.firstOrNull()?.url ?: safeContact.imageUri,
                                        bannerUri = safeContact.bannerUri,
                                        isFavorite = safeContact.isFavorite
                                    )
                                    dao.update(updatedContact)
                                    syncDao.delete(op)
                                    println("Sincronizado UPDATE: ${user.id}")
                                }
                            } else {
                                println("Error UPDATE: ${response.code()}")
                            }
                        }
                    }

                    "DELETE" -> {
                        val response = RetrofitClient.api.deleteUser(op.contactId)
                        if (response.isSuccessful) {
                            syncDao.delete(op)
                            println("Sincronizado DELETE: ${op.contactId}")
                        } else {
                            println("Error DELETE: ${response.code()}")
                        }
                    }
                }

            } catch (e: Exception) {
                println("Sync exception: ${e.message}")
            }
        }
    }
}