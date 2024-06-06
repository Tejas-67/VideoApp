package com.tejas.videoapp.datamodel

data class Message(
    val type: Events,
    val name: String? = null,
    val target: String? = null,
    val data:Any?=null
)