package com.aitidi.zzztracker.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aitidi.zzztracker.model.AchievementItem
import com.aitidi.zzztracker.ui.theme.AppColors
import com.aitidi.zzztracker.ui.theme.LocalAppPalette
import com.aitidi.zzztracker.ui.theme.ZzzTrackerTheme
import com.aitidi.zzztracker.viewmodel.SortMode
import com.aitidi.zzztracker.viewmodel.StatusFilter
import com.aitidi.zzztracker.viewmodel.TrackerUiState
import com.aitidi.zzztracker.viewmodel.TrackerViewModel
import com.aitidi.zzztracker.viewmodel.UiMessage
import kotlinx.coroutines.flow.collectLatest

internal enum class HomeTab(val title: String, val icon: ImageVector) {
    OVERVIEW("总览", Icons.Outlined.Dashboard),
    LIST("成就", Icons.Outlined.Checklist),
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

internal fun filterItems(all: List<AchievementItem>, ui: TrackerUiState): List<AchievementItem> {
    val q = ui.query.trim()
    return all.filter {
        when (ui.status) {
            StatusFilter.TODO -> !it.progress
            StatusFilter.DONE -> it.progress
            StatusFilter.ALL -> true
        } &&
            (q.isBlank() || it.name.contains(q, true) || it.description.contains(q, true) || it.category.contains(q, true)) &&
            (ui.selectedVersions.isEmpty() || it.version in ui.selectedVersions) &&
            (ui.selectedCategories.isEmpty() || it.category in ui.selectedCategories)
    }.sortedWith { a, b ->
        val version = if (ui.sortMode == SortMode.VERSION_DESC) compareVersionText(b.version, a.version) else compareVersionText(a.version, b.version)
        if (version != 0) version else a.name.compareTo(b.name)
    }
}

@Composable
fun TrackerApp(vm: TrackerViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    ZzzTrackerTheme(ui.themeMode) {
        SystemBarAppearance()
        TrackerContent(vm, ui)
    }
}

@Composable
private fun SystemBarAppearance() {
    val view = LocalView.current
    val dark = LocalAppPalette.current.isDark
    if (!view.isInEditMode) SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
}

@Composable
private fun TrackerContent(vm: TrackerViewModel, ui: TrackerUiState) {
    val allItems by vm.items.collectAsStateWithLifecycle()
    val pendingImport by vm.pendingImport.collectAsStateWithLifecycle()
    val versions = remember(allItems) { allItems.map { it.version }.distinct().sortedWith { a, b -> compareVersionText(b, a) } }
    val categories = remember(allItems) { allItems.map { it.category }.distinct().sorted() }
    var tab by rememberSaveable { mutableStateOf(HomeTab.OVERVIEW) }
    var showFilter by rememberSaveable { mutableStateOf(false) }
    var selection by remember { mutableStateOf<Set<String>?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val focus = LocalFocusManager.current
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let(vm::exportProgress) }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::importProgress) }
    LaunchedEffect(vm) {
        vm.events.collectLatest { message: UiMessage ->
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(message.text, actionLabel = if (message.undo != null) "撤销" else null,
                withDismissAction = false, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) message.undo?.invoke()
        }
    }
    val filtered = remember(allItems, ui.query, ui.status, ui.selectedVersions, ui.selectedCategories, ui.sortMode) { filterItems(allItems, ui) }
    BackHandler(selection != null) { selection = null }
    // Leaving the list drops a half-made selection rather than carrying it to another page.
    LaunchedEffect(tab) { if (tab != HomeTab.LIST) selection = null }

    if (showFilter) FilterSheet(ui, allItems, versions, categories, filtered.size, vm, onDismiss = { showFilter = false })
    pendingImport?.let { pending ->
        AlertDialog(
            onDismissRequest = vm::cancelImport,
            title = { Text("覆盖当前进度？") },
            text = { Text(
                "备份共 ${pending.source} 条，其中 ${pending.matched} 条与当前成就匹配（已完成 ${pending.completed} 条）。\n\n" +
                    "导入后将以备份为准：当前已完成的 ${pending.currentCompleted} 条会被替换，备份中没有的成就会标为未完成。此操作无法撤销。"
            ) },
            confirmButton = { TextButton(onClick = vm::confirmImport) { Text("覆盖导入", color = AppColors.Danger) } },
            dismissButton = { TextButton(onClick = vm::cancelImport) { Text("取消", color = AppColors.Text) } },
            containerColor = AppColors.Surface,
        )
    }
    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        containerColor = AppColors.Background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(data, shape = RoundedCornerShape(14.dp), containerColor = AppColors.Text,
                    contentColor = AppColors.Surface, actionColor = AppColors.Accent)
            }
        },
        bottomBar = {
            val current = selection
            if (current != null) SelectionBar(current.size, onMark = { done ->
                vm.setProgress(allItems.filter { it.id in current }, done)
                selection = null
            }, onCancel = { selection = null })
            else BottomTabBar(tab) { focus.clearFocus(); tab = it }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            when (tab) {
                HomeTab.OVERVIEW -> OverviewScreen(allItems, versions, ui.dataError,
                    onVersion = { vm.showVersion(it); tab = HomeTab.LIST },
                    onCategories = { vm.showCategories(it); tab = HomeTab.LIST })
                HomeTab.LIST -> AchievementScreen(ui, allItems, filtered, vm, selection, onSelectionChange = { selection = it },
                    onFilter = { showFilter = true })
                HomeTab.SETTINGS -> SettingsScreen(ui, versions.firstOrNull(), allItems.size,
                    onTheme = vm::setThemeMode,
                    onCompact = vm::setCompactMode,
                    onToggleLock = vm::toggleLockProgressEditing,
                    onExport = { export.launch("zzz_progress_backup.json") },
                    onImport = { import.launch(arrayOf("application/json")) },
                    onReset = vm::resetProgress,
                    onHint = vm::showHint)
            }
        }
    }
}
