package com.docviewer.allinone.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.docviewer.allinone.data.db.AppDatabase
import com.docviewer.allinone.data.model.DocumentFile
import com.docviewer.allinone.data.repository.FileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FileRepository(AppDatabase.getInstance(application))

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _allDocuments = MutableStateFlow<List<DocumentFile>>(emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val files: StateFlow<List<DocumentFile>> = combine(_searchQuery, _allDocuments) { query, allFiles ->
        if (query.isBlank()) {
            allFiles
        } else {
            allFiles.filter { it.name.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun scanLocalDocuments(context: android.content.Context) {
        viewModelScope.launch {
            val docs = repository.getAllDocuments(context)
            _allDocuments.value = docs
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addRecentFile(context: android.content.Context, originalUri: android.net.Uri, name: String, type: com.docviewer.allinone.data.model.DocumentType, size: Long, onComplete: (android.net.Uri) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Emulate ExcelReader-master fix: copy file to internal secure storage (filesDir)
                // This eliminates ALL Android URI permission issues for future access.
                val dir = java.io.File(context.filesDir, "documents")
                if (!dir.exists()) dir.mkdirs()
                
                val localFile = java.io.File(dir, "${System.currentTimeMillis()}_$name")
                
                context.contentResolver.openInputStream(originalUri)?.use { input ->
                    localFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                val localUri = android.net.Uri.fromFile(localFile)
                
                val docFile = DocumentFile(
                    uri = localUri,
                    name = name,
                    type = type,
                    size = size
                )
                repository.addRecentFile(docFile)
                
                // Switch to Main thread to invoke callback
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(localUri)
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Failed to copy file to internal storage", e)
                // Fallback to original URI if copy fails
                val docFile = DocumentFile(
                    uri = originalUri,
                    name = name,
                    type = type,
                    size = size
                )
                repository.addRecentFile(docFile)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(originalUri)
                }
            }
        }
    }
}
