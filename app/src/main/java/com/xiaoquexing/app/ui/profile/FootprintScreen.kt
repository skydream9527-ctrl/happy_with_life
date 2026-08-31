package com.xiaoquexing.app.ui.profile

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoquexing.app.di.rememberXiaoQueXingViewModelFactory
import com.xiaoquexing.app.viewmodel.FootprintPlace
import com.xiaoquexing.app.viewmodel.FootprintViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FootprintScreen(
    onBack: () -> Unit,
    viewModel: FootprintViewModel = viewModel(factory = rememberXiaoQueXingViewModelFactory()),
) {
    val ui by viewModel.uiState.collectAsState()
    val selected = ui.places.find { it.name == ui.selected }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        TopAppBar(
            title = { Text(selected?.name ?: "足迹") },
            navigationIcon = {
                IconButton(onClick = { if (selected != null) viewModel.close() else onBack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            },
        )
        if (selected == null) {
            Text("按记录里的地点汇总，不依赖地图密钥。", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            if (ui.places.isEmpty()) {
                Text("还没有带地点的记录。写一条时加上地点即可。")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(ui.places, key = { it.name }) { place ->
                        PlaceCard(place) { viewModel.open(place.name) }
                    }
                }
            }
        } else {
            Text("${selected.count} 条 · ${selected.gp} GP", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(selected.records, key = { it.id }) { rec ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(rec.moodTag ?: "记录", style = MaterialTheme.typography.titleSmall)
                            Text(rec.text.ifBlank { "（无正文）" })
                            Text(
                                SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date(rec.createdAt)),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceCard(place: FootprintPlace, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text("📍 ${place.name}", style = MaterialTheme.typography.titleMedium)
                Text("${place.count} 次", style = MaterialTheme.typography.bodySmall)
            }
            Text("${place.gp} GP", color = MaterialTheme.colorScheme.primary)
        }
    }
}
