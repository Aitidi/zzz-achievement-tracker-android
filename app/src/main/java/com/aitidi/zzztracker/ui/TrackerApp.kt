package com.aitidi.zzztracker.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aitidi.zzztracker.model.AchievementItem
import com.aitidi.zzztracker.ui.theme.ThemeMode
import com.aitidi.zzztracker.ui.theme.ZzzTrackerTheme
import com.aitidi.zzztracker.viewmodel.SortMode
import com.aitidi.zzztracker.viewmodel.TrackerViewModel

private enum class HomeTab(val title: String) { LIST("成就"), STATS("统计"), SETTINGS("设置") }

private object UiTokens {
    val ControlCorner = RoundedCornerShape(10.dp)
    val ControlHeight = 32.dp
    val IconButtonWidth = 40.dp

    val ActionButtonCorner = RoundedCornerShape(14.dp)
    val ActionButtonHeight = 48.dp
}

private object ProtoPalette {
    val Purple = Color(0xFF896CFE)
    val PurpleSoft = Color(0xFFB3A0FF)
    val Accent = Color(0xFFE2F163)
    val Muted = Color(0xFFA6A6A6)
    val Border = Color(0xFF3A3A3A)
    val SurfaceHi = Color(0xFF2A2A2A)
    val SurfaceLo = Color(0xFF262626)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerApp(vm: TrackerViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val allItems by vm.items.collectAsStateWithLifecycle()
    val versions = allItems.map { it.version }.distinct().sortedDescending()
    val installedVersions = versions.filterNot { it in ui.disabledVersions }
    val categories = allItems
        .asSequence()
        .filter { it.version !in ui.disabledVersions }
        .map { it.category }
        .distinct()
        .sorted()
        .toList()
    var tab by remember { mutableStateOf(HomeTab.LIST) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    val createExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        if (it != null) vm.exportProgress(it)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        if (it != null) vm.importProgress(it)
    }

    LaunchedEffect(Unit) { vm.events.collect { snackbar.showSnackbar(it) } }

    val sortedFiltered by remember(allItems, ui) {
        derivedStateOf {
            val filtered = allItems.filter {
                val installedOk = it.version !in ui.disabledVersions
                val todoOk = !ui.onlyTodo || !it.progress
                val q = ui.query.trim()
                val qOk = q.isBlank() || it.name.contains(q, true) || it.description.contains(q, true) || it.category.contains(q, true)
                val verOk = ui.selectedVersions.isEmpty() || it.version in ui.selectedVersions
                val catOk = ui.selectedCategories.isEmpty() || it.category in ui.selectedCategories
                installedOk && todoOk && qOk && verOk && catOk
            }

            when (ui.sortMode) {
                SortMode.VERSION_DESC -> filtered.sortedWith(compareByDescending<AchievementItem> { it.version }.thenBy { it.name })
                SortMode.VERSION_ASC -> filtered.sortedWith(compareBy<AchievementItem> { it.version }.thenBy { it.name })
                SortMode.TODO_FIRST -> filtered.sortedWith(compareBy<AchievementItem> { it.progress }.thenByDescending { it.version }.thenBy { it.name })
                SortMode.NAME -> filtered.sortedBy { it.name }
            }
        }
    }

    ZzzTrackerTheme(mode = ui.themeMode) {
        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("版本（可多选）", style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = ui.selectedVersions.isEmpty(),
                                onClick = vm::clearVersionFilter,
                                label = { Text("全部") }
                            )
                        }
                        items(installedVersions) { v ->
                            FilterChip(
                                selected = v in ui.selectedVersions,
                                onClick = { vm.toggleVersion(v) },
                                label = { Text(v) }
                            )
                        }
                    }
                    Text("分类（可多选）", style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = ui.selectedCategories.isEmpty(),
                                onClick = vm::clearCategoryFilter,
                                label = { Text("全部") }
                            )
                        }
                        items(categories) { c ->
                            FilterChip(
                                selected = c in ui.selectedCategories,
                                onClick = { vm.toggleCategory(c) },
                                label = { Text(c) }
                            )
                        }
                    }
                }
            }
        }

        if (showSortSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSortSheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("排序方式", style = MaterialTheme.typography.titleMedium)
                    SortMode.entries.forEach { mode ->
                        FilterChip(
                            selected = ui.sortMode == mode,
                            onClick = {
                                vm.setSortMode(mode)
                                showSortSheet = false
                            },
                            label = { Text(mode.label) }
                        )
                    }
                }
            }
        }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF3E2A7C), MaterialTheme.colorScheme.background),
                        center = Offset(860f, -220f),
                        radius = 1400f
                    )
                ),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("绝区零成就")
                            val latestVersion = installedVersions.firstOrNull()
                            if (latestVersion != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(ProtoPalette.Purple.copy(alpha = 0.22f))
                                        .border(1.dp, ProtoPalette.PurpleSoft.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = latestVersion,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ProtoPalette.PurpleSoft
                                    )
                                }
                            }
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                    HomeTab.entries.forEach {
                        NavigationBarItem(
                            selected = tab == it,
                            onClick = { tab = it },
                            icon = {},
                            label = { Text(it.title) }
                        )
                    }
                }
            }
        ) { padding ->
            when (tab) {
                HomeTab.LIST -> ListTab(
                    padding = padding,
                    ui = ui,
                    allItems = allItems,
                    filtered = sortedFiltered,
                    onOpenFilter = { showFilterSheet = true },
                    onOpenSort = { showSortSheet = true },
                    vm = vm,
                )
                HomeTab.STATS -> StatsTab(padding, allItems.filter { it.version !in ui.disabledVersions })
                HomeTab.SETTINGS -> SettingsTab(
                    padding = padding,
                    themeMode = ui.themeMode,
                    compactMode = ui.compactMode,
                    versions = versions,
                    disabledVersions = ui.disabledVersions,
                    onToggleVersionInstalled = vm::toggleVersionInstalled,
                    onThemeModeChange = vm::setThemeMode,
                    onCompactModeChange = vm::setCompactMode,
                    onExport = { createExportLauncher.launch("zzz_progress_backup.json") },
                    onImport = { importLauncher.launch(arrayOf("application/json")) },
                    onReset = vm::resetProgress,
                )
            }
        }
    }
}

@Composable
private fun ListTab(
    padding: PaddingValues,
    ui: com.aitidi.zzztracker.viewmodel.TrackerUiState,
    allItems: List<AchievementItem>,
    filtered: List<AchievementItem>,
    onOpenFilter: () -> Unit,
    onOpenSort: () -> Unit,
    vm: TrackerViewModel,
) {
    val done = allItems.count { it.progress }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchInteraction = remember { MutableInteractionSource() }
    val isSearchFocused by searchInteraction.collectIsFocusedAsState()

    fun dismissSearch() {
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    BackHandler(enabled = isSearchFocused) { dismissSearch() }

    Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        SummaryCard(done = done, total = allItems.size)

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = ui.query,
                onValueChange = vm::setQuery,
                interactionSource = searchInteraction,
                placeholder = { Text("搜索成就 / 输入关键词") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { dismissSearch() },
                    onDone = { dismissSearch() }
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            )

            AnimatedVisibility(visible = isSearchFocused) {
                TextButton(onClick = { dismissSearch() }) {
                    Text("退出")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(selected = ui.onlyTodo, onClick = { vm.setOnlyTodo(!ui.onlyTodo) }, label = { Text("仅未完成") })
                IconRectButton(
                    selected = ui.selectedVersions.isNotEmpty() || ui.selectedCategories.isNotEmpty(),
                    onClick = onOpenFilter
                ) {
                    Icon(Icons.Rounded.FilterList, contentDescription = "筛选")
                }
                IconRectButton(
                    selected = ui.sortMode != SortMode.VERSION_DESC,
                    onClick = onOpenSort
                ) {
                    Icon(Icons.Rounded.SwapVert, contentDescription = "排序")
                }
            }
            IconRectButton(
                selected = ui.lockProgressEditing,
                onClick = vm::toggleLockProgressEditing
            ) {
                Icon(
                    if (ui.lockProgressEditing) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                    contentDescription = if (ui.lockProgressEditing) "已锁定" else "未锁定"
                )
            }
        }

        if (ui.selectedVersions.isNotEmpty() || ui.selectedCategories.isNotEmpty()) {
            LazyRow(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(ui.selectedVersions.toList().sortedDescending()) { v ->
                    FilterChip(
                        selected = true,
                        onClick = { vm.toggleVersion(v) },
                        label = { Text("版本:$v") }
                    )
                }
                items(ui.selectedCategories.toList().sorted()) { c ->
                    FilterChip(
                        selected = true,
                        onClick = { vm.toggleCategory(c) },
                        label = { Text("分类:$c") }
                    )
                }
            }
        }

        val itemSpacing = if (ui.compactMode) 4.dp else 12.dp

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (ui.query.isNotBlank() || ui.selectedVersions.isNotEmpty() || ui.selectedCategories.isNotEmpty()) {
                        "没有匹配结果，试试清空筛选"
                    } else {
                        "暂无成就数据或已全部隐藏"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = ProtoPalette.Muted
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isSearchFocused) {
                        detectTapGestures(onTap = {
                            if (isSearchFocused) dismissSearch()
                        })
                    },
                contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(itemSpacing)
            ) {
                items(filtered, key = { it.id }, contentType = { "achievement" }) { item ->
                    AchievementRow(
                        item = item,
                        compact = ui.compactMode,
                        lockProgressEditing = ui.lockProgressEditing,
                        onToggle = vm::toggle
                    )
                }
            }
        }
    }
}

@Composable
private fun IconRectButton(
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val shape = UiTokens.ControlCorner
    val bg = if (selected) Color(0xFF2F3140) else MaterialTheme.colorScheme.surface
    val border = if (selected) ProtoPalette.PurpleSoft else ProtoPalette.Border

    Box(
        modifier = Modifier
            .width(UiTokens.IconButtonWidth)
            .height(UiTokens.ControlHeight)
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun SummaryCard(done: Int, total: Int) {
    val progress = if (total == 0) 0f else done.toFloat() / total.toFloat()
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ProtoPalette.Border, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier
                .background(brush = Brush.linearGradient(listOf(ProtoPalette.SurfaceHi, ProtoPalette.SurfaceLo)))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("总进度", style = MaterialTheme.typography.labelMedium, color = ProtoPalette.Muted)
            Text("$done / $total", style = MaterialTheme.typography.titleLarge)
            Text("剩余 ${total - done} · ${"%.1f".format(progress * 100)}%", style = MaterialTheme.typography.labelMedium, color = ProtoPalette.Accent)
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)).background(ProtoPalette.Border)) {
                Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(ProtoPalette.Accent))
            }
        }
    }
}

@Composable
private fun AchievementRow(
    item: AchievementItem,
    compact: Boolean,
    lockProgressEditing: Boolean,
    onToggle: (AchievementItem, Boolean) -> Unit
) {
    val hPad = if (compact) 10.dp else 14.dp
    val vPad = if (compact) 6.dp else 12.dp

    Card(
        shape = RoundedCornerShape(if (compact) 10.dp else 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = if (compact) 0.5.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ProtoPalette.Border, RoundedCornerShape(if (compact) 10.dp else 16.dp))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = hPad, vertical = vPad), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = item.progress,
                enabled = !lockProgressEditing,
                onCheckedChange = { checked -> if (!lockProgressEditing) onToggle(item, checked) }
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        item.name,
                        maxLines = if (compact) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                        style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge
                    )
                    Text(if (item.progress) "●" else "○", color = if (item.progress) ProtoPalette.Accent else ProtoPalette.Muted.copy(alpha = 0.66f))
                }
                Text(
                    item.description,
                    maxLines = if (compact) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ProtoPalette.Muted
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    MetaTag(text = item.category)
                    MetaTag(text = item.version)
                }
            }
        }
    }
}

@Composable
private fun MetaTag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(ProtoPalette.Accent)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF111111),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatsTab(padding: PaddingValues, allItems: List<AchievementItem>) {
    val grouped = allItems.groupBy { it.version }.toList().sortedBy { it.first }
    val done = allItems.count { it.progress }
    val total = allItems.size

    Column(modifier = Modifier.padding(padding).fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (allItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无可统计的成就数据", color = ProtoPalette.Muted)
            }
        } else {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    RingProgress(percent = if (total == 0) 0f else done.toFloat() / total)
                    Column {
                        Text("总完成率", style = MaterialTheme.typography.labelMedium)
                        Text("$done / $total", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }

            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("版本柱状图", style = MaterialTheme.typography.titleMedium)
                    grouped.forEach { (ver, list) ->
                        val d = list.count { it.progress }
                        val p = if (list.isEmpty()) 0f else d.toFloat() / list.size.toFloat()
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(ver, modifier = Modifier.width(42.dp), style = MaterialTheme.typography.labelMedium)
                            Box(modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(999.dp)).background(ProtoPalette.Border)) {
                                Box(modifier = Modifier.fillMaxWidth(p).fillMaxHeight().background(ProtoPalette.Accent))
                            }
                            Text("${"%.0f".format(p * 100)}%", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RingProgress(percent: Float) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = ProtoPalette.Border,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(6f, 6f),
                size = Size(size.width - 12f, size.height - 12f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 10f, cap = StrokeCap.Round)
            )
            drawArc(
                color = ProtoPalette.Purple,
                startAngle = -90f,
                sweepAngle = 360f * percent,
                useCenter = false,
                topLeft = Offset(6f, 6f),
                size = Size(size.width - 12f, size.height - 12f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 10f, cap = StrokeCap.Round)
            )
        }
        Text("${"%.0f".format(percent * 100)}%", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SettingsTab(
    padding: PaddingValues,
    themeMode: ThemeMode,
    compactMode: Boolean,
    versions: List<String>,
    disabledVersions: Set<String>,
    onToggleVersionInstalled: (String) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onCompactModeChange: (Boolean) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onReset: () -> Unit,
) {
    Column(modifier = Modifier.padding(padding).fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("显示设置", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = themeMode == ThemeMode.SYSTEM, onClick = { onThemeModeChange(ThemeMode.SYSTEM) }, label = { Text("跟随系统") })
            FilterChip(selected = themeMode == ThemeMode.LIGHT, onClick = { onThemeModeChange(ThemeMode.LIGHT) }, label = { Text("浅色") })
            FilterChip(selected = themeMode == ThemeMode.DARK, onClick = { onThemeModeChange(ThemeMode.DARK) }, label = { Text("深色") })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = compactMode, onClick = { onCompactModeChange(true) }, label = { Text("紧凑") })
            FilterChip(selected = !compactMode, onClick = { onCompactModeChange(false) }, label = { Text("舒适") })
        }

        Text("版本模块", style = MaterialTheme.typography.titleLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(versions) { version ->
                val installed = version !in disabledVersions
                FilterChip(
                    selected = installed,
                    onClick = { onToggleVersionInstalled(version) },
                    label = { Text(if (installed) "$version 已安装" else "$version 已卸载") }
                )
            }
        }

        Text("数据管理", style = MaterialTheme.typography.titleLarge)
        OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth().height(UiTokens.ActionButtonHeight), shape = UiTokens.ActionButtonCorner) {
            Icon(Icons.Rounded.FileUpload, contentDescription = null)
            Text("  导出进度 JSON")
        }
        OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth().height(UiTokens.ActionButtonHeight), shape = UiTokens.ActionButtonCorner) {
            Icon(Icons.Rounded.FileDownload, contentDescription = null)
            Text("  导入进度 JSON")
        }
        OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth().height(UiTokens.ActionButtonHeight), shape = UiTokens.ActionButtonCorner) {
            Icon(Icons.Rounded.Restore, contentDescription = null)
            Text("  重置全部进度")
        }
    }
}
