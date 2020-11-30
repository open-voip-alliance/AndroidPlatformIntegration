package nl.vialer.voip.android.example.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.fragment_dialer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import nl.vialer.voip.android.R
import nl.vialer.voip.android.VoIPPIL
import nl.vialer.voip.android.configuration.Auth
import nl.vialer.voip.android.events.Event
import nl.vialer.voip.android.events.Event.*
import nl.vialer.voip.android.events.EventListener
import nl.vialer.voip.android.example.ui.Dialer
import nl.vialer.voip.android.example.ui.TransferDialog
import nl.vialer.voip.android.example.ui.call.CallActivity

class DialerFragment : Fragment(), EventListener {

    private val prefs by lazy {
        PreferenceManager.getDefaultSharedPreferences(activity)
    }

    private val voip by lazy {
        VoIPPIL.instance
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_dialer, container, false)
        return root
    }

    override fun onResume() {
        super.onResume()
        voip.events.listen(this)
        requestCallingPermissions()
    }

    override fun onPause() {
        super.onPause()
        voip.events.stopListening(this)
    }

    override fun onEvent(event: Event) {
    }

    private fun requestCallingPermissions() {
        val requiredPermissions = arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_CONTACTS, Manifest.permission.READ_PHONE_STATE)

        requiredPermissions.forEach { permission ->
            if (ContextCompat.checkSelfPermission(requireActivity(), permission) == PERMISSION_DENIED) {
                requireActivity().requestPermissions(requiredPermissions, 101)
                return
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialer.onCallListener = Dialer.OnCallListener { number ->
            voip.call(number)
        }
    }
}