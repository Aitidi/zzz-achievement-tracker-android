package com.aitidi.zzztracker.model

data class CategoryGroup(val title: String, val categories: List<String>)

/** Display taxonomy only. Achievement identity must never depend on a renamed category. */
object AchievementCategories {
    fun canonical(category: String): String = when (category) {
        "布亚斯特" -> "罗斯凯利法"
        else -> category
    }

    fun canonicalSelection(categories: Set<String>): Set<String> = categories.map(::canonical).toSet()

    val groups = listOf(
        CategoryGroup("故事", listOf("法厄同纪事", "代理人秘闻", "独家视界", "代理人信赖", "际会之时", "绳网热议")),
        CategoryGroup("城市", listOf("绳匠业务", "雅努斯区", "卫非地", "外环地带", "索恩区", "罗斯凯利法")),
        CategoryGroup("战术", listOf("迷宫诡域", "作战技巧", "敌对目标", "战斗成就", "迷失之地", "枯萎之都")),
        CategoryGroup("探索", listOf("空洞探索指南", "零号密钥", "勘域探境")),
    )
}
