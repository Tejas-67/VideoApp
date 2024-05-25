package com.tejas.videoapp.util

import com.tejas.videoapp.MessageModel

interface NewMessageInterface {
    fun onNewMessage(message: MessageModel)
}