package com.xiaoquexing.app.ui.profile

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.FilterChip
import com.xiaoquexing.app.BuildConfig
import com.xiaoquexing.app.data.remote.SettingsStore
import com.xiaoquexing.app.di.rememberXiaoQueXingViewModelFactory
import com.xiaoquexing.app.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = rememberXiaoQueXingViewModelFactory()),
) {
    val ui by viewModel.uiState.collectAsState()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.setReminder(granted)
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let(viewModel::exportBackup)
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importBackup)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        TopAppBar(
            title = { Text("设置") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
            },
        )
        Text("通知", style = MaterialTheme.typography.titleMedium)
        ToggleRow("每天 21:00 提醒记录", ui.reminderOn) { on ->
            if (on && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else viewModel.setReminder(on)
        }
        Spacer(Modifier.height(16.dp))
        Text("隐私", style = MaterialTheme.typography.titleMedium)
        Text(
            "记录默认只存在本机。登录后才会同步到你的个人空间。关闭统计后，应用不会上报使用行为。",
            style = MaterialTheme.typography.bodySmall,
        )
        ToggleRow("关闭使用统计", ui.analyticsOff, viewModel::setAnalyticsOff)
        ToggleRow("个人页隐藏手机号", ui.hidePhone, viewModel::setHidePhone)
        Spacer(Modifier.height(16.dp))
        Text("外观", style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf(
                SettingsStore.MODE_SYSTEM to "跟随系统",
                SettingsStore.MODE_LIGHT to "浅色",
                SettingsStore.MODE_DARK to "深色",
            ).forEach { (mode, label) ->
                FilterChip(
                    selected = ui.themeMode == mode,
                    onClick = { viewModel.setTheme(mode) },
                    label = { Text(label) },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }
        ToggleRow("大字体（1.3 倍）", ui.largeText, viewModel::setLargeText)
        Spacer(Modifier.height(16.dp))
        Text("本地备份", style = MaterialTheme.typography.titleMedium)
        Text("导出记录和照片/录音到一个 zip，换机可再导入。重复条目会跳过。", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        Button(onClick = { export.launch("xqx-backup.zip") }, modifier = Modifier.fillMaxWidth()) {
            Text("导出备份")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { restore.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("从文件恢复") }
        Text("TalkBack 可朗读按钮和照片。系统字体缩放也会作用在本页。", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))
        Text("账号注销", style = MaterialTheme.typography.titleMedium)
        Text(
            "申请后进入 7 天冷静期。到期前可取消；到期后确认才会向服务器申请删除并退出登录。",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(8.dp))
        when {
            !ui.deleteRequested -> OutlinedButton(onClick = { viewModel.requestDelete() }, modifier = Modifier.fillMaxWidth()) {
                Text("申请注销账号")
            }
            !ui.coolingOver -> {
                Text("冷静期至 ${ui.purgeLabel}")
                Spacer(Modifier.height(8.dp))
                Button(onClick = { viewModel.cancelDelete() }, modifier = Modifier.fillMaxWidth()) {
                    Text("取消注销申请")
                }
            }
            else -> {
                Text("冷静期已结束，可以确认删除")
                Spacer(Modifier.height(8.dp))
                Button(onClick = { viewModel.confirmDelete() }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (ui.loggedIn) "确认删除并退出" else "清除本地登录状态")
                }
                OutlinedButton(onClick = { viewModel.cancelDelete() }, modifier = Modifier.fillMaxWidth()) {
                    Text("我再想想")
                }
            }
        }
        ui.message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(24.dp))
        Text("关于 小确幸 ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
