package lk.thiwak.megarunii.browser

import android.util.Log
import android.webkit.WebViewClient
import lk.thiwak.megarunii.Utils
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

open class CustomWebViewClient : WebViewClient() {

    companion object {
        const val TAG = "CustomWebViewClient"
    }

    private val client = OkHttpClient()

    private fun validateRequestHeaders(request: Request){
//        request.headers.forEach { (key, value) ->
//            Log.d(TAG, "Header: $key = $value")
//        }
        Log.w(TAG, "<< validateRequestHeaders >>")
    }

    // TODO need better exception handling and SSL
    fun fetchDataFromUrl(url: String, requestHeaders: Map<String, List<String>>): Response? {
        return try {
            val request = buildRequest(url, requestHeaders)
            validateRequestHeaders(request)
            val response = client.newCall(request).execute()
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching data from $url", e)
            null // Return null or handle differently if required
        }
    }

    private fun buildRequest(url: String, requestHeaders: Map<String, List<String>>): Request {
        val builder = Request.Builder()
            .url(url)

        requestHeaders.forEach { (key, values) ->
            if (!key.isNullOrBlank() && !values.isNullOrEmpty()) {
                val headerValue = values.joinToString(",")
                if (headerValue.isNotBlank()) {
                    builder.addHeader(key, headerValue)
//                    Log.d(TAG, "Injected header: $key = $headerValue")
                }
            }
        }

        // Build and return the request
        return builder.build()
    }
}