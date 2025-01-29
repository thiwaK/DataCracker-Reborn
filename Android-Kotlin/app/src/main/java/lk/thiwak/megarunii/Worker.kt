package lk.thiwak.megarunii

import android.content.Context
import android.content.Intent
import android.os.Handler
//import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import lk.thiwak.megarunii.game.Game
import lk.thiwak.megarunii.game.RaidShooter
import lk.thiwak.megarunii.log.Logger
import lk.thiwak.megarunii.network.API
import lk.thiwak.megarunii.ui.WebViewActivity
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.LinkedBlockingQueue

class Worker(val context: Context,
             val serviceQueue: LinkedBlockingQueue<String>, //To send data
             val threadQueue: LinkedBlockingQueue<String>,  //To retrieve data
             ): Thread() {

    @Volatile var stop:Boolean = false
    private lateinit var mainConfig: Configuration
    private lateinit var api: API
    var gameConfig: JSONObject = JSONObject()
    var gameUrlList: JSONObject = JSONObject()

    companion object {
        const val TAG = "BackgroundService"
    }

    fun stopNow() {
        stop = true
        interrupt()
    }

    private fun preInit(): JSONObject? {
        if (!api.checkout()){
            if (!api.getAccessToken()){
                Logger.error(context, "Unable to continue: getAccessToken failed")
                stopNow()
                return null
            }else{
                if (!api.checkout()){
                    Logger.error(context, "Unable to continue: checkout failed")
                    stopNow()
                    return null
                }
            }
        }

        if (!api.getMegaWasana()){
            Logger.error(context, "Unable to continue: getMegaWasana failed")
            stopNow()
            return null
        }

        if (!api.getUserInfo()){
            Logger.error(context, "Unable to continue: getUserInfo failed")
            stopNow()
            return null
        }

        if (!api.getBanners()){
            Logger.error(context, "Unable to continue: getBanners failed")
            stopNow()
            return null
        }

        val gameArena = api.authorizeMegaApp()
        if (gameArena.isNullOrEmpty()){
            Logger.error(context, "Unable to continue: authorizeMegaApp failed")
            stopNow()
            return null
        }



        return JSONObject(gameArena)
    }

    private fun shouldThisInterrupted(){
        if (stop && !currentThread().isInterrupted){
            stopNow()
            Logger.warning(context, "Requesting worker to quit!")
        }

    }

    private fun timePass(milli: Long) {
        val seconds = milli / 1000
        for (i in 1..seconds) {
            shouldThisInterrupted()
            sleep(1000)
            if (stop){
                return
            }
        }

        val remainingMillis = milli % 1000
        if (remainingMillis > 0) {
            sleep(remainingMillis)
        }
    }


    override fun interrupt() {
        val stopIntent = Intent(Utils.STOP_SERVICE_INTENT_ACTION)
        context.sendBroadcast(stopIntent)
        Logger.info(context, "Worker quit")
        super.interrupt()
    }

    override fun run() {
        mainConfig = Utils.getCoreConfiguration(context)!!
        api = API(context, mainConfig)

        Logger.warning(context, "Worker started")

        threadQueue.clear()
        serviceQueue.clear()


        // Pre-init game
        val gameArena = preInit() ?: return

        // Open game arena
        val url = "${gameArena.getString("redirectUrl")}?token=${gameArena.getString("token")}"
        val intent = Intent(context, WebViewActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra("url", url)
        context.startActivity(intent)

        // Wait for gameConfig
        //Log.w(TAG, "Looping until config receive. Max 5 min")
        for (i in 1..300) {
            timePass(1000)
            if (gameConfig.has("sessionId") && gameConfig.has("access_token")){
                break
            }
        }


        if (gameConfig.has("sessionId") && gameConfig.has("access_token")){
            Logger.info(context, "Game configuration received")
        } else {
            Logger.error(context, "No game configuration received")
            Logger.error(context, "Time out")
            stopNow()
            return
        }

        // ---------------- Identify Game ---------------- //

        var currentGame: Any? = null
        when(gameConfig.optJSONObject("game")?.optString("name")){
            "Raid Shooter" -> {
                currentGame = RaidShooter(context, gameConfig, serviceQueue, threadQueue)
            }
            //"Food Blocks" -> {
            //    val currentGame = RaidShooter(context, gameConfig)
            //}
        }

        // ---------------- Reward Loop ---------------- //
        while (true){
            if (currentGame is RaidShooter){
                if (stop){
                    interrupt()
                    return
                }
                currentGame.askForPlayerInfo()

                val waitingTime = currentGame.calWaitingTime()
                timePass(waitingTime)

                if (stop){
                    interrupt()
                    return
                }
                currentGame.askForTheGift()

                if (stop){
                    interrupt()
                    return
                }
                Logger.info(context, "Moving into the next hit!")
            }
        }


    }
}