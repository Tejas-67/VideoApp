package com.tejas.videoapp.remote.socket

import android.util.Log
import com.google.gson.Gson
import com.tejas.videoapp.datamodel.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketClient @Inject constructor(
    private val gson: Gson
){

    init{
        CoroutineScope(Dispatchers.IO).launch{
            delay(1000)
            initSocket()
        }
    }

    private var webSocket: WebSocketClient? = null
    private var socketEventListener: SocketEventListener? = null

    private fun initSocket(){
        webSocket = object: WebSocketClient(URI("")){
            override fun onOpen(handshakedata: ServerHandshake?) {
                socketEventListener?.onSocketOpen()
            }

            override fun onMessage(message: String?) {
                runCatching {
                    socketEventListener?.onNewMessage(
                        gson.fromJson(message, Message::class.java)
                    )
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                socketEventListener?.onSocketClose()
            }

            override fun onError(ex: Exception?) {
                Log.w("VideoApp", "Socket connection failed: $ex")
            }
        }
    }

    fun sendMessageToSocket(message: Message){
        webSocket?.send(gson.toJson(message))
    }

    fun setListener(messageListener: SocketEventListener){
        this.socketEventListener = messageListener
    }

    fun onStop(){
        socketEventListener = null
        runCatching {
            webSocket?.closeBlocking()
        }
    }

}