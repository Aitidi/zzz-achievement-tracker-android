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

data class TrackerUiState(
    val onlyTodo: Boolean = true,
    val query: String = "",
    val selectedVersion: String = "全部",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
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
    fun setVersion(v: String) { _ui.value = _ui.value.copy(selectedVersion = v) }
    fun setThemeMode(v: ThemeMode) { _ui.value = _ui.value.copy(themeMode = v) }

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
