package lk.thiwak.megarunii.browser

import android.content.Context
import android.content.Intent
import android.util.Log
import android.webkit.*
import lk.thiwak.megarunii.R
import lk.thiwak.megarunii.Utils
import lk.thiwak.megarunii.log.Logger
import lk.thiwak.webview.WebViewRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import org.intellij.lang.annotations.Language
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream


class MyWebViewClient(
    private val context: Context,
    private val data: Map<String, String>,
    webView: WebView) : CustomWebViewClient(webView) {

    companion object {
        private const val TAG = "WebViewClient"
        val gameUrlList: JSONObject = JSONObject()
        val gameConfig: JSONObject = JSONObject()

        private val httpClient = OkHttpClient()
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

    private fun fetchData(url: String, method: String, headers: Map<String, String>, body:String?,
    type:String?): Response {

        if (url.contains("/api/game/v1/game-session/") && !url.contains("random-gift")){
            if (type != null) {
                if (body != null) {
                    Log.w("NewGame", body)
                }
            }else{
                Log.e("NewGame", "NO TYPE")
                if (body != null) {
                    Log.w("NewGame", body)
                }
            }
        }

        val requestBuilder = Request.Builder().url(url)

        when (method) {
            "GET" -> requestBuilder.get()
            "POST" -> if (body != null) {
                if (type != null) {
                    requestBuilder.post(body.toRequestBody(type.toMediaType()))
                }else{
                    Log.e(TAG, "fetchData: $method NO MEDIA TYPE")
                }
            }else{
                Log.e(TAG, "fetchData: $method NO BODY")
            }
            "PUT" -> if (body != null) {
                if (type != null) {
                    requestBuilder.post(body.toRequestBody(type.toMediaType()))
                }else{
                    Log.e(TAG, "fetchData: $method NO MEDIA TYPE")
                }
            }else{
                Log.e(TAG, "fetchData: $method NO BODY")
            }
        }

        headers.forEach { (key, value) -> requestBuilder.addHeader(key, value) }

        return httpClient.newCall(requestBuilder.build()).execute()
    }

    private fun unzipGzipByteArray(gzipData: ByteArray?): String {
        if (gzipData == null) {
            throw IllegalArgumentException("Input byte array is null")
        }

        // Wrap the byte array in a ByteArrayInputStream
        ByteArrayInputStream(gzipData).use { byteArrayInputStream ->
            // Use GZIPInputStream to decompress
            GZIPInputStream(byteArrayInputStream).use { gzipInputStream ->
                ByteArrayOutputStream().use { outputStream ->
                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    while (gzipInputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    return outputStream.toString(Charsets.UTF_8.name())
                }
            }
        }
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

    override fun shouldInterceptRequest(view: WebView, request: WebViewRequest): WebResourceResponse? {

        fun getWebViewRequest(headers: Map<String, String>): WebViewRequest{
            return WebViewRequest(
                type = request.type,
                url = request.url,
                method = request.method,
                body = request.body,
                formParameters = request.formParameters,
                headers = headers,
                trace = request.trace,
                enctype = request.enctype,
                isForMainFrame = request.isForMainFrame,
                isRedirect = request.isRedirect,
                hasGesture = request.hasGesture
            )
        }

        data class ParsedContent (
            val webResourceResponse: WebResourceResponse?,
            val bodyText: String?
        )
        fun parseResponse(customResponse:Response): ParsedContent {
            Log.w(TAG, ":parseResponse:")

            val contentTypeHeader = customResponse.header("Content-Type") ?: "text/html"
            val contentType = contentTypeHeader.split(";")[0].trim()
            val contentEncoding = customResponse.header("Content-Encoding") ?: "UTF-8"
            val statusCode = customResponse.code
            val reasonPhrase = customResponse.message.ifBlank { "OK" }
            val responseHeaders = customResponse.headers.toMultimap().mapValues { it.value.joinToString(", ") }
            val bodyContent = customResponse.body?.bytes()

            var bodyText: String? = null

            if (bodyContent != null) {
                if (bodyContent.isEmpty()){
                    Log.e(TAG, "EMPTY BODY!!!!")
                }

                bodyText = when (contentEncoding.lowercase()) {
                    "gzip" -> {
                        Log.w(TAG, "gzip: $contentEncoding")
                        unzipGzipByteArray(bodyContent)
                    }
                    "utf-8" -> {
                        Log.w(TAG, "utf-8: $contentEncoding")
                        bodyContent.toString(Charsets.UTF_8)
                    }
                    else -> {
                        Log.w(TAG, "Unknown Content-Encoding: $contentEncoding")
                        null
                    }
                }
            } else {
                Log.e(TAG, "NULL BODY!!!!")
            }

            val newBodyStream = bodyContent?.inputStream()

            return ParsedContent(WebResourceResponse(
                contentType,
                contentEncoding,
                statusCode,
                reasonPhrase,
                responseHeaders,
                newBodyStream
            ), bodyText)

        }

//        val request = requestOriginal
        val headers = request.headers.toMutableMap()
        val body = request.body
        val contentType = request.headers["content-type"]
        val url = request.url
        val referer = data["referer"].toString()

        if (request != null) {

            if (!url.startsWith("http")) {
                return super.shouldInterceptRequest(view, request)
            }


//            Log.w(TAG, "> url $url")
//            if (headers.contains("referer")) {
//                Log.w(TAG, "> referer ${headers["referer"]}")
//            }
//            if (headers.contains("origin")) {
//                Log.w(TAG, "> origin ${headers["origin"]}")
//            }

            // 404 sites
            if(url.contains("analytics.google.com") ||
                url.contains("stats.g.doubleclick.net") ||
                url.contains("/ads/ga-audiences") ||
                url.contains("play.googleapis.com") ||
                url.contains("googletagmanager.com")
            ){

                return WebResourceResponse(
                    "text/plain",  // MIME type
                    "UTF-8",       // Encoding
                    404,           // HTTP status code
                    "Not Found",   // Status message
                    mapOf("Cache-Control" to "no-cache"), // Headers
                    null           // InputStream (null for empty body)
                )

            }


            // Landing page
            if (url.contains("/landingpage/v") && (url.contains(".css") || url.contains(".js"))) {
                Logger.info(context, "Landing page. Welcome!")
                headers["referer"] = referer
                headers["Accept-Encoding"] = "deflate"

                val response = fetchData(url, request.method.uppercase(), headers, body, contentType)
                val parsedResponse = parseResponse(response)
                val responseForReturn = parsedResponse.webResourceResponse
                val bodyText = parsedResponse.bodyText

                return responseForReturn

            }

            // Requests after landing page
            headers["x-requested-with"] = "lk.wow.superman"


            // Capture access token
            if (url.contains("/api/user/v1/access-token/${referer.split("token=").last()}")){
                Logger.info(context, "New access token requested")

                // Must set this header
                headers["referer"] = referer

                val response = fetchData(url, request.method.uppercase(), headers, body, contentType)
                val parsedResponse = parseResponse(response)
                val responseForReturn = parsedResponse.webResourceResponse
                val bodyText = parsedResponse.bodyText

                if (bodyText == null) {
                    Log.e(TAG, "EMPTY BODY")
                    return responseForReturn
                }

                val jsonObject = JSONObject(bodyText)
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
                        Log.e(TAG, bodyText)
                }

                return responseForReturn

            }

            // SessionId/Gift key capture (This is POST!)
            if (url.contains("/api/game/v1/game-session/random-gift/")){
                Logger.error(context, "Gift request captured")

                val pattern = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
                val match = pattern.find(url)
                if (match != null) {
                    gameConfig.put("sessionId", match.value)
                    Logger.warning(context, "Broadcasting configs")

                    val intentWorker = Intent(Utils.GAME_CONFIG_WORKER_INTENT_ACTION)
                    intentWorker.putExtra("gameConfig", gameConfig.toString())
                    intentWorker.putExtra("gameUrlList", gameUrlList.toString())
                    context.sendBroadcast(intentWorker)

                    val intentConfigReceiver = Intent(Utils.GAME_CONFIG_INTENT_ACTION)
                    intentConfigReceiver.putExtra("gameConfig", gameConfig.toString())
                    intentConfigReceiver.putExtra("gameUrlList", gameUrlList.toString())
                    context.sendBroadcast(intentConfigReceiver)

                }else{
                    Logger.error(context, "Broadcasting configs")
                    val intent = Intent()
                    intent.action = Utils.GAME_CONFIG_INTENT_ACTION
                    context.sendBroadcast(intent)
                    Logger.info(context, "Gift request captured")
                }
            }

            // New game
            if (url.contains("/api/game/v1/game-session/") && !url.contains("random-gift")){
                Logger.info(context, "New game")
            }

            // iFrame JS injection
            // dshl99o7otw46.cloudfront.net /games/907bd637-30c0-435c-af6a-ee2efc4c115a/build/v25/bundle.js
            val jsInjectActivate = false
            if (jsInjectActivate && url.contains("cloudfront.net/games/") && url.contains("/build/") && url.endsWith("bundle.js")){
                Log.w(TAG, "iFrame !!!!!!!!!!!")

                @Language("JS")
                val JAVASCRIPT_INTERCEPTION_CODE = """
                    // Override the XMLHttpRequest to set the x-request-with header
                    XMLHttpRequest.prototype._open = XMLHttpRequest.prototype.open;
                    XMLHttpRequest.prototype.open = function (method, url, async, user, password) {
                        this._open(method, url, async, user, password);
                    };

                    XMLHttpRequest.prototype._setRequestHeader = XMLHttpRequest.prototype.setRequestHeader;
                    XMLHttpRequest.prototype.setRequestHeader = function (header, value) {
                        if (header.toLowerCase() === 'x-request-with') {
                            value = 'test.app'; // Set only x-request-with header to test.app
                        }
                        this._setRequestHeader(header, value);
                    };

                    XMLHttpRequest.prototype._send = XMLHttpRequest.prototype.send;
                    XMLHttpRequest.prototype.send = function (body) {
                        this._send(body);
                    };

                    // Override fetch to add the x-request-with header to the request
                    window._fetch = window.fetch;
                    window.fetch = function () {
                        const firstArgument = arguments[0];
                        let headers = arguments[1] && 'headers' in arguments[1] ? arguments[1]['headers'] : {};

                        // Set the x-request-with header to 'test.app'
                        if (!headers['x-request-with']) {
                            headers['x-request-with'] = 'test.app';
                        }

                        return window._fetch.call(this, firstArgument, { ...arguments[1], headers });
                    }

                """.trimIndent()

                headers["Accept-Encoding"] = "deflate"
                headers["x-requested-with"] = "lk.wow.superman"

                val response = fetchData(url, request.method.uppercase(), headers, body, contentType)
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
                    val newBodyStream = JAVASCRIPT_INTERCEPTION_CODE + bodyContent.toString()

                    if (bodyContent != null) {
                        Log.w(TAG, bodyContent.toString())
                    }

                    Log.w(TAG, "Return response")
                    return WebResourceResponse(
                        contentType,
                        contentEncoding,
                        statusCode,
                        reasonPhrase,
                        responseHeaders,
                        ByteArrayInputStream(newBodyStream.toByteArray())
                    )
                }

            }

            // Splash webp replace (must implement before extension skip)
            val replaceSplash = true
            if (replaceSplash && url.contains("images/mega%20games.webp")) {
                Log.e(TAG, "Splash replace")

                val inputStream = getWebPImageInputStream(context, R.mipmap.splash)
                val response = fetchData(url, request.method.uppercase(), headers, body, contentType)

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
                    //return super.shouldInterceptRequest(view, getWebViewRequest(headers))
                }
            }


            // No way to handle iFrame requests
            if (!request.isForMainFrame){
                return super.shouldInterceptRequest(view, getWebViewRequest(headers))
            }

            // Default behaviour: Custom interceptor
            // headers["Accept-Encoding"] = "deflate"
            headers["x-requested-with"] = "lk.wow.superman"
            val response = fetchData(url, request.method.uppercase(), headers, body, contentType)
            val parsedResponse = parseResponse(response)
            val responseForReturn = parsedResponse.webResourceResponse
            val bodyText = parsedResponse.bodyText

            return responseForReturn

        }

        return super.shouldInterceptRequest(view, getWebViewRequest(headers))
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        Log.d(TAG, "Page finished: $url")
    }

    override fun onSafeBrowsingHit(view: WebView?, request: WebResourceRequest?, threatType: Int, callback: SafeBrowsingResponse?) {
        return
    }

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        super.onReceivedError(view, request, error)
    }
}

