package com.docviewer.allinone.data.model

import android.net.Uri

enum class DocumentType(val displayName: String, val extensions: List<String>) {
    PDF("PDF", listOf("pdf")),
    DOC("Word", listOf("doc", "docx")),
    XLS("Excel", listOf("xls", "xlsx")),
    PPT("PowerPoint", listOf("ppt", "pptx")),
    TXT("Text", listOf("txt")),
    CSV("CSV", listOf("csv")),
    IMAGE("Image", listOf("jpg", "jpeg", "png", "webp", "bmp", "gif")),
    UNKNOWN("Unknown", emptyList());

    companion object {
        fun fromExtension(ext: String): DocumentType {
            val lower = ext.lowercase()
            return entries.find { lower in it.extensions } ?: UNKNOWN
        }

        fun fromMimeType(mimeType: String?): DocumentType {
            if (mimeType == null) return UNKNOWN
            return when {
                mimeType.contains("pdf") -> PDF
                mimeType.contains("word") || mimeType.contains("officedocument.wordprocessingml") -> DOC
                mimeType.contains("excel") || mimeType.contains("spreadsheetml") -> XLS
                mimeType.contains("powerpoint") || mimeType.contains("presentationml") -> PPT
                mimeType.contains("text/plain") -> TXT
                mimeType.contains("text/csv") -> CSV
                mimeType.startsWith("image/") -> IMAGE
                else -> UNKNOWN
            }
        }
    }
}

data class DocumentFile(
    val uri: Uri,
    val name: String,
    val type: DocumentType,
    val size: Long = 0L,
    val lastOpened: Long = System.currentTimeMillis()
)
