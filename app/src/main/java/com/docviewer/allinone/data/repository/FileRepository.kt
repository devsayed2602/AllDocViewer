package com.docviewer.allinone.data.repository

import android.net.Uri
import com.docviewer.allinone.data.db.AppDatabase
import com.docviewer.allinone.data.db.RecentFileEntity
import com.docviewer.allinone.data.model.DocumentFile
import com.docviewer.allinone.data.model.DocumentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FileRepository(private val database: AppDatabase) {

    fun getRecentFiles(): Flow<List<DocumentFile>> {
        return database.recentFileDao().getRecentFiles().map { entities ->
            entities.map { it.toDocumentFile() }
        }
    }

    suspend fun getAllDocuments(context: android.content.Context): List<DocumentFile> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val documents = mutableListOf<DocumentFile>()
            val projection = arrayOf(
                android.provider.MediaStore.Files.FileColumns._ID,
                android.provider.MediaStore.Files.FileColumns.DISPLAY_NAME,
                android.provider.MediaStore.Files.FileColumns.MIME_TYPE,
                android.provider.MediaStore.Files.FileColumns.SIZE,
                android.provider.MediaStore.Files.FileColumns.DATE_MODIFIED
            )

            // Filter for documents based on user requested types (PDF, Word, Excel, PPT)
            val selection = "${android.provider.MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                    "${android.provider.MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                    "${android.provider.MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                    "${android.provider.MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                    "${android.provider.MediaStore.Files.FileColumns.MIME_TYPE} LIKE ?"

            val selectionArgs = arrayOf(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.%", // Matches docx, xlsx, pptx
                "application/vnd.ms-excel",
                "application/vnd.ms-powerpoint"
            )

            val sortOrder = "${android.provider.MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

            context.contentResolver.query(
                android.provider.MediaStore.Files.getContentUri("external"),
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns.DISPLAY_NAME)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns.MIME_TYPE)
                val sizeColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns.SIZE)
                val dateModifiedColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Unknown"
                    val mimeType = cursor.getString(mimeTypeColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateModified = cursor.getLong(dateModifiedColumn) * 1000L // Convert to ms

                    val uri = android.content.ContentUris.withAppendedId(
                        android.provider.MediaStore.Files.getContentUri("external"),
                        id
                    )

                    val documentType = DocumentType.fromMimeType(mimeType)
                    
                    // Filter out unknowns or unsupported texts (like code files if they somehow matched)
                    if (documentType != DocumentType.UNKNOWN) {
                        documents.add(
                            DocumentFile(
                                uri = uri,
                                name = name,
                                type = documentType,
                                size = size,
                                lastOpened = dateModified
                            )
                        )
                    }
                }
            }

            documents
        }
    }

    fun searchFiles(query: String): Flow<List<DocumentFile>> {
        return database.recentFileDao().searchFiles(query).map { entities ->
            entities.map { it.toDocumentFile() }
        }
    }

    suspend fun addRecentFile(file: DocumentFile) {
        database.recentFileDao().insertFile(file.toEntity())
    }

    suspend fun removeFile(file: DocumentFile) {
        database.recentFileDao().deleteFile(file.toEntity())
    }

    suspend fun clearHistory() {
        database.recentFileDao().clearAll()
    }

    private fun RecentFileEntity.toDocumentFile(): DocumentFile {
        return DocumentFile(
            uri = Uri.parse(uri),
            name = name,
            type = DocumentType.valueOf(type),
            size = size,
            lastOpened = lastOpened
        )
    }

    private fun DocumentFile.toEntity(): RecentFileEntity {
        return RecentFileEntity(
            uri = uri.toString(),
            name = name,
            type = type.name,
            size = size,
            lastOpened = lastOpened
        )
    }
}
