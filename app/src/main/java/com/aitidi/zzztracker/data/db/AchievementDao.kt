package com.aitidi.zzztracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements ORDER BY category, version, name")
    fun observeAll(): Flow<List<AchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<AchievementEntity>)

    @Query("UPDATE achievements SET progress = :progress WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Boolean)

    @Query("UPDATE achievements SET progress = 0")
    suspend fun resetAllProgress()

    @Query("SELECT * FROM achievements")
    suspend fun getAll(): List<AchievementEntity>

    @Query("DELETE FROM achievements WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Transaction
    suspend fun syncCatalog(items: List<AchievementEntity>) {
        val current = getAll().associateBy { it.id }
        upsertAll(items.map { incoming ->
            incoming.copy(progress = current[incoming.id]?.progress ?: false)
        })
        val incomingIds = items.asSequence().map { it.id }.toSet()
        val removedIds = current.keys.filterNot { it in incomingIds }
        if (removedIds.isNotEmpty()) deleteByIds(removedIds)
    }

    @Transaction
    suspend fun replaceProgress(progressById: Map<String, Boolean>) {
        getAll().forEach { item ->
            updateProgress(item.id, progressById[item.id] ?: false)
        }
    }
}
