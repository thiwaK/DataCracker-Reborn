package lk.thiwak.megarunii.ui

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import lk.thiwak.megarunii.browser.MyWebViewClient
import lk.thiwak.megarunii.browser.CustomWebChromeClient
import lk.thiwak.megarunii.R


class WebViewActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        val webView = findViewById<WebView>(R.id.webView)
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

        webView.loadUrl(url)

    }

}