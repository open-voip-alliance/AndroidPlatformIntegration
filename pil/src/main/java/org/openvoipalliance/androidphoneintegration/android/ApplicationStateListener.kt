package org.openvoipalliance.androidphoneintegration

import android.app.Activity
import com.takwolf.android.foreback.Foreback
import org.openvoipalliance.androidphoneintegration.call.CallState
import org.openvoipalliance.androidphoneintegration.service.VoIPService
import org.openvoipalliance.androidphoneintegration.service.startCallActivity

internal class ApplicationStateListener(private val pil: PIL) : Foreback.Listener {

    override fun onApplicationEnterForeground(activity: Activity?) {
        pil.writeLog("Application has entered the foreground")

        if (VoIPService.isRunning && pil.app.automaticallyStartCallActivity) {
            if (pil.calls.active?.state == CallState.CONNECTED) {
                pil.app.application.startCallActivity()
            }
        }

        try {
            pil.start()
        } catch (e: Exception) {
            pil.writeLog("Unable to start PIL when entering foreground")
        }
    }

    override fun onApplicationEnterBackground(activity: Activity?) {
        pil.writeLog("Application has entered the background")
    }
}
