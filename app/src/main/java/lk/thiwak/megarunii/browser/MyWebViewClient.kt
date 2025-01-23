package lk.thiwak.megarunii.browser

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.webkit.*
import lk.thiwak.megarunii.Utils
import lk.thiwak.megarunii.log.Logger
import okhttp3.Response
import org.json.JSONObject


class MyWebViewClient(private val context: Context, data: Map<String, String>) : CustomWebViewClient() {

    companion object {
        private const val TAG = "WebViewClient"
        val gameUrlList: MutableMap<String, JSONObject> = mutableMapOf()
    }

    private val requestHeaders: MutableMap<String, List<String>> = mutableMapOf(
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

    private fun parseResponseContent(url: String, content: ByteArray, contentEncoding: String) {

        val contentAsString = content?.toString(Charsets.UTF_8) ?: "Empty body"

        if (url.contains("/api/user/v1/access-token/")){
            val jsonObject = JSONObject(contentAsString)
            if (jsonObject.getString("statusInfo") == "OK" && jsonObject.getBoolean("success")){
                Logger.info(context, "Game arena configuration loaded")

                val gameList = jsonObject.getJSONObject("data").getJSONArray("game_list")
                for (i in 0 until gameList.length()) {
                    val game = gameList.getJSONObject(i)
                    val gameUrl = game.getString("url")
                    gameUrlList[gameUrl] = game
                }

                // Current game
                val gameAccessToken = jsonObject.getJSONObject("data").getString("access_token")
                val gameRefreshToken = jsonObject.getJSONObject("data").getString("refresh_token")

                Logger.info(context, "Game tokens")
                Logger.info(context, gameAccessToken)
                Logger.info(context, gameRefreshToken)

            }else{
                Log.e(TAG, contentAsString)
            }

        }

        else if(url.contains("/games/${Utils.FOOD_BLOCKS_GAME_ID}/build") && url.endsWith("bundle.js")){
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
    }

    override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
        if (url?.startsWith("https://") == true) {
            try {

                var response: Response? = null

                if (url.contains("googleapis.com") || url.contains("google.com")
                    || url.contains("amazonaws.com") || url.contains("google.lk")
                    || url.contains("googletagmanager.com") || url.contains("appcenter.ms")
                    || url.contains("gvt2.com")){
                    return super.shouldInterceptRequest(view, url)
                }

                if (!url.contains("/api/game/v1/game-session/random-gift/") &&
                    !url.contains("/api/user/v1/access-token/") ||
                    url.endsWith(".png") || url.endsWith(".webp") || url.endsWith(".mp3") ||
                    url.endsWith(".ico") || url.endsWith(".woff2")){
                    return super.shouldInterceptRequest(view, url)
                }

                if (url.contains("/api/game/v1/game-session/random-gift/")){
                    val newHeaders: MutableMap<String, List<String>> = requestHeaders.toMutableMap()
                    newHeaders.apply {
                        mutableMapOf<String, List<String>>(
                            "sec-ch-ua-mobile" to listOf("?1"),
                            "Accept" to listOf("*/*"),
                            "X-Requested-With" to listOf("lk.wow.superman"),
                            "Sec-Fetch-Site" to listOf("same-origin"),
                            "Sec-Fetch-Mode" to listOf("cors"),
                            "Sec-Fetch-Dest" to listOf("empty"),
                            "Referer" to listOf("https://dshl99o7otw46.cloudfront.net/"),
                            "Accept-Encoding" to listOf("gzip, deflate, br, zstd"),
                            "Accept-Language" to listOf("en-GB,en-US;q=0.9,en;q=0.8")
                        )
                    }

                    response = fetchDataFromUrl(url, requestHeaders)
                }else{
                    response = fetchDataFromUrl(url, requestHeaders)
                }



                if (response == null) {
                    Log.e(TAG, "Failed to fetch data from $url. Falling back to default behavior.")
                    return super.shouldInterceptRequest(view, url)
                }

                if (url.contains("/api/game/v1/game-session/random-gift/")){
                    Logger.info(context, "Random gift request detected")

                    val pattern = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
                    val match = pattern.find(url)
                    if (match != null) {
                        Logger.warning(context, match.value)
                    }
                }

                val contentTypeHeader = response.header("Content-Type", "text/html") ?: "text/html"
                val contentType = contentTypeHeader.split(";")[0].trim()
                val contentEncoding = response.header("Content-Encoding", "UTF-8")
                val statusCode = response.code
                val reasonPhrase = response.message.ifBlank { "OK" }
                val responseHeaders = response.headers.toMultimap().mapValues { it.value.joinToString(", ") }
                val bodyContent = response.body?.bytes()
                val newBodyStream = bodyContent?.inputStream()

                if (bodyContent != null) {
                    if (contentEncoding != null) {
                        parseResponseContent(url, bodyContent, contentEncoding)
                    }
                }

                return WebResourceResponse(
                    contentType,
                    contentEncoding,
                    statusCode,
                    reasonPhrase,
                    responseHeaders,
                    newBodyStream
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch data from $url", e)
            }
        }
        return super.shouldInterceptRequest(view, url)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        Log.d(TAG, "Page started: $url")
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        Log.d(TAG, "Page finished: $url")
    }

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        super.onReceivedError(view, request, error)
        view?.loadUrl("file:///android_asset/error.html")
    }
}

