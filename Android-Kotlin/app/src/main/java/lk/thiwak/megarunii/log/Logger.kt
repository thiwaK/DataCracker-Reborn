package lk.thiwak.megarunii.log

import android.content.Context
import android.content.Intent
import android.util.Log
import lk.thiwak.megarunii.DatabaseHelper
import lk.thiwak.megarunii.Utils

class Logger {

    enum class LogLevel(val code: String) { DEBUG("D"), INFO("I"), WARN("W"), ERROR("E") }

    companion object {
        // Cached database helper instance
        private var dbHelper: DatabaseHelper? = null

        // Initialize dbHelper only when needed
        private fun getDbHelper(context: Context): DatabaseHelper? {
            if (dbHelper == null) {
                dbHelper = DatabaseHelper.getInstance(context)
            }
            return dbHelper
        }

        private fun log(context: Context?, message: String, level: LogLevel) {

            val dbHelper = context?.let { getDbHelper(it) }
            dbHelper?.insertLog(0, level.name, message, System.currentTimeMillis())

//            context?.let {
//                val intent = Intent(Utils.LOG_INTENT_ACTION).apply {
//                    putExtra("logMessage", message)
//                    putExtra("logLevel", level.code)
//                }
//                it.sendBroadcast(intent)
//            }
//
//            // Optional: Log to logcat for debugging
//            logToLogcat("tag", message, level)
        }

        private fun logToLogcat(tag: String, message: String, level: LogLevel) {
            when (level) {
                LogLevel.DEBUG -> Log.d(tag, message)
                LogLevel.INFO -> Log.i(tag, message)
                LogLevel.WARN -> Log.w(tag, message)
                LogLevel.ERROR -> Log.e(tag, message)
            }
        }

        fun info(context: Context, message: String) = log(context, message, LogLevel.INFO)
        fun error(context: Context, message: String) = log(context, message, LogLevel.ERROR)
        fun warning(context: Context, message: String) = log(context, message, LogLevel.WARN)
        fun debug(context: Context, message: String) = log(context, message, LogLevel.DEBUG)
    }

}
