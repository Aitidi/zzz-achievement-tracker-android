package com.aitidi.zzztracker.ui

import com.aitidi.zzztracker.model.AchievementItem
import com.aitidi.zzztracker.viewmodel.GroupMode
import com.aitidi.zzztracker.viewmodel.SortMode
import com.aitidi.zzztracker.viewmodel.StatusFilter
import com.aitidi.zzztracker.viewmodel.TrackerUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class AchievementListTest {
    private val items = listOf(
        AchievementItem("a", "甲", "说明", "v1.0", "勘域探境", progress = true),
        AchievementItem("b", "乙", "说明", "v3.3", "法厄同纪事"),
        AchievementItem("c", "丙", "说明", "v1.10", "雅努斯区"),
        AchievementItem("d", "丁", "说明", "v3.3", "新分类"),
    )

    @Test
    fun statusFilterSplitsDoneAndTodo() {
        assertEquals(listOf("b", "d", "c"), filterItems(items, TrackerUiState(status = StatusFilter.TODO)).map { it.id })
        assertEquals(listOf("a"), filterItems(items, TrackerUiState(status = StatusFilter.DONE)).map { it.id })
        assertEquals(4, filterItems(items, TrackerUiState(status = StatusFilter.ALL)).size)
    }

    @Test
    fun versionSectionsFollowSortOrderNumerically() {
        assertEquals(listOf("v3.3", "v1.10", "v1.0"), sectionsOf(items, GroupMode.VERSION, SortMode.VERSION_DESC).map { it.first })
        assertEquals(listOf("v1.0", "v1.10", "v3.3"), sectionsOf(items, GroupMode.VERSION, SortMode.VERSION_ASC).map { it.first })
    }

    @Test
    fun categorySectionsFollowGroupOrderWithUnknownLast() {
        assertEquals(listOf("法厄同纪事", "雅努斯区", "勘域探境", "新分类"),
            sectionsOf(items, GroupMode.CATEGORY, SortMode.VERSION_DESC).map { it.first })
    }
}
