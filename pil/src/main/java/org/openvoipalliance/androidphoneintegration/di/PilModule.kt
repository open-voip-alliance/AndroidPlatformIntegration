package org.openvoipalliance.androidphoneintegration.di

import android.telecom.TelecomManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.openvoipalliance.androidphoneintegration.call.CallManager
import org.openvoipalliance.androidphoneintegration.call.Calls
import org.openvoipalliance.androidphoneintegration.PIL
import org.openvoipalliance.androidphoneintegration.helpers.VoIPLibHelper
import org.openvoipalliance.androidphoneintegration.audio.AudioManager
import org.openvoipalliance.androidphoneintegration.call.CallActions
import org.openvoipalliance.androidphoneintegration.call.PILCallFactory
import org.openvoipalliance.androidphoneintegration.contacts.Contacts
import org.openvoipalliance.androidphoneintegration.events.EventsManager
import org.openvoipalliance.androidphoneintegration.telecom.AndroidCallFramework
import org.openvoipalliance.androidphoneintegration.telecom.Connection
import org.openvoipalliance.voiplib.VoIPLib

fun getModules() = listOf(pilModule)

val pilModule = module {

    single {
        AndroidCallFramework(
            androidContext(),
            androidContext().getSystemService(TelecomManager::class.java)
        )
    }

    single { PILCallFactory(get(), get()) }

    single { Contacts(androidContext()) }

    single { CallManager(get(), get()) }

    single { PIL.instance }

    single { VoIPLib.getInstance(androidContext()) }

    single { CallActions(get(), get(), get(), get()) }

    single { AudioManager(get(), get()) }

    single { EventsManager(get()) }

    single { VoIPLibHelper(get(), get(), get()) }

    factory { Connection(get(), get(), get(), get()) }

    factory { Calls(get(), get()) }
}