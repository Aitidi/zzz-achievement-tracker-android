package com.aitidi.zzztracker.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aitidi.zzztracker.model.AchievementItem
import com.aitidi.zzztracker.ui.theme.AppColors
import java.text.NumberFormat

internal fun countLabel(count: Int): String = NumberFormat.getIntegerInstance().format(count)
internal fun percent(done: Int, total: Int): Int = if (total == 0) 0 else done * 100 / total
internal val GroupShape = RoundedCornerShape(16.dp)
internal val PageGutter = 16.dp

/** Completed and total counts for a slice of the catalog. */
internal data class Tally(val done: Int, val total: Int) {
    val ratio: Float get() = if (total == 0) 0f else done.toFloat() / total
    val remaining: Int get() = total - done
    val complete: Boolean get() = total > 0 && done == total
}

internal fun List<AchievementItem>.tally() = Tally(count { it.progress }, size)

/** Fixed-height bar shared by all three tabs so the page titles never shift when switching. */
@Composable
internal fun TopBar(title: String?, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().height(56.dp).padding(start = PageGutter, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (title != null) Text(title, Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.headlineLarge, maxLines = 1)
        actions()
    }
}

@Composable
internal fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 20.dp, bottom = 8.dp)) {
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = AppColors.Secondary)
        if (trailing != null) Text(trailing, style = MaterialTheme.typography.labelMedium, color = AppColors.Secondary)
    }
}

/** Amber progress bar with a hazard-stripe fill. */
@Composable
internal fun StripedProgress(progress: Float, modifier: Modifier = Modifier, height: Dp = 8.dp) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "progress")
    val track = AppColors.Control
    val accent = AppColors.Accent
    val stripe = AppColors.AccentDeep
    Canvas(modifier.fillMaxWidth().height(height)
        .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f) }) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(track, cornerRadius = radius)
        val width = size.width * animated
        if (width <= 0f) return@Canvas
        val fill = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, width, size.height, radius)) }
        clipPath(fill) {
            drawRect(accent, size = Size(width, size.height))
            val step = 9.dp.toPx()
            val band = 3.dp.toPx()
            var x = -size.height
            while (x < width) {
                val p = Path().apply {
                    moveTo(x + size.height, 0f); lineTo(x + size.height + band, 0f)
                    lineTo(x + band, size.height); lineTo(x, size.height); close()
                }
                drawPath(p, stripe)
                x += step
            }
        }
    }
}

@Composable
internal fun <T> SegmentedControl(
    options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier, compact: Boolean = false,
) {
    Row(modifier.clip(RoundedCornerShape(if (compact) 9.dp else 11.dp)).background(AppColors.Control).padding(if (compact) 2.dp else 3.dp).selectableGroup()) {
        options.forEach { option ->
            val active = option == selected
            Box(Modifier.weight(1f)
                .then(if (active) Modifier.shadow(1.dp, RoundedCornerShape(8.dp)).background(AppColors.Surface, RoundedCornerShape(8.dp)) else Modifier)
                .clip(RoundedCornerShape(8.dp))
                .selectable(active, role = Role.Tab, onClick = { onSelect(option) })
                .heightIn(min = if (compact) 28.dp else 36.dp).padding(horizontal = 6.dp, vertical = if (compact) 4.dp else 6.dp), contentAlignment = Alignment.Center) {
                Text(label(option), style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    color = if (active) AppColors.Text else AppColors.Secondary, maxLines = 1)
            }
        }
    }
}

@Composable
internal fun GroupDivider(start: Dp = 16.dp) {
    HorizontalDivider(Modifier.padding(start = start), thickness = 0.5.dp, color = AppColors.Separator)
}

@Composable
internal fun BottomTabBar(selected: HomeTab, onSelect: (HomeTab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(AppColors.Surface)) {
        HorizontalDivider(thickness = 0.5.dp, color = AppColors.Separator)
        Row(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .fillMaxWidth().heightIn(min = 64.dp).selectableGroup()) {
            HomeTab.entries.forEach { tab ->
                val active = selected == tab
                Column(Modifier.weight(1f).selectable(active, role = Role.Tab, onClick = { onSelect(tab) })
                    .padding(top = 8.dp, bottom = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(width = 52.dp, height = 28.dp).clip(RoundedCornerShape(14.dp))
                        .background(if (active) AppColors.Accent else AppColors.Surface), contentAlignment = Alignment.Center) {
                        Icon(tab.icon, contentDescription = null, tint = if (active) AppColors.OnAccent else AppColors.Secondary, modifier = Modifier.size(22.dp))
                    }
                    Text(tab.title, Modifier.padding(top = 3.dp), style = MaterialTheme.typography.labelSmall,
                        color = if (active) AppColors.Text else AppColors.Secondary, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                }
            }
        }
    }
}

/** Replaces the tab bar while the achievement list is in multi-select mode. */
@Composable
internal fun SelectionBar(count: Int, onMark: (Boolean) -> Unit, onCancel: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(AppColors.Text)
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
        .heightIn(min = 64.dp).padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("已选 $count 项", Modifier.weight(1f), color = AppColors.Surface, style = MaterialTheme.typography.titleSmall)
        TextButton(onClick = { onMark(false) }, enabled = count > 0) { Text("标为未完成", color = AppColors.Surface.copy(alpha = if (count > 0) 1f else 0.4f)) }
        Button(onClick = { onMark(true) }, enabled = count > 0, shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent, contentColor = AppColors.OnAccent)) { Text("标为已完成") }
        IconButton(onClick = onCancel) { Icon(Icons.Outlined.Close, "退出多选", tint = AppColors.Surface) }
    }
}

@Composable
internal fun SettingsIcon(icon: ImageVector, destructive: Boolean = false) {
    Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(if (destructive) AppColors.Danger.copy(alpha = 0.1f) else AppColors.AccentSoft),
        contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(20.dp), tint = if (destructive) AppColors.Danger else AppColors.AccentText)
    }
}

/** Splits [items] into rows of [columns], padding the last row so cells keep equal widths. */
@Composable
internal fun <T> GridRows(items: List<T>, columns: Int, spacing: Dp = 8.dp, cell: @Composable (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
        items.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                row.forEach { Box(Modifier.weight(1f).fillMaxHeight()) { cell(it) } }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
