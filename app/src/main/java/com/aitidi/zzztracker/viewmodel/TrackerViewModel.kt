package com.aitidi.zzztracker.viewmodel

import android.app.Application
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
    VERSION_DESC("版本新→旧"),
    VERSION_ASC("版本旧→新"),
    STATUS("完成状态"),
    NAME("名称")
}

data class TrackerUiState(
    val onlyTodo: Boolean = true,
    val query: String = "",
    val selectedVersions: Set<String> = emptySet(),
    val selectedCategories: Set<String> = emptySet(),
    val sortMode: SortMode = SortMode.VERSION_DESC,
    val lockProgressEditing: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val compactMode: Boolean = true,
)

class TrackerViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = TrackerRepository(app, AppDatabase.get(app).achievementDao())

    val items = repo.observeItems().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _ui = MutableStateFlow(TrackerUiState())
    val ui: StateFlow<TrackerUiState> = _ui.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch { repo.ensureSeeded() }
    }

    fun setOnlyTodo(v: Boolean) { _ui.value = _ui.value.copy(onlyTodo = v) }
    fun setQuery(v: String) { _ui.value = _ui.value.copy(query = v) }

    fun toggleVersion(v: String) {
        val next = _ui.value.selectedVersions.toMutableSet().apply {
            if (contains(v)) remove(v) else add(v)
        }
        _ui.value = _ui.value.copy(selectedVersions = next)
    }

    fun toggleCategory(v: String) {
        val next = _ui.value.selectedCategories.toMutableSet().apply {
            if (contains(v)) remove(v) else add(v)
        }
        _ui.value = _ui.value.copy(selectedCategories = next)
    }

    fun clearVersionFilter() { _ui.value = _ui.value.copy(selectedVersions = emptySet()) }
    fun clearCategoryFilter() { _ui.value = _ui.value.copy(selectedCategories = emptySet()) }

    fun setSortMode(v: SortMode) { _ui.value = _ui.value.copy(sortMode = v) }
    fun toggleLockProgressEditing() {
        _ui.value = _ui.value.copy(lockProgressEditing = !_ui.value.lockProgressEditing)
    }

    fun setThemeMode(v: ThemeMode) { _ui.value = _ui.value.copy(themeMode = v) }
    fun setCompactMode(v: Boolean) { _ui.value = _ui.value.copy(compactMode = v) }

    fun toggle(item: AchievementItem, checked: Boolean) {
        viewModelScope.launch { repo.toggle(item.id, checked) }
    }

    fun exportProgress(uri: Uri) {
        viewModelScope.launch {
            runCatching { repo.exportProgressToUri(uri) }
                .onSuccess { done -> _events.emit("导出完成：已完成 $done 项") }
                .onFailure { _events.emit("导出失败：${it.message}") }
        }
    }

    fun importProgress(uri: Uri) {
        viewModelScope.launch {
            runCatching { repo.importProgressFromUri(uri) }
                .onSuccess { r -> _events.emit("导入完成：应用 ${r.applied}/${r.source} 条") }
                .onFailure { _events.emit("导入失败：${it.message}") }
        }
    }

    fun resetProgress() {
        viewModelScope.launch {
            repo.resetAllProgress()
            _events.emit("已重置全部进度")
        }
    }
}
