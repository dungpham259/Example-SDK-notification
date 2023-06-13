package listener

import android.content.Context
import com.google.firebase.messaging.RemoteMessage


interface FirebaseMessageListener {

     fun onTokenAvailable(token: String)


     fun onPushReceived(remoteMessage: RemoteMessage, context: Context)
}
