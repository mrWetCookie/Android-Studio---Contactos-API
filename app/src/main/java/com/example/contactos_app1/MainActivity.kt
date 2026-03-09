package com.example.contactos_app1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.contactos_app1.ui.theme.Contactos_app1Theme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.contactos_app1.navigation.NavGraph
import com.example.contactos_app1.viewmodel.ContactViewModel


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Contactos_app1Theme {
                val viewModel: ContactViewModel = viewModel()
                NavGraph(viewModel)
            }
        }
    }
}