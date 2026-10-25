package com.aitidi.zzztracker.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aitidi.zzztracker.data.db.AppDatabase
import com.aitidi.zzztracker.data.repo.InvalidImportException
import com.aitidi.zzztracker.data.repo.PendingImport
import com.aitidi.zzztracker.data.repo.TrackerRepository
import com.aitidi.zzztracker.model.AchievementItem
import com.aitidi.zzztracker.model.AchievementCategories
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortMode(val label: String) {
    VERSION_DESC("新 → 旧"),
    VERSION_ASC("旧 → 新")
}

enum class StatusFilter(val label: String) { TODO("未完成"), DONE("已完成"), ALL("全部") }

enum class GroupMode(val label: String) { VERSION("版本"), CATEGORY("分类") }

enum class ThemeMode(val label: String) { SYSTEM("跟随系统"), LIGHT("浅色"), DARK("深色") }

/** A snackbar message; [undo] restores the state from before the change it reports. */
data class UiMessage(val text: String, val undo: (() -> Unit)? = null)

data class TrackerUiState(
    val status: StatusFilter = StatusFilter.TODO,
    val query: String = "",
    val selectedVersions: Set<String> = emptySet(),
    val selectedCategories: Set<String> = emptySet(),
    val sortMode: SortMode = SortMode.VERSION_DESC,
    val groupMode: GroupMode = GroupMode.VERSION,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val lockProgressEditing: Boolean = false,
    val dataError: String? = null,
)

class TrackerViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = TrackerRepository(app, AppDatabase.get(app).achievementDao())
    private val prefs = app.getSharedPreferences("tracker_prefs", Context.MODE_PRIVATE)

    private var resetInProgress = false

    val items = repo.observeItems().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _ui = MutableStateFlow(loadUiState())
    val ui: StateFlow<TrackerUiState> = _ui.asStateFlow()

    private val _pendingImport = MutableStateFlow<PendingImport?>(null)
    /** A validated backup waiting for the user to confirm overwriting current progress. */
    val pendingImport: StateFlow<PendingImport?> = _pendingImport.asStateFlow()

    private val _events = MutableSharedFlow<UiMessage>(replay = 0, extraBufferCapacity = 16)
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

    fun setStatus(v: StatusFilter) = updateUi { copy(status = v) }
    fun setGroupMode(v: GroupMode) = updateUi { copy(groupMode = v) }
    fun setThemeMode(v: ThemeMode) = updateUi { copy(themeMode = v) }

    /** Overview shortcut: open the to-do list for one version. */
    fun showVersion(version: String) = updateUi {
        copy(selectedVersions = setOf(version), selectedCategories = emptySet(), status = StatusFilter.TODO, groupMode = GroupMode.VERSION, query = "")
    }

    /** Overview shortcut: open the to-do list for a set of categories. */
    fun showCategories(categories: Collection<String>) = updateUi {
        copy(selectedVersions = emptySet(), selectedCategories = AchievementCategories.canonicalSelection(categories.toSet()),
            status = StatusFilter.TODO, groupMode = GroupMode.CATEGORY, query = "")
    }

    fun showHint(text: String) {
        _events.tryEmit(UiMessage(text))
    }

    fun clearFilters() = updateUi { copy(selectedVersions = emptySet(), selectedCategories = emptySet()) }

    fun showEverything() = updateUi { copy(selectedVersions = emptySet(), selectedCategories = emptySet(), query = "", status = StatusFilter.ALL) }
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


    fun toggle(item: AchievementItem, checked: Boolean) =
        setProgress(listOf(item), checked, if (checked) "已完成「${item.name}」" else "已取消「${item.name}」")

    /** Marks [items] and offers an undo that restores each item's previous state. */
    fun setProgress(items: List<AchievementItem>, checked: Boolean, label: String = "${items.size} 项标为${if (checked) "已完成" else "未完成"}") {
        if (_ui.value.lockProgressEditing) {
            _events.tryEmit(UiMessage("进度已锁定，可在成就页右上角菜单解锁"))
            return
        }
        val changed = items.filter { it.progress != checked }
        if (changed.isEmpty()) return
        viewModelScope.launch {
            runCatching { repo.setProgress(changed.map { it.id }, checked) }
                .onSuccess {
                    _events.emit(UiMessage(label) {
                        viewModelScope.launch { repo.setProgress(changed.map { it.id }, !checked) }
                    })
                }
                .onFailure { _events.emit(UiMessage("保存失败：${it.message ?: "未知错误"}")) }
        }
    }

    fun exportProgress(uri: Uri) {
        viewModelScope.launch {
            runCatching { repo.exportProgressToUri(uri) }
                .onSuccess { done -> _events.emit(UiMessage("导出完成：已完成 $done 项")) }
                .onFailure { _events.emit(UiMessage("导出失败：${it.message ?: "未知错误"}")) }
        }
    }

    fun importProgress(uri: Uri) {
        if (_ui.value.lockProgressEditing) {
            _events.tryEmit(UiMessage("已锁定，请先解锁后导入"))
            return
        }
        viewModelScope.launch {
            runCatching { repo.readImportFromUri(uri) }
                .onSuccess { _pendingImport.value = it }
                .onFailure {
                    val reason = if (it is InvalidImportException) it.message else "文件格式错误或内容无效"
                    _events.emit(UiMessage("导入失败：$reason，当前进度未改动"))
                }
        }
    }

    fun confirmImport() {
        val pending = _pendingImport.value ?: return
        _pendingImport.value = null
        if (_ui.value.lockProgressEditing) {
            _events.tryEmit(UiMessage("已锁定，请先解锁后导入"))
            return
        }
        viewModelScope.launch {
            runCatching { repo.applyImport(pending) }
                .onSuccess { r -> _events.emit(UiMessage("导入完成：应用 ${r.applied}/${r.source} 条")) }
                .onFailure { _events.emit(UiMessage("导入失败：${it.message ?: "未知错误"}")) }
        }
    }

    fun cancelImport() {
        if (_pendingImport.value == null) return
        _pendingImport.value = null
        _events.tryEmit(UiMessage("已取消导入，当前进度未改动"))
    }

    /** Called after the hold-to-confirm gesture; the snackbar offers to restore the previous progress. */
    fun resetProgress() {
        if (_ui.value.lockProgressEditing) {
            _events.tryEmit(UiMessage("已锁定，请先解锁后重置"))
            return
        }
        if (resetInProgress) return
        resetInProgress = true
        viewModelScope.launch {
            val completed = items.value.filter { it.progress }.map { it.id }
            runCatching { repo.resetAllProgress() }
                .onSuccess {
                    _events.emit(UiMessage("已重置全部进度（${completed.size} 项）") {
                        viewModelScope.launch { repo.setProgress(completed, true) }
                    })
                }
                .onFailure { _events.emit(UiMessage("重置失败：${it.message ?: "未知错误"}")) }
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
        // Older builds stored a single "only to-do" switch.
        val status = prefs.getString("statusFilter", null)?.let { raw -> runCatching { StatusFilter.valueOf(raw) }.getOrNull() }
            ?: if (prefs.getBoolean("onlyTodo", true)) StatusFilter.TODO else StatusFilter.ALL
        return TrackerUiState(
            status = status,
            query = prefs.getString("query", "") ?: "",
            selectedVersions = decodeSet(prefs.getString("selectedVersions", "") ?: ""),
            selectedCategories = loadCategorySelection(prefs),
            sortMode = sort,
            groupMode = prefs.getString("groupMode", null)?.let { runCatching { GroupMode.valueOf(it) }.getOrNull() } ?: GroupMode.VERSION,
            themeMode = prefs.getString("themeMode", null)?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            lockProgressEditing = prefs.getBoolean("lockProgressEditing", false),
        )
    }

    private fun persistUiState(state: TrackerUiState) {
        prefs.edit()
            .putString("statusFilter", state.status.name)
            .putString("query", state.query)
            .putString("selectedVersions", encodeSet(state.selectedVersions))
            .putString("selectedCategories", encodeSet(state.selectedCategories))
            .putString("sortMode", state.sortMode.name)
            .putString("groupMode", state.groupMode.name)
            .putString("themeMode", state.themeMode.name)
            .putBoolean("lockProgressEditing", state.lockProgressEditing)
            .apply()
    }

    private fun encodeSet(values: Set<String>): String = values.joinToString("\u001F")
    private fun decodeSet(raw: String): Set<String> = raw.split("\u001F").filter { it.isNotBlank() }.toSet()
}
