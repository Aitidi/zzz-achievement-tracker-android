package com.aitidi.zzztracker.data.repo

import com.aitidi.zzztracker.model.AchievementCategories
import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class CategoryMigrationTest {
    private val json = Json { ignoreUnknownKeys = true }
    private fun catalog(): List<RawItem> {
        val assets = listOf(File("app/src/main/assets"), File("src/main/assets")).first { it.isDirectory }
        val index = json.decodeFromString<VersionIndex>(File(assets, "data/index.json").readText())
        return index.versions.flatMap { json.decodeFromString<VersionPayload>(File(assets, it.file).readText()).items }
    }

    @Test
    fun everyExistingBackupIdSurvivesCategoryMerge() {
        val old = catalog()
        val migrated = old.map { it.toEntity() }
        assertEquals(804, migrated.size)
        assertEquals(old.map { it.id }, migrated.map { it.id })
        assertEquals(migrated.size, migrated.map { it.id }.toSet().size)
        assertEquals(26, old.count { it.分类 == "布亚斯特" })
        assertEquals(31, migrated.count { it.category == "罗斯凯利法" })
        assertFalse(migrated.any { it.category == "布亚斯特" })
    }

    @Test
    fun legacyCatalogWithoutIdsResolvesIdentityBeforeRenaming() {
        val raw = RawItem(name = "旧版成就", version = "v3.0", category = "布亚斯特")
        val migrated = raw.toEntity()
        assertEquals(stableAchievementId("旧版成就", "v3.0", "布亚斯特"), migrated.id)
        assertEquals("罗斯凯利法", migrated.category)
        assertEquals("immutable-id", raw.copy(id = "immutable-id").toEntity().id)
    }

    @Test
    fun fourGroupsCoverCatalogExactlyOnceInRequestedOrder() {
        val groups = AchievementCategories.groups
        assertEquals(listOf("故事", "城市", "战术", "探索"), groups.map { it.title })
        assertEquals(listOf(6, 6, 6, 3), groups.map { it.categories.size })
        val assigned = groups.flatMap { it.categories }
        assertEquals(assigned.size, assigned.toSet().size)
        assertEquals(catalog().map { it.toEntity().category }.toSet(), assigned.toSet())
    }

    @Test
    fun oldAndNewSelectionsMergeWithoutDiscardingOtherFilters() {
        val migrated = AchievementCategories.canonicalSelection(setOf("布亚斯特", "罗斯凯利法", "作战技巧"))
        assertEquals(setOf("罗斯凯利法", "作战技巧"), migrated)
        assertEquals(migrated, AchievementCategories.canonicalSelection(migrated))
        assertEquals(emptySet<String>(), AchievementCategories.canonicalSelection(emptySet()))
    }
}
