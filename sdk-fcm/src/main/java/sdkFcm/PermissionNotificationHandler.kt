package sdkFcm

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class PermissionNotificationHandler(private val context: Context) {
    private val notificationChannelId = "permission_channel"
    private val notificationId = 1

    fun requestPermission(activity: Activity, permission: String, rationale: String) {
        if (hasPermission(permission)) {
            // Permission already granted
            return
        }

        if (shouldShowRationale(permission, activity)) {
            showPermissionRationale(activity, permission, rationale)
        } else {
            requestPermission(activity, permission)
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun shouldShowRationale(permission: String, activity: Activity): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    private fun showPermissionRationale(activity: Activity, permission: String, rationale: String) {
        val builder = NotificationCompat.Builder(context, notificationChannelId)
            .setContentTitle("Permission Required")
            .setContentText(rationale)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)


        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:${context.packageName}")
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        builder.setContentIntent(pendingIntent)
        // Handle user action on the notification, e.g., open settings
        // builder.setContentIntent(pendingIntent)

        requestPermission(activity, permission)
    }

    private fun requestPermission(activity: Activity, permission: String) {
        ActivityCompat.requestPermissions(activity, arrayOf(permission), 0)
    }

}
