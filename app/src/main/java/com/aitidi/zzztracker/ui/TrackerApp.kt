package com.aitidi.zzztracker.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aitidi.zzztracker.model.AchievementItem
import com.aitidi.zzztracker.ui.theme.ThemeMode
import com.aitidi.zzztracker.ui.theme.ZzzTrackerTheme
import com.aitidi.zzztracker.viewmodel.SortMode
import com.aitidi.zzztracker.viewmodel.TrackerUiState
import com.aitidi.zzztracker.viewmodel.TrackerViewModel

private enum class HomeTab(val title: String, val icon: ImageVector) {
    LIST("成就", Icons.Rounded.Star),
    STATS("统计", Icons.Rounded.BarChart),
    SETTINGS("设置", Icons.Rounded.Settings)
}

private object UiTokens {
    val ControlCorner = RoundedCornerShape(10.dp)
    val ControlHeight = 32.dp
    val IconButtonWidth = 40.dp
    val ActionButtonCorner = RoundedCornerShape(10.dp)
    val ActionButtonHeight = 32.dp
}

private object ProtoPalette {
    val Purple = Color(0xFF896CFE)
    val PurpleSoft = Color(0xFFB3A0FF)
    val Accent = Color(0xFFE2F163)
    val Text = Color(0xFFF2F2F2)
    val Muted = Color(0xFFA6A6A6)
    val Border = Color(0xFF3A3A3A)
    val Bg = Color(0xFF1E1E1E)
    val Surface = Color(0xFF232323)
    val SurfaceAlt = Color(0xFF262626)
    val SurfaceSelected = Color(0xFF2F3140)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
                containerColor = ProtoPalette.Surface,
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("筛选", style = MaterialTheme.typography.titleLarge)

                    Text("版本", style = MaterialTheme.typography.labelMedium, color = ProtoPalette.Muted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OptionChip(text = "全部", selected = ui.selectedVersions.isEmpty(), onClick = vm::clearVersionFilter)
                        installedVersions.forEach { v ->
                            OptionChip(text = "版本 $v", selected = v in ui.selectedVersions, onClick = { vm.toggleVersion(v) })
                        }
                    }

                    Text("分类", style = MaterialTheme.typography.labelMedium, color = ProtoPalette.Muted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OptionChip(text = "全部", selected = ui.selectedCategories.isEmpty(), onClick = vm::clearCategoryFilter)
                        categories.forEach { c ->
                            OptionChip(text = c, selected = c in ui.selectedCategories, onClick = { vm.toggleCategory(c) })
                        }
                    }
                }
            }
        }

        if (showSortSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSortSheet = false },
                containerColor = ProtoPalette.Surface,
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("排序方式", style = MaterialTheme.typography.titleLarge)
                    SortMode.entries.forEach { mode ->
                        OptionRow(
                            text = mode.label,
                            selected = ui.sortMode == mode,
                            onClick = {
                                vm.setSortMode(mode)
                                showSortSheet = false
                            }
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
                        colors = listOf(Color(0xFF3E2A7C), ProtoPalette.Bg),
                    )
                ),
            containerColor = Color.Transparent,
            contentColor = ProtoPalette.Text,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                BottomTabBar(
                    selected = tab,
                    onSelect = { tab = it }
                )
            }
        ) { padding ->
            when (tab) {
                HomeTab.LIST -> ListTab(
                    padding = padding,
                    ui = ui,
                    allItems = allItems,
                    filtered = sortedFiltered,
                    latestVersion = installedVersions.firstOrNull(),
                    onOpenFilter = { showFilterSheet = true },
                    onOpenSort = { showSortSheet = true },
                    vm = vm,
                )

                HomeTab.STATS -> StatsTab(
                    padding = padding,
                    allItems = allItems.filter { it.version !in ui.disabledVersions },
                )

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ListTab(
    padding: PaddingValues,
    ui: TrackerUiState,
    allItems: List<AchievementItem>,
    filtered: List<AchievementItem>,
    latestVersion: String?,
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

    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        ScreenTitle(title = "成就", badge = latestVersion)
        SummaryCard(done = done, total = allItems.size)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = ui.query,
                onValueChange = vm::setQuery,
                interactionSource = searchInteraction,
                placeholder = { Text("搜索成就 / 输入关键词", color = ProtoPalette.Muted) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { dismissSearch() }, onDone = { dismissSearch() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ProtoPalette.PurpleSoft,
                    unfocusedBorderColor = ProtoPalette.Border,
                    focusedContainerColor = ProtoPalette.SurfaceAlt,
                    unfocusedContainerColor = ProtoPalette.SurfaceAlt,
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            )

            AnimatedVisibility(visible = isSearchFocused) {
                TextButton(onClick = { dismissSearch() }) {
                    Text("退出")
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OptionChip(text = "仅未完成", selected = ui.onlyTodo, onClick = { vm.setOnlyTodo(!ui.onlyTodo) })
                IconRectButton(
                    selected = ui.selectedVersions.isNotEmpty() || ui.selectedCategories.isNotEmpty(),
                    onClick = onOpenFilter
                ) { Icon(Icons.Rounded.FilterList, contentDescription = "筛选") }

                IconRectButton(
                    selected = ui.sortMode != SortMode.VERSION_DESC,
                    onClick = onOpenSort
                ) { Icon(Icons.Rounded.SwapVert, contentDescription = "排序") }
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
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ui.selectedVersions.toList().sortedDescending().forEach { v ->
                    OptionChip(text = "版本:$v", selected = true, onClick = { vm.toggleVersion(v) })
                }
                ui.selectedCategories.toList().sorted().forEach { c ->
                    OptionChip(text = "分类:$c", selected = true, onClick = { vm.toggleCategory(c) })
                }
            }
        }

        val itemSpacing = if (ui.compactMode) 8.dp else 10.dp

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (ui.query.isNotBlank() || ui.selectedVersions.isNotEmpty() || ui.selectedCategories.isNotEmpty()) {
                        "暂无结果，试试清空筛选"
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
                        detectTapGestures(onTap = { if (isSearchFocused) dismissSearch() })
                    },
                contentPadding = PaddingValues(top = 10.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(itemSpacing)
            ) {
                items(filtered, key = { it.id }, contentType = { "achievement" }) { item ->
                    AchievementRow(
                        item = item,
                        compact = ui.compactMode,
                        lockProgressEditing = ui.lockProgressEditing,
                        onToggle = vm::toggle,
                    )
                }
            }
        }
    }
}

@Composable
private fun ScreenTitle(title: String, badge: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = title, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = ProtoPalette.Text)
        if (!badge.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(ProtoPalette.Purple.copy(alpha = 0.22f))
                    .border(1.dp, ProtoPalette.PurpleSoft.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(text = badge, style = MaterialTheme.typography.labelSmall, color = ProtoPalette.PurpleSoft)
            }
        }
    }
}

@Composable
private fun OptionChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) ProtoPalette.SurfaceSelected else ProtoPalette.SurfaceAlt
    val border = if (selected) ProtoPalette.PurpleSoft else ProtoPalette.Border
    Box(
        modifier = Modifier
            .height(UiTokens.ControlHeight)
            .clip(UiTokens.ControlCorner)
            .background(bg)
            .border(1.dp, border, UiTokens.ControlCorner)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = ProtoPalette.Text)
    }
}

@Composable
private fun OptionRow(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Color(0xFF32295A) else ProtoPalette.SurfaceAlt
    val border = if (selected) ProtoPalette.Purple else ProtoPalette.Border
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text, color = ProtoPalette.Text)
    }
}

@Composable
private fun IconRectButton(
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val shape = UiTokens.ControlCorner
    val bg = if (selected) ProtoPalette.SurfaceSelected else ProtoPalette.SurfaceAlt
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
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ProtoPalette.Border, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier
                .background(brush = Brush.linearGradient(listOf(ProtoPalette.Surface, ProtoPalette.SurfaceAlt)))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("总进度", style = MaterialTheme.typography.labelMedium, color = ProtoPalette.Muted)
            Text("$done/$total", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = ProtoPalette.Text)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(ProtoPalette.Border)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(8.dp)
                        .background(ProtoPalette.Accent)
                )
            }
        }
    }
}

@Composable
private fun AchievementRow(
    item: AchievementItem,
    compact: Boolean,
    lockProgressEditing: Boolean,
    onToggle: (AchievementItem, Boolean) -> Unit,
) {
    val hPad = if (compact) 10.dp else 12.dp
    val vPad = if (compact) 9.dp else 12.dp
    val cardShape = RoundedCornerShape(14.dp)

    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = ProtoPalette.Surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ProtoPalette.Border, cardShape)
            .alpha(if (item.progress) 0.72f else 1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = hPad, vertical = vPad),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.name,
                    maxLines = if (compact) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ProtoPalette.Text,
                    textDecoration = if (item.progress) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    text = item.description,
                    maxLines = if (compact) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ProtoPalette.Muted
                )
            }

            Row(
                modifier = Modifier.padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetaTag(item.category)
                MetaTag("v${item.version}")

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (item.progress) Color(0xFF3A315F) else Color(0xFF2B2B2B))
                        .border(1.dp, ProtoPalette.Border, RoundedCornerShape(7.dp))
                        .alpha(if (lockProgressEditing) 0.4f else 1f)
                        .clickable(enabled = !lockProgressEditing) { onToggle(item, !item.progress) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (item.progress) "✓" else "", fontSize = 12.sp, color = ProtoPalette.Text)
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
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF111111),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatsTab(
    padding: PaddingValues,
    allItems: List<AchievementItem>,
) {
    val total = allItems.size
    val done = allItems.count { it.progress }
    val rate = if (total == 0) 0 else (done * 100 / total)

    val grouped = allItems
        .groupBy { it.version }
        .toList()
        .sortedByDescending { it.first }

    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ScreenTitle("统计")

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = ProtoPalette.Surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ProtoPalette.Border, RoundedCornerShape(18.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("总完成率", color = ProtoPalette.Muted)
                Text("$rate%", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = ProtoPalette.Text)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(ProtoPalette.Border)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(rate / 100f)
                            .height(8.dp)
                            .background(ProtoPalette.Accent)
                    )
                }
            }
        }

        if (grouped.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无可统计的成就数据", color = ProtoPalette.Muted)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
                items(grouped) { (version, list) ->
                    val d = list.count { it.progress }
                    val p = if (list.isEmpty()) 0 else (d * 100 / list.size)
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = ProtoPalette.Surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ProtoPalette.Border, RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("$version 版本", fontWeight = FontWeight.SemiBold, color = ProtoPalette.Text)
                                Text("完成 $d / ${list.size}", color = ProtoPalette.Muted, style = MaterialTheme.typography.bodyMedium)
                            }
                            MetaTag("$p%")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ScreenTitle("设置")

        SettingsActionRow(
            title = "紧凑模式",
            desc = if (compactMode) "已启用" else "已关闭",
            actionText = "切换",
            actionSelected = compactMode,
            onActionClick = { onCompactModeChange(!compactMode) }
        )

        SettingsActionRow(
            title = "导出进度",
            desc = "JSON",
            actionText = "导出",
            actionSelected = false,
            onActionClick = onExport
        )

        SettingsActionRow(
            title = "导入进度",
            desc = "JSON",
            actionText = "导入",
            actionSelected = false,
            onActionClick = onImport
        )

        SettingsActionRow(
            title = "重置进度",
            desc = "恢复为初始状态",
            actionText = "重置",
            actionSelected = false,
            onActionClick = onReset
        )

        Text("主题", color = ProtoPalette.Muted, style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OptionChip("跟随系统", themeMode == ThemeMode.SYSTEM) { onThemeModeChange(ThemeMode.SYSTEM) }
            OptionChip("浅色", themeMode == ThemeMode.LIGHT) { onThemeModeChange(ThemeMode.LIGHT) }
            OptionChip("深色", themeMode == ThemeMode.DARK) { onThemeModeChange(ThemeMode.DARK) }
        }

        Text("版本模块", color = ProtoPalette.Muted, style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            versions.forEach { version ->
                val installed = version !in disabledVersions
                OptionChip(
                    text = if (installed) "$version 已安装" else "$version 已卸载",
                    selected = installed,
                    onClick = { onToggleVersionInstalled(version) }
                )
            }
        }
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    desc: String,
    actionText: String,
    actionSelected: Boolean,
    onActionClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ProtoPalette.Surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ProtoPalette.Border, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, color = ProtoPalette.Text)
                Text(desc, color = ProtoPalette.Muted, style = MaterialTheme.typography.bodyMedium)
            }
            OptionChip(text = actionText, selected = actionSelected, onClick = onActionClick)
        }
    }
}

@Composable
private fun BottomTabBar(selected: HomeTab, onSelect: (HomeTab) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 14.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ProtoPalette.Surface)
            .border(1.dp, ProtoPalette.Border, RoundedCornerShape(16.dp))
            .height(58.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            HomeTab.entries.forEach { tab ->
                BottomTabButton(
                    modifier = Modifier.weight(1f),
                    selected = selected == tab,
                    title = tab.title,
                    icon = tab.icon,
                    onClick = { onSelect(tab) },
                )
            }
        }
    }
}

@Composable
private fun BottomTabButton(
    modifier: Modifier,
    selected: Boolean,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(0.dp))
            .background(if (selected) Color(0xFF2B2B2B) else Color.Transparent)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = title, tint = if (selected) Color.White else ProtoPalette.Muted, modifier = Modifier.size(16.dp))
        Text(
            text = title,
            color = if (selected) Color.White else ProtoPalette.Muted,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
