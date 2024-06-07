package com.tejas.videoapp.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tejas.videoapp.ui.screens.HomeScreen
import com.tejas.videoapp.ui.screens.VideoScreen
import com.tejas.videoapp.ui.viewmodel.MainViewModel
import com.tejas.videoapp.utils.Constants.MAIN_SCREEN
import com.tejas.videoapp.utils.Constants.VIDEO_SCREEN

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController,  startDestination = MAIN_SCREEN){
        composable(MAIN_SCREEN){
            HomeScreen(navController = navController, viewModel = viewModel)
        }
        composable("video_screen/{roomId}",
            arguments = listOf(navArgument("roomId"){
                type = NavType.StringType
            })){

            VideoScreen(
                roomId = it.arguments?.getString("roomId")?:"",
                navController = navController,
                viewModel = viewModel
            )

        }
    }
}