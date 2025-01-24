package lk.thiwak.megarunii.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import lk.thiwak.megarunii.*
import lk.thiwak.megarunii.browser.MyWebViewClient
import lk.thiwak.megarunii.browser.CustomWebChromeClient


class WebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var gameConfigReceiver: GameConfigReceiver

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        webView = findViewById(R.id.webView)
        val webSettings = webView.settings

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadsImagesAutomatically = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true
        //webSettings.loadWithOverviewMode = true
        //webSettings.useWideViewPort = true

        Log.w("##", webSettings.userAgentString)

        val url = intent.getStringExtra("url").toString()

        val data = mapOf(
            "referer" to url,
            "user-agent" to webSettings.userAgentString,
        )

        webView.webViewClient = MyWebViewClient(this, data)
        webView.webChromeClient = CustomWebChromeClient()
        webView.addJavascriptInterface(WebAppInterface(this), "Android")


        webView.loadUrl(url)

        gameConfigReceiver = GameConfigReceiver(this)
        registerReceiver(gameConfigReceiver, IntentFilter(Utils.GAME_CONFIG_INTENT_ACTION))

    }

    override fun onDestroy() {
        webView.stopLoading()
        webView.removeAllViews()
        webView.destroy()
        unregisterReceiver(gameConfigReceiver)
        super.onDestroy()
    }

    class WebAppInterface(private val context: Context) {
        @JavascriptInterface
        fun capturePostData(postData: String) {
            Log.d("WebView", "POST Data: $postData")
            // Handle the POST data here (e.g., send it via your own HTTP client)
        }
    }

}