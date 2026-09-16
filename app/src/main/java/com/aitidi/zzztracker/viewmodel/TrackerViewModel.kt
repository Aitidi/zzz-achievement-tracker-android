package com.aitidi.zzztracker.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aitidi.zzztracker.data.db.AppDatabase
import com.aitidi.zzztracker.data.repo.TrackerRepository
import com.aitidi.zzztracker.model.AchievementItem
import com.aitidi.zzztracker.model.AchievementCategories
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val sortMode: SortMode = SortMode.VERSION_DESC,
    val lockProgressEditing: Boolean = false,
    val compactMode: Boolean = true,
    val dataError: String? = null,
)

class TrackerViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = TrackerRepository(app, AppDatabase.get(app).achievementDao())
    private val prefs = app.getSharedPreferences("tracker_prefs", Context.MODE_PRIVATE)

    private var resetTapCount = 0
    private val resetConfirmTotal = 5
    private var resetInProgress = false
    private var resetHintTimerJob: Job? = null
    private val resetHintTimeoutMs = 3500L

    val items = repo.observeItems().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _ui = MutableStateFlow(loadUiState())
    val ui: StateFlow<TrackerUiState> = _ui.asStateFlow()

    private val _events = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            runCatching { repo.ensureSeeded() }
                .onFailure {
                    _ui.value = _ui.value.copy(
                        dataError = "成就数据加载失败：${it.message ?: "数据文件无效"}"
                    )
                }
        }
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
        val category = AchievementCategories.canonical(v)
        val next = _ui.value.selectedCategories.toMutableSet().apply {
            if (contains(category)) remove(category) else add(category)
        }
        updateUi { copy(selectedCategories = next) }
    }

    fun clearVersionFilter() = updateUi { copy(selectedVersions = emptySet()) }
    fun clearCategoryFilter() = updateUi { copy(selectedCategories = emptySet()) }

    fun setSortMode(v: SortMode) = updateUi { copy(sortMode = v) }
    fun toggleLockProgressEditing() = updateUi { copy(lockProgressEditing = !lockProgressEditing) }

    fun setCompactMode(v: Boolean) = updateUi { copy(compactMode = v) }

    fun toggle(item: AchievementItem, checked: Boolean) {
        if (_ui.value.lockProgressEditing) {
            _events.tryEmit("已锁定，请先解锁勾选")
            return
        }
        viewModelScope.launch {
            repo.toggle(item.id, checked)
            if (checked) _events.emit("已完成：${item.name}")
        }
    }

    fun exportProgress(uri: Uri) {
        viewModelScope.launch {
            runCatching { repo.exportProgressToUri(uri) }
                .onSuccess { done -> _events.emit("导出完成：已完成 $done 项") }
                .onFailure { _events.emit("导出失败：${it.message ?: "未知错误"}") }
        }
    }

    fun importProgress(uri: Uri) {
        if (_ui.value.lockProgressEditing) {
            _events.tryEmit("已锁定，请先解锁后导入")
            return
        }
        viewModelScope.launch {
            runCatching { repo.importProgressFromUri(uri) }
                .onSuccess { r -> _events.emit("导入完成：应用 ${r.applied}/${r.source} 条") }
                .onFailure { _events.emit("导入失败：${it.message ?: "文件格式错误或内容无效"}") }
        }
    }

    fun requestResetProgress() {
        if (_ui.value.lockProgressEditing) {
            _events.tryEmit("已锁定，请先解锁后重置")
            return
        }
        if (resetInProgress) {
            _events.tryEmit("正在重置中，请稍候…")
            return
        }

        resetTapCount += 1
        if (resetTapCount < resetConfirmTotal) {
            _events.tryEmit("重置确认 ${resetTapCount}/${resetConfirmTotal}，继续点击“重置”")
            resetHintTimerJob?.cancel()
            resetHintTimerJob = viewModelScope.launch {
                delay(resetHintTimeoutMs)
                if (!resetInProgress && resetTapCount in 1 until resetConfirmTotal) {
                    resetTapCount = 0
                }
            }
            return
        }

        resetHintTimerJob?.cancel()
        resetTapCount = 0
        resetInProgress = true
        _events.tryEmit("重置中…")
        viewModelScope.launch {
            runCatching { repo.resetAllProgress() }
                .onSuccess { _events.tryEmit("已重置全部进度（${resetConfirmTotal}/${resetConfirmTotal}）") }
                .onFailure { _events.tryEmit("重置失败：${it.message ?: "未知错误"}") }
            resetInProgress = false
        }
    }

    private inline fun updateUi(mutator: TrackerUiState.() -> TrackerUiState) {
        _ui.value = _ui.value.mutator()
        persistUiState(_ui.value)
    }

    private fun loadUiState(): TrackerUiState {
        val rawSort = prefs.getString("sortMode", SortMode.VERSION_DESC.name) ?: SortMode.VERSION_DESC.name
        val sort = when (rawSort) {
            "STATUS", "TODO_FIRST", "NAME" -> SortMode.VERSION_DESC // migration from old enum values
            else -> runCatching { SortMode.valueOf(rawSort) }.getOrDefault(SortMode.VERSION_DESC)
        }
        return TrackerUiState(
            onlyTodo = prefs.getBoolean("onlyTodo", true),
            query = prefs.getString("query", "") ?: "",
            selectedVersions = decodeSet(prefs.getString("selectedVersions", "") ?: ""),
            selectedCategories = loadCategorySelection(prefs),
            sortMode = sort,
            lockProgressEditing = prefs.getBoolean("lockProgressEditing", false),
            compactMode = prefs.getBoolean("compactMode", true),
        )
    }

    private fun persistUiState(state: TrackerUiState) {
        prefs.edit()
            .putBoolean("onlyTodo", state.onlyTodo)
            .putString("query", state.query)
            .putString("selectedVersions", encodeSet(state.selectedVersions))
            .putString("selectedCategories", encodeSet(state.selectedCategories))
            .putString("sortMode", state.sortMode.name)
            .putBoolean("lockProgressEditing", state.lockProgressEditing)
            .putBoolean("compactMode", state.compactMode)
            .apply()
    }

    private fun encodeSet(values: Set<String>): String = values.joinToString("\u001F")
    private fun decodeSet(raw: String): Set<String> = raw.split("\u001F").filter { it.isNotBlank() }.toSet()
}
