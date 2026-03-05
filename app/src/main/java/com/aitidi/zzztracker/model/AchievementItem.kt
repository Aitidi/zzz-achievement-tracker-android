package com.aitidi.zzztracker.model

import kotlinx.serialization.Serializable

@Serializable
data class AchievementItem(
    val id: String,
    val 成就名: String,
    val 描述: String,
    val 版本: String,
    val 分类: String,
    val 进度: Boolean = false,
)
