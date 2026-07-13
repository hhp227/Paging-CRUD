package com.hhp227.paging_crud

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hhp227.paging_crud.ui.theme.PagingCRUDTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PagingCRUDTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "content") {
                    composable("content") { backStackEntry ->
                        val created by backStackEntry.savedStateHandle.getStateFlow("created", false).collectAsState()

                        ContentScreen(
                            refreshRequested = created,
                            onRefreshHandled = { backStackEntry.savedStateHandle["created"] = false },
                            onNavigateToCreate = { navController.navigate("create") }
                        )
                    }
                    composable("create") {
                        CreateScreen(
                            onNavigateUp = navController::navigateUp,
                            onPostCreated = {
                                navController.previousBackStackEntry?.savedStateHandle?.set("created", true)
                                navController.navigateUp()
                            }
                        )
                    }
                }
            }
        }
    }
}
