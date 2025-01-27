package lk.thiwak.megarunii.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import lk.thiwak.megarunii.*
import lk.thiwak.megarunii.browser.CustomWebViewClient
import lk.thiwak.megarunii.browser.MyWebViewClient
import java.security.AccessController


class WebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var gameConfigReceiver: GameConfigReceiver

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        webView = findViewById(R.id.webView)
        //webView.clearCache(true)

        val webSettings = webView.settings

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadsImagesAutomatically = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true

        //webSettings.loadWithOverviewMode = true
        //webSettings.useWideViewPort = true

        val url = intent.getStringExtra("url").toString()
        val data = mapOf(
            "referer" to url,
            "user-agent" to webSettings.userAgentString,
        )

        webView.webViewClient = MyWebViewClient(this, data, webView)
//        webView.webChromeClient = ChromeClient()


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



}