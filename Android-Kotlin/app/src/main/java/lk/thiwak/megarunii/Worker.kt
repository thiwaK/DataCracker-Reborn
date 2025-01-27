package lk.thiwak.megarunii

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import lk.thiwak.megarunii.game.RaidShooter
import lk.thiwak.megarunii.log.Logger
import lk.thiwak.megarunii.network.API
import lk.thiwak.megarunii.ui.WebViewActivity
import org.json.JSONObject

class Worker(val context: Context): Thread() {

    @Volatile var stop:Boolean = false
    private lateinit var mainConfig: Configuration
    private lateinit var api: API
    lateinit var gameConfig: JSONObject
    lateinit var gameUrlList: JSONObject

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
            Log.w(TAG, "Requesting worker to quit!")
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
        val stopIntent = Intent(BackgroundService.STOP_SERVICE_INTENT_ACTION)
        context.sendBroadcast(stopIntent)
        Log.i(TAG, "Worker quit")
        super.interrupt()
    }

    override fun run() {

        mainConfig = Utils.getCoreConfiguration(context)!!
        api = API(context, mainConfig)

        Logger.warning(context, "Worker started")

        val gameArena = preInit() ?: return

        val url = "${gameArena.getString("redirectUrl")}?token=${gameArena.getString("token")}"
        val intent = Intent(context, WebViewActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra("url", url)
        context.startActivity(intent)

        Log.w(TAG, "Looping until config receive. Max 5 min")
        for (i in 1..300) {
            timePass(1000)

            if(::gameConfig.isInitialized){
                if (gameConfig.has("sessionId") && gameConfig.has("access_token")){
                    break
                }
            }

        }
        if(!::gameConfig.isInitialized){
            Logger.error(context, "No game configuration received")
            Logger.error(context, "Time out")
            stopNow()
            return
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
                currentGame = RaidShooter(context, gameConfig)
            }
            //"Food Blocks" -> {
            //    val currentGame = RaidShooter(context, gameConfig)
            //}
        }

        // ---------------- Reward Loop ---------------- //
        while (true){
            if (currentGame is RaidShooter){
                if (stop){ return }
                currentGame.askForPlayerInfo()

                val waitingTime = currentGame.calWaitingTime()
                timePass(waitingTime)

                if (stop){ return }
                currentGame.askForTheGift()

                Logger.info(context, "Moving into the next hit!")
            }
        }


    }
}