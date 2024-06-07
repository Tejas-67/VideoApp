package com.tejas.videoapp.webrtc

import com.google.gson.Gson
import com.tejas.videoapp.datamodel.Events
import com.tejas.videoapp.datamodel.Message
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription

class RTCClientImpl(
    private val connection: PeerConnection,
    private val username: String,
    private val target: String,
    private val gson: Gson,
    private var listener: WebRTCSignalListener? = null,
    private val destroyClient: () -> Unit
) : RTCClient {

    private val mediaConstraint = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
    }

    override val peerConnection = connection
    override fun call() {
        peerConnection.createOffer(object : MySdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                super.onCreateSuccess(desc)
                peerConnection.setLocalDescription(object : MySdpObserver() {
                    override fun onSetSuccess() {
                        super.onSetSuccess()
                        listener?.onTransferEventToSocket(
                            Message(
                                type = Events.Offer, name = username, target = target,
                                data = desc?.description
                            )
                        )
                    }
                }, desc)
            }
        }, mediaConstraint)
    }

    override fun answer() {
        peerConnection.createAnswer(object : MySdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                super.onCreateSuccess(desc)
                peerConnection.setLocalDescription(object : MySdpObserver() {
                    override fun onSetSuccess() {
                        super.onSetSuccess()
                        listener?.onTransferEventToSocket(
                            Message(
                                type = Events.Answer,
                                name = username,
                                target = target,
                                data = desc?.description
                            )
                        )
                    }
                }, desc)
            }
        }, mediaConstraint)
    }

    override fun onRemoteSessionReceived(sessionDescription: SessionDescription) {
        peerConnection.setRemoteDescription(MySdpObserver(), sessionDescription)
    }

    override fun addIceCandidateToPeer(iceCandidate: IceCandidate) {
        peerConnection.addIceCandidate(iceCandidate)
    }

    override fun sendIceCandidateToPeer(candidate: IceCandidate, target: String) {
        addIceCandidateToPeer(candidate)
        listener?.onTransferEventToSocket(
            Message(
                type = Events.Ice,
                name = username,
                target = target,
                data = gson.toJson(candidate)
            )
        )
    }

    override fun onDestroy() {
        connection.close()
        destroyClient()
    }
}