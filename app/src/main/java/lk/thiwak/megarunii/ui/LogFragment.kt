package lk.thiwak.megarunii.ui

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.FileObserver
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

class LogFragment : Fragment(R.layout.fragment_log) {

    private var currentPosition: Long = 0
    private val chunkSize = 1024 * 5
    private val logFileName = "app_log.txt"
    private lateinit var logScrollView: ScrollView
    private lateinit var logView: TextView
    private lateinit var logBgDrawable: Drawable
    private lateinit var fileObserver: FileObserver
    val TAG: String = "LogFragment"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logScrollView = view.findViewById(R.id.logScrollView)
        logView = view.findViewById(R.id.logView)

        // Monitor scrolling to append chunks when at the bottom
        logScrollView.viewTreeObserver.addOnScrollChangedListener {
            val view = logScrollView.getChildAt(logScrollView.childCount - 1)
            val diff = (view.bottom - (logScrollView.height + logScrollView.scrollY))
            if (diff == 0) {
                appendLogChunk()
            }
        }

        appendLogChunk()

        logScrollView.post {
            //logScrollView.scrollTo(0, logScrollView.getChildAt(0).height)
            logScrollView.scrollTo(0, 0)
        }


        // Initialize FileObserver
        initFileObserver()
    }


    private fun initFileObserver() {
        val logFilePath = File(requireContext().filesDir, logFileName).absolutePath

        fileObserver = object : FileObserver(logFilePath, MODIFY) {
            override fun onEvent(event: Int, path: String?) {
                if (event == MODIFY) {
                    requireActivity().runOnUiThread {
                        appendLogChunk()
                    }
                }
            }
        }

        fileObserver.startWatching()
    }

    private fun appendLogChunk() {
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

                    logEntries.forEach { logEntry ->
                        if (logEntry.trim().isEmpty()) return@forEach

                        val (date, logLevel, message) = parseLogEntry(logEntry)

                        if (date.isEmpty() || logLevel.isEmpty() || message.isEmpty()) return@forEach

                        logBgDrawable = if (logView.lineCount % 2 == 0) {
                            ContextCompat.getDrawable(requireContext(), R.drawable.log_box_background_a)!!
                        } else {
                            ContextCompat.getDrawable(requireContext(), R.drawable.log_box_background_b)!!
                        }

                        val spannableMessage = SpannableString(logEntry)
                        spannableMessage.setSpan(
                            CustomBackgroundSpan(logBgDrawable),
                            0,
                            logEntry.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                        logView.append(spannableMessage)
                    }
                }
            } else {
                logView.text = "No logs available."
            }
        } catch (e: IOException) {
            e.printStackTrace()
            logView.text = "Error loading logs."
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
        fileObserver.stopWatching() // Stop watching the file when the activity is destroyed
    }
}

