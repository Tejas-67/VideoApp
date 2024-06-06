package com.tejas.videoapp.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.tejas.videoapp.CloseActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CallBroadcastReceiver : BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.action?.let { action->
            if (action=="ACTION_EXIT"){
                context?.let { noneNullContext ->
                    CallService.stopService(noneNullContext)
                    noneNullContext.startActivity(Intent(noneNullContext, CloseActivity::class.java)
                        .apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                }
            }
        }
    }
}