package com.tejas.videoapp

import android.util.Log
import com.google.gson.Gson
import com.tejas.videoapp.util.NewMessageInterface
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI

class SocketRepository(
    val messageInterface: NewMessageInterface
){
    private var webSocket: WebSocketClient? = null
    private var username: String? = null
    val BASE_URL = "ws://192.168.123.205:3000"
    val BASE_URL_R = "ws://10.0.2.2:3000"

    fun initSocket(username: String){
        this.username = username
        webSocket = object: WebSocketClient(URI(BASE_URL)) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                sendMessageToSocket(
                    MessageModel(
                        type = "store_user",
                        name = username,
                        target = null,
                        data = null
                    )
                )
            }

            override fun onMessage(message: String?) {
                messageInterface.onNewMessage(
                    Gson().fromJson(message, MessageModel::class.java)
                )
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.w("socket", "repo onClose: $reason")
            }

            override fun onError(ex: Exception?) {
                Log.w("socket", "repo onError: $ex")
            }
        }
        webSocket?.connect()
    }

    fun sendMessageToSocket(message: MessageModel){
        try{
            webSocket?.send(Gson().toJson(message))
        }catch(e: Exception){
            e.printStackTrace()
        }
    }
}