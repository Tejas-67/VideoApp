package com.tejas.videoapp

import android.app.Application

class RTCClient(
    private val app: Application,
    private val username: String,
    private val socketRepository: SocketRepository
) {

    private fun initPeerConnectionFactory(application: Application){
        val peerConnectionOption = ""
    }
}