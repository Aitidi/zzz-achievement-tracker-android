package com.aitidi.zzztracker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aitidi.zzztracker.model.AchievementCategories
import com.aitidi.zzztracker.model.AchievementItem
import com.aitidi.zzztracker.ui.theme.AppColors
import com.aitidi.zzztracker.ui.theme.numberStyle
import com.aitidi.zzztracker.viewmodel.GroupMode
import com.aitidi.zzztracker.viewmodel.SortMode
import com.aitidi.zzztracker.viewmodel.StatusFilter
import com.aitidi.zzztracker.viewmodel.TrackerUiState
import com.aitidi.zzztracker.viewmodel.TrackerViewModel

private val categoryOrder = AchievementCategories.groups.flatMap { it.categories }

/** Groups the filtered list into version or category sections, in display order. */
internal fun sectionsOf(filtered: List<AchievementItem>, groupMode: GroupMode, sortMode: SortMode): List<Pair<String, List<AchievementItem>>> =
    when (groupMode) {
        GroupMode.VERSION -> filtered.groupBy { it.version }.toList().sortedWith { a, b ->
            if (sortMode == SortMode.VERSION_DESC) compareVersionText(b.first, a.first) else compareVersionText(a.first, b.first)
        }
        GroupMode.CATEGORY -> filtered.groupBy { it.category }.toList().sortedWith(
            compareBy<Pair<String, List<AchievementItem>>> { categoryOrder.indexOf(it.first).let { i -> if (i < 0) Int.MAX_VALUE else i } }.thenBy { it.first })
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AchievementScreen(
    ui: TrackerUiState,
    allItems: List<AchievementItem>,
    filtered: List<AchievementItem>,
    vm: TrackerViewModel,
    selection: Set<String>?,
    onSelectionChange: (Set<String>?) -> Unit,
    onFilter: () -> Unit,
) {
    val focus = LocalFocusManager.current
    var searching by rememberSaveable { mutableStateOf(ui.query.isNotEmpty()) }
    var menuOpen by remember { mutableStateOf(false) }
    var collapsed by remember { mutableStateOf(emptySet<String>()) }
    val closeSearch = { searching = false; vm.setQuery(""); focus.clearFocus() }
    BackHandler(searching && selection == null) { closeSearch() }

    val sections = remember(filtered, ui.groupMode, ui.sortMode) { sectionsOf(filtered, ui.groupMode, ui.sortMode) }
    val sectionTally = remember(allItems, ui.groupMode) {
        allItems.groupBy { if (ui.groupMode == GroupMode.VERSION) it.version else it.category }.mapValues { it.value.tally() }
    }
    val total = remember(allItems) { allItems.tally() }
    val filterCount = ui.selectedVersions.size + ui.selectedCategories.size
    val listState = rememberLazyListState()
    LaunchedEffect(ui.query, ui.status, ui.selectedVersions, ui.selectedCategories, ui.sortMode, ui.groupMode) {
        listState.scrollToItem(0)
    }

    Column(Modifier.fillMaxSize()) {
        if (searching) {
            TopBar(null) { SearchField(ui.query, vm::setQuery, onCancel = closeSearch, onDone = { focus.clearFocus() }) }
        } else {
            TopBar("成就") {
                Text("${countLabel(total.done)}/${countLabel(total.total)}", Modifier.padding(end = 4.dp), style = numberStyle(16), color = AppColors.Secondary)
                IconButton(onClick = { searching = true }) { Icon(Icons.Outlined.Search, "搜索成就", tint = AppColors.Text) }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(if (ui.lockProgressEditing) Icons.Outlined.Lock else Icons.Outlined.MoreVert, "更多操作",
                            tint = if (ui.lockProgressEditing) AppColors.AccentText else AppColors.Text)
                    }
                    DropdownMenu(menuOpen, onDismissRequest = { menuOpen = false }, containerColor = AppColors.Surface) {
                        DropdownMenuItem(text = { Text("多选") }, leadingIcon = { Icon(Icons.Outlined.Checklist, null) },
                            onClick = { menuOpen = false; onSelectionChange(emptySet()) })
                        DropdownMenuItem(text = { Text(if (ui.lockProgressEditing) "解锁进度编辑" else "锁定进度编辑") },
                            leadingIcon = { Icon(if (ui.lockProgressEditing) Icons.Outlined.LockOpen else Icons.Outlined.Lock, null) },
                            onClick = { menuOpen = false; vm.toggleLockProgressEditing() })
                        DropdownMenuItem(text = { Text("版本排序：${ui.sortMode.label}") }, leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Sort, null) },
                            onClick = {
                                menuOpen = false
                                vm.setSortMode(if (ui.sortMode == SortMode.VERSION_DESC) SortMode.VERSION_ASC else SortMode.VERSION_DESC)
                            })
                    }
                }
            }
        }
        Column(Modifier.padding(horizontal = PageGutter).padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SegmentedControl(StatusFilter.entries, ui.status, { it.label }, vm::setStatus, Modifier.weight(1f))
                val active = filterCount > 0
                Row(Modifier.heightIn(min = 42.dp).clip(RoundedCornerShape(11.dp)).background(if (active) AppColors.Accent else AppColors.Control)
                    .clickable(role = Role.Button, onClick = { focus.clearFocus(); onFilter() }).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.FilterList, null, Modifier.size(18.dp), tint = if (active) AppColors.OnAccent else AppColors.Text)
                    Text(if (active) " 筛选 $filterCount" else " 筛选", style = MaterialTheme.typography.labelLarge,
                        color = if (active) AppColors.OnAccent else AppColors.Text)
                }
            }
            if (filterCount > 0) ActiveFilterChips(ui, vm)
            if (ui.lockProgressEditing) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(AppColors.AccentSoft).padding(start = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, null, Modifier.size(16.dp), tint = AppColors.AccentText)
                    Text("进度已锁定，勾选暂不可用", Modifier.weight(1f).padding(start = 6.dp), style = MaterialTheme.typography.bodySmall, color = AppColors.AccentText)
                    TextButton(onClick = vm::toggleLockProgressEditing) { Text("解锁", color = AppColors.AccentText) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${countLabel(filtered.size)} 项", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = AppColors.Secondary)
                Text("分组", Modifier.padding(end = 6.dp), style = MaterialTheme.typography.bodySmall, color = AppColors.Secondary)
                SegmentedControl(GroupMode.entries, ui.groupMode, { it.label }, vm::setGroupMode, Modifier.width(128.dp), compact = true)
            }
        }
        if (filtered.isEmpty()) {
            EmptyState(ui, allItems.isNotEmpty(), filterCount > 0, onShowAll = { searching = false; vm.showEverything() })
            return@Column
        }
        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = PageGutter, end = PageGutter, bottom = 24.dp)) {
            sections.forEach { (key, rows) ->
                val isCollapsed = key in collapsed
                stickyHeader(key = "section:$key", contentType = "header") {
                    SectionHeader(
                        title = if (ui.groupMode == GroupMode.VERSION) versionLabel(key) else key,
                        isVersion = ui.groupMode == GroupMode.VERSION,
                        tally = sectionTally[key] ?: rows.tally(),
                        collapsed = isCollapsed,
                        onToggle = { collapsed = if (isCollapsed) collapsed - key else collapsed + key },
                    )
                }
                if (!isCollapsed) itemsIndexed(rows, key = { _, item -> item.id }, contentType = { _, _ -> "achievement" }) { index, item ->
                    val shape = RoundedCornerShape(
                        topStart = if (index == 0) 16.dp else 0.dp, topEnd = if (index == 0) 16.dp else 0.dp,
                        bottomStart = if (index == rows.lastIndex) 16.dp else 0.dp, bottomEnd = if (index == rows.lastIndex) 16.dp else 0.dp)
                    Column(Modifier.animateItem().clip(shape).background(AppColors.Surface)) {
                        AchievementRow(
                            item = item,
                            meta = if (ui.groupMode == GroupMode.VERSION) item.category else versionLabel(item.version),
                            locked = ui.lockProgressEditing,
                            selecting = selection != null,
                            selected = selection?.contains(item.id) == true,
                            onToggle = { vm.toggle(item, !item.progress) },
                            onSelect = {
                                if (selection != null) onSelectionChange(if (item.id in selection) selection - item.id else selection + item.id)
                            },
                            onLongClick = {
                                focus.clearFocus()
                                onSelectionChange((selection ?: emptySet()) + item.id)
                            },
                        )
                        if (index < rows.lastIndex) GroupDivider(if (selection != null) 46.dp else 14.dp)
                    }
                }
                item(key = "gap:$key", contentType = "gap") { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, isVersion: Boolean, tally: Tally, collapsed: Boolean, onToggle: () -> Unit) {
    val rotation by animateFloatAsState(if (collapsed) -90f else 0f, label = "chevron")
    Row(Modifier.fillMaxWidth().background(AppColors.Background)
        .clickable(role = Role.Button, onClickLabel = if (collapsed) "展开" else "收起", onClick = onToggle)
        .semantics(mergeDescendants = true) {
            heading()
            stateDescription = if (collapsed) "已收起" else "已展开"
        }
        .padding(start = 2.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.ExpandMore, null, Modifier.size(18.dp).rotate(rotation), tint = AppColors.Secondary)
        Text(title, Modifier.padding(start = 6.dp), style = if (isVersion) numberStyle(19) else MaterialTheme.typography.titleSmall)
        Text("${tally.done}/${tally.total}", Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.labelMedium, color = AppColors.Secondary)
        StripedProgress(tally.ratio, Modifier.width(56.dp), height = 5.dp)
        Spacer(Modifier.weight(1f))
        if (tally.complete) Text("已清空", style = MaterialTheme.typography.labelMedium, color = AppColors.AccentText)
    }
}

/** Only the checkbox changes progress; a long press starts multi-select, where tapping the card selects it. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AchievementRow(
    item: AchievementItem,
    meta: String,
    locked: Boolean,
    selecting: Boolean,
    selected: Boolean,
    onToggle: () -> Unit,
    onSelect: () -> Unit,
    onLongClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(Modifier.fillMaxWidth().background(if (selected) AppColors.AccentSoft else AppColors.Surface)
        .combinedClickable(
            interactionSource = interaction,
            indication = if (selecting) LocalIndication.current else null,
            onClickLabel = if (selecting) (if (selected) "取消选择" else "选择") else null,
            onLongClickLabel = if (selecting) null else "进入多选",
            onClick = onSelect, onLongClick = onLongClick)
        .then(if (selecting) Modifier.semantics { this.selected = selected } else Modifier)
        .padding(start = if (selecting) 12.dp else 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (selecting) {
            Box(Modifier.padding(end = 12.dp).size(22.dp).clip(CircleShape)
                .background(if (selected) AppColors.Text else AppColors.Surface)
                .border(2.dp, if (selected) AppColors.Text else AppColors.Secondary, CircleShape), contentAlignment = Alignment.Center) {
                if (selected) Icon(Icons.Outlined.Check, null, Modifier.size(14.dp), tint = AppColors.Surface)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.titleMedium, color = if (item.progress) AppColors.Secondary else AppColors.Text)
            Text(item.description, Modifier.padding(top = 2.dp), style = MaterialTheme.typography.bodyMedium, color = AppColors.Secondary)
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(meta, Modifier.clip(RoundedCornerShape(6.dp)).background(AppColors.Control).padding(horizontal = 6.dp),
                    style = MaterialTheme.typography.labelSmall, color = AppColors.Secondary, maxLines = 1)
                if (item.progress) Text("已完成", Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelSmall, color = AppColors.AccentText)
            }
        }
        if (!selecting) {
            Box(Modifier.size(48.dp)
                .toggleable(item.progress, role = Role.Checkbox, onValueChange = { onToggle() })
                .semantics { contentDescription = item.name; stateDescription = if (item.progress) "已完成" else "未完成" },
                contentAlignment = Alignment.Center) {
                Box(Modifier.size(26.dp).alpha(if (locked) 0.4f else 1f).clip(RoundedCornerShape(8.dp))
                    .background(if (item.progress) AppColors.Accent else AppColors.Surface)
                    .border(2.dp, if (item.progress) AppColors.Accent else AppColors.Secondary, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center) {
                    if (item.progress) Icon(Icons.Outlined.Check, null, Modifier.size(18.dp), tint = AppColors.OnAccent)
                }
            }
        }
    }
}

/** One scrollable line kept inside the page gutter; "清除" stays pinned at the end. */
@Composable
private fun ActiveFilterChips(ui: TrackerUiState, vm: TrackerViewModel) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).clip(RoundedCornerShape(50)).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ui.selectedVersions.sortedWith { a, b -> compareVersionText(b, a) }.forEach { v -> RemovableChip(versionLabel(v)) { vm.toggleVersion(v) } }
            ui.selectedCategories.sortedBy { categoryOrder.indexOf(it) }.forEach { c -> RemovableChip(c) { vm.toggleCategory(c) } }
        }
        Spacer(Modifier.width(6.dp))
        PlainChip("清除", AppColors.Control, AppColors.Secondary, "清除全部筛选", vm::clearFilters)
    }
}

@Composable
private fun PlainChip(label: String, background: androidx.compose.ui.graphics.Color, color: androidx.compose.ui.graphics.Color, clickLabel: String, onClick: () -> Unit) {
    Text(label, Modifier.clip(RoundedCornerShape(50)).background(background)
        .clickable(role = Role.Button, onClickLabel = clickLabel, onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1)
}

@Composable
private fun RemovableChip(label: String, onRemove: () -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(50)).background(AppColors.AccentSoft)
        .clickable(role = Role.Button, onClickLabel = "移除筛选条件 $label", onClick = onRemove)
        .padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = AppColors.AccentText, maxLines = 1)
        Icon(Icons.Outlined.Close, null, Modifier.padding(start = 2.dp).size(14.dp), tint = AppColors.AccentText)
    }
}

@Composable
private fun EmptyState(ui: TrackerUiState, hasData: Boolean, hasFilters: Boolean, onShowAll: () -> Unit) {
    val allDone = hasData && ui.status == StatusFilter.TODO && !hasFilters && ui.query.isBlank()
    Column(Modifier.fillMaxWidth().padding(horizontal = PageGutter, vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(if (allDone) Icons.Outlined.EmojiEvents else Icons.Outlined.SearchOff, null, Modifier.size(40.dp), tint = AppColors.Secondary)
        Text(ui.dataError ?: if (allDone) "全部完成了" else "没有符合条件的成就", Modifier.padding(top = 16.dp), style = MaterialTheme.typography.titleMedium)
        Text(if (ui.dataError != null) "请重新打开应用后重试" else "换个关键词，或查看全部成就", Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodyMedium, color = AppColors.Secondary)
        if (ui.dataError == null && !allDone) TextButton(onClick = onShowAll) { Text("查看全部", color = AppColors.AccentText) }
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit, onCancel: () -> Unit, onDone: () -> Unit) {
    val requester = remember { FocusRequester() }
    LaunchedEffect(Unit) { requester.requestFocus() }
    Row(Modifier.fillMaxWidth().padding(end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.weight(1f).focusRequester(requester).heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp))
                .background(AppColors.Surface).semantics { contentDescription = "搜索成就" },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = AppColors.Text),
            cursorBrush = SolidColor(AppColors.AccentDeep),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onDone() }),
            decorationBox = { inner ->
                Row(Modifier.padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Search, null, tint = AppColors.Secondary, modifier = Modifier.size(20.dp))
                    Box(Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 10.dp)) {
                        if (query.isEmpty()) Text("搜索名称、描述或分类", color = AppColors.Secondary, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                        inner()
                    }
                    if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Outlined.Cancel, "清空搜索", tint = AppColors.Secondary, modifier = Modifier.size(18.dp))
                    }
                }
            },
        )
        TextButton(onClick = onCancel) { Text("取消", color = AppColors.AccentText) }
    }
}
