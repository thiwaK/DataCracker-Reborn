package lk.thiwak.megarunii.ui

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import lk.thiwak.megarunii.*
import lk.thiwak.megarunii.log.LogReceiver
import lk.thiwak.megarunii.log.Logger
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    val TAG: String = "MainActivity"
    private lateinit var logReceiver: LogReceiver
    private lateinit var fab: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        generateClientUniqueId();

        // bottom nav
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }

        // bottom nav listener
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {

                R.id.nav_home -> {
                    Log.i(TAG, "HOME")
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.nav_log -> {
                    Log.i(TAG, "LOG")
                    replaceFragment(LogFragment())
                    true
                }
                else -> false
            }
        }

        // Initialize and set up the toolbar and fab
        fab = findViewById(R.id.fab)
        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // log receiver
        logReceiver = LogReceiver()
        registerReceiver(logReceiver, IntentFilter(Utils.LOG_INTENT_ACTION))

        // fab icon set
        if (isServiceRunning(BackgroundService::class.java, this)) {
            fab.setImageResource(R.drawable.ic_baseline_stop_circle_24) // Service is running
            Logger.debug(this, "FAB: set action to stop")
        } else {
            fab.setImageResource(R.drawable.ic_baseline_play_circle_24) // Service is not running
            Logger.debug(this, "FAB: set action to start")
        }

        // fab action set
        fab.setOnClickListener {
            if (isServiceRunning(BackgroundService::class.java, this)) {
                // If service is running, stop the service
                stopService()
            } else {
                // If service is not running, start the service

                startService()
            }
        }


    }

    fun shareDeviceInfo_(context: Context) {

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

    fun generateClientUniqueId(): String? {
        // Get the unique Android ID

        val androidId: String = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        Log.i("Utils", androidId)




        // Optionally, you can append other device information for more uniqueness
        return androidId + "-" + Build.MODEL + "-" + Build.VERSION.RELEASE
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.action_load_config -> {
                // Handle load config action
                true
            }
            R.id.action_reset_config -> {
                // Handle reset config action
                true
            }
            R.id.action_export_base_config -> {
                // Handle export base config action
                Utils.exportBaseConfig(this)
                true
            }
        }

        return super.onOptionsItemSelected(item)
    }


    private fun isServiceRunning(serviceClass: Class<out Service>, context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun startService(){
        Logger.debug(this, "Action: start service")
        startService(Intent(this, BackgroundService::class.java))
        fab.setImageResource(R.drawable.ic_baseline_stop_circle_24) // Change to stop icon
        Toast.makeText(this, "Service Started", Toast.LENGTH_SHORT).show()

    }

    private fun stopService(){
        Logger.debug(this, "Action: stop service")

        val stopIntent = Intent()
        stopIntent.action = BackgroundService.STOP_SERVICE_INTENT_ACTION
        sendBroadcast(stopIntent)

        fab.setImageResource(R.drawable.ic_baseline_play_circle_24) // Change to play icon
        Toast.makeText(this, "Service Stopped", Toast.LENGTH_SHORT).show()
    }



}

