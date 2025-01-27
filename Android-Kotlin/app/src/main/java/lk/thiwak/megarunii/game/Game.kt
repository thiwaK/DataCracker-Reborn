package lk.thiwak.megarunii.game


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Base64
import android.util.Log
import com.evgenii.jsevaluator.JsEvaluator
import com.evgenii.jsevaluator.interfaces.JsCallback
import com.squareup.duktape.Duktape
import lk.thiwak.megarunii.R
import lk.thiwak.megarunii.Utils
import lk.thiwak.megarunii.log.Logger
import okhttp3.Response
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random
import java.util.zip.ZipFile

open class Game(private val gameConfig:JSONObject, private val context:Context) {

    val noGiftsWarnLimit = 25
    var giftCount = 0
    var gameScore = 0

    var chancesGot = 0
    var chancesLeft = 0
    var dataGot = 0
    var dataLeft = 0

    private val staticNumber = Random.nextInt(1, 10)
    private val api = API(context, gameConfig)

    companion object{
        var giftKey: String? = null
        const val TAG = "Game"
    }

    fun getCurrentTimeStr(): String{
        val currentTimeMillis = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date(currentTimeMillis))
    }

    fun getStrTime(milliseconds: Long): String {
        val currentTimeMillis = System.currentTimeMillis()
        val newTimeMillis = currentTimeMillis + milliseconds
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date(newTimeMillis))
    }

    fun askForPlayerInfo() {
        try {
            val response = api.getInfo()
            if (response != null) {
                if (response.code == 200) {
                    val data = response.body?.string()
                    println("Data: $data")
                } else {
                    println("Error: ${response.code}")
                }
            }
        } catch (e: Exception) {
            println("Error fetching info: ${e.message}")
        }
    }

    private fun parseGiftResponseContent(response: Response): Int?{
        val responseText = response.body?.string()
        val jsonObject = responseText?.let { JSONObject(it) }
        if (jsonObject != null) {
            if (jsonObject.getString("statusInfo") == "OK") {
                return jsonObject.getJSONObject("data").getInt("amount")
            }
        }
        return null
    }


    fun getGift(body: String): Int? {

        Logger.error(context, "A gift magic requested")
        giftKey = null
        getGiftKey()

        for (i in 1..10){
            Thread.sleep(500)
            if(giftKey != null){
                break
            }
        }

        if (giftKey == null){
            Logger.error(context, "CRITICAL: Gift magic failed")
            return null
        }

        if (giftKey == "null"){
            Logger.error(context, "Null gift magic !")
            return null
        }

        Logger.info(context, "Gift magic: $giftKey")
        val additionalHeaders = mapOf(
            "Idempotency-Key" to giftKey.toString()
        )

        try {
            val response = api.getGift(body, additionalHeaders)
            if (response != null) {
                return parseGiftResponseContent(response)
            }
        } catch (e: Exception) {
            println("Error fetching random gift: ${e.message}")
        }
        return null
    }

    private fun getGiftKey() {

        fun runJS(script: String, argv:List<String>) {

            val giftKeyIntent = Intent(Utils.GIFT_KEY_INTENT_ACTION)
            giftKeyIntent.putExtra("argv", JSONObject(mapOf(
                "0" to argv[0],
                "1" to argv[1],
                "2" to argv[2]
            )).toString())
            giftKeyIntent.putExtra("src", script)
            context.sendBroadcast(giftKeyIntent)
        }

        val gameName = gameConfig.optJSONObject("game")?.optString("name") ?: run {
            Logger.error(context, "Invalid game configuration: game name is missing")
            return
        }

        val game = when (gameName) {
            "Raid Shooter" -> "RS"
            "Food Blocks", "Ghost Hunter",
            "Cake Zone" -> "FB"
            "Mega Run 2" -> "MR"
            else -> {
                Logger.warning(context, "Invalid game: $gameName")
                null
            }
        }

        if(!File(context.filesDir, Utils.CONFIG_NAME).exists()){
            return
        }

        val zipFile = net.lingala.zip4j.ZipFile(File(context.filesDir, Utils.CONFIG_NAME))
        if (zipFile.isEncrypted) {
            zipFile.setPassword(context.getString(R.string.app_key).reversed().toCharArray())
        }
        val comment = zipFile.comment
        zipFile.close()

        val decodedComment = StringBuilder()
        for (char in comment) {
            val encryptedChar = char.code.xor('='.code).toChar()
            decodedComment.append(encryptedChar)
        }
        val zipContent = Base64.decode(decodedComment.toString(), Base64.DEFAULT)
        val byteArrayInputStream = ByteArrayInputStream(zipContent)
        val unpackedZipContent = Utils.unpackZip(byteArrayInputStream, "", context.getString(R.string.app_key).reversed())
        if (unpackedZipContent !is List<*>){
            return
        }

        val script = unpackedZipContent
            .filterIsInstance<ByteArray>()
            .joinToString(separator = "") { byteArray -> byteArray.toString(Charsets.UTF_8) }

        val args = listOf (
            game.toString(),
            staticNumber.toString(),
            giftCount.toString()
        )

        return runJS(script, args)

    }

}