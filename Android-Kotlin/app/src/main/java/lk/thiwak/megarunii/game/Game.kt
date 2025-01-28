package lk.thiwak.megarunii.game

import android.content.Context
import android.util.Base64
import lk.thiwak.megarunii.R
import lk.thiwak.megarunii.Utils
import lk.thiwak.megarunii.log.Logger
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.random.Random

open class Game (
    private val context:Context,
    private val gameConfig:JSONObject,
    private val serviceQueue: LinkedBlockingQueue<String>, //To send data
    private val threadQueue: LinkedBlockingQueue<String>, //To retrieve data
)
{
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

    fun getStrTime(milliseconds: Long): String {
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date(milliseconds))
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

        // Getting gift magic
        Logger.info(context, "A gift magic requested")

        giftKey = null
        threadQueue.clear()
        serviceQueue.clear()

        // Send data through serviceQueue
        getGiftKey()

        // Wait for reply
        for (i in 1..10){
            Thread.sleep(500)
            if(!threadQueue.isEmpty()){
                break
            }
        }

        val data = threadQueue.take()
        if (data.isNullOrEmpty()){
            Logger.error(context, "CRITICAL: empty gift magic")
            return null
        }
        else if (data.lowercase() == "null"){
            Logger.error(context, "CRITICAL: null gift magic")
            return null
        }
        else{
            giftKey = data
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

    private fun getScript():String?{
        if(!File(context.filesDir, Utils.CONFIG_NAME).exists()){
            return null
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
            return null
        }

        val script = unpackedZipContent
            .filterIsInstance<ByteArray>()
            .joinToString(separator = "") { byteArray -> byteArray.toString(Charsets.UTF_8) }
        return script
    }

    private fun getGiftKey() {

        fun runJS(argv:List<String>) {

            val script = getScript()
            if (script.isNullOrEmpty()){
                Logger.error(context, "Empty script")
                return
            }
            val dataOut = JSONObject()
            val jsonArray = JSONArray(argv)
            dataOut.put("argv", jsonArray)
            dataOut.put("script", script)

            serviceQueue.clear()
            serviceQueue.put(dataOut.toString())

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


        val args = listOf (
            game.toString(),
            staticNumber.toString(),
            giftCount.toString()
        )

        return runJS(args)
    }

}