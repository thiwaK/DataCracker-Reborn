package lk.thiwak.megarunii

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class StopReceiver(private val service: BackgroundService, private val worker: Worker) : BroadcastReceiver() {


    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i("BackgroundService", "Stop action received, stopping service...")
        worker.stopNow()

        while (worker.isAlive) {
            Thread.sleep(1000)
        }
        Log.i("BackgroundService", "Thread stopped.")

        service.shouldRun = false
        service.handler.removeCallbacks(service.runnable)
        service.stopSelf()
    }
}