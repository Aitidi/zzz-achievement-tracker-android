package com.aitidi.zzztracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aitidi.zzztracker.model.AchievementItem

private enum class Tab(val title: String) { LIST("成就"), STATS("统计"), SETTINGS("设置") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerApp() {
    var currentTab by remember { mutableStateOf(Tab.LIST) }
    val allItems = remember {
        mutableStateListOf(
            AchievementItem("zzz_1", "自费鞭策自己", "解锁六分街的「COFF CAFE」。", "v1.0", "雅努斯区", true),
            AchievementItem("zzz_2", "拼装达人", "将任意音擎等级提升至20级。", "v1.0", "绳匠业务", false),
            AchievementItem("zzz_3", "迷宫的跨越者", "通关第100阶层。", "v1.3", "战斗成就", false),
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("ZZZ 成就追踪器") }) },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = {},
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { padding ->
        when (currentTab) {
            Tab.LIST -> AchievementListScreen(items = allItems, modifier = Modifier.padding(padding))
            Tab.STATS -> StatsScreen(items = allItems, modifier = Modifier.padding(padding))
            Tab.SETTINGS -> SettingsScreen(modifier = Modifier.padding(padding))
        }
    }
}

@Composable
private fun AchievementListScreen(items: List<AchievementItem>, modifier: Modifier = Modifier) {
    var onlyTodo by remember { mutableStateOf(true) }
    val display = if (onlyTodo) items.filter { !it.进度 } else items

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("共  项", style = MaterialTheme.typography.titleMedium)
            FilterChip(selected = onlyTodo, onClick = { onlyTodo = !onlyTodo }, label = { Text("仅未完成") })
        }

        LazyColumn(contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(display, key = { it.id }) { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = item.进度, onCheckedChange = null)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.成就名, style = MaterialTheme.typography.titleMedium)
                        Text(item.描述, style = MaterialTheme.typography.bodyMedium)
                        Text(" · ", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsScreen(items: List<AchievementItem>, modifier: Modifier = Modifier) {
    val done = items.count { it.进度 }
    val total = items.size
    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("总进度", style = MaterialTheme.typography.titleLarge)
        Text(" / ", style = MaterialTheme.typography.headlineMedium)
        Text("后续将加入：按分类/版本统计、图表、最近完成。")
    }
}

@Composable
private fun SettingsScreen(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("设置", style = MaterialTheme.typography.titleLarge)
        Text("- 导入成就库 JSON")
        Text("- 导出/导入本地进度 JSON")
        Text("- 勾选后自动跳下一条（开关）")
        Text("- 默认仅看未完成（开关）")
    }
}
