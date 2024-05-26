package com.tejas.videoapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.gson.Gson
import com.tejas.videoapp.databinding.ActivityCallBinding
import com.tejas.videoapp.util.NewMessageInterface
import com.tejas.videoapp.util.RTCAudioManager
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.SessionDescription

class CallActivity : AppCompatActivity(), NewMessageInterface {
    private var _binding: ActivityCallBinding? = null
    private val binding get() = _binding!!
    lateinit var username: String
    private var socketRepository: SocketRepository? = null
    private var rtcClient: RTCClient? = null
    private var target = ""
    private var isMute = false
    private var isCameraPause = false
    private val rtcAudioManager by lazy { RTCAudioManager.create(this) }
    private var isSpeakerMode = true

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
                rtcClient?.addIceCandidate(p0)
                val candidate = hashMapOf(
                    "sdpMid" to p0?.sdpMid,
                    "sdpMLineIndex" to p0?.sdpMLineIndex,
                    "sdpCandidate" to p0?.sdp
                )
                socketRepository?.sendMessageToSocket(
                    MessageModel(
                        type = "ice_candidate",
                        name = username,
                        target = target,
                        data = candidate
                    )
                )
            }

            override fun onAddStream(p0: MediaStream?) {
                super.onAddStream(p0)

                p0?.videoTracks?.get(0)?.addSink(binding.remoteView)
                Log.w("video-app", "onAddStream: $p0")
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
                target = binding.targetUserNameEt.text.toString()
            }
            switchCameraButton.setOnClickListener {
                rtcClient?.switchCamera()
            }
            micButton.setOnClickListener {
                if(isMute){
                    isMute = false
                    micButton.setImageResource(R.drawable.ic_baseline_mic_off_24)
                }else{
                    isMute = true
                    micButton.setImageResource(R.drawable.ic_baseline_mic_24)
                }
                rtcClient?.toggleAudio(isMute)
            }

            videoButton.setOnClickListener {
                if(isCameraPause){
                    isCameraPause = false
                    videoButton.setImageResource(R.drawable.ic_baseline_videocam_off_24)
                }else{
                    isCameraPause = true
                    videoButton.setImageResource(R.drawable.ic_baseline_videocam_24)
                }
                rtcClient?.toggleCamera(isCameraPause)
            }

            audioOutputButton.setOnClickListener {
                if(isSpeakerMode){
                    isSpeakerMode = false
                    audioOutputButton.setImageResource(R.drawable.ic_baseline_hearing_24)
                    rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.EARPIECE)
                }else{
                    isSpeakerMode = true
                    audioOutputButton.setImageResource(R.drawable.ic_baseline_speaker_up_24)
                    rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)
                }
            }

            endCallButton.setOnClickListener {
                hideCallLayout()
                showWhoToCallLayout()
                hideIncomingCallLayout()
                rtcClient?.endCall()
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
                    target = message.name!!
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
                        binding.remoteViewLoading.visibility = View.GONE
                    }
                    binding.rejectButton.setOnClickListener {
                        hideIncomingCallLayout()
                    }
                }
            }
            "answer_received" -> {
                val session = SessionDescription(
                    SessionDescription.Type.ANSWER,
                    message.data.toString()
                )
                rtcClient?.onRemoteSessionReceived(session)
                runOnUiThread {
                    binding.remoteViewLoading.visibility = View.GONE
                }
            }
            "ice_candidate" -> {
                runOnUiThread {
                    try{
                        val receivingCandidate = Gson().fromJson(
                            Gson().toJson(message.data), IceCandidateModel::class.java
                        )
                        rtcClient?.addIceCandidate(IceCandidate(receivingCandidate.sdpMid,
                            Math.toIntExact(receivingCandidate.sdpMLineIndex.toLong()),
                            receivingCandidate.sdpCandidate))
                    }catch(e: Exception){
                        e.printStackTrace()
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