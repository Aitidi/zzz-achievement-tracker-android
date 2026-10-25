package com.aitidi.zzztracker.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.aitidi.zzztracker.ui.theme.AppColors
import com.aitidi.zzztracker.viewmodel.ThemeMode
import com.aitidi.zzztracker.viewmodel.TrackerUiState
import kotlinx.coroutines.launch

@Composable
internal fun SettingsScreen(
    ui: TrackerUiState,
    catalogVersion: String?,
    catalogSize: Int,
    onTheme: (ThemeMode) -> Unit,
    onToggleLock: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onReset: () -> Unit,
    onHint: (String) -> Unit,
) {
    val context = LocalContext.current
    val appVersion = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() }
    Column(Modifier.fillMaxSize()) {
        TopBar("设置")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = PageGutter).padding(bottom = 24.dp)) {
            SectionLabel("外观", Modifier.padding(top = 0.dp))
            Row(Modifier.fillMaxWidth().clip(GroupShape).background(AppColors.Surface).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("主题", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                SegmentedControl(ThemeMode.entries, ui.themeMode, { it.label }, onTheme, Modifier.width(208.dp))
            }
            SectionLabel("进度")
            Row(Modifier.fillMaxWidth().clip(GroupShape).background(AppColors.Surface)
                .toggleable(ui.lockProgressEditing, role = Role.Switch, onValueChange = { onToggleLock() })
                .padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                SettingsIcon(Icons.Outlined.Lock)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text("锁定进度编辑", style = MaterialTheme.typography.titleMedium)
                    Text("防止误触，锁定后勾选、导入和重置都不可用", style = MaterialTheme.typography.bodySmall, color = AppColors.Secondary)
                }
                Switch(ui.lockProgressEditing, onCheckedChange = null, colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White, checkedTrackColor = AppColors.Accent, checkedBorderColor = AppColors.Accent,
                    uncheckedThumbColor = Color.White, uncheckedTrackColor = AppColors.Control, uncheckedBorderColor = AppColors.Separator))
            }
            SectionLabel("数据")
            Column(Modifier.clip(GroupShape).background(AppColors.Surface)) {
                SettingsRow("导出备份", "保存为 JSON 文件", Icons.Outlined.FileUpload, onClick = onExport)
                GroupDivider(64.dp)
                SettingsRow("从备份恢复", "会先显示条数，确认后才覆盖", Icons.Outlined.FileDownload, onClick = onImport)
            }
            HoldToResetButton(Modifier.padding(top = 12.dp), enabled = !ui.lockProgressEditing, onReset = onReset, onHint = onHint)
            Text("重置后底部会出现“撤销”。所有进度只保存在本机。", Modifier.fillMaxWidth().padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall, color = AppColors.Secondary)
            SectionLabel("关于")
            Column(Modifier.clip(GroupShape).background(AppColors.Surface)) {
                InfoRow("成就数据", (catalogVersion?.let { versionLabel(it) + " · " } ?: "") + "共 ${countLabel(catalogSize)} 个")
                GroupDivider()
                InfoRow("应用版本", appVersion ?: "—")
            }
        }
    }
}

/** Fills over two seconds while pressed; releasing early cancels, so a stray tap can never wipe progress. */
@Composable
private fun HoldToResetButton(modifier: Modifier, enabled: Boolean, onReset: () -> Unit, onHint: (String) -> Unit) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val danger = AppColors.Danger
    Box(modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(14.dp)).background(AppColors.Surface)
        .semantics {
            role = Role.Button
            contentDescription = "重置全部进度，需按住 2 秒"
            customActions = listOf(CustomAccessibilityAction("重置全部进度") { onReset(); true })
        }
        .pointerInput(enabled) {
            detectTapGestures(onPress = {
                if (!enabled) { onHint("进度已锁定，请先解锁后重置"); return@detectTapGestures }
                val fill = scope.launch {
                    progress.animateTo(1f, tween(2000, easing = LinearEasing))
                    onReset()
                    progress.snapTo(0f)
                }
                tryAwaitRelease()
                if (fill.isActive) {
                    fill.cancel()
                    onHint("请按住按钮 2 秒")
                    scope.launch { progress.animateTo(0f, tween(150)) }
                }
            })
        }, contentAlignment = Alignment.Center) {
        Box(Modifier.matchParentSize(), contentAlignment = Alignment.CenterStart) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(progress.value).background(danger.copy(alpha = 0.16f)))
        }
        Text("按住 2 秒重置全部进度", color = if (enabled) danger else danger.copy(alpha = 0.4f), style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        SettingsIcon(icon)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AppColors.Secondary)
        }
        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(20.dp), tint = AppColors.Secondary)
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = AppColors.Secondary)
    }
}
