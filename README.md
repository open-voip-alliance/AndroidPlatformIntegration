## What is this?

This is a library that handles the challenges involved in integrating VoIP functionality into the Android platform (hence Platform Integration Layer, or PIL for short).

### Does this library implement VoIP functionality?

No, it relies on [AndroidPhoneLib](https://github.com/open-voip-alliance/AndroidPhoneLib). A library that we also maintain which currently uses Linphone as the underlying SIP technology.

### Tasks handled by the PIL

- Starting and stopping the VoIP layer based on application state
- Managing registrations
- Managing call objects
- Running a foreground service
- Call notifications
- Incoming call notifications
- Responding to user input on notifications
- Integration with the Android Telecom Framework
- Audio Routing
- Bluetooth calling
- Responding to bluetooth headset input
- Fetching contact data
- Displaying activities appropriately

### Tasks the application must implement

- Implement an activity to show an active call
- Implement an activity to show an incoming call
- [Optional] Implement a middleware class if required by your VoIP architecture

## Example Application

This repo contains an example application with implementations of the basic functionality of the library, please check that if there are any questions not answered by this document.

## Getting Started

In your [Application Class](https://developer.android.com/reference/android/app/Application) under the onCreate method, you must start the PIL:

```kotlin
val pil = startAndroidPIL {
	ApplicationSetup (
  	applicationClass = this@Application
    ...
  )
}
```

The ApplicationSetup class takes a number of parameters, these are all Application level options that provide the PIL with some information about how it can manage VoIP in your application. These parameters are expected to be static and won't change after your application is created.

Mandatory parameters:

- applicationClass = The context of your application, this will be used when we need a Context and also for application lifecycle tracking.
- activities = You must provide a CallActivity and an IncomingCallActivity, the user will be directed to these when interacting with a notification. The CallActivity will also be launched when appropriate unless you set automaticallyStartCallActivity to FALSE.

Optional parameters:

- logger = Receive logs from the PIL and the underlying VoIP library
- middleware = If your VoIP architecture uses a middleware, in that you use FCM notifications to wake the phone, you must provide an implementation of Middleware. While this isn't required, incoming calls will not work in the background without it.

It is possible to get an instance of the PIL at any point:

```kotlin
val pil = PIL.instance
```

### Authentication

To actually authenticate and make/receive calls you must authenticate your VoIP account, this is done by providing an Authentication objecto the PIL instance. 

```kotlin
pil.auth = Auth(username = "", password = "" ...)
```

Where you decide to place this depends on the structure of your application and how the authentication details are recovered, but it is worth keeping in mind that updating the Auth object will trigger a re-registration so you probably do not want to update this constantly.

The recommended approach would be to include it in your Application's onCreate method if you already have the authentication information, and then to also call it when you receive the authentication info.

### Permissions

The following run-time permissions are essentially required by the application (as in, this serves no purpose without these permissions):

- CALL_PHONE
- RECORD_AUDIO
- READ_PHONE_STATE

The READ_CONTACTS is also used but not required.

### Placing a call

```kotlin
pil.call("0123456789")
```

If configured correctly, everything else should be handled for you, including launching your activity.

## Displaying a call

To retrieve a call object, simply request it from the PIL instance:

```kotlin
val call: Call? = pil.call
```

This call object is immutable and is a snap-shot of the call at the time it was requested.

### Render loop

When implementing a Call Activity it is recommended to implement a render loop, so every <1000ms to request the call from the PIL and update your UI appropriately. Obviously it is unlikely you will actually want to use a loop to implement this, a technology such as the Android Handler class is more appropriate.

### Event Handling

The PIL will emit events, when displaying a call you should also re-render when receiving the CALL_UPDATED event.

To listen to events, you should implement the PILEventListener interface.

```kotlin
pil.events.listen(this)
```

An example implementation that will display the call or close the activity depending on the event received:

```kotlin
override fun onEvent(event: Event) = when(event) {
	CALL_ENDED -> {
		if (pil.call == null) {
			finish()
		} else {
			displayCall()
		}
	}
	CALL_UPDATED -> displayCall()
	else -> {}
}
```

### Audio State

The audio state can be requested by querying:

```kotlin
val audioState: AudioState = pil.audio.state
```

Like the call object, this is also an immutable snap-shot at the time it was requested.

To check where you are currently routing audio simply call:

```kotlin
when (pil.audio.state.currentRoute) {
	SPEAKER -> 
	PHONE -> )
	BLUETOOTH -> 
}
```

or if you need to know if Bluetooth is available:

```kotlin
	pil.audio.state.availableRoutes.contains(AudioRoute.BLUETOOTH)
```

## Interacting with a call

All call interactions can be found on the CallActions object which is accessed via the actions property on the PIL.

```kotlin
pil.actions.end()
```

```kotlin
pil.actions.toggleHold()
```

###  Audio

Audio is not necessarily directly tied to a call so it can be found under the audio property on the PIL.

```kotlin
pil.audio.mute()
```

```kotlin
pil.audio.routeAudio(AudioRoute.BLUETOOTH)
```

## Customizing

The library contains colors.xml and strings.xml, your Application should override these if you wish to change the text and color of notifications.

## TODO

These are the long-term goals for the library:

- [ ] Support an option that will use an always-on foreground service to allow for background calls without middleware