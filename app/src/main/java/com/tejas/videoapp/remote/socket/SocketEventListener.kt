package com.tejas.videoapp.remote.socket

import com.tejas.videoapp.datamodel.Message

interface SocketEventListener{

    fun onNewMessage(message: Message)
    fun onSocketOpen()
    fun onSocketClose()

}