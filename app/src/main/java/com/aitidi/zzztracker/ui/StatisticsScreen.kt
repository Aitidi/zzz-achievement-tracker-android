package com.aitidi.zzztracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aitidi.zzztracker.model.AchievementItem
import com.aitidi.zzztracker.ui.theme.AppColors

@Composable
internal fun StatisticsScreen(allItems: List<AchievementItem>) {
    val done = allItems.count { it.progress }
    val progress = if (allItems.isEmpty()) 0f else done.toFloat() / allItems.size
    val grouped = remember(allItems) {
        allItems.groupBy { it.version }.toList().sortedWith { a, b -> compareVersionText(b.first, a.first) }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = PageGutter, end = PageGutter, bottom = 24.dp)) {
        item {
            PageTitle("统计", "每一步，都算数")
            Column(Modifier.fillMaxWidth().clip(GroupShape).background(AppColors.Surface).padding(20.dp)) {
                Text("总体完成度", style = MaterialTheme.typography.titleMedium)
                BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 20.dp)) {
                    if (maxWidth < 260.dp) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            ProgressRing(progress)
                            Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.SpaceAround) {
                                StatNumber("已完成", done)
                                StatNumber("待完成", allItems.size - done)
                            }
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            ProgressRing(progress)
                            Box(Modifier.padding(horizontal = 24.dp).width(0.5.dp).height(138.dp).background(AppColors.Separator))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                                StatNumber("已完成", done)
                                StatNumber("待完成", allItems.size - done)
                            }
                        }
                    }
                }
            }
            SectionLabel("分版本进度")
        }
        if (grouped.isEmpty()) item { Text("暂无可统计的成就数据", color = AppColors.Secondary) }
        itemsIndexed(grouped, key = { _, group -> group.first }) { index, (version, entries) ->
            val completed = entries.count { it.progress }
            val ratio = completed.toFloat() / entries.size
            val shape = RoundedCornerShape(topStart = if (index == 0) 20.dp else 0.dp, topEnd = if (index == 0) 20.dp else 0.dp,
                bottomStart = if (index == grouped.lastIndex) 20.dp else 0.dp, bottomEnd = if (index == grouped.lastIndex) 20.dp else 0.dp)
            Column(Modifier.clip(shape).background(AppColors.Surface)) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Text(versionLabel(version), style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth().padding(top = 3.dp, bottom = 9.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${countLabel(completed)} / ${countLabel(entries.size)}", style = MaterialTheme.typography.bodyMedium, color = AppColors.Secondary)
                        Text("${(ratio * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = AppColors.Secondary)
                    }
                    ProgressTrack(ratio)
                }
                if (index < grouped.lastIndex) GroupDivider(20)
            }
        }
    }
}

@Composable
private fun StatNumber(label: String, count: Int) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = AppColors.Secondary)
        Text(countLabel(count), Modifier.padding(top = 3.dp), style = MaterialTheme.typography.titleLarge)
    }
}
