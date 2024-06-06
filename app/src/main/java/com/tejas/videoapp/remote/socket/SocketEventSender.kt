package com.tejas.videoapp.remote.socket

import com.tejas.videoapp.App
import com.tejas.videoapp.datamodel.Events
import com.tejas.videoapp.datamodel.Message
import javax.inject.Inject

class SocketEventSender @Inject constructor(
    private val socketClient: SocketClient
) {
    private val username = App.username

    fun storeUser(){
        socketClient.sendMessageToSocket(
            Message(
                type = Events.StoreUser,
                name = username
            )
        )
    }
    fun createRoom(roomId: String){
        socketClient.sendMessageToSocket(
            Message(
                type = Events.CreateRoom,
                data = roomId,
                name = username
            )
        )
    }
    fun joinRoom(roomId: String){
        socketClient.sendMessageToSocket(
            Message(
                type = Events.JoinRoom,
                data = roomId,
                name = username
            )
        )
    }
    fun leaveRoom(roomId: String){
        socketClient.sendMessageToSocket(
            Message(
                type = Events.LeaveRoom,
                data = roomId,
                name = username
            )
        )
    }
    fun leaveAllRooms(){
        socketClient.sendMessageToSocket(
            Message(
                type = Events.LeaveAllRooms,
                name = username
            )
        )
    }
    fun startCall(target: String){
        socketClient.sendMessageToSocket(
            Message(
                type = Events.StartCall,
                name = username,
                target = target
            )
        )
    }
}