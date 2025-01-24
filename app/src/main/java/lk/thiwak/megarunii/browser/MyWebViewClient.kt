package lk.thiwak.megarunii.browser

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Message
import android.util.Log
import android.webkit.*
import lk.thiwak.megarunii.BackgroundService
import lk.thiwak.megarunii.R
import lk.thiwak.megarunii.Utils
import lk.thiwak.megarunii.log.Logger
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient


class MyWebViewClient(private val context: Context, private val data: Map<String, String>) : CustomWebViewClient() {

    companion object {
        private const val TAG = "WebViewClient"
        val gameUrlList: JSONObject = JSONObject()
        val gameConfig: JSONObject = JSONObject()
    }

    private val requestHeaders: MutableMap<String, List<String>> = mutableMapOf(
        "sec-ch-ua-mobile" to listOf("?1"),
        "Accept" to listOf("*/*"),
        "Sec-Fetch-Site" to listOf("same-origin"),
        "Sec-Fetch-Mode" to listOf("cors"),
        "Sec-Fetch-Dest" to listOf("empty"),
        "Accept-Encoding" to listOf("gzip, deflate, br, zstd"),
        "Accept-Language" to listOf("en-GB,en-US;q=0.9,en;q=0.8"),
        "sec-ch-ua-platform" to listOf("\"Android\""),
        "X-Requested-With" to listOf("lk.wow.superman"),
        "sec-ch-ua" to listOf(
            "\"Chromium\";v=\"130\"",
            "\"Android WebView\";v=\"130\"",
            "\"Not?A_Brand\";v=\"99\""
        ),
        "Accept-Encoding" to listOf("deflate"),
        "Accept-Language" to listOf("en-GB,en-US;q=0.9,en;q=0.8"),
        "Accept" to listOf("text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
    ).apply {
        data["referer"]?.let { this["Referer"] = listOf(it) }
        data["user-agent"]?.let { this["User-Agent"] = listOf(it) }
    }

    private val httpClient = OkHttpClient()
    private fun fetchData(url: String, method: String, headers: Map<String, String>): Response? {
        val requestBuilder = Request.Builder().url(url)

        // Set HTTP method
        // NOTE: Only the get request can execute. Requests with bodies can not.
        //       Because it is not allowed to inspect the body
        when (method) {
            "GET" -> requestBuilder.get()
        }

        // Add headers to the request
        headers.forEach { (key, value) -> requestBuilder.addHeader(key, value) }

        // Execute the request
        return httpClient.newCall(requestBuilder.build()).execute()
    }

    private fun createWebResourceResponse(response: Response): WebResourceResponse {
        // Extract data from the response

        // val contentType = response.header("Content-Type", "text/html") ?: "text/html"
        val contentEncoding = response.header("Content-Encoding", "UTF-8") ?: "UTF-8"
        val contentTypeHeader = response.header("Content-Type", "text/html") ?: "text/html"
        val contentType = contentTypeHeader.split(";")[0].trim()
        val statusCode = response.code
        val reasonPhrase = response.message.ifBlank { "OK" }
        val responseHeaders = response.headers.toMultimap().mapValues { it.value.joinToString(", ") }
        val bodyStream = response.body?.byteStream()

        // Return a WebResourceResponse
        return WebResourceResponse(
            contentType,
            contentEncoding,
            statusCode,
            reasonPhrase,
            responseHeaders,
            bodyStream
        )
    }

    private fun getWebPImageInputStream(context: Context, resourceId: Int): InputStream? {
        return try {
            context.resources.openRawResource(resourceId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun shouldInterceptRequest(view: WebView?, requestOriginal: WebResourceRequest?): WebResourceResponse? {

        var request = requestOriginal
        if (request != null) {
            val url = request.url.toString()
            val referer = data["referer"]

            if (!url.startsWith("https")) {
                return super.shouldInterceptRequest(view, requestOriginal)
            }

            Log.d(TAG,"")

            // Header - x-requested-with
            if (request.requestHeaders.contains("x-requested-with")) {
                request.requestHeaders.remove("x-requested-with")
                request.requestHeaders["x-requested-with"] = "lk.wow.superman"
            } else {
                request.requestHeaders["x-requested-with"] = "lk.wow.superman"
            }
            Log.w(TAG, "> x-requested-with ${request.requestHeaders["x-requested-with"]}")

            // Header - referer
            if (request.requestHeaders.contains("referer")) {
                request.requestHeaders.remove("referer")
                request.requestHeaders["referer"] = "lk.wow.superman"
            } else {

                request.requestHeaders["referer"] = referer

            }
            Log.w(TAG, "> referer ${requestHeaders["referer"]}")

            // Header - origin
            if (request.requestHeaders.contains("origin")) {
                //TODO
                Log.w(TAG, "> origin ${requestHeaders["origin"]}")
            }


            // Handle request headers
            if (url.contains("fonts.googleapis.com")
                || url.contains("firebase.googleapis.com")
                || url.contains("analytics.google.com")
                || url.contains("cloudfront.net")
                || url.contains("stats.g.doubleclick.net")
                || url.contains("firebaseinstallations.googleapis.com")
            ) {

                request.requestHeaders["Accept-Encoding"] = "gzip, deflate, br, zstd"
                request.requestHeaders["Accept-Language"] = "en-GB,en-US;q=0.9,en;q=0.8"
                request.requestHeaders["sec-ch-ua-mobile"] = "?1"
                request.requestHeaders["Accept"] = "*/*"

                request.requestHeaders["X-Requested-With"] = "lk.wow.superman"

                request.requestHeaders["Sec-Fetch-Site"] = "cross-site"
                request.requestHeaders["Sec-Fetch-Dest"] = "empty"
                request.requestHeaders["Sec-Fetch-Mode"] = "no-cors"

                if (url.split("/")[2] == "firebase.googleapis.com") {
                    request.requestHeaders["Sec-Fetch-Mode"] = "cors"
                }

                if (url.split("/")[2] == "fonts.googleapis.com") {
                    request.requestHeaders["Sec-Fetch-Dest"] = "style"
                    request.requestHeaders["Accept"] = "text/css,*/*;q=0.1"
                }

                if (url.split("/")[2] == "analytics.google.com") {
                    request.requestHeaders["Accept"] = "text/css,*/*;q=0.1"
                }

                if (url.split("/")[2] == "dshl99o7otw46.cloudfront.net") {
                    request.requestHeaders["Sec-Fetch-Site"] = "same-origin"
                    request.requestHeaders["Sec-Fetch-Mode"] = "cors"


                    if (!request.requestHeaders.contains("referer")) {
                        Log.e(TAG, "NO REFERER")
                    } else {
                        request.requestHeaders["referer"]?.let { Log.w(TAG, it) }
                    }

                }

                if (url.split("/")[2] == "firebaseinstallations.googleapis.com") {
                    request.requestHeaders["Sec-Fetch-Mode"] = "cors"
                }


                Log.w(TAG, "Generic request to ${url.split("/")[2]}")

            }

            // SessionId/Gift key capture (This is POST!)
            if (url.contains("/api/game/v1/game-session/random-gift/")){
                Logger.error(context, "Session id captured")

                val pattern = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
                val match = pattern.find(url)
                if (match != null) {
                    gameConfig.put("sessionId", match.value)
                    Logger.warning(context, match.value)
                    Logger.warning(context, "Leaving web-view")

                    val intent = Intent()
                    intent.action = Utils.GAME_CONFIG_INTENT_ACTION
                    intent.putExtra("gameConfig", gameConfig.toString())
                    intent.putExtra("gameUrlList", gameUrlList.toString())
                    context.sendBroadcast(intent)

                    return super.shouldInterceptRequest(view, url)
                }else{
                    val intent = Intent()
                    intent.action = Utils.GAME_CONFIG_INTENT_ACTION
                    context.sendBroadcast(intent)
                    return super.shouldInterceptRequest(view, url)
                }
            }



            if (request.method.equals("POST", ignoreCase = true)
                || request.method.equals("PUT", ignoreCase = true)
            ) {
                return super.shouldInterceptRequest(view, request)
            }


            // Splash webp replace (must implement before extension skip)
            if (url.contains("images/mega%20games.webp")) {
                Log.e(TAG, "Splash replace")
                val inputStream = getWebPImageInputStream(context, R.mipmap.splash)
                val response = fetchData(url, request.method.uppercase(), request.requestHeaders)

                if (response != null) {
                    val contentTypeHeader =
                        response.header("Content-Type", "text/html") ?: "text/html"
                    val contentType = contentTypeHeader.split(";")[0].trim()
                    val contentEncoding = response.header("Content-Encoding", "UTF-8")
                    val statusCode = response.code
                    val reasonPhrase = response.message.ifBlank { "OK" }
                    val responseHeaders =
                        response.headers.toMultimap().mapValues { it.value.joinToString(", ") }
                    val bodyContent = response.body?.bytes()
                    val newBodyStream = bodyContent?.inputStream()
                    Log.i(TAG, newBodyStream.hashCode().toString())

                    return WebResourceResponse(
                        contentType,
                        contentEncoding,
                        statusCode,
                        reasonPhrase,
                        responseHeaders,
                        inputStream
                    )
                }

            }



            // Capture access token
            if (url.contains("/api/user/v1/access-token/")){

                Log.e(TAG, "Access token")
                val response = fetchData(url, request.method.uppercase(), request.requestHeaders)
                val jsonObject = JSONObject(response.toString())

                if (jsonObject.getString("statusInfo") == "OK" && jsonObject.getBoolean("success")){
                    Logger.info(context, "Game arena configuration loaded")

                    val gameList = jsonObject.getJSONObject("data").getJSONArray("game_list")
                    for (i in 0 until gameList.length()) {
                        val game = gameList.getJSONObject(i)
                        val gameUrl = game.getString("url")
                        gameUrlList.put(gameUrl, game)
                    }

                    // Current game
                    val gameAccessToken = jsonObject.getJSONObject("data").getString("access_token")
                    val gameRefreshToken = jsonObject.getJSONObject("data").getString("refresh_token")
                    gameConfig.put("access_token", gameAccessToken)
                    gameConfig.put("refresh_token", gameRefreshToken)

                    Logger.info(context, "Game tokens")
                    Logger.info(context, gameAccessToken)
                    Logger.info(context, gameRefreshToken)

                }else{
                    Log.e(TAG, response.toString())
                }

                if (response != null) {
                    val contentTypeHeader =
                        response.header("Content-Type", "text/html") ?: "text/html"
                    val contentType = contentTypeHeader.split(";")[0].trim()
                    val contentEncoding = response.header("Content-Encoding", "UTF-8")
                    val statusCode = response.code
                    val reasonPhrase = response.message.ifBlank { "OK" }
                    val responseHeaders =
                        response.headers.toMultimap().mapValues { it.value.joinToString(", ") }
                    val bodyContent = response.body?.bytes()
                    val newBodyStream = bodyContent?.inputStream()

                    return WebResourceResponse(
                        contentType,
                        contentEncoding,
                        statusCode,
                        reasonPhrase,
                        responseHeaders,
                        newBodyStream
                    )
                }

            }

            if(url.contains("/games/${Utils.FOOD_BLOCKS_GAME_ID}/build") && url.endsWith("bundle.js")){
                if (url.split("/").dropLast(1).last() != "v${Utils.FOOD_BLOCKS_V}") {
                    Logger.warning(context, "Supported version: v${Utils.FOOD_BLOCKS_V}, " +
                            "but detected ${url.split("/").dropLast(1).last()}")
                }
            }

            else if(url.contains("/games/${Utils.RAID_SHOOTER_GAME_ID}/build") && url.endsWith("bundle.js")){
                if (url.split("/").dropLast(1).last() != "v${Utils.RAID_SHOOTER_V}") {
                    Logger.warning(context, "Supported version: v${Utils.RAID_SHOOTER_V}, " +
                            "but detected ${url.split("/").dropLast(1).last()}")
                }
            }



            // get data and parse
            val response = fetchData(url, request.method.uppercase(), request.requestHeaders)
            if (response != null) {
                val contentTypeHeader =
                    response.header("Content-Type", "text/html") ?: "text/html"
                val contentType = contentTypeHeader.split(";")[0].trim()
                val contentEncoding = response.header("Content-Encoding", "UTF-8")
                val statusCode = response.code
                val reasonPhrase = response.message.ifBlank { "OK" }
                val responseHeaders =
                    response.headers.toMultimap().mapValues { it.value.joinToString(", ") }
//                val bodyContent = response.body?.bytes()
//                val newBodyStream = bodyContent?.inputStream()


                return WebResourceResponse(
                    contentType,
                    contentEncoding,
                    statusCode,
                    reasonPhrase,
                    responseHeaders,
                    response.body?.byteStream() ?: null
                )
            }

        }

        return super.shouldInterceptRequest(view, request)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        Log.d(TAG, "Page started: $url")
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)

        // Inject JavaScript to intercept POST requests
        view?.evaluateJavascript(
            """
            (function() {
                var forms = document.querySelectorAll("form");
                forms.forEach(function(form) {
                    form.onsubmit = function(event) {
                        event.preventDefault(); // Prevent form submission
                        var formData = new FormData(form);
                        var data = {};
                        formData.forEach(function(value, key) {
                            data[key] = value;
                        });
                        Android.capturePostData(JSON.stringify(data)); // Send data to Android
                    };
                });
            })();
            """.trimIndent(), null
        )

        Log.d(TAG, "Page finished: $url")
    }

    override fun onSafeBrowsingHit(view: WebView?, request: WebResourceRequest?, threatType: Int, callback: SafeBrowsingResponse?) {
        return
    }

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        super.onReceivedError(view, request, error)
    }
}

