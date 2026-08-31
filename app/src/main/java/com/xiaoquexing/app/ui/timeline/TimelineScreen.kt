package com.xiaoquexing.app.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoquexing.app.data.entity.Record
import com.xiaoquexing.app.di.rememberXiaoQueXingViewModelFactory
import com.xiaoquexing.app.ui.components.RecordCard
import com.xiaoquexing.app.ui.components.TagChip
import com.xiaoquexing.app.viewmodel.TimelineViewModel

@Composable
fun TimelineScreen(
    onNavigateToShare: (Long) -> Unit,
    viewModel: TimelineViewModel = viewModel(factory = rememberXiaoQueXingViewModelFactory())
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(listState, uiState.hasMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { last ->
                val total = listState.layoutInfo.totalItemsCount
                if (uiState.hasMore && total > 0 && last >= total - 3) {
                    viewModel.loadMore()
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(text = "时间线", style = MaterialTheme.typography.displayLarge)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (uiState.query.active) "找到 ${uiState.totalCount} 条" else "你的每一个小确幸都在这里",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.query.search,
                onValueChange = viewModel::onSearch,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("搜索正文、心情、标签") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TagChip(
                    text = "有照片",
                    selected = uiState.query.photosOnly,
                    onClick = viewModel::togglePhotos,
                    small = true,
                )
                TagChip(
                    text = "有语音",
                    selected = uiState.query.voiceOnly,
                    onClick = viewModel::toggleVoice,
                    small = true,
                )
                viewModel.moodTags.forEach { mood ->
                    TagChip(
                        text = mood.name,
                        selected = uiState.query.mood == mood.name,
                        onClick = { viewModel.onMood(mood.name) },
                        small = true,
                        leadingEmoji = mood.emoji,
                    )
                }
            }
            if (uiState.query.active) {
                TextButton(onClick = viewModel::clearFilters) { Text("清除筛选") }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.recordsByDate.isEmpty() && !uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (uiState.query.active) "🔎" else "📔", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (uiState.query.active) "没有符合条件的记录" else "还没有记录",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (uiState.query.active) "换个关键词或心情试试" else "开始记录你的第一个小确幸吧～",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    uiState.recordsByDate.forEach { (date, records) ->
                        item {
                            Text(
                                text = date,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                            )
                        }
                        items(records, key = { record: Record -> record.id }) { record ->
                            RecordCard(
                                record = record,
                                onClick = { onNavigateToShare(record.id) }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}
