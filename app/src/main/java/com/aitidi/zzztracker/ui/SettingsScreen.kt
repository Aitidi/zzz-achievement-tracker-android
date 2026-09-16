package com.aitidi.zzztracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.aitidi.zzztracker.ui.theme.AppColors

@Composable
internal fun SettingsScreen(compact: Boolean, onCompact: (Boolean) -> Unit, onExport: () -> Unit, onImport: () -> Unit, onReset: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = PageGutter, end = PageGutter, bottom = 24.dp)) {
        item { PageTitle("设置", "按你的习惯，轻松记录") }
        item {
            SectionLabel("显示", Modifier.padding(top = 0.dp))
            Row(Modifier.fillMaxWidth().clip(GroupShape).background(AppColors.Surface)
                .toggleable(compact, role = Role.Switch, onValueChange = onCompact).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                SettingsIcon(Icons.AutoMirrored.Outlined.Notes)
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text("紧凑模式", style = MaterialTheme.typography.titleMedium)
                    Text("减少列表间距", Modifier.padding(top = 3.dp), style = MaterialTheme.typography.bodySmall, color = AppColors.Secondary)
                }
                Switch(compact, onCheckedChange = null, colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White, checkedTrackColor = AppColors.Green,
                    uncheckedThumbColor = Color.White, uncheckedTrackColor = AppColors.Control, uncheckedBorderColor = Color.Transparent))
            }
        }
        item {
            SectionLabel("数据管理")
            Column(Modifier.clip(GroupShape).background(AppColors.Surface)) {
                SettingsRow("导出进度", "保存 JSON 备份", Icons.Outlined.FileUpload, onClick = onExport)
                GroupDivider(68)
                SettingsRow("导入进度", "从备份恢复", Icons.Outlined.FileDownload, onClick = onImport)
            }
            Spacer(Modifier.height(16.dp))
            Column(Modifier.clip(GroupShape).background(AppColors.Surface)) {
                SettingsRow("重置进度", "连续点击 5 次确认", Icons.Outlined.DeleteOutline, destructive = true, onClick = onReset)
            }
            Text("所有进度仅保存在本机。", Modifier.padding(start = 4.dp, top = 18.dp), color = AppColors.Secondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SettingsIcon(icon: ImageVector, destructive: Boolean = false) {
    Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(if (destructive) AppColors.Red.copy(alpha = 0.07f) else AppColors.BlueSoft), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(24.dp), tint = if (destructive) AppColors.Red else AppColors.Blue)
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, icon: ImageVector, destructive: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        SettingsIcon(icon, destructive)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (destructive) AppColors.Red else AppColors.Text)
            Text(subtitle, Modifier.padding(top = 3.dp), style = MaterialTheme.typography.bodySmall, color = AppColors.Secondary)
        }
        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(20.dp), tint = AppColors.Secondary.copy(alpha = 0.6f))
    }
}
