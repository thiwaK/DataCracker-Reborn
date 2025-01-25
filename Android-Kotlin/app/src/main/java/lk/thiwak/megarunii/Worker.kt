package lk.thiwak.megarunii

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
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

    fun stopNow() {
        stop = true
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

        stop = true
        for (i in 1..60) {

            if (stop){ interrupt() }

            if (currentThread().isInterrupted) {
                Log.w("BackgroundService", "Background task interrupted, stopping task...")
                return
            }

            Log.w("BackgroundService", "Task running: $i")

            try {
                sleep(5000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }
}