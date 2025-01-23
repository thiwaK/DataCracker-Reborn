package lk.thiwak.megarunii.network

import okhttp3.Response
import android.content.Context
import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import lk.thiwak.megarunii.Configuration
import lk.thiwak.megarunii.Utils
import lk.thiwak.megarunii.log.Logger
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class API(private var context: Context, private var AppConfig: Configuration) {

    private var headers: MutableMap<String, String> = mutableMapOf()
    private var request: HttpRequest = HttpRequest(context)

    companion object {
        private const val OKHTTP_VER = "4.9.2"
        private const val BASE_URL = "https://api.wow.lk"

        private const val CART_PATH = "/superapp-common-checkout-service/cart/"
        private const val ACCESS_TOKEN_PATH = "/superapp-user-profile-service/user/authenticate"
        private const val MEGA_WASANA_PATH = "/superapp-mega-wasana-midend-service/dashboard"
        private const val USER_INFO_PATH = "/superapp-user-profile-service/v2/user/"
        private const val BANNER_PATH = "/superapp-admin-portal-service/banner/get-mobile"
        private const val MEGA_APP_PATH = "/superapp-mini-app-authentication-service/v3/application/authentication"
    }

    private fun isTokenExpired(token: String): Boolean {
        val decodedJWT: DecodedJWT = JWT.decode(token)
        val expiration: Date = decodedJWT.expiresAt
        return expiration.before(Date())
    }

    // Build headers dynamically based on AppConfig
    private fun buildHeaders(): MutableMap<String, String> {
        return if (AppConfig.accessToken.isNotBlank() && AppConfig.xDeviceID.isNotBlank()) {
            mutableMapOf(
                "accept" to "application/json, text/plain, */*",
                "accept-encoding" to "gzip",
                "accept-language" to "en",
                "authorization" to "Bearer ${AppConfig.accessToken}",
                "content-type" to "application/json",
                "user-agent" to "okhttp/$OKHTTP_VER",
                "x-device-id" to AppConfig.xDeviceID
            )
        } else {
            mutableMapOf()
        }
    }

    // Check and set headers
    private fun checkHeaders(headersOverride: Map<String, String>): Boolean {

        val builtHeaders = buildHeaders()
        if (builtHeaders.isEmpty()) {
            Logger.error(context, "Invalid headers or missing configuration.")
            return false
        }

        if (headersOverride.isNotEmpty()) {
            for (key in headersOverride.keys) {
                builtHeaders.remove(key)
            }
            Logger.info(context, "Overridden headers removed.")
        }
        builtHeaders.putAll(headersOverride)

        headers.clear()
        headers.putAll(builtHeaders)
        Logger.info(context, "Headers successfully set.")

        return true
    }


    // Perform a GET request
    private fun get(urlSuffix: String): String? {
        return try {
            request.addHeaders(headers)
            val response: Response? = request.getData("$BASE_URL$urlSuffix")
            response?.body?.string()
        } catch (e: Exception) {
            Logger.error(context, "Error during GET request: ${e.message}")
            null
        }
    }

    // Perform a POST request
    private fun post(urlSuffix: String, data: String): String? {
        return try {
            request.addHeaders(headers)
            val response: Response? = request.postData("$BASE_URL$urlSuffix", data)
            response?.body?.string()
        } catch (e: Exception) {
            Logger.error(context, "Error during GET request: ${e.message}")
            null
        }
    }


    fun checkout(): Boolean {
        Logger.info(context, "Initiating checkout process...")

        // Ensure headers are set before making the request
        if (!checkHeaders(mapOf())) {
            Logger.error(context, "Missing required headers or configuration values.")
            return false
        }

        // The URL path for the checkout
        val urlSuffix = "$CART_PATH${AppConfig.mobileNumber}"

        // Fetch and process the response body
        val responseBody = get(urlSuffix)
        if (!responseBody.isNullOrEmpty()) {
            try {
                val jsonResponse = JSONObject(responseBody)
                Logger.debug(context, "Checkout response: ${jsonResponse.toString(2)}")
                if (jsonResponse.getInt("statusCode") == 200){
                    return true
                }

            } catch (e: JSONException) {
                Logger.error(context, "Invalid JSON response: $responseBody")
            }
        } else {
            Logger.error(context, "Failed to retrieve a valid response body from the checkout request.")
        }

        return false
    }

    fun getAccessToken(): Boolean{
        Logger.info(context, ":getAccessToken:")

        if (AppConfig.refreshCode.isNullOrEmpty() && AppConfig.mobileNumber.isNullOrEmpty()){
            Logger.error(context, "Null or Empty config values")
            return false
        }

        if (isTokenExpired(AppConfig.refreshCode)){
            Logger.error(context, "Refresh token expired")
            return false
        }


        JSONObject().apply {
            put("bannerType", "SUPPER_APP")
        }

        val body = mapOf(
            "refreshCode" to AppConfig.refreshCode,
            "platform" to "MOBILE",
            "mobileOS" to "android",
            "grantType" to "refresh",
            "msisdn" to AppConfig.mobileNumber,
            "integrityToken" to ""
        )

        val jsonString = JSONObject(body).toString()

        val headers = mapOf(
            "authorization" to "Bearer undefined"
        )

        if (!checkHeaders(headers)) {
            Logger.error(context, "Missing required headers or configuration values.")
            return false
        }

        val urlSuffix = ACCESS_TOKEN_PATH
        val responseBody = post(urlSuffix, jsonString)

        if (!responseBody.isNullOrEmpty()) {
            try {
                val jsonResponse = JSONObject(responseBody)
                Logger.debug(context, "Checkout response: ${jsonResponse.toString(2)}")

                var dataObj = jsonResponse.getJSONObject("data")
                if (jsonResponse.getInt("statusCode") == 200 && dataObj.getInt("statusCode") == 200) {

                    val data = dataObj.getJSONObject("data")
                    AppConfig.updateAccessToken(data.getString("accessToken"))
                    AppConfig.updateRefreshCode(data.getString("refreshCode"))

                    return true
                }
            } catch (e: JSONException) {
                Logger.error(context, "Invalid JSON response: $responseBody")
            }
        } else {
            Logger.error(context, "Failed to retrieve a valid response body from the checkout request.")
        }
        return false
    }

    fun getMegaWasana(): Boolean{
        Logger.info(context, ":getMegaWasana:")

        if (!checkHeaders(mapOf())) {
            Logger.error(context, "Missing required headers or configuration values.")
            return false
        }

        val body: Map<String, String> = mapOf()
        val jsonString = JSONObject(body).toString()

        val urlSuffix = MEGA_WASANA_PATH
        val responseBody = post(urlSuffix, jsonString)

        if (!responseBody.isNullOrEmpty()) {
            try {
                val jsonResponse = JSONObject(responseBody)
                Logger.debug(context, "Checkout response: ${jsonResponse.toString(2)}")

                if (jsonResponse.getInt("statusCode") == 200) {
                    //TODO parse content, maybe?
                    return true
                }
            } catch (e: JSONException) {
                Logger.error(context, "Invalid JSON response: $responseBody")
            }
        } else {
            Logger.error(context, "Failed to retrieve a valid response body from the checkout request.")
        }
        return false

    }

    fun getUserInfo(): Boolean{
        Logger.info(context, ":getUserInfo:")

        if (!checkHeaders(mapOf())) {
            Logger.error(context, "Missing required headers or configuration values.")
            return false
        }

        val urlSuffix = "$USER_INFO_PATH${AppConfig.mobileNumber}"
        val responseBody = get(urlSuffix)

        if (!responseBody.isNullOrEmpty()) {
            try {
                val jsonResponse = JSONObject(responseBody)
                Logger.debug(context, "Checkout response: ${jsonResponse.toString(2)}")

                if (jsonResponse.getInt("statusCode") == 200) {
                    //TODO decrypt and parse content, maybe?
                    return true
                }
            } catch (e: JSONException) {
                Logger.error(context, "Invalid JSON response: $responseBody")
            }
        } else {
            Logger.error(context, "Failed to retrieve a valid response body from the checkout request.")
        }
        return false

    }

    fun getBanners(): Boolean {

        Logger.info(context, ":getBanners:")

        if (!checkHeaders(mapOf())) {
            Logger.error(context, "Missing required headers or configuration values.")
            return false
        }

        val body = JSONObject().apply {
            put("bannerType", "SUPPER_APP")
            put("landingPageLocation", "GAMING_ARENA")
            put("personas", JSONArray().apply {
                put("bank_user")
                put("social_media_user")
//                put("interest_in_higher_education")
                put("eat_out_seeker")
                put("whatsapp")
                put("selfcare_app_user")
                put("youtube_user")
                put("email_user")
                put("ussd_user")
//                put("complainer")
                put("ott_user")
                put("data_user")
                put("data_4g_user")
            })
        }

        val urlSuffix = BANNER_PATH
        Logger.info(context, body.toString())
        val responseBody = post(urlSuffix, body.toString())

        if (!responseBody.isNullOrEmpty()) {
            try {
                val jsonResponse = JSONObject(responseBody)
                Logger.debug(context, "Checkout response: ${jsonResponse.toString(2)}")

                if (jsonResponse.getInt("statusCode") == 200) {
                    //TODO decrypt and parse content, maybe?
                    return true
                }
            } catch (e: JSONException) {
                Logger.error(context, "Invalid JSON response: $responseBody")
            }
        } else {
            Logger.error(context, "Failed to retrieve a valid response body from the checkout request.")
        }
        return false

    }

    fun authorizeMegaApp(): String? {

        Logger.info(context, ":authorizeMegaApp:")

        var body = JSONObject(mapOf(
            "appId" to "GAMING_ARENA",
            "msisdn" to AppConfig.mobileNumber,
            "deviceId" to AppConfig.device_id,
            "extraData" to mapOf(
                "mdm" to "miniAppOpen",
                "cmpID" to "PLAY_MINIAPP",
                "dstn" to "PLMA"
            ),
            "isAuthenticated" to true
        )).toString()

        body = JSONObject(mapOf(
                "appId" to "MEGA_GAMES",
                "msisdn" to AppConfig.mobileNumber,
                "deviceId" to Utils.getDeviceCodeName(context),
                "extraData" to mapOf(
                    "mdm" to null,
                    "cmpID" to null,
                    "dstn" to null
                ),
                "isAuthenticated" to true
            )
        ).toString()

        val cryptBody = Utils.cry(context, body)
        if (cryptBody != null) {
            body = cryptBody
        }

        body = JSONObject(mapOf("data" to body)).toString()

        val headers = mapOf("ect" to "true")
        if (!checkHeaders(headers)) {
            Logger.error(context, "Missing required headers or configuration values.")
            return null
        }

        val urlSuffix = MEGA_APP_PATH
        Logger.info(context, body)
        val responseBody = post(urlSuffix, body)

        if (this.headers.containsKey("ect")){
            this.headers.remove("ect")
        }

        if (!responseBody.isNullOrEmpty()) {
            try {
                val jsonResponse = JSONObject(responseBody)
                Logger.debug(context, "Checkout response: ${jsonResponse.toString(2)}")

                if (jsonResponse.getInt("statusCode") == 200) {
                    val cryptBody = Utils.uncry(context, jsonResponse.getString("data"))
                    if (cryptBody != null) {
                        Logger.warning(context, cryptBody)
                        return cryptBody
                    }
                    return null
                }
            } catch (e: JSONException) {
                Logger.error(context, "Invalid JSON response: $responseBody")
            }
        } else {
            Logger.error(context, "Failed to retrieve a valid response body from the checkout request.")
        }


        return null
    }



}
