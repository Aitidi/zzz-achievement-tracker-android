package com.aitidi.zzztracker.model

data class AchievementItem(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val category: String,
    val progress: Boolean = false,
)
