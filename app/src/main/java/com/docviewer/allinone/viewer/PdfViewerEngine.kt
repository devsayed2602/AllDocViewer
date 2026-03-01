package com.docviewer.allinone.viewer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PdfViewerEngine {

    private var renderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null

    val pageCount: Int get() = renderer?.pageCount ?: 0

    suspend fun load(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        close()

        // If it's a file:// URI, open directly. Otherwise copy to temp.
        val pfd = if (uri.scheme == "file") {
            val file = File(uri.path!!)
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        } else {
            context.contentResolver.openFileDescriptor(uri, "r")
                ?: throw IllegalStateException("Cannot open PDF file")
        }

        fileDescriptor = pfd
        renderer = PdfRenderer(pfd)
    }

    suspend fun renderPage(pageIndex: Int, scale: Float = 2.5f): Bitmap = withContext(Dispatchers.IO) {
        val pdfRenderer = renderer ?: throw IllegalStateException("PDF not loaded")

        val page = pdfRenderer.openPage(pageIndex)
        val width = (page.width * scale).toInt()
        val height = (page.height * scale).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.WHITE)

        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()

        bitmap
    }

    fun close() {
        try { renderer?.close() } catch (_: Exception) {}
        try { fileDescriptor?.close() } catch (_: Exception) {}
        renderer = null
        fileDescriptor = null
    }
}
