package com.tejas.videoapp.ui.viewmodel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.tejas.videoapp.datamodel.RoomModel
import com.tejas.videoapp.service.CallService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
    private val context: Context,
    private val gson: Gson
): ViewModel() {

    var roomState: MutableStateFlow<List<RoomModel>?> = MutableStateFlow(null)
    private var callService: CallService? = null
    private var isBound = false

    private val serviceConnection = object: ServiceConnection{
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as CallService.LocalBinder
            callService = binder.getService()
            isBound = true
            handleServiceBound()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }

        override fun onBindingDied(name: ComponentName?) {
            super.onBindingDied(name)
        }

        override fun onNullBinding(name: ComponentName?) {
            super.onNullBinding(name)
        }
    }

    private fun handleServiceBound() {
        callService?.roomState?.onEach { rooms ->
            roomState.emit(rooms)
        }?.launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        if(isBound) {
            context.unbindService(serviceConnection)
            isBound = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun init(){
        Intent(context, CallService::class.java).apply {
            CallService.startService(context)
            context.bindService(this, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

}
