package com.xiaoquexing.app.ui.record

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.xiaoquexing.app.di.rememberXiaoQueXingViewModelFactory
import com.xiaoquexing.app.media.rememberVoicePlayer
import com.xiaoquexing.app.viewmodel.RecordDetailViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDetailScreen(
    recordId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onShare: (Long) -> Unit,
    viewModel: RecordDetailViewModel = viewModel(factory = rememberXiaoQueXingViewModelFactory()),
) {
    val ui by viewModel.uiState.collectAsState()
    LaunchedEffect(recordId) { viewModel.load(recordId) }
    val rec = ui.record
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        TopAppBar(
            title = { Text("记录") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
            },
        )
        if (rec == null) {
            Text(if (ui.gone) "这条记录不存在或已删除" else "加载中…")
            return
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(rec.moodTag ?: "未选心情", style = MaterialTheme.typography.titleMedium)
            Text(
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(rec.createdAt)),
                style = MaterialTheme.typography.bodySmall,
            )
            if (rec.isBackdated) Text("补记", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            Text(rec.text.ifBlank { "（无正文）" }, style = MaterialTheme.typography.bodyLarge)
            rec.locationName?.let {
                Spacer(Modifier.height(8.dp))
                Text("📍 $it")
            }
            rec.getPhotoUriList().forEach { uri ->
                Spacer(Modifier.height(8.dp))
                AsyncImage(
                    model = uri,
                    contentDescription = "照片",
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    contentScale = ContentScale.Crop,
                )
            }
            rec.voiceUri?.let { path ->
                Spacer(Modifier.height(12.dp))
                VoiceBlock(path)
            }
            Spacer(Modifier.height(12.dp))
            Text("+${rec.gpEarned} GP", color = MaterialTheme.colorScheme.primary)
            ui.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.height(16.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { onEdit(rec.id) }, modifier = Modifier.weight(1f)) { Text("编辑") }
            OutlinedButton(onClick = { onShare(rec.id) }, modifier = Modifier.weight(1f)) { Text("分享") }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { viewModel.delete(onBack) }, modifier = Modifier.fillMaxWidth()) {
            Text("删除")
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun VoiceBlock(path: String) {
    val player = rememberVoicePlayer()
    var playing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(playing) {
        while (playing) {
            progress = player.progress()
            delay(200)
        }
    }
    Row {
        IconButton(onClick = {
            if (playing) {
                player.pause()
                playing = false
            } else {
                playing = player.play(path, { progress = it }, { playing = false })
            }
        }) {
            Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "播放录音")
        }
        Column(Modifier.weight(1f).padding(top = 12.dp)) {
            Text("语音")
            LinearProgressIndicator(progress = progress.coerceIn(0f, 1f), modifier = Modifier.fillMaxWidth())
        }
    }
}
