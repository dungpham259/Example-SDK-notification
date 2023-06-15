package com.example.emample_sdk_notification

import android.R
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.firebase.messaging.RemoteMessage
import listener.FirebaseMessageListener
import sdkFcm.SdkFcmHelper


class FcmListener : FirebaseMessageListener {
    override fun onTokenAvailable(token: String) {
        Log.v("FcmListener1", "Token: $token")
    }

    override fun onPushReceived(remoteMessage: RemoteMessage, context: Context) {
//        val availableDeepLink=   remoteMessage.data.containsKey("deeplink")
//        val availableUrl=   remoteMessage.data.containsKey("url")
//        var deepLink: String?=null
//        var url: String?=null
//        if(availableDeepLink)
//            deepLink = remoteMessage.data["deeplink"]
//        else if (availableUrl)
//             url =  remoteMessage.data["url"]


        Log.v("FcmListener1", "Push Message: $remoteMessage")
        remoteMessage.notification?.let { notification ->
            val title = notification.title ?: "Notification Title"
            val body = notification.body ?: "Notification Body"
            SdkFcmHelper.PushNotificationBuilder(context)
                .setSmallIcon(R.drawable.ic_delete)
                .setTitle(title)
                .setBody(body)
                .setChannelId("push_notification")
                .setWebViewUrl("https://google.com")
//                .setDeepLink(deepLink)
                .show()

        }
    }

}
