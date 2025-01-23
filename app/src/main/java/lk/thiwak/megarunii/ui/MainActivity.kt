package lk.thiwak.megarunii.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import lk.thiwak.megarunii.BackgroundService
import lk.thiwak.megarunii.R
import lk.thiwak.megarunii.Utils
import lk.thiwak.megarunii.log.LogReceiver
import lk.thiwak.megarunii.log.Logger
import java.io.*
import java.util.*


class MainActivity : AppCompatActivity() {

    val TAG: String = "MainActivity"
    private lateinit var logReceiver: LogReceiver
    private lateinit var fab: FloatingActionButton
    private val REQUEST_CODE_PICK_FILE = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!arePermissionsGranted()) {
            requestPermissions()
        }
//        else{
//            Toast.makeText(applicationContext, "You are good to go!", Toast.LENGTH_SHORT).show()
//        }

//        parseConfig();


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

    private fun parseConfig() {

        val config = Utils.getCoreConfiguration(applicationContext)
        if (config == null) {
            Toast.makeText(applicationContext, "Ask for new configuration", Toast.LENGTH_LONG).show()
            return
        }
    }

    @SuppressLint("HardwareIds")
    private fun generateClientUniqueId(): String? {
        val androidId: String = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        Log.i("Utils", androidId)
        return androidId
    }



    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            requestCode -> if (grantResults.isNotEmpty()) {
                if (grantResults[0] === PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(applicationContext, "Permission granted", Toast.LENGTH_LONG)
                        .show()
                } else {
                    Toast.makeText(applicationContext, "Permission denied", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (permissions[Manifest.permission.READ_MEDIA_IMAGES] == true ||
                permissions[Manifest.permission.READ_MEDIA_AUDIO] == true ||
                permissions[Manifest.permission.READ_MEDIA_VIDEO] == true) {
                // perform task`
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
            }
        } else {
            if (permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true &&
                permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true) {
                // perform task`
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestPermissions() {
        val permissionsToRequest = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_PHONE_STATE
                )
            } else -> {
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_PHONE_STATE
                )
            }
        }
        requestPermissionLauncher.launch(permissionsToRequest)
    }

    private fun arePermissionsGranted(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            }
            else -> {
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        }
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
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "application/octet-stream"
                startActivityForResult(intent, REQUEST_CODE_PICK_FILE)
            }
            R.id.action_reset_config -> {
                File(applicationContext.filesDir, "configuration.bin").delete()
                File(applicationContext.filesDir, "config.json").delete()
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



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_PICK_FILE) {
            data?.data?.let { uri ->
                val inputStream = contentResolver.openInputStream(uri)
                val destinationFile = File(applicationContext.filesDir, "configuration.bin")

                try {
                    val outputStream = FileOutputStream(destinationFile)
                    inputStream?.copyTo(outputStream)
                    Utils.unpackConfig(applicationContext)
                    Toast.makeText(this, "Configuration file loaded successfully", Toast.LENGTH_SHORT).show()

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Failed to load configuration file", Toast.LENGTH_SHORT).show()
                } finally {
                    inputStream?.close()
                }
            }
        }
    }

}

