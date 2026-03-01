package com.docviewer.allinone.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.docviewer.allinone.data.model.DocumentType
import com.docviewer.allinone.viewer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class ViewerState {
    data object Loading : ViewerState()
    data class PdfLoaded(val pageCount: Int) : ViewerState()
    data class WordLoaded(val content: WordContent) : ViewerState()
    data class ExcelLoaded(val content: ExcelContent) : ViewerState()
    data class PptLoaded(val slideCount: Int) : ViewerState()
    data class TxtLoaded(val content: String) : ViewerState()
    data class CsvLoaded(val content: CsvContent) : ViewerState()
    data class ImageLoaded(val cachedUri: Uri) : ViewerState()
    data class Error(val message: String) : ViewerState()
}

class DocumentViewModel(application: Application) : AndroidViewModel(application) {

    private val pdfEngine = PdfViewerEngine()
    private val wordEngine = WordViewerEngine()
    private val excelEngine = ExcelViewerEngine()
    private val pptEngine = PowerPointViewerEngine()
    private val textEngine = TextViewerEngine()
    private val csvEngine = CsvViewerEngine()

    private val _state = MutableStateFlow<ViewerState>(ViewerState.Loading)
    val state: StateFlow<ViewerState> = _state.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _currentBitmap = MutableStateFlow<Bitmap?>(null)
    val currentBitmap: StateFlow<Bitmap?> = _currentBitmap.asStateFlow()

    private val _fileName = MutableStateFlow("")
    val fileName: StateFlow<String> = _fileName.asStateFlow()

    private val _documentType = MutableStateFlow(DocumentType.UNKNOWN)
    val documentType: StateFlow<DocumentType> = _documentType.asStateFlow()

    private var totalPages = 0
    private var tempFile: File? = null

    /**
     * Copy content:// URI to a temp file in app cache.
     * This avoids all URI permission issues.
     */
    private fun copyUriToCache(uri: Uri): File {
        val context = getApplication<Application>()
        val fileName = DocumentViewerFactory.getFileName(context, uri).ifBlank { "document" }
        val cacheFile = File(context.cacheDir, "doc_viewer_$fileName")

        context.contentResolver.openInputStream(uri)?.use { input ->
            cacheFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Cannot read the file. Please try opening it again using the + button.")

        return cacheFile
    }

    fun loadDocument(uri: Uri) {
        val context = getApplication<Application>()
        _state.value = ViewerState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val type = DocumentViewerFactory.getDocumentType(context, uri)
                _documentType.value = type
                _fileName.value = DocumentViewerFactory.getFileName(context, uri)

                // Copy content:// files to cache to avoid permission issues,
                // or use file:// directly if we already copied it to internal storage.
                val cachedFile = if (uri.scheme == "file" && uri.path != null) {
                    java.io.File(uri.path!!)
                } else {
                    val file = copyUriToCache(uri)
                    tempFile = file // Mark for deletion on cleanup
                    file
                }
                val cachedUri = Uri.fromFile(cachedFile)

                when (type) {
                    DocumentType.PDF -> {
                        pdfEngine.load(context, cachedUri)
                        totalPages = pdfEngine.pageCount
                        _state.value = ViewerState.PdfLoaded(totalPages)
                        if (totalPages > 0) renderPdfPage(0)
                    }
                    DocumentType.DOC -> {
                        wordEngine.loadFromFile(cachedFile)
                        _state.value = ViewerState.WordLoaded(wordEngine.getContent())
                    }
                    DocumentType.XLS -> {
                        excelEngine.loadFromFile(cachedFile)
                        _state.value = ViewerState.ExcelLoaded(excelEngine.getContent())
                    }
                    DocumentType.PPT -> {
                        pptEngine.loadFromFile(cachedFile)
                        totalPages = pptEngine.slideCount
                        _state.value = ViewerState.PptLoaded(totalPages)
                        if (totalPages > 0) renderSlidePage(0)
                    }
                    DocumentType.TXT -> {
                        textEngine.loadFromFile(cachedFile)
                        _state.value = ViewerState.TxtLoaded(textEngine.getContent())
                    }
                    DocumentType.CSV -> {
                        csvEngine.loadFromFile(cachedFile)
                        _state.value = ViewerState.CsvLoaded(csvEngine.getContent())
                    }
                    DocumentType.IMAGE -> {
                        _state.value = ViewerState.ImageLoaded(cachedUri)
                    }
                    DocumentType.UNKNOWN -> {
                        _state.value = ViewerState.Error("This file format is not supported.")
                    }
                }
            } catch (e: Throwable) {
                Log.e("DocumentViewModel", "Error loading document", e)
                _state.value = ViewerState.Error(
                    "Failed to load document: ${e.localizedMessage ?: e.javaClass.simpleName}"
                )
            }
        }
    }

    private suspend fun renderPdfPage(page: Int) {
        try {
            val bitmap = pdfEngine.renderPage(page)
            _currentBitmap.value = bitmap
            _currentPage.value = page
        } catch (e: Throwable) {
            Log.e("DocumentViewModel", "Error rendering PDF page", e)
            _state.value = ViewerState.Error("Failed to render page: ${e.localizedMessage ?: e.javaClass.simpleName}")
        }
    }

    private suspend fun renderSlidePage(page: Int) {
        try {
            val bitmap = pptEngine.renderSlide(page)
            _currentBitmap.value = bitmap
            _currentPage.value = page
        } catch (e: Throwable) {
            Log.e("DocumentViewModel", "Error rendering slide", e)
            _state.value = ViewerState.Error("Failed to render slide: ${e.localizedMessage ?: e.javaClass.simpleName}")
        }
    }

    fun nextPage() {
        if (_currentPage.value < totalPages - 1) {
            viewModelScope.launch(Dispatchers.IO) {
                val next = _currentPage.value + 1
                when (_state.value) {
                    is ViewerState.PdfLoaded -> renderPdfPage(next)
                    is ViewerState.PptLoaded -> renderSlidePage(next)
                    else -> {}
                }
            }
        }
    }

    fun previousPage() {
        if (_currentPage.value > 0) {
            viewModelScope.launch(Dispatchers.IO) {
                val prev = _currentPage.value - 1
                when (_state.value) {
                    is ViewerState.PdfLoaded -> renderPdfPage(prev)
                    is ViewerState.PptLoaded -> renderSlidePage(prev)
                    else -> {}
                }
            }
        }
    }

    fun goToPage(page: Int) {
        if (page in 0 until totalPages) {
            viewModelScope.launch(Dispatchers.IO) {
                when (_state.value) {
                    is ViewerState.PdfLoaded -> renderPdfPage(page)
                    is ViewerState.PptLoaded -> renderSlidePage(page)
                    else -> {}
                }
            }
        }
    }

    fun getTotalPages(): Int = totalPages

    override fun onCleared() {
        super.onCleared()
        try { pdfEngine.close() } catch (_: Throwable) {}
        try { wordEngine.close() } catch (_: Throwable) {}
        try { excelEngine.close() } catch (_: Throwable) {}
        try { pptEngine.close() } catch (_: Throwable) {}
        try { tempFile?.delete() } catch (_: Throwable) {}
    }
}
