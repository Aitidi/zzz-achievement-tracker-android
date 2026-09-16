package com.aitidi.zzztracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.aitidi.zzztracker.ui.theme.AppColors
import com.aitidi.zzztracker.viewmodel.SortMode
import com.aitidi.zzztracker.viewmodel.TrackerUiState
import com.aitidi.zzztracker.viewmodel.TrackerViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun FilterSheet(ui: TrackerUiState, versions: List<String>, categories: List<String>, vm: TrackerViewModel, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppColors.Background, contentWindowInsets = { WindowInsets.safeDrawing }, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
        Column(Modifier.padding(horizontal = PageGutter).padding(bottom = 16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("筛选", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = { vm.clearVersionFilter(); vm.clearCategoryFilter() }) { Text("重置") }
                TextButton(onClick = onDismiss) { Text("完成") }
            }
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
                SectionLabel("版本")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterOption("全部", ui.selectedVersions.isEmpty(), vm::clearVersionFilter)
                    versions.forEach { version -> FilterOption(versionLabel(version), version in ui.selectedVersions) { vm.toggleVersion(version) } }
                }
                SectionLabel("分类")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterOption("全部", ui.selectedCategories.isEmpty(), vm::clearCategoryFilter)
                    categories.forEach { category -> FilterOption(category, category in ui.selectedCategories) { vm.toggleCategory(category) } }
                }
            }
        }
    }
}

@Composable
private fun FilterOption(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label, Modifier.padding(vertical = 5.dp), style = MaterialTheme.typography.labelLarge) },
        modifier = Modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp), border = null,
        colors = FilterChipDefaults.filterChipColors(containerColor = AppColors.Surface, labelColor = AppColors.Text,
            selectedContainerColor = AppColors.Blue, selectedLabelColor = AppColors.Surface))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SortSheet(selected: SortMode, onSelect: (SortMode) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AppColors.Background,
        contentWindowInsets = { WindowInsets.safeDrawing }, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
        Column(Modifier.padding(horizontal = PageGutter).padding(bottom = 24.dp).verticalScroll(rememberScrollState())) {
            Text("排序方式", Modifier.padding(bottom = 20.dp), style = MaterialTheme.typography.titleLarge)
            Column(Modifier.clip(GroupShape).background(AppColors.Surface).selectableGroup()) {
                SortMode.entries.forEachIndexed { index, mode ->
                    Row(Modifier.fillMaxWidth().selectable(selected == mode, role = Role.RadioButton, onClick = { onSelect(mode) }).padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(mode.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        if (selected == mode) Icon(Icons.Outlined.Check, null, tint = AppColors.Blue)
                    }
                    if (index != SortMode.entries.lastIndex) GroupDivider(18)
                }
            }
        }
    }
}
