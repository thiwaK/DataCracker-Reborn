package lk.thiwak.megarunii

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import dalvik.system.DexClassLoader
import net.lingala.zip4j.ZipFile
import org.intellij.lang.annotations.Language
import org.json.JSONObject
import java.io.*
import java.lang.reflect.Method
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.*

object Utils {
    public const val GAME_CONFIG_WORKER_INTENT_ACTION = "lk.thiwak.megarunii.GAME_CONFIG_WORKER"
    public const val GAME_CONFIG_INTENT_ACTION = "lk.thiwak.megarunii.GAME_CONFIG"
    public const val LOG_INTENT_ACTION = "lk.thiwak.megarunii.LOG_MESSAGE"
    public const val STOP_SERVICE_INTENT_ACTION = "lk.thiwak.megarunii.STOP_SERVICE"
    public const val RAID_SHOOTER_V = 20
    public const val RAID_SHOOTER_GAME_ID = "9482808f-72c3-43a5-96c4-38c3d3a7673e"
    public const val FOOD_BLOCKS_V = 24
    public const val FOOD_BLOCKS_GAME_ID = "907bd637-30c0-435c-af6a-ee2efc4c115a"

    val TAG: String = "Utils"

    private fun loadClass(context: Context, config: Configuration, methodName:String): Method? {
        val libPath = File(context.filesDir.path, config.libName)
        val classPath = config.classPath

        if (classPath.isEmpty()){
            return null
        }

        if (!libPath.exists()) {
            Toast.makeText(context, "Something is missing", Toast.LENGTH_LONG).show()
            return null
        }


        try {
            val loader =
                DexClassLoader(libPath.path, context.filesDir.path, null, context.classLoader)
            val classToLoad = Class.forName(classPath, true, loader)
            return classToLoad.getMethod(methodName, String::class.java, String::class.java)

        } catch (e: ClassNotFoundException) {
            Log.e(TAG, "Class Not Found: ${e.message}")
        } catch (e: NoSuchMethodException) {
            Log.e(TAG, "Method Not Found: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error during encryption/decryption: ${e.message}")
        }

        return null
    }

    fun cry(context: Context, tears:String): String? {
        val mainConfig = getCoreConfiguration(context) ?: return null
        val method = loadClass(context, mainConfig,"encrypt") ?: return null
        if (mainConfig.encryptionKey.isEmpty()){
            return null
        }
        return method.invoke(null, tears, mainConfig.encryptionKey) as String
    }

    fun uncry(context: Context, tears:String): String? {
        val mainConfig = getCoreConfiguration(context) ?: return null
        val method = loadClass(context, mainConfig,"decrypt") ?: return null
        if (mainConfig.encryptionKey.isEmpty()){
            return null
        }
        return method.invoke(null, tears, mainConfig.encryptionKey) as String
    }

    private fun getExternalConfigPaths(context: Context): Array<File>? {
        val externalStorageDir = Environment.getExternalStorageDirectory()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR).toString()
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(currentYear.toByteArray())
            .joinToString("") { "%02x".format(it) }

        val cacheDir = File(externalStorageDir, "Android/data/lk.wow.superman/cache")
        if (!cacheDir.exists()) {
            Toast.makeText(context, "Cache directory does not exist.", Toast.LENGTH_SHORT).show()
        }

        val file = File(cacheDir, hash + "_000.cache")
        val file2 = File(cacheDir, hash + "_001.cache")
        if (file.exists() || file2.exists()) {
            return arrayOf(file, file2);
        }
        Toast.makeText(context, "Cache files does not exist.", Toast.LENGTH_SHORT).show()
        return null
    }

    fun exportBaseConfig(context: Context) {
        // Function to read system properties
        fun getSystemProperty(key: String): String {
            return try {
                val process = Runtime.getRuntime().exec("getprop $key")
                BufferedReader(InputStreamReader(process.inputStream)).use { it.readLine() ?: "" }
            } catch (e: Exception) {
                ""
            }
        }

        fun getExternalConfigContent(): String? {
            return getExternalConfigPaths(context)?.get(0)?.readText() + "id\n" + (getExternalConfigPaths(context)?.get(1)?.readText()
                ?.trim() ?: "")
        }

        fun xor(input: String): String {
            val key = context.getString(R.string.me).toCharArray()[0]
            val result = StringBuilder()
            for (char in input) {
                val encryptedChar = char.code.xor(key.code).toChar()
                result.append(encryptedChar)
            }
            return result.toString()
        }

        fun structure(firstString: String, secondString: String): ByteArray {

            val magic = context.getString(R.string.me).toByteArray().copyOfRange(0, 6) // 5 bytes magic string
            require(magic.size == 6) { "Overflow." }

            val firstStringBytes = xor(firstString).toByteArray()
            Log.i(TAG, firstStringBytes.size.toString())
            val secondStringBytes = xor(secondString).toByteArray()
            Log.i(TAG, secondStringBytes.size.toString())
            require(firstStringBytes.size <= 999999) { "Overflow." }
            require(secondStringBytes.size <= 999999) { "Overflow." }

            val firstStringLength = String.format("%06d", firstStringBytes.size).toByteArray()
            val secondStringLength = String.format("%06d", secondStringBytes.size).toByteArray()

            return ByteBuffer.allocate(6 + 6 + 6 + firstStringBytes.size + secondStringBytes.size).apply {
                put(magic)
                put(firstStringLength)
                put(secondStringLength)
                put(firstStringBytes)
                put(secondStringBytes)
            }.array()
        }


        // Collect device information from the system
        val deviceInfo = mutableMapOf<String, String>().apply {
            put("ro.build.type", getSystemProperty("ro.build.type"))
            put("ro.build.tags", getSystemProperty("ro.build.tags"))
            put("ro.build.version.security_patch", getSystemProperty("ro.build.version.security_patch"))
            put("ro.build.version.incremental", getSystemProperty("ro.build.version.incremental"))
            put("ro.build.version.release", getSystemProperty("ro.build.version.release"))
            put("ro.build.version.sdk", getSystemProperty("ro.build.version.sdk"))
            put("ro.build.id", getSystemProperty("ro.build.id"))
            put("ro.product.device", getSystemProperty("ro.product.device"))
            put("ro.product.name", getSystemProperty("ro.product.name"))
            put("ro.product.model", getSystemProperty("ro.product.model"))
            put("ro.product.brand", getSystemProperty("ro.product.brand"))
            put("ro.product.board", getSystemProperty("ro.product.board"))
            put("ro.build.host", getSystemProperty("ro.build.host"))
            put("ro.board.platform", getSystemProperty("ro.board.platform"))

        }

        // Generate additional dynamic values
        deviceInfo["ro.build.product"] = deviceInfo["ro.product.device"] ?: ""
        deviceInfo["ro.build.hidden_ver"] = deviceInfo["ro.build.version.incremental"] ?: ""
        deviceInfo["ro.build.display.id"] =
            "${deviceInfo["ro.build.id"]}.${deviceInfo["ro.build.version.incremental"]}"
        deviceInfo["ro.build.PDA"] = deviceInfo["ro.build.version.incremental"] ?: ""
        deviceInfo["ro.build.flavor"] =
            "${deviceInfo["ro.product.name"]}-${deviceInfo["ro.build.type"]}"
        deviceInfo["ro.build.description"] =
            "${deviceInfo["ro.product.name"]}-${deviceInfo["ro.build.type"]} " +
                    "${deviceInfo["ro.build.version.release"]} ${deviceInfo["ro.build.id"]} " +
                    "${deviceInfo["ro.build.version.incremental"]} ${deviceInfo["ro.build.tags"]}"
        deviceInfo["ro.build.fingerprint"] =
            "${deviceInfo["ro.product.brand"]}/${deviceInfo["ro.product.name"]}/" +
                    "${deviceInfo["ro.product.device"]}:${deviceInfo["ro.build.version.release"]}/" +
                    "${deviceInfo["ro.build.id"]}/${deviceInfo["ro.build.version.incremental"]}:" +
                    "${deviceInfo["ro.build.type"]}/${deviceInfo["ro.build.tags"]}"

        // Convert to key=value format
        val deviceInfoContent = deviceInfo.map { "${it.key}=${it.value}" }.joinToString("\n")
        val data = getExternalConfigContent()?.let { structure(deviceInfoContent, it) }

        // Save to a file
        val textFile = File(context.cacheDir, "base_config.tk").apply {
            if (data != null) {
                writeBytes(data)
            }
        }

        // Create a content Uri using FileProvider
        val fileUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider", // Replace with your FileProvider authority
            textFile
        )

        // Create an intent to share the file
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            //type = "text/plain"
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Start the share activity
        context.startActivity(Intent.createChooser(shareIntent, "Export base config"))
    }

    fun executeBinary(filePath: String, args: List<String>): Array<String>? {
        try {
            val command = mutableListOf(filePath).apply { addAll(args) }
            val process = Runtime.getRuntime().exec(command.toTypedArray())

            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            return arrayOf(output.toString(), error.toString(), exitCode.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun unpackConfig(context: Context){
        val k = context.getString(R.string.app_key).reversed()
        val zipFilePath = File(context.filesDir, "configuration.bin")
        val destinationPath = File(context.filesDir.toURI())

        try {
            unpackZip(zipFilePath.toString(), destinationPath.toString(), k)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun unpackZip(zipFilePath: String, destPath: String, password: String) {
        val zipFile = ZipFile(zipFilePath)
        if (zipFile.isEncrypted) {
            zipFile.setPassword(password.toCharArray())
        }
        zipFile.extractAll(destPath)
    }

    fun getCoreConfiguration(context: Context): Configuration? {

        fun verifyCoreConfig(deviceId: String): Boolean {
            if (getExternalConfigPaths(context)?.get(1)?.readText()?.trim().equals(deviceId)){
                return true
            }
            return false
        }

        if (!File(context.filesDir, "configuration.bin").exists()){
            Toast.makeText(context, "You need to have a configuration file", Toast.LENGTH_LONG).show()
            return null
        }

        val configJson = File(context.filesDir, "config.json")
        if (!configJson.exists()){
            Utils.unpackConfig(context);
            if (!configJson.exists()){
                Toast.makeText(context, "Something is wrong", Toast.LENGTH_LONG).show()
                return null
            }
        }

        Configuration.initialize(configJson.path)
        val config = Configuration.getInstance()

        if (verifyCoreConfig(config.xDeviceID)){
            return config
        }

        Toast.makeText(context, "Ex configuration has changed. Renew immediately.", Toast.LENGTH_LONG).show()
        return null
    }

    fun getDeviceCodeName(context: Context): String {
        return Build.DEVICE
    }

    fun shareDeviceInfo(context: Context) {

        fun getSystemProperty(key: String): String {
            return try {
                val process = Runtime.getRuntime().exec("getprop $key")
                BufferedReader(InputStreamReader(process.inputStream)).use { it.readLine() ?: "" }
            } catch (e: Exception) {
                ""
            }
        }

        // Collect device information
//        val deviceInfo = JSONObject().apply {
//            put("ro.build.PDA", android.os.Build.DISPLAY)
//            put("ro.build.id", android.os.Build.ID)
//            put("ro.build.display.id", android.os.Build.DISPLAY)
//            put("ro.build.version.incremental", android.os.Build.VERSION.INCREMENTAL)
//            put("ro.build.version.release", android.os.Build.VERSION.RELEASE)
//            put("ro.build.version.sdk", android.os.Build.VERSION.SDK_INT)
//            put("ro.product.model", android.os.Build.MODEL)
//            put("ro.product.brand", android.os.Build.BRAND)
//            put("ro.product.name", android.os.Build.PRODUCT)
//            put("ro.product.device", android.os.Build.DEVICE)
//            put("ro.product.board", android.os.Build.BOARD)
//            put("FINGERPRINT", android.os.Build.FINGERPRINT)
//            put("BOOTLOADER", android.os.Build.BOOTLOADER)
//            put("HOST", android.os.Build.HOST)
//            put("HARDWARE", android.os.Build.HARDWARE)
//            put("ro.product.manufacturer", android.os.Build.MANUFACTURER)
//            put("ro.product.cpu.abi", android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown")
//            put("ro.product.cpu.abilist", android.os.Build.SUPPORTED_ABIS.joinToString(","))
//
//        }

        val deviceInfo = mutableMapOf<String, String>().apply {
            put("ro.build.type", getSystemProperty("ro.build.type"))
            put("ro.build.tags", getSystemProperty("ro.build.tags"))
            put("ro.build.version.security_patch", getSystemProperty("ro.build.version.security_patch"))
            put("ro.build.version.incremental", getSystemProperty("ro.build.version.incremental"))
            put("ro.build.version.release", getSystemProperty("ro.build.version.release"))
            put("ro.build.version.sdk", getSystemProperty("ro.build.version.sdk"))
            put("ro.build.id", getSystemProperty("ro.build.id"))
            put("ro.product.device", getSystemProperty("ro.product.device"))
            put("ro.product.name", getSystemProperty("ro.product.name"))
            put("ro.product.model", getSystemProperty("ro.product.model"))
            put("ro.product.brand", getSystemProperty("ro.product.brand"))
            put("ro.product.board", getSystemProperty("ro.product.board"))
        }

        deviceInfo["ro.build.product"] = deviceInfo["ro.product.device"] ?: ""
        deviceInfo["ro.build.hidden_ver"] = deviceInfo["ro.build.version.incremental"] ?: ""
        deviceInfo["ro.build.display.id"] =
            "${deviceInfo["ro.build.id"]}.${deviceInfo["ro.build.version.incremental"]}"
        deviceInfo["ro.build.PDA"] = deviceInfo["ro.build.version.incremental"] ?: ""
        deviceInfo["ro.build.flavor"] =
            "${deviceInfo["ro.product.name"]}-${deviceInfo["ro.build.type"]}"
        deviceInfo["ro.build.description"] =
            "${deviceInfo["ro.product.name"]}-${deviceInfo["ro.build.type"]} " +
                    "${deviceInfo["ro.build.version.release"]} ${deviceInfo["ro.build.id"]} " +
                    "${deviceInfo["ro.build.version.incremental"]} ${deviceInfo["ro.build.tags"]}"
        deviceInfo["ro.build.fingerprint"] =
            "${deviceInfo["ro.product.brand"]}/${deviceInfo["ro.product.name"]}/" +
                    "${deviceInfo["ro.product.device"]}:${deviceInfo["ro.build.version.release"]}/" +
                    "${deviceInfo["ro.build.id"]}/${deviceInfo["ro.build.version.incremental"]}:" +
                    "${deviceInfo["ro.build.type"]}/${deviceInfo["ro.build.tags"]}"

        // Save the JSON to a file
        val deviceInfoJson = JSONObject(deviceInfo as Map<*, *>?)

        val jsonFile = File(context.cacheDir, "base_config.json").apply {
            writeText(deviceInfoJson.toString(4)) // Pretty print with 4 spaces
        }

        // Create a content Uri using FileProvider
        val fileUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider", // Replace with your FileProvider authority
            jsonFile
        )

        // Create an intent to share the file
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Start the share activity
        context.startActivity(Intent.createChooser(shareIntent, "Share Device Info"))
    }

}