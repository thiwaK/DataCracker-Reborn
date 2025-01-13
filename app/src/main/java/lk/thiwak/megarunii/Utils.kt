package lk.thiwak.megarunii

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.*

object Utils {
    public const val LOG_INTENT_ACTION = "lk.thiwak.megarunii.LOG_MESSAGE"
    public const val STOP_SERVICE_INTENT_ACTION = "lk.thiwak.megarunii.STOP_SERVICE"
    val TAG: String = "Utils"


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

        // Save to a file
        val textFile = File(context.cacheDir, "base_config.txt").apply {
            writeText(deviceInfoContent)
        }

        // Create a content Uri using FileProvider
        val fileUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider", // Replace with your FileProvider authority
            textFile
        )

        // Create an intent to share the file
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Start the share activity
        context.startActivity(Intent.createChooser(shareIntent, "Export base config"))
    }
}