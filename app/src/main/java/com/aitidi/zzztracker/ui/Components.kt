package com.aitidi.zzztracker.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.aitidi.zzztracker.ui.theme.AppColors
import java.text.NumberFormat

internal fun countLabel(count: Int): String = NumberFormat.getIntegerInstance().format(count)
internal val GroupShape = RoundedCornerShape(20.dp)
internal val PageGutter = 24.dp

@Composable
internal fun PageTitle(title: String, subtitle: String, action: (@Composable () -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 22.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge)
            action?.invoke()
        }
        Text(subtitle, Modifier.padding(top = 3.dp), style = MaterialTheme.typography.bodyMedium, color = AppColors.Secondary)
    }
}

@Composable
internal fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.padding(start = 4.dp, top = 24.dp, bottom = 12.dp),
        style = MaterialTheme.typography.titleSmall, color = AppColors.Secondary)
}

@Composable
internal fun ProgressTrack(progress: Float, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "progress")
    Box(modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(AppColors.Track)
        .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f) }) {
        Box(Modifier.fillMaxWidth(animated).fillMaxHeight().clip(RoundedCornerShape(50)).background(AppColors.Blue))
    }
}

@Composable
internal fun ProgressRing(progress: Float, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "ring")
    Box(modifier.size(144.dp).semantics(mergeDescendants = true) { contentDescription = "总体完成度 ${(progress * 100).toInt()}%" }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 10.dp.toPx()
            val diameter = size.minDimension - stroke
            drawArc(AppColors.Track, -90f, 360f, false, Offset(stroke / 2, stroke / 2), Size(diameter, diameter), style = Stroke(stroke))
            if (animated > 0f) drawArc(AppColors.Blue, -90f, animated * 360f, false, Offset(stroke / 2, stroke / 2), Size(diameter, diameter), style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text("${(progress * 100).toInt()}%", Modifier.clearAndSetSemantics {}, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
internal fun GroupDivider(start: Int = 16) {
    HorizontalDivider(Modifier.padding(start = start.dp), thickness = 0.5.dp, color = AppColors.Separator)
}

@Composable
internal fun BottomTabBar(selected: HomeTab, onSelect: (HomeTab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(AppColors.Surface)) {
        HorizontalDivider(thickness = 0.5.dp, color = AppColors.Separator)
        Row(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .fillMaxWidth().heightIn(min = 64.dp).selectableGroup()) {
            HomeTab.entries.forEach { tab ->
                val active = selected == tab
                val color = if (active) AppColors.Blue else AppColors.Secondary
                Column(Modifier.weight(1f).selectable(active, role = Role.Tab, onClick = { onSelect(tab) })
                    .padding(vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(tab.icon, contentDescription = null, tint = color, modifier = Modifier.size(25.dp))
                    Text(tab.title, Modifier.padding(top = 3.dp), color = color, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
