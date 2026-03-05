package com.aitidi.zzztracker.data.repo

import android.content.Context
import com.aitidi.zzztracker.data.db.AchievementDao
import com.aitidi.zzztracker.data.db.AchievementEntity
import com.aitidi.zzztracker.model.AchievementItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.MessageDigest

class TrackerRepository(private val context: Context, private val dao: AchievementDao) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    fun observeItems(): Flow<List<AchievementItem>> = dao.observeAll().map { it.map { e -> e.toModel() } }

    suspend fun ensureSeeded() {
        if (dao.totalCount() > 0) return
        val text = context.assets.open("achievements_master.json").bufferedReader().use { it.readText() }
        val payload = json.decodeFromString(MasterPayload.serializer(), text)
        val mapped = payload.items.map { raw ->
            val name = raw.成就名 ?: raw.name ?: ""
            val desc = raw.描述 ?: raw.description ?: ""
            val ver = raw.版本 ?: raw.version ?: "unknown"
            val cat = raw.分类 ?: raw.category ?: "未分类"
            val id = raw.id ?: stableId(name, ver, cat)
            AchievementEntity(id, name, desc, ver, cat, raw.进度 ?: false)
        }
        dao.upsertAll(mapped)
    }

    suspend fun toggle(id: String, progress: Boolean) = dao.updateProgress(id, progress)

    suspend fun exportProgressJson(): String {
        val items = dao.observeAll().first().map { ExportItem(it.id, it.progress) }
        return json.encodeToString(ExportPayload.serializer(), ExportPayload(items))
    }

    private fun stableId(name: String, version: String, category: String): String {
        val hash = MessageDigest.getInstance("SHA-1").digest("$name|$version|$category".toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}

private fun AchievementEntity.toModel() = AchievementItem(id, name, description, version, category, progress)

@Serializable
data class MasterPayload(
    val dataVersion: String? = null,
    val gameVersion: String? = null,
    val items: List<RawItem> = emptyList(),
)

@Serializable
data class RawItem(
    val id: String? = null,
    val 成就名: String? = null,
    val 描述: String? = null,
    val 版本: String? = null,
    val 分类: String? = null,
    val 进度: Boolean? = null,
    val name: String? = null,
    val description: String? = null,
    val version: String? = null,
    val category: String? = null,
)

@Serializable
data class ExportPayload(val items: List<ExportItem>)

@Serializable
data class ExportItem(val id: String, val progress: Boolean)
