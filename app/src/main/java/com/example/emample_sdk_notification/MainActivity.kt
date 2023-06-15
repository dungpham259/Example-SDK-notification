package com.example.emample_sdk_notification


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.emample_sdk_notification.ui.theme.EmampleSDKnotificationTheme
import sdkFcm.PermissionNotificationHandler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permissionHandler = PermissionNotificationHandler(applicationContext)
        permissionHandler.requestPermission(
            this,
            android.Manifest.permission.POST_NOTIFICATIONS,
            "We need your permission to write to external storage."
        )
        setContent {
            EmampleSDKnotificationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("SDK FCM")
                }
            }
        }
    }

}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    EmampleSDKnotificationTheme {
        Greeting("Android")
    }
}