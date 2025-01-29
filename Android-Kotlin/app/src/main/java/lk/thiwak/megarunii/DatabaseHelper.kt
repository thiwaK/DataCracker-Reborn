package lk.thiwak.megarunii

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper private constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {

        data class User(
            val id: Long,
            val username: String,
            val fullName: String,
            val mobileNumber: String
        )

        data class Log(
            val id: Long,
            val userId: Long?,
            val logType: String,
            val logMessage: String,
            val timestamp: Long
        )

        data class LogEntry(
            val logType: String,
            val logMessage: String,
            val timestamp: Long
        )

        data class Reward(
            val id: Long,
            val userId: Long,
            val amount: Double,
            val timestamp: Long
        )

        private const val DATABASE_NAME = "database"
        private const val DATABASE_VERSION = 1

        private const val TABLE_USERS = "users"
        private const val TABLE_LOGS = "logs"
        private const val TABLE_REWARDS = "rewards"

        private const val CREATE_USERS_TABLE = """
            CREATE TABLE $TABLE_USERS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT,
                full_name TEXT,
                mobile_number TEXT
            );
        """

        private const val CREATE_LOGS_TABLE = """
            CREATE TABLE $TABLE_LOGS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                log_type TEXT,
                log_message TEXT,
                timestamp INTEGER
            );
        """

        private const val CREATE_REWARDS_TABLE = """
            CREATE TABLE $TABLE_REWARDS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                amount REAL,
                timestamp INTEGER
            )
        ;
        """

        private const val CREATE_SYS_USER = """INSERT INTO $TABLE_USERS (username, full_name, mobile_number)
            VALUES ('sys', 'system', '0000000000');"""

        // Static reference to the single instance of AppDatabaseHelper
        @Volatile
        private var INSTANCE: DatabaseHelper? = null

        // Static method to get the singleton instance
        fun getInstance(context: Context): DatabaseHelper {
            return INSTANCE ?: synchronized(this) {
                val instance = DatabaseHelper(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(CREATE_USERS_TABLE)
        db?.execSQL(CREATE_LOGS_TABLE)
        db?.execSQL(CREATE_REWARDS_TABLE)
        db?.execSQL(CREATE_SYS_USER)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_LOGS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_REWARDS")
        onCreate(db)
    }

    @SuppressLint("Range")
    fun getLogs(limit: Int, offset: Int): List<LogEntry> {
        val logList = mutableListOf<LogEntry>()
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT timestamp, log_type, log_message FROM $TABLE_LOGS ORDER BY timestamp DESC LIMIT ? OFFSET ?",
            arrayOf(limit.toString(), offset.toString())
        )

        if (cursor != null) {
            val timestampIndex = cursor.getColumnIndex("timestamp")
            val typeIndex = cursor.getColumnIndex("log_type")
            val messageIndex = cursor.getColumnIndex("log_message")

            if (timestampIndex == -1) {
                cursor.close()
                throw IllegalStateException("Column not found in database: timestamp")
            }
            if (typeIndex == -1) {
                cursor.close()
                throw IllegalStateException("Column not found in database: type")
            }
            if (messageIndex == -1) {
                cursor.close()
                throw IllegalStateException("Column not found in database: message")
            }

            while (cursor.moveToNext()) {
                val timestamp = cursor.getLong(timestampIndex)
                val message = cursor.getString(messageIndex)
                val type = cursor.getString(typeIndex)
                logList.add(LogEntry(type, message, timestamp))
            }
            cursor.close()
        }
        return logList
    }

    // Simple methods for inserting data into users, logs, and rewards
    fun insertUser(username: String, fullName: String, mobileNumber: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("username", username)
            put("full_name", fullName)
            put("mobile_number", mobileNumber)
        }
        return db.insert(TABLE_USERS, null, values)
    }

    fun insertLog(userId: Long, logType: String, logMessage: String, timestamp: Long): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("user_id", userId)
            put("log_type", logType)
            put("log_message", logMessage)
            put("timestamp", timestamp)
        }
        return db.insert(TABLE_LOGS, null, values)
    }

    fun insertReward(userId: Long, amount: Double, timestamp: Long): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("user_id", userId)
            put("amount", amount)
            put("timestamp", timestamp)
        }
        return db.insert(TABLE_REWARDS, null, values)
    }

    // Retrieve all users
    @SuppressLint("Range")
    fun getAllUsers(): List<User> {
        val db = readableDatabase
        val cursor = db.query(TABLE_USERS, null, null, null, null, null, null)
        val users = mutableListOf<User>()
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndex("id"))
                val username = it.getString(it.getColumnIndex("username"))
                val fullName = it.getString(it.getColumnIndex("full_name"))
                val mobileNumber = it.getString(it.getColumnIndex("mobile_number"))
                users.add(User(id, username, fullName, mobileNumber))
            }
        }
        return users
    }

    // Retrieve logs for a user
    @SuppressLint("Range")
    fun getLogsByUser(userId: Long): List<Log> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_LOGS, null,
            "user_id = ?", arrayOf(userId.toString()), null, null, null
        )
        val logs = mutableListOf<Log>()
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndex("id"))
                val logType = it.getString(it.getColumnIndex("log_type"))
                val logMessage = it.getString(it.getColumnIndex("log_message"))
                val timestamp = it.getLong(it.getColumnIndex("timestamp"))
                logs.add(Log(id, userId, logType, logMessage, timestamp))
            }
        }
        return logs
    }

    // Retrieve rewards for a user
    @SuppressLint("Range")
    fun getRewardsByUser(userId: Long): List<Reward> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_REWARDS, null,
            "user_id = ?", arrayOf(userId.toString()), null, null, null
        )
        val rewards = mutableListOf<Reward>()
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndex("id"))
                val amount = it.getDouble(it.getColumnIndex("amount"))
                val timestamp = it.getLong(it.getColumnIndex("timestamp"))
                rewards.add(Reward(id, userId, amount, timestamp))
            }
        }
        return rewards
    }
}

