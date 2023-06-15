package com.example.emample_sdk_notification

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.os.StrictMode
import androidx.annotation.RequiresApi
import sdkFcm.PermissionNotificationHandler
import sdkFcm.SdkFcmHelper
import util.Logger

class ExampleApplication : Application() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork() // or .detectAll() for all detectable problems
                .penaltyLog()
                .build()
        )
        super.onCreate()
        // initialize sdk
        val skdFcm = SdkFcmHelper.getInstance(applicationContext);

        skdFcm.initialise(Logger.LogLevel.VERBOSE, 4)
        skdFcm.addListener(FcmListener())

        val path =
            ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/";
        val customSoundUri1 = Uri.parse(path + R.raw.custom_noti)
        val customSoundUri2 = Uri.parse(path + R.raw.alarm_clock)

        skdFcm
            .createNotificationChannel(
                "push_notification",
                "Example FCM Notifications",
                customSoundUri1,
            )
        skdFcm
            .createNotificationChannel(
                "push_notification2",
                "Example FCM Notifications 2",
                customSoundUri2,
            )

//                val topics = ArrayList<String>()
//        topics.add("topic_1")
//        topics.add("topic_2")
//        topics.add("topic_3")
//        SdkFcmHelper.getInstance(applicationContext).subscribeToTopics(topics)


    }
}