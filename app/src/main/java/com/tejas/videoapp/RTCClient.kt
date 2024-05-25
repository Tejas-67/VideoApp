package com.tejas.videoapp

import android.app.Application
import android.util.Log
import org.webrtc.Camera2Enumerator
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import java.lang.IllegalStateException

class RTCClient(
    val app: Application,
    val username: String,
    val socketRepository: SocketRepository,
    val observer: PeerConnection.Observer
) {
    private val eglContext = EglBase.create()
    private val peerConnectionFactory by lazy {createPeerConnectionFactory()}
    private val iceServer = listOf(
        PeerConnection.IceServer.builder("stun:iphone-stun.strato-iphone.de:3478").createIceServer()
    )
    private val peerConnection by lazy { createPeerConnection(observer) }
    private val localVideoSource by lazy {
        peerConnectionFactory.createVideoSource(false)
    }
    private val localAudioSource by lazy {
        peerConnectionFactory.createAudioSource(MediaConstraints())
    }
    init{
        initPeerConnectionFactory(app)
    }

    private fun initPeerConnectionFactory(application: Application){
        val peerConnectionOption = PeerConnectionFactory.InitializationOptions.builder(application)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(peerConnectionOption)
    }

    private fun createPeerConnectionFactory(): PeerConnectionFactory{
        return PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglContext.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglContext.eglBaseContext))
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption = true
                disableNetworkMonitor = true
            }).createPeerConnectionFactory()
    }

    private fun createPeerConnection(observer: PeerConnection.Observer): PeerConnection?{
        return peerConnectionFactory.createPeerConnection(iceServer, observer)
    }

    fun initializeSurfaceView(surface: SurfaceViewRenderer){
        surface.run{
            setEnableHardwareScaler(true)
            setMirror(true)
            init(eglContext.eglBaseContext, null)
        }
    }

    fun startLocalVideo(surface: SurfaceViewRenderer){
        val surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().name, eglContext.eglBaseContext)
        val videoCapturer = getVideoCapturer(app)
        videoCapturer.initialize(surfaceTextureHelper, surface.context, localVideoSource.capturerObserver)
        videoCapturer.startCapture(320, 240, 30)
        val videoTrack = peerConnectionFactory.createVideoTrack("local_video_track", localVideoSource)
        videoTrack.addSink(surface)
        val audioTrack = peerConnectionFactory.createAudioTrack("local_audio_track", localAudioSource)
        val localStream = peerConnectionFactory.createLocalMediaStream("local_stream")
        localStream.addTrack(audioTrack)
        localStream.addTrack(videoTrack)
        peerConnection?.addStream(localStream)
    }

    private fun getVideoCapturer(application: Application): VideoCapturer{
        return Camera2Enumerator(application).run {
            deviceNames.find {
                isFrontFacing(it)
            }?.let{
                createCapturer(it, null)
            }?: throw IllegalStateException()
        }
    }

    fun call(target: String) {
        val mediaConstraints = MediaConstraints()
        mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"))
        peerConnection?.createOffer(object: SdpObserver{
            override fun onCreateSuccess(desc: SessionDescription?) {
                Log.w("video-app", "CO Succ")
                peerConnection?.setLocalDescription(object: SdpObserver{
                    override fun onCreateSuccess(p0: SessionDescription?) {

                    }
                    override fun onSetSuccess() {
                        Log.w("video-app", "LD Succ")
                        val offerObject = hashMapOf(
                            "sdp" to desc?.description,
                            "type" to desc?.type
                        )
                        socketRepository.sendMessageToSocket(MessageModel(
                            type = "create_offer",
                            name = username,
                            target = target,
                            offerObject
                        ))
                        Log.w("video-app", "LD Succ E")
                    }

                    override fun onCreateFailure(p0: String?) {
                    }

                    override fun onSetFailure(p0: String?) {
                    }

                }, desc)
            }

            override fun onSetSuccess() {
            }

            override fun onCreateFailure(p0: String?) {
            }

            override fun onSetFailure(p0: String?) {
            }

        }, mediaConstraints)
    }

    fun onRemoteSessionReceived(session: SessionDescription){
        peerConnection?.setRemoteDescription(object:  SdpObserver{
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, session)
    }

    fun answer(target: String?) {
        val constraints = MediaConstraints()
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))

        peerConnection?.createAnswer(object: SdpObserver{
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection?.setLocalDescription(object: SdpObserver{
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        val answer = hashMapOf(
                            "sdp" to desc?.description,
                            "type" to desc?.type
                        )
                        socketRepository.sendMessageToSocket(MessageModel(
                            type = "create_answer", name = username, target = target, data = answer
                        ))
                    }
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                }, desc)
            }

            override fun onSetSuccess() {}

            override fun onCreateFailure(p0: String?) {}

            override fun onSetFailure(p0: String?) {}

        },constraints)
    }
}