package com.docviewer.allinone.viewer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.docviewer.allinone.data.model.DocumentType
import java.io.File

object DocumentViewerFactory {

    fun getFileExtension(context: Context, uri: Uri): String {
        return getFileName(context, uri).substringAfterLast('.', "").lowercase()
    }

    fun getFileName(context: Context, uri: Uri): String {
        if (uri.scheme == "file") {
            return uri.path?.let { File(it).name } ?: "Unknown"
        }
        
        var name = "Unknown"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    fun getFileSize(context: Context, uri: Uri): Long {
        if (uri.scheme == "file") {
            return uri.path?.let { File(it).length() } ?: 0L
        }
        
        var size = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst() && sizeIndex >= 0) {
                size = cursor.getLong(sizeIndex)
            }
        }
        return size
    }

    fun getDocumentType(context: Context, uri: Uri): DocumentType {
        // For content:// URIs try MIME type first
        if (uri.scheme == "content") {
            val mimeType = context.contentResolver.getType(uri)
            val fromMime = DocumentType.fromMimeType(mimeType)
            if (fromMime != DocumentType.UNKNOWN) return fromMime
        }

        // Fall back to extension for both file:// and content:// URIs where MIME fails
        val ext = getFileExtension(context, uri)
        return DocumentType.fromExtension(ext)
    }

    fun isSupported(type: DocumentType): Boolean {
        return type != DocumentType.UNKNOWN
    }
}
