package com.xiaoquexing.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoquexing.app.di.rememberXiaoQueXingViewModelFactory
import com.xiaoquexing.app.viewmodel.SharedSpaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedSpaceScreen(
    onBack: () -> Unit,
    onNeedLogin: () -> Unit,
    viewModel: SharedSpaceViewModel = viewModel(factory = rememberXiaoQueXingViewModelFactory()),
) {
    val ui by viewModel.uiState.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        TopAppBar(
            title = { Text("共享空间") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            },
        )
        if (ui.session == null) {
            Text("合种需要先登录。两个人各自登录后，一方建空间并发邀请码，另一方粘贴加入。")
            Spacer(Modifier.height(12.dp))
            Button(onClick = onNeedLogin, modifier = Modifier.fillMaxWidth()) { Text("去登录") }
            AnniversaryBlock(ui, viewModel)
            return
        }
        Text("当前空间的新记录会算进这个空间的植物。个人空间以外最多邀请朋友一起养。", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))
        ui.spaces.forEach { space ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { viewModel.switchTo(space.localId) }
                    .padding(14.dp),
            ) {
                Text(space.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${space.spaceType} · ${if (space.isDefault) "使用中" else "点击切换"}",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (!space.serverId.isNullOrBlank() && space.spaceType != "PERSONAL") {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.invite(space.serverId) }) { Text("生成邀请") }
                        OutlinedButton(onClick = { viewModel.loadMembers(space.serverId) }) { Text("成员") }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = ui.newName,
            onValueChange = viewModel::onName,
            label = { Text("新空间名称") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = { viewModel.create() }, enabled = !ui.busy, modifier = Modifier.fillMaxWidth()) {
            Text(if (ui.busy) "处理中…" else "创建合种空间")
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = ui.inviteToken,
            onValueChange = viewModel::onToken,
            label = { Text("粘贴邀请码") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { viewModel.accept() }, enabled = !ui.busy, modifier = Modifier.fillMaxWidth()) {
            Text("加入空间")
        }
        ui.inviteLink?.let {
            Spacer(Modifier.height(12.dp))
            Text("邀请： $it", style = MaterialTheme.typography.bodySmall)
        }
        if (ui.members.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("成员", style = MaterialTheme.typography.titleSmall)
            ui.members.forEach { m ->
                Text("${m.userId.take(8)} · ${m.role} · ${m.contributedGp} GP", style = MaterialTheme.typography.bodySmall)
            }
        }
        ui.message?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.primary)
        }
        AnniversaryBlock(ui, viewModel)
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun AnniversaryBlock(
    ui: com.xiaoquexing.app.viewmodel.SharedSpaceUi,
    viewModel: SharedSpaceViewModel,
) {
    Spacer(Modifier.height(20.dp))
    Text("纪念日", style = MaterialTheme.typography.titleMedium)
    Text("当天 21 点用本地通知提醒，不依赖推送服务。", style = MaterialTheme.typography.bodySmall)
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(value = ui.annTitle, onValueChange = viewModel::onAnnTitle, label = { Text("名称") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(value = ui.annMonth, onValueChange = viewModel::onAnnMonth, label = { Text("月") }, modifier = Modifier.weight(1f), singleLine = true)
        OutlinedTextField(value = ui.annDay, onValueChange = viewModel::onAnnDay, label = { Text("日") }, modifier = Modifier.weight(1f), singleLine = true)
    }
    Spacer(Modifier.height(8.dp))
    Button(onClick = { viewModel.addAnniversary() }, modifier = Modifier.fillMaxWidth()) { Text("添加纪念日") }
    ui.anniversaries.forEach { item ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("${item.month}月${item.day}日  ${item.title}")
            Text(
                "删除",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { viewModel.removeAnniversary(item.id) },
            )
        }
    }
}
