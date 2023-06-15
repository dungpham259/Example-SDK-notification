package activity

import sdkFcm.PermissionNotificationHandler
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat

class NotificationPermissionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
    }

    private fun requestNotificationPermission() {

        if (!isNotificationPermissionGranted()) {
            val permissionHandler = PermissionNotificationHandler(applicationContext)
            permissionHandler.requestPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS,
                "We need your permission to write to external storage."
            )
        } else {
            // Permission already granted
            finish()
        }
    }

    private fun isNotificationPermissionGranted(): Boolean {
        val notificationManagerCompat = NotificationManagerCompat.from(this)
        return notificationManagerCompat.areNotificationsEnabled()
    }

}

