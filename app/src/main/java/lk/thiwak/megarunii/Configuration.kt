package lk.thiwak.megarunii

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import kotlin.properties.Delegates

class Configuration private constructor(private val filePath: String) {

    var xDeviceID: String = ""
    var accessToken: String = ""
    var mobileNumber: String = ""
    var classPath: String = ""
    var libName: String = ""
    var appVersion: String = ""
    var logoutByPush: String = ""
    var isLoginCompleted: String = ""
    var firebaseTokenStatus: String = ""
    var fullName: String = ""
    var refreshCode: String = ""
    var notificationToken: String = ""
    var encryptionKey: String = ""
    var device_id: String = ""


    companion object {
        private var instance: Configuration? = null

        fun initialize(jsonFilePath: String): Configuration {
            if (instance == null) {
                instance = Configuration(jsonFilePath).apply {
                    loadFromFile()
                }
            }
            return instance!!
        }

        fun getInstance(): Configuration {
            return instance ?: throw IllegalStateException("Configuration is not initialized. Call initialize() first.")
        }
    }

    private fun loadFromFile() {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("File does not exist: $filePath")
        }

        val json = file.readText()
        val type = object : TypeToken<Configuration>() {}.type
        val loadedConfig: Configuration = Gson().fromJson(json, type)

        // Manually assign properties from loadedConfig
        this.encryptionKey = loadedConfig.encryptionKey
        this.xDeviceID = loadedConfig.xDeviceID
        this.accessToken = loadedConfig.accessToken
        this.mobileNumber = loadedConfig.mobileNumber
        this.libName = loadedConfig.libName
        this.classPath = loadedConfig.classPath
        this.firebaseTokenStatus = loadedConfig.firebaseTokenStatus
        this.appVersion = loadedConfig.appVersion
        this.logoutByPush = loadedConfig.logoutByPush
        this.isLoginCompleted = loadedConfig.isLoginCompleted
        this.fullName = loadedConfig.fullName
        this.refreshCode = loadedConfig.refreshCode
        this.notificationToken = loadedConfig.notificationToken
        this.device_id = device_id
    }

    fun saveToFile() {
        val file = File(filePath)
        val json = Gson().toJson(this)
        file.writeText(json)
    }

    fun updateRefreshCode(refreshCode: String) {
        this.refreshCode = refreshCode
        saveToFile()
    }

    fun updateAccessToken(accessToken: String) {
        this.accessToken = accessToken
        saveToFile()
    }
}