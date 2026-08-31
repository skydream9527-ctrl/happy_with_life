@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.xiaoquexing.app.ui.record

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoquexing.app.di.rememberXiaoQueXingViewModelFactory
import com.xiaoquexing.app.media.rememberMediaPicker
import com.xiaoquexing.app.media.rememberVoicePlayer
import com.xiaoquexing.app.ui.components.AddContentRow
import com.xiaoquexing.app.ui.components.LinkCard
import com.xiaoquexing.app.ui.components.LocationCard
import com.xiaoquexing.app.ui.components.MusicCard
import com.xiaoquexing.app.ui.components.PhotoGrid
import com.xiaoquexing.app.ui.components.TagChip
import com.xiaoquexing.app.ui.components.VoiceRecorder
import com.xiaoquexing.app.viewmodel.RecordViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    onPublished: () -> Unit,
    onBack: () -> Unit = onPublished,
    recordId: Long = 0L,
    viewModel: RecordViewModel = viewModel(factory = rememberXiaoQueXingViewModelFactory())
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val mediaPicker = rememberMediaPicker()
    val voicePlayer = rememberVoicePlayer()
    var voicePlaying by remember { mutableStateOf(false) }
    var voiceProgress by remember { mutableStateOf(0f) }
    var showAddMusicDialog by remember { mutableStateOf(false) }
    var showAddLinkDialog by remember { mutableStateOf(false) }
    var showAddLocationDialog by remember { mutableStateOf(false) }
    var showPhotoSourceSheet by remember { mutableStateOf(false) }
    var musicTitleInput by remember { mutableStateOf("") }
    var musicArtistInput by remember { mutableStateOf("") }
    var linkUrlInput by remember { mutableStateOf("") }
    var locationInput by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(recordId) {
        if (recordId > 0) viewModel.load(recordId)
    }

    LaunchedEffect(voicePlaying) {
        while (voicePlaying) {
            voiceProgress = voicePlayer.progress()
            kotlinx.coroutines.delay(100)
        }
    }

    // 接好录音的停止回调：picker.stopRecording() 后调到这里
    LaunchedEffect(mediaPicker) {
        mediaPicker.setOnRecordStoppedCallback { path, durationMs ->
            if (path != null && durationMs > 0) {
                viewModel.setVoice(path, durationMs)
            } else {
                viewModel.setVoiceRecording(false)
                if (path == null) {
                    viewModel.dismissError()
                    snackbarHostState.showSnackbar("录音失败或文件损坏")
                }
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    // GP Animation overlay
    if (uiState.showGpAnimation) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "+${uiState.earnedGp} GP",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("小确幸已记录 🌱", color = Color.White, fontSize = 18.sp)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // iOS 风格居中标题导航栏
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(
                    text = if (uiState.editingId > 0) "编辑记录" else "记录小确幸",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.align(Alignment.Center)
                )
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // Mood tags
            Text(
                text = "今天的心情是？",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewModel.moodTags.forEach { mood ->
                    TagChip(
                        text = mood.name,
                        selected = uiState.selectedMood == mood.name,
                        onClick = {
                            viewModel.selectMood(if (uiState.selectedMood == mood.name) null else mood.name)
                        },
                        leadingEmoji = mood.emoji,
                        selectedColor = Color(mood.color)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("发生日期", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = formatOccurred(uiState.occurredAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status tags
            Text(
                text = "添加标签",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewModel.statusTags.forEach { tag ->
                    TagChip(
                        text = tag.name,
                        selected = uiState.selectedStatusTags.contains(tag.name),
                        onClick = { viewModel.toggleStatusTag(tag.name) },
                        leadingEmoji = tag.emoji
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Text input - iOS 填充式输入框
            OutlinedTextField(
                value = uiState.text,
                onValueChange = { viewModel.updateText(it) },
                placeholder = { Text("今天发生了什么小确幸呢？") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                ),
                maxLines = 8
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Content previews
            // Photos
            if (uiState.photoUris.isNotEmpty()) {
                PhotoGrid(
                    photos = uiState.photoUris,
                    onRemove = { viewModel.removePhoto(it) },
                    showAddButton = false
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Voice
            if (uiState.voiceUri != null || uiState.isRecording) {
                VoiceRecorder(
                    isRecording = uiState.isRecording,
                    onStartRecording = {
                        if (mediaPicker.hasPermission(android.Manifest.permission.RECORD_AUDIO)) {
                            val started = mediaPicker.startRecording()
                            if (started) viewModel.setVoiceRecording(true)
                        } else {
                            mediaPicker.requestAudioPermission { granted ->
                                if (granted) {
                                    val started = mediaPicker.startRecording()
                                    if (started) viewModel.setVoiceRecording(true)
                                }
                            }
                        }
                    },
                    onStopRecording = {
                        viewModel.setVoiceRecording(false)
                        mediaPicker.stopRecording() // 回调由 LaunchedEffect 接住
                    },
                    recordedDurationMs = uiState.voiceDuration,
                    hasRecording = uiState.voiceUri != null,
                    isPlaying = voicePlaying,
                    playProgress = voiceProgress,
                    onPlay = {
                        val path = uiState.voiceUri
                        if (path.isNullOrBlank()) return@VoiceRecorder
                        if (voicePlayer.isPlaying()) {
                            voicePlayer.resume()
                            voicePlaying = true
                        } else {
                            val ok = voicePlayer.play(
                                path,
                                onProgress = { voiceProgress = it },
                                onDone = {
                                    voicePlaying = false
                                    voiceProgress = 1f
                                },
                            )
                            voicePlaying = ok
                            if (!ok) {
                                coroutineScope.launch { snackbarHostState.showSnackbar("无法播放，文件可能已损坏") }
                            }
                        }
                    },
                    onPause = {
                        voicePlayer.pause()
                        voicePlaying = false
                    },
                    onDelete = {
                        voicePlayer.stop()
                        voicePlaying = false
                        voiceProgress = 0f
                        if (uiState.isRecording) {
                            mediaPicker.cancelRecording()
                        }
                        viewModel.removeVoice()
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Music
            if (uiState.musicTitle != null) {
                MusicCard(
                    title = uiState.musicTitle!!,
                    artist = uiState.musicArtist ?: "未知歌手",
                    onRemove = { viewModel.removeMusic() }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Link
            if (uiState.linkUrl != null) {
                LinkCard(
                    url = uiState.linkUrl!!,
                    title = uiState.linkTitle,
                    summary = uiState.linkSummary,
                    onRemove = { viewModel.removeLink() }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Location
            if (uiState.locationName != null) {
                LocationCard(
                    locationName = uiState.locationName!!,
                    onRemove = { viewModel.removeLocation() }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Add content buttons
            AddContentRow(
                onPhotoClick = { showPhotoSourceSheet = true },
                onVoiceClick = {
                    // 没有权限先请求；有权限直接开始录音
                    if (mediaPicker.hasPermission(android.Manifest.permission.RECORD_AUDIO)) {
                        val started = mediaPicker.startRecording()
                        if (started) viewModel.setVoiceRecording(true)
                        else viewModel.setVoiceRecording(false) // 录音启动失败
                    } else {
                        mediaPicker.requestAudioPermission { granted ->
                            if (granted) {
                                val started = mediaPicker.startRecording()
                                if (started) viewModel.setVoiceRecording(true)
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("需要录音权限才能记录语音")
                                }
                            }
                        }
                    }
                },
                onMusicClick = { showAddMusicDialog = true },
                onLinkClick = { showAddLinkDialog = true },
                onLocationClick = { showAddLocationDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Estimated GP - iOS 风格浅绿卡片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "预计获得",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "+${uiState.estimatedGp} GP 🌱",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Publish button
            Button(
                onClick = { viewModel.publish(onPublished) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = !uiState.isPublishing
            ) {
                if (uiState.isPublishing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (uiState.editingId > 0) "保存修改" else "发布",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            if (uiState.editingId > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isDeleting,
                ) {
                    Text("删除这条记录", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Add Music Dialog
    if (showAddMusicDialog) {
        AlertDialog(
            onDismissRequest = { showAddMusicDialog = false },
            title = { Text("添加音乐") },
            text = {
                Column {
                    OutlinedTextField(
                        value = musicTitleInput,
                        onValueChange = { musicTitleInput = it },
                        label = { Text("歌曲名称") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = musicArtistInput,
                        onValueChange = { musicArtistInput = it },
                        label = { Text("歌手") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (musicTitleInput.isNotBlank()) {
                        viewModel.setMusic(musicTitleInput, musicArtistInput.ifBlank { "未知歌手" })
                        musicTitleInput = ""
                        musicArtistInput = ""
                        showAddMusicDialog = false
                    }
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showAddMusicDialog = false }) { Text("取消") }
            }
        )
    }

    // Add Link Dialog
    if (showAddLinkDialog) {
        AlertDialog(
            onDismissRequest = { showAddLinkDialog = false },
            title = { Text("添加链接") },
            text = {
                OutlinedTextField(
                    value = linkUrlInput,
                    onValueChange = { linkUrlInput = it },
                    label = { Text("链接URL或标题") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (linkUrlInput.isNotBlank()) {
                        viewModel.setLink(linkUrlInput, linkUrlInput)
                        linkUrlInput = ""
                        showAddLinkDialog = false
                    }
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showAddLinkDialog = false }) { Text("取消") }
            }
        )
    }

    // Add Location Dialog
    if (showAddLocationDialog) {
        AlertDialog(
            onDismissRequest = { showAddLocationDialog = false },
            title = { Text("添加地点") },
            text = {
                OutlinedTextField(
                    value = locationInput,
                    onValueChange = { locationInput = it },
                    label = { Text("地点名称") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (locationInput.isNotBlank()) {
                        viewModel.setLocation(locationInput)
                        locationInput = ""
                        showAddLocationDialog = false
                    }
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showAddLocationDialog = false }) { Text("取消") }
            }
        )
    }

    // Photo source bottom sheet
    if (showPhotoSourceSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPhotoSourceSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    "添加照片",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                PhotoSourceRow(
                    icon = Icons.Default.PhotoLibrary,
                    label = "从相册选择",
                    onClick = {
                        showPhotoSourceSheet = false
                        mediaPicker.pickPhotos { uris ->
                            if (uris.isNotEmpty()) viewModel.addPhotos(uris)
                        }
                    }
                )
                PhotoSourceRow(
                    icon = Icons.Default.CameraAlt,
                    label = "拍照",
                    onClick = {
                        showPhotoSourceSheet = false
                        mediaPicker.takePhoto { path ->
                            if (path != null) viewModel.addPhoto(path)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showDatePicker) {
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = uiState.occurredAt.takeIf { it > 0 } ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { viewModel.setOccurredAt(it) }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.clearOccurredAt()
                    showDatePicker = false
                }) { Text("今天") }
            },
        ) {
            DatePicker(state = picker)
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除记录") },
            text = { Text("删除后时间线和植物积分会重算，云端同步为软删除。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete(onPublished)
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            },
        )
    }
}

private fun formatOccurred(occurredAt: Long): String {
    val ms = if (occurredAt > 0) occurredAt else System.currentTimeMillis()
    val date = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    val today = java.time.LocalDate.now()
    return when {
        date == today -> "今天"
        date == today.minusDays(1) -> "昨天"
        else -> date.toString()
    }
}

@Composable
private fun PhotoSourceRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
