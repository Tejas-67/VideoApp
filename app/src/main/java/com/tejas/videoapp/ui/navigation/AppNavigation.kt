package com.tejas.videoapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tejas.videoapp.ui.screens.HomeScreen
import com.tejas.videoapp.ui.screens.VideoScreen
import com.tejas.videoapp.ui.viewmodel.MainViewModel
import com.tejas.videoapp.utils.Constants.MAIN_SCREEN
import com.tejas.videoapp.utils.Constants.VIDEO_SCREEN

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController,  startDestination = MAIN_SCREEN){
        composable(MAIN_SCREEN){
            HomeScreen(navController = navController, viewModel = viewModel)
        }
        composable(VIDEO_SCREEN){
            VideoScreen(navController = navController, viewModel = viewModel)
        }
    }
}