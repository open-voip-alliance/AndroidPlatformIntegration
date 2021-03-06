package org.openvoipalliance.androidphoneintegration.example.ui.call

import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_call.*
import org.openvoipalliance.androidphoneintegration.CallScreenLifecycleObserver
import org.openvoipalliance.androidphoneintegration.PIL
import org.openvoipalliance.androidphoneintegration.audio.AudioRoute
import org.openvoipalliance.androidphoneintegration.call.PILCall
import org.openvoipalliance.androidphoneintegration.events.Event
import org.openvoipalliance.androidphoneintegration.events.Event.CallEvent
import org.openvoipalliance.androidphoneintegration.events.PILEventListener
import org.openvoipalliance.androidphoneintegration.example.R
import org.openvoipalliance.androidphoneintegration.example.ui.TransferDialog

class CallActivity : AppCompatActivity(), PILEventListener {

    private val pil by lazy { PIL.instance }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)
        lifecycle.addObserver(CallScreenLifecycleObserver(this))

        endCallButton.setOnClickListener {
            pil.actions.end()
        }

        holdButton.setOnClickListener {
            pil.actions.toggleHold()
        }

        muteButton.setOnClickListener {
            pil.audio.toggleMute()
        }

        earpieceButton.setOnClickListener {
            pil.audio.routeAudio(AudioRoute.PHONE)
        }

        speakerButton.setOnClickListener {
            pil.audio.routeAudio(AudioRoute.SPEAKER)
        }

        bluetoothButton.setOnClickListener {
            pil.audio.routeAudio(AudioRoute.BLUETOOTH)
        }

        transferButton.setOnClickListener {
            TransferDialog(this).apply {
                onTransferListener = TransferDialog.OnTransferListener { number ->
                    pil.actions.beginAttendedTransfer(number)
                    dismiss()
                }
                show(supportFragmentManager, "")
            }
        }

        transferMergeButton.setOnClickListener {
            pil.actions.completeAttendedTransfer()
        }

        dtmfButton.setOnClickListener {

            val editText = EditText(this).apply {
                inputType = InputType.TYPE_CLASS_NUMBER
            }

            AlertDialog.Builder(this).apply {
                setView(editText)
                setTitle("Send DTMF to Remote Party")
                setPositiveButton("Send DTMF") { _, _ ->
                    pil.actions.sendDtmf(editText.text.toString())
                }
                setNegativeButton("Cancel") { _, _ ->
                }
            }.show()
        }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    override fun onDestroy() {
        super.onDestroy()
        pil.events.stopListening(this)
    }

    private fun render(call: PILCall? = pil.calls.active) {
        if (call == null) {
            finish()
            return
        }

        if (pil.calls.isInTransfer) {
            transferCallInformation.text = pil.calls.inactive?.remotePartyHeading
            if (pil.calls.inactive?.remotePartySubheading?.isNotBlank() == true) {
                transferCallInformation.text = "${transferCallInformation.text} (${pil.calls.inactive?.remotePartySubheading})"
            }
            transferContainer.visibility = View.VISIBLE
        } else {
            transferContainer.visibility = View.GONE
        }

        callTitle.text = call.remotePartyHeading
        callSubtitle.text = call.remotePartySubheading
        callDuration.text = call.prettyDuration

        holdButton.text = if (call.isOnHold) "unhold" else "hold"
        muteButton.text = if (pil.audio.isMicrophoneMuted) "unmute" else "mute"

        callStatus.text = call.state.name

        callDetailsAdvanced.text = ""

        earpieceButton.isEnabled = pil.audio.state.availableRoutes.contains(AudioRoute.PHONE)
        speakerButton.isEnabled = pil.audio.state.availableRoutes.contains(AudioRoute.SPEAKER)
        bluetoothButton.isEnabled = pil.audio.state.availableRoutes.contains(AudioRoute.BLUETOOTH)

        earpieceButton.setTypeface(null, if (pil.audio.state.currentRoute == AudioRoute.PHONE) Typeface.BOLD else Typeface.NORMAL)
        speakerButton.setTypeface(null, if (pil.audio.state.currentRoute == AudioRoute.SPEAKER) Typeface.BOLD else Typeface.NORMAL)
        bluetoothButton.setTypeface(null, if (pil.audio.state.currentRoute == AudioRoute.BLUETOOTH) Typeface.BOLD else Typeface.NORMAL)
        bluetoothButton.text = pil.audio.state.bluetoothDeviceName ?: "Bluetooth"
    }

    override fun onEvent(event: Event) = when (event) {
        is CallEvent.CallEnded -> {
            if (pil.calls.active == null) {
                finish()
            } else {

                render(event.call)
            }
        }
        is CallEvent.CallUpdated -> render(event.call)
        else -> {}
    }
}
