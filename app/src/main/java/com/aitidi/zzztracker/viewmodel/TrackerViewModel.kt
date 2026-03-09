package com.aitidi.zzztracker.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aitidi.zzztracker.data.db.AppDatabase
import com.aitidi.zzztracker.data.repo.TrackerRepository
import com.aitidi.zzztracker.model.AchievementItem
import com.aitidi.zzztracker.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortMode(val label: String) {
    VERSION_DESC("版本：新 → 旧"),
    VERSION_ASC("版本：旧 → 新")
}

data class TrackerUiState(
    val onlyTodo: Boolean = true,
    val query: String = "",
    val selectedVersions: Set<String> = emptySet(),
    val selectedCategories: Set<String> = emptySet(),
    val disabledVersions: Set<String> = emptySet(),
    val sortMode: SortMode = SortMode.VERSION_DESC,
    val lockProgressEditing: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val compactMode: Boolean = true,
)

class TrackerViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = TrackerRepository(app, AppDatabase.get(app).achievementDao())
    private val prefs = app.getSharedPreferences("tracker_prefs", Context.MODE_PRIVATE)

    val items = repo.observeItems().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _ui = MutableStateFlow(loadUiState())
    val ui: StateFlow<TrackerUiState> = _ui.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch { repo.ensureSeeded() }
    }

    fun setOnlyTodo(v: Boolean) = updateUi { copy(onlyTodo = v) }
    fun setQuery(v: String) = updateUi { copy(query = v) }

    fun toggleVersion(v: String) {
        val next = _ui.value.selectedVersions.toMutableSet().apply {
            if (contains(v)) remove(v) else add(v)
        }
        updateUi { copy(selectedVersions = next) }
    }

    fun toggleCategory(v: String) {
        val next = _ui.value.selectedCategories.toMutableSet().apply {
            if (contains(v)) remove(v) else add(v)
        }
        updateUi { copy(selectedCategories = next) }
    }

    fun clearVersionFilter() = updateUi { copy(selectedVersions = emptySet()) }
    fun clearCategoryFilter() = updateUi { copy(selectedCategories = emptySet()) }

    fun setSortMode(v: SortMode) = updateUi { copy(sortMode = v) }
    fun toggleLockProgressEditing() = updateUi { copy(lockProgressEditing = !lockProgressEditing) }

    fun setThemeMode(v: ThemeMode) = updateUi { copy(themeMode = v) }
    fun setCompactMode(v: Boolean) = updateUi { copy(compactMode = v) }

    fun toggleVersionInstalled(version: String) {
        val next = _ui.value.disabledVersions.toMutableSet().apply {
            if (contains(version)) remove(version) else add(version)
        }
        updateUi {
            copy(
                disabledVersions = next,
                selectedVersions = selectedVersions - next
            )
        }
    }

    fun toggle(item: AchievementItem, checked: Boolean) {
        viewModelScope.launch { repo.toggle(item.id, checked) }
    }

    fun exportProgress(uri: Uri) {
        viewModelScope.launch {
            runCatching { repo.exportProgressToUri(uri) }
                .onSuccess { done -> _events.emit("导出完成：已完成 $done 项") }
                .onFailure { _events.emit("导出失败：${it.message ?: "未知错误"}") }
        }
    }

    fun importProgress(uri: Uri) {
        viewModelScope.launch {
            runCatching { repo.importProgressFromUri(uri) }
                .onSuccess { r -> _events.emit("导入完成：应用 ${r.applied}/${r.source} 条") }
                .onFailure { _events.emit("导入失败：${it.message ?: "文件格式错误或内容无效"}") }
        }
    }

    fun resetProgress() {
        viewModelScope.launch {
            repo.resetAllProgress()
            _events.emit("已重置全部进度")
        }
    }

    private inline fun updateUi(mutator: TrackerUiState.() -> TrackerUiState) {
        _ui.value = _ui.value.mutator()
        persistUiState(_ui.value)
    }

    private fun loadUiState(): TrackerUiState {
        val migratedThemeV2 = prefs.getBoolean("themeMigratedV2", false)
        val rawTheme = prefs.getString("themeMode", ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        val theme = when {
            !migratedThemeV2 && rawTheme == ThemeMode.SYSTEM.name -> ThemeMode.DARK
            else -> runCatching { ThemeMode.valueOf(rawTheme) }.getOrDefault(ThemeMode.DARK)
        }
        if (!migratedThemeV2) {
            prefs.edit().putBoolean("themeMigratedV2", true).apply()
        }

        val rawSort = prefs.getString("sortMode", SortMode.VERSION_DESC.name) ?: SortMode.VERSION_DESC.name
        val sort = when (rawSort) {
            "STATUS", "TODO_FIRST", "NAME" -> SortMode.VERSION_DESC // migration from old enum values
            else -> runCatching { SortMode.valueOf(rawSort) }.getOrDefault(SortMode.VERSION_DESC)
        }
        return TrackerUiState(
            onlyTodo = prefs.getBoolean("onlyTodo", true),
            query = prefs.getString("query", "") ?: "",
            selectedVersions = decodeSet(prefs.getString("selectedVersions", "") ?: ""),
            selectedCategories = decodeSet(prefs.getString("selectedCategories", "") ?: ""),
            disabledVersions = decodeSet(prefs.getString("disabledVersions", "") ?: ""),
            sortMode = sort,
            lockProgressEditing = prefs.getBoolean("lockProgressEditing", false),
            themeMode = theme,
            compactMode = prefs.getBoolean("compactMode", true),
        )
    }

    private fun persistUiState(state: TrackerUiState) {
        prefs.edit()
            .putBoolean("onlyTodo", state.onlyTodo)
            .putString("query", state.query)
            .putString("selectedVersions", encodeSet(state.selectedVersions))
            .putString("selectedCategories", encodeSet(state.selectedCategories))
            .putString("disabledVersions", encodeSet(state.disabledVersions))
            .putString("sortMode", state.sortMode.name)
            .putBoolean("lockProgressEditing", state.lockProgressEditing)
            .putString("themeMode", state.themeMode.name)
            .putBoolean("compactMode", state.compactMode)
            .apply()
    }

    private fun encodeSet(values: Set<String>): String = values.joinToString("\u001F")
    private fun decodeSet(raw: String): Set<String> = raw.split("\u001F").filter { it.isNotBlank() }.toSet()
}
