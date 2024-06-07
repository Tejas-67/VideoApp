package com.tejas.videoapp.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tejas.videoapp.ui.viewmodel.MainViewModel
import com.tejas.videoapp.utils.Constants.VIDEO_SCREEN

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: MainViewModel
){
     val permissionLauncher = rememberLauncherForActivityResult(
         contract = ActivityResultContracts.RequestMultiplePermissions()
     ){ permissions ->
         if(permissions.all { it.value }){
             //viewModel.init()
         }
     }

    LaunchedEffect(key1 = Unit){
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.POST_NOTIFICATIONS
            )
        )
    }

    val roomState = viewModel.roomState.collectAsState()
    Column(Modifier.fillMaxWidth()) {
        Button(
            onClick = {
               //show a dialog
            },
            Modifier
                .padding(10.dp)
                .height(40.dp)
                .fillMaxWidth()
        ) {
            Text(text = "Create Room")
        }
        roomState.value?.let { roomList ->
            LazyColumn(Modifier.weight(15f)) {
                items(roomList) { item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(1.dp)
                            .border(
                                width = 1.dp,
                                color = Color.Gray,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(5.dp)
                            .clickable {
                                navController.navigate("$VIDEO_SCREEN/${item.name}")
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,


                        ) {
                        Text(text = "Room name: ${item.name}")
                        Spacer(modifier = Modifier.padding(5.dp))
                        Text(text = "Members: ${item.population}")

                    }
                }
            }

        }
    }

}