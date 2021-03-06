package org.openvoipalliance.androidphoneintegration.service

import android.annotation.SuppressLint
import android.app.*
import android.app.Notification.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import java.util.*
import org.openvoipalliance.androidphoneintegration.PIL
import org.openvoipalliance.androidphoneintegration.R

import org.openvoipalliance.androidphoneintegration.call.CallDirection
import org.openvoipalliance.androidphoneintegration.call.CallState
import org.openvoipalliance.androidphoneintegration.events.Event
import org.openvoipalliance.androidphoneintegration.events.PILEventListener

internal class VoIPService : Service(), PILEventListener {

    private val pil by lazy { PIL.instance }

    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }
    private val powerManager by lazy { getSystemService(PowerManager::class.java) }

    private var timer: Timer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val isIncomingRinging
        get() = kotlin.run {
            pil.calls.active?.let {
                return@run it.state == CallState.INITIALIZING && it.direction == CallDirection.INBOUND
            }

            false
        }

    private val handler = Handler()

    val callEventLoop = object : Runnable {
        override fun run() {
            if (pil.calls.active != null)
                pil.events.broadcast(Event.CallEvent.CallUpdated(pil.calls.active))
            else
                stopSelf()

            handler.postDelayed(this, REPEAT_MS)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        pil.events.listen(this)

        pil.writeLog("Starting the VoIP Service and creating notification channels")

        createNotificationChannel()
        createIncomingCallsNotificationChannel()

        pil.writeLog("Transitioning to a foreground service")

        startForeground()

        updateNotificationBasedOnCallStatus()

        if (isIncomingRinging) {
            notifyUserOfIncomingCall()
        }

        handler.post(callEventLoop)

        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                createNotification().build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification().build())
        }
    }

    private fun notifyUserOfIncomingCall() {
        pil.writeLog("Notifying the user of an incoming call")
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "Vialer::IncomingCallWakelock"
        )
        wakeLock?.acquire(30000)

        val incomingCallActivity = pil.app.activities.incomingCall ?: return
        val call = pil.calls.active ?: return

        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.flags = Intent.FLAG_ACTIVITY_NO_USER_ACTION or Intent.FLAG_ACTIVITY_NEW_TASK
        intent.setClass(this, incomingCallActivity)
        val pendingIntent = PendingIntent.getActivity(this, 1, intent, 0)

        val notification = Builder(this, incomingCallsChannelId).apply {
            setOngoing(true)
            setContentIntent(pendingIntent)
            setFullScreenIntent(pendingIntent, true)
            setSmallIcon(R.drawable.ic_service)
            setContentTitle(call.remoteNumber)
            setCategory(CATEGORY_CALL)
            setVisibility(VISIBILITY_PUBLIC)
            setContentText(getString(R.string.notification_incoming_context_text))
            setColor(getColor(R.color.notification_background))
            setColorized(true)
            addAction(
                R.drawable.ic_service,
                getString(R.string.notification_answer_action),
                createActionIntent(NotificationButtonReceiver.Action.ANSWER)
            )
            addAction(
                R.drawable.ic_service,
                getString(R.string.notification_decline_action),
                createActionIntent(NotificationButtonReceiver.Action.DECLINE)
            )
        }.build()
        notification.flags = notification.flags or Notification.FLAG_INSISTENT
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun updateNotificationBasedOnCallStatus() {
        if (isIncomingRinging) return

        pil.writeLog("Updating call notification")

        val call = pil.calls.active ?: return

        val notification = createNotification()
            .setContentTitle(call.remoteNumber)
            .setContentText(call.state.name.toLowerCase(Locale.ROOT))
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createActionIntent(action: NotificationButtonReceiver.Action) = PendingIntent.getBroadcast(
        this,
        0,
        Intent(application, NotificationButtonReceiver::class.java).apply {
            setAction(action.name)
        },
        0
    )

    private fun createNotification(): NotificationCompat.Builder {
        val notificationIntent = Intent(this, pil.app.activities.call)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            0
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentTitle(getString(R.string.notification_default_title))
            .setContentText(getString(R.string.notification_default_subtitle))
            .setSmallIcon(R.drawable.ic_service)
            .setContentIntent(pendingIntent)
            .setShowWhen(false)
            .setColorized(true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setColor(getColor(R.color.notification_background))
            .addAction(
                R.drawable.ic_service,
                getString(R.string.notification_hang_up_action),
                createActionIntent(
                    NotificationButtonReceiver.Action.HANG_UP
                )
            )
    }

    private fun createNotificationChannel() {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_MIN
            )
        )
    }

    private val incomingCallsChannelId
        get() = if (pil.preferences.useApplicationProvidedRingtone) INCOMING_CALLS_APP_RING_CHANNEL_ID else INCOMING_CALLS_CHANNEL_ID

    private val ringtone: Uri
        get() = if (pil.preferences.useApplicationProvidedRingtone) {
            Uri.parse("android.resource://${pil.app.application.packageName}/raw/ringtone")
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        }

    @SuppressLint("WrongConstant")
    private fun createIncomingCallsNotificationChannel() {
        val channel = NotificationChannel(
            incomingCallsChannelId,
            getString(R.string.notification_incoming_calls_channel_name),
            NotificationManager.IMPORTANCE_MAX
        ).apply {
            setSound(
                ringtone,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }

        notificationManager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        pil.writeLog("Stopping VoIPService")

        isRunning = false
        timer?.cancel()
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }

            wakeLock = null
        }

        pil.events.stopListening(this)
    }

    override fun onEvent(event: Event) {
        updateNotificationBasedOnCallStatus()
    }

    override fun onBind(intent: Intent): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 341
        const val CHANNEL_ID = "VoIP"
        const val INCOMING_CALLS_CHANNEL_ID = "VoIP Incoming Calls"
        const val INCOMING_CALLS_APP_RING_CHANNEL_ID = "VoIP Incoming Calls (App Ring)"
        const val REPEAT_MS = 500L

        internal var isRunning = false
    }
}

fun Context.startVoipService() {
    startForegroundService(Intent(this, VoIPService::class.java))
}

fun Context.stopVoipService() {
    stopService((Intent(this, VoIPService::class.java)))
}

fun Context.startCallActivity() {
    if (!PIL.instance.app.automaticallyStartCallActivity) return

    PIL.instance.app.application.startActivity(
        Intent(PIL.instance.app.application, PIL.instance.app.activities.call).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK
        }
    )
}
