package com.aitidi.zzztracker

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aitidi.zzztracker.data.db.AppDatabase
import com.aitidi.zzztracker.data.repo.*
import com.aitidi.zzztracker.viewmodel.loadCategorySelection
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryMigrationIntegrationTest {
    @Test
    fun upgradeAndOldBackupRoundTripPreserveEveryProgressFlag() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val json = Json { ignoreUnknownKeys = true }
        val index = json.decodeFromString<VersionIndex>(context.assets.open("data/index.json").bufferedReader().use { it.readText() })
        val old = index.versions.flatMap { version ->
            json.decodeFromString<VersionPayload>(context.assets.open(version.file).bufferedReader().use { it.readText() }).items
        }.mapIndexed { i, raw -> raw.toEntity().copy(category = raw.分类 ?: raw.category!!, progress = i % 3 == 0) }
        val expected = old.associate { it.id to it.progress }
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val backup = File.createTempFile("category-migration-", ".json", context.cacheDir)
        try {
            val dao = database.achievementDao()
            dao.upsertAll(old)
            val repository = TrackerRepository(context, dao)
            // Export in the old database state: the persisted category still contains 布亚斯特.
            repository.exportProgressToUri(Uri.fromFile(backup))
            repeat(2) {
                repository.ensureSeeded()
                val migrated = dao.getAll()
                assertEquals(expected, migrated.associate { item -> item.id to item.progress })
                assertEquals(31, migrated.count { item -> item.category == "罗斯凯利法" })
                assertFalse(migrated.any { item -> item.category == "布亚斯特" })
            }
            repository.resetAllProgress()
            assertTrue(dao.getAll().none { it.progress })
            val imported = repository.importProgressFromUri(Uri.fromFile(backup))
            assertEquals(804, imported.applied)
            assertEquals(expected, dao.getAll().associate { it.id to it.progress })
            assertEquals(31, dao.getAll().count { it.category == "罗斯凯利法" })
        } finally {
            database.close()
            backup.delete()
        }
    }

    @Test
    fun savedLegacyFilterIsMigratedAndPersistedOnce() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("category_migration_test", Context.MODE_PRIVATE)
        try {
            prefs.edit().putString("selectedCategories", "布亚斯特\u001F罗斯凯利法\u001F作战技巧").commit()
            val expected = setOf("罗斯凯利法", "作战技巧")
            assertEquals(expected, loadCategorySelection(prefs))
            assertEquals(expected, prefs.getString("selectedCategories", "")!!.split("\u001F").toSet())
            assertEquals(expected, loadCategorySelection(prefs))
        } finally {
            prefs.edit().clear().commit()
        }
    }
}
