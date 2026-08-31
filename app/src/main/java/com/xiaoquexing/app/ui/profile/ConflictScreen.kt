package com.xiaoquexing.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoquexing.app.di.rememberXiaoQueXingViewModelFactory
import com.xiaoquexing.app.viewmodel.ConflictViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConflictScreen(
    onBack: () -> Unit,
    viewModel: ConflictViewModel = viewModel(factory = rememberXiaoQueXingViewModelFactory()),
) {
    val ui by viewModel.uiState.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        TopAppBar(
            title = { Text("同步冲突") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
            },
        )
        Text("本地和云端不是同一版本。选保留哪一边，不会自动合并。", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        ui.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        if (ui.items.isEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text("现在没有冲突。")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
                items(ui.items, key = { it.localId }) { row ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(row.moodTag, style = MaterialTheme.typography.titleMedium)
                            Text(row.contentText.orEmpty().ifBlank { "（无正文）" })
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.keepLocal(row.localId) },
                                    enabled = ui.busyId == null,
                                ) { Text("保留本地") }
                                OutlinedButton(
                                    onClick = { viewModel.keepCloud(row.localId) },
                                    enabled = ui.busyId == null,
                                ) { Text("采用云端") }
                            }
                        }
                    }
                }
            }
        }
    }
}
