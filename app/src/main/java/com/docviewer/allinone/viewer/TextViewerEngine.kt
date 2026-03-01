package com.docviewer.allinone.viewer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.Charset

class TextViewerEngine {

    private var content: String = ""

    fun getContent(): String = content

    suspend fun loadFromFile(file: File) = withContext(Dispatchers.IO) {
        content = try {
            // Read up to 2MB to prevent OOM on massive text files
            val maxBytesToRead = 2 * 1024 * 1024
            if (file.length() > maxBytesToRead) {
                file.inputStream().reader(Charset.defaultCharset()).use { reader ->
                    val buffer = CharArray(maxBytesToRead)
                    val read = reader.read(buffer)
                    String(buffer, 0, read) + "\n\n... (File truncated due to size)"
                }
            } else {
                file.readText(Charset.defaultCharset())
            }
        } catch (e: Exception) {
            "Error reading text file: ${e.message}"
        }
    }

    fun close() {
        content = ""
    }
}
