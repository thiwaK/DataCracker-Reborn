package lk.thiwak.megarunii.ui

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import lk.thiwak.megarunii.R
import lk.thiwak.megarunii.log.CustomBackgroundSpan
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import android.util.Log
import android.util.TypedValue
import android.widget.LinearLayout
import kotlinx.coroutines.*

class LogFragment : Fragment(R.layout.fragment_log) {

    private var currentPosition: Long = 0
    private val chunkSize = 1024 * 5
    private val logFileName = "app_log.txt"
    private lateinit var logScrollView: ScrollView
    private lateinit var logContainer: LinearLayout
    private lateinit var fileObserver: FileObserver
    private lateinit var handler: Handler
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    val TAG: String = "LogFragment"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logScrollView = view.findViewById(R.id.logScrollView)
        logContainer = view.findViewById(R.id.logContainer) // LinearLayout for holding log entries

        handler = Handler(Looper.getMainLooper())

        // Monitor scrolling to append chunks when at the bottom
        logScrollView.viewTreeObserver.addOnScrollChangedListener {
            val view = logScrollView.getChildAt(logScrollView.childCount - 1)
            val diff = (view.bottom - (logScrollView.height + logScrollView.scrollY))
            if (diff == 0) {
                appendLogChunk()
            }
        }

        // Initial log append
        appendLogChunk()

        // Initialize FileObserver
        initFileObserver()
    }

    private fun initFileObserver() {
        val logFilePath = File(requireContext().filesDir, logFileName).absolutePath

        fileObserver = object : FileObserver(logFilePath, MODIFY) {
            override fun onEvent(event: Int, path: String?) {
                if (event == MODIFY) {
                    handler.post {
                        appendLogChunk()
                    }
                }
            }
        }

        fileObserver.startWatching()
    }

    private fun appendLogChunk() {
        // Use Coroutine to handle file reading in the background
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val logFile = File(requireContext().filesDir, logFileName)
                if (logFile.exists()) {
                    RandomAccessFile(logFile, "r").use { reader ->
                        if (currentPosition == 0L) {
                            currentPosition = reader.length()
                        }

                        val newPosition = (currentPosition - chunkSize).coerceAtLeast(0)
                        val sizeToRead = (currentPosition - newPosition).toInt()

                        reader.seek(newPosition)
                        val buffer = ByteArray(sizeToRead)
                        reader.readFully(buffer)

                        currentPosition = newPosition

                        val newContent = String(buffer)
                        val logEntries = newContent.split("\n").reversed()

                        withContext(Dispatchers.Main) {
                            logEntries.forEach { logEntry ->
                                if (logEntry.trim().isEmpty()) return@forEach

                                val (date, logLevel, message) = parseLogEntry(logEntry)

                                if (date.isEmpty() || logLevel.isEmpty() || message.isEmpty()) return@forEach

                                // Set colors based on log level
                                var msgTextColor = "#CCCCCC" // Default message text color
                                var dateTextColor = "#808080" // Default datetime text color

                                when (logLevel) {
                                    "I" -> {
                                        msgTextColor = "#2481d1" // Info color
                                        dateTextColor = "#4682B4" // Info datetime color
                                    }
                                    "E" -> {
                                        msgTextColor = "#FF4444" // Error color
                                        dateTextColor = "#8B0000" // Error datetime color
                                    }
                                    "W" -> {
                                        msgTextColor = "#FFFF00" // Warning color
                                        dateTextColor = "#B8860B" // Warning datetime color
                                    }
                                }

                                // Create a new TextView for the log entry
                                val logTextView = TextView(requireContext())
                                val formattedLog = "<font color=\"$dateTextColor\">$date</font> <font color=\"$msgTextColor\">$message</font>"
                                logTextView.text = Html.fromHtml(formattedLog) // HTML formatting for date and message

                                logTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)

                                // Set background color based on even/odd lines
                                val backgroundDrawable = if (logContainer.childCount % 2 == 0) {
                                    ContextCompat.getDrawable(requireContext(), R.drawable.log_box_background_a)!!
                                } else {
                                    ContextCompat.getDrawable(requireContext(), R.drawable.log_box_background_b)!!
                                }

                                logTextView.background = backgroundDrawable

                                logContainer.addView(logTextView)

                                // Scroll to the bottom smoothly after appending logs
                                logScrollView.smoothScrollTo(0, logScrollView.bottom)
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        val emptyTextView = TextView(requireContext())
                        emptyTextView.text = "No logs available."
                        logContainer.addView(emptyTextView)
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    val errorTextView = TextView(requireContext())
                    errorTextView.text = "Error loading logs."
                    logContainer.addView(errorTextView)
                }
            }
        }
    }

    private fun parseLogEntry(logEntry: String): Triple<String, String, String> {
        val regex = """\[(.*?)\] \[(.*?)\] (.*)""".toRegex()
        val matchResult = regex.matchEntire(logEntry)

        return matchResult?.let {
            val (datetime, level, message) = it.destructured
            Triple(datetime, level, message)
        } ?: Triple("", "", "")
    }

    override fun onDestroy() {
        super.onDestroy()
        fileObserver.stopWatching() // Stop watching the file when the fragment is destroyed
        coroutineScope.cancel() // Cancel coroutine scope to avoid leaks
    }
}
