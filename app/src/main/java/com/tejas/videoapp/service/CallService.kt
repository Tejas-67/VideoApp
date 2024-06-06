package com.tejas.videoapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Binder
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.tejas.videoapp.MainActivity
import com.tejas.videoapp.R
import com.tejas.videoapp.datamodel.Message
import com.tejas.videoapp.remote.socket.SocketClient
import com.tejas.videoapp.remote.socket.SocketEventListener
import com.tejas.videoapp.remote.socket.SocketEventSender
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CallService: Service(), SocketEventListener{

    @Inject
    lateinit var socketClient: SocketClient
    @Inject
    lateinit var eventSender: SocketEventSender

    private lateinit var mainNotification: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManager

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let{
            when(it.action){
                CallServiceActions.START.name -> handleStartService()
                CallServiceActions.STOP.name -> handleStopService()
                else -> {}
            }
        }
        return START_STICKY
    }

    private fun handleStartService(){
        if(!isServiceRunning){
            isServiceRunning = true
            startServiceWithNotification()
        }
    }

    private fun startServiceWithNotification() {
        startForeground(MAIN_NOTIFICATION_ID, mainNotification.build())
    }

    private fun handleStopService(){
        if(isServiceRunning){
            isServiceRunning = false
            socketClient.onStop()
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(
            NotificationManager::class.java
        )
        createNotifications()
        socketClient.setListener(this)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotifications() {
        val callChannel = NotificationChannel(
            CALL_NOTIFICATION_CHANNEL_ID,
            CALL_NOTIFICATION_CHANNEL_ID,
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(callChannel)
        val contentIntent = Intent(
            this, MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE
        )


        val notificationChannel = NotificationChannel(
            "chanel_terminal_bluetooth",
            "chanel_terminal_bluetooth",
            NotificationManager.IMPORTANCE_HIGH
        )


        val intent = Intent(this, CallBroadcastReceiver::class.java).apply {
            action = "ACTION_EXIT"
        }
        val pendingIntent: PendingIntent =
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        notificationManager.createNotificationChannel(notificationChannel)

        mainNotification = NotificationCompat.Builder(
            this, "chanel_terminal_bluetooth"
        ).setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(false)
            .addAction(R.mipmap.ic_launcher, "Exit", pendingIntent)
            .setContentIntent(contentPendingIntent)
    }

    private val binder: LocalBinder = LocalBinder()

    inner class LocalBinder: Binder(){
        fun getService(): CallService = this@CallService
    }

    override fun onBind(intent: Intent?): IBinder{
        return binder
    }

    override fun onNewMessage(message: Message) {

    }

    override fun onSocketOpen() {
        eventSender.storeUser()
    }

    override fun onSocketClose() {

    }

    companion object {
        var isServiceRunning = false
        const val CALL_NOTIFICATION_CHANNEL_ID = "CALL_CHANNEL"
        const val MAIN_NOTIFICATION_ID = 2323
        @RequiresApi(Build.VERSION_CODES.O)
        fun startService(context: Context) {
            Thread {
                startIntent(context, Intent(context, CallService::class.java).apply {
                    action = CallServiceActions.START.name
                })
            }.start()
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun stopService(context: Context) {
            startIntent(context, Intent(context, CallService::class.java).apply {
                action = CallServiceActions.STOP.name
            })
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun startIntent(context: Context, intent: Intent) {
            context.startForegroundService(intent)
        }
    }
}