package com.example.emample_sdk_notification

import android.R
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_ONE_SHOT
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.firebase.messaging.RemoteMessage
import listener.FirebaseMessageListener
import sdkFcm.SdkFcmHelper


class FcmListener : FirebaseMessageListener {
    override fun onTokenAvailable(token: String) {
        Log.v("FcmListener1", "Token: $token")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPushReceived(remoteMessage: RemoteMessage, context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/"))
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_MUTABLE)

        Log.v("FcmListener1", "Push Message: $remoteMessage")
        remoteMessage.notification?.let { notification ->
            val title = notification.title ?: "Notification Title"
            val body = notification.body ?: "Notification Body"
            SdkFcmHelper.PushNotificationBuilder(context)
                .setSmallIcon(R.drawable.ic_delete)
                .setTitle(title)
                .setBody(body)
                .setChannelId("push_notification2")
                .setClickAction(pendingIntent)
                .show()

        }
    }

}
