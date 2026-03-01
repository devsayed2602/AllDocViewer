package com.docviewer.allinone.viewer

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileInputStream

data class WordContent(
    val paragraphs: List<WordParagraph>
)

data class WordParagraph(
    val text: String,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val fontSize: Int = 12,
    val isHeading: Boolean = false
)

class WordViewerEngine {

    private var content: WordContent? = null

    fun getContent(): WordContent = content ?: WordContent(emptyList())

    suspend fun loadFromFile(file: File) = withContext(Dispatchers.IO) {
        content = if (file.name.endsWith(".docx", ignoreCase = true)) {
            loadDocx(file)
        } else {
            WordContent(
                listOf(
                    WordParagraph(
                        text = "The legacy .doc format is not supported. Please convert to .docx using Microsoft Word or LibreOffice.",
                        isBold = true
                    )
                )
            )
        }
    }

    private fun loadDocx(file: File): WordContent {
        val inputStream = FileInputStream(file)
        val document = XWPFDocument(inputStream)
        val paragraphs = document.paragraphs.map { para ->
            val isBold = para.runs.any { it.isBold }
            val isItalic = para.runs.any { it.isItalic }
            val fontSize = para.runs.firstOrNull()?.fontSize?.takeIf { it > 0 } ?: 12
            val isHeading = para.style?.startsWith("Heading") == true

            WordParagraph(
                text = para.text,
                isBold = isBold,
                isItalic = isItalic,
                fontSize = fontSize,
                isHeading = isHeading
            )
        }.filter { it.text.isNotBlank() }

        document.close()
        inputStream.close()
        return WordContent(paragraphs)
    }

    fun close() {
        content = null
    }
}
