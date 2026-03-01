package com.docviewer.allinone.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.docviewer.allinone.data.db.AppDatabase
import com.docviewer.allinone.data.model.DocumentFile
import com.docviewer.allinone.data.repository.FileRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RecentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FileRepository(AppDatabase.getInstance(application))

    val recentFiles: StateFlow<List<DocumentFile>> = repository.getRecentFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeFile(file: DocumentFile) {
        viewModelScope.launch {
            repository.removeFile(file)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
