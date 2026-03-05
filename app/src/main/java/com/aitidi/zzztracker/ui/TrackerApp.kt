package com.aitidi.zzztracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aitidi.zzztracker.model.AchievementItem
import com.aitidi.zzztracker.viewmodel.TrackerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerApp(vm: TrackerViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val allItems by vm.items.collectAsStateWithLifecycle()
    val versions = listOf("全部") + allItems.map { it.version }.distinct().sorted()

    val filtered = allItems.filter {
        val todoOk = !ui.onlyTodo || !it.progress
        val q = ui.query.trim()
        val qOk = q.isBlank() || it.name.contains(q, true) || it.description.contains(q, true) || it.category.contains(q, true)
        val verOk = ui.selectedVersion == "全部" || it.version == ui.selectedVersion
        todoOk && qOk && verOk
    }

    Scaffold(topBar = { TopAppBar(title = { Text("ZZZ 成就追踪器") }) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("已完成 ${allItems.count { it.progress }} / ${allItems.size}", style = MaterialTheme.typography.titleMedium)
                FilterChip(selected = ui.onlyTodo, onClick = { vm.setOnlyTodo(!ui.onlyTodo) }, label = { Text("仅未完成") })
            }

            OutlinedTextField(
                value = ui.query,
                onValueChange = vm::setQuery,
                label = { Text("搜索成就/描述/分类") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(versions) { ver ->
                    FilterChip(
                        selected = ui.selectedVersion == ver,
                        onClick = { vm.setVersion(ver) },
                        label = { Text(ver) }
                    )
                }
            }

            LazyColumn(contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { item ->
                    AchievementRow(item = item, onToggle = vm::toggle)
                }
            }
        }
    }
}

@Composable
private fun AchievementRow(item: AchievementItem, onToggle: (AchievementItem, Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = item.progress, onCheckedChange = { onToggle(item, it) })
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.titleMedium)
            Text(item.description, style = MaterialTheme.typography.bodyMedium)
            Text("${item.category} · ${item.version}", style = MaterialTheme.typography.labelSmall)
        }
    }
}
