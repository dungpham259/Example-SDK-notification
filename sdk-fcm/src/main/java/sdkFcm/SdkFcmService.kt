package sdkFcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import util.Logger


public class SdkFcmService : FirebaseMessagingService() {

    private val logger = Logger.getLogger("FcmClientMessageService")

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        logger.log { " onMessageReceived() Firebase message received." }
        SdkFcmHelper.getInstance(applicationContext).onPushReceived(remoteMessage)

    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        logger.log { " onNewToken(): $token" }
        SdkFcmHelper.getInstance(applicationContext).onNewToken(token)
    }
}
