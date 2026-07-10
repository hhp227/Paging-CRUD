package com.hhp227.paging_crud

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hhp227.paging_crud.ui.theme.PagingCRUDTheme
import com.hhp227.paging_crud.util.InjectorUtils
import com.hhp227.paging_crud.viewmodel.PostViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PagingCRUDTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "content") {
                    composable("content") { backStackEntry ->
                        val viewModel: PostViewModel = viewModel(factory = InjectorUtils.providePostViewModelFactory())
                        val created by backStackEntry.savedStateHandle.getStateFlow("created", false).collectAsState()

                        LaunchedEffect(created) {
                            if (created) {
                                viewModel.refresh()
                                backStackEntry.savedStateHandle["created"] = false
                            }
                        }
                        ContentScreen(
                            viewModel = viewModel,
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
