package nl.vialer.voip.android.telecom

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import android.util.Log
import nl.vialer.voip.android.BuildConfig

/**
 * A class designed to make using the Android telecom manager easier
 * and less verbose.
 *
 */
internal class AndroidTelecomManager(private val context: Context, private val telecomManager: TelecomManager) {

    private val handle: PhoneAccountHandle by lazy {
        PhoneAccountHandle(ComponentName(context, ConnectionService::class.java), PHONE_ACCOUNT_HANDLE_ID)
    }

    private val phoneAccount: PhoneAccount by lazy {
        PhoneAccount.builder(handle, BuildConfig.LIBRARY_PACKAGE_NAME).setCapabilities(
            PhoneAccount.CAPABILITY_SELF_MANAGED
        ).build()
    }

    @SuppressLint("MissingPermission")
    internal fun placeCall(number: String) {
        telecomManager.registerPhoneAccount(phoneAccount)
Log.e("TEST123", "Placing call...")
        telecomManager.placeCall(
            Uri.fromParts("", number, null),
            Bundle().apply {
                putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, false)
                putInt(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE, VideoProfile.STATE_AUDIO_ONLY)
                putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
            }
        )
    }

    internal fun addNewIncomingCall() {
        telecomManager.addNewIncomingCall(handle, Bundle())
    }


    companion object {
        private const val PHONE_ACCOUNT_HANDLE_ID = BuildConfig.LIBRARY_PACKAGE_NAME
    }
}