package com.aitidi.zzztracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aitidi.zzztracker.data.db.AppDatabase
import com.aitidi.zzztracker.data.repo.TrackerRepository
import com.aitidi.zzztracker.model.AchievementItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TrackerUiState(
    val onlyTodo: Boolean = true,
    val query: String = "",
    val selectedVersion: String = "全部",
)

class TrackerViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = TrackerRepository(app, AppDatabase.get(app).achievementDao())

    val items = repo.observeItems().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _ui = MutableStateFlow(TrackerUiState())
    val ui: StateFlow<TrackerUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch { repo.ensureSeeded() }
    }

    fun setOnlyTodo(v: Boolean) {
        _ui.value = _ui.value.copy(onlyTodo = v)
    }

    fun setQuery(v: String) {
        _ui.value = _ui.value.copy(query = v)
    }

    fun setVersion(v: String) {
        _ui.value = _ui.value.copy(selectedVersion = v)
    }

    fun toggle(item: AchievementItem, checked: Boolean) {
        viewModelScope.launch { repo.toggle(item.id, checked) }
    }
}
