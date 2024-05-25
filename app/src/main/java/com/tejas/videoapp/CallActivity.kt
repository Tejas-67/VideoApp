package com.tejas.videoapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.tejas.videoapp.databinding.ActivityCallBinding
import com.tejas.videoapp.util.NewMessageInterface
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription

class CallActivity : AppCompatActivity(), NewMessageInterface {
    private var _binding: ActivityCallBinding? = null
    private val binding get() = _binding!!
    lateinit var username: String
    private var socketRepository: SocketRepository? = null
    private var rtcClient: RTCClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initSocket()

    }

    private fun initSocket(){
        username = intent.getStringExtra("username")?:""
        socketRepository = SocketRepository(this)
        socketRepository?.initSocket(username)
        rtcClient = RTCClient(application, username, socketRepository!! , object: PeerConnectionObserver(){
            override fun onIceCandidate(p0: IceCandidate?) {
                super.onIceCandidate(p0)
            }

            override fun onAddStream(p0: MediaStream?) {
                super.onAddStream(p0)
            }
        })

        binding.apply{
            callBtn.setOnClickListener{
                socketRepository?.sendMessageToSocket(
                    MessageModel(
                        type = "start_call",
                        name = username,
                        target = binding.targetUserNameEt.text.toString(),
                        data = null
                    )
                )
            }
        }
    }

    override fun onNewMessage(message: MessageModel) {
        Log.w("video-app", message.toString())
        when(message.type){
            "call_response" -> {
                if(message.data=="user is not online"){
                    runOnUiThread {
                        Toast.makeText(this, message.data as String, Toast.LENGTH_LONG).show()
                    }
                }else{
                    runOnUiThread{
                        Log.w("video-app", "here")
                        hideWhoToCallLayout()
                        showCallLayout()
                        binding.apply {
                            rtcClient?.initializeSurfaceView(localView)
                            rtcClient?.initializeSurfaceView(remoteView)
                            rtcClient?.startLocalVideo(localView)
                            rtcClient?.call(targetUserNameEt.text.toString())
                        }
                    }
                }
            }
            "offer_received" -> {
                runOnUiThread {
                    showIncomingCallLayout()
                    binding.incomingNameTV.text = "${message.name} is calling you"
                    binding.acceptButton.setOnClickListener {
                        hideIncomingCallLayout()
                        showCallLayout()
                        hideWhoToCallLayout()
                        binding.apply {
                            rtcClient?.initializeSurfaceView(localView)
                            rtcClient?.initializeSurfaceView(remoteView)
                            rtcClient?.startLocalVideo(localView)
                        }
                        val session = SessionDescription(
                            SessionDescription.Type.OFFER,
                            message.data.toString()
                        )
                        rtcClient?.onRemoteSessionReceived(session)
                        rtcClient?.answer(message.name)
                    }
                    binding.rejectButton.setOnClickListener {
                        hideIncomingCallLayout()
                    }
                }
            }
            else -> {}
        }
    }
    private fun hideIncomingCallLayout(){
        binding.incomingCallLayout.visibility = View.GONE
    }
    private fun showIncomingCallLayout(){
        binding.incomingCallLayout.visibility = View.VISIBLE
    }
    private fun hideWhoToCallLayout(){
        binding.whoToCallLayout.visibility = View.GONE
    }
    private fun showWhoToCallLayout(){
        binding.whoToCallLayout.visibility = View.VISIBLE
    }
    private fun hideCallLayout(){
        binding.callLayout.visibility = View.GONE
    }
    private fun showCallLayout(){
        binding.callLayout.visibility = View.VISIBLE
    }
}