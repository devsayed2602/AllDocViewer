package com.docviewer.allinone.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentFileDao {

    @Query("SELECT * FROM recent_files ORDER BY lastOpened DESC LIMIT 50")
    fun getRecentFiles(): Flow<List<RecentFileEntity>>

    @Query("SELECT * FROM recent_files WHERE name LIKE '%' || :query || '%' ORDER BY lastOpened DESC")
    fun searchFiles(query: String): Flow<List<RecentFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: RecentFileEntity)

    @Delete
    suspend fun deleteFile(file: RecentFileEntity)

    @Query("DELETE FROM recent_files")
    suspend fun clearAll()
}
