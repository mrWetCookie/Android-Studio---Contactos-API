package com.example.contactos_app1.navigation


import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.contactos_app1.uii.*
import com.example.contactos_app1.viewmodel.ContactViewModel

@Composable
fun NavGraph(viewModel: ContactViewModel) {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "list"
    ) {

        composable("list") {
            ContactListScreen(navController, viewModel)
        }

        composable(
            route = "detail/{id}",
            arguments = listOf(
                navArgument("id") { type = NavType.IntType }
            )
        ) { backStackEntry ->

            val id = backStackEntry.arguments?.getInt("id") ?: 0

            ContactDetailScreen(
                id = id,
                navController = navController,
                viewModel = viewModel
            )
        }

        composable(
            route = "form/{id}",
            arguments = listOf(
                navArgument("id") { type = NavType.IntType }
            )
        ) { backStackEntry ->

            val id = backStackEntry.arguments?.getInt("id") ?: 0

            ContactFormScreen(
                id = id,
                navController = navController,
                viewModel = viewModel
            )
        }
    }
}