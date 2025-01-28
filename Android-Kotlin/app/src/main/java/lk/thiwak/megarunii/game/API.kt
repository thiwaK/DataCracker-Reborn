package lk.thiwak.megarunii.game

import android.content.Context
import lk.thiwak.megarunii.log.Logger
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class API(var context: Context, var gameConfig:JSONObject) {

    private var headers: MutableMap<String, String> = mutableMapOf()
    private var client: OkHttpClient = OkHttpClient.Builder().build()
    private var defaultHeaders: MutableMap<String, String> = mutableMapOf()

    private val GIFT_PATH = "/api/game/v1/game-session/random-gift/${gameConfig.getString("sessionId")}/1"
    private val PROFILE_DATA = "/api/game/v1/profile/data"
    private val BASE_URL = "https://dshl99o7otw46.cloudfront.net"

    init {
        defaultHeaders = mutableMapOf(
            "Sec-Fetch-Site" to "same-origin",
            "Sec-Fetch-Mode" to "cors",
            "Sec-Fetch-Dest" to "empty",
            "sec-ch-ua-platform" to "Android",
            "Accept-Encoding" to "gzip, deflate, br, zstd",
            "Accept-Language" to "en-GB,en-US;q=0.9,en;q=0.8",
            "accept" to "application/json, text/plain, */*",
            "content-type" to "application/json",
            "Referer" to "${gameConfig.getJSONObject("game").getString("url")}?platform=pwa&version=200",
            "sec-ch-ua" to gameConfig.getString("SEC_CH_UA"),
            "user-agent" to gameConfig.getString("UA"),
            "authorization" to "Bearer ${gameConfig.getString("access_token")}",
        )
    }


    /*
        Build url that has parameters
    */
    private fun buildUrlWithParams(url: String, params: Map<String, String>): String {
        val httpUrl = url.toHttpUrlOrNull()?.newBuilder()
        params.forEach { (key, value) -> httpUrl?.addQueryParameter(key, value) }
        return httpUrl?.build().toString()
    }

    /*
        Check response code and return whether the request has succeeded or not
    */
    private fun checkResponseCode(response: Response): Boolean {
        return when (response.code) {
            in 200..299 -> true
            403 -> {
                Logger.error(context, "403: Forbidden")
                false
            }
            401 -> {
                Logger.error(context, "401: Unauthorized")
                false
            }
            else -> {
                Logger.debug(context, "Unknown response: ${response.code}")
                Logger.debug(context, response.body?.string() ?: "No response body")
                false
            }
        }
    }

    /*
        Execute the request
    */
    private fun executeRequest(request: okhttp3.Request): Response? {
        return try {
            val response = client.newCall(request).execute()
            if (checkResponseCode(response)) response else null
        } catch (e: IOException) {
            Logger.error(context, "IOException during request: ${e.message}")
            null
        }
    }

    /*
        Preform GET request
    */
    private fun getData(url: String, additionalHeaders:Map<String, String>, data: Map<String, String>?): Response? {
        val finalUrl = data?.let { buildUrlWithParams(url, it) } ?: url
        val requestBuilder = okhttp3.Request.Builder().url(finalUrl)

        headers = defaultHeaders.toMutableMap()
        additionalHeaders.forEach{ (key, value) -> headers[key] = value}
        headers.forEach { (key, value) -> requestBuilder.addHeader(key, value) }

        return executeRequest(requestBuilder.build())
    }

    /*
        Preform POST request
    */
    private fun postData(url: String, additionalHeaders:Map<String, String>, data: String): Response? {
        val requestBody = data.toRequestBody("application/json".toMediaType())
        val requestBuilder = okhttp3.Request.Builder().url(url).post(requestBody)

        headers = defaultHeaders.toMutableMap()
        additionalHeaders.forEach{ (key, value) -> headers[key] = value}
        headers.forEach { (key, value) -> requestBuilder.addHeader(key, value) }

        return executeRequest(requestBuilder.build())
    }


    fun getInfo(): Response? {
        Logger.info(context, "Getting profile data")
        return getData("$BASE_URL$PROFILE_DATA", mapOf(), null)
    }

    fun getGift(data: String, headers: Map<String, String>): Response? {
        Logger.info(context, "Getting new gift")
        return postData("$BASE_URL$GIFT_PATH", headers, data)
    }
}