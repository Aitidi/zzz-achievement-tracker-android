package com.aitidi.zzztracker.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Restore
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aitidi.zzztracker.model.AchievementItem
import com.aitidi.zzztracker.ui.theme.ThemeMode
import com.aitidi.zzztracker.ui.theme.ZzzTrackerTheme
import com.aitidi.zzztracker.viewmodel.TrackerViewModel

private enum class HomeTab(val title: String) { LIST("成就"), STATS("统计"), SETTINGS("设置") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerApp(vm: TrackerViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val allItems by vm.items.collectAsStateWithLifecycle()
    val versions = listOf("全部") + allItems.map { it.version }.distinct().sortedDescending()
    val categories = listOf("全部") + allItems.map { it.category }.distinct().sorted()
    var tab by remember { mutableStateOf(HomeTab.LIST) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    val createExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        if (it != null) vm.exportProgress(it)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        if (it != null) vm.importProgress(it)
    }

    LaunchedEffect(Unit) { vm.events.collect { snackbar.showSnackbar(it) } }

    val filtered = allItems.filter {
        val todoOk = !ui.onlyTodo || !it.progress
        val q = ui.query.trim()
        val qOk = q.isBlank() || it.name.contains(q, true) || it.description.contains(q, true) || it.category.contains(q, true)
        val verOk = ui.selectedVersion == "全部" || it.version == ui.selectedVersion
        val catOk = ui.selectedCategory == "全部" || it.category == ui.selectedCategory
        todoOk && qOk && verOk && catOk
    }

    ZzzTrackerTheme(mode = ui.themeMode) {
        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("版本", style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(versions) { v ->
                            FilterChip(selected = ui.selectedVersion == v, onClick = { vm.setVersion(v) }, label = { Text(v) })
                        }
                    }
                    Text("分类", style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { c ->
                            FilterChip(selected = ui.selectedCategory == c, onClick = { vm.setCategory(c) }, label = { Text(c) })
                        }
                    }
                }
            }
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("绝区零成就")
                            val latestVersion = versions.firstOrNull { it != "全部" }
                            if (latestVersion != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = latestVersion,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
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
                    filtered = filtered,
                    onOpenFilter = { showFilterSheet = true },
                    vm = vm,
                )
                HomeTab.STATS -> StatsTab(padding, allItems)
                HomeTab.SETTINGS -> SettingsTab(
                    padding = padding,
                    themeMode = ui.themeMode,
                    compactMode = ui.compactMode,
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
    vm: TrackerViewModel,
) {
    val done = allItems.count { it.progress }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        SummaryCard(done = done, total = allItems.size)

        OutlinedTextField(
            value = ui.query,
            onValueChange = vm::setQuery,
            label = { Text("搜索成就") },
            placeholder = { Text("输入关键词") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                },
                onDone = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(56.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = ui.onlyTodo, onClick = { vm.setOnlyTodo(!ui.onlyTodo) }, label = { Text("仅未完成") })
                FilterChip(
                    selected = ui.selectedVersion != "全部" || ui.selectedCategory != "全部",
                    onClick = onOpenFilter,
                    label = { Text("筛选") },
                    leadingIcon = { Icon(Icons.Rounded.FilterList, contentDescription = null) }
                )
            }
            Text("${filtered.size} 项", style = MaterialTheme.typography.labelMedium)
        }

        if (ui.selectedVersion != "全部" || ui.selectedCategory != "全部") {
            LazyRow(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (ui.selectedVersion != "全部") item { FilterChip(selected = true, onClick = { vm.setVersion("全部") }, label = { Text("版本:${ui.selectedVersion}") }) }
                if (ui.selectedCategory != "全部") item { FilterChip(selected = true, onClick = { vm.setCategory("全部") }, label = { Text("分类:${ui.selectedCategory}") }) }
            }
        }

        val itemSpacing = if (ui.compactMode) 4.dp else 8.dp
        LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(itemSpacing)) {
            items(filtered, key = { it.id }) { item -> AchievementRow(item = item, compact = ui.compactMode, onToggle = vm::toggle) }
        }
    }
}

@Composable
private fun SummaryCard(done: Int, total: Int) {
    val progress = if (total == 0) 0f else done.toFloat() / total.toFloat()
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("总进度", style = MaterialTheme.typography.labelMedium)
            Text("$done / $total", style = MaterialTheme.typography.titleLarge)
            Text("剩余 ${total - done} · ${"%.1f".format(progress * 100)}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)).background(Color(0x14000000))) {
                Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
            }
        }
    }
}

@Composable
private fun AchievementRow(item: AchievementItem, compact: Boolean, onToggle: (AchievementItem, Boolean) -> Unit) {
    val hPad = if (compact) 10.dp else 12.dp
    val vPad = if (compact) 6.dp else 8.dp

    Card(
        shape = RoundedCornerShape(if (compact) 10.dp else 14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = hPad, vertical = vPad), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = item.progress, onCheckedChange = { onToggle(item, it) })
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                    Text(if (item.progress) "●" else "○", color = if (item.progress) MaterialTheme.colorScheme.secondary else Color(0x668E8E93))
                }
                Text(item.description, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = Color(0xCC8E8E93))
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
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
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
                        Box(modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(999.dp)).background(Color(0x12000000))) {
                            Box(modifier = Modifier.fillMaxWidth(p).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                        }
                        Text("${"%.0f".format(p * 100)}%", style = MaterialTheme.typography.labelMedium)
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
                color = Color(0x220A84FF),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(6f, 6f),
                size = Size(size.width - 12f, size.height - 12f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 10f, cap = StrokeCap.Round)
            )
            drawArc(
                color = Color(0xFF0A84FF),
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

        Text("数据管理", style = MaterialTheme.typography.titleLarge)
        OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Rounded.FileDownload, contentDescription = null)
            Text("  导出进度 JSON")
        }
        OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Rounded.FileUpload, contentDescription = null)
            Text("  导入进度 JSON")
        }
        OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Rounded.Restore, contentDescription = null)
            Text("  重置全部进度")
        }
    }
}
