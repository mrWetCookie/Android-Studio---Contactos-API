package com.example.contactos_app1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.contactos_app1.data.AppDatabase
import com.example.contactos_app1.data.Contact
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow

class ContactViewModel(application: Application) :
    AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).contactDao()

    val contacts = dao.getAllContacts()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun getContactById(id: Int): Flow<Contact?> {
        return dao.getContactById(id)
    }

    fun insert(contact: Contact) {
        viewModelScope.launch {
            dao.insert(contact)
        }
    }

    fun update(contact: Contact) {
        viewModelScope.launch {
            dao.update(contact)
        }
    }

    fun delete(contact: Contact) {
        viewModelScope.launch {
            dao.delete(contact)
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            dao.deleteAll()
        }
    }
}