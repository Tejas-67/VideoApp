package com.tejas.videoapp.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun ConfirmBackDialog(onConfirmBack: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    BackHandler(enabled = true) {
        showDialog = true
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
            },
            title = { Text("Confirm Exit") },
            text = { Text("Are you sure you want to leave?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        onConfirmBack()
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showDialog = false
                    }
                ) {
                    Text("No")
                }
            }
        )
    }
}