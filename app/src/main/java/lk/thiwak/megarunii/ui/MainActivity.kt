package lk.thiwak.megarunii.ui

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import lk.thiwak.megarunii.*
import lk.thiwak.megarunii.log.LogReceiver
import lk.thiwak.megarunii.log.Logger
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var logReceiver: LogReceiver
    private lateinit var fab: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        class LogFragment : Fragment(R.layout.fragment_log)

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
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.nav_log -> {
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

