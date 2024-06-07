package com.tejas.videoapp.webrtc

import com.tejas.videoapp.datamodel.Message

interface WebRTCSignalListener {
    fun onTransferEventToSocket(data: Message)
}