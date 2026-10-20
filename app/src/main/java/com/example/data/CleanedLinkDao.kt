package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class CleanStats(
    val totalCleaned: Int,
    val totalTrackersStripped: Int,
    val totalBytesSaved: Int
)

@Dao
interface CleanedLinkDao {
    @Query("SELECT * FROM cleaned_links ORDER BY timestamp DESC")
    fun getAllLinks(): Flow<List<CleanedLinkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: CleanedLinkEntity): Long

    @Delete
    suspend fun deleteLink(link: CleanedLinkEntity)

    @Query("DELETE FROM cleaned_links")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) as totalCleaned, COALESCE(SUM(strippedCount), 0) as totalTrackersStripped, COALESCE(SUM(bytesSaved), 0) as totalBytesSaved FROM cleaned_links")
    fun getStats(): Flow<CleanStats>
}
