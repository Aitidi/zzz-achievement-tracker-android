package com.aitidi.zzztracker.data.repo

import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogDataTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun versionPacksHaveStableUniqueIdsAndCorrectTotals() {
        val assets = listOf(File("app/src/main/assets"), File("src/main/assets"))
            .first { it.isDirectory }
        val index = json.decodeFromString<VersionIndex>(File(assets, "data/index.json").readText())
        val all = index.versions.flatMap { entry ->
            val payload = json.decodeFromString<VersionPayload>(File(assets, entry.file).readText())
            assertEquals(entry.total, payload.total)
            assertEquals(payload.total, payload.items.size)
            payload.items
        }

        assertEquals(index.total, all.size)
        assertTrue(all.all { !it.id.isNullOrBlank() })
        assertEquals(all.size, all.mapNotNull { it.id }.toSet().size)
    }

    @Test
    fun fallbackIdIsDeterministic() {
        val first = stableAchievementId("苏醒之日", "v3.0", "法厄同纪事")
        val second = stableAchievementId("苏醒之日", "v3.0", "法厄同纪事")

        assertEquals(first, second)
        assertEquals(40, first.length)
        assertTrue(first.matches(Regex("[0-9a-f]{40}")))
    }
}
