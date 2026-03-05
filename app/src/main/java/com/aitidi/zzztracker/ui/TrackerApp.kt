package com.aitidi.zzztracker.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aitidi.zzztracker.model.AchievementItem
import com.aitidi.zzztracker.viewmodel.TrackerViewModel

private enum class HomeTab(val title: String) { LIST("成就"), STATS("统计"), SETTINGS("设置") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerApp(vm: TrackerViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val allItems by vm.items.collectAsStateWithLifecycle()
    val versions = listOf("全部") + allItems.map { it.version }.distinct().sorted()
    var tab by remember { mutableStateOf(HomeTab.LIST) }
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
        todoOk && qOk && verOk
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Zenless 成就追踪") }) },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                HomeTab.entries.forEach {
                    NavigationBarItem(selected = tab == it, onClick = { tab = it }, icon = {}, label = { Text(it.title) })
                }
            }
        }
    ) { padding ->
        when (tab) {
            HomeTab.LIST -> ListTab(padding, ui.onlyTodo, ui.query, ui.selectedVersion, versions, allItems, filtered, vm)
            HomeTab.STATS -> StatsTab(padding, allItems)
            HomeTab.SETTINGS -> SettingsTab(
                padding = padding,
                onExport = { createExportLauncher.launch("zzz_progress_backup.json") },
                onImport = { importLauncher.launch(arrayOf("application/json")) },
                onReset = vm::resetProgress,
            )
        }
    }
}

@Composable
private fun ListTab(
    padding: PaddingValues,
    onlyTodo: Boolean,
    query: String,
    selectedVersion: String,
    versions: List<String>,
    allItems: List<AchievementItem>,
    filtered: List<AchievementItem>,
    vm: TrackerViewModel,
) {
    val done = allItems.count { it.progress }

    Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 12.dp)) {
        ProgressHero(done = done, total = allItems.size)

        OutlinedTextField(
            value = query,
            onValueChange = vm::setQuery,
            label = { Text("搜索成就/描述/分类") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            FilterChip(selected = onlyTodo, onClick = { vm.setOnlyTodo(!onlyTodo) }, label = { Text("仅未完成") })
            Text("当前显示 ${filtered.size} 项", style = MaterialTheme.typography.labelLarge)
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(versions) { ver ->
                FilterChip(
                    selected = selectedVersion == ver,
                    onClick = { vm.setVersion(ver) },
                    label = { Text(ver) }
                )
            }
        }

        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.id }) { item -> AchievementCard(item = item, onToggle = vm::toggle) }
        }
    }
}

@Composable
private fun ProgressHero(done: Int, total: Int) {
    val progress = if (total == 0) 0f else done.toFloat() / total.toFloat()
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f),
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("完成进度", style = MaterialTheme.typography.labelLarge)
                Text("$done / $total", style = MaterialTheme.typography.headlineMedium)
                Text("${"%.1f".format(progress * 100)}%", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun AchievementCard(item: AchievementItem, onToggle: (AchievementItem, Boolean) -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = item.progress, onCheckedChange = { onToggle(item, it) })
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Text(item.description, style = MaterialTheme.typography.bodyMedium)
                Text("${item.category} · ${item.version}", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun StatsTab(padding: PaddingValues, allItems: List<AchievementItem>) {
    val grouped = allItems.groupBy { it.version }.toSortedMap()
    Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ProgressHero(done = allItems.count { it.progress }, total = allItems.size)
        Text("按版本统计", style = MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            grouped.forEach { (ver, list) ->
                val done = list.count { it.progress }
                item {
                    Card(shape = RoundedCornerShape(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(ver)
                            Text("$done / ${list.size}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTab(
    padding: PaddingValues,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onReset: () -> Unit,
) {
    Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("数据管理", style = MaterialTheme.typography.titleLarge)
        OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.FileDownload, contentDescription = null)
            Text("  导出进度 JSON")
        }
        OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.FileUpload, contentDescription = null)
            Text("  导入进度 JSON")
        }
        OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Restore, contentDescription = null)
            Text("  重置全部进度")
        }
    }
}
