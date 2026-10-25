package com.aitidi.zzztracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aitidi.zzztracker.model.AchievementCategories
import com.aitidi.zzztracker.model.AchievementItem
import com.aitidi.zzztracker.ui.theme.AppColors
import com.aitidi.zzztracker.ui.theme.numberStyle
import com.aitidi.zzztracker.viewmodel.TrackerUiState
import com.aitidi.zzztracker.viewmodel.TrackerViewModel

/** Every version and category on one sheet, each with its completion, applied live. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterSheet(
    ui: TrackerUiState,
    allItems: List<AchievementItem>,
    versions: List<String>,
    categories: List<String>,
    resultCount: Int,
    vm: TrackerViewModel,
    onDismiss: () -> Unit,
) {
    val byVersion = remember(allItems) { allItems.groupBy { it.version }.mapValues { it.value.tally() } }
    val byCategory = remember(allItems) { allItems.groupBy { it.category }.mapValues { it.value.tally() } }
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.fillMaxHeight(0.92f),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppColors.Background, contentWindowInsets = { WindowInsets.safeDrawing },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("筛选", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = vm::clearFilters) { Text("重置", color = AppColors.AccentText) }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = PageGutter).padding(bottom = 12.dp)) {
                SectionLabel("版本")
                GridRows(versions, columns = 4, spacing = 6.dp) { v ->
                    val t = byVersion[v] ?: Tally(0, 0)
                    FilterOption(versionLabel(v), if (t.complete) "已清空" else "${t.done}/${t.total}", v in ui.selectedVersions, isVersion = true) { vm.toggleVersion(v) }
                }
                SectionLabel("分类", Modifier.padding(top = 4.dp))
                AchievementCategories.groups.forEach { group ->
                    val present = group.categories.filter { it in categories }
                    if (present.isEmpty()) return@forEach
                    Text(group.title, Modifier.padding(start = 4.dp, top = 8.dp, bottom = 6.dp), style = MaterialTheme.typography.labelMedium, color = AppColors.Secondary)
                    CategoryGrid(present, byCategory, ui.selectedCategories, vm::toggleCategory)
                }
                // Keep future catalog additions reachable before they are assigned a group.
                val known = AchievementCategories.groups.flatMap { it.categories }.toSet()
                val ungrouped = categories.filterNot { it in known }
                if (ungrouped.isNotEmpty()) {
                    Text("其他", Modifier.padding(start = 4.dp, top = 8.dp, bottom = 6.dp), style = MaterialTheme.typography.labelMedium, color = AppColors.Secondary)
                    CategoryGrid(ungrouped, byCategory, ui.selectedCategories, vm::toggleCategory)
                }
            }
            Button(onClick = onDismiss, Modifier.fillMaxWidth().padding(horizontal = PageGutter, vertical = 12.dp).heightIn(min = 48.dp),
                shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent, contentColor = AppColors.OnAccent)) {
                Text("显示 ${countLabel(resultCount)} 项", style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun CategoryGrid(categories: List<String>, tallies: Map<String, Tally>, selected: Set<String>, onToggle: (String) -> Unit) {
    GridRows(categories, columns = 3, spacing = 6.dp) { c ->
        val t = tallies[c] ?: Tally(0, 0)
        FilterOption(c, "${t.done}/${t.total}", c in selected, isVersion = false) { onToggle(c) }
    }
}

@Composable
private fun FilterOption(label: String, detail: String, selected: Boolean, isVersion: Boolean, onToggle: () -> Unit) {
    Column(Modifier.fillMaxSize().clip(RoundedCornerShape(11.dp))
        .background(if (selected) AppColors.AccentSoft else AppColors.Surface)
        .border(2.dp, if (selected) AppColors.AccentDeep else Color.Transparent, RoundedCornerShape(11.dp))
        .toggleable(selected, role = Role.Checkbox, onValueChange = { onToggle() })
        .heightIn(min = 48.dp).padding(horizontal = 8.dp, vertical = 6.dp)) {
        Text(label, style = if (isVersion) numberStyle(16) else MaterialTheme.typography.labelLarge, color = AppColors.Text,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(detail, style = MaterialTheme.typography.labelSmall, color = AppColors.Secondary)
    }
}
