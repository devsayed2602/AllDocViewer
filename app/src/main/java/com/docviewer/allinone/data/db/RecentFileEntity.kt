package com.docviewer.allinone.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_files")
data class RecentFileEntity(
    @PrimaryKey
    val uri: String,
    val name: String,
    val type: String,
    val size: Long,
    val lastOpened: Long
)
