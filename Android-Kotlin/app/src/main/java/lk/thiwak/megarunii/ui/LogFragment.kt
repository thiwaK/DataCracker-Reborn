package lk.thiwak.megarunii.ui


import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import lk.thiwak.megarunii.R
import android.util.TypedValue
import android.widget.LinearLayout
import kotlinx.coroutines.*
import lk.thiwak.megarunii.DatabaseHelper
import java.text.SimpleDateFormat
import java.util.*

class LogFragment : Fragment(R.layout.fragment_log) {

    private lateinit var logScrollView: ScrollView
    private lateinit var logContainer: LinearLayout
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var dbHelper: DatabaseHelper? = null

    private val PAGE_SIZE = 100
    private var offset = 0
    private var isLoading = false
    private var allLogsLoaded = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logScrollView = view.findViewById(R.id.logScrollView)
        logContainer = view.findViewById(R.id.logContainer)

        dbHelper = DatabaseHelper.getInstance(requireContext())

        // Load the first set of logs
        loadLogsFromDatabase(initialLoad = true)

        // Monitor scrolling to load more logs when needed
//        logScrollView.viewTreeObserver.addOnScrollChangedListener {
//            if (!isLoading && !allLogsLoaded) {
//                val totalHeight = logContainer.height
//                val currentScrollY = logScrollView.scrollY
//                val visibleHeight = logScrollView.height
//                val remainingItems = totalHeight - (currentScrollY + visibleHeight)
//
//                if (remainingItems < 50 * dpToPx(20)) { // If 50 logs are left to be seen
//                    loadLogsFromDatabase(initialLoad = false)
//                }
//            }
//        }

        logScrollView.viewTreeObserver.addOnScrollChangedListener {
            val view = logScrollView.getChildAt(logScrollView.childCount - 1)
            val diff = (view.bottom - (logScrollView.height + logScrollView.scrollY))
            if (diff == 0) {
                loadLogsFromDatabase(initialLoad = false)
            }
        }
    }

    private fun getStrTime(milliseconds: Long): String {
        val dateFormat = SimpleDateFormat("MM-DD HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date(milliseconds))
    }

    private fun loadLogsFromDatabase(initialLoad: Boolean) {
        if (isLoading) return
        isLoading = true

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val logEntries = dbHelper?.getLogs(PAGE_SIZE, offset) ?: emptyList()

                withContext(Dispatchers.Main) {
                    if (logEntries.isEmpty()) {
                        allLogsLoaded = true
                    } else {
                        offset += logEntries.size

                        if (initialLoad) logContainer.removeAllViews()

                        logEntries.forEach { logEntry ->

                            var msgTextColor = "#CCCCCC" // Default message text color
                            var dateTextColor = "#808080" // Default datetime text color

                            if (logEntry.logType[0].uppercase() == "I") {
                                msgTextColor = "#2481d1" // Info color
                                dateTextColor = "#4682B4" // Info datetime color
                            } else if (logEntry.logType[0].uppercase()  == "E") {
                                msgTextColor = "#FF4444" // Error color
                                dateTextColor = "#8B0000" // Error datetime color
                            } else if (logEntry.logType[0].uppercase()  == "W") {
                                msgTextColor = "#FFFF00" // Warning color
                                dateTextColor = "#B8860B" // Warning datetime color
                            }


                            val formattedLog = "<font color=\"$dateTextColor\">${getStrTime(logEntry.timestamp)}</font> <font color=\"$msgTextColor\">${logEntry.logMessage}</font>"
                            val logTextView = TextView(requireContext())
                            logTextView.text = Html.fromHtml(formattedLog)
                            logTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)

                            val backgroundDrawable = if (logContainer.childCount % 2 == 0) {
                                ContextCompat.getDrawable(requireContext(), R.drawable.log_box_background_a)!!
                            } else {
                                ContextCompat.getDrawable(requireContext(), R.drawable.log_box_background_b)!!
                            }
                            logTextView.background = backgroundDrawable

                            logContainer.addView(logTextView)
                        }

                        if (initialLoad) {
                            logScrollView.smoothScrollTo(0, logScrollView.bottom) // Scroll to bottom on first load
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    val errorTextView = TextView(requireContext())
                    errorTextView.text = "Error loading logs."
                    logContainer.addView(errorTextView)
                }
            } finally {
                isLoading = false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
