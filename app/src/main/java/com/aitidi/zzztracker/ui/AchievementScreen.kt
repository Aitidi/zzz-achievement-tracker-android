package com.aitidi.zzztracker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aitidi.zzztracker.model.AchievementItem
import com.aitidi.zzztracker.ui.theme.AppColors
import com.aitidi.zzztracker.viewmodel.TrackerUiState
import com.aitidi.zzztracker.viewmodel.TrackerViewModel

@Composable
internal fun AchievementScreen(
    ui: TrackerUiState,
    allItems: List<AchievementItem>,
    filtered: List<AchievementItem>,
    latestVersion: String?,
    vm: TrackerViewModel,
    onFilter: () -> Unit,
    onSort: () -> Unit,
) {
    val done = allItems.count { it.progress }
    val focus = LocalFocusManager.current
    val searchInteraction = remember { MutableInteractionSource() }
    val searchFocused by searchInteraction.collectIsFocusedAsState()
    val shortWindow = LocalConfiguration.current.screenHeightDp < 680
    BackHandler(searchFocused) { focus.clearFocus() }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    val detail = allItems.firstOrNull { it.id == detailId }
    val hasFilters = ui.selectedVersions.isNotEmpty() || ui.selectedCategories.isNotEmpty()
    val listState = rememberLazyListState()
    // Keep search controls visible when result sets change; header scrolls away on small screens.
    LaunchedEffect(ui.query, ui.onlyTodo, ui.selectedVersions, ui.selectedCategories, ui.sortMode) {
        listState.scrollToItem(0)
    }
    if (detail != null) {
        AlertDialog(
            onDismissRequest = { detailId = null },
            title = { Text(detail.name) },
            text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("${versionLabel(detail.version)} · ${detail.category}", color = AppColors.Secondary)
                Text(detail.description, style = MaterialTheme.typography.bodyLarge)
                if (ui.lockProgressEditing) Text("进度已锁定，请先在成就页解锁。", color = AppColors.Secondary)
            } },
            confirmButton = { TextButton(enabled = !ui.lockProgressEditing, onClick = { vm.toggle(detail, !detail.progress); detailId = null }) {
                Text(if (detail.progress) "标为未完成" else "标为已完成")
            } },
            dismissButton = { TextButton(onClick = { detailId = null }) { Text("关闭") } },
            containerColor = AppColors.Surface,
        )
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = PageGutter, end = PageGutter, bottom = 24.dp),
    ) {
        item(key = "header") {
            if (!searchFocused && !shortWindow) {
                PageTitle("成就", "绝区零" + (latestVersion?.let { " · ${versionLabel(it)}" } ?: "")) {
                    IconButton(onClick = vm::toggleLockProgressEditing) {
                        Icon(if (ui.lockProgressEditing) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                            contentDescription = if (ui.lockProgressEditing) "解锁进度编辑" else "锁定进度编辑", tint = AppColors.Blue)
                    }
                }
                SummaryCard(done, allItems.size)
            } else if (!searchFocused) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("成就", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    Text("${countLabel(done)} / ${countLabel(allItems.size)}", color = AppColors.Secondary)
                    IconButton(onClick = vm::toggleLockProgressEditing) {
                        Icon(if (ui.lockProgressEditing) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                            contentDescription = if (ui.lockProgressEditing) "解锁进度编辑" else "锁定进度编辑", tint = AppColors.Blue)
                    }
                }
            }
            val search: @Composable (Modifier) -> Unit = { modifier ->
                SearchBar(ui.query, vm::setQuery, searchInteraction, searchFocused, { focus.clearFocus() }, modifier)
            }
            if (LocalConfiguration.current.screenWidthDp > 500) {
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    search(Modifier.weight(1f))
                    TodoSegments(ui.onlyTodo, vm::setOnlyTodo, Modifier.weight(0.7f))
                }
            } else {
                search(Modifier.padding(top = 14.dp))
                TodoSegments(ui.onlyTodo, vm::setOnlyTodo, Modifier.padding(top = 14.dp))
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${countLabel(filtered.size)} 项成就", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = AppColors.Secondary)
                TextButton(onClick = onFilter, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Icon(Icons.Outlined.FilterAlt, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (hasFilters) "筛选 ${ui.selectedVersions.size + ui.selectedCategories.size}" else "筛选")
                }
                TextButton(onClick = onSort, contentPadding = PaddingValues(start = 8.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.Sort, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp)); Text("排序")
                }
            }
            if (ui.lockProgressEditing) Text("进度已锁定，点按右上角解锁", Modifier.padding(bottom = 12.dp), color = AppColors.Secondary, style = MaterialTheme.typography.bodySmall)
            if (hasFilters) Row(Modifier.padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text((ui.selectedVersions.sorted().map(::versionLabel) + ui.selectedCategories.sorted()).joinToString(" · "),
                    Modifier.weight(1f), color = AppColors.Blue, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                TextButton(onClick = { vm.clearVersionFilter(); vm.clearCategoryFilter() }) { Text("清除") }
            }
        }
        if (filtered.isEmpty()) {
            item(key = "empty") {
                Column(Modifier.fillMaxWidth().padding(vertical = 44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.SearchOff, null, Modifier.size(40.dp), tint = AppColors.Secondary)
                    Text(ui.dataError ?: if (allItems.isNotEmpty() && ui.onlyTodo && !hasFilters && ui.query.isBlank()) "全部完成，真棒！" else "没有找到成就",
                        Modifier.padding(top = 16.dp), style = MaterialTheme.typography.titleMedium)
                    Text(if (ui.dataError != null) "请重新打开应用后重试" else "试试其他关键词，或查看全部成就", Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.bodyMedium, color = AppColors.Secondary)
                    if (ui.query.isNotBlank() || hasFilters || ui.onlyTodo) TextButton(onClick = {
                        vm.setQuery(""); vm.clearVersionFilter(); vm.clearCategoryFilter(); vm.setOnlyTodo(false)
                    }) { Text("查看全部") }
                }
            }
        } else {
            itemsIndexed(filtered, key = { _, item -> item.id }, contentType = { _, _ -> "achievement" }) { index, item ->
                val shape = RoundedCornerShape(topStart = if (index == 0) 20.dp else 0.dp, topEnd = if (index == 0) 20.dp else 0.dp,
                    bottomStart = if (index == filtered.lastIndex) 20.dp else 0.dp, bottomEnd = if (index == filtered.lastIndex) 20.dp else 0.dp)
                Column(Modifier.clip(shape).background(AppColors.Surface)) {
                    AchievementRow(item, ui.compactMode, ui.lockProgressEditing, onToggle = { vm.toggle(item, !item.progress) },
                        onDetail = { focus.clearFocus(); detailId = item.id })
                    if (index < filtered.lastIndex) GroupDivider(60)
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(done: Int, total: Int) {
    val progress = if (total == 0) 0f else done.toFloat() / total
    Column(Modifier.fillMaxWidth().clip(GroupShape).background(AppColors.Surface).padding(20.dp)) {
        Text("总进度", style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.fillMaxWidth().padding(top = 3.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${countLabel(done)} / ${countLabel(total)}", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            Text("${(progress * 100).toInt()}%", color = AppColors.Secondary, style = MaterialTheme.typography.bodyMedium)
        }
        ProgressTrack(progress)
    }
}

@Composable
private fun TodoSegments(onlyTodo: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(AppColors.Control)
        .padding(3.dp).selectableGroup()) {
        listOf(false to "全部", true to "未完成").forEach { (value, label) ->
            val active = onlyTodo == value
            Box(Modifier.weight(1f).then(if (active) Modifier.shadow(2.dp, RoundedCornerShape(9.dp)).background(AppColors.Surface, RoundedCornerShape(9.dp)) else Modifier)
                .clip(RoundedCornerShape(9.dp)).selectable(active, role = Role.Tab, onClick = { onChange(value) })
                .heightIn(min = 42.dp).padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                Text(label, color = if (active) AppColors.Text else AppColors.Secondary, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun AchievementRow(item: AchievementItem, compact: Boolean, locked: Boolean, onToggle: () -> Unit, onDetail: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 96.dp).padding(start = 6.dp, end = 12.dp, top = if (compact) 12.dp else 18.dp, bottom = if (compact) 12.dp else 18.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).toggleable(item.progress, enabled = !locked, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { contentDescription = item.name; stateDescription = if (item.progress) "已完成" else "未完成" }, contentAlignment = Alignment.Center) {
            Box(Modifier.size(25.dp).clip(CircleShape).background(if (item.progress) AppColors.Blue else AppColors.Surface)
                .border(1.5.dp, if (item.progress) AppColors.Blue else AppColors.Secondary.copy(alpha = if (locked) 0.3f else 0.6f), CircleShape), contentAlignment = Alignment.Center) {
                if (item.progress) Icon(Icons.Outlined.Check, null, Modifier.size(17.dp), tint = AppColors.Surface)
            }
        }
        Row(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable(onClickLabel = "查看成就详情", onClick = onDetail).padding(start = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, color = if (item.progress) AppColors.Secondary else AppColors.Text,
                    maxLines = if (compact) 1 else 3, overflow = TextOverflow.Ellipsis)
                Text(item.description, Modifier.padding(top = 3.dp), style = MaterialTheme.typography.bodyMedium, color = AppColors.Secondary,
                    maxLines = if (compact) 1 else 3, overflow = TextOverflow.Ellipsis)
                Text("${versionLabel(item.version)}  ·  ${item.category}", Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall,
                    color = AppColors.Secondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Outlined.ChevronRight, null, Modifier.padding(start = 6.dp).size(18.dp), tint = AppColors.Secondary.copy(alpha = 0.55f))
        }
    }
}


@Composable
private fun SearchBar(
    query: String,
    onQuery: (String) -> Unit,
    interaction: MutableInteractionSource,
    focused: Boolean,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BasicTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp)).background(AppColors.Control)
                .semantics { contentDescription = "搜索成就" },
            singleLine = true,
            interactionSource = interaction,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = AppColors.Text),
            cursorBrush = SolidColor(AppColors.Blue),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onDone() }),
            decorationBox = { inner ->
                Row(Modifier.padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Search, null, tint = AppColors.Secondary, modifier = Modifier.size(21.dp))
                    Box(Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 10.dp)) {
                        if (query.isEmpty()) Text("搜索成就", color = AppColors.Secondary, style = MaterialTheme.typography.bodyLarge)
                        inner()
                    }
                    if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Outlined.Cancel, "清空搜索", tint = AppColors.Secondary, modifier = Modifier.size(18.dp))
                    }
                }
            },
        )
        if (focused) TextButton(onClick = onDone) { Text("完成") }
    }
}
