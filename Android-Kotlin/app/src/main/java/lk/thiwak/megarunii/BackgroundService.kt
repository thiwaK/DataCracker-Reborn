package lk.thiwak.megarunii

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
//import android.util.Log
import androidx.core.app.NotificationCompat
import com.evgenii.jsevaluator.JsEvaluator
import com.evgenii.jsevaluator.interfaces.JsCallback
import lk.thiwak.megarunii.log.LogReceiver
import lk.thiwak.megarunii.log.Logger
import org.json.JSONObject
import java.util.concurrent.LinkedBlockingQueue
import kotlin.properties.Delegates

class BackgroundService : Service() {

    private lateinit var logReceiver: LogReceiver
    private lateinit var stopReceiver: StopReceiver
    private var startTime by Delegates.notNull<Long>()

    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder
    lateinit var serviceHandler: Handler
    lateinit var serviceRunnable: Runnable

    lateinit var backgroundThread: Worker
    private lateinit var threadLooper: Looper

    var shouldRun by Delegates.notNull<Boolean>()

    private val serviceQueue = LinkedBlockingQueue<String>()
    private val threadQueue = LinkedBlockingQueue<String>()

    private val configReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            if (context != null) {
                Logger.info(context, "Data received")
            }
            intent?.let {
                val gameConfig = it.getStringExtra("gameConfig").toString()
                val gameUrlList = it.getStringExtra("gameUrlList").toString()
                //Log.i("BackgroundService", "Received data: key1=$gameConfig, key2=$gameUrlList")

                backgroundThread.gameUrlList = JSONObject(gameUrlList)
                backgroundThread.gameConfig = JSONObject(gameConfig)
            }

        }
    }

    companion object {
        const val CHANNEL_ID = "MegaRunIIBackgroundServiceChannel"
        const val TAG = "BackgroundService"
    }


    override fun onCreate() {
        super.onCreate()
        // register log receiver
        logReceiver = LogReceiver()
        registerReceiver(logReceiver, IntentFilter(Utils.LOG_INTENT_ACTION))


        //----------------- Core logic -----------------//

        // Start time
        startTime = System.currentTimeMillis()
        shouldRun = true

        // Notification setup
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationBuilder = getNotificationBuilder()

        // Start service in foreground
        startForeground(1, notificationBuilder.build())

        // Set up the handler to update the notification every second
        serviceRunnable = object : Runnable {
            override fun run() {
                if (shouldRun) {
                    updateNotification()
                    serviceHandler.postDelayed(this, 1000)

                    if(!serviceQueue.isEmpty()){
                        runJS(serviceQueue.take())
                    }

                }
            }
        }
        serviceHandler = Handler(Looper.getMainLooper())
        serviceHandler.post(serviceRunnable)




        // Register other receivers
        val configReceiverIntent = IntentFilter(Utils.GAME_CONFIG_INTENT_ACTION)
        registerReceiver(configReceiver, configReceiverIntent)

        Logger.info(this, "Service created")
    }

    private fun runJS(jsObj: String) {
        var giftKey:String  = "null"

        val script = JSONObject(jsObj).getString("script")
        val argv = JSONObject(jsObj).getJSONArray("argv")

        //Log.d(TAG, script)
        //Log.d(TAG, argv.toString())

        serviceQueue.clear()
        try {

            val jsEvaluator = JsEvaluator(applicationContext)
            jsEvaluator.callFunction(script,
                object : JsCallback {
                    override fun onResult(result: String) {
//                        Log.e("jsEvaluator", result)
                        // Send data to Worker
                        threadQueue.put(result)
                    }
                    override fun onError(errorMessage: String) {
//                        Log.e("jsEvaluator", errorMessage)
                        giftKey = "null"
                    }
                }, "ab", argv.get(0), argv.get(1), argv.get(2)
            )

            // JS executed and result send. Done.
            return

        }catch (e: Exception){
            giftKey = "null"
        }

        // Something failed and sending null
        threadQueue.put(giftKey)


    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (::backgroundThread.isInitialized){
            if (this.backgroundThread.isAlive){
                Logger.info(this, "Worker already at work")
            }
        } else {
            Logger.info(this, "Worker staring work")
            startNewThread()
            stopReceiver = StopReceiver(this, backgroundThread)
            registerReceiver(stopReceiver, IntentFilter(Utils.STOP_SERVICE_INTENT_ACTION))
        }

        return START_STICKY // Keep the service running unless explicitly stopped
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(logReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            unregisterReceiver(stopReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            unregisterReceiver(configReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            threadLooper.quit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            backgroundThread.stopNow()
        } catch (e: Exception){
            e.printStackTrace()
        }

        Logger.info(this, "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Binding not supported
        return null
    }

    private fun getNotificationBuilder(): NotificationCompat.Builder {
        // Create notification channel for Android 8.0 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "MegaRun-II Background Service"
            val descriptionText = "Notification for the MegaRun-II background service"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Create the notification for the service
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Background Service Running")
            .setContentText("Self playing is active.")
            .setOnlyAlertOnce(true)
            .setProgress(40, 20, false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_baseline_play_circle_24) // Replace with your icon
    }

    private fun updateNotification() {

        fun getElapsedTime(): String {
            val elapsedMillis = System.currentTimeMillis() - startTime
            val seconds = (elapsedMillis / 1000) % 60
            val minutes = (elapsedMillis / (1000 * 60)) % 60
            val hours = (elapsedMillis / (1000 * 60 * 60)) % 24

            // Format time as HH:mm:ss
            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }

        val elapsedTime = getElapsedTime()
        notificationBuilder.setContentText("Elapsed Time: $elapsedTime")
        notificationManager.notify(1, notificationBuilder.build())
    }

    private fun startNewThread() {
        backgroundThread = Worker(applicationContext, serviceQueue, threadQueue)
        backgroundThread.start()
    }

}
