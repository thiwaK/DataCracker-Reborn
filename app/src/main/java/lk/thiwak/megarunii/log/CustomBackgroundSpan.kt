package lk.thiwak.megarunii.log

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.*
import android.text.style.ReplacementSpan
import android.util.Log

class CustomBackgroundSpan(private val drawable: Drawable) : ReplacementSpan() {

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        return paint.measureText(text, start, end).toInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val parentWidth = canvas.width.toFloat()

        // Draw the background drawable
        drawable.setBounds(0, top, parentWidth.toInt(), bottom)
        drawable.draw(canvas)

        // Parse and format the log entry
        val formattedText = htmlFormatter(text.substring(start, end))

        // Use the older StaticLayout constructor
        val staticLayout = StaticLayout(
            formattedText,
            TextPaint(paint),
            parentWidth.toInt(),
            Layout.Alignment.ALIGN_NORMAL,
            1.0f, // line spacing multiplier
            0.0f, // line spacing extra
            false // include padding
        )

        canvas.save()
        canvas.translate(x, top.toFloat()) // Position text within the drawable
        staticLayout.draw(canvas)
        canvas.restore()
    }

    private fun parseLogEntry(logEntry: String): Triple<String, String, String> {
        val regex = """\[(.*?)\] \[(.*?)\] (.*)""".toRegex()
        val matchResult = regex.matchEntire(logEntry)

        return matchResult?.let {
            val (datetime, level, message) = it.destructured
            Log.i("MM", "$datetime, $level, $message")
            Triple(datetime, level, message)
        } ?: Triple("", "", "")
    }

    private fun htmlFormatter(logEntry: String): Spanned {
        val (datetime, logLevel, message) = parseLogEntry(logEntry)

        var msgTextColor = "#CCCCCC" // Default message text color
        var dateTextColor = "#808080" // Default datetime text color

        if (logLevel == "I") {
            msgTextColor = "#2481d1" // Info color
            dateTextColor = "#4682B4" // Info datetime color
        } else if (logLevel == "E") {
            msgTextColor = "#FF4444" // Error color
            dateTextColor = "#8B0000" // Error datetime color
        } else if (logLevel == "W") {
            msgTextColor = "#FFFF00" // Warning color
            dateTextColor = "#B8860B" // Warning datetime color
        }

        // Use older Html.fromHtml for backward compatibility
        @Suppress("DEPRECATION")
        return Html.fromHtml(
            "<p style='font-family:monospace;'>" +
                    "<span style=\"color:$dateTextColor;\">  $datetime  </span> " +
                    "<span style=\"color:$msgTextColor; font-size:8px;\">$message</span>" +
                    "</p>"
        )
    }
}

