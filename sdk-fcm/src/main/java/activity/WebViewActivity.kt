package activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.example.sdk_fcm.R

class WebViewActivity : AppCompatActivity() {
    companion object {
        private const val EXTRA_URL = "extra_url"

        fun createIntent(context: Context, url: String?): Intent {
            return Intent(context, WebViewActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
            }
        }
    }

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        webView = findViewById(R.id.webView)

        val url = intent.getStringExtra(EXTRA_URL)
        if (url != null) {
            loadWebViewWithUrl(url)
        } else {
            // Handle fallback behavior if the URL is not provided
        }
    }

    private fun loadWebViewWithUrl(url: String) {
        webView.webViewClient = WebViewClient()
        webView.loadUrl(url)
    }
}
