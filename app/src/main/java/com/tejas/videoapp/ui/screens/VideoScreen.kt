package com.tejas.videoapp.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.tejas.videoapp.App
import com.tejas.videoapp.ui.components.ConfirmBackDialog
import com.tejas.videoapp.ui.components.SurfaceViewRendererComposable
import com.tejas.videoapp.ui.viewmodel.MainViewModel
import com.tejas.videoapp.utils.Constants
import com.tejas.videoapp.utils.Constants.MAIN_SCREEN

@Composable
fun VideoScreen(
    roomId: String,
    navController: NavController,
    viewModel: MainViewModel
){
    val streamState = viewModel.mediaStreamsState.collectAsState().value ?: hashMapOf()

    // Total number of streams includes the local stream plus the number of remote streams
    val totalNumberOfStreams = 1 + streamState.count { it.key != App.username }

    Column(Modifier.fillMaxSize()) {
        Text(text = "room name = $roomId")

        // Calculate the modifier for each stream so they share the space equally
        val streamModifier = Modifier
            .fillMaxWidth()
            .weight(1f / totalNumberOfStreams) // Divide space equally among all streams

        // Render the local stream
        SurfaceViewRendererComposable(
            modifier = streamModifier,
            streamName = "Local",
            onSurfaceReady = { viewModel.onRoomJoined(roomId!!, it) }
        )

        // Render each remote stream
        streamState.forEach { (streamId, mediaStream) ->
            if (streamId != App.username) {
                // Use the key composable to manage recomposition based on streamId
                key(streamId) {
                    SurfaceViewRendererComposable(
                        modifier = streamModifier,
                        streamName = streamId,
                        onSurfaceReady = { surfaceView ->
                            viewModel.initRemoteSurfaceView(surfaceView)
                            mediaStream.videoTracks.firstOrNull()?.addSink(surfaceView)
                        }
                    )
                }
            }
        }
    }

    ConfirmBackDialog {
        viewModel::onLeaveConferenceClicked.invoke()
        navController.navigate(MAIN_SCREEN)
    }
}