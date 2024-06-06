package com.tejas.videoapp.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Binder
import com.tejas.videoapp.remote.socket.SocketClient
import com.tejas.videoapp.remote.socket.SocketEventListener
import com.tejas.videoapp.remote.socket.SocketEventSender
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CallService: Service(){

    @Inject
    lateinit var socketClient: SocketClient
    @Inject
    lateinit var eventSender: SocketEventSender

    private val binder: LocalBinder = LocalBinder()

    inner class LocalBinder: Binder(){
        fun getService(): CallService = this@CallService
    }

    override fun onBind(intent: Intent?): IBinder{
        return binder
    }

}