package com.aitidi.zzztracker.data.repo

import android.content.Context
import android.net.Uri
import com.aitidi.zzztracker.data.db.AchievementDao
import com.aitidi.zzztracker.data.db.AchievementEntity
import com.aitidi.zzztracker.model.AchievementItem
import com.aitidi.zzztracker.model.AchievementCategories
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
        dao.syncCatalog(loadFromVersionPacks())
    }

    suspend fun toggle(id: String, progress: Boolean) = dao.updateProgress(id, progress)

    suspend fun setProgress(ids: List<String>, progress: Boolean) = dao.setProgress(ids, progress)

    suspend fun resetAllProgress() = dao.resetAllProgress()

    suspend fun exportProgressToUri(uri: Uri): Int {
        val items = dao.observeAll().first().map { ExportItem(it.id, it.progress) }
        val payload = ExportPayload(items)
        val text = json.encodeToString(ExportPayload.serializer(), payload)
        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(text) }
            ?: error("无法写入文件")
        return items.count { it.progress }
    }

    /** Reads and validates a backup without touching the database; apply it with [applyImport]. */
    suspend fun readImportFromUri(uri: Uri): PendingImport {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("无法读取文件")
        val current = dao.getAll()
        return parseImport(text, current.asSequence().map { it.id }.toSet(), current.count { it.progress })
    }

    /** Replaces all progress with the backup: achievements missing from it become incomplete. */
    suspend fun applyImport(pending: PendingImport): ImportResult {
        dao.replaceProgress(pending.progressById)
        return ImportResult(applied = pending.matched, source = pending.source)
    }

    suspend fun importProgressFromUri(uri: Uri): ImportResult = applyImport(readImportFromUri(uri))

    private fun loadFromVersionPacks(): List<AchievementEntity> {
        val indexText = context.assets.open("data/index.json").bufferedReader().use { it.readText() }
        val index = json.decodeFromString(VersionIndex.serializer(), indexText)
        val items = index.versions.flatMap { ver ->
            val verText = context.assets.open(ver.file).bufferedReader().use { it.readText() }
            val payload = json.decodeFromString(VersionPayload.serializer(), verText)
            check(payload.total == payload.items.size) { "${ver.file} 数量与声明不一致" }
            payload.items.map { it.toEntity() }
        }
        check(index.total == null || index.total == items.size) { "成就总数与索引不一致" }
        check(items.map { it.id }.toSet().size == items.size) { "成就 ID 重复" }
        return items
    }

}

internal fun RawItem.toEntity(): AchievementEntity {
    val n = 成就名 ?: name ?: ""
    val d = 描述 ?: description ?: ""
    val v = 版本 ?: version ?: "unknown"
    val originalCategory = 分类 ?: category ?: "未分类"
    // Resolve legacy identity BEFORE normalizing the category. Old JSON backups store this ID.
    val resolvedId = id ?: stableAchievementId(n, v, originalCategory)
    return AchievementEntity(resolvedId, n, d, v, AchievementCategories.canonical(originalCategory), false)
}

internal fun stableAchievementId(name: String, version: String, category: String): String {
    val hash = MessageDigest.getInstance("SHA-1").digest("$name|$version|$category".toByteArray())
    return hash.joinToString("") { "%02x".format(it) }
}

private fun AchievementEntity.toModel() = AchievementItem(id, name, description, version, AchievementCategories.canonical(category), progress)

data class ImportResult(val applied: Int, val source: Int)

data class PendingImport(
    val progressById: Map<String, Boolean>,
    /** Entries in the backup file. */
    val source: Int,
    /** Backup entries whose id exists in the current catalog. */
    val matched: Int,
    /** Matched entries marked completed in the backup. */
    val completed: Int,
    /** Completed achievements in the database before the import. */
    val currentCompleted: Int,
)

class InvalidImportException(message: String) : IllegalArgumentException(message)

private val importJson = Json { ignoreUnknownKeys = true }

/** Rejects empty files, non-backup JSON and backups that share no id with the catalog. */
internal fun parseImport(text: String, currentIds: Set<String>, currentCompleted: Int): PendingImport {
    if (text.isBlank()) throw InvalidImportException("文件为空")
    val payload = runCatching { importJson.decodeFromString(ExportPayload.serializer(), text) }
        .getOrElse { throw InvalidImportException("不是有效的进度备份文件") }
    if (payload.items.isEmpty()) throw InvalidImportException("备份中没有任何进度记录")
    val imported = payload.items.associate { it.id to it.progress }
    val matched = imported.filterKeys { it in currentIds }
    if (matched.isEmpty()) throw InvalidImportException("备份中的成就与当前数据不匹配")
    return PendingImport(
        progressById = imported,
        source = payload.items.size,
        matched = matched.size,
        completed = matched.count { it.value },
        currentCompleted = currentCompleted,
    )
}

@Serializable
data class VersionIndex(
    val generatedAt: String? = null,
    val total: Int? = null,
    val versions: List<VersionEntry> = emptyList(),
)

@Serializable
data class VersionEntry(
    val version: String,
    val file: String,
    val total: Int,
)

@Serializable
data class VersionPayload(
    val version: String,
    val total: Int,
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
