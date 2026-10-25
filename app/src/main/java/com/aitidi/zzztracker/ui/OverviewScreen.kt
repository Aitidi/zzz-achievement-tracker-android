package com.aitidi.zzztracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitidi.zzztracker.model.AchievementCategories
import com.aitidi.zzztracker.model.AchievementItem
import com.aitidi.zzztracker.ui.theme.AppColors
import com.aitidi.zzztracker.ui.theme.numberStyle

/** Replaces the old statistics page: every block opens the matching to-do list. */
@Composable
internal fun OverviewScreen(
    allItems: List<AchievementItem>,
    versions: List<String>,
    dataError: String?,
    onVersion: (String) -> Unit,
    onCategories: (List<String>) -> Unit,
) {
    val total = remember(allItems) { allItems.tally() }
    val byVersion = remember(allItems) { allItems.groupBy { it.version }.mapValues { it.value.tally() } }
    val byGroup = remember(allItems) {
        AchievementCategories.groups.map { group -> group to allItems.filter { it.category in group.categories }.tally() }
    }
    val latest = versions.firstOrNull()
    val secondary = AppColors.Secondary
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = PageGutter).padding(bottom = 24.dp)) {
        PageTitle("总览", "绝区零" + (latest?.let { " · 成就数据 ${versionLabel(it)}" } ?: ""))
        if (dataError != null) {
            Text(dataError, Modifier.padding(vertical = 24.dp), color = AppColors.Danger)
            return@Column
        }
        Column(Modifier.fillMaxWidth().clip(GroupShape).background(AppColors.Surface).padding(16.dp)
            .semantics(mergeDescendants = true) { contentDescription = "总进度 ${total.done} / ${total.total}，${percent(total.done, total.total)}%" }) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(buildAnnotatedString {
                    append(countLabel(total.done))
                    withStyle(SpanStyle(color = secondary, fontSize = 24.sp)) { append(" / ${countLabel(total.total)}") }
                }, Modifier.weight(1f), style = numberStyle(48))
                Text("${percent(total.done, total.total)}%", style = numberStyle(26))
            }
            StripedProgress(total.ratio, Modifier.padding(top = 12.dp, bottom = 8.dp))
            Text(if (total.remaining == 0 && total.total > 0) "全部完成了" else "还有 ${countLabel(total.remaining)} 个成就未完成",
                style = MaterialTheme.typography.bodySmall, color = AppColors.Secondary)
        }
        if (latest != null) {
            val t = byVersion[latest] ?: Tally(0, 0)
            Row(Modifier.padding(top = 12.dp).fillMaxWidth().clip(GroupShape).background(AppColors.Text)
                .clickable(role = Role.Button, onClickLabel = "查看 ${versionLabel(latest)} 未完成成就") { onVersion(latest) }
                .padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(versionLabel(latest), Modifier.clip(RoundedCornerShape(10.dp)).background(AppColors.Accent).padding(horizontal = 10.dp, vertical = 2.dp),
                    style = numberStyle(22), color = AppColors.OnAccent)
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text(if (t.complete) "新版本已全部完成" else "新版本还差 ${t.remaining} 个", color = AppColors.Surface, style = MaterialTheme.typography.titleSmall)
                    Text("共 ${t.total} 个，已完成 ${t.done} 个", color = AppColors.Surface.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Outlined.ChevronRight, null, tint = AppColors.Surface)
            }
        }
        SectionLabel("按分类")
        GridRows(byGroup, columns = 2, spacing = 10.dp) { (group, t) ->
            Column(Modifier.fillMaxSize().clip(GroupShape).background(AppColors.Surface)
                .clickable(role = Role.Button, onClickLabel = "查看${group.title}类未完成成就") { onCategories(group.categories) }
                .padding(horizontal = 14.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(group.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Text("${percent(t.done, t.total)}%", style = numberStyle(20))
                }
                StripedProgress(t.ratio, Modifier.padding(vertical = 8.dp), height = 6.dp)
                Text("${countLabel(t.done)} / ${countLabel(t.total)} · 还差 ${t.remaining}", style = MaterialTheme.typography.bodySmall, color = AppColors.Secondary)
            }
        }
        SectionLabel("按版本", trailing = "点按查看未完成")
        GridRows(versions, columns = 4, spacing = 6.dp) { version ->
            val t = byVersion[version] ?: Tally(0, 0)
            VersionTile(version, t) { onVersion(version) }
        }
    }
}

@Composable
private fun VersionTile(version: String, tally: Tally, onClick: () -> Unit) {
    val full = tally.complete
    Box(Modifier.fillMaxWidth().heightIn(min = 58.dp).clip(RoundedCornerShape(12.dp)).background(AppColors.Surface)
        .clickable(role = Role.Button, onClickLabel = "查看${versionLabel(version)}未完成成就", onClick = onClick)
        .semantics(mergeDescendants = true) { contentDescription = "${versionLabel(version)}，已完成 ${tally.done} / ${tally.total}" }) {
        Box(Modifier.matchParentSize(), contentAlignment = Alignment.BottomStart) {
            Box(Modifier.fillMaxWidth().fillMaxHeight(tally.ratio).background(if (full) AppColors.Accent else AppColors.AccentSoft))
        }
        Column(Modifier.padding(horizontal = 8.dp, vertical = 7.dp)) {
            Text(versionLabel(version), style = numberStyle(17), color = if (full) AppColors.OnAccent else AppColors.Text)
            Text(if (full) "已清空" else "${tally.done}/${tally.total}", style = MaterialTheme.typography.labelSmall,
                color = if (full) AppColors.OnAccent else AppColors.Secondary)
        }
    }
}
