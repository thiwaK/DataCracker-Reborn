package lk.thiwak.megarunii.browser

import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import lk.thiwak.webview.RequestInspectorWebViewClient
import java.security.AccessController.getContext

open class CustomWebViewClient(webView: WebView) : RequestInspectorWebViewClient(webView) {


}