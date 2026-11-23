package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoHistoryDao {
    @Query("SELECT * FROM video_history WHERE userId = :userId ORDER BY createdAt DESC LIMIT 50")
    fun observeHistory(userId: String): Flow<List<VideoHistoryEntity>>

    @Query("SELECT * FROM video_history WHERE userId = :userId ORDER BY createdAt DESC LIMIT 50")
    suspend fun getHistoryForUser(userId: String): List<VideoHistoryEntity>

    @Query("SELECT * FROM video_history WHERE userId = '' ORDER BY createdAt DESC LIMIT 50")
    suspend fun getLegacyUnassignedHistory(): List<VideoHistoryEntity>

    @Query("UPDATE video_history SET userId = :targetUserId WHERE userId = ''")
    suspend fun claimLegacyUnassignedHistory(targetUserId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: VideoHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<VideoHistoryEntity>)

    @Query(
        "DELETE FROM video_history WHERE userId = :userId AND id NOT IN " +
            "(SELECT id FROM video_history WHERE userId = :userId ORDER BY createdAt DESC LIMIT :keepCount)"
    )
    suspend fun pruneOlderThan(userId: String, keepCount: Int = 50)

    @Query("DELETE FROM video_history WHERE userId = :userId AND id = :id")
    suspend fun deleteById(userId: String, id: String)

    @Query("DELETE FROM video_history WHERE userId = :userId")
    suspend fun clearAllForUser(userId: String)
}
