package com.aitidi.zzztracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val version: String,
    val category: String,
    val progress: Boolean = false,
)
