package sdkFcm

import activity.NotificationPermissionActivity
import activity.WebViewActivity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import listener.FirebaseMessageListener
import repository.Provider
import util.Logger
import util.isDebugBuild
import android.Manifest
import android.content.pm.PackageManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

class SdkFcmHelper internal constructor(private var context: Context) {


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun isNotificationPermissionGranted() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private val logger = Logger.getLogger("SdkFcmHelper")

    private val listeners = mutableListOf<FirebaseMessageListener>()

    private var retryInterval: Long = DEFAULT_RETRY_INTERVAL

    private val notificationManager: NotificationManagerCompat =
        NotificationManagerCompat.from(context)


    // ...

    fun addListener(listener: FirebaseMessageListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: FirebaseMessageListener) {
        listeners.remove(listener)
    }

    @JvmOverloads
    fun initialise(
        logLevel: Logger.LogLevel = Logger.LogLevel.ERROR,
        retryInterval: Long = DEFAULT_RETRY_INTERVAL,
    ) {
        try {
            setupLogging(logLevel)
            registerForPushIfRequired()
            if (retryInterval >= 5) {
                this.retryInterval = retryInterval
            }
//       requestNotificationPermission(context)
            logger.log { " initialise() Initialising SDK. Log level - $logLevel" }
        } catch (e: Exception) {
            logger.log(Logger.LogLevel.ERROR, e) { " initialise() " }
        }
    }

    @Synchronized
    internal fun onPushReceived(remoteMessage: RemoteMessage) {
        for (listener in listeners) {
            try {
                listener.onPushReceived(remoteMessage, context)
            } catch (e: Exception) {
                logger.log(Logger.LogLevel.ERROR, e) { " onPushReceived() " }
            }
        }
    }

    internal fun onNewToken(token: String) {
        AsyncExecutor.submit {
            try {
                Provider.getRepository(context).saveToken(token)
                notifyListeners(token)
            } catch (e: Exception) {
                logger.log(Logger.LogLevel.ERROR, e) { " onNewToken()" }
            }
        }
    }

    private fun registerForPushIfRequired() {
        AsyncExecutor.submit {
            val savedToken = Provider.getRepository(context).getToken()
            if (savedToken.isEmpty()) {
                registerForPush()
            }
        }
    }

//    private fun checkAndRequestNotificationPermissions(activity: Activity) {
//        if (Build.VERSION.SDK_INT >= 33) {
//            if (ContextCompat.checkSelfPermission(activity, POST_NOTIFICATIONS) != PermissionChecker.PERMISSION_GRANTED) {
//                requestPermissionLauncher.launch(POST_NOTIFICATIONS);
//
//            } else {
//                //permission already granted
//
//            }
//        }
//    }


    fun subscribeToTopics(topics: List<String>) {
        AsyncExecutor.submit {
            try {
                for (topic in topics) {
                    logger.log { "Subscribing to $topic" }
                    FirebaseMessaging.getInstance().subscribeToTopic(topic)
                        .addOnCompleteListener { task ->
                            logger.log { "subscribeToTopic() isSuccess ${task.isSuccessful}" }
                        }
                }
            } catch (e: Exception) {
                logger.log(Logger.LogLevel.ERROR, e) { " subscribeToTopic()" }
            }
        }
    }

    fun unSubscribeTopic(topics: List<String>) {
        AsyncExecutor.submit {
            try {
                for (topic in topics) {
                    logger.log { "Un-subscribing to $topic" }
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                }
            } catch (e: Exception) {
                logger.log(Logger.LogLevel.ERROR, e) { " unSubscribeTopic()" }
            }
        }
    }

    private fun requestNotificationPermission(context: Context) {
        val intent = Intent(context, NotificationPermissionActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun registerForPush() {
        logger.log { " registerForPush(): Will register for push." }
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            try {
                if (!task.isSuccessful) {
                    logger.log { " registerForPush(): Token registration failed." }
                    return@addOnCompleteListener
                }
                val token = task.result
                if (token.isNullOrEmpty()) {
                    logger.log { " registerForPush(): Token null or empty." }
                    return@addOnCompleteListener
                }
                logger.log(Logger.LogLevel.INFO) { " registerForPush() Token: $token" }
                onNewToken(token)
            } catch (e: Exception) {
                logger.log(Logger.LogLevel.ERROR, e) { "registerForPush(): " }
//                scheduleRetry()
            }
        }
    }


    private fun notifyListeners(token: String) {
        try {
            logger.log { " notifyListenersIfRequired() : Notifying listeners" }
            for (listener in listeners) {
                try {
                    listener.onTokenAvailable(token)
                } catch (e: Exception) {
                    logger.log(Logger.LogLevel.ERROR, e) { " notifyListenersIfRequired() : " }
                }
            }
        } catch (e: Exception) {
            logger.log(Logger.LogLevel.ERROR, e) { " notifyListenersIfRequired() " }
        }
    }

    private fun setupLogging(logLevel: Logger.LogLevel) {
        Logger.logLevel = logLevel
        Logger.isLogEnabled = isDebugBuild(context)
    }

    fun createNotificationChannel(
        channelId: String,
        channelName: String,
        soundUri: Uri?,
        importance: Int = NotificationManager.IMPORTANCE_HIGH,
        description: String? = null
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description?.let { setDescription(it) }
                soundUri?.let { setSound(it, audioAttributes) }
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    class PushNotificationBuilder(private val context: Context) {
        private var channelId: String = NOTIFICATION_CHANNEL_ID_DEFAULT
        private var title: String? = null
        private var body: String? = null
        private var soundUri: Uri? = null
        private var smallIcon: Int = android.R.drawable.ic_dialog_info
        private var clickAction: Intent? = null
        private var deepLink: String? = null
        private var webViewUrl: String? = null
        fun setTitle(title: String): PushNotificationBuilder {
            this.title = title

            return this
        }

        fun setBody(body: String): PushNotificationBuilder {
            this.body = body
            return this
        }

        fun setChannelId(channelId: String): PushNotificationBuilder {
            this.channelId = channelId
            return this
        }

        fun setSound(soundUri: Uri?): PushNotificationBuilder {
            this.soundUri = soundUri
            return this
        }


        fun setSmallIcon(icon: Int): PushNotificationBuilder {
            this.smallIcon = icon
            return this
        }

        fun setClickAction(clickIntent: Intent): PushNotificationBuilder {

            return this
        }
        fun setDeepLink(deepLink: String?): PushNotificationBuilder {
            this.deepLink = deepLink
            return this
        }

        fun setWebViewUrl(webViewUrl: String?): PushNotificationBuilder {
            this.webViewUrl = webViewUrl
            return this
        }

        fun show() {
            val intent = when {
                deepLink != null -> {
                    // Open deep link
                    Intent(Intent.ACTION_VIEW, Uri.parse(deepLink))
                }
                webViewUrl != null -> {
                    // Open web view
                    WebViewActivity.createIntent(context, webViewUrl)
                }
                clickAction!=null ->{
                    clickAction
                }
                else -> {
                    // Open the app
                    context.packageManager.getLaunchIntentForPackage(context.packageName)
                }
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_MUTABLE
            )

            val notificationBuilder =
                NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(smallIcon)
                    .setContentTitle(title)
                    .setContentText(body).setAutoCancel(true)
                    .setContentIntent(pendingIntent)


            val notificationId = System.currentTimeMillis().toInt()
            val notificationManager = NotificationManagerCompat.from(context)
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                val notificationChannel = NotificationChannel(
//                    notificationId.toString(), "web_app", NotificationManager.IMPORTANCE_HIGH
//                )
//                notificationManager.createNotificationChannel(
//                    notificationChannel
//                )
//            }
            notificationManager.notify(notificationId, notificationBuilder.build())
        }

    }


    companion object {
        private var instance: SdkFcmHelper? = null

        @JvmStatic
        fun getInstance(context: Context): SdkFcmHelper {
            return instance ?: synchronized(SdkFcmHelper::class.java) {
                val inst = instance ?: SdkFcmHelper(context)
                instance = inst
                inst
            }
        }
    }

}
