package com.docviewer.allinone.viewer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.Charset

data class CsvContent(
    val headers: List<String>,
    val rows: List<List<String>>
)

class CsvViewerEngine {

    private var content: CsvContent? = null

    fun getContent(): CsvContent = content ?: CsvContent(emptyList(), emptyList())

    suspend fun loadFromFile(file: File) = withContext(Dispatchers.IO) {
        try {
            val lines = file.readLines(Charset.defaultCharset())
            if (lines.isEmpty()) {
                content = CsvContent(emptyList(), emptyList())
                return@withContext
            }

            // Very basic CSV parser that splits by comma
            // Does not handle quoted commas properly, but is fast for standard CSV files
            val rows = lines.map { line ->
                line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
                    .map { it.trim('\"', ' ') }
            }

            val headers = rows.firstOrNull() ?: emptyList()
            val dataRows = if (rows.size > 1) rows.drop(1) else emptyList()

            content = CsvContent(headers = headers, rows = dataRows)

        } catch (e: Exception) {
            content = CsvContent(
                headers = listOf("Error"),
                rows = listOf(listOf("Failed to parse CSV: ${e.message}"))
            )
        }
    }

    fun close() {
        content = null
    }
}
