package lk.thiwak.megarunii.network

import android.content.Context
import lk.thiwak.megarunii.log.Logger
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

open class HttpRequest(private val context: Context) {

    private var client: OkHttpClient = OkHttpClient.Builder().build()
    private val headers: MutableMap<String, String> = mutableMapOf()

    private fun validateResponse(response: Response): Boolean {
        return when (response.code) {
            in 200..299 -> true
            403 -> {
                Logger.error(context, "403: Forbidden")
                false
            }
            401 -> {
                Logger.error(context, "401: Unauthorized")
                Logger.info(context, "Retry with --update-token")
                false
            }
            else -> {
                Logger.debug(context, "Unknown response: ${response.code}")
                Logger.debug(context, response.body?.string() ?: "No response body")
                false
            }
        }
    }

    private fun executeRequest(request: okhttp3.Request): Response? {
        return try {
//            Logger.info(context, "Request: ${request.method} ${request.url}")
            val response = client.newCall(request).execute()
            if (validateResponse(response)) response else null
        } catch (e: IOException) {
            Logger.error(context, "IOException during request: ${e.message}")
            null
        }
    }

    private fun buildUrlWithParams(url: String, params: Map<String, String>): String {
        val httpUrl = url.toHttpUrlOrNull()?.newBuilder()
        params.forEach { (key, value) -> httpUrl?.addQueryParameter(key, value) }
        return httpUrl?.build().toString()
    }

    fun addHeaders(newHeaders: Map<String, String>) {
        headers.clear()
        headers.putAll(newHeaders)
//        Logger.info(context, "Headers updated: $headers")
    }

    fun replaceHeaders(newHeaders: Map<String, String>) {
        for (key in newHeaders.keys) {
            headers.remove(key)
        }
        headers.putAll(newHeaders)
//        Logger.info(context, "Headers updated: $headers")
    }

    fun appendHeaders(newHeaders: Map<String, String>) {
        headers.putAll(newHeaders)
//        Logger.info(context, "Headers appended: $headers")
    }

    fun getData(url: String, data: Map<String, String>? = null): Response? {
        val finalUrl = data?.let { buildUrlWithParams(url, it) } ?: url
        Logger.info(context, "GET $finalUrl")

        val requestBuilder = okhttp3.Request.Builder().url(finalUrl)

        // Add headers to the request
        headers.forEach { (key, value) -> requestBuilder.addHeader(key, value) }

        return executeRequest(requestBuilder.build())
    }

    fun postData(url: String, data: String): Response? {
//        val requestBody = FormBody.Builder().apply { data }.build()
        val requestBody = data.toRequestBody("application/json".toMediaType())
        val requestBuilder = okhttp3.Request.Builder()
            .url(url)
            .post(requestBody)

        // Add headers to the request
        headers.forEach { (key, value) -> requestBuilder.addHeader(key, value) }

        return executeRequest(requestBuilder.build())
    }

    fun putData(url: String, data: Map<String, String>): Response? {
        val requestBody = FormBody.Builder().apply {
            data.forEach { (key, value) -> add(key, value) }
        }.build()

        val requestBuilder = okhttp3.Request.Builder()
            .url(url)
            .put(requestBody)

        // Add headers to the request
        headers.forEach { (key, value) -> requestBuilder.addHeader(key, value) }

        return executeRequest(requestBuilder.build())
    }
}


