package com.aitidi.zzztracker.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aitidi.zzztracker.ui.theme.AppColors
import com.aitidi.zzztracker.ui.theme.ZzzTrackerTheme
import com.aitidi.zzztracker.viewmodel.SortMode
import com.aitidi.zzztracker.viewmodel.TrackerViewModel
import kotlinx.coroutines.flow.collectLatest

internal enum class HomeTab(val title: String, val icon: ImageVector) {
    LIST("成就", Icons.Outlined.EmojiEvents),
    STATS("统计", Icons.Outlined.BarChart),
    SETTINGS("设置", Icons.Outlined.Settings),
}

internal fun compareVersionText(a: String, b: String): Int {
    fun parts(s: String) = s.trim().split(Regex("[^0-9]+")).filter { it.isNotBlank() }.map { it.toIntOrNull() ?: 0 }
    val left = parts(a)
    val right = parts(b)
    for (i in 0 until maxOf(left.size, right.size)) {
        val result = left.getOrElse(i) { 0 }.compareTo(right.getOrElse(i) { 0 })
        if (result != 0) return result
    }
    return a.compareTo(b, ignoreCase = true)
}

internal fun versionLabel(version: String) = "v" + version.trim().removePrefix("v").removePrefix("V")

@Composable
fun TrackerApp(vm: TrackerViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val allItems by vm.items.collectAsStateWithLifecycle()
    val versions = remember(allItems) { allItems.map { it.version }.distinct().sortedWith { a, b -> compareVersionText(b, a) } }
    val categories = remember(allItems) { allItems.map { it.category }.distinct().sorted() }
    var tab by rememberSaveable { mutableStateOf(HomeTab.LIST) }
    var showFilter by rememberSaveable { mutableStateOf(false) }
    var showSort by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val focus = LocalFocusManager.current
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let(vm::exportProgress) }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::importProgress) }
    LaunchedEffect(vm) {
        vm.events.collectLatest {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(it)
        }
    }
    val filtered = remember(allItems, ui.query, ui.onlyTodo, ui.selectedVersions, ui.selectedCategories, ui.sortMode) {
        val q = ui.query.trim()
        allItems.filter {
            (!ui.onlyTodo || !it.progress) &&
                (q.isBlank() || it.name.contains(q, true) || it.description.contains(q, true) || it.category.contains(q, true)) &&
                (ui.selectedVersions.isEmpty() || it.version in ui.selectedVersions) &&
                (ui.selectedCategories.isEmpty() || it.category in ui.selectedCategories)
        }.sortedWith { a, b ->
            val version = if (ui.sortMode == SortMode.VERSION_DESC) compareVersionText(b.version, a.version) else compareVersionText(a.version, b.version)
            if (version != 0) version else a.name.compareTo(b.name)
        }
    }
    ZzzTrackerTheme {
        if (showFilter) FilterSheet(ui, versions, categories, vm, onDismiss = { showFilter = false })
        if (showSort) SortSheet(ui.sortMode, onSelect = { vm.setSortMode(it); showSort = false }, onDismiss = { showSort = false })
        Scaffold(
            modifier = Modifier.fillMaxSize().imePadding(),
            containerColor = AppColors.Background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = { BottomTabBar(tab) { focus.clearFocus(); tab = it } },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
                when (tab) {
                    HomeTab.LIST -> AchievementScreen(ui, allItems, filtered, versions.firstOrNull(), vm,
                        onFilter = { focus.clearFocus(); showFilter = true },
                        onSort = { focus.clearFocus(); showSort = true })
                    HomeTab.STATS -> StatisticsScreen(allItems)
                    HomeTab.SETTINGS -> SettingsScreen(ui.compactMode, vm::setCompactMode,
                        onExport = { export.launch("zzz_progress_backup.json") },
                        onImport = { import.launch(arrayOf("application/json")) },
                        onReset = vm::requestResetProgress)
                }
            }
        }
    }
}
