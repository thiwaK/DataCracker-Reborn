package lk.thiwak.megarunii

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import lk.thiwak.megarunii.log.Logger
import lk.thiwak.megarunii.ui.MainActivity
import lk.thiwak.megarunii.ui.WebViewActivity

class GameConfigReceiver(private val webViewActivity: WebViewActivity) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        val intentNew = Intent(context, MainActivity::class.java)
        if (intent != null) {
            intentNew.putExtra("gameConfig", intent.getStringExtra("gameConfig"))
            intentNew.putExtra("gameUrlList", intent.getStringExtra("gameUrlList"))
        }
        intentNew.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        context?.startActivity(intentNew)


        if (context != null) {
            Logger.info(context, "Killing web view...")
        }
        webViewActivity.finish()
        webViewActivity.finishAndRemoveTask()

    }
}