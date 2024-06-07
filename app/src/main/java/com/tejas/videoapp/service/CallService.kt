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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tejas.videoapp.App
import com.tejas.videoapp.MainActivity
import com.tejas.videoapp.R
import com.tejas.videoapp.datamodel.Events
import com.tejas.videoapp.datamodel.Message
import com.tejas.videoapp.datamodel.RoomModel
import com.tejas.videoapp.remote.socket.SocketClient
import com.tejas.videoapp.remote.socket.SocketEventListener
import com.tejas.videoapp.remote.socket.SocketEventSender
import com.tejas.videoapp.webrtc.LocalStreamListener
import com.tejas.videoapp.webrtc.PeerObserver
import com.tejas.videoapp.webrtc.RTCClient
import com.tejas.videoapp.webrtc.WebRTCFactory
import com.tejas.videoapp.webrtc.WebRTCSignalListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject
@AndroidEntryPoint
class CallService : Service(), SocketEventListener, WebRTCSignalListener {

    @Inject
    lateinit var socketClient: SocketClient

    @Inject
    lateinit var eventSender: SocketEventSender

    @Inject
    lateinit var gson: Gson

    @Inject
    lateinit var webRTCFactory: WebRTCFactory


    //service section
    private lateinit var mainNotification: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManager

    //state
    val roomsState: MutableStateFlow<List<RoomModel>?> = MutableStateFlow(null)
    val mediaStreamsState: MutableStateFlow<HashMap<String, MediaStream>> = MutableStateFlow(
        hashMapOf()
    )

    private fun getMediaStreams() = mediaStreamsState.value
    fun addMediaStreamToState(username: String, mediaStream: MediaStream) {
        val updatedData = HashMap(getMediaStreams()).apply {
            put(username, mediaStream)
        }
        mediaStreamsState.value = updatedData
    }

    fun removeMediaStreamFromState(username: String) {
        val updatedData = HashMap(getMediaStreams()).apply {
            remove(username)
        }
        // Update the state with the new HashMap
        mediaStreamsState.value = updatedData
    }

    //connection list
    private val connections: MutableMap<String, RTCClient> = mutableMapOf()


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                CallServiceActions.START.name -> handleStartService()
                CallServiceActions.STOP.name -> handleStopService()
                else -> Unit
            }
        }
        return START_STICKY
    }

    private fun handleStartService() {
        if (!isServiceRunning) {
            isServiceRunning = true
            //start service here
            startServiceWithNotification()
        }
    }

    private fun handleStopService() {
        if (isServiceRunning) {
            isServiceRunning = false
        }
        socketClient.onStop()
        connections.onEach {
            runCatching {
                it.value.onDestroy()
            }
        }
        webRTCFactory.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
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

    override fun onNewMessage(message: Message) {
        when (message.type) {
            Events.RoomStatus -> handleRoomStatus(message)
            Events.NewSession -> handleNewSession(message)
            Events.StartCall -> handleStartCall(message)
            Events.Offer -> handleOffer(message)
            Events.Answer -> handleAnswer(message)
            Events.Ice -> handleIceCandidates(message)
            else -> Unit
        }
    }

    override fun onSocketOpen() {
        eventSender.storeUser()
    }

    override fun onSocketClose() {

    }

    fun initializeSurface(view: SurfaceViewRenderer) {
        webRTCFactory.init(view, object : LocalStreamListener {
            override fun onLocalStreamReady(mediaStream: MediaStream) {
                addMediaStreamToState(App.username, mediaStream)
            }
        })
    }

    private fun handleNewSession(message: Message) {
        message.name?.let { target ->
            startNewConnection(target) {
                eventSender.startCall(target)
            }
        }
    }

    private fun handleStartCall(message: Message) {
        //we create new connection here
        startNewConnection(message.name!!) {
            it.call()
        }
    }

    private fun startNewConnection(targetName: String, done: (RTCClient) -> Unit) {
        webRTCFactory.createRtcClient(object : PeerObserver() {
            override fun onIceCandidate(p0: IceCandidate?) {
                super.onIceCandidate(p0)
                findClient(targetName)?.let {
                    if (p0 != null) {
                        it.sendIceCandidateToPeer(p0, targetName)
                    }
                }
            }

            override fun onAddStream(p0: MediaStream?) {
                super.onAddStream(p0)
                p0?.let {
                    addMediaStreamToState(targetName, it)
                }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                super.onConnectionChange(newState)
                if (
                    newState == PeerConnection.PeerConnectionState.CLOSED ||
                    newState == PeerConnection.PeerConnectionState.DISCONNECTED ||
                    newState == PeerConnection.PeerConnectionState.FAILED
                ) {
                    removeMediaStreamFromState(targetName)
                }
            }
        }, targetName, this).also {
            it?.let {
                connections[targetName] = it
                CoroutineScope(Dispatchers.IO).launch {
                    delay(1000)
                    done(it)
                }
            }
        }
    }

    private fun handleOffer(message: Message) {
        findClient(message.name!!)?.let {
            it.onRemoteSessionReceived(
                SessionDescription(
                    SessionDescription.Type.OFFER,
                    message.data.toString()
                )
            )
            it.answer()
        }
    }

    private fun handleAnswer(message: Message) {
        findClient(message.name!!).apply {
            this?.onRemoteSessionReceived(
                SessionDescription(
                    SessionDescription.Type.ANSWER,
                    message.data.toString()
                )
            )
        }
    }

    private fun handleIceCandidates(message: Message) {
        val ice = runCatching {
            gson.fromJson(message.data.toString(), IceCandidate::class.java)
        }
        ice.onSuccess {
            findClient(message.name!!).apply {
                this?.addIceCandidateToPeer(it)
            }
        }
    }

    fun leaveRoom(){
        connections.onEach {
            it.value.onDestroy()
        }
    }

    private fun findClient(username: String): RTCClient? {
        return connections[username]
    }


    private fun handleRoomStatus(message: Message) {
        val type = object : TypeToken<List<RoomModel>>() {}.type
        val rooms: List<RoomModel> = gson.fromJson(message.data.toString(), type)

        roomsState.value = rooms
    }


    private fun startServiceWithNotification() {
        startForeground(MAIN_NOTIFICATION_ID, mainNotification.build())
    }

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): CallService = this@CallService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
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

    companion object {
        var isServiceRunning = false
        const val CALL_NOTIFICATION_CHANNEL_ID = "CALL_CHANNEL"
        const val MAIN_NOTIFICATION_ID = 2323
        fun startService(context: Context) {
            Thread {
                startIntent(context, Intent(context, CallService::class.java).apply {
                    action = CallServiceActions.START.name
                })
            }.start()
        }

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

    override fun onTransferEventToSocket(data: Message) {
        socketClient.sendMessageToSocket(data)
    }
}