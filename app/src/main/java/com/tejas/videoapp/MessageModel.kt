package com.tejas.videoapp

import com.google.gson.annotations.SerializedName

data class MessageModel(
    @SerializedName("type") val type: String,
    @SerializedName("name") val name: String? = null,
    @SerializedName("target") val target: String? = null,
    @SerializedName("data") val data: Any? = null
)
