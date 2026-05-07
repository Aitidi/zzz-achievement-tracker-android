package com.aitidi.zzztracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements ORDER BY category, version, name")
    fun observeAll(): Flow<List<AchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<AchievementEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMissing(items: List<AchievementEntity>)

    @Query("UPDATE achievements SET progress = :progress WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Boolean)

    @Query("UPDATE achievements SET progress = 0")
    suspend fun resetAllProgress()

    @Query("SELECT COUNT(*) FROM achievements")
    suspend fun totalCount(): Int

    @Query("SELECT COUNT(*) FROM achievements WHERE progress = 1")
    suspend fun doneCount(): Int
}
