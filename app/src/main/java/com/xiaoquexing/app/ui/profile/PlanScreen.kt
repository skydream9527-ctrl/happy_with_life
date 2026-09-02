package com.xiaoquexing.app.ui.profile

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.xiaoquexing.app.data.remote.PlanStore
import com.xiaoquexing.app.di.rememberXiaoQueXingViewModelFactory
import com.xiaoquexing.app.viewmodel.PlanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    onBack: () -> Unit,
    viewModel: PlanViewModel = viewModel(factory = rememberXiaoQueXingViewModelFactory()),
) {
    val ui by viewModel.uiState.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        com.xiaoquexing.app.ui.theme.IosNavBar(title = "订阅", onBack = onBack)
        Text("当前：${ui.tierLabel}", style = MaterialTheme.typography.titleMedium)
        Text(
            "支付通道尚未接入（Play / 微信 / 支付宝都还没有）。下面只是权益对照和本机预览开关。",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(16.dp))
        Text("免费版", style = MaterialTheme.typography.titleMedium)
        PlanStore.freeBenefits.forEach { Text("· $it") }
        Spacer(Modifier.height(16.dp))
        Text("会员", style = MaterialTheme.typography.titleMedium)
        PlanStore.memberBenefits.forEach { Text("· $it") }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("预览会员界面", modifier = Modifier.weight(1f))
            Switch(checked = ui.previewMember, onCheckedChange = viewModel::setPreview)
        }
        Text("打开后只改本机标记，不会扣费，也不会开通真会员。", style = MaterialTheme.typography.bodySmall)
    }
}
