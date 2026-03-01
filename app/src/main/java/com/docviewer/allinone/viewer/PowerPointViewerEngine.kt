package com.docviewer.allinone.viewer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xslf.usermodel.XSLFTextShape
import java.io.File
import java.io.FileInputStream

data class SlideContent(
    val title: String,
    val bodyText: List<String>
)

class PowerPointViewerEngine {

    private var slides: List<SlideContent> = emptyList()

    val slideCount: Int get() = slides.size

    fun getSlides(): List<SlideContent> = slides

    suspend fun loadFromFile(file: File) = withContext(Dispatchers.IO) {
        slides = if (file.name.endsWith(".pptx", ignoreCase = true)) {
            loadPptx(file)
        } else {
            listOf(
                SlideContent(
                    title = "Unsupported Format",
                    bodyText = listOf("The legacy .ppt format is not supported. Please convert to .pptx using Microsoft PowerPoint or LibreOffice.")
                )
            )
        }
    }

    private fun loadPptx(file: File): List<SlideContent> {
        val inputStream = FileInputStream(file)
        val pptx = XMLSlideShow(inputStream)
        val result = pptx.slides.map { slide ->
            val title = slide.title ?: ""
            val bodyTexts = mutableListOf<String>()
            slide.shapes.forEach { shape ->
                if (shape is XSLFTextShape) {
                    val text = shape.text
                    if (text.isNotBlank() && text != title) {
                        bodyTexts.add(text)
                    }
                }
            }
            SlideContent(title = title, bodyText = bodyTexts)
        }
        pptx.close()
        inputStream.close()
        return result
    }

    suspend fun renderSlide(index: Int, width: Int = 960, height: Int = 720): Bitmap =
        withContext(Dispatchers.IO) {
            val slide = slides.getOrNull(index)
                ?: throw IndexOutOfBoundsException("Slide index $index out of range")

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1A1A2E")
                textSize = 42f
                typeface = Typeface.DEFAULT_BOLD
            }

            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#333333")
                textSize = 28f
                typeface = Typeface.DEFAULT
            }

            // Draw gradient header bar
            val headerPaint = Paint().apply {
                shader = android.graphics.LinearGradient(
                    0f, 0f, width.toFloat(), 80f,
                    Color.parseColor("#7C4DFF"),
                    Color.parseColor("#448AFF"),
                    android.graphics.Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), 6f, headerPaint)

            var y = 80f
            if (slide.title.isNotBlank()) {
                canvas.drawText(slide.title, 40f, y, titlePaint)
                y += 60f
            }

            val linePaint = Paint().apply {
                color = Color.parseColor("#E0E0E0")
                strokeWidth = 2f
            }
            canvas.drawLine(40f, y, width - 40f, y, linePaint)
            y += 40f

            slide.bodyText.forEach { text ->
                val lines = wrapText(text, bodyPaint, width - 80f)
                lines.forEach { line ->
                    if (y < height - 40f) {
                        canvas.drawText(line, 40f, y, bodyPaint)
                        y += 38f
                    }
                }
                y += 12f
            }

            bitmap
        }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""
        for (word in words) {
            val test = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(test) <= maxWidth) {
                currentLine = test
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)
        return lines
    }

    fun close() {
        slides = emptyList()
    }
}
