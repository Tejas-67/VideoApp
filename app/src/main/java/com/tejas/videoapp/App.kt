package com.tejas.videoapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import java.util.UUID

@HiltAndroidApp
class App: Application() {

    companion object {
        val username = UUID.randomUUID().toString().substring(0, 6)
    }
}