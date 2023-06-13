package sdkFcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import listener.FirebaseMessageListener
import repository.Provider
import util.Logger
import util.isDebugBuild
import java.util.Random


class SdkFcmHelper internal constructor(private var context: Context) {

    private val logger = Logger.getLogger("SdkFcmHelper")

    private val listeners = mutableListOf<FirebaseMessageListener>()

    private var retryInterval: Long = DEFAULT_RETRY_INTERVAL


     fun addListener(listener: FirebaseMessageListener) {
        listeners.add(listener)
    }

     fun removeListener(listener: FirebaseMessageListener) {
        listeners.remove(listener)
    }

    @JvmOverloads
    fun initialise(
        logLevel: Logger.LogLevel = Logger.LogLevel.ERROR,
        retryInterval: Long = DEFAULT_RETRY_INTERVAL
    ) {
        try {
            setupLogging(logLevel)
            registerForPushIfRequired()
            if (retryInterval >= 5) {
                this.retryInterval = retryInterval
            }
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

    @RequiresApi(Build.VERSION_CODES.O)

    fun createNotificationChannel(soundUri: Uri,
                                  channelId:String,
                                  channelName:String,
                                  descriptionChannel:String?="",
                                  importance: Int?= NotificationManager.IMPORTANCE_HIGH) {
        val channel = NotificationChannel(channelId, channelName, importance!!).apply {
            description = descriptionChannel
        }
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()
        channel.setSound(soundUri,audioAttributes)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    class PushNotificationBuilder(private val context: Context) {
        private var channelId: String = NOTIFICATION_CHANNEL_ID_DEFAULT
        private var title: String? = null
        private var body: String? = null
        private var soundUri: Uri? = null
        private var smallIcon: Int = android.R.drawable.ic_dialog_info
        private var clickAction: PendingIntent? = null

        fun setTitle(title: String): PushNotificationBuilder {
            this.title = title

            return this
        }

        fun setBody(body: String): PushNotificationBuilder {
            this.body = body
            return this
        }
        fun setChannelId(channelId:  String): PushNotificationBuilder {
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

        fun setClickAction(pendingIntent: PendingIntent?): PushNotificationBuilder {
            this.clickAction = pendingIntent
            return this
        }

        private fun generateNotificationId(): Int {
            val timestamp = System.currentTimeMillis()
            val random = Random().nextInt(1000) // Choose any range for random numbers

            return (timestamp + random).toInt()
        }

        fun show() {

            val notificationBuilder =
                NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(smallIcon)
                    .setContentTitle(title)
                    .setContentText(body).setAutoCancel(true)
                    .setContentIntent(clickAction)


            val notificationId = generateNotificationId()
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
