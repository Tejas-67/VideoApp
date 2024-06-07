package com.tejas.videoapp.webrtc

import org.webrtc.MediaStream

interface LocalStreamListener {
    fun onLocalStreamReady(mediaStream: MediaStream)
}